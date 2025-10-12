package com.tunelapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * VLESS Server configuration model
 */
@Entity(tableName = "vless_servers")
data class VlessServer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic info
    val name: String,
    val uuid: String,
    val address: String,
    val port: Int,
    
    // Connection settings
    val encryption: String = "none",
    val flow: String? = null,
    
    // Transport settings
    val network: String = "tcp", // tcp, ws, grpc, http, quic
    val security: String = "none", // none, tls, reality
    
    // TLS settings
    val sni: String? = null,
    val fingerprint: String? = null,
    val alpn: String? = null,
    val allowInsecure: Boolean = false,
    
    // WebSocket settings
    val path: String? = null,
    val host: String? = null,
    
    // gRPC settings
    val serviceName: String? = null,
    
    // QUIC settings
    val quicSecurity: String? = null,
    val key: String? = null,
    val headerType: String? = null,
    
    // Additional
    val remarks: String? = null,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null
)

/**
 * Connection state
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

/**
 * Traffic statistics
 */
data class TrafficStats(
    val uploadSpeed: Long = 0,      // bytes per second
    val downloadSpeed: Long = 0,    // bytes per second
    val totalUpload: Long = 0,      // total bytes
    val totalDownload: Long = 0,    // total bytes
    val connectedTime: Long = 0     // milliseconds
)

/**
 * VPN state with server and stats
 */
data class VpnState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val server: VlessServer? = null,
    val stats: TrafficStats = TrafficStats(),
    val errorMessage: String? = null
)

