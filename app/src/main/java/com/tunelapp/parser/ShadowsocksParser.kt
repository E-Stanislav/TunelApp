package com.tunelapp.parser

import android.net.Uri
import android.util.Base64
import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer
import java.net.URLDecoder

/**
 * Parser for Shadowsocks URL formats
 * Supports:
 * - ss://base64(method:password@host:port)#name
 * - ss://base64(method:password)@host:port#name (SIP002)
 */
object ShadowsocksParser {
    
    fun parse(url: String): Result<ProxyServer> {
        return try {
            if (!url.startsWith("ss://")) {
                return Result.failure(IllegalArgumentException("URL must start with ss://"))
            }
            
            // Remove ss:// prefix
            val withoutScheme = url.substring(5)
            
            // Split by # to get name
            val parts = withoutScheme.split("#")
            val mainPart = parts[0]
            val name = if (parts.size > 1) {
                URLDecoder.decode(parts[1], "UTF-8")
            } else {
                "Shadowsocks Server"
            }
            
            // Try SIP002 format first (method:password@host:port)
            if (mainPart.contains("@")) {
                parseSIP002(mainPart, name)
            } else {
                // Legacy format (entire string is base64)
                parseLegacy(mainPart, name)
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseSIP002(mainPart: String, name: String): Result<ProxyServer> {
        // Format: base64(method:password)@host:port?plugin=xxx
        val atIndex = mainPart.lastIndexOf('@')
        if (atIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid SIP002 format"))
        }
        
        // Decode method:password
        val encodedAuth = mainPart.substring(0, atIndex)
        val decodedAuth = String(Base64.decode(encodedAuth, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        
        val colonIndex = decodedAuth.indexOf(':')
        if (colonIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid auth format"))
        }
        
        val method = decodedAuth.substring(0, colonIndex)
        val password = decodedAuth.substring(colonIndex + 1)
        
        // Parse host:port?plugin
        val serverPart = mainPart.substring(atIndex + 1)
        val questionIndex = serverPart.indexOf('?')
        
        val hostPort = if (questionIndex != -1) {
            serverPart.substring(0, questionIndex)
        } else {
            serverPart
        }
        
        val portIndex = hostPort.lastIndexOf(':')
        if (portIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid host:port"))
        }
        
        val host = hostPort.substring(0, portIndex)
        val port = hostPort.substring(portIndex + 1).toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid port"))
        
        // Parse plugin parameters
        var plugin: String? = null
        var pluginOpts: String? = null
        
        if (questionIndex != -1) {
            val query = serverPart.substring(questionIndex + 1)
            val params = parseQueryParams(query)
            plugin = params["plugin"]
            
            // Plugin options are in format: plugin;opts
            plugin?.let {
                val semiIndex = it.indexOf(';')
                if (semiIndex != -1) {
                    pluginOpts = it.substring(semiIndex + 1)
                    plugin = it.substring(0, semiIndex)
                }
            }
        }
        
        val server = ProxyServer(
            name = name,
            protocol = ProxyProtocol.SHADOWSOCKS,
            address = host,
            port = port,
            method = method,
            password = password,
            plugin = plugin,
            pluginOpts = pluginOpts,
            remarks = name
        )
        
        return Result.success(server)
    }
    
    private fun parseLegacy(mainPart: String, name: String): Result<ProxyServer> {
        // Format: base64(method:password@host:port)
        val decoded = String(Base64.decode(mainPart, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        
        val atIndex = decoded.lastIndexOf('@')
        if (atIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid legacy format"))
        }
        
        val auth = decoded.substring(0, atIndex)
        val hostPort = decoded.substring(atIndex + 1)
        
        val colonIndex = auth.indexOf(':')
        if (colonIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid auth format"))
        }
        
        val method = auth.substring(0, colonIndex)
        val password = auth.substring(colonIndex + 1)
        
        val portIndex = hostPort.lastIndexOf(':')
        if (portIndex == -1) {
            return Result.failure(IllegalArgumentException("Invalid host:port"))
        }
        
        val host = hostPort.substring(0, portIndex)
        val port = hostPort.substring(portIndex + 1).toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid port"))
        
        val server = ProxyServer(
            name = name,
            protocol = ProxyProtocol.SHADOWSOCKS,
            address = host,
            port = port,
            method = method,
            password = password,
            remarks = name
        )
        
        return Result.success(server)
    }
    
    fun toUrl(server: ProxyServer): String {
        require(server.protocol == ProxyProtocol.SHADOWSOCKS) { "Server must be Shadowsocks" }
        require(server.method != null && server.password != null) { "Method and password required" }
        
        // Use SIP002 format
        val auth = "${server.method}:${server.password}"
        val encodedAuth = Base64.encodeToString(
            auth.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        
        var url = "ss://$encodedAuth@${server.address}:${server.port}"
        
        // Add plugin if present
        if (server.plugin != null) {
            val pluginParam = if (server.pluginOpts != null) {
                "${server.plugin};${server.pluginOpts}"
            } else {
                server.plugin
            }
            url += "?plugin=${Uri.encode(pluginParam)}"
        }
        
        url += "#${Uri.encode(server.name)}"
        
        return url
    }
    
    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        query.split('&').forEach { param ->
            val keyValue = param.split('=', limit = 2)
            if (keyValue.size == 2) {
                params[keyValue[0]] = URLDecoder.decode(keyValue[1], "UTF-8")
            }
        }
        return params
    }
    
    fun validate(server: ProxyServer): Result<Unit> {
        return try {
            require(server.protocol == ProxyProtocol.SHADOWSOCKS) { "Protocol must be Shadowsocks" }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            require(server.method != null && server.method.isNotEmpty()) { "Method is required" }
            require(server.password != null && server.password.isNotEmpty()) { "Password is required" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

