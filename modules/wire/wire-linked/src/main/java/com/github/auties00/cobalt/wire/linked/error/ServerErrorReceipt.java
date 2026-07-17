package com.github.auties00.cobalt.wire.linked.error;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A client to server receipt that reports a failed media download and
 * requests the sender to re-upload the media.
 *
 * <p>When a chat contains a media message (image, video, document, audio,
 * sticker, or history sync payload) the receiving client downloads the
 * encrypted blob from the WhatsApp CDN using the direct path attached to
 * the message. If that download fails because the CDN link has expired,
 * the blob has been evicted, or the bytes fail integrity verification,
 * the client cannot recover the plaintext from local data alone. To ask
 * the sender to produce a fresh copy it encrypts an instance of this
 * message and transmits it as a {@code server-error} type receipt stanza.
 * The server routes the request to the original sender, which re-uploads
 * the media and notifies the requester through a
 * {@link com.github.auties00.cobalt.wire.linked.media.MediaRetryNotification}.
 *
 * <p>The receipt payload is a single {@link #stanzaId()} that identifies
 * the failed message. It is serialized to protobuf, encrypted with
 * AES-GCM using a key derived from the original media key via HKDF with
 * the info string {@code "WhatsApp Media Retry Notification"}, and paired
 * with a random 12-byte initialization vector. The serialized plaintext
 * doubles as additional authenticated data so that the ciphertext is
 * bound to the specific stanza identifier.
 *
 * @see com.github.auties00.cobalt.wire.linked.media.MediaRetryNotification
 */
@ProtobufMessage(name = "ServerErrorReceipt")
public final class ServerErrorReceipt {
    /**
     * The identifier of the message stanza whose media download failed.
     *
     * <p>The server uses this value to correlate the retry request with
     * the original media message and to route the re-upload notification
     * back to the requester.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * Constructs a new {@code ServerErrorReceipt} for the given stanza
     * identifier.
     *
     * @param stanzaId the identifier of the message stanza whose media
     *        download failed, or {@code null} if not available
     */
    ServerErrorReceipt(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    /**
     * Returns the identifier of the message stanza whose media download
     * failed.
     *
     * @return an {@link Optional} containing the stanza identifier, or an
     *         empty {@code Optional} if no identifier was supplied
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Replaces the identifier of the message stanza whose media download
     * failed.
     *
     * @param stanzaId the new stanza identifier, or {@code null} to clear
     */
    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }
}
