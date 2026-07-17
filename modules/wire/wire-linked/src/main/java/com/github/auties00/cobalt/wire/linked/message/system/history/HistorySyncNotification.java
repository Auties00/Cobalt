package com.github.auties00.cobalt.wire.linked.message.system.history;

import com.github.auties00.cobalt.wire.linked.media.MediaPath;
import com.github.auties00.cobalt.wire.linked.media.MediaProvider;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Announces the availability of an encrypted history sync blob that a
 * companion device needs to download in order to ingest a slice of the user's
 * history.
 *
 * <p>The primary device packages chats, messages, contacts and related
 * collections into encrypted chunks that are uploaded to WhatsApp's media
 * servers. For each chunk the primary sends a peer message carrying one of
 * these notifications; the companion then downloads the blob via the standard
 * media pipeline (using the sha256 and media key fields), decrypts it, and
 * applies the decoded data to its local store. The {@link HistorySyncType}
 * field distinguishes initial bootstrap chunks from recent, full, on-demand,
 * status and ancillary data flavours.
 *
 * <p>The class implements {@link MediaProvider} so that the generic media
 * download flow can treat it like any other attachment.
 */
@ProtobufMessage(name = "Message.HistorySyncNotification")
public final class HistorySyncNotification implements Message, MediaProvider {
    /**
     * The SHA-256 digest of the plaintext history chunk, used to verify
     * integrity after decryption.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] mediaSha256;

    /**
     * The length of the encrypted history blob in bytes, as reported by the
     * media server.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64)
    Long mediaSize;

    /**
     * The symmetric media key used to derive the cipher and MAC keys that
     * decrypt this history chunk.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    byte[] mediaKey;

    /**
     * The SHA-256 digest of the encrypted history blob, used to authenticate
     * the ciphertext before decryption.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
    byte[] mediaEncryptedSha256;

    /**
     * The direct-path URL fragment that the companion uses to fetch the
     * encrypted history blob from the WhatsApp media CDN.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String mediaDirectPath;

    /**
     * The flavour of this history sync chunk, which controls how the
     * companion ingests the decoded data.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.ENUM)
    HistorySyncType syncType;

    /**
     * The zero-based position of this chunk inside the sequence that makes up
     * the complete sync.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
    Integer chunkOrder;

    /**
     * The identifier of the original message whose history this chunk is
     * related to, typically used by placeholder-resend and on-demand flows.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String originalMessageId;

    /**
     * The overall progress of the sync as a percentage, used by the companion
     * to render a progress indicator to the user.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    Integer progress;

    /**
     * The timestamp of the oldest message included in this chunk, allowing
     * the companion to show how far back the sync currently extends.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64, mixins = InstantSecondsMixin.class)
    Instant oldestMsgInChunkTimestampSec;

    /**
     * An optional inline payload carrying the initial bootstrap chunk directly
     * inside the notification when the sync data is small enough to avoid a
     * media download.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.BYTES)
    byte[] initialHistBootstrapInlinePayload;

    /**
     * The identifier of the peer-data-request session this chunk belongs to,
     * used to correlate chunks with the request that initiated them.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    String peerDataRequestSessionId;

    /**
     * The metadata that correlates this chunk with its originating full
     * on-demand history sync request.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
    FullHistorySyncOnDemandRequestMetadata fullHistorySyncOnDemandRequestMetadata;

    /**
     * An opaque handle used by the media server to resolve server-side
     * encryption state for this chunk.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    String encHandle;

    /**
     * The nested access-status payload delivered as part of a
     * {@link HistorySyncType#MESSAGE_ACCESS_STATUS} sync.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
    HistorySyncMessageAccessStatus messageAccessStatus;


    /**
     * Constructs a new history sync notification with the provided fields.
     *
     * @param mediaSha256                             the plaintext SHA-256
     * @param mediaSize                               the encrypted blob size
     * @param mediaKey                                the symmetric media key
     * @param mediaEncryptedSha256                    the ciphertext SHA-256
     * @param mediaDirectPath                         the direct-path URL fragment
     * @param syncType                                the flavour of this chunk
     * @param chunkOrder                              the chunk position in the sequence
     * @param originalMessageId                       the originating message id
     * @param progress                                the overall progress percentage
     * @param oldestMsgInChunkTimestampSec            the oldest message timestamp in this chunk
     * @param initialHistBootstrapInlinePayload       optional inline bootstrap payload
     * @param peerDataRequestSessionId                the peer-data-request session id
     * @param fullHistorySyncOnDemandRequestMetadata  the originating on-demand request metadata
     * @param encHandle                               the opaque server-side encryption handle
     * @param messageAccessStatus                     the nested access-status payload
     */
    HistorySyncNotification(byte[] mediaSha256, Long mediaSize, byte[] mediaKey, byte[] mediaEncryptedSha256, String mediaDirectPath, HistorySyncType syncType, Integer chunkOrder, String originalMessageId, Integer progress, Instant oldestMsgInChunkTimestampSec, byte[] initialHistBootstrapInlinePayload, String peerDataRequestSessionId, FullHistorySyncOnDemandRequestMetadata fullHistorySyncOnDemandRequestMetadata, String encHandle, HistorySyncMessageAccessStatus messageAccessStatus) {
        this.mediaSha256 = mediaSha256;
        this.mediaSize = mediaSize;
        this.mediaKey = mediaKey;
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
        this.mediaDirectPath = mediaDirectPath;
        this.syncType = syncType;
        this.chunkOrder = chunkOrder;
        this.originalMessageId = originalMessageId;
        this.progress = progress;
        this.oldestMsgInChunkTimestampSec = oldestMsgInChunkTimestampSec;
        this.initialHistBootstrapInlinePayload = initialHistBootstrapInlinePayload;
        this.peerDataRequestSessionId = peerDataRequestSessionId;
        this.fullHistorySyncOnDemandRequestMetadata = fullHistorySyncOnDemandRequestMetadata;
        this.encHandle = encHandle;
        this.messageAccessStatus = messageAccessStatus;
    }

