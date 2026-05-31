package org.ecoguardian

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class EcoGuardianApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configurar logs solo en debug
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
