package com.payscope.app.di

import com.payscope.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.payscope.app.data.api.PayScopeApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private fun getBaseUrl(): String {
        if (BuildConfig.API_BASE_URL.isNotBlank()) return BuildConfig.API_BASE_URL.trimEnd('/') + "/"
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK built for x86") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.PRODUCT.contains("sdk_gphone")
        return if (isEmulator) "http://10.0.2.2:8000/" else "http://127.0.0.1:8000/"
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        loggingInterceptor.redactHeader("Authorization")
        val firebaseTokenInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val user = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }.getOrNull()
            val authorization = request.header("Authorization").orEmpty().removePrefix("Bearer ")
            val isFirebase = runCatching {
                val payload = android.util.Base64.decode(authorization.split('.')[1], android.util.Base64.URL_SAFE)
                org.json.JSONObject(String(payload)).optString("iss").startsWith("https://securetoken.google.com/")
            }.getOrDefault(false)
            val refreshed = if (user != null && isFirebase) {
                runCatching { com.google.android.gms.tasks.Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS).token }.getOrNull()
            } else null
            chain.proceed(if (refreshed != null) request.newBuilder()
                .header("Authorization", "Bearer $refreshed").build() else request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(firebaseTokenInterceptor)
            .addInterceptor(com.payscope.app.data.api.TransientReadRetryInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(600, TimeUnit.SECONDS)
            .callTimeout(660, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun providePayScopeApiService(retrofit: Retrofit): PayScopeApiService {
        return retrofit.create(PayScopeApiService::class.java)
    }
}
