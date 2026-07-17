package com.github.auties00.cobalt.wire.linked.message.system.peer;

import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;

/**
 * Enumerates the kinds of peer-to-peer data operation that one of a user's
 * devices can request from another.
 *
 * <p>Peer data operations are messages sent between the different devices
 * owned by the same user (primary and companions) to ask the peer to perform
 * a task on behalf of the requester. Typical examples include asking the
 * primary to reupload an expired sticker, generate a link preview, back-fill
 * a slice of chat history, resend a missing message or fetch an
 * authentication nonce. Each constant identifies one such task and matches a
 * corresponding request body inside {@link PeerDataOperationRequestMessage}
 * and a response body inside {@link PeerDataOperationRequestResponseMessage}.
 */
@ProtobufEnum(name = "Message.PeerDataOperationRequestType")
public enum PeerDataOperationRequestType {
    /**
     * Requests that the peer reuploads a sticker whose media has expired on
     * the server.
     */
    UPLOAD_STICKER(0),
    /**
     * Requests that the peer sends its list of recent stickers so the client
     * can bootstrap its sticker picker.
     */
    SEND_RECENT_STICKER_BOOTSTRAP(1),
    /**
     * Requests that the peer fetches and returns a link preview for a URL
     * that the requesting device cannot resolve on its own.
     */
    GENERATE_LINK_PREVIEW(2),
    /**
     * Requests an on-demand back-fill of additional older messages for a
     * specific chat.
     */
    HISTORY_SYNC_ON_DEMAND(3),
    /**
     * Requests that the peer resends a message that was delivered only as a
     * placeholder stub.
     */
    PLACEHOLDER_MESSAGE_RESEND(4),
    /**
     * Requests a fresh nonce that can be used to link this device against the
     * Meta authentication (WAFFLE) flow.
     */
    WAFFLE_LINKING_NONCE_FETCH(5),
    /**
     * Requests a complete on-demand history sync covering the user's entire
     * accessible message archive.
     */
    FULL_HISTORY_SYNC_ON_DEMAND(6),
    /**
     * Requests a nonce that allows the companion to fetch Meta-side metadata
     * about this account.
     */
    COMPANION_META_NONCE_FETCH(7),
    /**
     * Requests the primary to generate a fresh snapshot of an app-state
     * collection after the companion has suffered a fatal desynchronisation.
     */
    COMPANION_SYNCD_SNAPSHOT_FATAL_RECOVERY(8),
    /**
     * Requests a nonce used to resolve the canonical cross-platform user
     * identity for this companion.
     */
    COMPANION_CANONICAL_USER_NONCE_FETCH(9),
    /**
     * Requests that the peer regenerates or resends a specific history sync
     * chunk that failed to arrive.
     */
    HISTORY_SYNC_CHUNK_RETRY(10),
    /**
     * Signals a Galaxy flow action (for example launching or advancing a
     * multi-step flow) to the peer.
     */
    GALAXY_FLOW_ACTION(11);

    /**
     * Constructs a new constant with the given protobuf wire index.
     *
     * @param index the numeric index used on the wire
     */
    PeerDataOperationRequestType(@ProtobufEnumIndex int index) {
        this.index = index;
    }

    /**
     * The numeric index used to represent this constant on the protobuf wire.
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
