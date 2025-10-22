package com.tunelapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Universal proxy server configuration model
 * Supports: VLESS, VMess, Shadowsocks, Trojan, SOCKS, HTTP, etc.
 */
@Entity(tableName = "proxy_servers")
data class ProxyServer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic info
    val name: String,
    val protocol: ProxyProtocol,
    val address: String,
    val port: Int,
    
    // Authentication (protocol-specific)
    val uuid: String? = null,           // VLESS, VMess
    val password: String? = null,       // Shadowsocks, Trojan
    val username: String? = null,       // SOCKS, HTTP
    val alterId: Int? = null,           // VMess legacy
    
    // Encryption
    val encryption: String? = "none",   // VLESS, VMess encryption
    val method: String? = null,         // Shadowsocks method (aes-256-gcm, chacha20-poly1305, etc.)
    val flow: String? = null,           // VLESS flow control
    
    // Transport settings
    val network: String = "tcp",        // tcp, ws, grpc, http, quic, h2
    val security: String = "none",      // none, tls, reality
    
    // TLS settings
    val sni: String? = null,
    val fingerprint: String? = null,
    val alpn: String? = null,
    val allowInsecure: Boolean = false,
    val publicKey: String? = null,      // Reality
    val shortId: String? = null,        // Reality
    val spiderX: String? = null,        // Reality
    
    // WebSocket settings
    val path: String? = null,
    val host: String? = null,
    val headers: String? = null,        // JSON string of headers
    
    // gRPC settings
    val serviceName: String? = null,
    val mode: String? = null,           // gun, multi
    
    // HTTP/2 settings
    val httpHost: String? = null,
    val httpPath: String? = null,
    
    // QUIC settings
    val quicSecurity: String? = null,
    val key: String? = null,
    val headerType: String? = null,
    
    // Shadowsocks specific
    val plugin: String? = null,         // obfs, v2ray-plugin, etc.
    val pluginOpts: String? = null,
    
    // Subscription info
    val subscriptionId: Long? = null,
    val subscriptionTag: String? = null,
    
    // UI/UX
    val remarks: String? = null,
    val groupName: String? = null,
    val isFavorite: Boolean = false,
    val isActive: Boolean = false,
    
    // Stats
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long? = null,
    val testLatency: Long? = null,      // milliseconds, null if not tested
    val testSpeed: Long? = null,        // bytes per second
    val testTime: Long? = null          // when last tested
)

/**
 * Supported proxy protocols
 */
enum class ProxyProtocol {
    VLESS,
    VMESS,
    SHADOWSOCKS,
    TROJAN,
    SOCKS,
    HTTP,
    HTTPS,
    WIREGUARD,
    HYSTERIA,
    TUIC,
    SSH;
    
    companion object {
        fun fromString(protocol: String): ProxyProtocol? {
            return when (protocol.lowercase()) {
                "vless" -> VLESS
                "vmess" -> VMESS
                "shadowsocks", "ss" -> SHADOWSOCKS
                "trojan" -> TROJAN
                "socks", "socks5" -> SOCKS
                "http" -> HTTP
                "https" -> HTTPS
                "wireguard", "wg" -> WIREGUARD
                "hysteria", "hy", "hy2" -> HYSTERIA
                "tuic" -> TUIC
                "ssh" -> SSH
                else -> null
            }
        }
    }
}

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
    val server: ProxyServer? = null,
    val stats: TrafficStats = TrafficStats(),
    val errorMessage: String? = null
)

/**
 * Server test result
 */
data class ServerTestResult(
    val serverId: Long,
    val isReachable: Boolean,
    val latency: Long? = null,      // milliseconds
    val speed: Long? = null,        // bytes per second
    val error: String? = null,
    val testedAt: Long = System.currentTimeMillis()
)

