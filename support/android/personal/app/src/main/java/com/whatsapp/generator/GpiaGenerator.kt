package com.whatsapp.generator

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.whatsapp.result.IntegrityTokenResult

fun generateIntegrityToken(authKey: ByteArray, context: Context): IntegrityTokenResult {
    val integrityManager = IntegrityManagerFactory.create(context)
    val request = IntegrityTokenRequest.builder()
        .setNonce(authKey.decodeToString())
        .build()
    val integrityTokenResponse = integrityManager.requestIntegrityToken(request)
    return IntegrityTokenResult(integrityTokenResponse.result?.token()!!)
}