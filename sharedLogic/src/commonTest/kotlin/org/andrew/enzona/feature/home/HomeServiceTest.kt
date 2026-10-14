package org.andrew.enzona.feature.home

import org.andrew.enzona.feature.home.domain.HomeService
import org.andrew.enzona.feature.home.domain.HomeServices
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeServiceTest {
    @Test
    fun orderAndLabelsMatchTheOriginalGrid() {
        // Orden y textos de items.json del APK original.
        val expected = listOf(
            "Transferir", "Balance", "Bulevar", "Regalo", "Donar", "Electricidad", "Etecsa",
            "Impuestos", "Gas", "Correos", "Caja extra", "Mis compras", "Crear cuenta", "Organizaciones",
        )
        assertEquals(expected, HomeServices.all.map { it.label })
    }

    @Test
    fun iconKeysAreUniqueAndResourceSafe() {
        val keys = HomeServices.all.map { it.iconKey }
        assertEquals(keys.size, keys.toSet().size)
        // Nombres válidos de recurso Android: minúsculas, dígitos y guion bajo.
        assertTrue(keys.all { Regex("service_[a-z0-9_]+").matches(it) }, "claves: $keys")
        assertEquals("service_create_account", HomeService.CREATE_ACCOUNT.iconKey)
    }
}
