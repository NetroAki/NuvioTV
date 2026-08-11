package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.domain.repository.MetaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioWatchedStateResolver
    @Inject
    constructor(
        private val metaRepository: MetaRepository,
    ) {
        suspend fun resolve(item: StremioLibraryItemDto): List<WatchedItem> {
            if (item.state.watched.isNullOrBlank()) return item.toWatchedItems()
            val videos = videosFor(item)
            return item.toWatchedItems(videos)
        }

        internal suspend fun videosFor(item: StremioLibraryItemDto): List<StremioEpisodeReference> {
            val result =
                withTimeoutOrNull(METADATA_TIMEOUT_MS) {
                    metaRepository
                        .getMetaFromPrimaryAddon(item.type, item.id)
                        .first { it !is NetworkResult.Loading }
                }
            val meta = (result as? NetworkResult.Success)?.data ?: return emptyList()
            return meta.videos.mapNotNull { video ->
                val season = video.season ?: return@mapNotNull null
                val episode = video.episode ?: return@mapNotNull null
                StremioEpisodeReference(
                    videoId = video.id,
                    season = season,
                    episode = episode,
                    title = video.title,
                )
            }
        }

        private companion object {
            const val METADATA_TIMEOUT_MS = 8_000L
        }
    }
