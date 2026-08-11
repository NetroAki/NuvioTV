package com.nuvio.tv.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServerAssistHealthEnvelope(
    val apiVersion: Int,
    val data: ServerAssistHealthDto,
)

@JsonClass(generateAdapter = true)
data class ServerAssistHealthDto(
    val status: String,
    val serverVersion: String,
)

@JsonClass(generateAdapter = true)
data class ServerAssistCapabilitiesEnvelope(
    val apiVersion: Int,
    val data: ServerAssistCapabilitiesDto,
)

@JsonClass(generateAdapter = true)
data class ServerAssistCapabilitiesDto(
    val apiVersions: List<Int>,
    val transport: ServerAssistTransportDto,
    val system: ServerAssistSystemDto,
    val media: ServerAssistMediaDto,
)

@JsonClass(generateAdapter = true)
data class ServerAssistTransportDto(
    @param:Json(name = "interface") val interfaceName: String,
    val addressFamily: String,
    val tailnetOnly: Boolean,
)

@JsonClass(generateAdapter = true)
data class ServerAssistSystemDto(
    val logicalCpus: Int,
)

@JsonClass(generateAdapter = true)
data class ServerAssistMediaDto(
    val ffmpegAvailable: Boolean,
    val ffprobeAvailable: Boolean,
    val hardwareAccelerators: List<String>,
)
