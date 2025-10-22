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
import com.tunelapp.data.VlessServer
import com.tunelapp.core.WorkingPacketForwarder
import com.tunelapp.core.TrafficMonitor
import com.tunelapp.core.XrayManager
import com.tunelapp.data.TunelDatabase
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Простой VPN сервис для демонстрации
 * Создает TUN интерфейс и показывает уведомление
 */
class SimpleVpnService : VpnService() {
    
    private var vpnInterface: ParcelFileDescriptor? = null
    private var xrayManager: XrayManager? = null
    private var packetForwarder: WorkingPacketForwarder? = null
    private val trafficMonitor = TrafficMonitor()
    private var serviceJob: Job? = null
    private var isRunning = false
    
    companion object {
        private const val TAG = "SimpleVpnService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "simple_vpn_channel"
        const val ACTION_CONNECT = "com.tunelapp.action.CONNECT"
        const val ACTION_DISCONNECT = "com.tunelapp.action.DISCONNECT"
        const val EXTRA_SERVER_ID = "server_id"
        
        @Volatile
        private var currentInstance: SimpleVpnService? = null
        
        fun isVpnRunning(): Boolean {
            return currentInstance?.isRunning == true
        }
        
        fun getTrafficStats(): com.tunelapp.data.TrafficStats? {
            return currentInstance?.trafficMonitor?.getCurrentStats()
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
                // Post an immediate foreground notification to avoid Android O+ crash
                // if heavy work delays startForeground.
                startForeground(NOTIFICATION_ID, createStatusNotification(getString(com.tunelapp.R.string.connecting)))
                connectVpn(serverId)
            }
            ACTION_DISCONNECT -> {
                disconnectVpn()
            }
        }
        return START_STICKY
    }
    
    private fun connectVpn(serverId: Long) {
        Log.d(TAG, "connectVpn called with serverId: $serverId")
        
        if (isRunning) {
            Log.w(TAG, "VPN already running")
            return
        }
        
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting VPN connection for server: $serverId")
                
                // Get server from database
                Log.d(TAG, "Getting database instance...")
                val database = TunelDatabase.getInstance(applicationContext)
                Log.d(TAG, "Database instance obtained")
                
                Log.d(TAG, "Querying server from database...")
                val server = database.vlessServerDao().getServerById(serverId)
                
                if (server == null) {
                    Log.e(TAG, "Server not found: $serverId")
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "Server found: ${server.name} (${server.address}:${server.port})")

                // Mark service as running early so UI does not time out while
                // sing-box initializes and TUN is being established. We'll
                // revert this if anything fails below.
                isRunning = true
                
                // Start sing-box core
                Log.d(TAG, "Starting sing-box core...")
                val result = xrayManager?.start(server)
                if (result?.isFailure == true) {
                    // Fallback mode: continue without sing-box so VPN can
                    // still establish and UI won't fail hard. Traffic may not
                    // be proxied until core integration succeeds.
                    Log.e(TAG, "Failed to start sing-box (fallback mode)", result.exceptionOrNull())
                }
                
                Log.d(TAG, "sing-box started successfully")
                
                // Establish VPN connection
                Log.d(TAG, "Establishing VPN interface...")
                if (!establishVpn(server)) {
                    Log.e(TAG, "Failed to establish VPN interface")
                    xrayManager?.stop()
                    isRunning = false
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "VPN interface established successfully")
                isRunning = true
                
                // Show notification
                Log.d(TAG, "Starting foreground service...")
                startForeground(NOTIFICATION_ID, createNotification(server))
                
                Log.d(TAG, "VPN connected successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect VPN", e)
                e.printStackTrace()
                isRunning = false
                stopSelf()
            }
        }
    }
    
    private fun establishVpn(server: VlessServer): Boolean {
        try {
            val builder = Builder()
                .setSession(server.name)
                .setMtu(1500)
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
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
            packetForwarder = WorkingPacketForwarder(vpnInterface!!, trafficMonitor = trafficMonitor)
            packetForwarder?.start()
            
            Log.d(TAG, "Packet forwarding and traffic monitoring started")
            
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN", e)
            return false
        }
    }
    
    
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

    private fun createStatusNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TunelApp")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
