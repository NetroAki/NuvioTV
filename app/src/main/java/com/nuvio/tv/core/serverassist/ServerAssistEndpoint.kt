package com.nuvio.tv.core.serverassist

import java.net.URI

object ServerAssistEndpoint {
    fun normalize(raw: String): Result<String> =
        runCatching {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "Server endpoint is required" }
            val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase()
            require(scheme == "http" || scheme == "https") { "Only HTTP and HTTPS are supported" }
            require(!uri.host.isNullOrBlank()) { "Server endpoint must include a host" }
            require(uri.userInfo == null) { "Credentials are not allowed in the endpoint" }
            require(uri.query == null && uri.fragment == null) { "Query strings and fragments are not allowed" }
            require(uri.path.isNullOrEmpty() || uri.path == "/") { "Server endpoint must not contain a path" }
            require(uri.port in -1..65535 && uri.port != 0) { "Server endpoint port is invalid" }

            URI(scheme, null, uri.host.lowercase(), uri.port, "/", null, null).toASCIIString()
        }
}
