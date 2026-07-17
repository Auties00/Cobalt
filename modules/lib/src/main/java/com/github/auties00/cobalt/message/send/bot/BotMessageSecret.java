package com.github.auties00.cobalt.message.send.bot;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Objects;

/**
 * Derives the bot-side message secret from a user message secret.
 *
 * <p>The derived secret is the key material the bot encryption layer feeds into
 * AES-GCM alongside the envelope's encryption IV and payload. It lets plaintext
 * sent to a bot be encrypted with a key the user can rotate without exposing the
 * original {@code messageSecret} value carried by the message
 * {@link com.github.auties00.cobalt.wire.linked.chat.ChatMessageContextInfo}. It is
 * produced by {@link #derive(byte[])} and installed by
 * {@link BotProtobufTransform#transformForCapi(com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer, byte[])}
 * when preparing an outbound CAPI bot message.
 */
@WhatsAppWebModule(moduleName = "WAWebBotMessageSecret")
public final class BotMessageSecret {
    /**
     * Names the HKDF algorithm requested from {@link KDF#getInstance(String)}.
     */
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    /**
     * Provides the fixed info string consumed by the HKDF expand step.
     */
    private static final String INFO = "Bot Message";

    /**
     * Holds the length, in bytes, of the derived secret.
     */
    private static final int SECRET_LENGTH = 32;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private BotMessageSecret() {
        throw new AssertionError();
    }

    /**
     * Derives the 32-byte bot message secret from the supplied base message secret.
     *
     * <p>Runs HKDF-SHA-256 with an all-zero salt for the extract step and the
     * fixed info string {@code "Bot Message"} for the expand step. Producing
     * different bytes here means the bot cannot decrypt the message at all, so
     * {@code messageSecret} must not be mutated concurrently with this call.
     *
     * @implNote
     * This implementation accepts any input keying material length because
     * HKDF-Extract collapses arbitrary input to a fixed-size pseudorandom key;
     * WA Web passes a 32-byte secret in practice. The output length is pinned at
     * {@value #SECRET_LENGTH} to match the {@code u=32} constant in
     * {@code WAWebBotMessageSecret}.
     *
     * @param messageSecret the base message secret bytes, typically 32 bytes
     * @return the derived 32-byte bot message secret
     * @throws NullPointerException     if {@code messageSecret} is {@code null}
     * @throws GeneralSecurityException if HKDF-SHA-256 is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebBotMessageSecret", exports = "genBotMsgSecretFromMsgSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] derive(byte[] messageSecret) throws GeneralSecurityException {
        Objects.requireNonNull(messageSecret, "messageSecret");
        var kdf = KDF.getInstance(HKDF_ALGORITHM);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(messageSecret)
                .thenExpand(INFO.getBytes(StandardCharsets.UTF_8), SECRET_LENGTH);
        return kdf.deriveData(params);
    }
}