    /**
     * Returns the SHA-256 digest of the decrypted history chunk.
     *
     * @return an {@link Optional} containing the plaintext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> fileSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the SHA-256 digest of the decrypted history chunk.
     *
     * @return an {@link Optional} containing the plaintext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    @Override
    public Optional<byte[]> mediaSha256() {
        return Optional.ofNullable(mediaSha256);
    }

    /**
     * Returns the length of the encrypted history blob in bytes.
     *
     * @return an {@link OptionalLong} containing the size, or
     *         {@link OptionalLong#empty()} if it was not provided
     */
    public OptionalLong fileLength() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the length of the encrypted history blob in bytes.
     *
     * @return an {@link OptionalLong} containing the size, or
     *         {@link OptionalLong#empty()} if it was not provided
     */
    @Override
    public OptionalLong mediaSize() {
        return mediaSize == null ? OptionalLong.empty() : OptionalLong.of(mediaSize);
    }

    /**
     * Returns the symmetric media key used to decrypt this chunk.
     *
     * @return an {@link Optional} containing the media key, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> mediaKey() {
        return Optional.ofNullable(mediaKey);
    }

    /**
     * Returns the SHA-256 digest of the encrypted history blob.
     *
     * @return an {@link Optional} containing the ciphertext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<byte[]> fileEncSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the SHA-256 digest of the encrypted history blob.
     *
     * @return an {@link Optional} containing the ciphertext digest, or
     *         {@link Optional#empty()} if it was not provided
     */
    @Override
    public Optional<byte[]> mediaEncryptedSha256() {
        return Optional.ofNullable(mediaEncryptedSha256);
    }

    /**
     * Returns the direct-path URL fragment used to fetch the encrypted blob.
     *
     * @return an {@link Optional} containing the direct path, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> directPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the direct-path URL fragment used to fetch the encrypted blob.
     *
     * @return an {@link Optional} containing the direct path, or
     *         {@link Optional#empty()} if it was not provided
     */
    @Override
    public Optional<String> mediaDirectPath() {
        return Optional.ofNullable(mediaDirectPath);
    }

    /**
     * Returns the flavour of this history sync chunk.
     *
     * @return an {@link Optional} containing the sync type, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<HistorySyncType> syncType() {
        return Optional.ofNullable(syncType);
    }

    /**
     * Returns the zero-based position of this chunk in the sync sequence.
     *
     * @return an {@link OptionalInt} containing the chunk order, or
     *         {@link OptionalInt#empty()} if it was not provided
     */
    public OptionalInt chunkOrder() {
        return chunkOrder == null ? OptionalInt.empty() : OptionalInt.of(chunkOrder);
    }

    /**
     * Returns the identifier of the message that originated this chunk.
     *
     * @return an {@link Optional} containing the message identifier, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> originalMessageId() {
        return Optional.ofNullable(originalMessageId);
    }

    /**
     * Returns the overall progress of the sync as a percentage between
     * {@code 0} and {@code 100}.
     *
     * @return an {@link OptionalInt} containing the progress percentage, or
     *         {@link OptionalInt#empty()} if it was not provided
     */
    public OptionalInt progress() {
        return progress == null ? OptionalInt.empty() : OptionalInt.of(progress);
    }

    /**
     * Returns the timestamp of the oldest message included in this chunk.
     *
     * @return an {@link Optional} containing the oldest-message timestamp, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<Instant> oldestMsgInChunkTimestampSec() {
        return Optional.ofNullable(oldestMsgInChunkTimestampSec);
    }

    /**
     * Returns the inline bootstrap payload, if the sync data was small enough
     * to be delivered directly in the notification instead of via a media
     * download.
     *
     * @return an {@link Optional} containing the inline payload, or
     *         {@link Optional#empty()} when a normal media download is
     *         required
     */
    public Optional<byte[]> initialHistBootstrapInlinePayload() {
        return Optional.ofNullable(initialHistBootstrapInlinePayload);
    }

    /**
     * Returns the identifier of the peer-data-request session that produced
     * this chunk.
     *
     * @return an {@link Optional} containing the session identifier, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> peerDataRequestSessionId() {
        return Optional.ofNullable(peerDataRequestSessionId);
    }

    /**
     * Returns the metadata that correlates this chunk with the originating
     * full on-demand history sync request.
     *
     * @return an {@link Optional} containing the request metadata, or
     *         {@link Optional#empty()} if this chunk was not part of such a
     *         request
     */
    public Optional<FullHistorySyncOnDemandRequestMetadata> fullHistorySyncOnDemandRequestMetadata() {
        return Optional.ofNullable(fullHistorySyncOnDemandRequestMetadata);
    }

    /**
     * Returns the opaque server-side encryption handle for this chunk.
     *
     * @return an {@link Optional} containing the encryption handle, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> encHandle() {
        return Optional.ofNullable(encHandle);
    }

    /**
     * Returns the nested access-status payload attached to a
     * {@link HistorySyncType#MESSAGE_ACCESS_STATUS} notification.
     *
     * @return an {@link Optional} containing the access-status payload, or
     *         {@link Optional#empty()} for notifications of any other type
     */
    public Optional<HistorySyncMessageAccessStatus> messageAccessStatus() {
        return Optional.ofNullable(messageAccessStatus);
    }

    /**
     * Returns the direct media URL for this chunk.
     *
     * <p>History sync blobs are addressed exclusively via
     * {@link #mediaDirectPath()}, so this accessor always returns
     * {@link Optional#empty()} to satisfy the {@link MediaProvider}
     * contract.
     *
     * @return {@link Optional#empty()}
     */
    @Override
    public Optional<String> mediaUrl() {
        return Optional.empty();
    }

    /**
     * Returns the media namespace that history sync chunks are stored in.
     *
     * @return {@link MediaPath#HISTORY_SYNC}
     */
    @Override
    public MediaPath mediaPath() {
        return MediaPath.HISTORY_SYNC;
    }

    /**
     * Sets the SHA-256 digest of the decrypted history chunk.
     *
     * @param mediaSha256 the new plaintext digest, may be {@code null}
     */
    @Override
    public void setMediaSha256(byte[] mediaSha256) {
        this.mediaSha256 = mediaSha256;
    }

    /**
     * Sets the length of the encrypted history blob in bytes.
     *
     * @param mediaSize the new size in bytes
     */
    @Override
    public void setMediaSize(long mediaSize) {
        this.mediaSize = mediaSize;
    }

    /**
     * Sets the symmetric media key used to decrypt this chunk.
     *
     * @param mediaKey the new media key, may be {@code null}
     */
    @Override
    public void setMediaKey(byte[] mediaKey) {
        this.mediaKey = mediaKey;
    }

    /**
     * Sets the SHA-256 digest of the encrypted history blob.
     *
     * @param mediaEncryptedSha256 the new ciphertext digest, may be
     *                             {@code null}
     */
    @Override
    public void setMediaEncryptedSha256(byte[] mediaEncryptedSha256) {
        this.mediaEncryptedSha256 = mediaEncryptedSha256;
    }

    /**
     * Sets the direct-path URL fragment used to fetch the encrypted blob.
     *
     * @param mediaDirectPath the new direct path, may be {@code null}
     */
    @Override
    public void setMediaDirectPath(String mediaDirectPath) {
        this.mediaDirectPath = mediaDirectPath;
    }

    /**
     * Sets the flavour of this history sync chunk.
     *
     * @param syncType the new sync type, may be {@code null}
     */
    public void setSyncType(HistorySyncType syncType) {
        this.syncType = syncType;
    }

    /**
     * Sets the zero-based position of this chunk in the sync sequence.
     *
     * @param chunkOrder the new chunk order, may be {@code null}
     */
    public void setChunkOrder(Integer chunkOrder) {
        this.chunkOrder = chunkOrder;
    }

    /**
     * Sets the identifier of the message that originated this chunk.
     *
     * @param originalMessageId the new originating message identifier, may be
     *                          {@code null}
     */
    public void setOriginalMessageId(String originalMessageId) {
        this.originalMessageId = originalMessageId;
    }

    /**
     * Sets the overall progress of the sync as a percentage.
     *
     * @param progress the new progress percentage, may be {@code null}
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    /**
     * Sets the timestamp of the oldest message included in this chunk.
     *
     * @param oldestMsgInChunkTimestampSec the new timestamp, may be
     *                                     {@code null}
     */
    public void setOldestMsgInChunkTimestampSec(Instant oldestMsgInChunkTimestampSec) {
        this.oldestMsgInChunkTimestampSec = oldestMsgInChunkTimestampSec;
    }

    /**
     * Sets the inline bootstrap payload carried directly inside this
     * notification.
     *
     * @param initialHistBootstrapInlinePayload the new inline payload, may be
     *                                          {@code null}
     */
    public void setInitialHistBootstrapInlinePayload(byte[] initialHistBootstrapInlinePayload) {
        this.initialHistBootstrapInlinePayload = initialHistBootstrapInlinePayload;
    }

    /**
     * Sets the identifier of the peer-data-request session that produced this
     * chunk.
     *
     * @param peerDataRequestSessionId the new session identifier, may be
     *                                 {@code null}
     */
    public void setPeerDataRequestSessionId(String peerDataRequestSessionId) {
        this.peerDataRequestSessionId = peerDataRequestSessionId;
    }

    /**
     * Sets the metadata that correlates this chunk with the originating full
     * on-demand history sync request.
     *
     * @param fullHistorySyncOnDemandRequestMetadata the new metadata, may be
     *                                               {@code null}
     */
    public void setFullHistorySyncOnDemandRequestMetadata(FullHistorySyncOnDemandRequestMetadata fullHistorySyncOnDemandRequestMetadata) {
        this.fullHistorySyncOnDemandRequestMetadata = fullHistorySyncOnDemandRequestMetadata;
    }

    /**
     * Sets the opaque server-side encryption handle for this chunk.
     *
     * @param encHandle the new encryption handle, may be {@code null}
     */
    public void setEncHandle(String encHandle) {
        this.encHandle = encHandle;
    }

    /**
     * Sets the nested access-status payload for this chunk.
     *
     * @param messageAccessStatus the new access-status payload, may be
     *                            {@code null}
     */
    public void setMessageAccessStatus(HistorySyncMessageAccessStatus messageAccessStatus) {
        this.messageAccessStatus = messageAccessStatus;
    }

    /**
     * Setter required by the {@link MediaProvider} contract.
     *
     * <p>History sync chunks are addressed exclusively by their direct path,
     * so this operation is intentionally a no-op.
     *
     * @param mediaUrl the ignored media URL
     */
    @Override
    public void setMediaUrl(String mediaUrl) {
    }

    /**
     * Setter required by the {@link MediaProvider} contract.
     *
     * <p>History sync chunks do not carry a media-key timestamp, so this
     * operation is intentionally a no-op.
     *
     * @param timestamp the ignored media-key timestamp
     */
    @Override
    public void setMediaKeyTimestamp(Instant timestamp) {
    }
}
