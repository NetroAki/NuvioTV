package com.nuvio.tv.data.remote

import com.nuvio.tv.core.serverassist.ServerAssistEndpoint
import com.nuvio.tv.data.remote.dto.ServerAssistCapabilitiesEnvelope
import com.nuvio.tv.data.remote.dto.ServerAssistHealthEnvelope
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val SUPPORTED_API_VERSION = 1
private const val MAX_RESPONSE_BYTES = 64L * 1024L

@Singleton
class ServerAssistClient
    @Inject
    constructor(
        @param:Named("serverAssist") private val httpClient: OkHttpClient,
        moshi: Moshi,
    ) {
        private val healthAdapter = moshi.adapter(ServerAssistHealthEnvelope::class.java)
        private val capabilitiesAdapter = moshi.adapter(ServerAssistCapabilitiesEnvelope::class.java)

        suspend fun probe(rawEndpoint: String): Result<ServerAssistProbe> =
            runCatching {
                withContext(Dispatchers.IO) {
                    val endpoint = ServerAssistEndpoint.normalize(rawEndpoint).getOrThrow()
                    val health =
                        request(endpoint, "v1/health") { json ->
                            healthAdapter.fromJson(json) ?: error("Server returned an empty health response")
                        }
                    require(health.apiVersion == SUPPORTED_API_VERSION) {
                        "Server API ${health.apiVersion} is not supported"
                    }
                    require(health.data.status == "ok") { "Server is not ready" }

                    val capabilities =
                        request(endpoint, "v1/capabilities") { json ->
                            capabilitiesAdapter.fromJson(json) ?: error("Server returned empty capabilities")
                        }
                    require(capabilities.apiVersion == SUPPORTED_API_VERSION) {
                        "Server API ${capabilities.apiVersion} is not supported"
                    }
                    require(capabilities.data.transport.tailnetOnly) {
                        "Server did not report a tailnet-only transport"
                    }

                    ServerAssistProbe(
                        endpoint = endpoint,
                        serverVersion = health.data.serverVersion,
                        logicalCpus = capabilities.data.system.logicalCpus,
                        ffmpegAvailable = capabilities.data.media.ffmpegAvailable,
                        ffprobeAvailable = capabilities.data.media.ffprobeAvailable,
                        hardwareAccelerators = capabilities.data.media.hardwareAccelerators,
                    )
                }
            }

        private fun <T> request(
            endpoint: String,
            path: String,
            decode: (String) -> T,
        ): T {
            val request =
                Request
                    .Builder()
                    .url(endpoint + path)
                    .get()
                    .build()
            return httpClient.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "Server returned HTTP ${response.code}" }
                val bytes = response.body.source().readByteArray(MAX_RESPONSE_BYTES + 1)
                require(bytes.size <= MAX_RESPONSE_BYTES) { "Server response exceeded the size limit" }
                decode(String(bytes, StandardCharsets.UTF_8))
            }
        }
    }

data class ServerAssistProbe(
    val endpoint: String,
    val serverVersion: String,
    val logicalCpus: Int,
    val ffmpegAvailable: Boolean,
    val ffprobeAvailable: Boolean,
    val hardwareAccelerators: List<String>,
)
