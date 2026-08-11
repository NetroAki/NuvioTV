package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTrackSelectionPolicyTest {
    @Test
    fun `preferred language never chooses commentary when a normal track exists`() {
        val tracks =
            listOf(
                track(0, "English Director Commentary (AAC 2.0)", "eng"),
                track(1, "English (E-AC3 5.1)", "eng"),
                track(2, "Japanese (AAC 2.0)", "jpn"),
            )

        val selected = AudioTrackSelectionPolicy.select(tracks, AudioTrackPreference(listOf("eng")))

        assertEquals(1, selected)
    }

    @Test
    fun `audio description is rejected even when it appears before the main mix`() {
        val tracks =
            listOf(
                track(0, "English Audio Description", "en"),
                track(1, "English Dolby Atmos", "en"),
            )

        assertEquals(1, AudioTrackSelectionPolicy.select(tracks, AudioTrackPreference(listOf("en"))))
    }

    @Test
    fun `remembered semantic label survives codec and channel changes`() {
        val tracks =
            listOf(
                track(0, "Japanese Commentary (AAC Stereo)", "ja"),
                track(1, "Japanese Main (TrueHD 7.1)", "ja"),
            )

        val selected =
            AudioTrackSelectionPolicy.select(
                tracks,
                AudioTrackPreference(languages = listOf("jpn"), label = "Japanese Main (AAC 2.0)"),
            )

        assertEquals(1, selected)
    }

    private fun track(
        index: Int,
        name: String,
        language: String,
    ) = TrackInfo(index = index, name = name, language = language)
}
