package com.tunelapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Subscription configuration for importing multiple servers
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Basic info
    val name: String,
    val url: String,
    val type: SubscriptionType = SubscriptionType.AUTO,
    
    // Auto-update settings
    val autoUpdate: Boolean = true,
    val updateInterval: Long = 24 * 60 * 60 * 1000, // 24 hours in milliseconds
    
    // Status
    val isEnabled: Boolean = true,
    val lastUpdate: Long? = null,
    val lastError: String? = null,
    val serverCount: Int = 0,
    
    // Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val remarks: String? = null,
    
    // User-agent and headers for fetching
    val userAgent: String? = null,
    val customHeaders: String? = null // JSON string
)

/**
 * Subscription types
 */
enum class SubscriptionType {
    AUTO,           // Auto-detect format
    BASE64,         // Simple base64 encoded URLs
    CLASH,          // Clash/ClashMeta YAML
    V2RAYN,         // v2rayN format
    SIP008,         // Shadowsocks SIP008 JSON
    SINGBOX,        // sing-box JSON
    QUANTUMULT      // Quantumult(X) format
}

/**
 * Subscription update result
 */
data class SubscriptionUpdateResult(
    val subscriptionId: Long,
    val success: Boolean,
    val serversAdded: Int = 0,
    val serversUpdated: Int = 0,
    val serversRemoved: Int = 0,
    val error: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

