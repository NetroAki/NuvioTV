package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.local.LibraryPreferences
import com.nuvio.tv.data.local.StremioSessionScope
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.local.StremioSyncStateStore
import com.nuvio.tv.data.remote.StremioAccountClient
import com.nuvio.tv.data.remote.dto.StremioLibraryItemDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StremioLibrarySyncTest {
    private val accountClient = mockk<StremioAccountClient>()
    private val preferences = mockk<LibraryPreferences>()
    private val stateStore = mockk<StremioSyncStateStore>()
    private val sessionStore = mockk<StremioSessionStore>()
    private val sync = StremioLibrarySync(accountClient, preferences, stateStore, sessionStore)
    private val scope = StremioSessionScope(profileId = 3, generation = 2)

    @Test
    fun `new remote library item is imported without deleting local state`() =
        runTest {
            val remote = libraryItem()
            every { sessionStore.currentScope() } returns scope
            coEvery { stateStore.getLibraryKeys(3) } returns emptySet()
            coEvery { preferences.getAllItems(3) } returns emptyList()
            coEvery { preferences.addItem(any(), 3) } returns Unit
            coEvery { stateStore.setLibraryKeys(3, setOf("movie:tt1234567")) } returns Unit

            val result = sync.reconcile("auth-key", scope, listOf(remote))

            assertEquals(1, result.importedItems)
            coVerify(exactly = 1) { preferences.addItem(match { it.id == "tt1234567" }, 3) }
            coVerify(exactly = 0) { accountClient.putLibraryItems(any(), any()) }
        }

    @Test
    fun `local removal after a prior sync is pushed to Stremio`() =
        runTest {
            val remote = libraryItem()
            every { sessionStore.currentScope() } returns scope
            coEvery { stateStore.getLibraryKeys(3) } returns setOf("movie:tt1234567")
            coEvery { preferences.getAllItems(3) } returns emptyList()
            coEvery { accountClient.putLibraryItems("auth-key", any()) } returns Result.success(Unit)
            coEvery { stateStore.setLibraryKeys(3, emptySet()) } returns Unit

            val result = sync.reconcile("auth-key", scope, listOf(remote))

            assertEquals(1, result.pushedChanges)
            coVerify {
                accountClient.putLibraryItems(
                    "auth-key",
                    match { changes -> changes.single().removed },
                )
            }
            assertTrue(result.importedItems == 0)
        }

    private fun libraryItem() =
        StremioLibraryItemDto(
            id = "tt1234567",
            name = "Example",
            type = "movie",
            modifiedAt = "2026-08-11T12:00:00Z",
        )
}
