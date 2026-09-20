package com.v2ray.ang.enums

enum class AppRoutingAction(val value: String) {
    PROXY("proxy"),
    DIRECT("direct"),
    BLOCK("block");

    companion object {
        fun fromValue(value: String?): AppRoutingAction {
            return entries.firstOrNull { it.value.equals(value?.trim(), ignoreCase = true) } ?: PROXY
        }
    }
}
