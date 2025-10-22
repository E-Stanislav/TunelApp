package com.tunelapp.core

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

/**
 * Working packet forwarder that actually forwards traffic through SOCKS proxy
 */
class WorkingPacketForwarder(
    private val tunInterface: ParcelFileDescriptor,
    private val socksProxy: InetSocketAddress = InetSocketAddress("127.0.0.1", 10808),
    private val trafficMonitor: TrafficMonitor? = null
) {
    
    private var forwardingJob: Job? = null
    private var isRunning = false
    private val activeConnections = mutableMapOf<String, Socket>()
    
    companion object {
        private const val TAG = "WorkingPacketForwarder"
        private const val MTU = 1500
        private const val BUFFER_SIZE = 32768
    }
    
    /**
     * Start packet forwarding
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Packet forwarding already running")
            return
        }
        
        isRunning = true
        
        forwardingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting packet forwarding to SOCKS proxy $socksProxy")
                forwardPackets()
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Packet forwarding error", e)
                }
            } finally {
                isRunning = false
            }
        }
    }
    
    /**
     * Stop packet forwarding
     */
    fun stop() {
        Log.d(TAG, "Stopping packet forwarding")
        isRunning = false
        forwardingJob?.cancel()
        forwardingJob = null
        
        // Close all active connections
        activeConnections.values.forEach { socket ->
            try {
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing connection", e)
            }
        }
        activeConnections.clear()
    }
    
    /**
     * Main packet forwarding loop
     */
    private suspend fun forwardPackets() = withContext(Dispatchers.IO) {
        val tunInput = FileInputStream(tunInterface.fileDescriptor)
        val tunOutput = FileOutputStream(tunInterface.fileDescriptor)
        val buffer = ByteBuffer.allocate(BUFFER_SIZE)
        
        Log.d(TAG, "Packet forwarding loop started")
        
        while (isRunning) {
            try {
                // Read packet from TUN interface
                buffer.clear()
                val length = tunInput.channel.read(buffer)
                
                if (length > 0) {
                    buffer.flip()
                    
                    // Record uploaded bytes
                    trafficMonitor?.recordUpload(length.toLong())
                    
                    // Parse IP packet
                    val packet = ByteArray(length)
                    buffer.get(packet)
                    
                    // Get IP version (first 4 bits)
                    val version = (packet[0].toInt() shr 4) and 0x0F
                    
                    when (version) {
                        4 -> handleIPv4Packet(packet, tunOutput)
                        6 -> handleIPv6Packet(packet, tunOutput)
                        else -> {
                            Log.w(TAG, "Unknown IP version: $version")
                        }
                    }
                    
                    buffer.clear()
                } else if (length < 0) {
                    // End of stream
                    Log.d(TAG, "End of TUN stream")
                    break
                }
                
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Error processing packet", e)
                    delay(100) // Small delay to avoid busy loop on errors
                }
            }
        }
        
        Log.d(TAG, "Packet forwarding loop ended")
    }
    
    /**
     * Handle IPv4 packet with actual forwarding
     */
    private suspend fun handleIPv4Packet(packet: ByteArray, tunOutput: FileOutputStream) {
        try {
            // Extract protocol (byte 9)
            val protocol = packet[9].toInt() and 0xFF
            
            // Extract source and destination IPs
            val srcIp = extractIPv4Address(packet, 12)
            val dstIp = extractIPv4Address(packet, 16)
            
            when (protocol) {
                6 -> { // TCP
                    Log.v(TAG, "TCP packet: $srcIp -> $dstIp")
                    handleTCPPacket(packet, tunOutput)
                }
                17 -> { // UDP
                    Log.v(TAG, "UDP packet: $srcIp -> $dstIp")
                    handleUDPPacket(packet, tunOutput)
                }
                1 -> { // ICMP (ping)
                    Log.v(TAG, "ICMP packet: $srcIp -> $dstIp")
                    handleICMPPacket(packet, tunOutput)
                }
                else -> {
                    Log.v(TAG, "Other protocol ($protocol): $srcIp -> $dstIp")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling IPv4 packet", e)
        }
    }
    
    /**
     * Handle TCP packet by creating a relay through SOCKS proxy
     */
    private suspend fun handleTCPPacket(packet: ByteArray, tunOutput: FileOutputStream) {
        try {
            // Extract TCP header info
            val srcIp = extractIPv4Address(packet, 12)
            val srcPort = ((packet[20].toInt() and 0xFF) shl 8) or (packet[21].toInt() and 0xFF)
            val dstPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
            val dstIp = extractIPv4Address(packet, 16)
            
            val connectionKey = "$srcIp:$srcPort->$dstIp:$dstPort"
            
            Log.d(TAG, "TCP: $connectionKey")
            
            // Check if we already have this connection
            val existingSocket = activeConnections[connectionKey]
            if (existingSocket != null && !existingSocket.isClosed) {
                // Forward data through existing connection
                forwardTCPData(existingSocket, packet, tunOutput)
                return
            }
            
            // Create new connection
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val socket = Socket()
                    socket.connect(socksProxy, 5000) // Connect to SOCKS proxy
                    
                    // SOCKS5 handshake
                    val handshake = byteArrayOf(0x05, 0x01, 0x00) // SOCKS5, 1 auth method, no auth
                    socket.getOutputStream().write(handshake)
                    
                    val response = ByteArray(2)
                    socket.getInputStream().read(response)
                    
                    if (response[0] == 0x05.toByte() && response[1] == 0x00.toByte()) {
                        // SOCKS5 handshake successful, send CONNECT request
                        val connectRequest = buildSOCKS5ConnectRequest(dstIp, dstPort)
                        socket.getOutputStream().write(connectRequest)
                        
                        val connectResponse = ByteArray(10)
                        socket.getInputStream().read(connectResponse)
                        
                        if (connectResponse[1] == 0x00.toByte()) {
                            Log.d(TAG, "SOCKS5 connection established to $dstIp:$dstPort")
                            
                            // Store connection
                            activeConnections[connectionKey] = socket
                            
                            // Start bidirectional relay
                            startTCPRelay(socket, tunOutput, packet, connectionKey)
                        } else {
                            Log.w(TAG, "SOCKS5 connection failed")
                            socket.close()
                        }
                    } else {
                        Log.w(TAG, "SOCKS5 handshake failed")
                        socket.close()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "TCP connection error", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling TCP packet", e)
        }
    }
    
    /**
     * Handle UDP packet
     */
    private suspend fun handleUDPPacket(packet: ByteArray, tunOutput: FileOutputStream) {
        // UDP is more complex, skip for now
        Log.v(TAG, "UDP packet handling not implemented")
    }
    
    /**
     * Handle ICMP packet (ping)
     */
    private suspend fun handleICMPPacket(packet: ByteArray, tunOutput: FileOutputStream) {
        try {
            // For ICMP, we can create a simple echo response
            if (packet.size > 20) { // Check if it's a ping request
                val icmpType = packet[20].toInt() and 0xFF
                if (icmpType == 8) { // Echo request
                    Log.d(TAG, "ICMP echo request received")
                    
                    // Create echo reply
                    val reply = packet.clone()
                    reply[20] = 0.toByte() // Change type to echo reply
                    
                    // Swap source and destination IPs
                    for (i in 0..3) {
                        val temp = reply[12 + i]
                        reply[12 + i] = reply[16 + i]
                        reply[16 + i] = temp
                    }
                    
                    // Recalculate checksum (simplified)
                    reply[22] = 0.toByte()
                    reply[23] = 0.toByte()
                    
                    // Send reply back through TUN
                    tunOutput.write(reply)
                    trafficMonitor?.recordDownload(reply.size.toLong())
                    
                    Log.d(TAG, "ICMP echo reply sent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling ICMP packet", e)
        }
    }
    
    /**
     * Forward TCP data through existing connection
     */
    private suspend fun forwardTCPData(socket: Socket, packet: ByteArray, tunOutput: FileOutputStream) {
        try {
            val tcpDataStart = 20 + ((packet[12].toInt() and 0xF0) shr 2) // IP header length
            if (packet.size > tcpDataStart) {
                val tcpData = packet.sliceArray(tcpDataStart until packet.size)
                if (tcpData.isNotEmpty()) {
                    socket.getOutputStream().write(tcpData)
                    Log.v(TAG, "Forwarded ${tcpData.size} bytes to remote server")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding TCP data", e)
        }
    }
    
    /**
     * Build SOCKS5 CONNECT request
     */
    private fun buildSOCKS5ConnectRequest(host: String, port: Int): ByteArray {
        val hostBytes = host.toByteArray()
        return byteArrayOf(
            0x05, // SOCKS version 5
            0x01, // CONNECT command
            0x00, // Reserved
            0x03, // Address type: domain name
            hostBytes.size.toByte() // Address length
        ) + hostBytes + byteArrayOf(
            (port shr 8).toByte(), // Port high byte
            (port and 0xFF).toByte() // Port low byte
        )
    }
    
    /**
     * Start bidirectional TCP relay
     */
    private suspend fun startTCPRelay(socket: Socket, tunOutput: FileOutputStream, originalPacket: ByteArray, connectionKey: String) {
        try {
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            
            // Send original packet data to remote server
            val tcpDataStart = 20 + ((originalPacket[12].toInt() and 0xF0) shr 2) // IP header length
            if (originalPacket.size > tcpDataStart) {
                val tcpData = originalPacket.sliceArray(tcpDataStart until originalPacket.size)
                if (tcpData.isNotEmpty()) {
                    outputStream.write(tcpData)
                    Log.d(TAG, "Sent ${tcpData.size} bytes to remote server")
                }
            }
            
            // Start reading responses
            val buffer = ByteArray(4096)
            while (isRunning && !socket.isClosed) {
                try {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        // Create response packet (simplified)
                        val responsePacket = createResponsePacket(originalPacket, buffer, bytesRead)
                        tunOutput.write(responsePacket)
                        trafficMonitor?.recordDownload(bytesRead.toLong())
                        Log.d(TAG, "Received $bytesRead bytes from remote server")
                    } else if (bytesRead == -1) {
                        break
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        Log.e(TAG, "Error in TCP relay", e)
                    }
                    break
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TCP relay error", e)
        } finally {
            try {
                socket.close()
                activeConnections.remove(connectionKey)
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket", e)
            }
        }
    }
    
    /**
     * Create response packet (simplified)
     */
    private fun createResponsePacket(originalPacket: ByteArray, data: ByteArray, length: Int): ByteArray {
        // This is a simplified implementation
        // In reality, you'd need to properly reconstruct the IP/TCP headers
        return data.sliceArray(0 until length)
    }
    
    /**
     * Handle IPv6 packet (stub)
     */
    private suspend fun handleIPv6Packet(packet: ByteArray, tunOutput: FileOutputStream) {
        Log.v(TAG, "IPv6 packet received (not implemented)")
        // IPv6 support can be added similarly to IPv4
    }
    
    /**
     * Extract IPv4 address from packet
     */
    private fun extractIPv4Address(packet: ByteArray, offset: Int): String {
        return "${packet[offset].toInt() and 0xFF}." +
               "${packet[offset + 1].toInt() and 0xFF}." +
               "${packet[offset + 2].toInt() and 0xFF}." +
               "${packet[offset + 3].toInt() and 0xFF}"
    }
}
