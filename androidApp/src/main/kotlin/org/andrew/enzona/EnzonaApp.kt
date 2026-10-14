package org.andrew.enzona

import android.app.Application
import org.andrew.enzona.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class EnzonaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@EnzonaApp)
        }
    }
}
