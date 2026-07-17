package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * The addressing shape of an incoming message stanza, classified once during
 * parsing from the {@code from} JID, the optional {@code participant}
 * attribute, and the logged-in user's identity.
 *
 * <p>This is the single discriminator the receive pipeline branches on for
 * every per-message decision: which device-sent-message unwrapping rule to
 * apply, what receipt type to emit, whether to process incoming sender-key
 * distribution payloads, whether the broadcast contact list is meaningful, and
 * what placeholder shape to surface to the UI when decryption fails. Set by
 * {@link MessageReceiveStanzaParser} and exposed via
 * {@link MessageReceiveStanza#messageType()}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgTypes.flow")
@WhatsAppWebModule(moduleName = "WAWebHandleMsgCommon")
public enum MessageType {
    /**
     * A 1:1 chat message between two user, LID, or bot JIDs that is not a
     * peer-protocol message.
     *
     * <p>The receive pipeline's default path: decrypt the per-device envelope,
     * decode the inner protobuf, persist as a chat message.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    CHAT,

    /**
     * A group or community message, addressed by the {@code from} attribute
     * pointing at the group JID and the {@code participant} attribute
     * identifying the sender's device inside that group.
     *
     * <p>Drives the sender-key decryption path for the group; the participant
     * JID also feeds the LID-versus-PN addressing decision that selects the
     * Signal protocol address.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    GROUP,

    /**
     * A broadcast-list message that this device's own primary device sent; the
     * {@code <participants>} child carries the per-recipient broadcast contact
     * list that the local device mirrors.
     *
     * <p>Distinguished from {@link #OTHER_BROADCAST} by an is-me-account check
     * on the participant inside the parser; selects the code path that persists
     * the broadcast contact list locally so the companion device can render the
     * recipients exactly as the primary sender did.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    PEER_BROADCAST,

    /**
     * A broadcast-list message addressed to the local user from another user's
     * broadcast list.
     *
     * <p>The receive pipeline treats this as an inbound 1:1 message wrapped in a
     * broadcast envelope; the {@code eph_setting} attribute on the stanza is the
     * per-recipient ephemeral setting.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    OTHER_BROADCAST,

    /**
     * A status-broadcast message originated by the local user's primary device
     * and encrypted directly (every {@code <enc>} child is non-{@code skmsg}).
     *
     * <p>Status posts from another of the local user's devices land here so the
     * current device can mirror the post locally; the broadcast contact list
     * (mapped onto {@link MessageReceiveStanza#bclParticipants()}) tells the UI
     * who the post was sent to.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    DIRECT_PEER_STATUS,

    /**
     * A status-broadcast message from another user, or a self-originated status
     * whose payload is not directly encrypted (carries an {@code skmsg}).
     *
     * <p>Decrypted via the sender's status-broadcast sender key and surfaced
     * into the status feed.
     *
     * @implNote
     * This implementation collapses the upstream direct and non-direct
     * other-status variants into one enum value because Cobalt's downstream
     * paths do not branch on the boolean directly; callers that need it can
     * read {@link MessageReceiveStanza#isDirect()}.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.DIRECT)
    OTHER_STATUS,

    /**
     * A peer-protocol message: a 1:1 chat message between two of the local
     * user's own devices, carrying control payloads such as app-state syncs,
     * history transfer, or device-list rotations.
     *
     * <p>The receive pipeline routes this away from the chat database and into
     * the peer-protocol handler; the message never surfaces to the UI.
     *
     * @implNote
     * This implementation fuses the upstream two-axis check (a chat message
     * combined with the peer message category) into a single enum value; the
     * parser does the fusion so the downstream pipeline can dispatch on one
     * enum.
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgTypes.flow", exports = "MESSAGE_TYPE",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgCommon", exports = "MSG_CATEGORY",
            adaptation = WhatsAppAdaptation.ADAPTED)
    PEER_CHAT,
}
