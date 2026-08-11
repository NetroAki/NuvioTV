package com.nuvio.tv.core.sync.stremio

import android.util.Log
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.StremioSessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioAccountAutoSync
    @Inject
    constructor(
        private val profileManager: ProfileManager,
        private val sessionStore: StremioSessionStore,
        private val synchronizer: StremioAccountSynchronizer,
    ) {
        private val started = AtomicBoolean(false)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                profileManager.activeProfileId.collect { profileId ->
                    delay(PROFILE_SETTLE_DELAY_MS)
                    val sessionScope = sessionStore.currentScope()
                    if (sessionScope.profileId != profileId || sessionStore.currentSession(sessionScope) == null) return@collect
                    synchronizer.refresh().onFailure { error ->
                        Log.w(TAG, "Automatic Stremio account sync failed for profile $profileId", error)
                    }
                }
            }
        }

        private companion object {
            const val TAG = "StremioAccountSync"
            const val PROFILE_SETTLE_DELAY_MS = 250L
        }
    }
