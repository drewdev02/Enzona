package org.andrew.enzona.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.enzona.core.di.PlainHttpClient
import org.andrew.enzona.core.di.coreModule
import org.andrew.enzona.core.storage.SecureStorage
import org.andrew.enzona.feature.account.accountModule
import org.andrew.enzona.feature.account.presentation.AccountViewModel
import org.andrew.enzona.feature.auth.authModule
import org.andrew.enzona.feature.auth.data.OAuthService
import org.andrew.enzona.feature.auth.presentation.AuthViewModel
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

private class MemoryStorage : SecureStorage {
    private val map = mutableMapOf<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String) = map[key]
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}

/**
 * Compilar no basta: el grafo de Koin falla en TIEMPO DE EJECUCIÓN (definición ausente,
 * cliente mal calificado). Este test resuelve cada pieza que usan las apps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KoinGraphTest {
    @Test
    fun resolvesTheWholeGraph() = runTest {
        // Los ViewModels lanzan en Dispatchers.Main desde su init.
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val koin = koinApplication {
                modules(coreModule, authModule, accountModule, module { single<SecureStorage> { MemoryStorage() } })
            }.koin

            assertNotNull(koin.get<AuthViewModel>())
            assertNotNull(koin.get<AccountViewModel>())
            assertNotNull(koin.get<OAuthService>())
            // Dos clientes distintos: el del IdP (sin auth) y el de la API (con Bearer + refresh).
            assertNotSame(koin.get<HttpClient>(), koin.get<HttpClient>(PlainHttpClient))
        } finally {
            Dispatchers.resetMain()
        }
    }
}
