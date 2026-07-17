package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.message.MessageEncryptionType;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Objects;
import java.util.Optional;

/**
 * One {@code <enc>} child of an incoming {@code <message>} stanza, parsed into
 * its decoded {@link MessageEncryptionType}, optional media-type tag, raw
 * ciphertext, retry counter, and {@code decrypt-fail} flag.
 *
 * <p>A single stanza can carry several encrypted payloads at once: a group
 * message arrives with an {@code skmsg} (sender-key group ciphertext) plus a
 * {@code pkmsg} or {@code msg} (Signal per-device retry envelope). The receive
 * pipeline iterates over {@link MessageReceiveStanza#encs()} and routes each
 * one to the appropriate Signal cipher; {@link #e2eType()} picks the cipher,
 * {@link #ciphertext()} is the input, {@link #encMediaType()} is forwarded to
 * the protobuf decoder for media-specific framing, and {@link #retryCount()} /
 * {@link #hideFail()} drive the retry-receipt and placeholder-suppression
 * branches.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceiveEncryptedPayload {
    /**
     * The Signal encryption type decoded from the {@code type} attribute.
     */
    private final MessageEncryptionType e2eType;

    /**
     * The optional {@code mediatype} attribute, identifying the media class of
     * the encrypted payload (for example {@code "image"}, {@code "video"},
     * {@code "ptt"}, {@code "GroupHistory"}).
     */
    private final String encMediaType;

    /**
     * The raw encrypted bytes read from the {@code <enc>} stanza's content
     * region.
     */
    private final byte[] ciphertext;

    /**
     * The {@code count} attribute on the {@code <enc>} stanza, reporting how many
     * times the sender has already retried delivery of this ciphertext.
     */
    private final int retryCount;

    /**
     * {@code true} when the stanza carried {@code decrypt-fail="hide"} on this
     * {@code <enc>}, telling the receiver to drop the message silently if
     * decryption fails.
     */
    private final boolean hideFail;

    /**
     * Constructs a populated record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * @param e2eType      the decoded encryption type
     * @param encMediaType the optional media-type tag, or {@code null}
     * @param ciphertext   the raw encrypted bytes
     * @param retryCount   the sender-reported retry count, or {@code 0} when absent
     * @param hideFail     whether {@code decrypt-fail="hide"} was present
     * @throws NullPointerException if {@code e2eType} or {@code ciphertext} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceiveEncryptedPayload(
            MessageEncryptionType e2eType,
            String encMediaType,
            byte[] ciphertext,
            int retryCount,
            boolean hideFail
    ) {
        this.e2eType = Objects.requireNonNull(e2eType, "e2eType cannot be null");
        this.encMediaType = encMediaType;
        this.ciphertext = Objects.requireNonNull(ciphertext, "ciphertext cannot be null");
        this.retryCount = retryCount;
        this.hideFail = hideFail;
    }

    /**
     * Returns the Signal encryption type that selects which cipher decrypts
     * {@link #ciphertext()}.
     *
     * <p>Distinguishes per-device envelopes (PKMSG / MSG), sender-key group
     * envelopes (SKMSG), and bot multi-side envelopes (MSMSG); the receive
     * pipeline branches on this to choose the unwrapping path.
     *
     * @return the encryption type
     */
    public MessageEncryptionType e2eType() {
        return e2eType;
    }

    /**
     * Returns the {@code mediatype} attribute, when present.
     *
     * <p>Forwarded to the protobuf decoder so it can pick the right inner
     * framing for media payloads; the {@code "GroupHistory"} value also gates
     * reporting-token capture for group-history shares.
     *
     * @return an {@link Optional} wrapping the media type tag
     */
    public Optional<String> encMediaType() {
        return Optional.ofNullable(encMediaType);
    }

    /**
     * Returns the raw encrypted bytes of this payload.
     *
     * <p>Fed directly to the Signal cipher selected by {@link #e2eType()}; the
     * receive pipeline does not modify these bytes before decryption.
     *
     * @return the ciphertext
     */
    public byte[] ciphertext() {
        return ciphertext;
    }

    /**
     * Returns the sender-reported retry count.
     *
     * <p>Used to emit a {@code retry} receipt with the matching count when the
     * receiver successfully decrypts a retried envelope; aggregated across all
     * {@code <enc>} children of the stanza by
     * {@link MessageReceiveStanza#retryCount()}.
     *
     * @return the retry count, or {@code 0} when the {@code count} attribute was absent
     */
    public int retryCount() {
        return retryCount;
    }

    /**
     * Returns whether this payload carried {@code decrypt-fail="hide"}.
     *
     * <p>When {@code true} the receive pipeline drops the message silently on
     * decryption failure rather than surfacing the usual placeholder; this is
     * set on outgoing sender-key distribution messages so peers that cannot
     * decrypt them do not see error stubs in the chat.
     *
     * @return {@code true} if decrypt-fail is set to {@code "hide"}
     */
    public boolean hideFail() {
        return hideFail;
    }
}
