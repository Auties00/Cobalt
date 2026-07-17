package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.libsignal.protocol.SignalCiphertextMessage;

/**
 * Identifies the wire-level Signal envelope variant carried in the {@code type}
 * attribute of an {@code <enc>} stanza child.
 *
 * <p>The four constants correspond one-to-one to the four wire strings
 * {@code "pkmsg"}, {@code "msg"}, {@code "skmsg"}, and {@code "msmsg"}. The
 * outbound send pipeline derives the variant from a freshly produced Signal
 * ciphertext through {@link #fromSignalCiphertext(SignalCiphertextMessage)},
 * and the inbound receive pipeline parses the incoming attribute through
 * {@link #fromProtocolValue(String)}.
 */
@WhatsAppWebModule(moduleName = "WAWebBackendJobs.flow")
public enum MessageEncryptionType {
    /**
     * Tags the very first encrypted payload sent to a device when no Signal
     * session exists yet.
     *
     * <p>The first {@code <enc type="pkmsg">} that reaches a device carries the
     * ephemeral keys needed to establish the Signal session; subsequent
     * payloads to the same device switch to {@link #MSG}. PreKey-bearing
     * fanouts must also ship the linked device's {@code <device-identity>} stanza
     * for ADV signature validation. Round-trips from
     * {@link SignalCiphertextMessage#PRE_KEY_TYPE}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    PKMSG("pkmsg"),

    /**
     * Tags every encrypted payload to a device once the Signal session has been
     * established.
     *
     * <p>This is the default variant for one-to-one and broadcast device
     * fanouts after the first {@link #PKMSG} has bootstrapped the session.
     * Round-trips from {@link SignalCiphertextMessage#WHISPER_TYPE}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    MSG("msg"),

    /**
     * Tags group payloads encrypted once by the sender with a group sender key.
     *
     * <p>Each group member decrypts the same ciphertext using the previously
     * distributed sender-key record, avoiding per-member re-encryption.
     * Round-trips from {@link SignalCiphertextMessage#SENDER_KEY_TYPE}.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    SKMSG("skmsg"),

    /**
     * Tags bot-targeted payloads where the Signal envelope wraps an additional
     * inner AES-GCM ciphertext.
     *
     * <p>The outer Signal envelope wraps a {@code MessageSecretMessage}
     * protobuf whose {@code encPayload} is AES-GCM-encrypted under a key derived
     * from the parent message's {@code messageSecret} via HKDF-SHA256. This
     * variant has no matching Signal type byte; the caller overrides the variant
     * to this constant after the Signal layer has been encoded.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    MSMSG("msmsg");

    /**
     * Holds the wire string identifying this variant in the {@code type}
     * attribute of an {@code <enc>} stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final String protocolValue;

    /**
     * Constructs a variant bound to the given wire string.
     *
     * @param protocolValue the wire string tag emitted on the {@code <enc>}
     *                      {@code type} attribute
     */
    MessageEncryptionType(String protocolValue) {
        this.protocolValue = protocolValue;
    }

    /**
     * Returns the wire string emitted as the {@code type} attribute of an
     * {@code <enc>} stanza for this variant.
     *
     * <p>The four possible return values are {@code "pkmsg"}, {@code "msg"},
     * {@code "skmsg"}, and {@code "msmsg"}; they round-trip through
     * {@link #fromProtocolValue(String)}.
     *
     * @return the wire string for this variant
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public String protocolValue() {
        return protocolValue;
    }

    /**
     * Maps a freshly produced {@link SignalCiphertextMessage} to the matching
     * wire variant so the outbound encryption pipeline can stamp the correct
     * {@code type} on the {@code <enc>} stanza.
     *
     * <p>Bot payloads never reach this method. Their Signal type byte still
     * indicates {@link SignalCiphertextMessage#WHISPER_TYPE} or
     * {@link SignalCiphertextMessage#PRE_KEY_TYPE}; the bot-message wrapping
     * step in the send pipeline overrides the result to {@link #MSMSG} after the
     * fact.
     *
     * @param ciphertext the Signal ciphertext whose type byte is read
     * @return the matching wire variant, one of {@link #PKMSG}, {@link #MSG},
     *         or {@link #SKMSG}
     * @throws IllegalArgumentException if the ciphertext reports an unknown
     *                                  Signal type byte
     */
    public static MessageEncryptionType fromSignalCiphertext(SignalCiphertextMessage ciphertext) {
        return switch (ciphertext.type()) {
            case SignalCiphertextMessage.PRE_KEY_TYPE -> PKMSG;
            case SignalCiphertextMessage.WHISPER_TYPE -> MSG;
            case SignalCiphertextMessage.SENDER_KEY_TYPE -> SKMSG;
            default -> throw new IllegalArgumentException(
                    "Unknown Signal ciphertext type: " + ciphertext.type()
            );
        };
    }

    /**
     * Parses the {@code type} attribute of an incoming {@code <enc>} stanza into
     * the matching variant.
     *
     * <p>Inputs other than the four canonical wire strings are rejected rather
     * than silently mapped to a fallback variant; the inbound receive pipeline
     * relies on the strict mapping to decide which decryption path to take.
     *
     * @param value the wire string read off the stanza attribute
     * @return the matching variant
     * @throws IllegalArgumentException if {@code value} is not one of the four
     *                                  supported wire strings
     */
    @WhatsAppWebExport(moduleName = "WAWebBackendJobs.flow", exports = "CiphertextType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static MessageEncryptionType fromProtocolValue(String value) {
        return switch (value) {
            case "pkmsg" -> PKMSG;
            case "msg" -> MSG;
            case "skmsg" -> SKMSG;
            case "msmsg" -> MSMSG;
            default -> throw new IllegalArgumentException("Unknown encryption type: " + value);
        };
    }

    /**
     * Returns whether this variant is the PreKey envelope that establishes a
     * fresh Signal session.
     *
     * <p>Callers use this to decide whether the outbound fanout must include the
     * linked device's {@code <device-identity>} child stanza for ADV signature
     * validation.
     *
     * @return {@code true} when this is {@link #PKMSG}
     */
    public boolean isPreKeyMessage() {
        return this == PKMSG;
    }

    /**
     * Returns whether this variant is the SenderKey envelope used for group
     * fanout.
     *
     * <p>The group receive path uses this to dispatch the payload through the
     * group sender-key cipher rather than the per-device session cipher.
     *
     * @return {@code true} when this is {@link #SKMSG}
     */
    public boolean isSenderKeyMessage() {
        return this == SKMSG;
    }
}
