package com.nuvio.tv.core.di

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.profile.ProfileScopedCredentialStore
import com.nuvio.tv.core.sync.stremio.StremioTrackingHistoryWriter
import com.nuvio.tv.core.sync.stremio.StremioTrackingProvider
import com.nuvio.tv.core.tracking.TrackingHistoryWriter
import com.nuvio.tv.core.tracking.TrackingProvider
import com.nuvio.tv.data.local.StremioSessionStore
import com.nuvio.tv.data.remote.api.StremioAccountApi
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StremioAccountModule {
    @Provides
    @Singleton
    @Named("stremioAccount")
    fun provideStremioAccountHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                val version = BuildConfig.VERSION_NAME.ifBlank { "dev" }
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", "Nuvio/$version stremio-account")
                        .header("Accept", "application/json")
                        .build(),
                )
            }.build()

    @Provides
    @Singleton
    @Named("stremioAccount")
    fun provideStremioAccountRetrofit(
        @Named("stremioAccount") client: OkHttpClient,
        moshi: Moshi,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.strem.io/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideStremioAccountApi(
        @Named("stremioAccount") retrofit: Retrofit,
    ): StremioAccountApi = retrofit.create(StremioAccountApi::class.java)

    @Provides
    @IntoSet
    fun provideStremioCredentialStore(store: StremioSessionStore): ProfileScopedCredentialStore = store

    @Provides
    @IntoSet
    fun provideStremioTrackingProvider(provider: StremioTrackingProvider): TrackingProvider = provider

    @Provides
    @IntoSet
    fun provideStremioHistoryWriter(writer: StremioTrackingHistoryWriter): TrackingHistoryWriter = writer
}
