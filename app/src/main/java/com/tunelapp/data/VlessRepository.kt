package com.tunelapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing VLESS servers
 */
class VlessRepository(private val dao: VlessServerDao) {
    
    val allServers: Flow<List<VlessServer>> = dao.getAllServers()
    
    suspend fun getServerById(id: Long): VlessServer? {
        return dao.getServerById(id)
    }
    
    suspend fun getActiveServer(): VlessServer? {
        return dao.getActiveServer()
    }
    
    suspend fun insertServer(server: VlessServer): Long {
        return dao.insertServer(server)
    }
    
    suspend fun updateServer(server: VlessServer) {
        dao.updateServer(server)
    }
    
    suspend fun deleteServer(server: VlessServer) {
        dao.deleteServer(server)
    }
    
    suspend fun deleteServerById(id: Long) {
        dao.deleteServerById(id)
    }
    
    suspend fun setActiveServer(id: Long) {
        dao.deactivateAllServers()
        dao.setActiveServer(id)
        dao.updateLastUsed(id, System.currentTimeMillis())
    }
    
    suspend fun deleteAllServers() {
        dao.deleteAllServers()
    }
}






