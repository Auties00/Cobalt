package com.github.auties00.cobalt.wire.cloud.phone;

/**
 * The delivery channel of a WhatsApp Cloud API phone-number verification code.
 *
 * <p>Registering a phone number for Cloud API use requires proving ownership: the server delivers a
 * one-time code over one of these channels, and the code is then submitted back through the
 * verification endpoint. Each constant maps to one {@code code_method} value of the
 * {@code request_code} edge.
 */
public enum CloudVerificationMethod {
    /**
     * The code is delivered in a text message.
     */
    SMS,
    /**
     * The code is read out in a voice call.
     */
    VOICE
}
