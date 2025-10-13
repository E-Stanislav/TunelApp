package com.tunelapp.core

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.tunelapp.data.VlessServer

/**
 * Xray configuration generator
 */
object XrayConfig {
    
    /**
     * Generate Xray configuration JSON
     */
    fun generate(server: VlessServer, socksPort: Int = 10808, httpPort: Int = 10809): String {
        val config = XrayConfiguration(
            log = LogConfig(loglevel = "warning"),
            inbounds = listOf(
                Inbound(
                    tag = "socks-in",
                    port = socksPort,
                    listen = "127.0.0.1",
                    protocol = "socks",
                    settings = InboundSettings(
                        auth = "noauth",
                        udp = true,
                        ip = "127.0.0.1"
                    )
                ),
                Inbound(
                    tag = "http-in",
                    port = httpPort,
                    listen = "127.0.0.1",
                    protocol = "http"
                )
            ),
            outbounds = listOf(
                Outbound(
                    tag = "proxy",
                    protocol = "vless",
                    settings = OutboundSettings(
                        vnext = listOf(
                            VnextServer(
                                address = server.address,
                                port = server.port,
                                users = listOf(
                                    VlessUser(
                                        id = server.uuid,
                                        encryption = server.encryption,
                                        flow = server.flow
                                    )
                                )
                            )
                        )
                    ),
                    streamSettings = createStreamSettings(server),
                    mux = MuxConfig(enabled = false)
                ),
                Outbound(
                    tag = "direct",
                    protocol = "freedom",
                    settings = OutboundSettings()
                ),
                Outbound(
                    tag = "block",
                    protocol = "blackhole",
                    settings = OutboundSettings()
                )
            ),
            routing = RoutingConfig(
                domainStrategy = "IPIfNonMatch",
                rules = listOf(
                    RoutingRule(
                        type = "field",
                        ip = listOf("geoip:private"),
                        outboundTag = "direct"
                    ),
                    RoutingRule(
                        type = "field",
                        domain = listOf("geosite:cn"),
                        outboundTag = "direct"
                    )
                )
            )
        )
        
        return Gson().toJson(config)
    }
    
    private fun createStreamSettings(server: VlessServer): StreamSettings {
        val security = if (server.security == "tls" || server.security == "reality") {
            server.security
        } else {
            "none"
        }
        
        val tlsSettings = if (security == "tls") {
            TlsSettings(
                serverName = server.sni,
                allowInsecure = server.allowInsecure,
                fingerprint = server.fingerprint,
                alpn = server.alpn?.split(",")
            )
        } else null
        
        val networkSettings = when (server.network) {
            "ws" -> NetworkSettings(
                wsSettings = WsSettings(
                    path = server.path,
                    headers = server.host?.let { mapOf("Host" to it) }
                )
            )
            "grpc" -> NetworkSettings(
                grpcSettings = GrpcSettings(
                    serviceName = server.serviceName ?: ""
                )
            )
            "http" -> NetworkSettings(
                httpSettings = HttpSettings(
                    path = server.path,
                    host = server.host?.split(",")
                )
            )
            "quic" -> NetworkSettings(
                quicSettings = QuicSettings(
                    security = server.quicSecurity ?: "none",
                    key = server.key,
                    header = QuicHeader(type = server.headerType ?: "none")
                )
            )
            else -> null
        }
        
        return StreamSettings(
            network = server.network,
            security = security,
            tlsSettings = tlsSettings,
            wsSettings = networkSettings?.wsSettings,
            grpcSettings = networkSettings?.grpcSettings,
            httpSettings = networkSettings?.httpSettings,
            quicSettings = networkSettings?.quicSettings
        )
    }
}

// Xray configuration data classes
data class XrayConfiguration(
    val log: LogConfig,
    val inbounds: List<Inbound>,
    val outbounds: List<Outbound>,
    val routing: RoutingConfig
)

data class LogConfig(
    val loglevel: String
)

data class Inbound(
    val tag: String,
    val port: Int,
    val listen: String,
    val protocol: String,
    val settings: InboundSettings? = null
)

data class InboundSettings(
    val auth: String? = null,
    val udp: Boolean? = null,
    val ip: String? = null
)

data class Outbound(
    val tag: String,
    val protocol: String,
    val settings: OutboundSettings,
    val streamSettings: StreamSettings? = null,
    val mux: MuxConfig? = null
)

data class OutboundSettings(
    val vnext: List<VnextServer>? = null
)

data class VnextServer(
    val address: String,
    val port: Int,
    val users: List<VlessUser>
)

data class VlessUser(
    val id: String,
    val encryption: String,
    val flow: String? = null
)

data class StreamSettings(
    val network: String,
    val security: String,
    val tlsSettings: TlsSettings? = null,
    val wsSettings: WsSettings? = null,
    val grpcSettings: GrpcSettings? = null,
    val httpSettings: HttpSettings? = null,
    val quicSettings: QuicSettings? = null
)

data class TlsSettings(
    val serverName: String?,
    val allowInsecure: Boolean,
    val fingerprint: String?,
    val alpn: List<String>?
)

data class NetworkSettings(
    val wsSettings: WsSettings? = null,
    val grpcSettings: GrpcSettings? = null,
    val httpSettings: HttpSettings? = null,
    val quicSettings: QuicSettings? = null
)

data class WsSettings(
    val path: String?,
    val headers: Map<String, String>?
)

data class GrpcSettings(
    val serviceName: String
)

data class HttpSettings(
    val path: String?,
    val host: List<String>?
)

data class QuicSettings(
    val security: String,
    val key: String?,
    val header: QuicHeader
)

data class QuicHeader(
    val type: String
)

data class MuxConfig(
    val enabled: Boolean
)

data class RoutingConfig(
    val domainStrategy: String,
    val rules: List<RoutingRule>
)

data class RoutingRule(
    val type: String,
    val ip: List<String>? = null,
    val domain: List<String>? = null,
    val outboundTag: String
)



