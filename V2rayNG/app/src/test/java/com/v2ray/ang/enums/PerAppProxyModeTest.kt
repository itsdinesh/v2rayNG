package com.v2ray.ang.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class PerAppProxyModeTest {

    @Test
    fun `fromValue parses valid strings`() {
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromValue("proxy"))
        assertEquals(PerAppProxyMode.BYPASS, PerAppProxyMode.fromValue("bypass"))
        assertEquals(PerAppProxyMode.BLOCK, PerAppProxyMode.fromValue("block"))
    }

    @Test
    fun `fromValue handles case insensitivity and whitespace`() {
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromValue(" Proxy "))
        assertEquals(PerAppProxyMode.BYPASS, PerAppProxyMode.fromValue("BYPASS"))
        assertEquals(PerAppProxyMode.BLOCK, PerAppProxyMode.fromValue("Block"))
    }

    @Test
    fun `fromValue falls back to PROXY on invalid or null input`() {
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromValue(null))
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromValue(""))
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromValue("unknown"))
    }

    @Test
    fun `fromLegacy correctly maps legacy bypass boolean`() {
        assertEquals(PerAppProxyMode.BYPASS, PerAppProxyMode.fromLegacy(true))
        assertEquals(PerAppProxyMode.PROXY, PerAppProxyMode.fromLegacy(false))
    }

    @Test
    fun `serialized string values match expected keys`() {
        assertEquals("proxy", PerAppProxyMode.PROXY.value)
        assertEquals("bypass", PerAppProxyMode.BYPASS.value)
        assertEquals("block", PerAppProxyMode.BLOCK.value)
    }
}
