package org.andrew.enzona.core.di

import org.andrew.enzona.core.storage.AndroidSecureStorage
import org.andrew.enzona.core.storage.SecureStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
}
