package com.tunelapp.core

import com.google.gson.Gson
import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer

/**
 * Universal proxy configuration generator for Xray/sing-box
 * Supports all protocols: VLESS, VMess, Shadowsocks, Trojan, SOCKS, HTTP
 */
object ProxyConfig {
    
    /**
     * Generate Xray configuration JSON for any protocol
     */
    fun generateXrayConfig(server: ProxyServer, socksPort: Int = 10808, httpPort: Int = 10809): String {
        val config = XrayConfiguration(
            log = LogConfig(loglevel = "warning"),
            inbounds = createInbounds(socksPort, httpPort),
            outbounds = createOutbounds(server),
            routing = createRouting()
        )
        
        return Gson().toJson(config)
    }
    
    private fun createInbounds(socksPort: Int, httpPort: Int): List<Inbound> {
        return listOf(
            Inbound(
                tag = "socks-in",
                port = socksPort,
                listen = "127.0.0.1",
                protocol = "socks",
                settings = mapOf(
                    "auth" to "noauth",
                    "udp" to true,
                    "ip" to "127.0.0.1"
                )
            ),
            Inbound(
                tag = "http-in",
                port = httpPort,
                listen = "127.0.0.1",
                protocol = "http",
                settings = mapOf<String, Any>()
            )
        )
    }
    
    private fun createOutbounds(server: ProxyServer): List<Outbound> {
        val proxyOutbound = when (server.protocol) {
            ProxyProtocol.VLESS -> createVlessOutbound(server)
            ProxyProtocol.VMESS -> createVMessOutbound(server)
            ProxyProtocol.SHADOWSOCKS -> createShadowsocksOutbound(server)
            ProxyProtocol.TROJAN -> createTrojanOutbound(server)
            ProxyProtocol.SOCKS -> createSOCKSOutbound(server)
            ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> createHTTPOutbound(server)
            else -> createDirectOutbound()
        }
        
        return listOf(
            proxyOutbound,
            createDirectOutbound(),
            createBlockOutbound()
        )
    }
    
    private fun createVlessOutbound(server: ProxyServer): Outbound {
        require(server.uuid != null) { "UUID is required for VLESS" }
        
        return Outbound(
            tag = "proxy",
            protocol = "vless",
            settings = mapOf(
                "vnext" to listOf(
                    mapOf(
                        "address" to server.address,
                        "port" to server.port,
                        "users" to listOf(
                            mapOf(
                                "id" to server.uuid,
                                "encryption" to (server.encryption ?: "none"),
                                "flow" to server.flow
                            ).filterValues { it != null }
                        )
                    )
                )
            ),
            streamSettings = createStreamSettings(server),
            mux = mapOf("enabled" to false)
        )
    }
    
    private fun createVMessOutbound(server: ProxyServer): Outbound {
        require(server.uuid != null) { "UUID is required for VMess" }
        
        return Outbound(
            tag = "proxy",
            protocol = "vmess",
            settings = mapOf(
                "vnext" to listOf(
                    mapOf(
                        "address" to server.address,
                        "port" to server.port,
                        "users" to listOf(
                            mapOf(
                                "id" to server.uuid,
                                "alterId" to (server.alterId ?: 0),
                                "security" to (server.encryption ?: "auto")
                            )
                        )
                    )
                )
            ),
            streamSettings = createStreamSettings(server),
            mux = mapOf("enabled" to false)
        )
    }
    
    private fun createShadowsocksOutbound(server: ProxyServer): Outbound {
        require(server.password != null) { "Password is required for Shadowsocks" }
        require(server.method != null) { "Method is required for Shadowsocks" }
        
        return Outbound(
            tag = "proxy",
            protocol = "shadowsocks",
            settings = mapOf(
                "servers" to listOf(
                    mapOf(
                        "address" to server.address,
                        "port" to server.port,
                        "method" to server.method,
                        "password" to server.password
                    )
                )
            ),
            streamSettings = null, // Shadowsocks doesn't use stream settings
            mux = null
        )
    }
    
    private fun createTrojanOutbound(server: ProxyServer): Outbound {
        require(server.password != null) { "Password is required for Trojan" }
        
        return Outbound(
            tag = "proxy",
            protocol = "trojan",
            settings = mapOf(
                "servers" to listOf(
                    mapOf(
                        "address" to server.address,
                        "port" to server.port,
                        "password" to server.password
                    )
                )
            ),
            streamSettings = createStreamSettings(server),
            mux = mapOf("enabled" to false)
        )
    }
    
    private fun createSOCKSOutbound(server: ProxyServer): Outbound {
        val serverConfig = mutableMapOf<String, Any>(
            "address" to server.address,
            "port" to server.port
        )
        
        // Add auth if provided
        if (server.username != null && server.password != null) {
            val users = listOf(
                mapOf<String, Any>(
                    "user" to server.username,
                    "pass" to server.password
                )
            )
            serverConfig["users"] = users
        }
        
        return Outbound(
            tag = "proxy",
            protocol = "socks",
            settings = mapOf("servers" to listOf(serverConfig)),
            streamSettings = null,
            mux = null
        )
    }
    
