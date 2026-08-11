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

class StremioAddonImporterTest {
    private val accountClient = mockk<StremioAccountClient>()
    private val sessionStore = mockk<StremioSessionStore>()
    private val addonRepository = mockk<AddonRepositoryImpl>()
    private val importer = StremioAddonImporter(accountClient, sessionStore, addonRepository)

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
            coEvery { addonRepository.reconcileWithRemoteAddonUrls(urls, true) } returns Unit

            val result = importer.connectAndImport("viewer@example.com", "password").getOrThrow()

            assertEquals(2, result.importedAddons)
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
