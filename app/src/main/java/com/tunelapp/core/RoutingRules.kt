package com.tunelapp.core

import com.tunelapp.data.ProxyServer

/**
 * Routing rules for split tunneling and traffic management
 */
data class RoutingRules(
    val mode: RoutingMode = RoutingMode.PROXY_ALL,
    val bypassRules: List<BypassRule> = emptyList(),
    val directRules: List<DirectRule> = emptyList(),
    val domainRules: List<DomainRule> = emptyList(),
    val ipRules: List<IpRule> = emptyList(),
    val appRules: List<AppRule> = emptyList(),
    val customRules: List<CustomRule> = emptyList()
)

/**
 * Routing mode
 */
enum class RoutingMode {
    PROXY_ALL,              // Route all traffic through VPN
    DIRECT_ALL,             // Route all traffic directly (VPN disabled)
    BYPASS_LOCAL,           // Bypass local/LAN addresses
    BYPASS_CHINA,           // Bypass China IPs (GFW)
    BYPASS_CUSTOM,          // Custom rules
    SPLIT_TUNNELING         // Per-app routing
}

/**
 * Bypass rule (traffic goes direct, not through VPN)
 */
data class BypassRule(
    val id: String,
    val type: BypassType,
    val value: String,
    val isEnabled: Boolean = true,
    val description: String? = null
)

enum class BypassType {
    DOMAIN,         // Domain name
    IP,             // IP address or CIDR
    APP,            // Android package name
    GEOIP,          // GeoIP country code
    GEOSITE,        // GeoSite category
    PORT            // Port number
}

/**
 * Direct rule (force traffic to go directly)
 */
data class DirectRule(
    val id: String,
    val pattern: String,
    val isEnabled: Boolean = true
)

/**
 * Domain-based rule
 */
data class DomainRule(
    val domain: String,
    val matchType: DomainMatchType = DomainMatchType.FULL,
    val action: RoutingAction = RoutingAction.PROXY
)

enum class DomainMatchType {
    FULL,           // Exact match
    SUFFIX,         // Domain suffix (e.g., .google.com)
    KEYWORD,        // Contains keyword
    REGEX           // Regular expression
}

/**
 * IP-based rule
 */
data class IpRule(
    val cidr: String,           // CIDR notation (e.g., 192.168.0.0/16)
    val action: RoutingAction = RoutingAction.DIRECT
)

/**
 * App-based rule (split tunneling)
 */
data class AppRule(
    val packageName: String,
    val appName: String,
    val action: RoutingAction = RoutingAction.PROXY
)

/**
 * Custom rule (advanced)
 */
data class CustomRule(
    val id: String,
    val name: String,
    val rule: String,           // Rule expression
    val action: RoutingAction = RoutingAction.PROXY,
    val isEnabled: Boolean = true
)

/**
 * Routing action
 */
enum class RoutingAction {
    PROXY,          // Route through VPN
    DIRECT,         // Direct connection
    BLOCK,          // Block connection
    REJECT          // Reject connection
}

/**
 * Preset routing configurations
 */
object RoutingPresets {
    
    /**
     * Bypass local/private IP addresses
     */
    val bypassLocal = RoutingRules(
        mode = RoutingMode.BYPASS_LOCAL,
        ipRules = listOf(
            IpRule("10.0.0.0/8", RoutingAction.DIRECT),
            IpRule("172.16.0.0/12", RoutingAction.DIRECT),
            IpRule("192.168.0.0/16", RoutingAction.DIRECT),
            IpRule("127.0.0.0/8", RoutingAction.DIRECT),
            IpRule("169.254.0.0/16", RoutingAction.DIRECT)
        )
    )
    
    /**
     * Bypass China IPs (for users in China)
     */
    val bypassChina = RoutingRules(
        mode = RoutingMode.BYPASS_CHINA,
        bypassRules = listOf(
            BypassRule("geoip-cn", BypassType.GEOIP, "cn", description = "China IPs"),
            BypassRule("geosite-cn", BypassType.GEOSITE, "cn", description = "China domains")
        )
    )
    
