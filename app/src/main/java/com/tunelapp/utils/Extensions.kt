package com.tunelapp.utils

import com.tunelapp.data.ProxyProtocol
import com.tunelapp.data.ProxyServer
import com.tunelapp.data.VlessServer

/**
 * Extension functions for converting between server types
 */

fun ProxyServer.toVlessServer(): VlessServer {
    return VlessServer(
        id = this.id,
        name = this.name,
        uuid = this.uuid ?: "",
        address = this.address,
        port = this.port,
        encryption = this.encryption ?: "none",
        flow = this.flow,
        network = this.network,
        security = this.security,
        sni = this.sni,
        fingerprint = this.fingerprint,
        alpn = this.alpn,
        allowInsecure = this.allowInsecure,
        path = this.path,
        host = this.host,
        serviceName = this.serviceName,
        quicSecurity = this.quicSecurity,
        key = this.key,
        headerType = this.headerType,
        remarks = this.remarks,
        isActive = this.isActive,
        createdAt = this.createdAt,
        lastUsed = this.lastUsed
    )
}

fun VlessServer.toProxyServer(): ProxyServer {
    return ProxyServer(
        id = this.id,
        name = this.name,
        protocol = ProxyProtocol.VLESS,
        address = this.address,
        port = this.port,
        uuid = this.uuid,
        encryption = this.encryption,
        flow = this.flow,
        network = this.network,
        security = this.security,
        sni = this.sni,
        fingerprint = this.fingerprint,
        alpn = this.alpn,
        allowInsecure = this.allowInsecure,
        path = this.path,
        host = this.host,
        serviceName = this.serviceName,
        quicSecurity = this.quicSecurity,
        key = this.key,
        headerType = this.headerType,
        remarks = this.remarks,
        isActive = this.isActive,
        createdAt = this.createdAt,
        lastUsed = this.lastUsed
    )
}

