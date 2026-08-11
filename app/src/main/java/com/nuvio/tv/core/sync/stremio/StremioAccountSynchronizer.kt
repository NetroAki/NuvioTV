package com.nuvio.tv.core.sync.stremio

import com.nuvio.tv.data.local.StremioSessionScope
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.remote.StremioAccountClient
import com.nuvio.tv.data.remote.StremioAccountException
import com.nuvio.tv.data.remote.StremioSession
import com.nuvio.tv.data.repository.AddonRepositoryImpl
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioAccountSynchronizer
    @Inject
    constructor(
        private val accountClient: StremioAccountClient,
        private val sessionStore: StremioSessionStore,
        private val addonRepository: AddonRepositoryImpl,
        private val librarySync: StremioLibrarySync,
        private val playbackStateSync: StremioPlaybackStateSync,
    ) {
        private val syncMutex = Mutex()

        suspend fun connectAndImport(
            email: String,
            password: String,
        ): Result<StremioAccountSyncResult> =
            syncMutex.withLock {
                runCatching {
                    val scope = sessionStore.currentScope()
                    val session = accountClient.login(email, password).getOrThrow()
                    ensureCurrent(scope)
                    if (!sessionStore.save(session, scope)) throw profileChanged()
                    import(session, scope)
                }
            }

        suspend fun refresh(): Result<StremioAccountSyncResult> =
            syncMutex.withLock {
                runCatching {
                    val scope = sessionStore.currentScope()
                    val session =
                        sessionStore.currentSession(scope)
                            ?: throw StremioAccountException("Connect a Stremio account first")
                    import(session, scope)
                }
            }

        suspend fun disconnect(): Result<Unit> =
            syncMutex.withLock {
                runCatching {
                    val scope = sessionStore.currentScope()
                    val session = sessionStore.currentSession(scope) ?: return@runCatching
                    accountClient.logout(session.authKey).getOrThrow()
                    ensureCurrent(scope)
                    if (!sessionStore.clear(scope)) throw profileChanged()
                }
            }

        private suspend fun import(
            session: StremioSession,
            scope: StremioSessionScope,
        ): StremioAccountSyncResult {
            val urls = accountClient.getAddonUrls(session.authKey).getOrThrow()
            val remoteLibrary = accountClient.getLibraryItems(session.authKey).getOrThrow()
            ensureCurrent(scope)
            addonRepository.reconcileWithRemoteAddonUrls(
                remoteUrls = urls,
                removeMissingLocal = true,
            )
            val libraryResult = librarySync.reconcile(session.authKey, scope, remoteLibrary)
            val playbackResult = playbackStateSync.import(scope, remoteLibrary)
            return StremioAccountSyncResult(
                email = session.email,
                addonCount = urls.size,
                importedLibraryItems = libraryResult.importedItems,
                removedLibraryItems = libraryResult.removedItems,
                pushedLibraryChanges = libraryResult.pushedChanges,
                importedProgressItems = playbackResult.importedProgress,
                importedWatchedItems = playbackResult.importedWatchedItems,
            )
        }

        private fun ensureCurrent(scope: StremioSessionScope) {
            if (sessionStore.currentScope() != scope) throw profileChanged()
        }

        private fun profileChanged(): StremioAccountException =
            StremioAccountException("The active profile changed during Stremio sync; try again")
    }
