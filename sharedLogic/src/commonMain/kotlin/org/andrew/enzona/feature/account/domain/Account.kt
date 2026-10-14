package org.andrew.enzona.feature.account.domain

/** Cuenta del usuario, ya normalizada (sin los tipos laxos del backend). */
data class Account(
    val uuid: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val primaryCurrency: String?,
    val profileLevel: Int?,
    val isVerified: Boolean,
    val isPhoneVerified: Boolean,
    val hasPaymentPassword: Boolean,
    val biometricEnabled: Boolean,
    val isMerchant: Boolean,
    val createdAt: String?,
) {
    val fullName: String get() = "$firstName $lastName".trim().ifEmpty { username }
    val initials: String
        get() = listOf(firstName, lastName).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("").ifEmpty { username.take(1).uppercase() }
}
