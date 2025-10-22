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
import com.tunelapp.core.PacketForwarder
import com.tunelapp.core.TrafficMonitor
import com.tunelapp.core.XrayManager
import com.tunelapp.data.VlessServer
import com.tunelapp.data.TunelDatabase
import kotlinx.coroutines.*

/**
 * VPN Service for TunelApp
 * Manages VPN connection and routes traffic through Xray-core
 */
class TunelVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null
    private var packetForwarder: PacketForwarder? = null
    private val trafficMonitor = TrafficMonitor()
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
        
        @Volatile
        private var currentInstance: TunelVpnService? = null
        
        /**
         * Get current traffic stats from running VPN service
         */
        fun getTrafficStats(): com.tunelapp.data.TrafficStats? {
            return currentInstance?.trafficMonitor?.getCurrentStats()
        }
        
        /**
         * Check if VPN is running
         */
        fun isVpnRunning(): Boolean {
            return currentInstance?.isRunning == true
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        currentInstance = this
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
                val database = TunelDatabase.getInstance(applicationContext)
                val server = database.vlessServerDao().getServerById(serverId)
                
                if (server == null) {
                    Log.e(TAG, "Server not found: $serverId")
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "Connecting to: ${server.name}")
                
                // VPN permission should be checked before starting service
                Log.d(TAG, "Starting VPN connection for server: $serverId")
                
                // Start sing-box core
                val result = xrayManager?.start(server)
                if (result?.isFailure == true) {
                    Log.e(TAG, "Failed to start sing-box", result.exceptionOrNull())
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "sing-box started successfully")
                
                // Establish VPN connection
                if (!establishVpn(server)) {
                    Log.e(TAG, "Failed to establish VPN interface")
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
            
            // Start traffic monitoring
            trafficMonitor.start()
            
            // Start packet forwarding
            packetForwarder = PacketForwarder(vpnInterface!!, trafficMonitor = trafficMonitor)
            packetForwarder?.start()
            
            Log.d(TAG, "Packet forwarding and traffic monitoring started")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN", e)
            return false
        }
    }
    
    /**
     * Note: Packet forwarding is now handled by PacketForwarder class
     * See core/PacketForwarder.kt
     * 
     * For production, consider using:
     * - tun2socks library
     * - OR sing-box's built-in TUN mode (configure in ProxyConfig)
     */
    
    private fun disconnectVpn() {
        Log.d(TAG, "Disconnecting VPN")
        
        isRunning = false
        serviceJob?.cancel()
        
        // Stop packet forwarding
        packetForwarder?.stop()
        packetForwarder = null
        
        // Stop traffic monitoring
        trafficMonitor.stop()
        
        // Stop sing-box
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
        currentInstance = null
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





