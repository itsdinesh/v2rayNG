package com.v2ray.ang.enums

/**
 * Operating modes for per-app proxy routing.
 */
enum class PerAppProxyMode(val value: String) {
    /** Checked apps use proxy, unchecked apps connect directly. */
    PROXY("proxy"),

    /** Checked apps connect directly, unchecked apps use proxy. */
    BYPASS("bypass"),

    /** Checked apps are completely blocked from network access, unchecked apps use proxy. */
    BLOCK("block");

    companion object {
        fun fromValue(value: String?): PerAppProxyMode {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PROXY
        }
    }
}
