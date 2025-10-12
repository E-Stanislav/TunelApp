package com.tunelapp

import com.tunelapp.utils.Utils
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Utils
 */
class UtilsTest {
    
    @Test
    fun `format bytes correctly`() {
        assertEquals("0 B", Utils.formatBytes(0))
        assertEquals("512 B", Utils.formatBytes(512))
        assertTrue(Utils.formatBytes(1024).contains("KB"))
        assertTrue(Utils.formatBytes(1024 * 1024).contains("MB"))
        assertTrue(Utils.formatBytes(1024L * 1024 * 1024).contains("GB"))
    }
    
    @Test
    fun `format speed correctly`() {
        assertEquals("0 B/s", Utils.formatSpeed(0))
        assertTrue(Utils.formatSpeed(1024).contains("KB/s"))
        assertTrue(Utils.formatSpeed((5.5 * 1024 * 1024).toLong()).contains("MB/s"))
    }
    
    @Test
    fun `format duration correctly`() {
        assertEquals("0:00", Utils.formatDuration(0))
        assertEquals("0:05", Utils.formatDuration(5000))
        assertEquals("1:00", Utils.formatDuration(60000))
        assertEquals("1:05", Utils.formatDuration(65000))
        assertEquals("1:00:00", Utils.formatDuration(3600000))
        assertEquals("1:30:45", Utils.formatDuration(5445000))
    }
    
    @Test
    fun `validate UUID correctly`() {
        assertTrue(Utils.isValidUUID("550e8400-e29b-41d4-a716-446655440000"))
        assertTrue(Utils.isValidUUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
        assertFalse(Utils.isValidUUID("not-a-uuid"))
        assertFalse(Utils.isValidUUID("550e8400-e29b-41d4-a716"))
        assertFalse(Utils.isValidUUID(""))
    }
    
    @Test
    fun `validate host correctly`() {
        assertTrue(Utils.isValidHost("example.com"))
        assertTrue(Utils.isValidHost("sub.example.com"))
        assertTrue(Utils.isValidHost("192.168.1.1"))
        assertTrue(Utils.isValidHost("10.0.0.1"))
        assertFalse(Utils.isValidHost(""))
        assertFalse(Utils.isValidHost("invalid host!"))
    }
    
    @Test
    fun `validate port correctly`() {
        assertTrue(Utils.isValidPort(1))
        assertTrue(Utils.isValidPort(80))
        assertTrue(Utils.isValidPort(443))
        assertTrue(Utils.isValidPort(65535))
        assertFalse(Utils.isValidPort(0))
        assertFalse(Utils.isValidPort(-1))
        assertFalse(Utils.isValidPort(65536))
    }
}