    private fun createHTTPOutbound(server: ProxyServer): Outbound {
        val serverConfig = mutableMapOf<String, Any>(
            "address" to server.address,
            "port" to server.port
        )
        
        // Add auth if provided
        if (server.username != null && server.password != null) {
            val users = listOf(
                mapOf<String, Any>(
                    "user" to server.username,
                    "pass" to server.password
                )
            )
            serverConfig["users"] = users
        }
        
        return Outbound(
            tag = "proxy",
            protocol = "http",
            settings = mapOf("servers" to listOf(serverConfig)),
            streamSettings = null,
            mux = null
        )
    }
    
    private fun createDirectOutbound(): Outbound {
        return Outbound(
            tag = "direct",
            protocol = "freedom",
            settings = mapOf<String, Any>(),
            streamSettings = null,
            mux = null
        )
    }
    
    private fun createBlockOutbound(): Outbound {
        return Outbound(
            tag = "block",
            protocol = "blackhole",
            settings = mapOf<String, Any>(),
            streamSettings = null,
            mux = null
        )
    }
    
    private fun createStreamSettings(server: ProxyServer): Map<String, Any>? {
        if (server.network == "tcp" && server.security == "none") {
            return null // No special stream settings needed
        }
        
        val settings = mutableMapOf<String, Any>(
            "network" to server.network
        )
        
        // Add security settings (TLS/Reality)
        if (server.security != "none") {
            settings["security"] = server.security
            
            if (server.security == "tls") {
                val tlsSettings = mutableMapOf<String, Any>()
                
                server.sni?.let { tlsSettings["serverName"] = it }
                tlsSettings["allowInsecure"] = server.allowInsecure
                server.fingerprint?.let { tlsSettings["fingerprint"] = it }
                server.alpn?.let { tlsSettings["alpn"] = it.split(",").map { s -> s.trim() } }
                
                settings["tlsSettings"] = tlsSettings
            } else if (server.security == "reality") {
                val realitySettings = mutableMapOf<String, Any>()
                
                server.sni?.let { realitySettings["serverName"] = it }
                server.fingerprint?.let { realitySettings["fingerprint"] = it }
                server.publicKey?.let { realitySettings["publicKey"] = it }
                server.shortId?.let { realitySettings["shortId"] = it }
                server.spiderX?.let { realitySettings["spiderX"] = it }
                
                settings["realitySettings"] = realitySettings
            }
        }
        
        // Add network-specific settings
        when (server.network) {
            "ws" -> {
                val wsSettings = mutableMapOf<String, Any>()
                server.path?.let { wsSettings["path"] = it }
                server.host?.let {
                    wsSettings["headers"] = mapOf("Host" to it)
                }
                settings["wsSettings"] = wsSettings
            }
            "grpc" -> {
                val grpcSettings = mutableMapOf<String, Any>()
                grpcSettings["serviceName"] = server.serviceName ?: ""
                server.mode?.let { grpcSettings["multiMode"] = (it == "multi") }
                settings["grpcSettings"] = grpcSettings as Any
            }
            "http", "h2" -> {
                val httpSettings = mutableMapOf<String, Any>()
                server.httpPath?.let { httpSettings["path"] = it }
                server.httpHost?.let { httpSettings["host"] = it.split(",").map { s -> s.trim() } as Any }
                settings["httpSettings"] = httpSettings as Any
            }
            "quic" -> {
                val quicSettings = mutableMapOf<String, Any>()
                quicSettings["security"] = server.quicSecurity ?: "none"
                server.key?.let { quicSettings["key"] = it }
                val header = mutableMapOf<String, Any>()
                header["type"] = server.headerType ?: "none"
                quicSettings["header"] = header
                settings["quicSettings"] = quicSettings
            }
        }
        
        return settings
    }
    
    private fun createRouting(): Map<String, Any> {
        return mapOf(
            "domainStrategy" to "IPIfNonMatch",
            "rules" to listOf(
                mapOf(
                    "type" to "field",
                    "ip" to listOf("geoip:private"),
                    "outboundTag" to "direct"
                )
            )
        )
    }
}

// Simplified configuration data classes using generic Maps
data class XrayConfiguration(
    val log: LogConfig,
    val inbounds: List<Inbound>,
    val outbounds: List<Outbound>,
    val routing: Map<String, Any>
)

data class LogConfig(
    val loglevel: String
)

data class Inbound(
    val tag: String,
    val port: Int,
    val listen: String,
    val protocol: String,
    val settings: Map<String, Any>
)

data class Outbound(
    val tag: String,
    val protocol: String,
    val settings: Map<String, Any>,
    val streamSettings: Map<String, Any>? = null,
    val mux: Map<String, Any>? = null
)

