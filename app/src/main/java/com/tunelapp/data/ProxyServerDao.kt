package com.tunelapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ProxyServer
 */
@Dao
interface ProxyServerDao {
    
    @Query("SELECT * FROM proxy_servers ORDER BY createdAt DESC")
    fun getAllServers(): Flow<List<ProxyServer>>
    
    @Query("SELECT * FROM proxy_servers WHERE id = :serverId")
    suspend fun getServerById(serverId: Long): ProxyServer?
    
    @Query("SELECT * FROM proxy_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): ProxyServer?
    
    @Query("SELECT * FROM proxy_servers WHERE protocol = :protocol ORDER BY createdAt DESC")
    fun getServersByProtocol(protocol: ProxyProtocol): Flow<List<ProxyServer>>
    
    @Query("SELECT * FROM proxy_servers WHERE isFavorite = 1 ORDER BY lastUsed DESC")
    fun getFavoriteServers(): Flow<List<ProxyServer>>
    
    @Query("SELECT * FROM proxy_servers WHERE subscriptionId = :subscriptionId ORDER BY createdAt DESC")
    fun getServersBySubscription(subscriptionId: Long): Flow<List<ProxyServer>>
    
    @Query("SELECT * FROM proxy_servers WHERE groupName = :groupName ORDER BY createdAt DESC")
    fun getServersByGroup(groupName: String): Flow<List<ProxyServer>>
    
    @Query("SELECT * FROM proxy_servers WHERE name LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%'")
    fun searchServers(query: String): Flow<List<ProxyServer>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ProxyServer): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ProxyServer>): List<Long>
    
    @Update
    suspend fun updateServer(server: ProxyServer)
    
    @Delete
    suspend fun deleteServer(server: ProxyServer)
    
    @Query("DELETE FROM proxy_servers WHERE id = :serverId")
    suspend fun deleteServerById(serverId: Long)
    
    @Query("DELETE FROM proxy_servers WHERE subscriptionId = :subscriptionId")
    suspend fun deleteServersBySubscription(subscriptionId: Long)
    
    @Query("DELETE FROM proxy_servers")
    suspend fun deleteAllServers()
    
    @Query("UPDATE proxy_servers SET isActive = 0")
    suspend fun deactivateAllServers()
    
    @Query("UPDATE proxy_servers SET isActive = 1 WHERE id = :serverId")
    suspend fun setActiveServer(serverId: Long)
    
    @Query("UPDATE proxy_servers SET lastUsed = :timestamp WHERE id = :serverId")
    suspend fun updateLastUsed(serverId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE proxy_servers SET testLatency = :latency, testSpeed = :speed, testTime = :testTime WHERE id = :serverId")
    suspend fun updateTestResult(serverId: Long, latency: Long?, speed: Long?, testTime: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM proxy_servers")
    suspend fun getServerCount(): Int
    
    @Query("SELECT COUNT(*) FROM proxy_servers WHERE subscriptionId = :subscriptionId")
    suspend fun getServerCountBySubscription(subscriptionId: Long): Int
}

