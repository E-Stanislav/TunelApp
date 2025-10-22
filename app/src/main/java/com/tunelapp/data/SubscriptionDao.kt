package com.tunelapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Subscription
 */
@Dao
interface SubscriptionDao {
    
    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE isEnabled = 1 ORDER BY createdAt DESC")
    fun getEnabledSubscriptions(): Flow<List<Subscription>>
    
    @Query("SELECT * FROM subscriptions WHERE id = :subscriptionId")
    suspend fun getSubscriptionById(subscriptionId: Long): Subscription?
    
    @Query("SELECT * FROM subscriptions WHERE autoUpdate = 1 AND isEnabled = 1")
    suspend fun getAutoUpdateSubscriptions(): List<Subscription>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long
    
    @Update
    suspend fun updateSubscription(subscription: Subscription)
    
    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
    
    @Query("DELETE FROM subscriptions WHERE id = :subscriptionId")
    suspend fun deleteSubscriptionById(subscriptionId: Long)
    
    @Query("UPDATE subscriptions SET lastUpdate = :timestamp, lastError = :error, serverCount = :serverCount WHERE id = :subscriptionId")
    suspend fun updateSubscriptionStatus(
        subscriptionId: Long,
        timestamp: Long = System.currentTimeMillis(),
        error: String? = null,
        serverCount: Int
    )
    
    @Query("UPDATE subscriptions SET isEnabled = :enabled WHERE id = :subscriptionId")
    suspend fun setSubscriptionEnabled(subscriptionId: Long, enabled: Boolean)
    
    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun getSubscriptionCount(): Int
}

