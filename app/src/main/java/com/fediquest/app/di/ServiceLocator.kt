// File: app/src/main/java/com/fediquest/app/di/ServiceLocator.kt
package com.fediquest.app.di

import android.content.Context
import com.fediquest.app.data.local.AppDatabase
import com.fediquest.app.data.repositories.AvatarRepository
import com.fediquest.app.data.repositories.CompanionRepository
import com.fediquest.app.data.repositories.QuestRepository
import com.fediquest.app.data.repositories.UserRepository
import com.fediquest.app.domain.usecases.CalculateXPUseCase
import com.fediquest.app.domain.usecases.PostToFediverseUseCase
import com.fediquest.app.domain.usecases.ValidateQuestUseCase
import com.fediquest.app.util.FediverseClient
import com.fediquest.app.util.LocationTracker
import com.fediquest.app.util.XPManager
import com.fediquest.app.viewmodel.SharedViewModel
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * Manual dependency injection service locator.
 * Provides singleton instances of repositories, use cases, and utilities.
 */
object ServiceLocator {

    private lateinit var context: Context
    
    // Database
    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    // HTTP Client
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/") // Placeholder - replace with actual Fediverse instance
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Repositories
    val questRepository: QuestRepository by lazy {
        QuestRepository(database.questDao(), retrofit)
    }

    val avatarRepository: AvatarRepository by lazy {
        AvatarRepository(database.avatarDao())
    }

    val companionRepository: CompanionRepository by lazy {
        CompanionRepository(database.companionDao())
    }

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao())
    }

    // Use Cases
    val calculateXPUseCase: CalculateXPUseCase by lazy {
        CalculateXPUseCase(XPManager)
    }

    val validateQuestUseCase: ValidateQuestUseCase by lazy {
        ValidateQuestUseCase(questRepository, XPManager)
    }

    val postToFediverseUseCase: PostToFediverseUseCase by lazy {
        PostToFediverseUseCase(FediverseClient)
    }

    // Utilities
    val locationTracker: LocationTracker by lazy {
        LocationTracker(context)
    }

    /**
     * Initialize the ServiceLocator with application context.
     * Must be called from Application.onCreate()
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    /**
     * Get application context safely.
     */
    fun getContext(): Context {
        return requireNotNull(::context.isInitialized) {
            "ServiceLocator not initialized. Call init() first."
        }
    }
}
