package com.github.auties00.cobalt.client;
import com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.emoji.WhatsAppEmoji;

import com.github.auties00.cobalt.listener.DisconnectedListener;
import com.github.auties00.cobalt.listener.LoggedInListener;
import com.github.auties00.cobalt.listener.MessageDeletedListener;
import com.github.auties00.cobalt.listener.MessageStatusListener;
import com.github.auties00.cobalt.listener.NewMessageListener;
import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfile;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Root contract shared by every WhatsApp client flavour Cobalt exposes.
 *
 * <p>WhatsApp can be reached through two fundamentally different transports, and this sealed
 * interface is the common supertype of both:
 * <ul>
 *   <li>{@link LinkedWhatsAppClient} drives the reverse-engineered Web/Desktop/Mobile protocol over
 *       an encrypted XMPP-like socket (Signal/Noise cryptography, protobuf wire format, QR or
 *       registration pairing, app-state sync).</li>
 *   <li>{@link CloudWhatsAppClient} drives Meta's hosted WhatsApp Cloud API: stateless HTTPS/JSON
 *       requests to {@code graph.facebook.com} for outbound traffic and a webhook receiver for
 *       inbound traffic.</li>
 * </ul>
 *
 * <p>This type collects only the operations that are meaningful on both transports: lifecycle
 * control, listener registration, sending a message, reacting, marking a chat read, editing the
 * own business profile, and blocking contacts. Everything that exists on a single transport (the
 * hundreds of socket-only operations on {@link LinkedWhatsAppClient}, the REST-only management
 * operations on {@link CloudWhatsAppClient}) lives on the respective sub-interface. Callers that
 * want flavour-agnostic code depend on this type; callers that need a transport-specific feature
 * depend on the matching sub-interface.
 *
 * <p>The persistence model is intentionally not shared: the Linked client is backed by a rich
 * {@code LinkedWhatsAppStore} holding Signal keys and synced collections, whereas the Cloud client keeps
 * only a lightweight credential session, so no {@code store()} accessor appears here.
 *
 * @apiNote
 * Obtain an instance through {@link #builder()}, which branches into the Linked flavours
 * ({@link WhatsAppClientBuilder#linkedApi()}) or the Cloud flavour
 * ({@link WhatsAppClientBuilder#cloudApi()}). Because the type is sealed, a {@code switch} over a
 * {@code WhatsAppClient} is exhaustive with {@link LinkedWhatsAppClient} and
 * {@link CloudWhatsAppClient} as the only cases.
 *
 * @param <SELF> the concrete client flavour, bound to itself ({@link LinkedWhatsAppClient} or
 *               {@link CloudWhatsAppClient}) so that lifecycle methods return, and the shared
 *               listener callbacks receive, the exact flavour rather than this root type
 */
public sealed interface WhatsAppClient<SELF extends WhatsAppClient<SELF>> permits LinkedWhatsAppClient, CloudWhatsAppClient {
    /**
     * Returns the entry point for assembling a configured {@link WhatsAppClient} of either flavour.
     *
     * @apiNote
     * The returned builder exposes {@link WhatsAppClientBuilder#linkedApi()} for the socket-based
     * Web/Mobile flavours and {@link WhatsAppClientBuilder#cloudApi()} for the Cloud API flavour.
     *
     * @return the shared {@link WhatsAppClientBuilder} singleton
     */
    static WhatsAppClientBuilder builder() {
        return WhatsAppClientBuilder.INSTANCE;
    }

    /**
     * Brings the client up and starts processing traffic.
     *
     * <p>For {@link LinkedWhatsAppClient} this opens the encrypted socket and starts the stanza
     * pump; for {@link CloudWhatsAppClient} this validates the access token and starts the webhook
     * receiver. The method returns as soon as the client is live; subsequent events are delivered
     * asynchronously to registered listeners.
     *
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     * @throws IllegalStateException if the client is already connected
     */
    SELF connect();

    /**
     * Tears the client down, releasing the socket or stopping the webhook receiver.
     *
     * <p>After this call the client is inert until {@link #connect()} or {@link #reconnect()} is
     * invoked again. Pending in-flight work is allowed to settle where the transport supports it.
     */
    void disconnect();

    /**
     * Tears the client down and immediately brings it back up.
     *
     * <p>For {@link LinkedWhatsAppClient} this re-establishes the socket reusing the persisted
     * credentials; for {@link CloudWhatsAppClient} this restarts the webhook receiver and
     * re-validates the access token.
     *
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF reconnect();

    /**
     * Returns whether the client is currently live.
     *
     * @return {@code true} if the socket is up (Linked) or the webhook receiver is running (Cloud),
     *         {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Blocks the calling thread until the client disconnects.
     *
     * <p>The call returns when a terminal disconnect lands: a non-reconnecting socket close for
     * {@link LinkedWhatsAppClient}, or the webhook receiver stopping for
     * {@link CloudWhatsAppClient}. It is the idiomatic way to keep a process alive for the lifetime
     * of a session.
     *
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF waitForDisconnection();

    /**
     * Sends a message to a recipient and returns the key that identifies it.
     *
     * <p>The {@link LinkedMessageContainer} is the universal, transport-independent message model. The
     * Linked client encrypts and dispatches it over the socket; the Cloud client translates it to
     * the Cloud {@code /messages} REST shape and posts it. The returned {@link MessageKey} carries
     * the recipient and the freshly minted message id (the {@code wamid} on the Cloud transport),
     * so callers can correlate later status updates.
     *
     * @param recipient the destination chat or user
     * @param message   the message content to send
     * @return the key identifying the sent message
     */
    MessageKey sendMessage(JidProvider recipient, LinkedMessageContainer message);

    /**
     * Sends a pre-assembled {@link LinkedMessageInfo}.
     *
     * <p>This overload is for callers that have already built a full message descriptor (recipient,
     * content, and metadata). The Cloud client reads the recipient and {@link LinkedMessageContainer} out
     * of the descriptor and posts them; the Linked client dispatches the descriptor directly.
     *
     * @param messageInfo the message descriptor to send
     */
    void sendMessage(LinkedMessageInfo messageInfo);

    /**
     * Reacts to a message with an emoji.
     *
     * <p>Replacing an existing reaction is done by sending a new emoji for the same
     * {@link MessageKey}; clearing it is done with {@link #removeReaction(MessageKey)}.
     *
     * @param messageKey the key of the message to react to
     * @param emoji      the reaction emoji
     */
    void addReaction(MessageKey messageKey, String emoji);

    /**
     * Reacts to a message with a typed emoji.
     *
     * <p>Convenience overload of {@link #addReaction(MessageKey, String)} that sends the
     * {@linkplain WhatsAppEmoji#value() canonical value} of {@code emoji}. Replacing an existing
     * reaction is done by reacting again with a different emoji; clearing it is done with
     * {@link #removeReaction(MessageKey)}.
     *
     * @param messageKey the key of the message to react to
     * @param emoji      the reaction emoji
     * @throws NullPointerException if {@code emoji} is {@code null}
     */
    default void addReaction(MessageKey messageKey, WhatsAppEmoji emoji) {
        Objects.requireNonNull(emoji, "emoji cannot be null");
        addReaction(messageKey, emoji.value());
    }

    /**
     * Removes the reaction previously sent to a message.
     *
     * @param messageKey the key of the message whose reaction is cleared
     */
    void removeReaction(MessageKey messageKey);

    /**
     * Marks a chat as read.
     *
     * <p>On the Linked transport this sends a read receipt covering the chat; on the Cloud
     * transport, where read receipts are addressed to a specific inbound message, the client marks
     * the most recent inbound message of the chat as read.
     *
     * @param chat the chat to mark read
     */
    void markChatAsRead(JidProvider chat);

    /**
     * Updates the business profile of the account this client operates.
     *
     * <p>Both transports expose the same {@link BusinessProfile} model. The Linked client pushes it
     * over the socket; the Cloud client posts it to the phone number's
     * {@code whatsapp_business_profile} edge.
     *
     * @param profile the new business profile
     */
    void editBusinessProfile(BusinessProfile profile);

    /**
     * Blocks a contact so they can no longer message this account.
     *
     * @param contact the contact to block
     */
    void blockContact(JidProvider contact);

    /**
     * Unblocks a previously {@linkplain #blockContact(JidProvider) blocked} contact.
     *
     * @param contact the contact to unblock
     */
    void unblockContact(JidProvider contact);

    /**
     * Marks a single message as read.
     *
     * <p>On the Linked transport this issues a read receipt for the message; on the Cloud transport
     * this posts a {@code status: read} update keyed by the message id. This is the precise,
     * message-keyed counterpart of {@link #markChatAsRead(JidProvider)}.
     *
     * @param messageKey the key of the message to mark read
     */
    void markMessageAsRead(MessageKey messageKey);

    /**
     * Shows a typing indicator in the chat of an inbound message.
     *
     * <p>The indicator is keyed by an inbound {@link MessageKey} because the Cloud API only allows a
     * typing indicator in response to a recently received message; the Linked client derives the chat
     * from the key and broadcasts a composing chat-state. The indicator is dismissed automatically
     * when a reply is sent or after the server-side timeout elapses.
     *
     * @param inboundMessage the key of the inbound message whose chat shows the indicator
     */
    void sendTypingIndicator(MessageKey inboundMessage);

    /**
     * Queries the business profile of the account this client operates.
     *
     * @return the own business profile, or {@link Optional#empty()} when none is set
     */
    Optional<BusinessProfile> queryBusinessProfile();

    /**
     * Lists the contacts this account has blocked.
     *
     * @return the blocked contacts
     */
    List<Jid> queryBlockedContacts();

    /**
     * Registers a listener for inbound messages.
     *
     * @param listener the listener
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF addNewMessageListener(NewMessageListener<? super SELF> listener);

    /**
     * Registers a listener for message status transitions.
     *
     * @param listener the listener
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF addMessageStatusListener(MessageStatusListener<? super SELF> listener);

    /**
     * Registers a listener for message deletions.
     *
     * @param listener the listener
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF addMessageDeletedListener(MessageDeletedListener<? super SELF> listener);

    /**
     * Registers a listener invoked once the client is connected and authenticated.
     *
     * @param listener the listener
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF addLoggedInListener(LoggedInListener<? super SELF> listener);

    /**
     * Registers a listener invoked when the client disconnects.
     *
     * @param listener the listener
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF addDisconnectedListener(DisconnectedListener<? super SELF> listener);

    /**
     * Unregisters a previously registered listener.
     *
     * <p>Removing a listener that is not registered is a no-op.
     *
     * @param listener the listener to remove
     * @return {@code this}, narrowed to the concrete flavour, for fluent chaining
     */
    SELF removeListener(WhatsAppListener listener);
}
