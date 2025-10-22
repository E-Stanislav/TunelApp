package com.tunelapp.core

import android.util.Log
import com.tunelapp.data.TrafficStats
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Traffic monitor for tracking VPN data usage
 * Tracks upload/download speeds and totals
 */
class TrafficMonitor {
    
    private val _stats = MutableStateFlow(TrafficStats())
    val stats: StateFlow<TrafficStats> = _stats.asStateFlow()
    
    private val totalUpload = AtomicLong(0)
    private val totalDownload = AtomicLong(0)
    private var connectionStartTime: Long = 0
    private var monitorJob: Job? = null
    private var isRunning = false
    
    // Speed calculation (bytes in last second)
    private val uploadInLastSecond = AtomicLong(0)
    private val downloadInLastSecond = AtomicLong(0)
    
    companion object {
        private const val TAG = "TrafficMonitor"
        private const val UPDATE_INTERVAL = 1000L // 1 second
    }
    
    /**
     * Start monitoring traffic
     */
    fun start() {
        if (isRunning) return
        
        isRunning = true
        connectionStartTime = System.currentTimeMillis()
        totalUpload.set(0)
        totalDownload.set(0)
        
        monitorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isRunning) {
                updateStats()
                delay(UPDATE_INTERVAL)
            }
        }
        
        Log.d(TAG, "Traffic monitoring started")
    }
    
    /**
     * Stop monitoring
     */
    fun stop() {
        isRunning = false
        monitorJob?.cancel()
        monitorJob = null
        
        _stats.value = TrafficStats()
        
        Log.d(TAG, "Traffic monitoring stopped")
    }
    
    /**
     * Record uploaded bytes
     */
    fun recordUpload(bytes: Long) {
        totalUpload.addAndGet(bytes)
        uploadInLastSecond.addAndGet(bytes)
    }
    
    /**
     * Record downloaded bytes
     */
    fun recordDownload(bytes: Long) {
        totalDownload.addAndGet(bytes)
        downloadInLastSecond.addAndGet(bytes)
    }
    
    /**
     * Update statistics
     */
    private fun updateStats() {
        val now = System.currentTimeMillis()
        val connectedTime = now - connectionStartTime
        
        // Get current speeds (bytes in last second)
        val uploadSpeed = uploadInLastSecond.getAndSet(0)
        val downloadSpeed = downloadInLastSecond.getAndSet(0)
        
        _stats.value = TrafficStats(
            uploadSpeed = uploadSpeed,
            downloadSpeed = downloadSpeed,
            totalUpload = totalUpload.get(),
            totalDownload = totalDownload.get(),
            connectedTime = connectedTime
        )
    }
    
    /**
     * Get current stats snapshot
     */
    fun getCurrentStats(): TrafficStats {
        return _stats.value
    }
    
    /**
     * Reset counters
     */
    fun reset() {
        totalUpload.set(0)
        totalDownload.set(0)
        uploadInLastSecond.set(0)
        downloadInLastSecond.set(0)
        connectionStartTime = System.currentTimeMillis()
    }
}

