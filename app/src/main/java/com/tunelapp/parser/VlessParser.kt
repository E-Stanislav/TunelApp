package com.tunelapp.parser

import android.net.Uri
import android.util.Base64
import com.tunelapp.data.VlessServer
import java.net.URLDecoder

/**
 * Parser for VLESS URL format
 * Format: vless://uuid@address:port?param1=value1&param2=value2#name
 */
object VlessParser {
    
    /**
     * Parse VLESS URL string
     */
    fun parse(url: String): Result<VlessServer> {
        return try {
            if (!url.startsWith("vless://")) {
                return Result.failure(IllegalArgumentException("URL must start with vless://"))
            }
            
            // Remove vless:// prefix
            val withoutScheme = url.substring(8)
            
            // Split by # to get name/remarks
            val parts = withoutScheme.split("#")
            val mainPart = parts[0]
            val name = if (parts.size > 1) {
                URLDecoder.decode(parts[1], "UTF-8")
            } else {
                "Imported Server"
            }
            
            // Split by @ to get uuid and server part
            val atIndex = mainPart.indexOf('@')
            if (atIndex == -1) {
                return Result.failure(IllegalArgumentException("Invalid format: missing @"))
            }
            
            val uuid = mainPart.substring(0, atIndex)
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
                    val keyValue = param.split('=')
                    if (keyValue.size == 2) {
                        params[keyValue[0]] = URLDecoder.decode(keyValue[1], "UTF-8")
                    }
                }
            }
            
            // Build VlessServer object
            val server = VlessServer(
                name = name,
                uuid = uuid,
                address = address,
                port = port,
                encryption = params["encryption"] ?: "none",
                flow = params["flow"],
                network = params["type"] ?: "tcp",
                security = params["security"] ?: "none",
                sni = params["sni"],
                fingerprint = params["fp"],
                alpn = params["alpn"],
                allowInsecure = params["allowInsecure"] == "1",
                path = params["path"],
                host = params["host"],
                serviceName = params["serviceName"],
                quicSecurity = params["quicSecurity"],
                key = params["key"],
                headerType = params["headerType"],
                remarks = name
            )
            
            Result.success(server)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Convert VlessServer back to URL string
     */
    fun toUrl(server: VlessServer): String {
        val params = mutableListOf<String>()
        
        if (server.encryption != "none") {
            params.add("encryption=${server.encryption}")
        }
        server.flow?.let { params.add("flow=$it") }
        if (server.network != "tcp") {
            params.add("type=${server.network}")
        }
        if (server.security != "none") {
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
        server.quicSecurity?.let { params.add("quicSecurity=$it") }
        server.key?.let { params.add("key=$it") }
        server.headerType?.let { params.add("headerType=$it") }
        
        val query = if (params.isNotEmpty()) "?${params.joinToString("&")}" else ""
        val name = Uri.encode(server.name)
        
        return "vless://${server.uuid}@${server.address}:${server.port}$query#$name"
    }
    
    /**
     * Validate VLESS server configuration
     */
    fun validate(server: VlessServer): Result<Unit> {
        return try {
            require(server.uuid.isNotEmpty()) { "UUID is required" }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            require(server.name.isNotEmpty()) { "Name is required" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}



