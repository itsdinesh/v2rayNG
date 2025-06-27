package com.v2ray.ang2.dto

data class ConfigResult(
    var status: Boolean,
    var guid: String? = null,
    var content: String = "",
    var socksPort: Int? = null,
)

