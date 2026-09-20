package com.v2ray.ang.enums

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoutingActionTest {

    @Test
    fun `fromValue parses valid strings`() {
        assertEquals(AppRoutingAction.PROXY, AppRoutingAction.fromValue("proxy"))
        assertEquals(AppRoutingAction.DIRECT, AppRoutingAction.fromValue("direct"))
        assertEquals(AppRoutingAction.BLOCK, AppRoutingAction.fromValue("block"))
    }

    @Test
    fun `fromValue handles case insensitivity and whitespace`() {
        assertEquals(AppRoutingAction.PROXY, AppRoutingAction.fromValue(" Proxy "))
        assertEquals(AppRoutingAction.DIRECT, AppRoutingAction.fromValue("DIRECT"))
        assertEquals(AppRoutingAction.BLOCK, AppRoutingAction.fromValue("Block"))
    }

    @Test
    fun `fromValue falls back to PROXY on invalid or null input`() {
        assertEquals(AppRoutingAction.PROXY, AppRoutingAction.fromValue(null))
        assertEquals(AppRoutingAction.PROXY, AppRoutingAction.fromValue(""))
        assertEquals(AppRoutingAction.PROXY, AppRoutingAction.fromValue("unknown"))
    }

    @Test
    fun `serialized string values match expected keys`() {
        assertEquals("proxy", AppRoutingAction.PROXY.value)
        assertEquals("direct", AppRoutingAction.DIRECT.value)
        assertEquals("block", AppRoutingAction.BLOCK.value)
    }
}
