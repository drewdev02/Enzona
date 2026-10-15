package org.andrew.enzona.feature.home.domain

/**
 * Casillas de la cuadrícula del inicio. EL ORDEN DEL ENUM ES EL ORDEN EN PANTALLA (el mismo
 * que `items.json` del APK original).
 *
 * El nombre en minúsculas es la clave del icono: `TRANSFER` -> `service_transfer` en
 * Android (drawable) e iOS (imageset). `tools/extract_assets.py` genera esos recursos y
 * `tools/check_assets.py` comprueba que enum y recursos coinciden.
 *
 * `isAvailable` pasa a true cuando la función existe en la app; mientras tanto la casilla
 * se muestra y avisa de que llegará próximamente.
 */
enum class HomeService(val label: String, val isAvailable: Boolean = false) {
    TRANSFER("Transferir", isAvailable = true),
    BALANCE("Balance", isAvailable = true),
    BULEVAR("Bulevar"),
    GIFT("Regalo"),
    DONATE("Donar"),
    ELECTRICITY("Electricidad"),
    ETECSA("Etecsa"),
    TAXES("Impuestos"),
    GAS("Gas"),
    MAIL("Correos"),
    EXTRA_CASH("Caja extra"),
    PURCHASES("Mis compras"),
    CREATE_ACCOUNT("Crear cuenta"),
    ORGANIZATIONS("Organizaciones");

    /** Nombre del recurso de imagen en ambas plataformas. */
    val iconKey: String get() = "service_${name.lowercase()}"
}

/** Lista en orden, accesible desde Swift sin tener que lidiar con `entries`. */
object HomeServices {
    val all: List<HomeService> = HomeService.entries.toList()
}
