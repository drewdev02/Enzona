package org.andrew.enzona.core

/**
 * Configuración de endpoints y OAuth. Usa tu PROPIO client_id para operar.
 * Hosts y rutas verificados contra el backend real (ver docs 02/07).
 */
object EnzonaConfig {
    const val API_BASE = "https://api.enzona.net"
    const val IDENTITY_BASE = "https://identity.enzona.net"

    // ⚠ Reemplazar por tu client_id registrado en EnZona/XETID.
    const val CLIENT_ID = "ofr3Wz9nnfZaFd18OewdZYvuTaEa"
    const val REDIRECT_URI = "http://apk-callback"
    const val SCOPE = "openid"

    const val AUTHORIZE_PATH = "/oauth2/authorize"
    const val TOKEN_PATH = "/oauth2/token"
}
