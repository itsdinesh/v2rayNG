package com.v2ray.ang.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSelectionTest {

    @Test
    fun `invert selection adds unselected and removes selected`() {
        val current = setOf("com.app.one", "com.app.two")
        val all = listOf("com.app.one", "com.app.two", "com.app.three")
        val result = AppSelection.invert(current, all)
        assertEquals(setOf("com.app.three"), result)
    }

    @Test
    fun `fromProxyList selects proxied apps in proxy or block mode`() {
        val packages = listOf("com.example.one", "com.example.two", "org.test.three")
        val proxyList = "com.example.one\norg.test.three"

        val selected = AppSelection.fromProxyList(
            packageNames = packages,
            proxyAppList = proxyList,
            bypassApps = false,
            forceGoogleApps = false
        )

        assertEquals(setOf("com.example.one", "org.test.three"), selected)
    }

    @Test
    fun `fromProxyList inverts selection in bypass mode`() {
        val packages = listOf("com.example.one", "com.example.two", "org.test.three")
        val proxyList = "com.example.one\norg.test.three"

        val selected = AppSelection.fromProxyList(
            packageNames = packages,
            proxyAppList = proxyList,
            bypassApps = true,
            forceGoogleApps = false
        )

        assertEquals(setOf("com.example.two"), selected)
    }

    @Test
    fun `fromProxyList handles forceGoogleApps correctly`() {
        val packages = listOf("com.google.android.gms", "com.google.android.webview", "com.other.app")
        val proxyList = ""

        val selected = AppSelection.fromProxyList(
            packageNames = packages,
            proxyAppList = proxyList,
            bypassApps = false,
            forceGoogleApps = true
        )

        assertTrue(selected.contains("com.google.android.gms"))
        assertTrue(!selected.contains("com.google.android.webview"))
        assertTrue(!selected.contains("com.other.app"))
    }
}
