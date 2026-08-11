package com.nuvio.tv.core.sync.stremio

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

internal data class StremioEpisodeReference(
    val videoId: String,
    val season: Int,
    val episode: Int,
    val title: String? = null,
)

internal object StremioWatchedField {
    fun decode(
        serialized: String?,
        videos: List<StremioEpisodeReference>,
    ): List<StremioEpisodeReference> {
        if (serialized.isNullOrBlank() || videos.isEmpty()) return emptyList()
        return runCatching { decodeValid(serialized, videos) }.getOrDefault(emptyList())
    }

    fun update(
        serialized: String?,
        videos: List<StremioEpisodeReference>,
        videoId: String,
        watched: Boolean,
    ): String? {
        val selectedIndex = videos.indexOfFirst { it.videoId == videoId }
        if (selectedIndex < 0) return serialized
        val watchedIds = decode(serialized, videos).mapTo(mutableSetOf()) { it.videoId }
        if (watched) watchedIds += videoId else watchedIds -= videoId
        val values = ByteArray((videos.size + 7) / 8)
        videos.forEachIndexed { index, video ->
            if (video.videoId in watchedIds) values.setBit(index)
        }
        val anchorIndex = videos.indices.lastOrNull { values.bitAt(it) } ?: 0
        return "${videos[anchorIndex].videoId}:${anchorIndex + 1}:${deflate(values)}"
    }

    private fun decodeValid(
        serialized: String,
        videos: List<StremioEpisodeReference>,
    ): List<StremioEpisodeReference> {
        val components = serialized.split(':')
        require(components.size >= 3)
        val anchorLength = components[components.lastIndex - 1].toInt()
        val anchorVideoId = components.dropLast(2).joinToString(":")
        val anchorIndex = videos.indexOfFirst { it.videoId == anchorVideoId }
        if (anchorIndex < 0) return emptyList()

        val values = inflate(components.last())
        val offset = anchorLength - anchorIndex - 1
        return videos.filterIndexed { index, _ -> values.bitAt(index + offset) }
    }

    private fun inflate(encoded: String): ByteArray {
        val compressed = encoded.decodeBase64()?.toByteArray() ?: error("Invalid watched bitfield encoding")
        return InflaterInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
    }

    private fun deflate(values: ByteArray): String {
        val output = ByteArrayOutputStream()
        DeflaterOutputStream(output, Deflater(6)).use { it.write(values) }
        return output.toByteArray().toByteString().base64()
    }

    private fun ByteArray.setBit(index: Int) {
        val byteIndex = index / 8
        this[byteIndex] = (this[byteIndex].toInt() or (1 shl (index % 8))).toByte()
    }

    private fun ByteArray.bitAt(index: Int): Boolean =
        index >= 0 && index / 8 < size &&
            ((this[index / 8].toInt() ushr (index % 8)) and 1) == 1
}
