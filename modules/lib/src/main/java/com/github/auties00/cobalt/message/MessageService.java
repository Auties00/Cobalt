package com.github.auties00.cobalt.message;

import com.github.auties00.cobalt.ack.AckResult;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppMessageException;
import com.github.auties00.cobalt.message.receive.MessageReceivingService;
import com.github.auties00.cobalt.message.send.MessageSendingService;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;

/**
 * Facade that fans message traffic between the outbound send pipeline and the inbound receive
 * pipeline.
 *
 * <p>Code that wants to send or react to messages talks to this service instead of touching the
 * {@link MessageSendingService} and {@link MessageReceivingService} pair directly. The two
 * pipelines share the {@link LinkedWhatsAppClient#store() client store}, so the send and receive
 * sides observe a single source of truth for sessions, devices, and pending-message caches.
 *
 * @implSpec
 * Implementations must forward every outbound call to a {@link MessageSendingService} and every
 * inbound call to a {@link MessageReceivingService} backed by the same store, and must own no
 * additional message state of their own.
 */
public interface MessageService {
    /**
     * Sends a fresh outbound message to the given chat.
     *
     * <p>This allocates a message id, resolves the sender and recipient device
     * lists, encrypts the payload, ships the fanout, and blocks on the server
     * acknowledgment. This overload is for plain user-facing sends where the
     * caller does not need to pre-build a {@link LinkedMessageInfo}.
     *
     * @implSpec
     * Implementations must delegate to {@link MessageSendingService#send(Jid, LinkedMessageContainer)}.
     *
     * @param chatJid   the recipient chat JID
     * @param container the message payload
     * @return the server acknowledgment outcome
     * @throws NullPointerException if {@code chatJid} or {@code container} is
     *                              {@code null}
     * @see MessageSendingService#send(Jid, LinkedMessageContainer)
     */
    AckResult send(Jid chatJid, LinkedMessageContainer container);

    /**
     * Sends a pre-populated {@link LinkedMessageInfo} the caller has already keyed,
     * timestamped, and decorated with any extension metadata.
     *
     * <p>This overload is for messages prepared by the caller, for example when
     * rehydrating a stored draft or re-transmitting after a nack. The sending
     * service does not mutate the supplied {@link LinkedMessageInfo}; the same
     * instance can safely be passed again on a retry.
     *
     * @implSpec
     * Implementations must delegate to {@link MessageSendingService#send(LinkedMessageInfo)}.
     *
     * @param messageInfo the prepared outbound message, either a
     *                    {@link ChatMessageInfo} or a {@link NewsletterMessageInfo}
     * @return the server acknowledgment outcome
     * @throws NullPointerException if {@code messageInfo} is {@code null}
     * @see MessageSendingService#send(LinkedMessageInfo)
     */
    AckResult send(LinkedMessageInfo messageInfo);

    /**
     * Sends a peer protocol message to one of the current account's own linked
     * devices.
     *
     * <p>Peer messages never reach other users; they carry app-state sync
     * payloads, key share notifications, and fatal-exception reports between
     * linked devices of the same account. The {@code targetDevice} argument is
     * normally the account's primary device JID.
     *
     * @implSpec
     * Implementations must delegate to
     * {@link MessageSendingService#sendPeer(Jid, ChatMessageInfo)}.
     *
     * @param targetDevice the target device JID
     * @param messageInfo  the peer protocol message
     * @return the server acknowledgment outcome
     * @throws NullPointerException if {@code targetDevice} or
     *                              {@code messageInfo} is {@code null}
     * @see MessageSendingService#sendPeer(Jid, ChatMessageInfo)
     */
    AckResult sendPeer(Jid targetDevice, ChatMessageInfo messageInfo);

