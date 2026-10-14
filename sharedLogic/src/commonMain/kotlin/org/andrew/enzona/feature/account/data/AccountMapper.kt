package org.andrew.enzona.feature.account.data

import org.andrew.enzona.core.EnzonaConfig
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.core.util.asFlag
import org.andrew.enzona.feature.account.domain.Account

/** `false` llega como texto en campos de texto no aplicables: se trata como ausente. */
private fun String?.textOrNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("false", ignoreCase = true) }

internal fun AccountDto.toDomain(): EzResult<Account> {
    val username = username.textOrNull()
        ?: return EzResult.Error(Failure.Parse("La cuenta no incluye username."))
    return EzResult.Success(
        Account(
            uuid = uuid.textOrNull().orEmpty(),
            username = username,
            firstName = name.textOrNull().orEmpty(),
            lastName = lastname.textOrNull().orEmpty(),
            email = email.textOrNull(),
            phone = phone.textOrNull(),
            avatarUrl = avatar.textOrNull()?.let { EnzonaConfig.MEDIA_BASE + it.trimStart('/') },
            primaryCurrency = primaryCurrency.textOrNull()?.uppercase(),
            profileLevel = profileLevel.textOrNull()?.toIntOrNull(),
            isVerified = verified.asFlag(),
            isPhoneVerified = verifiedPhone.asFlag(),
            hasPaymentPassword = paymentPassword.asFlag(),
            biometricEnabled = fingerprint.asFlag(),
            isMerchant = merchantAccount.asFlag(),
            createdAt = createdAt.textOrNull(),
        )
    )
}
