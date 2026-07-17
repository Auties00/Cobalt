package com.github.auties00.cobalt.wire.linked.message.system.peer;

import com.github.auties00.cobalt.wire.linked.media.MediaRetryNotification;
import com.github.auties00.cobalt.wire.linked.message.Message;
import com.github.auties00.cobalt.wire.linked.message.media.StickerMessage;
import com.github.auties00.cobalt.wire.linked.message.system.history.FullHistorySyncOnDemandRequestMetadata;
import com.github.auties00.cobalt.wire.linked.message.system.history.HistorySyncType;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Delivers the response to a previously sent
 * {@link PeerDataOperationRequestMessage}.
 *
 * <p>The envelope echoes the originating request type and the stanza
 * identifier of the request, then carries one or more
 * {@link PeerDataOperationResult} entries that each contain the outcome of a
 * sub-request (for example the reuploaded sticker, the generated link
 * preview, the serialised resent message, or the requested nonce). A single
 * response may aggregate multiple results because several requests of the
 * same type can be batched together in the originating message.
 */
@ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage")
public final class PeerDataOperationRequestResponseMessage implements Message {
    /**
     * The discriminator that identifies which kind of request this response
     * answers.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PeerDataOperationRequestType peerDataOperationRequestType;

    /**
     * The identifier of the stanza that carried the originating request,
     * used by the requester to match this response to the outstanding
     * operation.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String stanzaId;

    /**
     * The list of individual operation results delivered by this response.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    List<PeerDataOperationResult> peerDataOperationResult;


    /**
     * Constructs a new peer data operation response envelope.
     *
     * @param peerDataOperationRequestType the request type discriminator
     * @param stanzaId                     the originating stanza identifier
     * @param peerDataOperationResult      the list of operation results
     */
    PeerDataOperationRequestResponseMessage(PeerDataOperationRequestType peerDataOperationRequestType, String stanzaId, List<PeerDataOperationResult> peerDataOperationResult) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
        this.stanzaId = stanzaId;
        this.peerDataOperationResult = peerDataOperationResult;
    }

    /**
     * Returns the discriminator that identifies which kind of request this
     * response answers.
     *
     * @return an {@link Optional} containing the request type, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<PeerDataOperationRequestType> peerDataOperationRequestType() {
        return Optional.ofNullable(peerDataOperationRequestType);
    }

    /**
     * Returns the identifier of the stanza that carried the originating
     * request.
     *
     * @return an {@link Optional} containing the stanza identifier, or
     *         {@link Optional#empty()} if it was not provided
     */
    public Optional<String> stanzaId() {
        return Optional.ofNullable(stanzaId);
    }

    /**
     * Returns an unmodifiable view of the operation results delivered by
     * this response.
     *
     * @return the list of results, or an empty list when none were provided
     */
    public List<PeerDataOperationResult> peerDataOperationResult() {
        return peerDataOperationResult == null ? List.of() : Collections.unmodifiableList(peerDataOperationResult);
    }

    /**
     * Sets the discriminator that identifies which kind of request this
     * response answers.
     *
     * @param peerDataOperationRequestType the new request type, may be
     *                                     {@code null}
     */
    public void setPeerDataOperationRequestType(PeerDataOperationRequestType peerDataOperationRequestType) {
        this.peerDataOperationRequestType = peerDataOperationRequestType;
    }

    /**
     * Sets the identifier of the stanza that carried the originating
     * request.
     *
     * @param stanzaId the new stanza identifier, may be {@code null}
     */
    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    /**
     * Sets the list of operation results delivered by this response.
     *
     * @param peerDataOperationResult the new list, may be {@code null}
     */
    public void setPeerDataOperationResult(List<PeerDataOperationResult> peerDataOperationResult) {
        this.peerDataOperationResult = peerDataOperationResult;
    }

    /**
     * Carries the outcome of a single sub-operation that was part of the
     * originating peer request.
     *
     * <p>Depending on which discriminator was set in the request, exactly one
     * of the optional response bodies will be populated. Sticker reupload
     * responses combine the {@link #mediaUploadResult} field (a status code)
     * with a fully-formed {@link StickerMessage} carrying the new media
     * handles; link previews, nonce fetches, placeholder resends, history
     * chunks and recovery snapshots have their own dedicated nested types.
     */
    @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult")
    public static final class PeerDataOperationResult {
        /**
         * The status code of a sticker reupload response.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
        MediaRetryNotification.ResultType mediaUploadResult;

        /**
         * The newly reuploaded sticker carrying fresh media handles, when
         * the request asked for a sticker reupload.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        StickerMessage stickerMessage;

        /**
         * The link preview body produced by the peer for a
         * {@link PeerDataOperationRequestType#GENERATE_LINK_PREVIEW}
         * request.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.LinkPreviewResponse linkPreviewResponse;

        /**
         * The serialised resent message returned for a
         * {@link PeerDataOperationRequestType#PLACEHOLDER_MESSAGE_RESEND}
         * request.
         */
        @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.PlaceholderMessageResendResponse placeholderMessageResendResponse;

        /**
         * The nonce returned for a
         * {@link PeerDataOperationRequestType#WAFFLE_LINKING_NONCE_FETCH}
         * request.
         */
        @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.WaffleNonceFetchResponse waffleNonceFetchRequestResponse;

        /**
         * The response body returned for a
         * {@link PeerDataOperationRequestType#FULL_HISTORY_SYNC_ON_DEMAND}
         * request.
         */
        @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.FullHistorySyncOnDemandRequestResponse fullHistorySyncOnDemandRequestResponse;

        /**
         * The nonce returned for a
         * {@link PeerDataOperationRequestType#COMPANION_META_NONCE_FETCH}
         * request.
         */
        @ProtobufProperty(index = 7, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.CompanionMetaNonceFetchResponse companionMetaNonceFetchRequestResponse;

        /**
         * The snapshot returned for a
         * {@link PeerDataOperationRequestType#COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY}
         * request.
         */
        @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.SyncDSnapshotFatalRecoveryResponse syncdSnapshotFatalRecoveryResponse;

        /**
         * The nonce returned for a
         * {@link PeerDataOperationRequestType#COMPANION_CANONICAL_USER_NONCE_FETCH}
         * request.
         */
        @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.CompanionCanonicalUserNonceFetchResponse companionCanonicalUserNonceFetchRequestResponse;

        /**
         * The response body returned for a
         * {@link PeerDataOperationRequestType#HISTORY_SYNC_CHUNK_RETRY}
         * request.
         */
        @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
        PeerDataOperationResult.HistorySyncChunkRetryResponse historySyncChunkRetryResponse;


        /**
         * Constructs a new peer data operation result.
         *
         * @param mediaUploadResult                                the sticker reupload status
         * @param stickerMessage                                   the reuploaded sticker
         * @param linkPreviewResponse                              the link preview body
         * @param placeholderMessageResendResponse                 the resent message body
         * @param waffleNonceFetchRequestResponse                  the WAFFLE nonce body
         * @param fullHistorySyncOnDemandRequestResponse           the full history sync body
         * @param companionMetaNonceFetchRequestResponse           the Meta nonce body
         * @param syncdSnapshotFatalRecoveryResponse               the recovery snapshot body
         * @param companionCanonicalUserNonceFetchRequestResponse  the canonical-user nonce body
         * @param historySyncChunkRetryResponse                    the history chunk retry body
         */
        PeerDataOperationResult(MediaRetryNotification.ResultType mediaUploadResult, StickerMessage stickerMessage, LinkPreviewResponse linkPreviewResponse, PlaceholderMessageResendResponse placeholderMessageResendResponse, WaffleNonceFetchResponse waffleNonceFetchRequestResponse, FullHistorySyncOnDemandRequestResponse fullHistorySyncOnDemandRequestResponse, CompanionMetaNonceFetchResponse companionMetaNonceFetchRequestResponse, SyncDSnapshotFatalRecoveryResponse syncdSnapshotFatalRecoveryResponse, CompanionCanonicalUserNonceFetchResponse companionCanonicalUserNonceFetchRequestResponse, HistorySyncChunkRetryResponse historySyncChunkRetryResponse) {
            this.mediaUploadResult = mediaUploadResult;
            this.stickerMessage = stickerMessage;
            this.linkPreviewResponse = linkPreviewResponse;
            this.placeholderMessageResendResponse = placeholderMessageResendResponse;
            this.waffleNonceFetchRequestResponse = waffleNonceFetchRequestResponse;
            this.fullHistorySyncOnDemandRequestResponse = fullHistorySyncOnDemandRequestResponse;
            this.companionMetaNonceFetchRequestResponse = companionMetaNonceFetchRequestResponse;
            this.syncdSnapshotFatalRecoveryResponse = syncdSnapshotFatalRecoveryResponse;
            this.companionCanonicalUserNonceFetchRequestResponse = companionCanonicalUserNonceFetchRequestResponse;
            this.historySyncChunkRetryResponse = historySyncChunkRetryResponse;
        }

        /**
         * Returns the status code of a sticker reupload response.
         *
         * @return an {@link Optional} containing the status code, or
         *         {@link Optional#empty()} if this result is not a sticker
         *         reupload
         */
        public Optional<MediaRetryNotification.ResultType> mediaUploadResult() {
            return Optional.ofNullable(mediaUploadResult);
        }

        /**
         * Returns the sticker returned by a reupload response.
         *
         * @return an {@link Optional} containing the sticker, or
         *         {@link Optional#empty()} if this result is not a sticker
         *         reupload
         */
        public Optional<StickerMessage> stickerMessage() {
            return Optional.ofNullable(stickerMessage);
        }

        /**
         * Returns the link preview body of this result.
         *
         * @return an {@link Optional} containing the preview body, or
         *         {@link Optional#empty()} if this result is not a link
         *         preview
         */
        public Optional<LinkPreviewResponse> linkPreviewResponse() {
            return Optional.ofNullable(linkPreviewResponse);
        }

        /**
         * Returns the placeholder message resend body of this result.
         *
         * @return an {@link Optional} containing the resend body, or
         *         {@link Optional#empty()} if this result is not a resend
         */
        public Optional<PlaceholderMessageResendResponse> placeholderMessageResendResponse() {
            return Optional.ofNullable(placeholderMessageResendResponse);
        }

        /**
         * Returns the WAFFLE nonce fetch body of this result.
         *
         * @return an {@link Optional} containing the nonce body, or
         *         {@link Optional#empty()} if this result is not a WAFFLE
         *         nonce fetch
         */
        public Optional<WaffleNonceFetchResponse> waffleNonceFetchRequestResponse() {
            return Optional.ofNullable(waffleNonceFetchRequestResponse);
        }

        /**
         * Returns the full on-demand history sync response body of this
         * result.
         *
         * @return an {@link Optional} containing the response body, or
         *         {@link Optional#empty()} if this result is not a full
         *         history sync response
         */
        public Optional<FullHistorySyncOnDemandRequestResponse> fullHistorySyncOnDemandRequestResponse() {
            return Optional.ofNullable(fullHistorySyncOnDemandRequestResponse);
        }

        /**
         * Returns the Meta nonce fetch body of this result.
         *
         * @return an {@link Optional} containing the nonce body, or
         *         {@link Optional#empty()} if this result is not a Meta
         *         nonce fetch
         */
        public Optional<CompanionMetaNonceFetchResponse> companionMetaNonceFetchRequestResponse() {
            return Optional.ofNullable(companionMetaNonceFetchRequestResponse);
        }

        /**
         * Returns the fatal recovery snapshot body of this result.
         *
         * @return an {@link Optional} containing the snapshot body, or
         *         {@link Optional#empty()} if this result is not a recovery
         *         snapshot
         */
        public Optional<SyncDSnapshotFatalRecoveryResponse> syncdSnapshotFatalRecoveryResponse() {
            return Optional.ofNullable(syncdSnapshotFatalRecoveryResponse);
        }

        /**
         * Returns the canonical-user nonce fetch body of this result.
         *
         * @return an {@link Optional} containing the nonce body, or
         *         {@link Optional#empty()} if this result is not a
         *         canonical-user nonce fetch
         */
        public Optional<CompanionCanonicalUserNonceFetchResponse> companionCanonicalUserNonceFetchRequestResponse() {
            return Optional.ofNullable(companionCanonicalUserNonceFetchRequestResponse);
        }

        /**
         * Returns the history sync chunk retry body of this result.
         *
         * @return an {@link Optional} containing the retry body, or
         *         {@link Optional#empty()} if this result is not a chunk
         *         retry response
         */
        public Optional<HistorySyncChunkRetryResponse> historySyncChunkRetryResponse() {
            return Optional.ofNullable(historySyncChunkRetryResponse);
        }

        /**
         * Sets the status code of a sticker reupload response.
         *
         * @param mediaUploadResult the new status code, may be {@code null}
         */
        public void setMediaUploadResult(MediaRetryNotification.ResultType mediaUploadResult) {
            this.mediaUploadResult = mediaUploadResult;
    }

        /**
         * Sets the sticker returned by a reupload response.
         *
         * @param stickerMessage the new sticker, may be {@code null}
         */
        public void setStickerMessage(StickerMessage stickerMessage) {
            this.stickerMessage = stickerMessage;
    }

        /**
         * Sets the link preview body of this result.
         *
         * @param linkPreviewResponse the new preview body, may be {@code null}
         */
        public void setLinkPreviewResponse(LinkPreviewResponse linkPreviewResponse) {
            this.linkPreviewResponse = linkPreviewResponse;
    }

        /**
         * Sets the placeholder message resend body of this result.
         *
         * @param placeholderMessageResendResponse the new resend body, may be
         *                                         {@code null}
         */
        public void setPlaceholderMessageResendResponse(PlaceholderMessageResendResponse placeholderMessageResendResponse) {
            this.placeholderMessageResendResponse = placeholderMessageResendResponse;
    }

        /**
         * Sets the WAFFLE nonce fetch body of this result.
         *
         * @param waffleNonceFetchRequestResponse the new nonce body, may be
         *                                        {@code null}
         */
        public void setWaffleNonceFetchRequestResponse(WaffleNonceFetchResponse waffleNonceFetchRequestResponse) {
            this.waffleNonceFetchRequestResponse = waffleNonceFetchRequestResponse;
    }

        /**
         * Sets the full on-demand history sync response body of this result.
         *
         * @param fullHistorySyncOnDemandRequestResponse the new response body,
         *                                               may be {@code null}
         */
        public void setFullHistorySyncOnDemandRequestResponse(FullHistorySyncOnDemandRequestResponse fullHistorySyncOnDemandRequestResponse) {
            this.fullHistorySyncOnDemandRequestResponse = fullHistorySyncOnDemandRequestResponse;
    }

        /**
         * Sets the Meta nonce fetch body of this result.
         *
         * @param companionMetaNonceFetchRequestResponse the new nonce body,
         *                                               may be {@code null}
         */
        public void setCompanionMetaNonceFetchRequestResponse(CompanionMetaNonceFetchResponse companionMetaNonceFetchRequestResponse) {
            this.companionMetaNonceFetchRequestResponse = companionMetaNonceFetchRequestResponse;
    }

        /**
         * Sets the fatal recovery snapshot body of this result.
         *
         * @param syncdSnapshotFatalRecoveryResponse the new snapshot body,
         *                                           may be {@code null}
         */
        public void setSyncdSnapshotFatalRecoveryResponse(SyncDSnapshotFatalRecoveryResponse syncdSnapshotFatalRecoveryResponse) {
            this.syncdSnapshotFatalRecoveryResponse = syncdSnapshotFatalRecoveryResponse;
    }

        /**
         * Sets the canonical-user nonce fetch body of this result.
         *
         * @param companionCanonicalUserNonceFetchRequestResponse the new nonce
         *                                                        body, may be
         *                                                        {@code null}
         */
        public void setCompanionCanonicalUserNonceFetchRequestResponse(CompanionCanonicalUserNonceFetchResponse companionCanonicalUserNonceFetchRequestResponse) {
            this.companionCanonicalUserNonceFetchRequestResponse = companionCanonicalUserNonceFetchRequestResponse;
    }

        /**
         * Sets the history sync chunk retry body of this result.
         *
         * @param historySyncChunkRetryResponse the new retry body, may be
         *                                      {@code null}
         */
        public void setHistorySyncChunkRetryResponse(HistorySyncChunkRetryResponse historySyncChunkRetryResponse) {
            this.historySyncChunkRetryResponse = historySyncChunkRetryResponse;
    }

        /**
         * Enumerates the outcomes of a full on-demand history sync request.
         */
        @ProtobufEnum(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.FullHistorySyncOnDemandResponseCode")
        public static enum FullHistorySyncOnDemandResponseCode {
            /**
             * The peer accepted the request and will deliver the requested
             * history.
             */
            REQUEST_SUCCESS(0),
            /**
             * The request was rejected because the maximum time window during
             * which full history can be requested has already elapsed.
             */
            REQUEST_TIME_EXPIRED(1),
            /**
             * The peer refuses to share its full history with the requester.
             */
            DECLINED_SHARING_HISTORY(2),
            /**
             * The peer failed to serve the request due to an unspecified
             * error.
             */
            GENERIC_ERROR(3),
            /**
             * The request was rejected because it originated on a non-primary
             * device of a WhatsApp Business small-business account.
             */
            ERROR_REQUEST_ON_NON_SMB_PRIMARY(4),
            /**
             * The peer cannot serve the request because the hosted device is
             * currently offline.
             */
            ERROR_HOSTED_DEVICE_NOT_CONNECTED(5),
            /**
             * The peer cannot serve the request because the hosted device has
             * never recorded a login time.
             */
            ERROR_HOSTED_DEVICE_LOGIN_TIME_NOT_SET(6);

            /**
             * Constructs a new constant with the given protobuf wire index.
             *
             * @param index the numeric index used on the wire
             */
            FullHistorySyncOnDemandResponseCode(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric index used to represent this constant on the
             * protobuf wire.
             */
            final int index;

            /**
             * Returns the protobuf wire index associated with this constant.
             *
             * @return the numeric index used on the wire
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * Enumerates the outcomes of a history sync chunk retry request.
         */
        @ProtobufEnum(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.HistorySyncChunkRetryResponseCode")
        public static enum HistorySyncChunkRetryResponseCode {
            /**
             * The peer failed to regenerate the requested chunk.
             */
            GENERATION_ERROR(1),
            /**
             * The chunk is no longer available because it has already been
             * consumed by the requester.
             */
            CHUNK_CONSUMED(2),
            /**
             * The peer took too long to produce the chunk and the retry
             * timed out.
             */
            TIMEOUT(3),
            /**
             * The current sync session has no budget left for further
             * retries.
             */
            SESSION_EXHAUSTED(4),
            /**
             * The specific chunk has exceeded its maximum retry count.
             */
            CHUNK_EXHAUSTED(5),
            /**
             * The peer is already serving an identical retry and ignored the
             * duplicate.
             */
            DUPLICATED_REQUEST(6);

            /**
             * Constructs a new constant with the given protobuf wire index.
             *
             * @param index the numeric index used on the wire
             */
            HistorySyncChunkRetryResponseCode(@ProtobufEnumIndex int index) {
                this.index = index;
            }

            /**
             * The numeric index used to represent this constant on the
             * protobuf wire.
             */
            final int index;

            /**
             * Returns the protobuf wire index associated with this constant.
             *
             * @return the numeric index used on the wire
             */
            public int index() {
                return this.index;
            }
        }

        /**
         * Delivers the nonce that allows a companion to look up its canonical
         * cross-platform user identity.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.CompanionCanonicalUserNonceFetchResponse")
        public static final class CompanionCanonicalUserNonceFetchResponse {
            /**
             * The canonical-user nonce issued to the companion.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String nonce;

            /**
             * The Meta platform FBID associated with the WhatsApp account.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String waFbid;

            /**
             * Whether the companion should discard any cached identity and
             * perform a fresh lookup using the new nonce.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
            Boolean forceRefresh;


            /**
             * Constructs a new canonical-user nonce fetch response.
             *
             * @param nonce         the issued nonce
             * @param waFbid        the platform FBID
             * @param forceRefresh  whether to force a cache refresh
             */
            CompanionCanonicalUserNonceFetchResponse(String nonce, String waFbid, Boolean forceRefresh) {
                this.nonce = nonce;
                this.waFbid = waFbid;
                this.forceRefresh = forceRefresh;
            }

            /**
             * Returns the issued canonical-user nonce.
             *
             * @return an {@link Optional} containing the nonce, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> nonce() {
                return Optional.ofNullable(nonce);
            }

            /**
             * Returns the Meta platform FBID associated with the account.
             *
             * @return an {@link Optional} containing the FBID, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> waFbid() {
                return Optional.ofNullable(waFbid);
            }

            /**
             * Returns whether the companion should force a cache refresh.
             *
             * @return {@code true} if a forced refresh is requested,
             *         {@code false} otherwise or when the flag was not set
             */
            public boolean forceRefresh() {
                return forceRefresh != null && forceRefresh;
            }

            /**
             * Sets the issued canonical-user nonce.
             *
             * @param nonce the new nonce, may be {@code null}
             */
            public void setNonce(String nonce) {
                this.nonce = nonce;
    }

            /**
             * Sets the Meta platform FBID associated with the account.
             *
             * @param waFbid the new FBID, may be {@code null}
             */
            public void setWaFbid(String waFbid) {
                this.waFbid = waFbid;
    }

            /**
             * Sets whether the companion should force a cache refresh.
             *
             * @param forceRefresh the new flag, may be {@code null}
             */
            public void setForceRefresh(Boolean forceRefresh) {
                this.forceRefresh = forceRefresh;
    }
        }

        /**
         * Delivers the nonce that allows a companion to fetch Meta-side
         * metadata about the WhatsApp account.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.CompanionMetaNonceFetchResponse")
        public static final class CompanionMetaNonceFetchResponse {
            /**
             * The issued Meta nonce.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String nonce;


            /**
             * Constructs a new Meta nonce fetch response.
             *
             * @param nonce the issued nonce
             */
            CompanionMetaNonceFetchResponse(String nonce) {
                this.nonce = nonce;
            }

            /**
             * Returns the issued Meta nonce.
             *
             * @return an {@link Optional} containing the nonce, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> nonce() {
                return Optional.ofNullable(nonce);
            }

            /**
             * Sets the issued Meta nonce.
             *
             * @param nonce the new nonce, may be {@code null}
             */
            public void setNonce(String nonce) {
                this.nonce = nonce;
    }
        }

        /**
         * Acknowledges a full on-demand history sync request and reports the
         * outcome.
         *
         * <p>The response carries the originating request metadata so the
         * requester can match it and a status code describing whether the
         * peer accepted the request or rejected it for a specific reason.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.FullHistorySyncOnDemandRequestResponse")
        public static final class FullHistorySyncOnDemandRequestResponse {
            /**
             * The metadata of the originating full on-demand history sync
             * request.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
            FullHistorySyncOnDemandRequestMetadata requestMetadata;

            /**
             * The outcome of the request.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
            PeerDataOperationResult.FullHistorySyncOnDemandResponseCode responseCode;


            /**
             * Constructs a new full on-demand history sync response.
             *
             * @param requestMetadata the originating request metadata
             * @param responseCode    the outcome code
             */
            FullHistorySyncOnDemandRequestResponse(FullHistorySyncOnDemandRequestMetadata requestMetadata, FullHistorySyncOnDemandResponseCode responseCode) {
                this.requestMetadata = requestMetadata;
                this.responseCode = responseCode;
            }

            /**
             * Returns the metadata of the originating request.
             *
             * @return an {@link Optional} containing the metadata, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<FullHistorySyncOnDemandRequestMetadata> requestMetadata() {
                return Optional.ofNullable(requestMetadata);
            }

            /**
             * Returns the outcome of the request.
             *
             * @return an {@link Optional} containing the response code, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<FullHistorySyncOnDemandResponseCode> responseCode() {
                return Optional.ofNullable(responseCode);
            }

            /**
             * Sets the metadata of the originating request.
             *
             * @param requestMetadata the new metadata, may be {@code null}
             */
            public void setRequestMetadata(FullHistorySyncOnDemandRequestMetadata requestMetadata) {
                this.requestMetadata = requestMetadata;
    }

            /**
             * Sets the outcome of the request.
             *
             * @param responseCode the new response code, may be {@code null}
             */
            public void setResponseCode(FullHistorySyncOnDemandResponseCode responseCode) {
                this.responseCode = responseCode;
    }
        }

        /**
         * Acknowledges a history sync chunk retry request and reports the
         * outcome.
         *
         * <p>The response echoes the sync type and chunk order of the retry
         * so the requester can match it, identifies the retry with a fresh
         * request id, and tells the requester whether the failure is
         * recoverable or definitive.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.HistorySyncChunkRetryResponse")
        public static final class HistorySyncChunkRetryResponse {
            /**
             * The sync flavour the chunk belongs to.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
            HistorySyncType syncType;

            /**
             * The zero-based position of the chunk in its sequence.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.UINT32)
            Integer chunkOrder;

            /**
             * The identifier assigned by the peer to this retry attempt.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String requestId;

            /**
             * The outcome of the retry attempt.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
            PeerDataOperationResult.HistorySyncChunkRetryResponseCode responseCode;

            /**
             * Whether the requester may attempt a further retry in the future.
             */
            @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
            Boolean canRecover;


            /**
             * Constructs a new history sync chunk retry response.
             *
             * @param syncType     the sync flavour
             * @param chunkOrder   the chunk position
             * @param requestId    the retry identifier
             * @param responseCode the outcome
             * @param canRecover   whether future retries are allowed
             */
            HistorySyncChunkRetryResponse(HistorySyncType syncType, Integer chunkOrder, String requestId, HistorySyncChunkRetryResponseCode responseCode, Boolean canRecover) {
                this.syncType = syncType;
                this.chunkOrder = chunkOrder;
                this.requestId = requestId;
                this.responseCode = responseCode;
                this.canRecover = canRecover;
            }

            /**
             * Returns the sync flavour the chunk belongs to.
             *
             * @return an {@link Optional} containing the sync type, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<HistorySyncType> syncType() {
                return Optional.ofNullable(syncType);
            }

            /**
             * Returns the zero-based position of the chunk.
             *
             * @return an {@link OptionalInt} containing the chunk order, or
             *         {@link OptionalInt#empty()} if it was not provided
             */
            public OptionalInt chunkOrder() {
                return chunkOrder == null ? OptionalInt.empty() : OptionalInt.of(chunkOrder);
            }

            /**
             * Returns the identifier assigned to this retry attempt.
             *
             * @return an {@link Optional} containing the retry identifier, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> requestId() {
                return Optional.ofNullable(requestId);
            }

            /**
             * Returns the outcome of the retry attempt.
             *
             * @return an {@link Optional} containing the response code, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<HistorySyncChunkRetryResponseCode> responseCode() {
                return Optional.ofNullable(responseCode);
            }

            /**
             * Returns whether the requester may attempt further retries.
             *
             * @return {@code true} if recovery is still possible,
             *         {@code false} otherwise or when the flag was not set
             */
            public boolean canRecover() {
                return canRecover != null && canRecover;
            }

            /**
             * Sets the sync flavour the chunk belongs to.
             *
             * @param syncType the new sync type, may be {@code null}
             */
            public void setSyncType(HistorySyncType syncType) {
                this.syncType = syncType;
    }

            /**
             * Sets the zero-based position of the chunk.
             *
             * @param chunkOrder the new chunk order, may be {@code null}
             */
            public void setChunkOrder(Integer chunkOrder) {
                this.chunkOrder = chunkOrder;
    }

            /**
             * Sets the identifier assigned to this retry attempt.
             *
             * @param requestId the new retry identifier, may be {@code null}
             */
            public void setRequestId(String requestId) {
                this.requestId = requestId;
    }

            /**
             * Sets the outcome of the retry attempt.
             *
             * @param responseCode the new response code, may be {@code null}
             */
            public void setResponseCode(HistorySyncChunkRetryResponseCode responseCode) {
                this.responseCode = responseCode;
    }

            /**
             * Sets whether the requester may attempt further retries.
             *
             * @param canRecover the new flag, may be {@code null}
             */
            public void setCanRecover(Boolean canRecover) {
                this.canRecover = canRecover;
    }
        }

        /**
         * Delivers the link preview produced by the peer for a URL that the
         * requester could not resolve.
         *
         * <p>The preview carries the standard fields rendered by the chat UI:
         * title, description, a small inline thumbnail, the URL the preview
         * refers to, and optional high-quality and payment-metadata sub
         * records.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.LinkPreviewResponse")
        public static final class LinkPreviewResponse {
            /**
             * The URL that the preview refers to.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String url;

            /**
             * The preview title extracted from the fetched page.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String title;

            /**
             * The preview description extracted from the fetched page.
             */
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String description;

            /**
             * A small inline thumbnail rendered directly in the chat bubble.
             */
            @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
            byte[] thumbData;

            /**
             * The substring of the original message text that produced this
             * preview, used by the UI to highlight it.
             */
            @ProtobufProperty(index = 6, type = ProtobufType.STRING)
            String matchText;

            /**
             * The category of preview (for example a standard website,
             * product page or payment link).
             */
            @ProtobufProperty(index = 7, type = ProtobufType.STRING)
            String previewType;

            /**
             * An optional high-quality thumbnail suitable for large-format
             * rendering.
             */
            @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
            PeerDataOperationResult.LinkPreviewResponse.LinkPreviewHighQualityThumbnail hqThumbnail;

            /**
             * Additional metadata that applies to payment-link previews.
             */
            @ProtobufProperty(index = 9, type = ProtobufType.MESSAGE)
            PeerDataOperationResult.LinkPreviewResponse.PaymentLinkPreviewMetadata previewMetadata;


            /**
             * Constructs a new link preview response body.
             *
             * @param url             the preview URL
             * @param title           the preview title
             * @param description     the preview description
             * @param thumbData       the inline thumbnail bytes
             * @param matchText       the matching substring of the message
             * @param previewType     the preview category
             * @param hqThumbnail     the optional high-quality thumbnail
             * @param previewMetadata the optional payment-link metadata
             */
            LinkPreviewResponse(String url, String title, String description, byte[] thumbData, String matchText, String previewType, LinkPreviewHighQualityThumbnail hqThumbnail, PaymentLinkPreviewMetadata previewMetadata) {
                this.url = url;
                this.title = title;
                this.description = description;
                this.thumbData = thumbData;
                this.matchText = matchText;
                this.previewType = previewType;
                this.hqThumbnail = hqThumbnail;
                this.previewMetadata = previewMetadata;
            }

            /**
             * Returns the URL the preview refers to.
             *
             * @return an {@link Optional} containing the URL, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> url() {
                return Optional.ofNullable(url);
            }

            /**
             * Returns the preview title.
             *
             * @return an {@link Optional} containing the title, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> title() {
                return Optional.ofNullable(title);
            }

            /**
             * Returns the preview description.
             *
             * @return an {@link Optional} containing the description, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> description() {
                return Optional.ofNullable(description);
            }

            /**
             * Returns the inline thumbnail bytes.
             *
             * @return an {@link Optional} containing the thumbnail bytes, or
             *         {@link Optional#empty()} if they were not provided
             */
            public Optional<byte[]> thumbData() {
                return Optional.ofNullable(thumbData);
            }

            /**
             * Returns the substring of the message that produced this
             * preview.
             *
             * @return an {@link Optional} containing the match text, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> matchText() {
                return Optional.ofNullable(matchText);
            }

            /**
             * Returns the category of this preview.
             *
             * @return an {@link Optional} containing the preview type, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> previewType() {
                return Optional.ofNullable(previewType);
            }

            /**
             * Returns the optional high-quality thumbnail.
             *
             * @return an {@link Optional} containing the high-quality
             *         thumbnail, or {@link Optional#empty()} if it was not
             *         provided
             */
            public Optional<LinkPreviewHighQualityThumbnail> hqThumbnail() {
                return Optional.ofNullable(hqThumbnail);
            }

            /**
             * Returns the optional payment-link metadata.
             *
             * @return an {@link Optional} containing the metadata, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<PaymentLinkPreviewMetadata> previewMetadata() {
                return Optional.ofNullable(previewMetadata);
            }

            /**
             * Sets the URL the preview refers to.
             *
             * @param url the new URL, may be {@code null}
             */
            public void setUrl(String url) {
                this.url = url;
    }

            /**
             * Sets the preview title.
             *
             * @param title the new title, may be {@code null}
             */
            public void setTitle(String title) {
                this.title = title;
    }

            /**
             * Sets the preview description.
             *
             * @param description the new description, may be {@code null}
             */
            public void setDescription(String description) {
                this.description = description;
    }

            /**
             * Sets the inline thumbnail bytes.
             *
             * @param thumbData the new thumbnail bytes, may be {@code null}
             */
            public void setThumbData(byte[] thumbData) {
                this.thumbData = thumbData;
    }

            /**
             * Sets the substring of the message that produced this preview.
             *
             * @param matchText the new match text, may be {@code null}
             */
            public void setMatchText(String matchText) {
                this.matchText = matchText;
    }

            /**
             * Sets the category of this preview.
             *
             * @param previewType the new preview type, may be {@code null}
             */
            public void setPreviewType(String previewType) {
                this.previewType = previewType;
    }

            /**
             * Sets the optional high-quality thumbnail.
             *
             * @param hqThumbnail the new high-quality thumbnail, may be
             *                    {@code null}
             */
            public void setHqThumbnail(LinkPreviewHighQualityThumbnail hqThumbnail) {
                this.hqThumbnail = hqThumbnail;
    }

            /**
             * Sets the optional payment-link metadata.
             *
             * @param previewMetadata the new metadata, may be {@code null}
             */
            public void setPreviewMetadata(PaymentLinkPreviewMetadata previewMetadata) {
                this.previewMetadata = previewMetadata;
    }

            /**
             * Describes a full-resolution thumbnail that can be downloaded
             * from the WhatsApp media CDN and rendered when the user opens
             * the link preview in expanded form.
             */
            @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.LinkPreviewResponse.LinkPreviewHighQualityThumbnail")
            public static final class LinkPreviewHighQualityThumbnail {
                /**
                 * The direct-path URL fragment used to fetch the encrypted
                 * thumbnail.
                 */
                @ProtobufProperty(index = 1, type = ProtobufType.STRING)
                String directPath;

                /**
                 * The hash of the plaintext thumbnail, used for integrity
                 * verification.
                 */
                @ProtobufProperty(index = 2, type = ProtobufType.STRING)
                String thumbHash;

                /**
                 * The hash of the encrypted thumbnail, used to authenticate
                 * the ciphertext before decryption.
                 */
                @ProtobufProperty(index = 3, type = ProtobufType.STRING)
                String encThumbHash;

                /**
                 * The symmetric media key used to decrypt the thumbnail.
                 */
                @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
                byte[] mediaKey;

                /**
                 * The timestamp at which the media key was generated.
                 */
                @ProtobufProperty(index = 5, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
                Instant mediaKeyTimestampMs;

                /**
                 * The width of the full-resolution thumbnail in pixels.
                 */
                @ProtobufProperty(index = 6, type = ProtobufType.INT32)
                Integer thumbWidth;

                /**
                 * The height of the full-resolution thumbnail in pixels.
                 */
                @ProtobufProperty(index = 7, type = ProtobufType.INT32)
                Integer thumbHeight;


                /**
                 * Constructs a new high-quality thumbnail descriptor.
                 *
                 * @param directPath          the direct-path URL fragment
                 * @param thumbHash           the plaintext thumbnail hash
                 * @param encThumbHash        the encrypted thumbnail hash
                 * @param mediaKey            the symmetric media key
                 * @param mediaKeyTimestampMs the media-key generation time
                 * @param thumbWidth          the thumbnail width in pixels
                 * @param thumbHeight         the thumbnail height in pixels
                 */
                LinkPreviewHighQualityThumbnail(String directPath, String thumbHash, String encThumbHash, byte[] mediaKey, Instant mediaKeyTimestampMs, Integer thumbWidth, Integer thumbHeight) {
                    this.directPath = directPath;
                    this.thumbHash = thumbHash;
                    this.encThumbHash = encThumbHash;
                    this.mediaKey = mediaKey;
                    this.mediaKeyTimestampMs = mediaKeyTimestampMs;
                    this.thumbWidth = thumbWidth;
                    this.thumbHeight = thumbHeight;
                }

                /**
                 * Returns the direct-path URL fragment used to fetch the
                 * encrypted thumbnail.
                 *
                 * @return an {@link Optional} containing the direct path, or
                 *         {@link Optional#empty()} if it was not provided
                 */
                public Optional<String> directPath() {
                    return Optional.ofNullable(directPath);
                }

                /**
                 * Returns the hash of the plaintext thumbnail.
                 *
                 * @return an {@link Optional} containing the hash, or
                 *         {@link Optional#empty()} if it was not provided
                 */
                public Optional<String> thumbHash() {
                    return Optional.ofNullable(thumbHash);
                }

                /**
                 * Returns the hash of the encrypted thumbnail.
                 *
                 * @return an {@link Optional} containing the hash, or
                 *         {@link Optional#empty()} if it was not provided
                 */
                public Optional<String> encThumbHash() {
                    return Optional.ofNullable(encThumbHash);
                }

                /**
                 * Returns the symmetric media key used to decrypt the
                 * thumbnail.
                 *
                 * @return an {@link Optional} containing the media key, or
                 *         {@link Optional#empty()} if it was not provided
                 */
                public Optional<byte[]> mediaKey() {
                    return Optional.ofNullable(mediaKey);
                }

                /**
                 * Returns the timestamp at which the media key was generated.
                 *
                 * @return an {@link Optional} containing the timestamp, or
                 *         {@link Optional#empty()} if it was not provided
                 */
                public Optional<Instant> mediaKeyTimestampMs() {
                    return Optional.ofNullable(mediaKeyTimestampMs);
                }

                /**
                 * Returns the width of the thumbnail in pixels.
                 *
                 * @return an {@link OptionalInt} containing the width, or
                 *         {@link OptionalInt#empty()} if it was not provided
                 */
                public OptionalInt thumbWidth() {
                    return thumbWidth == null ? OptionalInt.empty() : OptionalInt.of(thumbWidth);
                }

                /**
                 * Returns the height of the thumbnail in pixels.
                 *
                 * @return an {@link OptionalInt} containing the height, or
                 *         {@link OptionalInt#empty()} if it was not provided
                 */
                public OptionalInt thumbHeight() {
                    return thumbHeight == null ? OptionalInt.empty() : OptionalInt.of(thumbHeight);
                }

                /**
                 * Sets the direct-path URL fragment used to fetch the
                 * encrypted thumbnail.
                 *
                 * @param directPath the new direct path, may be {@code null}
                 */
                public void setDirectPath(String directPath) {
                    this.directPath = directPath;
    }

                /**
                 * Sets the hash of the plaintext thumbnail.
                 *
                 * @param thumbHash the new hash, may be {@code null}
                 */
                public void setThumbHash(String thumbHash) {
                    this.thumbHash = thumbHash;
    }

                /**
                 * Sets the hash of the encrypted thumbnail.
                 *
                 * @param encThumbHash the new hash, may be {@code null}
                 */
                public void setEncThumbHash(String encThumbHash) {
                    this.encThumbHash = encThumbHash;
    }

                /**
                 * Sets the symmetric media key used to decrypt the
                 * thumbnail.
                 *
                 * @param mediaKey the new media key, may be {@code null}
                 */
                public void setMediaKey(byte[] mediaKey) {
                    this.mediaKey = mediaKey;
    }

                /**
                 * Sets the timestamp at which the media key was generated.
                 *
                 * @param mediaKeyTimestampMs the new timestamp, may be
                 *                            {@code null}
                 */
                public void setMediaKeyTimestampMs(Instant mediaKeyTimestampMs) {
                    this.mediaKeyTimestampMs = mediaKeyTimestampMs;
    }

                /**
                 * Sets the width of the thumbnail in pixels.
                 *
                 * @param thumbWidth the new width, may be {@code null}
                 */
                public void setThumbWidth(Integer thumbWidth) {
                    this.thumbWidth = thumbWidth;
    }

                /**
                 * Sets the height of the thumbnail in pixels.
                 *
                 * @param thumbHeight the new height, may be {@code null}
                 */
                public void setThumbHeight(Integer thumbHeight) {
                    this.thumbHeight = thumbHeight;
    }
            }

            /**
             * Describes additional metadata attached to a link preview that
             * points to a payment link.
             */
            @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.LinkPreviewResponse.PaymentLinkPreviewMetadata")
            public static final class PaymentLinkPreviewMetadata {
                /**
                 * Whether the payment provider is a verified WhatsApp
                 * Business account.
                 */
                @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
                Boolean isBusinessVerified;

                /**
                 * The human-readable name of the payment provider behind the
                 * link.
                 */
                @ProtobufProperty(index = 2, type = ProtobufType.STRING)
                String providerName;


                /**
                 * Constructs a new payment link preview metadata payload.
                 *
                 * @param isBusinessVerified whether the provider is verified
                 * @param providerName       the provider's display name
                 */
                PaymentLinkPreviewMetadata(Boolean isBusinessVerified, String providerName) {
                    this.isBusinessVerified = isBusinessVerified;
                    this.providerName = providerName;
                }

                /**
                 * Returns whether the payment provider is a verified Business
                 * account.
                 *
                 * @return {@code true} if the provider is verified,
                 *         {@code false} otherwise or when the flag was not
                 *         set
                 */
                public boolean isBusinessVerified() {
                    return isBusinessVerified != null && isBusinessVerified;
                }

                /**
                 * Returns the human-readable name of the payment provider.
                 *
                 * @return an {@link Optional} containing the provider name,
                 *         or {@link Optional#empty()} if it was not provided
                 */
                public Optional<String> providerName() {
                    return Optional.ofNullable(providerName);
                }

                /**
                 * Sets whether the payment provider is a verified Business
                 * account.
                 *
                 * @param isBusinessVerified the new flag, may be {@code null}
                 */
                public void setBusinessVerified(Boolean isBusinessVerified) {
                    this.isBusinessVerified = isBusinessVerified;
    }

                /**
                 * Sets the human-readable name of the payment provider.
                 *
                 * @param providerName the new provider name, may be
                 *                     {@code null}
                 */
                public void setProviderName(String providerName) {
                    this.providerName = providerName;
    }
            }
        }

        /**
         * Delivers a previously-placeholder message in its fully serialised
         * form so the requester can ingest it as a normal chat message.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.PlaceholderMessageResendResponse")
        public static final class PlaceholderMessageResendResponse {
            /**
             * The serialised {@code WebMessageInfo} protobuf bytes of the
             * resent message.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
            byte[] webMessageInfoBytes;


            /**
             * Constructs a new placeholder resend response.
             *
             * @param webMessageInfoBytes the serialised message bytes
             */
            PlaceholderMessageResendResponse(byte[] webMessageInfoBytes) {
                this.webMessageInfoBytes = webMessageInfoBytes;
            }

            /**
             * Returns the serialised {@code WebMessageInfo} bytes of the
             * resent message.
             *
             * @return an {@link Optional} containing the serialised bytes, or
             *         {@link Optional#empty()} if they were not provided
             */
            public Optional<byte[]> webMessageInfoBytes() {
                return Optional.ofNullable(webMessageInfoBytes);
            }

            /**
             * Sets the serialised {@code WebMessageInfo} bytes of the resent
             * message.
             *
             * @param webMessageInfoBytes the new serialised bytes, may be
             *                            {@code null}
             */
            public void setWebMessageInfoBytes(byte[] webMessageInfoBytes) {
                this.webMessageInfoBytes = webMessageInfoBytes;
    }
        }

        /**
         * Delivers a fresh snapshot of an app-state collection so the
         * requester can recover from a fatal desynchronisation.
         *
         * <p>The snapshot contains the complete serialised collection and a
         * flag telling the requester whether the payload has been compressed
         * before transmission.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.SyncDSnapshotFatalRecoveryResponse")
        public static final class SyncDSnapshotFatalRecoveryResponse {
            /**
             * The serialised snapshot of the app-state collection.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
            byte[] collectionSnapshot;

            /**
             * Whether the snapshot payload has been compressed before
             * transmission.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
            Boolean isCompressed;


            /**
             * Constructs a new fatal recovery response.
             *
             * @param collectionSnapshot the serialised snapshot bytes
             * @param isCompressed       whether the snapshot is compressed
             */
            SyncDSnapshotFatalRecoveryResponse(byte[] collectionSnapshot, Boolean isCompressed) {
                this.collectionSnapshot = collectionSnapshot;
                this.isCompressed = isCompressed;
            }

            /**
             * Returns the serialised snapshot of the app-state collection.
             *
             * @return an {@link Optional} containing the snapshot, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<byte[]> collectionSnapshot() {
                return Optional.ofNullable(collectionSnapshot);
            }

            /**
             * Returns whether the snapshot payload has been compressed.
             *
             * @return {@code true} if the payload is compressed,
             *         {@code false} otherwise or when the flag was not set
             */
            public boolean isCompressed() {
                return isCompressed != null && isCompressed;
            }

            /**
             * Sets the serialised snapshot of the app-state collection.
             *
             * @param collectionSnapshot the new snapshot, may be {@code null}
             */
            public void setCollectionSnapshot(byte[] collectionSnapshot) {
                this.collectionSnapshot = collectionSnapshot;
    }

            /**
             * Sets whether the snapshot payload has been compressed.
             *
             * @param isCompressed the new flag, may be {@code null}
             */
            public void setCompressed(Boolean isCompressed) {
                this.isCompressed = isCompressed;
    }
        }

        /**
         * Delivers the nonce that a companion uses to link itself against the
         * Meta WAFFLE authentication flow.
         */
        @ProtobufMessage(name = "Message.PeerDataOperationRequestResponseMessage.PeerDataOperationResult.WaffleNonceFetchResponse")
        public static final class WaffleNonceFetchResponse {
            /**
             * The issued WAFFLE nonce.
             */
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String nonce;

            /**
             * The Meta entity FBID of the account that owns the nonce.
             */
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String waEntFbid;


            /**
             * Constructs a new WAFFLE nonce fetch response.
             *
             * @param nonce     the issued nonce
             * @param waEntFbid the Meta entity FBID
             */
            WaffleNonceFetchResponse(String nonce, String waEntFbid) {
                this.nonce = nonce;
                this.waEntFbid = waEntFbid;
            }

            /**
             * Returns the issued WAFFLE nonce.
             *
             * @return an {@link Optional} containing the nonce, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> nonce() {
                return Optional.ofNullable(nonce);
            }

            /**
             * Returns the Meta entity FBID of the account that owns the
             * nonce.
             *
             * @return an {@link Optional} containing the entity FBID, or
             *         {@link Optional#empty()} if it was not provided
             */
            public Optional<String> waEntFbid() {
                return Optional.ofNullable(waEntFbid);
            }

            /**
             * Sets the issued WAFFLE nonce.
             *
             * @param nonce the new nonce, may be {@code null}
             */
            public void setNonce(String nonce) {
                this.nonce = nonce;
    }

            /**
             * Sets the Meta entity FBID of the account that owns the nonce.
             *
             * @param waEntFbid the new entity FBID, may be {@code null}
             */
            public void setWaEntFbid(String waEntFbid) {
                this.waEntFbid = waEntFbid;
    }
        }
    }
}
