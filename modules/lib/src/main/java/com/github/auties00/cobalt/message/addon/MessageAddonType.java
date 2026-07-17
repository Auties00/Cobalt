package com.github.auties00.cobalt.message.addon;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Identifies the use case that WhatsApp mixes into the HKDF info parameter
 * when it derives a message-secret-bound encryption or MAC key.
 *
 * <p>Each variant binds an HKDF derivation to a distinct label so keys
 * derived for different use cases never collide even when the parent message,
 * sender, and stanza id are otherwise identical. Eight variants drive
 * dual-encrypted addons (poll votes, encrypted reactions, encrypted comments,
 * event responses, event edits, poll edits, poll add-options, message edits);
 * the ninth, {@link #REPORT_TOKEN}, drives the HMAC franking tag the server
 * verifies on abuse reports. Each variant also carries whether the use case
 * authenticates additional data through AES-GCM AAD; only {@link #POLL_VOTE}
 * and {@link #EVENT_RESPONSE} set that flag.
 */
@WhatsAppWebModule(moduleName = "WAUseCaseSecret")
@WhatsAppWebModule(moduleName = "WAWebAddonEncryption")
public enum MessageAddonType {
    /**
     * Identifies the poll vote addon, dual-encrypted under a key derived with
     * this label.
     *
     * <p>Drives {@link EncMessageFactory#encryptPollVote(java.util.List,
     * com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo,
     * com.github.auties00.cobalt.wire.core.jid.Jid)}. Sets the AAD flag so the
     * AES-GCM cipher binds {@code stanzaId + "\0" + voterJid} into the tag,
     * preventing the server from reattributing a vote to a different user.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    POLL_VOTE("Poll Vote", true),

    /**
     * Identifies the encrypted reaction addon used in community and
     * announcement group threads.
     *
     * <p>Drives {@link EncMessageFactory#encryptReaction(
     * com.github.auties00.cobalt.wire.linked.message.text.ReactionMessage,
     * com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo,
     * com.github.auties00.cobalt.wire.core.jid.Jid)}. The default non-encrypted
     * reaction wire format leaks the emoji content to the server, which is
     * unacceptable on such threads; this label selects the dual-encrypted
     * variant.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    ENC_REACTION("Enc Reaction", false),

    /**
     * Identifies the encrypted comment addon used in community and
     * announcement group threads.
     *
     * <p>Drives {@link EncMessageFactory#encryptComment(
     * com.github.auties00.cobalt.wire.linked.message.text.CommentMessage,
     * com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo,
     * com.github.auties00.cobalt.wire.core.jid.Jid)}. The inner message container
     * payload is dual-encrypted so the server can route the comment without
     * reading its body.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    ENC_COMMENT("Enc Comment", false),

    /**
     * Identifies the reporting-token label.
     *
     * <p>Unlike the other variants, this label does not drive AES-GCM
     * encryption of an addon payload. It is mixed into the HKDF info when
     * deriving the 32-byte HMAC key used to compute the franking tag that the
     * server verifies on abuse reports.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    REPORT_TOKEN("Report Token", false),

    /**
     * Identifies the event response addon (RSVP).
     *
     * <p>Sets the AAD flag so the AES-GCM cipher binds
     * {@code stanzaId + "\0" + responderJid} into the tag, preventing the
     * server from lifting an encrypted RSVP emitted by one user and replaying
     * it as if it came from a different user.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    EVENT_RESPONSE("Event Response", true),

    /**
     * Identifies the event edit addon, emitted when an event organiser updates
     * the details of a previously scheduled event.
     *
     * <p>The edited payload is dual-encrypted so the server can route the edit
     * without reading its body. Maps to WA Web's {@code EVENT_EDIT_ENCRYPTED}
     * enum entry.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    EVENT_EDIT("Event Edit", false),

    /**
     * Identifies the poll edit addon, emitted when a poll creator updates the
     * poll question, options, or end-time.
     *
     * <p>Maps to WA Web's {@code POLL_EDIT_ENCRYPTED} enum entry.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    POLL_EDIT("Poll Edit", false),

    /**
     * Identifies the poll add-option addon, emitted when a poll participant
     * adds a new option to a poll that allows open-ended contributions.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    POLL_ADD_OPTION("Poll Add Option", false),

    /**
     * Identifies the message edit addon, emitted when a user edits a
     * previously sent message.
     *
     * <p>The edited payload is dual-encrypted so the server can route the
     * stanza without reading the new content.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    MESSAGE_EDIT("Message Edit", false);

    /**
     * Holds the label mixed into the HKDF info parameter for this use case.
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    private final String value;

    /**
     * Holds whether this use case binds the stanza id and addon sender JID as
     * AES-GCM AAD.
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = {"encryptAddOn", "decryptAddOn"},
            adaptation = WhatsAppAdaptation.DIRECT)
    private final boolean usesAad;

    /**
     * Constructs an addon type bound to the given HKDF label and AAD flag.
     *
     * @param value   the HKDF info string associated with this use case
     * @param usesAad whether this use case binds stanza id and addon sender
     *                as AES-GCM AAD
     */
    MessageAddonType(String value, boolean usesAad) {
        this.value = value;
        this.usesAad = usesAad;
    }

    /**
     * Returns the HKDF info label associated with this use case.
     *
     * <p>The label is consumed inside {@link MessageAddonEncryption#encrypt(
     * byte[], byte[], String, com.github.auties00.cobalt.wire.core.jid.Jid,
     * com.github.auties00.cobalt.wire.core.jid.Jid, MessageAddonType)} when
     * assembling the HKDF info parameter; it is never exposed to callers of
     * the high-level factory methods in {@link EncMessageFactory}.
     *
     * @return the HKDF info string
     */
    @WhatsAppWebExport(moduleName = "WAUseCaseSecret", exports = "UseCaseSecretModificationType",
            adaptation = WhatsAppAdaptation.DIRECT)
    public String value() {
        return value;
    }

    /**
     * Returns whether this use case binds the stanza id and addon sender JID
     * as AES-GCM AAD.
     *
     * <p>Only {@link #POLL_VOTE} and {@link #EVENT_RESPONSE} set the flag;
     * those addons need per-sender binding because the server otherwise sees
     * enough structural metadata to attempt cross-user rebinding of the
     * ciphertext.
     *
     * @return {@code true} when AAD should be applied during encrypt and
     *         decrypt
     */
    @WhatsAppWebExport(moduleName = "WAWebAddonEncryption", exports = {"encryptAddOn", "decryptAddOn"},
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean usesAad() {
        return usesAad;
    }
}
