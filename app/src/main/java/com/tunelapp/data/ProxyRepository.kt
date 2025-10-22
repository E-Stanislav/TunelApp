package com.tunelapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for ProxyServer operations
 */
class ProxyRepository(context: Context) {
    
    private val database = TunelDatabase.getInstance(context)
    private val proxyServerDao = database.proxyServerDao()
    
    // Observe all servers
    fun getAllServers(): Flow<List<ProxyServer>> = proxyServerDao.getAllServers()
    
    // Observe servers by protocol
    fun getServersByProtocol(protocol: ProxyProtocol): Flow<List<ProxyServer>> =
        proxyServerDao.getServersByProtocol(protocol)
    
    // Observe favorite servers
    fun getFavoriteServers(): Flow<List<ProxyServer>> = proxyServerDao.getFavoriteServers()
    
    // Observe servers by subscription
    fun getServersBySubscription(subscriptionId: Long): Flow<List<ProxyServer>> =
        proxyServerDao.getServersBySubscription(subscriptionId)
    
    // Search servers
    fun searchServers(query: String): Flow<List<ProxyServer>> =
        proxyServerDao.searchServers(query)
    
    // Get single server
    suspend fun getServerById(serverId: Long): ProxyServer? =
        proxyServerDao.getServerById(serverId)
    
    // Get active server
    suspend fun getActiveServer(): ProxyServer? =
        proxyServerDao.getActiveServer()
    
    // Insert server
    suspend fun insertServer(server: ProxyServer): Long =
        proxyServerDao.insertServer(server)
    
    // Insert multiple servers
    suspend fun insertServers(servers: List<ProxyServer>): List<Long> =
        proxyServerDao.insertServers(servers)
    
    // Update server
    suspend fun updateServer(server: ProxyServer) =
        proxyServerDao.updateServer(server)
    
    // Delete server
    suspend fun deleteServer(server: ProxyServer) =
        proxyServerDao.deleteServer(server)
    
    // Delete by ID
    suspend fun deleteServerById(serverId: Long) =
        proxyServerDao.deleteServerById(serverId)
    
    // Delete servers by subscription
    suspend fun deleteServersBySubscription(subscriptionId: Long) =
        proxyServerDao.deleteServersBySubscription(subscriptionId)
    
    // Set active server
    suspend fun setActiveServer(serverId: Long) {
        proxyServerDao.deactivateAllServers()
        proxyServerDao.setActiveServer(serverId)
    }
    
    // Update last used
    suspend fun updateLastUsed(serverId: Long) =
        proxyServerDao.updateLastUsed(serverId)
    
    // Update test result
    suspend fun updateTestResult(serverId: Long, latency: Long?, speed: Long?) =
        proxyServerDao.updateTestResult(serverId, latency, speed)
    
    // Get server count
    suspend fun getServerCount(): Int =
        proxyServerDao.getServerCount()
    
    // Toggle favorite
    suspend fun toggleFavorite(server: ProxyServer) {
        val updated = server.copy(isFavorite = !server.isFavorite)
        proxyServerDao.updateServer(updated)
    }
}

