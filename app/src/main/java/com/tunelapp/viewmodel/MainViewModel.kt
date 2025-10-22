package com.tunelapp.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tunelapp.data.*
import com.tunelapp.parser.UniversalParser
import com.tunelapp.parser.VlessParser
import com.tunelapp.service.SimpleVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the application
 * NOTE: This is legacy code, consider using ProxyViewModel for new features
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = TunelDatabase.getInstance(application)
    private val repository = VlessRepository(database.vlessServerDao())
    private val proxyRepository = ProxyRepository(application)
    
    companion object {
        private const val TAG = "MainViewModel"
    }
    
    // State flows
    val servers: StateFlow<List<VlessServer>> = repository.allServers
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _currentServer = MutableStateFlow<VlessServer?>(null)
    val currentServer: StateFlow<VlessServer?> = _currentServer.asStateFlow()
    
    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _importResult = MutableStateFlow<Result<VlessServer>?>(null)
    val importResult: StateFlow<Result<VlessServer>?> = _importResult.asStateFlow()
    
    private var statsUpdateJob: Job? = null
    
    // Combined VPN state
    val vpnState: StateFlow<VpnState> = combine(
        connectionState,
        currentServer,
        trafficStats,
        errorMessage
    ) { state, server, stats, error ->
        // Convert VlessServer to ProxyServer for VpnState
        val proxyServer = server?.let { vless ->
            ProxyServer(
                id = vless.id,
                name = vless.name,
                protocol = ProxyProtocol.VLESS,
                address = vless.address,
                port = vless.port,
                uuid = vless.uuid,
                encryption = vless.encryption,
                flow = vless.flow,
                network = vless.network,
                security = vless.security,
                sni = vless.sni,
                fingerprint = vless.fingerprint,
                alpn = vless.alpn,
                allowInsecure = vless.allowInsecure,
                path = vless.path,
                host = vless.host,
                serviceName = vless.serviceName,
                quicSecurity = vless.quicSecurity,
                key = vless.key,
                headerType = vless.headerType,
                remarks = vless.remarks,
                isActive = vless.isActive,
                createdAt = vless.createdAt,
                lastUsed = vless.lastUsed
            )
        }
        VpnState(state, proxyServer, stats, error)
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        VpnState()
    )
    
    init {
        loadActiveServer()
    }
    
    /**
     * Load the active server from database
     */
    private fun loadActiveServer() {
        viewModelScope.launch {
            val server = repository.getActiveServer()
            _currentServer.value = server
        }
    }
    
    /**
     * Import VLESS URL from clipboard
     */
    fun importFromClipboard(context: Context) {
        viewModelScope.launch {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = clipboard.primaryClip
                
                if (clipData == null || clipData.itemCount == 0) {
                    _importResult.value = Result.failure(Exception("Clipboard is empty"))
                    return@launch
                }
                
                val text = clipData.getItemAt(0).text?.toString()
                if (text.isNullOrEmpty()) {
                    _importResult.value = Result.failure(Exception("Clipboard is empty"))
                    return@launch
                }
                
                // Try to parse with UniversalParser first (supports all protocols)
                val parseResult = UniversalParser.parse(text)
                
                if (parseResult.isSuccess) {
                    val proxyServer = parseResult.getOrThrow()
                    
                    // If it's VLESS, also save to VlessServer for backward compatibility
                    if (proxyServer.protocol == ProxyProtocol.VLESS) {
                        val vlessServer = VlessServer(
                            name = proxyServer.name,
                            uuid = proxyServer.uuid!!,
                            address = proxyServer.address,
                            port = proxyServer.port,
                            encryption = proxyServer.encryption ?: "none",
                            flow = proxyServer.flow,
                            network = proxyServer.network,
                            security = proxyServer.security,
                            sni = proxyServer.sni,
                            fingerprint = proxyServer.fingerprint,
                            alpn = proxyServer.alpn,
                            allowInsecure = proxyServer.allowInsecure,
                            path = proxyServer.path,
                            host = proxyServer.host,
                            serviceName = proxyServer.serviceName,
                            quicSecurity = proxyServer.quicSecurity,
                            key = proxyServer.key,
                            headerType = proxyServer.headerType,
                            remarks = proxyServer.remarks
                        )
                        
                        val id = repository.insertServer(vlessServer)
                        _importResult.value = Result.success(vlessServer.copy(id = id))
                        Log.d(TAG, "Server imported successfully: ${vlessServer.name}")
                    } else {
                        // For other protocols, save to ProxyServer table
                        proxyRepository.insertServer(proxyServer)
                        // Convert to VlessServer for UI compatibility
                        _importResult.value = Result.success(VlessServer(
                            name = proxyServer.name,
                            uuid = proxyServer.uuid ?: "",
                            address = proxyServer.address,
                            port = proxyServer.port,
                            remarks = "${proxyServer.protocol} server"
                        ))
                    }
                } else {
                    _importResult.value = Result.failure(parseResult.exceptionOrNull() ?: Exception("Parse failed"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import from clipboard", e)
                _importResult.value = Result.failure(e)
            }
        }
    }
    
    /**
     * Clear import result
     */
    fun clearImportResult() {
        _importResult.value = null
    }
    
    /**
     * Connect to VPN
     */
    fun connect(context: Context, server: VlessServer) {
        Log.d(TAG, "connect called with server: ${server.name}")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Setting connection state to CONNECTING")
                _connectionState.value = ConnectionState.CONNECTING
                _currentServer.value = server
                
                // Set as active server
                Log.d(TAG, "Setting active server: ${server.id}")
                repository.setActiveServer(server.id)
                
                // Request VPN permission first
                Log.d(TAG, "Checking VPN permission...")
                val vpnIntent = VpnService.prepare(context)
                if (vpnIntent != null) {
                    // Need VPN permission
                    Log.w(TAG, "VPN permission required")
                    _connectionState.value = ConnectionState.ERROR
                    _errorMessage.value = "VPN permission required. Please grant permission and try again."
                    return@launch
                }
                
                Log.d(TAG, "VPN permission granted, starting service...")
                // Start VPN service
                val serviceIntent = Intent(context, SimpleVpnService::class.java).apply {
                    action = SimpleVpnService.ACTION_CONNECT
                    putExtra(SimpleVpnService.EXTRA_SERVER_ID, server.id)
                }
                
                Log.d(TAG, "Starting foreground service...")
                context.startForegroundService(serviceIntent)

                // Actively wait for the service to finish bringing up the TUN
                // interface and mark itself as running. This can take several
                // seconds on some devices/networks. We'll poll up to ~15s.
                Log.d(TAG, "Waiting for VPN service to report running...")
                var started = false
                repeat(60) { // 60 * 250ms = 15s max
                    if (SimpleVpnService.isVpnRunning()) {
                        started = true
                        return@repeat
                    }
                    delay(250)
                }

                // Check if service is running
                Log.d(TAG, "Checking if VPN service is running...")
                if (started && SimpleVpnService.isVpnRunning()) {
                    Log.d(TAG, "VPN service is running, setting state to CONNECTED")
                    _connectionState.value = ConnectionState.CONNECTED
                    startStatsUpdates()
                    Log.d(TAG, "Connected to: ${server.name}")
                } else {
                    Log.e(TAG, "VPN service failed to start within timeout")
                    _connectionState.value = ConnectionState.ERROR
                    _errorMessage.value = "Failed to start VPN service"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                e.printStackTrace()
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.value = e.message ?: "Unknown error occurred"
            }
        }
    }
    
    /**
     * Disconnect from VPN
     */
    fun disconnect(context: Context) {
        viewModelScope.launch {
            try {
                _connectionState.value = ConnectionState.DISCONNECTING
                
                // Stop stats updates
                stopStatsUpdates()
                
                // Stop VPN service
                val serviceIntent = Intent(context, SimpleVpnService::class.java).apply {
                    action = SimpleVpnService.ACTION_DISCONNECT
                }
                context.startService(serviceIntent)
                
                // Simulate disconnection
                kotlinx.coroutines.delay(500)
                _connectionState.value = ConnectionState.DISCONNECTED
                _trafficStats.value = TrafficStats()
                
                Log.d(TAG, "Disconnected")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.value = e.message
            }
        }
    }
    
    /**
     * Toggle VPN connection
     */
    fun toggleConnection(context: Context) {
        when (_connectionState.value) {
            ConnectionState.DISCONNECTED -> {
                _currentServer.value?.let { server ->
                    connect(context, server)
                } ?: run {
                    _errorMessage.value = context.getString(com.tunelapp.R.string.no_server_selected)
                }
            }
            ConnectionState.CONNECTED -> {
                disconnect(context)
            }
            else -> {
                Log.w(TAG, "Cannot toggle connection in state: ${_connectionState.value}")
            }
        }
    }
    
    /**
     * Select a server
     */
    fun selectServer(server: VlessServer) {
        _currentServer.value = server
    }
    
    /**
     * Delete a server
     */
    fun deleteServer(server: VlessServer) {
        viewModelScope.launch {
            repository.deleteServer(server)
            if (_currentServer.value?.id == server.id) {
                _currentServer.value = null
            }
        }
    }
    
    /**
     * Start periodic stats updates
     */
    private fun startStatsUpdates() {
        statsUpdateJob?.cancel()
        statsUpdateJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Update every second
                
                // Get stats from VPN service (simulate for now)
                val stats: TrafficStats? = null
                if (stats != null) {
                    _trafficStats.value = stats
                } else {
                    // Simulate some stats for demo
                    val currentStats = _trafficStats.value
                    _trafficStats.value = currentStats.copy(
                        uploadSpeed = (0..1024).random().toLong(),
                        downloadSpeed = (1024..5120).random().toLong(),
                        totalUpload = currentStats.totalUpload + (0..1024).random().toLong(),
                        totalDownload = currentStats.totalDownload + (1024..5120).random().toLong(),
                        connectedTime = System.currentTimeMillis() - (currentStats.connectedTime)
                    )
                }
            }
        }
    }
    
    /**
     * Stop stats updates
     */
    private fun stopStatsUpdates() {
        statsUpdateJob?.cancel()
        statsUpdateJob = null
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopStatsUpdates()
    }
}

