package com.tunelapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main database for TunelApp
 * Includes both legacy VlessServer and new ProxyServer + Subscription tables
 */
@Database(
    entities = [
        VlessServer::class,
        ProxyServer::class,
        Subscription::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TunelDatabase : RoomDatabase() {
    
    abstract fun vlessServerDao(): VlessServerDao
    abstract fun proxyServerDao(): ProxyServerDao
    abstract fun subscriptionDao(): SubscriptionDao
    
    companion object {
        @Volatile
        private var INSTANCE: TunelDatabase? = null
        
        private const val DATABASE_NAME = "tunel_database"
        
        fun getInstance(context: Context): TunelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TunelDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Migration from version 1 to 2
         * Adds ProxyServer and Subscription tables
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create proxy_servers table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `proxy_servers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `protocol` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `uuid` TEXT,
                        `password` TEXT,
                        `username` TEXT,
                        `alterId` INTEGER,
                        `encryption` TEXT,
                        `method` TEXT,
                        `flow` TEXT,
                        `network` TEXT NOT NULL DEFAULT 'tcp',
                        `security` TEXT NOT NULL DEFAULT 'none',
                        `sni` TEXT,
                        `fingerprint` TEXT,
                        `alpn` TEXT,
                        `allowInsecure` INTEGER NOT NULL DEFAULT 0,
                        `publicKey` TEXT,
                        `shortId` TEXT,
                        `spiderX` TEXT,
                        `path` TEXT,
                        `host` TEXT,
                        `headers` TEXT,
                        `serviceName` TEXT,
                        `mode` TEXT,
                        `httpHost` TEXT,
                        `httpPath` TEXT,
                        `quicSecurity` TEXT,
                        `key` TEXT,
                        `headerType` TEXT,
                        `plugin` TEXT,
                        `pluginOpts` TEXT,
                        `subscriptionId` INTEGER,
                        `subscriptionTag` TEXT,
                        `remarks` TEXT,
                        `groupName` TEXT,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `lastUsed` INTEGER,
                        `testLatency` INTEGER,
                        `testSpeed` INTEGER,
                        `testTime` INTEGER
                    )
                """.trimIndent())
                
                // Create subscriptions table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `subscriptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `autoUpdate` INTEGER NOT NULL DEFAULT 1,
                        `updateInterval` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL DEFAULT 1,
                        `lastUpdate` INTEGER,
                        `lastError` TEXT,
                        `serverCount` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `remarks` TEXT,
                        `userAgent` TEXT,
                        `customHeaders` TEXT
                    )
                """.trimIndent())
                
                // Migrate existing vless_servers to proxy_servers
                database.execSQL("""
                    INSERT INTO proxy_servers (
                        name, protocol, address, port, uuid, encryption, flow,
                        network, security, sni, fingerprint, alpn, allowInsecure,
                        path, host, serviceName, quicSecurity, key, headerType,
                        remarks, isActive, createdAt, lastUsed
                    )
                    SELECT 
                        name, 'VLESS', address, port, uuid, encryption, flow,
                        network, security, sni, fingerprint, alpn, allowInsecure,
                        path, host, serviceName, quicSecurity, key, headerType,
                        remarks, isActive, createdAt, lastUsed
                    FROM vless_servers
                """.trimIndent())
            }
        }
    }
}

/**
 * Type converters for Room
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromProxyProtocol(protocol: ProxyProtocol): String {
        return protocol.name
    }
    
    @androidx.room.TypeConverter
    fun toProxyProtocol(value: String): ProxyProtocol {
        return ProxyProtocol.valueOf(value)
    }
    
    @androidx.room.TypeConverter
    fun fromSubscriptionType(type: SubscriptionType): String {
        return type.name
    }
    
    @androidx.room.TypeConverter
    fun toSubscriptionType(value: String): SubscriptionType {
        return SubscriptionType.valueOf(value)
    }
}

