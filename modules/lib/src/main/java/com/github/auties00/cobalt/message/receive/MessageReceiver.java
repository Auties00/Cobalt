package com.github.auties00.cobalt.message.receive;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveStanza;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainerSpec;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.util.Objects;

/**
 * Sealed base for the two inbound message receivers that collapse WhatsApp Web's
 * {@code WAWebHandleMsg} entry point into a per-stanza-class implementation.
 *
 * <p>The base owns the shared protobuf decoder and the self-account JID comparison so
 * both subclasses agree on which messages are self-authored. Subclasses are
 * package-private and only reachable through {@link MessageReceivingService#process(Stanza)};
 * neither is instantiated nor subclassed outside this package.
 *
 * @implSpec
 * A subclass must convert a raw inbound {@code <message>} stanza into the appropriate
 * {@link LinkedMessageInfo} subtype via {@link #receive(Stanza, Jid)}; returning {@code null}
 * is reserved for stanzas that the receiver intentionally drops without raising an
 * error (for example unavailable fanout placeholders or newsletter messages with no
 * payload).
 *
 * @param <T> the concrete {@link LinkedMessageInfo} subtype produced by the receiver
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsg")
abstract sealed class MessageReceiver<T extends LinkedMessageInfo>
        permits ChatMessageReceiver, NewsletterMessageReceiver {

    /**
     * The logger for {@link MessageReceiver}.
     */
    private static final System.Logger LOGGER = Log.get(MessageReceiver.class);

    /**
     * Central session store shared with every receive subclass.
     *
     * <p>Holds the local PN, LID, Signal sessions, sender keys, and the per-chat
     * message cache that every concrete receiver uses to identify the self account and
     * to look up bot-message metadata.
     */
    final LinkedWhatsAppStore store;

    /**
     * Constructs a receiver bound to the given store.
     *
     * @param store the central session store; must be non-{@code null}
     * @throws NullPointerException if {@code store} is {@code null}
     */
    MessageReceiver(LinkedWhatsAppStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * Processes an incoming {@code <message>} stanza into the receiver's concrete
     * {@link LinkedMessageInfo} subtype.
     *
     * <p>Invoked by {@link MessageReceivingService#process(Stanza)} after the router has
     * selected the receiver from the {@code from} JID. A {@code null} return means the
     * message was intentionally dropped (an unavailable fanout placeholder or a
     * newsletter with no payload) and the caller acknowledges with a plain ack.
     *
     * @implSpec
     * A subclass must return {@code null} only for intentional silent drops; it must
     * throw a
     * {@link com.github.auties00.cobalt.exception.linked.WhatsAppMessageException.Receive}
     * subtype for failures so the orchestrator can decide between retry, NACK, and
     * dedup.
     *
     * @param stanza    the raw incoming {@code <message>} stanza
     * @param fromJid the JID parsed from the stanza's {@code from} attribute
     * @return the processed message info, or {@code null} for an intentional silent
     *         drop
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsg", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    abstract T receive(Stanza stanza, Jid fromJid);

    /**
     * Returns the logged-in device's PN JID or fails fast when no session is active.
     *
     * <p>Subclasses call this to construct device-sent-message fallbacks and bot
     * target-sender JIDs when the stanza does not carry one.
     *
     * @implNote
     * This implementation throws {@link IllegalStateException} because the receive
     * pipeline is only entered while a session is active; the orchestrator drains
     * pending messages before tearing the session down.
     *
     * @return the local PN JID
     * @throws IllegalStateException if no session is active
     */
    Jid requireSelfJid() {
        return store.accountStore().jid().orElseThrow(() ->
                new IllegalStateException("Not logged in"));
    }

    /**
     * Decodes the raw protobuf plaintext into a {@link LinkedMessageContainer}, returning
     * {@code null} on parse failure rather than throwing.
     *
     * <p>Both receivers call this after obtaining the plaintext (after decryption on
     * the chat path, after reading the {@code <plaintext>} child on the newsletter
     * path). A {@code null} return lets the caller decide whether to NACK, retry, or
     * silently drop based on the originating stanza class.
     *
     * @implNote
     * This implementation logs the failure at WARNING level and swallows the
     * exception; WhatsApp Web's {@code WAWebHandleMsgProcess.processDecryptedMessageProto}
     * raises a {@code MessageValidationError} instead, but Cobalt centralises the NACK
     * decision in the caller rather than the decoder.
     *
     * @param messageId the message id used for log context
     * @param plaintext the raw protobuf bytes
     * @return the decoded container, or {@code null} when the protobuf cannot be
     *         parsed
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgProcess", exports = "processDecryptedMessageProto",
            adaptation = WhatsAppAdaptation.ADAPTED)
    LinkedMessageContainer decodeProtobuf(String messageId, byte[] plaintext) {
        try {
            return LinkedMessageContainerSpec.decode(plaintext);
        } catch (Exception e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING,
                    "failed to decode protobuf for message " + messageId, e);
            return null;
        }
    }

    /**
     * Returns whether the parsed stanza was authored by the logged-in user.
     *
     * <p>Convenience overload over {@link #isFromMe(Jid)} for callers that already hold
     * the parsed stanza; {@link ChatMessageReceiver} uses it to gate recipient-attribute
     * validation and device-sent-message expectations.
     *
     * @param stanza the parsed stanza
     * @return {@code true} if {@link MessageReceiveStanza#senderJid()} matches the
     *         local PN or LID account
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser", exports = "isMeAccount",
            adaptation = WhatsAppAdaptation.ADAPTED)
    boolean isFromMe(MessageReceiveStanza stanza) {
        return isFromMe(stanza.senderJid());
    }

    /**
     * Returns whether the given sender JID matches the logged-in user's account in
     * either the PN or LID addressing mode.
     *
     * <p>Both addressing modes are checked so a message addressed via either appears as
     * self, which matters for the companion-device receipts that always loop back via
     * the primary PN. A {@code null} sender returns {@code false}.
     *
     * @implNote
     * This implementation compares user-level JIDs via {@link Jid#toUserJid()} so
     * companion-device addresses ({@code user:device@server}) collapse to the same
     * account as the primary {@code user@server} JID.
     *
     * @param senderJid the sender JID to test; {@code null} returns {@code false}
     * @return {@code true} if {@code senderJid} matches the local PN or LID account
     */
    @WhatsAppWebExport(moduleName = "WAWebUserPrefsMeUser", exports = "isMeAccount",
            adaptation = WhatsAppAdaptation.ADAPTED)
    boolean isFromMe(Jid senderJid) {
        if (senderJid == null) {
            return false;
        }
        var senderUser = senderJid.toUserJid();
        var selfPn = store.accountStore().jid().orElse(null);
        if (selfPn != null && senderUser.equals(selfPn.toUserJid())) {
            return true;
        }
        var selfLid = store.accountStore().lid().orElse(null);
        return selfLid != null && senderUser.equals(selfLid.toUserJid());
    }
}
