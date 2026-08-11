package com.nuvio.tv.core.di

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.core.serverassist.TailnetOnlyDns
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerAssistNetworkModule {
    @Provides
    @Singleton
    @Named("serverAssist")
    fun provideServerAssistHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .dns(TailnetOnlyDns())
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor { chain ->
                val version = BuildConfig.VERSION_NAME.ifBlank { "dev" }
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", "Nuvio/$version server-assist")
                        .build(),
                )
            }.build()
}
