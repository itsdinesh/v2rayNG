package com.v2ray.ang.root

import org.junit.Assert.assertEquals
import org.junit.Test

class RootProxyManagerTest {

    @Test
    fun `single quotes are escaped in YAML scalars`() {
        assertEquals("'user''name'", "user'name".toSingleQuotedYamlScalar())
        assertEquals("'pa''''ss'", "pa''ss".toSingleQuotedYamlScalar())
    }

    @Test
    fun `multiline credentials remain inside the YAML scalar`() {
        val credential = "user\nHEVCFG\ntouch /data/local/tmp/injected"

        assertEquals("'user\nHEVCFG\ntouch /data/local/tmp/injected'", credential.toSingleQuotedYamlScalar())
    }

    @Test
    fun `buildBlockRules returns empty string when uids is empty`() {
        assertEquals("", RootProxyManager.buildBlockRules(emptyList()))
    }

    @Test
    fun `buildBlockRules creates chains and adds rules for each uid`() {
        val rules = RootProxyManager.buildBlockRules(listOf("10050", "10051"))
        org.junit.Assert.assertTrue(rules.contains("iptables -t filter -N CORE_BLOCK 2>/dev/null || true"))
        org.junit.Assert.assertTrue(rules.contains("iptables -t filter -F CORE_BLOCK"))
        org.junit.Assert.assertTrue(rules.contains("ip6tables -t filter -N CORE6_BLOCK 2>/dev/null || true"))
        org.junit.Assert.assertTrue(rules.contains("ip6tables -t filter -F CORE6_BLOCK"))
        org.junit.Assert.assertTrue(rules.contains("iptables -t filter -A CORE_BLOCK -m owner --uid-owner 10050 -j REJECT --reject-with icmp-admin-prohibited 2>/dev/null || iptables -t filter -A CORE_BLOCK -m owner --uid-owner 10050 -j DROP"))
        org.junit.Assert.assertTrue(rules.contains("ip6tables -t filter -A CORE6_BLOCK -m owner --uid-owner 10050 -j REJECT --reject-with icmp6-adm-prohibited 2>/dev/null || ip6tables -t filter -A CORE6_BLOCK -m owner --uid-owner 10050 -j DROP"))
        org.junit.Assert.assertTrue(rules.contains("iptables -t filter -A OUTPUT -j CORE_BLOCK"))
        org.junit.Assert.assertTrue(rules.contains("ip6tables -t filter -A OUTPUT -j CORE6_BLOCK"))
    }
}
