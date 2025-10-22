package com.tunelapp.parser

import android.net.Uri
import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer
import java.net.URLDecoder

/**
 * Parser for Trojan URL format
 * Format: trojan://password@host:port?param1=value1#name
 */
object TrojanParser {
    
    fun parse(url: String): Result<ProxyServer> {
        return try {
            if (!url.startsWith("trojan://")) {
                return Result.failure(IllegalArgumentException("URL must start with trojan://"))
            }
            
            // Remove trojan:// prefix
            val withoutScheme = url.substring(9)
            
            // Split by # to get name
            val parts = withoutScheme.split("#")
            val mainPart = parts[0]
            val name = if (parts.size > 1) {
                URLDecoder.decode(parts[1], "UTF-8")
            } else {
                "Trojan Server"
            }
            
            // Split by @ to get password and server part
            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) {
                return Result.failure(IllegalArgumentException("Invalid format: missing @"))
            }
            
            val password = mainPart.substring(0, atIndex)
            val serverPart = mainPart.substring(atIndex + 1)
            
            // Split by ? to get address:port and parameters
            val questionIndex = serverPart.indexOf('?')
            val addressPort = if (questionIndex != -1) {
                serverPart.substring(0, questionIndex)
            } else {
                serverPart
            }
            
            // Parse address and port
            val colonIndex = addressPort.lastIndexOf(':')
            if (colonIndex == -1) {
                return Result.failure(IllegalArgumentException("Invalid format: missing port"))
            }
            
            val address = addressPort.substring(0, colonIndex)
            val port = addressPort.substring(colonIndex + 1).toIntOrNull()
                ?: return Result.failure(IllegalArgumentException("Invalid port"))
            
            // Parse query parameters
            val params = mutableMapOf<String, String>()
            if (questionIndex != -1) {
                val query = serverPart.substring(questionIndex + 1)
                query.split('&').forEach { param ->
                    val keyValue = param.split('=', limit = 2)
                    if (keyValue.size == 2) {
                        params[keyValue[0]] = URLDecoder.decode(keyValue[1], "UTF-8")
                    }
                }
            }
            
            // Build ProxyServer object
            val server = ProxyServer(
                name = name,
                protocol = ProxyProtocol.TROJAN,
                address = address,
                port = port,
                password = password,
                network = params["type"] ?: "tcp",
                security = params["security"] ?: "tls",
                sni = params["sni"],
                fingerprint = params["fp"],
                alpn = params["alpn"],
                allowInsecure = params["allowInsecure"] == "1",
                path = params["path"],
                host = params["host"],
                serviceName = params["serviceName"],
                headerType = params["headerType"],
                remarks = name
            )
            
            Result.success(server)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun toUrl(server: ProxyServer): String {
        require(server.protocol == ProxyProtocol.TROJAN) { "Server must be Trojan" }
        require(server.password != null) { "Password is required" }
        
        val params = mutableListOf<String>()
        
        if (server.network != "tcp") {
            params.add("type=${server.network}")
        }
        if (server.security != "tls") {
            params.add("security=${server.security}")
        }
        server.sni?.let { params.add("sni=$it") }
        server.fingerprint?.let { params.add("fp=$it") }
        server.alpn?.let { params.add("alpn=$it") }
        if (server.allowInsecure) {
            params.add("allowInsecure=1")
        }
        server.path?.let { params.add("path=$it") }
        server.host?.let { params.add("host=$it") }
        server.serviceName?.let { params.add("serviceName=$it") }
        server.headerType?.let { params.add("headerType=$it") }
        
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val name = Uri.encode(server.name)
        
        return "trojan://${server.password}@${server.address}:${server.port}$query#$name"
    }
    
    fun validate(server: ProxyServer): Result<Unit> {
        return try {
            require(server.protocol == ProxyProtocol.TROJAN) { "Protocol must be Trojan" }
            require(server.password != null && server.password.isNotEmpty()) { "Password is required" }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

