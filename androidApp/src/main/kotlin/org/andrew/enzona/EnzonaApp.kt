package org.andrew.enzona

import android.app.Application
import android.content.pm.ApplicationInfo
import org.andrew.enzona.core.logging.installLogging
import org.andrew.enzona.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class EnzonaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Debug: desde Debug. Release: desde Info. Antes de Koin para no perder los primeros logs.
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        installLogging(debug = debuggable)
        initKoin {
            androidLogger()
            androidContext(this@EnzonaApp)
        }
    }
}
