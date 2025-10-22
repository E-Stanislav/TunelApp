package com.tunelapp.parser

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tunelapp.data.ProxyServer
import com.tunelapp.data.SubscriptionType
import java.net.URLDecoder

/**
 * Parser for subscription formats
 * Supports: Base64, Clash, v2rayN, SIP008, sing-box
 */
object SubscriptionParser {
    
    private val gson = Gson()
    
    /**
     * Parse subscription content
     */
    fun parse(content: String, type: SubscriptionType = SubscriptionType.AUTO, subscriptionId: Long? = null): Result<List<ProxyServer>> {
        return try {
            val trimmedContent = content.trim()
            
            val detectedType = if (type == SubscriptionType.AUTO) {
                detectType(trimmedContent)
            } else {
                type
            }
            
            val servers = when (detectedType) {
                SubscriptionType.BASE64 -> parseBase64(trimmedContent)
                SubscriptionType.CLASH -> parseClash(trimmedContent)
                SubscriptionType.V2RAYN -> parseV2rayN(trimmedContent)
                SubscriptionType.SIP008 -> parseSIP008(trimmedContent)
                SubscriptionType.SINGBOX -> parseSingBox(trimmedContent)
                SubscriptionType.QUANTUMULT -> parseQuantumult(trimmedContent)
                else -> parseBase64(trimmedContent) // Fallback
            }
            
            // Add subscription ID to all servers
            val serversWithSubscription = if (subscriptionId != null) {
                servers.map { it.copy(subscriptionId = subscriptionId) }
            } else {
                servers
            }
            
            Result.success(serversWithSubscription)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Detect subscription type from content
     */
    private fun detectType(content: String): SubscriptionType {
        return when {
            // YAML format (Clash)
            content.trimStart().startsWith("port:") ||
            content.trimStart().startsWith("socks-port:") ||
            content.contains("proxies:") -> SubscriptionType.CLASH
            
            // JSON formats
            content.trimStart().startsWith("{") -> {
                try {
                    val json = gson.fromJson(content, JsonObject::class.java)
                    when {
                        json.has("version") && json.has("servers") -> SubscriptionType.SIP008
                        json.has("outbounds") -> SubscriptionType.SINGBOX
                        else -> SubscriptionType.BASE64
                    }
                } catch (e: Exception) {
                    SubscriptionType.BASE64
                }
            }
            
            // Base64 or v2rayN format (lines of URLs)
            else -> SubscriptionType.BASE64
        }
    }
    
    /**
     * Parse Base64 encoded subscription (most common)
     * Format: base64(url1\nurl2\nurl3...)
     */
    private fun parseBase64(content: String): List<ProxyServer> {
        val servers = mutableListOf<ProxyServer>()
        
        try {
            // Try to decode as base64 first
            val decoded = try {
                String(Base64.decode(content, Base64.DEFAULT))
            } catch (e: Exception) {
                // If not base64, treat as plain text
                content
            }
            
            // Split by newlines and parse each URL
            decoded.lines().forEach { line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                    try {
                        val result = UniversalParser.parse(trimmedLine)
                        result.getOrNull()?.let { servers.add(it) }
                    } catch (e: Exception) {
                        // Skip invalid lines
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty list if parsing fails
        }
        
        return servers
    }
    
    /**
     * Parse v2rayN format (similar to Base64 but with specific structure)
     */
    private fun parseV2rayN(content: String): List<ProxyServer> {
        // v2rayN format is essentially Base64, so reuse that parser
        return parseBase64(content)
    }
    
    /**
     * Parse Clash/ClashMeta YAML format
     */
    private fun parseClash(content: String): List<ProxyServer> {
        val servers = mutableListOf<ProxyServer>()
        
        try {
            // Simple YAML parser for proxies section
            val lines = content.lines()
            var inProxiesSection = false
            var currentProxy = mutableMapOf<String, String>()
            
            for (line in lines) {
                val trimmedLine = line.trim()
                
                when {
                    trimmedLine.startsWith("proxies:") -> {
                        inProxiesSection = true
                    }
                    inProxiesSection && trimmedLine.startsWith("- name:") -> {
                        // Save previous proxy if exists
                        if (currentProxy.isNotEmpty()) {
                            parseClashProxy(currentProxy)?.let { servers.add(it) }
                        }
                        // Start new proxy
                        currentProxy = mutableMapOf()
                        currentProxy["name"] = trimmedLine.substringAfter("name:").trim().removeSurrounding("\"", "'")
                    }
                    inProxiesSection && trimmedLine.contains(":") && !trimmedLine.startsWith("-") -> {
                        val key = trimmedLine.substringBefore(":").trim()
                        val value = trimmedLine.substringAfter(":").trim().removeSurrounding("\"", "'")
                        currentProxy[key] = value
                    }
                    inProxiesSection && !trimmedLine.startsWith("-") && !trimmedLine.contains(":") -> {
                        // End of proxies section
                        if (currentProxy.isNotEmpty()) {
                            parseClashProxy(currentProxy)?.let { servers.add(it) }
                        }
                        break
                    }
                }
            }
            
            // Don't forget the last proxy
            if (currentProxy.isNotEmpty()) {
                parseClashProxy(currentProxy)?.let { servers.add(it) }
            }
            
        } catch (e: Exception) {
            // Return what we have so far
        }
        
        return servers
    }
    
    /**
     * Parse single Clash proxy from map
     */
    private fun parseClashProxy(proxy: Map<String, String>): ProxyServer? {
        return try {
            val name = proxy["name"] ?: return null
            val type = proxy["type"]?.lowercase() ?: return null
            val server = proxy["server"] ?: return null
            val port = proxy["port"]?.toIntOrNull() ?: return null
            
            val protocol = when (type) {
                "ss", "shadowsocks" -> com.tunelapp.data.ProxyProtocol.SHADOWSOCKS
                "vmess" -> com.tunelapp.data.ProxyProtocol.VMESS
                "trojan" -> com.tunelapp.data.ProxyProtocol.TROJAN
                "vless" -> com.tunelapp.data.ProxyProtocol.VLESS
                "socks5" -> com.tunelapp.data.ProxyProtocol.SOCKS
                "http", "https" -> com.tunelapp.data.ProxyProtocol.HTTP
                else -> return null
            }
            
            // Build proxy server based on type
            when (protocol) {
                com.tunelapp.data.ProxyProtocol.SHADOWSOCKS -> {
                    ProxyServer(
                        name = name,
                        protocol = protocol,
                        address = server,
                        port = port,
                        password = proxy["password"],
                        method = proxy["cipher"],
                        plugin = proxy["plugin"],
                        pluginOpts = proxy["plugin-opts"]
                    )
                }
                com.tunelapp.data.ProxyProtocol.VMESS -> {
                    ProxyServer(
                        name = name,
                        protocol = protocol,
                        address = server,
                        port = port,
                        uuid = proxy["uuid"],
                        alterId = proxy["alterId"]?.toIntOrNull() ?: 0,
                        encryption = proxy["cipher"] ?: "auto",
                        network = proxy["network"] ?: "tcp",
                        security = if (proxy["tls"] == "true") "tls" else "none",
                        sni = proxy["sni"],
                        path = proxy["ws-path"] ?: proxy["path"],
                        host = proxy["ws-headers"]?.let { parseWsHeaders(it) }?.get("Host")
                    )
                }
                com.tunelapp.data.ProxyProtocol.TROJAN -> {
                    ProxyServer(
                        name = name,
                        protocol = protocol,
                        address = server,
                        port = port,
                        password = proxy["password"],
                        network = proxy["network"] ?: "tcp",
                        security = "tls",
                        sni = proxy["sni"],
                        alpn = proxy["alpn"]
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse WebSocket headers
     */
    private fun parseWsHeaders(headers: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val json = gson.fromJson(headers, JsonObject::class.java)
            json.entrySet().forEach { (key, value) ->
                map[key] = value.asString
            }
        } catch (e: Exception) {
            // Ignore invalid headers
        }
        return map
    }
    
    /**
     * Parse SIP008 format (Shadowsocks)
     * https://shadowsocks.org/doc/sip008.html
     */
    private fun parseSIP008(content: String): List<ProxyServer> {
        val servers = mutableListOf<ProxyServer>()
        
        try {
            val json = gson.fromJson(content, JsonObject::class.java)
            val serversArray = json.getAsJsonArray("servers")
            
            serversArray?.forEach { element ->
                val serverObj = element.asJsonObject
                
                val server = ProxyServer(
                    name = serverObj.get("remarks")?.asString ?: "Shadowsocks",
                    protocol = com.tunelapp.data.ProxyProtocol.SHADOWSOCKS,
                    address = serverObj.get("server")?.asString ?: return@forEach,
                    port = serverObj.get("server_port")?.asInt ?: return@forEach,
                    password = serverObj.get("password")?.asString,
                    method = serverObj.get("method")?.asString,
                    plugin = serverObj.get("plugin")?.asString,
                    pluginOpts = serverObj.get("plugin_opts")?.asString
                )
                
                servers.add(server)
            }
        } catch (e: Exception) {
            // Return empty list on error
        }
        
        return servers
    }
    
    /**
     * Parse sing-box JSON format
     */
    private fun parseSingBox(content: String): List<ProxyServer> {
        val servers = mutableListOf<ProxyServer>()
        
        try {
            val json = gson.fromJson(content, JsonObject::class.java)
            val outbounds = json.getAsJsonArray("outbounds")
            
            outbounds?.forEach { element ->
                val outbound = element.asJsonObject
                val type = outbound.get("type")?.asString ?: return@forEach
                
                // Parse based on outbound type
                // This is a simplified version - full implementation would be more complex
                when (type) {
                    "shadowsocks" -> {
                        val server = ProxyServer(
                            name = outbound.get("tag")?.asString ?: "Shadowsocks",
                            protocol = com.tunelapp.data.ProxyProtocol.SHADOWSOCKS,
                            address = outbound.get("server")?.asString ?: return@forEach,
                            port = outbound.get("server_port")?.asInt ?: return@forEach,
                            password = outbound.get("password")?.asString,
                            method = outbound.get("method")?.asString
                        )
                        servers.add(server)
                    }
                    // Add more types as needed
                }
            }
        } catch (e: Exception) {
            // Return empty list on error
        }
        
        return servers
    }
    
    /**
     * Parse Quantumult(X) format
     */
    private fun parseQuantumult(content: String): List<ProxyServer> {
        // Quantumult format is line-based with specific syntax
        // This is a placeholder - full implementation would parse their specific format
        return parseBase64(content)
    }
}

