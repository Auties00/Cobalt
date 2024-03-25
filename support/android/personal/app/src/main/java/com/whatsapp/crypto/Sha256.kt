package com.whatsapp.crypto

import java.security.MessageDigest

fun sha256(bytes: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") {
            str, it -> str + "%02x".format(it)
    }
}