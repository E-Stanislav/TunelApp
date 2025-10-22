package com.tunelapp.core

import android.content.Context
import android.util.Log
import com.tunelapp.data.ProxyServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manager for sing-box binary (executable)
 * This is a working implementation that uses the sing-box binary directly
 */
class SingBoxManager(private val context: Context) {
    
    private var process: Process? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "SingBoxManager"
        private const val BINARY_NAME = "sing-box"
        private const val CONFIG_FILE = "config.json"
    }
    
    /**
     * Start sing-box with the given server configuration
     */
    suspend fun start(server: ProxyServer): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isRunning) {
                return@withContext Result.failure(IllegalStateException("sing-box is already running"))
            }
            
            // Copy binary from assets to internal storage (if not exists)
            val binaryFile = prepareBinary()
            
            // Generate configuration
            val config = ProxyConfig.generateXrayConfig(server)
            
            // Save configuration to file
            val configFile = File(context.filesDir, CONFIG_FILE)
            configFile.writeText(config)
            
            Log.d(TAG, "Configuration saved to: ${configFile.absolutePath}")
            
            // Start sing-box process
            val command = arrayOf(
                binaryFile.absolutePath,
                "run",
                "-c", configFile.absolutePath
            )
            
            process = Runtime.getRuntime().exec(command)
            isRunning = true
            
            // Monitor process output in background
            monitorProcess()
            
            Log.d(TAG, "sing-box started successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start sing-box", e)
            isRunning = false
            Result.failure(e)
        }
    }
    
    /**
     * Stop sing-box
     */
    suspend fun stop(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isRunning) {
                return@withContext Result.success(Unit)
            }
            
            process?.destroy()
            process = null
            isRunning = false
            
            // Clean up config file
            val configFile = File(context.filesDir, CONFIG_FILE)
            if (configFile.exists()) {
                configFile.delete()
            }
            
            Log.d(TAG, "sing-box stopped")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop sing-box", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if sing-box is running
     */
    fun isRunning(): Boolean {
        return isRunning && process?.isAlive == true
    }
    
    /**
     * Get current statistics from sing-box
     * 
     * sing-box exposes statistics via:
     * 1. Stats API (if configured in config)
     * 2. Process output parsing
     * 3. Memory-mapped file
     * 
     * For now, we track manually via packet forwarding
     */
    suspend fun getStats(): Map<String, Long> = withContext(Dispatchers.IO) {
        try {
            // TODO: Connect to sing-box stats API
            // sing-box can be configured with stats outbound:
            // {
            //   "type": "direct",
            //   "tag": "stats",
            //   "stats": true
            // }
            //
            // Then query via HTTP API or gRPC
            
            // For now, return placeholder stats
            // Real implementation would query: http://127.0.0.1:6090/stats
            mapOf(
                "uplink" to 0L,
                "downlink" to 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stats", e)
            mapOf(
                "uplink" to 0L,
                "downlink" to 0L
            )
        }
    }
    
    /**
     * Enable stats collection in sing-box
     * This modifies the config to include stats outbound
     */
    fun enableStats(): Boolean {
        // Would need to regenerate config with stats enabled
        return false
    }
    
    /**
     * Prepare sing-box binary for execution
     */
    private fun prepareBinary(): File {
        val binaryFile = File(context.filesDir, BINARY_NAME)
        
        // If binary already exists and is executable, use it
        if (binaryFile.exists() && binaryFile.canExecute()) {
            Log.d(TAG, "Using existing binary: ${binaryFile.absolutePath}")
            return binaryFile
        }
        
        // Copy from assets
        try {
            context.assets.open(BINARY_NAME).use { input ->
                FileOutputStream(binaryFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Make executable
            binaryFile.setExecutable(true, false)
            
            Log.d(TAG, "Binary copied and made executable: ${binaryFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare binary", e)
            throw e
        }
        
        return binaryFile
    }
    
    /**
     * Monitor process output for debugging
     */
    private fun monitorProcess() {
        Thread {
            try {
                process?.inputStream?.bufferedReader()?.use { reader ->
                    reader.lineSequence().forEach { line ->
                        Log.d(TAG, "sing-box: $line")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading process output", e)
            }
        }.start()
        
        Thread {
            try {
                process?.errorStream?.bufferedReader()?.use { reader ->
                    reader.lineSequence().forEach { line ->
                        Log.e(TAG, "sing-box error: $line")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading process error stream", e)
            }
        }.start()
    }
}

