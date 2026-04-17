// File: app/src/main/java/com/fediquest/app/FediQuestApp.kt
package com.fediquest.app

import android.app.Application
import com.fediquest.app.di.ServiceLocator

/**
 * Application class for FediQuest.
 * Initializes the ServiceLocator and performs app-wide setup.
 */
class FediQuestApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize dependency injection
        ServiceLocator.init(this)
    }

    companion object {
        lateinit var instance: FediQuestApp
            private set
    }
}
