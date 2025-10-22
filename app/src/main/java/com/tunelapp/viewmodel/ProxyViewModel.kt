package com.tunelapp.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tunelapp.core.SpeedTester
import com.tunelapp.data.*
import com.tunelapp.parser.UniversalParser
import com.tunelapp.service.TunelVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Enhanced ViewModel supporting all proxy protocols
 */
class ProxyViewModel(application: Application) : AndroidViewModel(application) {
    
    private val proxyRepository = ProxyRepository(application)
    private val speedTester = SpeedTester()
    
    companion object {
        private const val TAG = "ProxyViewModel"
        private const val STATS_UPDATE_INTERVAL = 1000L // 1 second
    }
    
    // State flows
    val servers: StateFlow<List<ProxyServer>> = proxyRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val favoriteServers: StateFlow<List<ProxyServer>> = proxyRepository.getFavoriteServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _vpnState = MutableStateFlow(VpnState())
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()
    
    private val _importResult = MutableStateFlow<Result<ProxyServer>?>(null)
    val importResult: StateFlow<Result<ProxyServer>?> = _importResult.asStateFlow()
    
    private val _testResults = MutableStateFlow<Map<Long, ServerTestResult>>(emptyMap())
    val testResults: StateFlow<Map<Long, ServerTestResult>> = _testResults.asStateFlow()
    
    private var statsUpdateJob: Job? = null
    
    init {
        loadActiveServer()
    }
    
    /**
     * Load the active server from database
     */
    private fun loadActiveServer() {
        viewModelScope.launch {
            val server = proxyRepository.getActiveServer()
            _vpnState.update { it.copy(server = server) }
        }
    }
    
    /**
     * Import proxy URL from clipboard (supports all protocols)
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
                
                // Parse using UniversalParser (auto-detects protocol)
                val parseResult = UniversalParser.parse(text)
                
                if (parseResult.isSuccess) {
                    val server = parseResult.getOrThrow()
                    
                    // Validate server
                    val validateResult = UniversalParser.validate(server)
                    if (validateResult.isFailure) {
                        _importResult.value = Result.failure(validateResult.exceptionOrNull()!!)
                        return@launch
                    }
                    
                    // Save to database
                    val id = proxyRepository.insertServer(server)
                    val savedServer = server.copy(id = id)
                    
                    _importResult.value = Result.success(savedServer)
                    Log.d(TAG, "Server imported successfully: ${server.name} (${server.protocol})")
                    
                } else {
                    _importResult.value = parseResult
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
    fun connect(context: Context, server: ProxyServer) {
        viewModelScope.launch {
            try {
                _vpnState.update { 
                    it.copy(
                        connectionState = ConnectionState.CONNECTING,
                        server = server,
                        errorMessage = null
                    )
                }
                
                // Set as active server
                proxyRepository.setActiveServer(server.id)
                proxyRepository.updateLastUsed(server.id)
                
                // Start VPN service
                val serviceIntent = Intent(context, TunelVpnService::class.java).apply {
                    action = TunelVpnService.ACTION_CONNECT
                    putExtra(TunelVpnService.EXTRA_SERVER_ID, server.id)
                }
                context.startForegroundService(serviceIntent)
                
                // Start stats updates
                startStatsUpdates()
                
                // Simulate connection (in production, listen to service broadcasts)
                delay(1000)
                _vpnState.update { it.copy(connectionState = ConnectionState.CONNECTED) }
                
                Log.d(TAG, "Connected to: ${server.name} via ${server.protocol}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _vpnState.update { 
                    it.copy(
                        connectionState = ConnectionState.ERROR,
                        errorMessage = e.message
                    )
                }
            }
        }
    }
    
    /**
     * Disconnect from VPN
     */
    fun disconnect(context: Context) {
        viewModelScope.launch {
            try {
                _vpnState.update { it.copy(connectionState = ConnectionState.DISCONNECTING) }
                
                // Stop stats updates
                stopStatsUpdates()
                
                // Stop VPN service
                val serviceIntent = Intent(context, TunelVpnService::class.java).apply {
                    action = TunelVpnService.ACTION_DISCONNECT
                }
                context.startService(serviceIntent)
                
                // Simulate disconnection
                delay(500)
                _vpnState.update { 
                    it.copy(
                        connectionState = ConnectionState.DISCONNECTED,
                        stats = TrafficStats()
                    )
                }
                
                Log.d(TAG, "Disconnected")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                _vpnState.update { 
                    it.copy(
                        connectionState = ConnectionState.ERROR,
                        errorMessage = e.message
                    )
                }
            }
        }
    }
    
