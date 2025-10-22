package com.tunelapp.core

import android.util.Log
import com.tunelapp.data.ProxyServer
import com.tunelapp.data.ServerTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.system.measureTimeMillis

/**
 * Utility for testing proxy server speed and latency
 */
class SpeedTester {
    
    companion object {
        private const val TAG = "SpeedTester"
        private const val PING_TIMEOUT = 5000L // 5 seconds
        private const val SPEED_TEST_TIMEOUT = 30000L // 30 seconds
        private const val SPEED_TEST_URL = "https://www.google.com/generate_204"
        private const val SPEED_TEST_SIZE = 1024 * 1024 // 1MB
    }
    
    /**
     * Test server latency (ping)
     */
    suspend fun testLatency(server: ProxyServer): ServerTestResult = withContext(Dispatchers.IO) {
        try {
            val latency = measureTcpPing(server.address, server.port)
            
            ServerTestResult(
                serverId = server.id,
                isReachable = true,
                latency = latency
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test latency for ${server.name}", e)
            ServerTestResult(
                serverId = server.id,
                isReachable = false,
                error = e.message
            )
        }
    }
    
    /**
     * Test server download speed
     */
    suspend fun testSpeed(server: ProxyServer): ServerTestResult = withContext(Dispatchers.IO) {
        try {
            // First check if server is reachable
            val latency = measureTcpPing(server.address, server.port)
            
            // Then test download speed
            // Note: This would require the VPN to be connected through this server
            // For now, we just test connectivity
            val speed = 0L // Placeholder - would need actual VPN connection
            
            ServerTestResult(
                serverId = server.id,
                isReachable = true,
                latency = latency,
                speed = speed
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to test speed for ${server.name}", e)
            ServerTestResult(
                serverId = server.id,
                isReachable = false,
                error = e.message
            )
        }
    }
    
    /**
     * Perform complete server test (latency + speed)
     */
    suspend fun testServer(server: ProxyServer, includeSpeed: Boolean = false): ServerTestResult {
        return if (includeSpeed) {
            testSpeed(server)
        } else {
            testLatency(server)
        }
    }
    
    /**
     * Test multiple servers and return results
     */
    suspend fun testServers(
        servers: List<ProxyServer>,
        includeSpeed: Boolean = false,
        onProgress: ((ProxyServer, ServerTestResult) -> Unit)? = null
    ): List<ServerTestResult> {
        val results = mutableListOf<ServerTestResult>()
        
        servers.forEach { server ->
            val result = testServer(server, includeSpeed)
            results.add(result)
            onProgress?.invoke(server, result)
        }
        
        return results
    }
    
    /**
     * Find the fastest server from a list
     */
    suspend fun findFastestServer(servers: List<ProxyServer>): ProxyServer? {
        if (servers.isEmpty()) return null
        
        val results = testServers(servers, includeSpeed = false)
        
        return results
            .filter { it.isReachable && it.latency != null }
            .minByOrNull { it.latency!! }
            ?.let { result ->
                servers.find { it.id == result.serverId }
            }
    }
    
    /**
     * Measure TCP ping to a server
     */
    private suspend fun measureTcpPing(host: String, port: Int): Long {
        return withTimeout(PING_TIMEOUT) {
            withContext(Dispatchers.IO) {
                val latency = measureTimeMillis {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), PING_TIMEOUT.toInt())
                    }
                }
                latency
            }
        }
    }
    
    /**
     * Perform HTTP GET request to measure download speed
     * Note: This requires the VPN to be active and routing through the server
     */
    private suspend fun measureDownloadSpeed(url: String): Long {
        return withTimeout(SPEED_TEST_TIMEOUT) {
            withContext(Dispatchers.IO) {
                try {
                    // This is a placeholder - actual implementation would use
                    // HttpURLConnection or OkHttp to download data and measure speed
                    // It needs to go through the VPN connection
                    0L
                } catch (e: IOException) {
                    throw e
                }
            }
        }
    }
    
    /**
     * Calculate average latency from multiple pings
     */
    suspend fun measureAverageLatency(
        server: ProxyServer,
        pingCount: Int = 3
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val latencies = mutableListOf<Long>()
            
            repeat(pingCount) {
                try {
                    val latency = measureTcpPing(server.address, server.port)
                    latencies.add(latency)
                } catch (e: Exception) {
                    // Skip failed pings
                }
            }
            
            if (latencies.isEmpty()) null else latencies.average().toLong()
        } catch (e: Exception) {
            null
        }
    }
}

