package com.github.auties00.cobalt.wire.linked.message.system.history;

import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;

/**
 * Packages a slice of chat history into an encrypted media attachment that can
 * be shared with another participant in the conversation.
 *
 * <p>When a user decides to share part of a conversation (for example when a
 * new member is added to a group and the existing members elect to back-fill
 * the group's history for them), the client serialises the selected messages
 * into an encrypted blob that is uploaded to WhatsApp's media CDN. This
 * payload carries the media-download parameters (direct path, media key,
 * digests, mime type) together with a nested
 * {@link MessageHistoryMetadata} describing the bundle in human-readable
 * terms. The recipient downloads and decrypts the blob and then ingests the
 * contained messages into the chat.
 */
@ProtobufMessage(name = "Message.MessageHistoryBundle")
public final class MessageHistoryBundle implements ContextualMessage {
    /**
     * The MIME type of the decoded history payload.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String mimetype;

    /**
     * The SHA-256 digest of the plaintext history bundle, used to verify
     * integrity after decryption.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
    byte[] fileSha256;

    /**
     * The symmetric media key used to decrypt the encrypted history bundle.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The SHA-256 digest of the encrypted history bundle, used to
     * authenticate the ciphertext before decryption.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] fileEncSha256;

    /**
     * The direct-path URL fragment used to fetch the encrypted history bundle
     * from the WhatsApp media CDN.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String directPath;

    /**
     * The timestamp at which the media key was generated, used for media key
     * lifetime validation.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant mediaKeyTimestamp;

    /**
     * The quoted-message and mention context in which this history bundle
     * was sent.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    /**
     * The human-readable description of the shared history in terms of
     * recipients, time range and message count.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    MessageHistoryMetadata messageHistoryMetadata;


    /**
     * Constructs a new message history bundle.
     *
     * @param mimetype                the MIME type of the decoded payload
     * @param fileSha256              the plaintext SHA-256 digest
     * @param mediaKey                the symmetric media key
     * @param fileEncSha256           the ciphertext SHA-256 digest
     * @param directPath              the direct-path URL fragment
     * @param mediaKeyTimestamp       the timestamp at which the media key was
     *                                generated
     * @param contextInfo             the contextual information surrounding
     *                                the message
     * @param messageHistoryMetadata  the descriptive metadata of the shared
     *                                history
     */
    MessageHistoryBundle(String mimetype, byte[] fileSha256, byte[] mediaKey, byte[] fileEncSha256, String directPath, Instant mediaKeyTimestamp, ContextInfo contextInfo, MessageHistoryMetadata messageHistoryMetadata) {
        this.mimetype = mimetype;
        this.fileSha256 = fileSha256;
        this.mediaKey = mediaKey;
        this.fileEncSha256 = fileEncSha256;
        this.directPath = directPath;
        this.mediaKeyTimestamp = mediaKeyTimestamp;
        this.contextInfo = contextInfo;
        this.messageHistoryMetadata = messageHistoryMetadata;
    }

    /**
     * Returns the MIME type of the decoded history payload.
     *
     * @return an {@link Optional} containing the MIME type, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> mimetype() {
        return Optional.ofNullable(mimetype);
    }

    /**
     * Returns the SHA-256 digest of the plaintext history bundle.
     *
     * @return an {@link Optional} containing the plaintext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(fileSha256);
    }

    /**
     * Returns the symmetric media key used to decrypt the bundle.
     *
     * @return an {@link Optional} containing the media key, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the SHA-256 digest of the encrypted history bundle.
     *
     * @return an {@link Optional} containing the ciphertext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(fileEncSha256);
    }

    /**
     * Returns the direct-path URL fragment used to fetch the encrypted
     * bundle.
     *
     * @return an {@link Optional} containing the direct path, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(directPath);
    }

    /**
     * Returns the timestamp at which the media key was generated.
     *
     * @return an {@link Optional} containing the media-key timestamp, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<Instant> mediaKeyTimestamp() {
        return Optional.ofNullable(mediaKeyTimestamp);
    }

    /**
     * Returns the contextual information (quoted message, mentions, ephemeral
     * settings) that surrounds this history bundle.
     *
     * @return an {@link Optional} containing the context info, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Returns the descriptive metadata of the shared history bundle.
     *
     * @return an {@link Optional} containing the history metadata, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<MessageHistoryMetadata> messageHistoryMetadata() {
        return Optional.ofNullable(messageHistoryMetadata);
    }

    /**
     * Sets the MIME type of the decoded history payload.
     *
     * @param mimetype the new MIME type, may be {@code null}
     */
    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Sets the SHA-256 digest of the plaintext history bundle.
     *
     * @param fileSha256 the new plaintext digest, may be {@code null}
     */
    public void setFileSha256(byte[] fileSha256) {
        this.fileSha256 = fileSha256;
    }

    /**
     * Sets the symmetric media key used to decrypt the bundle.
     *
     * @param mediaKey the new media key, may be {@code null}
     */
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the SHA-256 digest of the encrypted history bundle.
     *
     * @param fileEncSha256 the new ciphertext digest, may be {@code null}
     */
    public void setFileEncSha256(byte[] fileEncSha256) {
        this.fileEncSha256 = fileEncSha256;
    }

    /**
     * Sets the direct-path URL fragment used to fetch the encrypted bundle.
     *
     * @param directPath the new direct path, may be {@code null}
     */
    public void setDirectPath(String directPath) {
        this.directPath = directPath;
    }

    /**
     * Sets the timestamp at which the media key was generated.
     *
     * @param mediaKeyTimestamp the new media-key timestamp, may be
     *                          {@code null}
     */
    public void setMediaKeyTimestamp(Instant mediaKeyTimestamp) {
        this.mediaKeyTimestamp = mediaKeyTimestamp;
    }

    /**
     * Sets the contextual information associated with this history bundle.
     *
     * @param contextInfo the new context info, may be {@code null}
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Sets the descriptive metadata of the shared history bundle.
     *
     * @param messageHistoryMetadata the new history metadata, may be
     *                               {@code null}
     */
    public void setMessageHistoryMetadata(MessageHistoryMetadata messageHistoryMetadata) {
        this.messageHistoryMetadata = messageHistoryMetadata;
    }
}
