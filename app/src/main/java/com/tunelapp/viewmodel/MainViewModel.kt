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
import com.tunelapp.parser.VlessParser
import com.tunelapp.service.TunelVpnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Main ViewModel for the application
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = VlessDatabase.getDatabase(application)
    private val repository = VlessRepository(database.vlessServerDao())
    
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
    
    // Combined VPN state
    val vpnState: StateFlow<VpnState> = combine(
        connectionState,
        currentServer,
        trafficStats,
        errorMessage
    ) { state, server, stats, error ->
        VpnState(state, server, stats, error)
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
                
                // Parse VLESS URL
                val parseResult = VlessParser.parse(text)
                
                if (parseResult.isSuccess) {
                    val server = parseResult.getOrThrow()
                    
                    // Validate server
                    val validateResult = VlessParser.validate(server)
                    if (validateResult.isFailure) {
                        _importResult.value = Result.failure(validateResult.exceptionOrNull()!!)
                        return@launch
                    }
                    
                    // Save to database
                    val id = repository.insertServer(server)
                    val savedServer = server.copy(id = id)
                    
                    _importResult.value = Result.success(savedServer)
                    Log.d(TAG, "Server imported successfully: ${server.name}")
                    
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
    fun connect(context: Context, server: VlessServer) {
        viewModelScope.launch {
            try {
                // Check VPN permission
                val intent = VpnService.prepare(context)
                if (intent != null) {
                    _errorMessage.value = "VPN permission required"
                    // The UI should handle starting the permission activity
                    return@launch
                }
                
                _connectionState.value = ConnectionState.CONNECTING
                _currentServer.value = server
                
                // Set as active server
                repository.setActiveServer(server.id)
                
                // Start VPN service
                val serviceIntent = Intent(context, TunelVpnService::class.java).apply {
                    action = TunelVpnService.ACTION_CONNECT
                    putExtra(TunelVpnService.EXTRA_SERVER_ID, server.id)
                }
                context.startForegroundService(serviceIntent)
                
                // Simulate connection (in production, we'd listen to service broadcasts)
                kotlinx.coroutines.delay(1000)
                _connectionState.value = ConnectionState.CONNECTED
                
                Log.d(TAG, "Connected to: ${server.name}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _connectionState.value = ConnectionState.ERROR
                _errorMessage.value = e.message
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
                
                // Stop VPN service
                val serviceIntent = Intent(context, TunelVpnService::class.java).apply {
                    action = TunelVpnService.ACTION_DISCONNECT
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
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

