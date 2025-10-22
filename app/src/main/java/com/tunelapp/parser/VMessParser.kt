package com.tunelapp.parser

import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer
import java.net.URLDecoder

/**
 * Parser for VMess URL format
 * Format: vmess://base64(json)
 */
object VMessParser {
    
    private val gson = Gson()
    
    fun parse(url: String): Result<ProxyServer> {
        return try {
            if (!url.startsWith("vmess://")) {
                return Result.failure(IllegalArgumentException("URL must start with vmess://"))
            }
            
            // Remove vmess:// prefix
            val withoutScheme = url.substring(8)
            
            // Decode base64
            val decoded = String(Base64.decode(withoutScheme, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
            
            // Parse JSON
            val json = gson.fromJson(decoded, JsonObject::class.java)
            
            val name = json.get("ps")?.asString ?: "VMess Server"
            val address = json.get("add")?.asString
                ?: return Result.failure(IllegalArgumentException("Missing address"))
            val port = json.get("port")?.let {
                when {
                    it.isJsonPrimitive && it.asJsonPrimitive.isNumber -> it.asInt
                    it.isJsonPrimitive && it.asJsonPrimitive.isString -> it.asString.toIntOrNull()
                    else -> null
                }
            } ?: return Result.failure(IllegalArgumentException("Missing or invalid port"))
            
            val uuid = json.get("id")?.asString
                ?: return Result.failure(IllegalArgumentException("Missing UUID"))
            
            val alterId = json.get("aid")?.let {
                when {
                    it.isJsonPrimitive && it.asJsonPrimitive.isNumber -> it.asInt
                    it.isJsonPrimitive && it.asJsonPrimitive.isString -> it.asString.toIntOrNull() ?: 0
                    else -> 0
                }
            } ?: 0
            
            val network = json.get("net")?.asString ?: "tcp"
            val security = json.get("tls")?.asString?.let {
                if (it == "tls" || it == "reality") it else "none"
            } ?: "none"
            
            val encryption = json.get("scy")?.asString ?: "auto"
            
            // TLS settings
            val sni = json.get("sni")?.asString
            val alpn = json.get("alpn")?.asString
            val fingerprint = json.get("fp")?.asString
            
            // WebSocket settings
            val path = json.get("path")?.asString
            val host = json.get("host")?.asString
            
            // HTTP/2 settings
            val httpHost = if (network == "h2") host else null
            val httpPath = if (network == "h2") path else null
            
            // gRPC settings
            val serviceName = json.get("serviceName")?.asString
            val mode = json.get("mode")?.asString
            
            // QUIC settings
            val quicSecurity = json.get("type")?.asString
            val key = json.get("key")?.asString
            val headerType = json.get("headerType")?.asString
            
            val server = ProxyServer(
                name = name,
                protocol = ProxyProtocol.VMESS,
                address = address,
                port = port,
                uuid = uuid,
                alterId = alterId,
                encryption = encryption,
                network = network,
                security = security,
                sni = sni,
                alpn = alpn,
                fingerprint = fingerprint,
                path = if (network == "ws") path else null,
                host = if (network == "ws") host else null,
                httpHost = httpHost,
                httpPath = httpPath,
                serviceName = serviceName,
                mode = mode,
                quicSecurity = quicSecurity,
                key = key,
                headerType = headerType,
                remarks = name
            )
            
            Result.success(server)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun toUrl(server: ProxyServer): String {
        require(server.protocol == ProxyProtocol.VMESS) { "Server must be VMess" }
        require(server.uuid != null) { "UUID is required" }
        
        val json = JsonObject().apply {
            addProperty("v", "2")
            addProperty("ps", server.name)
            addProperty("add", server.address)
            addProperty("port", server.port)
            addProperty("id", server.uuid)
            addProperty("aid", server.alterId ?: 0)
            addProperty("scy", server.encryption ?: "auto")
            addProperty("net", server.network)
            addProperty("tls", server.security)
            
            // TLS settings
            server.sni?.let { addProperty("sni", it) }
            server.alpn?.let { addProperty("alpn", it) }
            server.fingerprint?.let { addProperty("fp", it) }
            
            // Transport settings
            when (server.network) {
                "ws" -> {
                    server.path?.let { addProperty("path", it) }
                    server.host?.let { addProperty("host", it) }
                }
                "h2" -> {
                    server.httpPath?.let { addProperty("path", it) }
                    server.httpHost?.let { addProperty("host", it) }
                }
                "grpc" -> {
                    server.serviceName?.let { addProperty("serviceName", it) }
                    server.mode?.let { addProperty("mode", it) }
                }
                "quic" -> {
                    server.quicSecurity?.let { addProperty("type", it) }
                    server.key?.let { addProperty("key", it) }
                    server.headerType?.let { addProperty("headerType", it) }
                }
            }
        }
        
        val jsonString = gson.toJson(json)
        val encoded = Base64.encodeToString(
            jsonString.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        
        return "vmess://$encoded"
    }
    
    fun validate(server: ProxyServer): Result<Unit> {
        return try {
            require(server.protocol == ProxyProtocol.VMESS) { "Protocol must be VMess" }
            require(server.uuid != null && server.uuid.isNotEmpty()) { "UUID is required" }
            require(server.address.isNotEmpty()) { "Address is required" }
            require(server.port in 1..65535) { "Port must be between 1 and 65535" }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

