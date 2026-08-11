package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.local.StremioSessionScope
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.remote.StremioAccountClient
import com.nuvio.tv.data.remote.StremioSession
import com.nuvio.tv.data.repository.AddonRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioAccountSynchronizerTest {
    private val accountClient = mockk<StremioAccountClient>()
    private val sessionStore = mockk<StremioSessionStore>()
    private val addonRepository = mockk<AddonRepositoryImpl>()
    private val librarySync = mockk<StremioLibrarySync>()
    private val playbackStateSync = mockk<StremioPlaybackStateSync>()
    private val importer =
        StremioAccountSynchronizer(
            accountClient,
            sessionStore,
            addonRepository,
            librarySync,
            playbackStateSync,
        )

    @Test
    fun `connect stores session and imports remote order`() =
        runTest {
            val scope = StremioSessionScope(profileId = 2, generation = 4)
            val session = StremioSession(authKey = "private-key", email = "viewer@example.com")
            val urls = listOf("https://first.example/manifest.json", "https://second.example/manifest.json")
            every { sessionStore.currentScope() } returns scope
            every { sessionStore.save(session, scope) } returns true
            coEvery { accountClient.login("viewer@example.com", "password") } returns Result.success(session)
            coEvery { accountClient.getAddonUrls("private-key") } returns Result.success(urls)
            coEvery { accountClient.getLibraryItems("private-key") } returns Result.success(emptyList())
            coEvery { addonRepository.reconcileWithRemoteAddonUrls(urls, true) } returns Unit
            coEvery { librarySync.reconcile("private-key", scope, emptyList()) } returns
                StremioLibrarySyncResult(importedItems = 0, removedItems = 0, pushedChanges = 0)
            coEvery { playbackStateSync.import(scope, emptyList()) } returns
                StremioPlaybackStateSyncResult(importedProgress = 0, importedWatchedItems = 0)

            val result = importer.connectAndImport("viewer@example.com", "password").getOrThrow()

            assertEquals(2, result.addonCount)
            coVerify(exactly = 1) { addonRepository.reconcileWithRemoteAddonUrls(urls, true) }
        }

    @Test
    fun `profile change prevents addon mutation`() =
        runTest {
            val initialScope = StremioSessionScope(profileId = 1, generation = 2)
            val changedScope = StremioSessionScope(profileId = 3, generation = 3)
            val session = StremioSession(authKey = "private-key", email = "viewer@example.com")
            every { sessionStore.currentScope() } returnsMany listOf(initialScope, changedScope)
            coEvery { accountClient.login(any(), any()) } returns Result.success(session)

            val result = importer.connectAndImport("viewer@example.com", "password")

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { addonRepository.reconcileWithRemoteAddonUrls(any(), any()) }
        }

    @Test
    fun `failed remote logout keeps encrypted session for retry`() =
        runTest {
            val scope = StremioSessionScope(profileId = 1, generation = 2)
            val session = StremioSession(authKey = "private-key", email = "viewer@example.com")
            every { sessionStore.currentScope() } returns scope
            every { sessionStore.currentSession(scope) } returns session
            coEvery { accountClient.logout("private-key") } returns Result.failure(IllegalStateException("offline"))

            val result = importer.disconnect()

            assertTrue(result.isFailure)
            verify(exactly = 0) { sessionStore.clear(any()) }
        }
}