    /**
     * Toggle VPN connection
     */
    fun toggleConnection(context: Context) {
        when (_vpnState.value.connectionState) {
            ConnectionState.DISCONNECTED -> {
                _vpnState.value.server?.let { server ->
                    connect(context, server)
                } ?: run {
                    _vpnState.update { 
                        it.copy(errorMessage = "No server selected")
                    }
                }
            }
            ConnectionState.CONNECTED -> {
                disconnect(context)
            }
            else -> {
                Log.w(TAG, "Cannot toggle connection in state: ${_vpnState.value.connectionState}")
            }
        }
    }
    
    /**
     * Select a server
     */
    fun selectServer(server: ProxyServer) {
        _vpnState.update { it.copy(server = server) }
    }
    
    /**
     * Delete a server
     */
    fun deleteServer(server: ProxyServer) {
        viewModelScope.launch {
            proxyRepository.deleteServer(server)
            if (_vpnState.value.server?.id == server.id) {
                _vpnState.update { it.copy(server = null) }
            }
        }
    }
    
    /**
     * Toggle favorite status
     */
    fun toggleFavorite(server: ProxyServer) {
        viewModelScope.launch {
            proxyRepository.toggleFavorite(server)
        }
    }
    
    /**
     * Test server latency
     */
    fun testServer(server: ProxyServer) {
        viewModelScope.launch {
            val result = speedTester.testLatency(server)
            _testResults.update { 
                it + (server.id to result)
            }
            
            // Save to database
            if (result.isReachable && result.latency != null) {
                proxyRepository.updateTestResult(server.id, result.latency, null)
            }
        }
    }
    
    /**
     * Test all servers
     */
    fun testAllServers() {
        viewModelScope.launch {
            val serverList = servers.value
            speedTester.testServers(serverList, includeSpeed = false) { server, result ->
                viewModelScope.launch {
                    _testResults.update { 
                        it + (server.id to result)
                    }
                    
                    if (result.isReachable && result.latency != null) {
                        proxyRepository.updateTestResult(server.id, result.latency, null)
                    }
                }
            }
        }
    }
    
    /**
     * Find fastest server and connect
     */
    fun connectToFastest(context: Context) {
        viewModelScope.launch {
            val serverList = servers.value
            if (serverList.isEmpty()) {
                _vpnState.update { it.copy(errorMessage = "No servers available") }
                return@launch
            }
            
            val fastest = speedTester.findFastestServer(serverList)
            if (fastest != null) {
                connect(context, fastest)
            } else {
                _vpnState.update { it.copy(errorMessage = "No reachable servers found") }
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
                delay(STATS_UPDATE_INTERVAL)
                // TODO: Get real stats from XrayManager when core is integrated
                // val stats = xrayManager.getStats()
                // _vpnState.update { it.copy(stats = stats) }
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
        _vpnState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Search servers
     */
    fun searchServers(query: String): Flow<List<ProxyServer>> {
        return proxyRepository.searchServers(query)
    }
    
    /**
     * Get servers by protocol
     */
    fun getServersByProtocol(protocol: ProxyProtocol): Flow<List<ProxyServer>> {
        return proxyRepository.getServersByProtocol(protocol)
    }
    
    override fun onCleared() {
        super.onCleared()
        stopStatsUpdates()
    }
}

