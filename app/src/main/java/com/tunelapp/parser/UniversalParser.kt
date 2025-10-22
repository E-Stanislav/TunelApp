package com.tunelapp.parser

import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer

/**
 * Universal parser that detects protocol and delegates to appropriate parser
 */
object UniversalParser {
    
    /**
     * Parse any supported proxy URL
     */
    fun parse(url: String): Result<ProxyServer> {
        return try {
            val trimmedUrl = url.trim()
            
            when {
                trimmedUrl.startsWith("vless://") -> {
                    VlessParser.parse(trimmedUrl)
                }
                trimmedUrl.startsWith("vmess://") -> {
                    VMessParser.parse(trimmedUrl)
                }
                trimmedUrl.startsWith("ss://") -> {
                    ShadowsocksParser.parse(trimmedUrl)
                }
                trimmedUrl.startsWith("trojan://") -> {
                    TrojanParser.parse(trimmedUrl)
                }
                trimmedUrl.startsWith("socks://") || trimmedUrl.startsWith("socks5://") -> {
                    SOCKSParser.parse(trimmedUrl)
                }
                trimmedUrl.startsWith("http://") || trimmedUrl.startsWith("https://") -> {
                    // Check if it's a proxy URL or subscription URL
                    if (trimmedUrl.contains("@")) {
                        HTTPParser.parse(trimmedUrl)
                    } else {
                        Result.failure(IllegalArgumentException("HTTP/HTTPS subscription URLs should use SubscriptionParser"))
                    }
                }
                else -> {
                    Result.failure(IllegalArgumentException("Unsupported protocol: ${trimmedUrl.take(20)}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Convert server to URL string
     */
    fun toUrl(server: ProxyServer): String {
        return when (server.protocol) {
            ProxyProtocol.VLESS -> VlessParser.toUrl(server)
            ProxyProtocol.VMESS -> VMessParser.toUrl(server)
            ProxyProtocol.SHADOWSOCKS -> ShadowsocksParser.toUrl(server)
            ProxyProtocol.TROJAN -> TrojanParser.toUrl(server)
            ProxyProtocol.SOCKS -> SOCKSParser.toUrl(server)
            ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> HTTPParser.toUrl(server)
            else -> throw IllegalArgumentException("Unsupported protocol: ${server.protocol}")
        }
    }
    
    /**
     * Validate server configuration
     */
    fun validate(server: ProxyServer): Result<Unit> {
        return when (server.protocol) {
            ProxyProtocol.VLESS -> VlessParser.validate(server)
            ProxyProtocol.VMESS -> VMessParser.validate(server)
            ProxyProtocol.SHADOWSOCKS -> ShadowsocksParser.validate(server)
            ProxyProtocol.TROJAN -> TrojanParser.validate(server)
            ProxyProtocol.SOCKS -> SOCKSParser.validate(server)
            ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> HTTPParser.validate(server)
            else -> Result.failure(IllegalArgumentException("Unsupported protocol: ${server.protocol}"))
        }
    }
    
    /**
     * Detect protocol from URL
     */
    fun detectProtocol(url: String): ProxyProtocol? {
        val trimmedUrl = url.trim().lowercase()
        return when {
            trimmedUrl.startsWith("vless://") -> ProxyProtocol.VLESS
            trimmedUrl.startsWith("vmess://") -> ProxyProtocol.VMESS
            trimmedUrl.startsWith("ss://") -> ProxyProtocol.SHADOWSOCKS
            trimmedUrl.startsWith("trojan://") -> ProxyProtocol.TROJAN
            trimmedUrl.startsWith("socks://") || trimmedUrl.startsWith("socks5://") -> ProxyProtocol.SOCKS
            trimmedUrl.startsWith("http://") -> ProxyProtocol.HTTP
            trimmedUrl.startsWith("https://") -> ProxyProtocol.HTTPS
            else -> null
        }
    }
}

/**
 * Simple SOCKS parser
 * Format: socks5://username:password@host:port or socks5://host:port
 */
object SOCKSParser {
    fun parse(url: String): Result<ProxyServer> {
        return try {
            val protocol = when {
                url.startsWith("socks5://") -> "socks5://"
                url.startsWith("socks://") -> "socks://"
                else -> return Result.failure(IllegalArgumentException("Invalid SOCKS URL"))
            }
            
            val withoutScheme = url.substring(protocol.length)
            
            var username: String? = null
            var password: String? = null
            var hostPort = withoutScheme
            
            // Check for username:password@
            if (withoutScheme.contains("@")) {
                val atIndex = withoutScheme.lastIndexOf('@')
                val auth = withoutScheme.substring(0, atIndex)
                hostPort = withoutScheme.substring(atIndex + 1)
                
                val colonIndex = auth.indexOf(':')
                if (colonIndex != -1) {
                    username = auth.substring(0, colonIndex)
                    password = auth.substring(colonIndex + 1)
                }
            }
            
            val colonIndex = hostPort.lastIndexOf(':')
            if (colonIndex == -1) {
                return Result.failure(IllegalArgumentException("Invalid host:port"))
            }
            
            val host = hostPort.substring(0, colonIndex)
            val port = hostPort.substring(colonIndex + 1).toIntOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid port"))
            
            val server = ProxyServer(
                name = "SOCKS5 Server",
                protocol = ProxyProtocol.SOCKS,
                address = host,
                port = port,
                username = username,
                password = password
            )
            
            Result.success(server)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun toUrl(server: ProxyServer): String {
        require(server.protocol == ProxyProtocol.SOCKS) { "Server must be SOCKS" }
        
        val auth = if (server.username != null && server.password != null) {
            "${server.username}:${server.password}@"
        } else {
            ""
        }
        
        return "socks5://$auth${server.address}:${server.port}"
    }
    
    fun validate(server: ProxyServer): Result<Unit> {
        return try {
            require(server.protocol == ProxyProtocol.SOCKS) { "Protocol must be SOCKS" }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Simple HTTP/HTTPS parser
 * Format: http://username:password@host:port or http://host:port
 */
object HTTPParser {
    fun parse(url: String): Result<ProxyServer> {
        return try {
            val isHttps = url.startsWith("https://")
            val protocol = if (isHttps) "https://" else "http://"
            
            val withoutScheme = url.substring(protocol.length)
            
            var username: String? = null
            var password: String? = null
            var hostPort = withoutScheme
            
            // Check for username:password@
            if (withoutScheme.contains("@")) {
                val atIndex = withoutScheme.lastIndexOf('@')
                val auth = withoutScheme.substring(0, atIndex)
                hostPort = withoutScheme.substring(atIndex + 1)
                
                val colonIndex = auth.indexOf(':')
                if (colonIndex != -1) {
                    username = auth.substring(0, colonIndex)
                    password = auth.substring(colonIndex + 1)
                }
            }
            
            val colonIndex = hostPort.lastIndexOf(':')
            val (host, port) = if (colonIndex != -1) {
                hostPort.substring(0, colonIndex) to hostPort.substring(colonIndex + 1).toIntOrNull()
            } else {
                hostPort to if (isHttps) 443 else 80
            }
            
            if (port == null || port !in 1..65535) {
                return Result.failure(IllegalArgumentException("Invalid port"))
            }
            
            val server = ProxyServer(
                name = if (isHttps) "HTTPS Proxy" else "HTTP Proxy",
                protocol = if (isHttps) ProxyProtocol.HTTPS else ProxyProtocol.HTTP,
                address = host,
                port = port,
                username = username,
                password = password
            )
            
            Result.success(server)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun toUrl(server: ProxyServer): String {
        require(server.protocol == ProxyProtocol.HTTP || server.protocol == ProxyProtocol.HTTPS) {
            "Server must be HTTP or HTTPS"
        }
        
        val scheme = if (server.protocol == ProxyProtocol.HTTPS) "https://" else "http://"
        val auth = if (server.username != null && server.password != null) {
            "${server.username}:${server.password}@"
        } else {
            ""
        }
        
        return "$scheme$auth${server.address}:${server.port}"
    }
    
    fun validate(server: ProxyServer): Result<Unit> {
        return try {
            require(server.protocol == ProxyProtocol.HTTP || server.protocol == ProxyProtocol.HTTPS) {
                "Protocol must be HTTP or HTTPS"
            }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

