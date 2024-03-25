package com.whatsapp.w4b.generator

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.whatsapp.w4b.result.CertificateResult
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.util.Base64
import java.util.Date
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

// TODO: Replace with app signature as it's the same
private const val APP_SIGNATURE = "3987d043d10aefaf5a8710b3671418fe57e0e19b653c9df82558feb5ffce5d44"

private val COUNTER = AtomicInteger(0)


fun generateCertificate(authKey: ByteArray, enc: String): CertificateResult {
    val alias = "ws_cert_" + COUNTER.incrementAndGet()
    val ks = KeyStore.getInstance("AndroidKeyStore")
    ks.load(null)
    ks.deleteEntry(alias)
    val expireTime = Date()
    expireTime.time = System.currentTimeMillis() + 80L * 365 * 24 * 60 * 60 * 1000
    val attestationChallenge = ByteBuffer.allocate(authKey.size + 9)
    attestationChallenge.order(ByteOrder.BIG_ENDIAN)
    attestationChallenge.putLong(System.currentTimeMillis() / 1000 - 15)
    attestationChallenge.put(0x1F.toByte())
    attestationChallenge.put(authKey)
    val keyPairGenerator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore")
    keyPairGenerator.initialize(
        KeyGenParameterSpec.Builder(
            alias, KeyProperties.PURPOSE_SIGN
        )
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setUserAuthenticationRequired(false)
            .setCertificateNotAfter(expireTime)
            .setAttestationChallenge(attestationChallenge.array())
            .build()
    )
    keyPairGenerator.generateKeyPair()
    val certs = Objects.requireNonNull(ks.getCertificateChain(alias), "Missing certificates")
    val ba = ByteArrayOutputStream()
    for (i in certs.size - 1 downTo 1) {
        ba.write(certs[i].encoded)
    }
    val c0Hex = bytesToHex(certs[0].encoded)
    val pubHex = bytesToHex(authKey)
    val timeBytes = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array()
    val time = bytesToHex(timeBytes).substring(4)
    val pubIndex = c0Hex.indexOf(pubHex)
    val timeIndex = pubIndex + 64 + 20
    val signIndex = timeIndex + time.length + 80
    val tailIndex = signIndex + APP_SIGNATURE.length
    val newC0Hex = (c0Hex.substring(0, timeIndex)
            + time
            + c0Hex.substring(timeIndex + time.length, signIndex)
            + APP_SIGNATURE
            + c0Hex.substring(tailIndex))
    ba.write(hexStringToByteArray(newC0Hex))
    val s = Signature.getInstance("SHA256withECDSA")
    val entry = ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry
    s.initSign(entry.privateKey)
    s.update(enc.toByteArray(StandardCharsets.UTF_8))
    ks.deleteEntry(alias)
    val encSign = Base64.getEncoder().encodeToString(s.sign())
    val encCert = Base64.getEncoder().encodeToString(ba.toByteArray())
    return CertificateResult(encCert, encSign)
}

private fun bytesToHex(data: ByteArray): String = String.format("%X", ByteBuffer.wrap(data).long)

private fun hexStringToByteArray(s: String): ByteArray = s.chunked(2)
    .map { it.toInt(16).toByte() }
    .toByteArray()