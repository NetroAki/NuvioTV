package com.nuvio.tv.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import com.nuvio.tv.data.remote.StremioSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StremioSessionStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        profileDataStore: ProfileDataStore,
    ) : ProfileScopedCredentialStore {
        private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        private val json = Json { ignoreUnknownKeys = true }
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val lock = Any()
        private val _state = MutableStateFlow(StremioSessionState())

        @Volatile
        private var active = ActiveSession(profileId = 1, generation = 0, session = null)

        val state: StateFlow<StremioSessionState> = _state.asStateFlow()

        init {
            load(1)
            scope.launch {
                profileDataStore.activeProfileId
                    .distinctUntilChanged()
                    .collect { profileId ->
                        if (profileId != active.profileId) load(profileId)
                    }
            }
        }

        fun currentScope(): StremioSessionScope = active.scope()

        fun currentSession(scope: StremioSessionScope = currentScope()): StremioSession? {
            val current = active
            return current.session?.takeIf { current.scope() == scope }
        }

        fun save(
            session: StremioSession,
            scope: StremioSessionScope,
        ): Boolean =
            synchronized(lock) {
                val current = active
                if (current.scope() != scope) return@synchronized false
                val stored = StoredSession(authKey = session.authKey, email = session.email)
                preferences
                    .edit()
                    .putString(profileKey(current.profileId), encrypt(json.encodeToString(stored)))
                    .apply()
                active = current.copy(session = session)
                publish()
                true
            }

        fun clear(scope: StremioSessionScope = currentScope()): Boolean =
            synchronized(lock) {
                val current = active
                if (current.scope() != scope) return@synchronized false
                preferences.edit().remove(profileKey(current.profileId)).apply()
                active = current.copy(generation = current.generation + 1, session = null)
                publish()
                true
            }

        override fun removeProfile(profileId: Int) {
            preferences.edit().remove(profileKey(profileId)).apply()
            synchronized(lock) {
                if (profileId == active.profileId) {
                    active = active.copy(generation = active.generation + 1, session = null)
                    publish()
                }
            }
        }

        override fun clearAllProfiles() {
            preferences.edit().clear().apply()
            synchronized(lock) {
                active = active.copy(generation = active.generation + 1, session = null)
                publish()
            }
        }

        private fun load(profileId: Int) {
            val session =
                preferences
                    .getString(profileKey(profileId), null)
                    ?.let { encoded ->
                        runCatching {
                            json.decodeFromString<StoredSession>(decrypt(encoded)).toSession()
                        }.onFailure {
                            preferences.edit().remove(profileKey(profileId)).apply()
                        }.getOrNull()
                    }
            synchronized(lock) {
                val generation = if (profileId == active.profileId) active.generation else active.generation + 1
                active = ActiveSession(profileId = profileId, generation = generation, session = session)
                publish()
            }
        }

        private fun publish() {
            val session = active.session
            _state.value =
                StremioSessionState(
                    connected = session != null,
                    email = session?.email,
                )
        }

        private fun encrypt(value: String): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            return "${cipher.iv.toBase64()}.${cipher.doFinal(value.toByteArray()).toBase64()}"
        }

        private fun decrypt(value: String): String {
            val separator = value.indexOf('.')
            require(separator > 0 && separator < value.lastIndex)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_BITS, value.substring(0, separator).fromBase64()),
            )
            return cipher.doFinal(value.substring(separator + 1).fromBase64()).toString(Charsets.UTF_8)
        }

        private fun getOrCreateSecretKey(): SecretKey {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
                init(
                    KeyGenParameterSpec
                        .Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .build(),
                )
                generateKey()
            }
        }

        private fun profileKey(profileId: Int): String = "session.p$profileId"

        private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

        private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

        private data class ActiveSession(
            val profileId: Int,
            val generation: Long,
            val session: StremioSession?,
        ) {
            fun scope(): StremioSessionScope = StremioSessionScope(profileId, generation)
        }

        @Serializable
        private data class StoredSession(
            val authKey: String,
            val email: String,
        ) {
            fun toSession(): StremioSession = StremioSession(authKey = authKey, email = email)
        }

        private companion object {
            const val PREFERENCES_NAME = "nuvio_stremio_auth"
            const val KEYSTORE_PROVIDER = "AndroidKeyStore"
            const val KEY_ALIAS = "com.nuvio.tv.stremio.credentials.v1"
            const val TRANSFORMATION = "AES/GCM/NoPadding"
            const val GCM_TAG_BITS = 128
        }
    }

data class StremioSessionState(
    val connected: Boolean = false,
    val email: String? = null,
)

data class StremioSessionScope(
    val profileId: Int,
    val generation: Long,
)
