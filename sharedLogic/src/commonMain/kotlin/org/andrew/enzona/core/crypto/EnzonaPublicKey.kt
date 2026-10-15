package org.andrew.enzona.core.crypto

/**
 * Clave pública RSA-2048 que la app original empaqueta como `enzona_pubkey.pem` y usa para cifrar la contraseña de
 * pago. Es pública: no es un secreto. Copiada tal cual del APK v20003.
 */
const val ENZONA_PUBLIC_KEY_PEM: String = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt2qPaTDVWOejUhIj6mbX
rqlGHGiOwpKlpTjMkzRZhfEEC4BKCVtY/KytCC3VJMxQ+poeiPEDcuz34InRUPsS
Ox0xBEnQvbZ5NuT03xZyIvTE46UZqGLmfInAdRRpAfmYA1Nt1qsbZrLIsS/tWGTa
clnSE721RlLvwIrxncXmfwrpu3LUn0HTuneMTN7GoqVMMPM9gPodVZ9OkjzOLJSv
jsMfgab5a+csUrtlHhlMZDz+8aHjj190rJBuIQJlYDHLkMtq4gjRjvQEApe65WWg
f6LYhq9QAckUs5KWhWBdHhTyPGBQS4fZd4gtT7NjvpYsQEmLTNJqsJ9QHBjuWdQd
TwIDAQAB
-----END PUBLIC KEY-----"""
