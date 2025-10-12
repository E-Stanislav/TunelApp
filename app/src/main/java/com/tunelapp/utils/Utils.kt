package com.tunelapp.utils

import android.content.Context
import android.widget.Toast

/**
 * Utility functions for the app
 */
object Utils {
    
    /**
     * Show a toast message
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
    
    /**
     * Format bytes to human readable format
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.2f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
    
    /**
     * Format speed to human readable format
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }
    
    /**
     * Format duration to human readable format
     */
    fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes % 60, seconds % 60)
            minutes > 0 -> "%d:%02d".format(minutes, seconds % 60)
            else -> "0:%02d".format(seconds)
        }
    }
    
    /**
     * Validate VLESS UUID format
     */
    fun isValidUUID(uuid: String): Boolean {
        val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        return uuid.matches(uuidRegex.toRegex())
    }
    
    /**
     * Validate host/IP address
     */
    fun isValidHost(host: String): Boolean {
        // Simple validation - can be improved
        return host.isNotEmpty() && (
            host.matches("^[a-zA-Z0-9.-]+$".toRegex()) || // domain
            host.matches("^([0-9]{1,3}\\.){3}[0-9]{1,3}$".toRegex()) // IPv4
        )
    }
    
    /**
     * Validate port number
     */
    fun isValidPort(port: Int): Boolean {
        return port in 1..65535
    }
}