    /**
     * Bypass Russia IPs
     */
    val bypassRussia = RoutingRules(
        mode = RoutingMode.BYPASS_CUSTOM,
        bypassRules = listOf(
            BypassRule("geoip-ru", BypassType.GEOIP, "ru", description = "Russia IPs"),
            BypassRule("geosite-ru", BypassType.GEOSITE, "ru", description = "Russia domains")
        )
    )
    
    /**
     * Proxy all traffic
     */
    val proxyAll = RoutingRules(
        mode = RoutingMode.PROXY_ALL
    )
    
    /**
     * Common domains to bypass (for better performance)
     */
    val commonBypassDomains = listOf(
        "localhost",
        "*.local",
        "*.lan",
        // Add more as needed
    )
    
    /**
     * Get preset by name
     */
    fun getPreset(name: String): RoutingRules? {
        return when (name.lowercase()) {
            "bypass_local" -> bypassLocal
            "bypass_china" -> bypassChina
            "bypass_russia" -> bypassRussia
            "proxy_all" -> proxyAll
            else -> null
        }
    }
}

/**
 * Routing rule builder
 */
class RoutingRuleBuilder {
    private var mode = RoutingMode.PROXY_ALL
    private val bypassRules = mutableListOf<BypassRule>()
    private val directRules = mutableListOf<DirectRule>()
    private val domainRules = mutableListOf<DomainRule>()
    private val ipRules = mutableListOf<IpRule>()
    private val appRules = mutableListOf<AppRule>()
    private val customRules = mutableListOf<CustomRule>()
    
    fun setMode(mode: RoutingMode) = apply {
        this.mode = mode
    }
    
    fun addBypassRule(rule: BypassRule) = apply {
        bypassRules.add(rule)
    }
    
    fun addDomainRule(domain: String, matchType: DomainMatchType = DomainMatchType.FULL, action: RoutingAction = RoutingAction.PROXY) = apply {
        domainRules.add(DomainRule(domain, matchType, action))
    }
    
    fun addIpRule(cidr: String, action: RoutingAction = RoutingAction.DIRECT) = apply {
        ipRules.add(IpRule(cidr, action))
    }
    
    fun addAppRule(packageName: String, appName: String, action: RoutingAction = RoutingAction.PROXY) = apply {
        appRules.add(AppRule(packageName, appName, action))
    }
    
    fun addCustomRule(id: String, name: String, rule: String, action: RoutingAction = RoutingAction.PROXY) = apply {
        customRules.add(CustomRule(id, name, rule, action))
    }
    
    fun build(): RoutingRules {
        return RoutingRules(
            mode = mode,
            bypassRules = bypassRules.toList(),
            directRules = directRules.toList(),
            domainRules = domainRules.toList(),
            ipRules = ipRules.toList(),
            appRules = appRules.toList(),
            customRules = customRules.toList()
        )
    }
}

/**
 * Routing rule matcher
 */
object RoutingMatcher {
    
    /**
     * Check if domain matches any rule
     */
    fun matchDomain(domain: String, rules: List<DomainRule>): RoutingAction? {
        for (rule in rules) {
            val matches = when (rule.matchType) {
                DomainMatchType.FULL -> domain.equals(rule.domain, ignoreCase = true)
                DomainMatchType.SUFFIX -> domain.endsWith(rule.domain, ignoreCase = true)
                DomainMatchType.KEYWORD -> domain.contains(rule.domain, ignoreCase = true)
                DomainMatchType.REGEX -> domain.matches(rule.domain.toRegex())
            }
            
            if (matches) {
                return rule.action
            }
        }
        return null
    }
    
    /**
     * Check if IP matches any rule
     */
    fun matchIp(ip: String, rules: List<IpRule>): RoutingAction? {
        // Simplified IP matching - full implementation would parse CIDR
        for (rule in rules) {
            if (ipMatchesCidr(ip, rule.cidr)) {
                return rule.action
            }
        }
        return null
    }
    
    /**
     * Check if app matches any rule
     */
    fun matchApp(packageName: String, rules: List<AppRule>): RoutingAction? {
        return rules.find { it.packageName == packageName }?.action
    }
    
    /**
     * Simple CIDR matching (placeholder - full implementation needed)
     */
    private fun ipMatchesCidr(ip: String, cidr: String): Boolean {
        // This is a simplified version
        // Full implementation would parse CIDR and check IP ranges
        return ip.startsWith(cidr.substringBefore("/"))
    }
}