    /**
     * Processes a single inbound {@code <message>} stanza and returns the typed
     * {@link LinkedMessageInfo} ready for consumption.
     *
     * <p>Newsletter messages are returned as {@link NewsletterMessageInfo};
     * every other shape goes through the Signal decryption pipeline and is
     * returned as {@link ChatMessageInfo}. Fanout placeholders for messages the
     * server failed to deliver produce a {@code null} return so the caller can
     * distinguish them from genuine payloads.
     *
     * @implSpec
     * Implementations must delegate to {@link MessageReceivingService#process(Stanza)}.
     *
     * @param stanza the raw inbound {@code <message>} stanza
     * @return the processed {@link LinkedMessageInfo}, or {@code null} for
     *         unavailable fanout placeholders
     * @throws NullPointerException if {@code stanza} is {@code null}
     * @throws WhatsAppMessageException.Receive
     *         if decryption or validation fails for an encrypted payload
     * @see MessageReceivingService#process(Stanza)
     */
    LinkedMessageInfo process(Stanza stanza);

    /**
     * Resolves the LID addressing a one-to-one call offer to {@code peer} must use.
     *
     * <p>WhatsApp addresses call signaling by LID: the offer's {@code <call to>} and per-device
     * {@code <destination>} fanout carry the peer's LID, not its phone number. This resolves the peer's
     * phone number to its LID (querying the server through the USync LID protocol via
     * {@link com.github.auties00.cobalt.device.DeviceService#queryUserLid(Jid)} when no mapping is
     * cached), synchronises the peer's device list in that LID addressing mode, and returns the peer LID
     * user JID together with one LID device JID per device.
     *
     * @implSpec
     * Implementations must resolve the peer to LID before synchronising its device list, must address
     * the returned device JIDs in LID, and must throw rather than return a phone-number-addressed
     * result when no LID can be resolved; WhatsApp Web aborts the call instead of sending a phone-number
     * offer the server rejects with {@code error="439"}.
     *
     * @param peer the peer user JID being called, in phone-number or LID form
     * @return the resolved LID addressing for the peer
     * @throws NullPointerException  if {@code peer} is {@code null}
     * @throws IllegalStateException if the peer has no resolvable LID
     */
    CallPeerAddressing resolveCallPeerAddressing(Jid peer);

    /**
     * Decrypts a Signal-encrypted call payload into its plaintext bytes.
     *
     * <p>Used by the call layer's {@code <enc_rekey>} runtime to recover the
     * {@link com.github.auties00.cobalt.wire.linked.call.datachannel.E2eRekeyPayload E2eRekeyPayload}
     * the peer published. The {@code encType} attribute is the wire-level Signal envelope
     * variant ({@code msg} or {@code pkmsg}) carried on the inbound {@code <enc>} child.
     *
     * @param senderJid  the device JID that authored the envelope
     * @param encType    the Signal envelope variant
     * @param ciphertext the Signal-encrypted bytes
     * @return the plaintext bytes
     * @throws NullPointerException             if any argument is {@code null}
     * @throws WhatsAppMessageException.Receive if decryption or validation fails
     */
    byte[] processCall(Jid senderJid, MessageEncryptionType encType, byte[] ciphertext);

    /**
     * Clears the pending-message deduplication cache held by the receiving
     * service.
     *
     * <p>This runs when the offline-delivery phase ends so stanzas replayed in a
     * new session are not mistakenly treated as duplicates of pre-reconnect
     * traffic.
     *
     * @implSpec
     * Implementations must delegate to {@link MessageReceivingService#clearPendingMessages()}.
     *
     * @see MessageReceivingService#clearPendingMessages()
     */
    void clearPendingMessages();

    /**
     * Carries the LID addressing for a one-to-one call peer.
     *
     * <p>Produced by {@link #resolveCallPeerAddressing(Jid)} and consumed by the call-placement path to
     * address the outbound offer: {@link #peer()} is the peer's LID user JID written onto the
     * {@code <call to>} envelope, and {@link #peerDevices()} are the per-device LID JIDs the offer fans
     * the call key out to.
     *
     * @param peer        the peer's LID user JID; never {@code null}
     * @param peerDevices the peer's per-device LID JIDs; never {@code null}, never empty
     */
    record CallPeerAddressing(Jid peer, List<Jid> peerDevices) {}
}
