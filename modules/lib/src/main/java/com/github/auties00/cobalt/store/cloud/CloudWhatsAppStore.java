package com.github.auties00.cobalt.store.cloud;

import com.github.auties00.cobalt.store.WhatsAppStore;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The persistent state backing a {@link com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient}.
 *
 * <p>The Cloud transport is stateless beyond its credentials, so this store holds only the access
 * token and identifiers that address the WhatsApp Business Account assets, the graph API version, and
 * the configuration of the built-in webhook receiver. It is the Cloud analogue of
 * {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore}, but it carries credentials and
 * endpoint configuration rather than Signal keys and synced collections.
 *
 * <p>Beyond the static configuration, the store also accumulates the small amount of runtime state the
 * Cloud transport cannot reconstruct on its own: the id of the last inbound message seen in each chat,
 * which {@link com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient#markChatAsRead} needs because
 * the Cloud API marks a message read rather than a chat.
 *
 * @apiNote
 * Embedders do not implement this interface. An instance is configured through
 * {@link com.github.auties00.cobalt.client.cloud.CloudWhatsAppClientBuilder} (the
 * {@code WhatsAppClient.builder().cloudApi()} stage) or built directly with the generated
 * {@link com.github.auties00.cobalt.store.cloud.protobuf.ProtobufCloudWhatsAppStoreBuilder}; the interface is
 * {@code non-sealed} only so the backing representation is not fixed to the protobuf message
 * {@link com.github.auties00.cobalt.store.cloud.protobuf.ProtobufCloudWhatsAppStore} that implements it
 * today.
 *
 * @see com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient
 * @see com.github.auties00.cobalt.store.cloud.protobuf.ProtobufCloudWhatsAppStore
 */
public non-sealed interface CloudWhatsAppStore extends WhatsAppStore {
    /**
     * Returns the system-user access token that authenticates every Cloud API request.
     *
     * @return the access token
     */
    String accessToken();

    /**
     * Returns the phone number id whose edges send messages and manage the business profile.
     *
     * @return the phone number id
     */
    String phoneNumberId();

    /**
     * Returns the WhatsApp Business Account id addressed by the template and phone-number management edges.
     *
     * @return an {@link Optional} carrying the WhatsApp Business Account id, or empty when those edges
     *         are not used
     */
    Optional<String> whatsappBusinessAccountId();

    /**
     * Returns the business portfolio id addressed by the partner onboarding edges.
     *
     * @return an {@link Optional} carrying the business portfolio id, or empty when those edges are not used
     */
    Optional<String> businessId();

    /**
     * Returns the graph API version segment prefixing every request path.
     *
     * @return the API version segment, for example {@code v23.0}
     */
    String apiVersion();

    /**
     * Returns the app secret used to compute the {@code appsecret_proof} request parameter and to verify
     * the {@code X-Hub-Signature-256} of inbound webhook deliveries.
     *
     * @return an {@link Optional} carrying the app secret, or empty when proofs and signature checks are
     *         disabled
     */
    Optional<String> appSecret();

    /**
     * Returns the token echoed back during the webhook subscription verification handshake.
     *
     * @return an {@link Optional} carrying the verify token, or empty when the built-in receiver is disabled
     */
    Optional<String> webhookVerifyToken();

    /**
     * Returns the local address the built-in webhook receiver binds to.
     *
     * @return an {@link Optional} carrying the bind address, or empty to bind the wildcard address
     */
    Optional<String> webhookBindAddress();

    /**
     * Returns the TCP port the built-in webhook receiver listens on.
     *
     * @implSpec
     * Implementations return an empty result whenever no receiver is configured; an absent port and a
     * port of zero both mean the built-in receiver is disabled, consistent with {@link #hasWebhookReceiver()}.
     *
     * @return an {@link OptionalInt} carrying the port, or empty when the built-in receiver is disabled
     */
    OptionalInt webhookPort();

    /**
     * Returns the URL path the built-in webhook receiver serves.
     *
     * @return the webhook path
     */
    String webhookPath();

    /**
     * Returns the Meta app id addressed by the Resumable Upload API.
     *
     * <p>The Resumable Upload API creates an upload session under the {@code /{APP_ID}/uploads} edge, so
     * unlike the phone-number and WhatsApp Business Account edges it is keyed by the application rather
     * than a business asset, and it stays unset until resumable uploads are used.
     *
     * @return an {@link Optional} carrying the app id, or empty when resumable uploads are not used
     */
    Optional<String> appId();

    /**
     * Returns the live mapping of bare chat JID string to the id of the last inbound message seen in
     * that chat.
     *
     * @implSpec
     * The returned map is the live backing collection, not a copy: reads and writes through it are
     * reflected in the store and must be safe for concurrent access. Callers should prefer
     * {@link #recordLastInboundMessageId(String, String)} and {@link #lastInboundMessageId(String)} for
     * the common record-and-lookup operations.
     *
     * @return the last-inbound-message-id map
     */
    Map<String, String> lastInboundMessageIdByChat();

    /**
     * Records the id of the last inbound message seen in a chat, replacing any earlier id.
     *
     * @implSpec
     * The mapping is overwritten on each call so a later inbound message supersedes an earlier one, and
     * the recorded id is observable through {@link #lastInboundMessageId(String)} and
     * {@link #lastInboundMessageIdByChat()}.
     *
     * @param chatJid   the bare chat JID string
     * @param messageId the id of the most recent inbound message of the chat
     * @throws NullPointerException if {@code chatJid} or {@code messageId} is {@code null}
     */
    void recordLastInboundMessageId(String chatJid, String messageId);

    /**
     * Returns the id of the last inbound message seen in a chat.
     *
     * @param chatJid the bare chat JID string
     * @return an {@link Optional} carrying the last inbound message id, or empty when none has been
     *         recorded for the chat
     * @throws NullPointerException if {@code chatJid} is {@code null}
     */
    Optional<String> lastInboundMessageId(String chatJid);

    /**
     * Returns whether the built-in webhook receiver is configured.
     *
     * <p>The receiver requires both a port and a verify token; when either is missing the client runs
     * send-only and inbound deliveries must be handled by the embedder.
     *
     * @implSpec
     * The result must be consistent with {@link #webhookPort()} and {@link #webhookVerifyToken()}: it is
     * {@code true} exactly when both are present.
     *
     * @return {@code true} if both a webhook port and a verify token are configured
     */
    boolean hasWebhookReceiver();

    /**
     * Sets the system-user access token that authenticates every Cloud API request.
     *
     * @apiNote
     * Cloud access tokens rotate, so a resumed session refreshes its stored token through this setter to
     * absorb a token issued after the session was last persisted.
     *
     * @param accessToken the access token
     * @return this store, for chaining
     */
    CloudWhatsAppStore setAccessToken(String accessToken);

    /**
     * Sets the WhatsApp Business Account id addressed by the template and phone-number management edges.
     *
     * @param whatsappBusinessAccountId the WhatsApp Business Account id, or {@code null} to clear it
     * @return this store, for chaining
     */
    CloudWhatsAppStore setWhatsappBusinessAccountId(String whatsappBusinessAccountId);

    /**
     * Sets the business portfolio id addressed by the partner onboarding edges.
     *
     * @param businessId the business portfolio id, or {@code null} to clear it
     * @return this store, for chaining
     */
    CloudWhatsAppStore setBusinessId(String businessId);

    /**
     * Sets the graph API version segment prefixing every request path.
     *
     * @param apiVersion the API version segment, for example {@code v23.0}
     * @return this store, for chaining
     */
    CloudWhatsAppStore setApiVersion(String apiVersion);

    /**
     * Sets the app secret used to compute the {@code appsecret_proof} request parameter and to verify
     * inbound webhook signatures.
     *
     * @param appSecret the app secret, or {@code null} to disable proofs and signature checks
     * @return this store, for chaining
     */
    CloudWhatsAppStore setAppSecret(String appSecret);

    /**
     * Sets the Meta app id addressed by the Resumable Upload API.
     *
     * @param appId the Meta app id, or {@code null} to clear it
     * @return this store, for chaining
     */
    CloudWhatsAppStore setAppId(String appId);

    /**
     * Sets the token echoed back during the webhook subscription verification handshake.
     *
     * @param webhookVerifyToken the verify token, or {@code null} to disable the built-in receiver
     * @return this store, for chaining
     */
    CloudWhatsAppStore setWebhookVerifyToken(String webhookVerifyToken);

    /**
     * Sets the local address the built-in webhook receiver binds to.
     *
     * @param webhookBindAddress the bind address, or {@code null} to bind the wildcard address
     * @return this store, for chaining
     */
    CloudWhatsAppStore setWebhookBindAddress(String webhookBindAddress);

    /**
     * Sets the TCP port the built-in webhook receiver listens on.
     *
     * @implSpec
     * A {@code null} or zero port disables the built-in receiver, consistent with {@link #webhookPort()}
     * and {@link #hasWebhookReceiver()}.
     *
     * @param webhookPort the port, or {@code null} to disable the built-in receiver
     * @return this store, for chaining
     */
    CloudWhatsAppStore setWebhookPort(Integer webhookPort);

    /**
     * Sets the URL path the built-in webhook receiver serves.
     *
     * @param webhookPath the webhook path
     * @return this store, for chaining
     */
    CloudWhatsAppStore setWebhookPath(String webhookPath);

    /**
     * Persists the current state of this store to its backing storage.
     *
     * <p>This is the durability hook the embedder calls after configuring or mutating the store so a
     * later {@link CloudWhatsAppStoreFactory#load(String) load} observes the change. It is a no-op for
     * stores that keep no durable backing, such as the in-memory variant produced by
     * {@link CloudWhatsAppStoreFactory#temporary()}.
     *
     * @implSpec
     * Implementations that have no durable backing must return without error. Implementations that do
     * persist must make the write durable and must not propagate {@link IOException}: a failed write is
     * logged and swallowed, mirroring the fire-and-forget save of the Linked store, so a caller in a
     * mutation path is never forced to handle persistence failure.
     */
    void save();

    /**
     * Permanently removes this store from its backing storage.
     *
     * @implSpec
     * Implementations that have no durable backing must return without error. Implementations that do
     * persist must remove every artifact written by {@link #save()} and may propagate {@link IOException}
     * when the on-disk state cannot be removed.
     *
     * @throws IOException if the persisted state cannot be removed
     */
    void delete() throws IOException;
}
