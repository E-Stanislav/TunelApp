package com.tunelapp.core

import android.content.Context
import android.util.Log
import com.tunelapp.data.ProxyServer
import com.tunelapp.data.VlessServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manager for Xray-core operations
 * Now uses SingBoxManager with actual sing-box binary!
 */
class XrayManager(private val context: Context) {
    
    private val singBoxManager = SingBoxManager(context)
    
    private var isRunning = false
    private var configPath: String? = null
    
    companion object {
        private const val TAG = "XrayManager"
        private const val CONFIG_FILE = "config.json"
    }
    
    /**
     * Start Xray with the given server configuration
     * Now uses actual sing-box binary!
     */
    suspend fun start(server: VlessServer, tunFd: Int? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Convert VlessServer to ProxyServer for new API
            val proxyServer = ProxyServer(
                id = server.id,
                name = server.name,
                protocol = com.tunelapp.data.ProxyProtocol.VLESS,
                address = server.address,
                port = server.port,
                uuid = server.uuid,
                encryption = server.encryption,
                flow = server.flow,
                network = server.network,
                security = server.security,
                sni = server.sni,
                fingerprint = server.fingerprint,
                alpn = server.alpn,
                allowInsecure = server.allowInsecure,
                path = server.path,
                host = server.host,
                serviceName = server.serviceName,
                quicSecurity = server.quicSecurity,
                key = server.key,
                headerType = server.headerType,
                remarks = server.remarks,
                isActive = server.isActive,
                createdAt = server.createdAt,
                lastUsed = server.lastUsed
            )
            
            // Use sing-box binary
            val result = singBoxManager.start(proxyServer, tunFd)
            if (result.isSuccess) {
                isRunning = true
            }
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sing-box", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start with ProxyServer (new API)
     */
    suspend fun start(server: ProxyServer, tunFd: Int? = null): Result<Unit> {
        return singBoxManager.start(server, tunFd)
    }
    
    /**
     * Stop sing-box
     */
    suspend fun stop(): Result<Unit> {
        isRunning = false
        return singBoxManager.stop()
    }
    
    /**
     * Check if sing-box is running
     */
    fun isRunning(): Boolean {
        return singBoxManager.isRunning()
    }
    
    /**
     * Get current statistics from sing-box
     */
    suspend fun getStats(): Map<String, Long> {
        return singBoxManager.getStats()
    }
}

/**
 * JNI interface for Xray (to be implemented)
 * This will interface with libXray.aar
 */
object XrayJNI {
    init {
        try {
            System.loadLibrary("xray")
        } catch (e: Exception) {
            Log.w("XrayJNI", "Failed to load libxray.so: ${e.message}")
        }
    }
    
    // Native methods to be implemented
    external fun startXray(configPath: String): Int
    external fun stopXray(): Int
    external fun getStats(): String
    external fun getVersion(): String
}





