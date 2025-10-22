package com.tunelapp.core

import android.content.Context
import android.util.Log
import com.tunelapp.data.ProxyRepository
import com.tunelapp.data.Subscription
import com.tunelapp.data.SubscriptionRepository
import com.tunelapp.data.SubscriptionUpdateResult
import com.tunelapp.parser.SubscriptionParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manager for subscription operations
 */
class SubscriptionManager(context: Context) {
    
    private val subscriptionRepository = SubscriptionRepository(context)
    private val proxyRepository = ProxyRepository(context)
    
    companion object {
        private const val TAG = "SubscriptionManager"
        private const val TIMEOUT_MS = 30000 // 30 seconds
        private const val DEFAULT_USER_AGENT = "TunelApp/1.0"
    }
    
    /**
     * Update a subscription by fetching new servers
     */
    suspend fun updateSubscription(subscription: Subscription): SubscriptionUpdateResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating subscription: ${subscription.name}")
            
            // Fetch subscription content
            val content = fetchSubscriptionContent(subscription)
            
            // Parse servers
            val parseResult = SubscriptionParser.parse(content, subscription.type, subscription.id)
            
            if (parseResult.isFailure) {
                val error = parseResult.exceptionOrNull()?.message ?: "Unknown parsing error"
                Log.e(TAG, "Failed to parse subscription: $error")
                
                // Update subscription with error
                subscriptionRepository.updateSubscriptionStatus(
                    subscriptionId = subscription.id,
                    error = error,
                    serverCount = 0
                )
                
                return@withContext SubscriptionUpdateResult(
                    subscriptionId = subscription.id,
                    success = false,
                    error = error
                )
            }
            
            val newServers = parseResult.getOrNull() ?: emptyList()
            
            // Get existing servers from this subscription
            val existingServers = mutableListOf<com.tunelapp.data.ProxyServer>()
            // Note: Would need to collect from Flow, simplified here
            
            // Delete old servers and insert new ones
            proxyRepository.deleteServersBySubscription(subscription.id)
            val insertedIds = proxyRepository.insertServers(newServers)
            
            Log.d(TAG, "Updated subscription ${subscription.name}: ${insertedIds.size} servers")
            
            // Update subscription status
            subscriptionRepository.updateSubscriptionStatus(
                subscriptionId = subscription.id,
                error = null,
                serverCount = insertedIds.size
            )
            
            SubscriptionUpdateResult(
                subscriptionId = subscription.id,
                success = true,
                serversAdded = insertedIds.size,
                serversUpdated = 0,
                serversRemoved = existingServers.size
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update subscription: ${subscription.name}", e)
            
            val error = e.message ?: "Unknown error"
            subscriptionRepository.updateSubscriptionStatus(
                subscriptionId = subscription.id,
                error = error,
                serverCount = 0
            )
            
            SubscriptionUpdateResult(
                subscriptionId = subscription.id,
                success = false,
                error = error
            )
        }
    }
    
    /**
     * Update all enabled subscriptions
     */
    suspend fun updateAllSubscriptions(): List<SubscriptionUpdateResult> = withContext(Dispatchers.IO) {
        val subscriptions = subscriptionRepository.getAutoUpdateSubscriptions()
        val results = mutableListOf<SubscriptionUpdateResult>()
        
        subscriptions.forEach { subscription ->
            val result = updateSubscription(subscription)
            results.add(result)
        }
        
        results
    }
    
    /**
     * Check if any subscriptions need updating
     */
    suspend fun checkForUpdates(): List<Subscription> = withContext(Dispatchers.IO) {
        val subscriptions = subscriptionRepository.getAutoUpdateSubscriptions()
        val now = System.currentTimeMillis()
        
        subscriptions.filter { subscription ->
            val lastUpdate = subscription.lastUpdate ?: 0
            val shouldUpdate = (now - lastUpdate) >= subscription.updateInterval
            shouldUpdate
        }
    }
    
    /**
     * Fetch subscription content from URL
     */
    private suspend fun fetchSubscriptionContent(subscription: Subscription): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(subscription.url)
            connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            
            // Set user agent
            val userAgent = subscription.userAgent ?: DEFAULT_USER_AGENT
            connection.setRequestProperty("User-Agent", userAgent)
            
            // Set custom headers if provided
            subscription.customHeaders?.let { headersJson ->
                try {
                    // Parse JSON headers and add them
                    // Simplified - full implementation would parse JSON
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse custom headers", e)
                }
            }
            
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: $responseCode")
            }
            
            val content = connection.inputStream.bufferedReader().use { it.readText() }
            content
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch subscription from ${subscription.url}", e)
            throw e
        } finally {
            connection?.disconnect()
        }
    }
    
    /**
     * Validate subscription URL
     */
    suspend fun validateSubscriptionUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }
}

