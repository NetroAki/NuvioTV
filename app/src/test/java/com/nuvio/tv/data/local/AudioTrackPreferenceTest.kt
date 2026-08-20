package com.nuvio.tv.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTrackPreferenceTest {
    private val series = PersistedAudioTrackPreference(language = "en", name = "English")
    private val episode = PersistedAudioTrackPreference(language = "ja", name = "Japanese")

    @Test
    fun `episode preference overrides series preference`() {
        assertEquals(
            ScopedAudioTrackPreference(episode, AudioTrackPreferenceScope.EPISODE),
            resolveScopedAudioPreference(episode, series),
        )
    }

    @Test
    fun `series preference is used without episode override`() {
        assertEquals(
            ScopedAudioTrackPreference(series, AudioTrackPreferenceScope.SERIES),
            resolveScopedAudioPreference(null, series),
        )
    }

    @Test
    fun `no saved choice returns no preference`() {
        assertNull(resolveScopedAudioPreference(null, null))
    }

    @Test
    fun `episode scope requires series content and a video id`() {
        assertTrue(supportsEpisodeAudioPreference("Series", "tt123:1:2"))
        assertTrue(supportsEpisodeAudioPreference("TV", "episode-2"))
        assertFalse(supportsEpisodeAudioPreference("movie", "tt123"))
        assertFalse(supportsEpisodeAudioPreference("series", null))
    }
}
