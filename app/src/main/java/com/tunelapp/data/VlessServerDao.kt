package com.tunelapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VLESS servers
 */
@Dao
interface VlessServerDao {
    
    @Query("SELECT * FROM vless_servers ORDER BY lastUsed DESC, createdAt DESC")
    fun getAllServers(): Flow<List<VlessServer>>
    
    @Query("SELECT * FROM vless_servers WHERE id = :id")
    suspend fun getServerById(id: Long): VlessServer?
    
    @Query("SELECT * FROM vless_servers WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveServer(): VlessServer?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: VlessServer): Long
    
    @Update
    suspend fun updateServer(server: VlessServer)
    
    @Delete
    suspend fun deleteServer(server: VlessServer)
    
    @Query("DELETE FROM vless_servers WHERE id = :id")
    suspend fun deleteServerById(id: Long)
    
    @Query("UPDATE vless_servers SET isActive = 0")
    suspend fun deactivateAllServers()
    
    @Query("UPDATE vless_servers SET isActive = 1 WHERE id = :id")
    suspend fun setActiveServer(id: Long)
    
    @Query("UPDATE vless_servers SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)
    
    @Query("DELETE FROM vless_servers")
    suspend fun deleteAllServers()
}






