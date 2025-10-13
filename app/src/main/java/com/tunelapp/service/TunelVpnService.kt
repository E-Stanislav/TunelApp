package com.tunelapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tunelapp.R
import com.tunelapp.core.XrayManager
import com.tunelapp.data.VlessServer
import com.tunelapp.data.VlessDatabase
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 * VPN Service for TunelApp
 * Manages VPN connection and routes traffic through Xray-core
 */
class TunelVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null
    private var serviceJob: Job? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "TunelVpnService"
        private const val VPN_MTU = 1500
        private const val VPN_ADDRESS = "10.0.0.2"
        private const val VPN_ROUTE = "0.0.0.0"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "tunel_vpn_channel"
        const val ACTION_CONNECT = "com.tunelapp.action.CONNECT"
        const val ACTION_DISCONNECT = "com.tunelapp.action.DISCONNECT"
        const val EXTRA_SERVER_ID = "server_id"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        xrayManager = XrayManager(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val serverId = intent.getLongExtra(EXTRA_SERVER_ID, -1)
                if (serverId != -1L) {
                    connectVpn(serverId)
                }
            }
            ACTION_DISCONNECT -> {
                disconnectVpn()
            }
        }
        return START_STICKY
    }
    
    private fun connectVpn(serverId: Long) {
        if (isRunning) {
            Log.w(TAG, "VPN already running")
            return
        }
        
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get server from database
                val database = VlessDatabase.getDatabase(applicationContext)
                val server = database.vlessServerDao().getServerById(serverId)
                
                if (server == null) {
                    Log.e(TAG, "Server not found: $serverId")
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "Connecting to: ${server.name}")
                
                // Request VPN permission if needed
                val intent = prepare(this@TunelVpnService)
                if (intent != null) {
                    Log.e(TAG, "VPN permission not granted")
                    stopSelf()
                    return@launch
                }
                
                // Start Xray core
                val result = xrayManager?.start(server)
                if (result?.isFailure == true) {
                    Log.e(TAG, "Failed to start Xray", result.exceptionOrNull())
                    stopSelf()
                    return@launch
                }
                
                // Establish VPN connection
                if (!establishVpn(server)) {
                    xrayManager?.stop()
                    stopSelf()
                    return@launch
                }
                
                isRunning = true
                
                // Show notification
                startForeground(NOTIFICATION_ID, createNotification(server))
                
                Log.d(TAG, "VPN connected successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect VPN", e)
                stopSelf()
            }
        }
    }
    
    private fun establishVpn(server: VlessServer): Boolean {
        try {
            val builder = Builder()
                .setSession(server.name)
                .setMtu(VPN_MTU)
                .addAddress(VPN_ADDRESS, 24)
                .addRoute(VPN_ROUTE, 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("8.8.4.4")
            
            // Establish the VPN
            vpnInterface = builder.establish()
            
            if (vpnInterface == null) {
                Log.e(TAG, "Failed to establish VPN interface")
                return false
            }
            
            Log.d(TAG, "VPN interface established")
            
            // Start packet forwarding
            startPacketForwarding()
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN", e)
            return false
        }
    }
    
    private fun startPacketForwarding() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vpnInput = FileInputStream(vpnInterface?.fileDescriptor)
                val vpnOutput = FileOutputStream(vpnInterface?.fileDescriptor)
                val buffer = ByteBuffer.allocate(VPN_MTU)
                
                // Simple packet forwarding loop
                // In production, this would route through Xray's SOCKS proxy
                while (isRunning) {
                    val length = vpnInput.channel.read(buffer)
                    if (length > 0) {
                        buffer.flip()
                        
                        // TODO: Forward packet to Xray SOCKS proxy (127.0.0.1:10808)
                        // For now, we just log
                        Log.v(TAG, "Received packet: $length bytes")
                        
                        buffer.clear()
                    }
                }
                
            } catch (e: Exception) {
                if (isRunning) {
                    Log.e(TAG, "Packet forwarding error", e)
                }
            }
        }
    }
    
    private fun disconnectVpn() {
        Log.d(TAG, "Disconnecting VPN")
        
        isRunning = false
        serviceJob?.cancel()
        
        // Stop Xray
        CoroutineScope(Dispatchers.IO).launch {
            xrayManager?.stop()
        }
        
        // Close VPN interface
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close VPN interface", e)
        }
        vpnInterface = null
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        Log.d(TAG, "VPN disconnected")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        disconnectVpn()
    }
    
    override fun onRevoke() {
        super.onRevoke()
        Log.d(TAG, "VPN permission revoked")
        disconnectVpn()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows VPN connection status"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(server: VlessServer) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TunelApp")
            .setContentText("Connected to ${server.name}")
            .setSmallIcon(R.drawable.ic_vpn)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}



