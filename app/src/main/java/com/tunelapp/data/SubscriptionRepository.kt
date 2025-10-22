package com.tunelapp.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Subscription operations
 */
class SubscriptionRepository(context: Context) {
    
    private val database = TunelDatabase.getInstance(context)
    private val subscriptionDao = database.subscriptionDao()
    private val proxyServerDao = database.proxyServerDao()
    
    // Observe all subscriptions
    fun getAllSubscriptions(): Flow<List<Subscription>> =
        subscriptionDao.getAllSubscriptions()
    
    // Observe enabled subscriptions
    fun getEnabledSubscriptions(): Flow<List<Subscription>> =
        subscriptionDao.getEnabledSubscriptions()
    
    // Get single subscription
    suspend fun getSubscriptionById(subscriptionId: Long): Subscription? =
        subscriptionDao.getSubscriptionById(subscriptionId)
    
    // Get subscriptions that need auto-update
    suspend fun getAutoUpdateSubscriptions(): List<Subscription> =
        subscriptionDao.getAutoUpdateSubscriptions()
    
    // Insert subscription
    suspend fun insertSubscription(subscription: Subscription): Long =
        subscriptionDao.insertSubscription(subscription)
    
    // Update subscription
    suspend fun updateSubscription(subscription: Subscription) =
        subscriptionDao.updateSubscription(subscription)
    
    // Delete subscription
    suspend fun deleteSubscription(subscription: Subscription) {
        // Delete all servers associated with this subscription
        proxyServerDao.deleteServersBySubscription(subscription.id)
        subscriptionDao.deleteSubscription(subscription)
    }
    
    // Delete by ID
    suspend fun deleteSubscriptionById(subscriptionId: Long) {
        proxyServerDao.deleteServersBySubscription(subscriptionId)
        subscriptionDao.deleteSubscriptionById(subscriptionId)
    }
    
    // Update subscription status after update
    suspend fun updateSubscriptionStatus(
        subscriptionId: Long,
        error: String? = null,
        serverCount: Int
    ) = subscriptionDao.updateSubscriptionStatus(
        subscriptionId = subscriptionId,
        error = error,
        serverCount = serverCount
    )
    
    // Enable/disable subscription
    suspend fun setSubscriptionEnabled(subscriptionId: Long, enabled: Boolean) =
        subscriptionDao.setSubscriptionEnabled(subscriptionId, enabled)
    
    // Get subscription count
    suspend fun getSubscriptionCount(): Int =
        subscriptionDao.getSubscriptionCount()
    
    // Get server count for subscription
    suspend fun getServerCountBySubscription(subscriptionId: Long): Int =
        proxyServerDao.getServerCountBySubscription(subscriptionId)
}

