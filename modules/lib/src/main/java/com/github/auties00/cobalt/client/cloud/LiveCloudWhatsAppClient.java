package com.github.auties00.cobalt.client.cloud;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.WhatsAppClientDisconnectReason;
import com.github.auties00.cobalt.cloud.CloudApiClient;
import com.github.auties00.cobalt.cloud.CloudMessageEncoder;
import com.github.auties00.cobalt.cloud.CloudWebhookDecoder;
import com.github.auties00.cobalt.cloud.CloudWebhookServer;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudUnsupportedVersionException;
import com.github.auties00.cobalt.exception.cloud.WhatsAppCloudApiException;
import com.github.auties00.cobalt.listener.*;
import com.github.auties00.cobalt.listener.cloud.*;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfile;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfileBuilder;
import com.github.auties00.cobalt.wire.cloud.*;
import com.github.auties00.cobalt.wire.cloud.analytics.*;
import com.github.auties00.cobalt.wire.cloud.commerce.*;
import com.github.auties00.cobalt.wire.cloud.flow.*;
import com.github.auties00.cobalt.wire.cloud.phone.*;
import com.github.auties00.cobalt.wire.cloud.signup.CloudAppCredentials;
import com.github.auties00.cobalt.wire.cloud.signup.CloudOAuthToken;
import com.github.auties00.cobalt.wire.cloud.signup.CloudSignupCodeExchange;
import com.github.auties00.cobalt.wire.cloud.signup.CloudTokenInspection;
import com.github.auties00.cobalt.wire.cloud.template.*;
import com.github.auties00.cobalt.wire.cloud.template.library.CloudTemplateLibraryAdoption;
import com.github.auties00.cobalt.wire.cloud.waba.*;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Production implementation of {@link CloudWhatsAppClient}.
 *
 * <p>This class wires the Cloud transport ({@link CloudApiClient}), the message encoder/decoder
 * ({@link CloudMessageEncoder}, {@link CloudWebhookDecoder}), the built-in webhook receiver
 * ({@link CloudWebhookServer}), a listener registry, and the configurable error handler. Outbound
 * operations translate to graph requests; inbound webhook deliveries are decoded and dispatched to the
 * registered listeners.
 */
public final class LiveCloudWhatsAppClient implements CloudWhatsAppClient {
    /**
     * The logger for {@link LiveCloudWhatsAppClient}.
     */
    private static final System.Logger LOGGER = Log.get(LiveCloudWhatsAppClient.class);

    /**
     * The minimum Cloud API version required to query a consumer's call permissions.
     */
    private static final CloudApiVersion CALL_PERMISSIONS_MIN = CloudApiVersion.V23_0;

    /**
     * The minimum Cloud API version required to delete message templates in bulk by id.
     */
    private static final CloudApiVersion TEMPLATE_BATCH_DELETE_MIN = CloudApiVersion.V25_0;

    /**
     * The minimum Cloud API version required to read and write phone-number local-storage settings.
     */
    private static final CloudApiVersion PHONE_LOCAL_STORAGE_MIN = CloudApiVersion.V21_0;

    /**
     * The minimum Cloud API version required to upload a template-header media handle whose OTP
     * {@code supported_apps} array shape it emits.
     */
    private static final CloudApiVersion TEMPLATE_HEADER_MEDIA_MIN = CloudApiVersion.V21_0;

    /**
     * The phone-number stanza fields requested when projecting a {@link CloudPhoneNumber}.
     */
    private static final String PHONE_NUMBER_FIELDS = "id,display_phone_number,verified_name,quality_rating,"
            + "code_verification_status,status,name_status,new_name_status,messaging_limit_tier,throughput,"
            + "platform_type,certificate,new_certificate,is_official_business_account,account_mode";

    /**
     * The credential and webhook configuration.
     */
    private final CloudWhatsAppStore store;

    /**
     * The HTTP/JSON transport.
     */
    private final CloudApiClient api;

    /**
     * The WhatsApp Business Account stanza fields requested when projecting a {@link CloudWaba}.
     */
    private static final String WABA_FIELDS = "id,name,currency,timezone_id,message_template_namespace,"
            + "country,business_verification_status,account_review_status,status,ownership_type";

    /**
     * The Graph API version the client is configured to target, used to gate version-restricted
     * operations before a request is sent.
     */
    private final CloudApiVersion apiVersion;

    /**
     * The registered listeners.
     */
    private final CopyOnWriteArraySet<WhatsAppListener> listeners;

    /**
     * Whether the client is currently connected.
     */
    private final AtomicBoolean connected;

    /**
     * The webhook receiver, or {@code null} when the receiver is disabled.
     */
    private volatile CloudWebhookServer webhookServer;

    /**
     * The latch released on disconnect, used by {@link #waitForDisconnection()}.
     */
    private volatile CountDownLatch disconnectLatch;

    /**
     * The JVM shutdown hook installed on the first {@link #connect()}, or {@code null} when no
     * connection has been opened or the hook has been deregistered by a terminal disconnect.
     *
     * <p>Mirrors the Linked client's hook so that a process exit gracefully stops the
     * {@link CloudWebhookServer} (and fires the disconnect listeners) rather than abandoning the
     * open HTTP listener.
     */
    private volatile Thread shutdownHook;

    /**
     * Constructs a new live Cloud client.
     *
     * @param store      the credential and webhook configuration
     * @param httpClient   the HTTP client backing the transport
     * @throws NullPointerException if any argument is {@code null}
     */
    LiveCloudWhatsAppClient(CloudWhatsAppStore store, HttpClient httpClient) {
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.api = new CloudApiClient(httpClient, store.accessToken(), store.apiVersion(),
                store.appSecret().orElse(null));
        this.apiVersion = CloudApiVersion.of(store.apiVersion());
        this.listeners = new CopyOnWriteArraySet<>();
        this.connected = new AtomicBoolean();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient connect() {
        if (!connected.compareAndSet(false, true)) {
            throw new IllegalStateException("client already connected");
        }
        api.get(store.phoneNumberId(), Map.of("fields", "id"));
        disconnectLatch = new CountDownLatch(1);
        if (store.hasWebhookReceiver()) {
            var server = new CloudWebhookServer(
                    store.webhookBindAddress().orElse(null),
                    store.webhookPort().orElseThrow(),
                    store.webhookPath(),
                    store.webhookVerifyToken().orElseThrow(),
                    store.appSecret().orElse(null),
                    this::dispatchEnvelope,
                    this::fireError);
            server.start();
            this.webhookServer = server;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cloud webhook receiver started on port {0}",
                    store.webhookPort().orElse(-1));
        }
        if (shutdownHook == null) {
            this.shutdownHook = Thread.ofPlatform()
                    .name("CobaltCloudShutdownHandler")
                    .unstarted(() -> disconnect(WhatsAppClientDisconnectReason.DISCONNECTED, false));
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
        forEach(LoggedInListener.class, listener -> listener.onLoggedIn(this));
        if (Log.INFO) LOGGER.log(Level.INFO, "cloud client connected");
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() {
        disconnect(WhatsAppClientDisconnectReason.DISCONNECTED, true);
    }

    /**
     * Tears the client down, notifying the disconnect listeners with the given reason.
     *
     * @implNote
     * This implementation stops the {@link CloudWebhookServer}, fires the disconnect listeners, and
     * releases the {@link #waitForDisconnection()} latch. On a non-reconnect disconnect it also
     * deregisters the JVM shutdown hook, unless {@code canRemoveShutdownHook} is {@code false}
     * because the call originates from the hook itself (where
     * {@link Runtime#removeShutdownHook(Thread)} would throw while shutdown is in progress).
     *
     * @param reason                the reason surfaced to the disconnect listeners
     * @param canRemoveShutdownHook whether the shutdown hook may be deregistered, set to
     *                              {@code false} when invoked from the hook during JVM shutdown
     */
    private void disconnect(WhatsAppClientDisconnectReason reason, boolean canRemoveShutdownHook) {
        if (!connected.compareAndSet(true, false)) {
            return;
        }
        var server = webhookServer;
        if (server != null) {
            server.stop();
            webhookServer = null;
        }
        if (reason != WhatsAppClientDisconnectReason.RECONNECTING && shutdownHook != null && canRemoveShutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
        forEach(DisconnectedListener.class, listener -> listener.onDisconnected(this, reason));
        var latch = disconnectLatch;
        if (latch != null) {
            latch.countDown();
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "cloud client disconnected, reason {0}", reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient reconnect() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "cloud client reconnecting");
        disconnect(WhatsAppClientDisconnectReason.RECONNECTING, true);
        connect();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient waitForDisconnection() {
        var latch = disconnectLatch;
        if (latch != null) {
            try {
                latch.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addListener(CloudListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
        return this;
    }

    /**
     * Registers a transport-agnostic listener.
     *
     * @param listener the listener to register
     * @return {@code this}, for fluent chaining
     */
    private CloudWhatsAppClient addShared(WhatsAppListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient removeListener(WhatsAppListener listener) {
        listeners.remove(listener);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendMessage(JidProvider recipient, LinkedMessageContainer message) {
        var body = CloudMessageEncoder.encode(recipient, message);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent message to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(LinkedMessageInfo messageInfo) {
        var recipient = messageInfo.key().parentJid()
                .orElseThrow(() -> new IllegalArgumentException("messageInfo key must carry a parentJid"));
        sendMessage(recipient, messageInfo.message());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addReaction(MessageKey messageKey, String emoji) {
        sendReaction(messageKey, emoji);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeReaction(MessageKey messageKey) {
        sendReaction(messageKey, "");
    }

    /**
     * Posts a reaction message for the given target with the given emoji.
     *
     * @param messageKey the key of the message to react to
     * @param emoji      the reaction emoji, or the empty string to clear it
     */
    private void sendReaction(MessageKey messageKey, String emoji) {
        var recipient = messageKey.parentJid()
                .orElseThrow(() -> new IllegalArgumentException("messageKey must carry a parentJid"));
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.user());
        body.put("type", "reaction");
        var reaction = new JSONObject();
        reaction.put("message_id", messageKey.id().orElseThrow(
                () -> new IllegalArgumentException("messageKey must carry an id")));
        reaction.put("emoji", emoji);
        body.put("reaction", reaction);
        api.post(store.phoneNumberId() + "/messages", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent reaction to {0}, message id {1}", recipient,
                messageKey.id().orElse(null));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markChatAsRead(JidProvider chat) {
        // The chat-to-last-inbound-message mapping is held in the store, so it is persisted when the
        // embedder serialises and restored when the embedder restores the store.
        var lastInbound = store.lastInboundMessageId(chat.toJid().toString()).orElse(null);
        if (lastInbound == null) {
            return;
        }
        postRead(lastInbound, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markMessageAsRead(MessageKey messageKey) {
        var id = messageKey.id().orElseThrow(
                () -> new IllegalArgumentException("messageKey must carry an id"));
        postRead(id, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendTypingIndicator(MessageKey inboundMessage) {
        var id = inboundMessage.id().orElseThrow(
                () -> new IllegalArgumentException("inboundMessage must carry an id"));
        postRead(id, "text");
    }

    /**
     * Posts a {@code status: read} update for a message, optionally attaching a typing indicator.
     *
     * <p>The Cloud API couples the two concerns: a typing-indicator request is a read update with a
     * {@code typing_indicator} attachment, so requesting the indicator also marks the message read.
     *
     * @param messageId     the inbound message id
     * @param indicatorType the typing indicator type, or {@code null} to send a plain read update
     */
    private void postRead(String messageId, String indicatorType) {
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("status", "read");
        body.put("message_id", messageId);
        if (indicatorType != null) {
            var indicator = new JSONObject();
            indicator.put("type", indicatorType);
            body.put("typing_indicator", indicator);
        }
        api.post(store.phoneNumberId() + "/messages", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "marked message {0} read, indicator {1}", messageId, indicatorType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadMedia(byte[] data, String mimeType, String fileName) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploading media, size {0}, mime type {1}", data.length, mimeType);
        var response = api.uploadMedia(store.phoneNumberId(), data, mimeType, fileName);
        var mediaId = response.getString("id");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploaded media, id {0}", mediaId);
        return mediaId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadMedia(Path file, String mimeType) {
        try {
            var data = Files.readAllBytes(file);
            return uploadMedia(data, mimeType, file.getFileName().toString());
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to read media file", exception);
            throw new IllegalArgumentException("failed to read media file: " + file, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadMedia(Path file) {
        String mimeType;
        try {
            mimeType = Files.probeContentType(file);
        } catch (IOException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to probe media MIME type", exception);
            throw new IllegalArgumentException("failed to probe the MIME type of " + file, exception);
        }
        if (mimeType == null) {
            throw new IllegalArgumentException("cannot determine the MIME type of " + file);
        }
        return uploadMedia(file, mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadTemplateHeaderMedia(byte[] data, String mimeType, String fileName) {
        requireVersion(TEMPLATE_HEADER_MEDIA_MIN, "uploadTemplateHeaderMedia");
        var sessionId = createResumableUploadSession(data.length, mimeType, fileName);
        return uploadToResumableSession(sessionId, 0, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createResumableUploadSession(long fileLength, String fileType, String fileName) {
        var sessionId = api.createUploadSession(requireApp(), fileLength, fileType, fileName);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created resumable upload session, file length {0}, type {1}",
                fileLength, fileType);
        return sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadToResumableSession(String uploadSessionId, long fileOffset, byte[] data) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploading to resumable session, offset {0}, size {1}",
                fileOffset, data.length);
        return api.uploadToSession(uploadSessionId, fileOffset, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long queryResumableUploadOffset(String uploadSessionId) {
        return api.queryUploadStatus(uploadSessionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] downloadMedia(String mediaId) {
        var data = api.download(queryMediaUrl(mediaId));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "downloaded media {0}, size {1}", mediaId, data.length);
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI queryMediaUrl(String mediaId) {
        var response = api.get(mediaId, Map.of());
        return URI.create(response.getString("url"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMedia(String mediaId) {
        api.delete(mediaId, Map.of());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted media {0}", mediaId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<BusinessProfile> queryBusinessProfile() {
        var response = api.get(store.phoneNumberId() + "/whatsapp_business_profile",
                Map.of("fields", "about,address,description,email,websites,vertical,profile_picture_url"));
        var data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parseBusinessProfile(data.getJSONObject(0)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editBusinessProfile(BusinessProfile profile) {
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        profile.description().ifPresent(value -> body.put("description", value));
        profile.address().ifPresent(value -> body.put("address", value));
        profile.email().ifPresent(value -> body.put("email", value));
        if (!profile.websites().isEmpty()) {
            var websites = new JSONArray();
            for (var website : profile.websites()) {
                websites.add(website.toString());
            }
            body.put("websites", websites);
        }
        api.post(store.phoneNumberId() + "/whatsapp_business_profile", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited business profile");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void blockContact(JidProvider contact) {
        api.post(store.phoneNumberId() + "/block_users", blockUsersBody(contact));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "blocked contact {0}", contact.toJid());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unblockContact(JidProvider contact) {
        api.delete(store.phoneNumberId() + "/block_users", blockUsersBody(contact));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unblocked contact {0}", contact.toJid());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Jid> queryBlockedContacts() {
        var response = api.get(store.phoneNumberId() + "/block_users", Map.of());
        var data = response.getJSONArray("data");
        var result = new ArrayList<Jid>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                var user = data.getJSONObject(index).getString("wa_id");
                if (user != null) {
                    result.add(Jid.of(user, JidServer.user()));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudMessageTemplate createMessageTemplate(CloudMessageTemplate template) {
        var body = new JSONObject();
        body.put("name", template.name());
        body.put("language", template.language());
        body.put("category", template.category().token());
        if (!template.components().isEmpty()) {
            body.put("components", CloudMessageEncoder.encodeTemplateComponents(template.components()));
        }
        var response = api.post(requireWaba() + "/message_templates", body);
        var created = new CloudMessageTemplate(response.getString("id"), template.name(), template.language(),
                template.category(), CloudTemplateStatus.of(response.getString("status")), template.components());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created message template {0}, status {1}",
                created.id().orElse(null), created.status().orElse(null));
        return created;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudMessageTemplate> queryMessageTemplates() {
        return queryMessageTemplates(Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudMessageTemplate> queryMessageTemplates(int limit) {
        return queryMessageTemplates(Map.of("limit", String.valueOf(limit)));
    }

    /**
     * Lists the message templates of the WhatsApp Business Account with the given query parameters.
     *
     * @param parameters the query parameters
     * @return the message templates
     */
    private List<CloudMessageTemplate> queryMessageTemplates(Map<String, String> parameters) {
        var response = api.get(requireWaba() + "/message_templates", parameters);
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudMessageTemplate>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseTemplate(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudMessageTemplate> queryMessageTemplateByName(String name) {
        var response = api.get(requireWaba() + "/message_templates", Map.of("name", name));
        var data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(parseTemplate(data.getJSONObject(0)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editMessageTemplate(CloudMessageTemplate template) {
        var templateId = template.id().orElseThrow(
                () -> new IllegalArgumentException("template must carry an id"));
        var body = new JSONObject();
        body.put("category", template.category().token());
        if (!template.components().isEmpty()) {
            body.put("components", CloudMessageEncoder.encodeTemplateComponents(template.components()));
        }
        api.post(templateId, body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited message template {0}", templateId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageTemplate(String name) {
        api.delete(requireWaba() + "/message_templates", Map.of("name", name));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted message template {0}", name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudMessageTemplate> queryMessageTemplateById(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        var response = api.get(templateId,
                Map.of("fields", "id,name,language,category,status,components"));
        if (response == null || response.getString("id") == null) {
            return Optional.empty();
        }
        return Optional.of(parseTemplate(response));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudMessageTemplate> queryAllMessageTemplates() {
        var waba = requireWaba();
        var collected = new ArrayList<CloudMessageTemplate>();
        String after = null;
        var restarted = false;
        while (true) {
            var parameters = new HashMap<String, String>();
            parameters.put("limit", "100");
            if (after != null) {
                parameters.put("after", after);
            }
            JSONObject response;
            try {
                response = api.get(waba + "/message_templates", parameters);
            } catch (WhatsAppCloudApiException exception) {
                // A stale cursor cannot be recovered in place; Graph cursors are not durable. Restart
                // once from the first page rather than propagating the rejection.
                if (after != null && !restarted && exception.code().orElse(0) == 100) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "stale template listing cursor, restarting from first page");
                    collected.clear();
                    after = null;
                    restarted = true;
                    continue;
                }
                throw exception;
            }
            var data = response.getJSONArray("data");
            if (data != null) {
                for (var index = 0; index < data.size(); index++) {
                    collected.add(parseTemplate(data.getJSONObject(index)));
                }
            }
            var paging = response.getJSONObject("paging");
            var cursors = paging == null ? null : paging.getJSONObject("cursors");
            var nextAfter = cursors == null ? null : cursors.getString("after");
            if (paging == null || paging.getString("next") == null || nextAfter == null || nextAfter.equals(after)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "listed all message templates, count {0}", collected.size());
                return collected;
            }
            after = nextAfter;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageTemplateLanguage(String name, String languageTemplateId) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(languageTemplateId, "languageTemplateId must not be null");
        api.delete(requireWaba() + "/message_templates", Map.of("name", name, "hsm_id", languageTemplateId));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted message template {0} language {1}", name, languageTemplateId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageTemplates(List<String> templateIds) {
        requireVersion(TEMPLATE_BATCH_DELETE_MIN, "deleteMessageTemplates");
        Objects.requireNonNull(templateIds, "templateIds must not be null");
        if (templateIds.isEmpty()) {
            throw new IllegalArgumentException("templateIds must not be empty");
        }
        var ids = new JSONArray();
        ids.addAll(templateIds);
        api.delete(requireWaba() + "/message_templates", Map.of("hsm_ids", ids.toString()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted {0} message templates", templateIds.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudMessageTemplate> migrateMessageTemplates(String sourceWabaId) {
        Objects.requireNonNull(sourceWabaId, "sourceWabaId must not be null");
        var body = new JSONObject();
        body.put("source_waba_id", sourceWabaId);
        var response = api.post(requireWaba() + "/migrate_message_templates", body);
        // TODO: unverified - the migrated/failed split of the response is not documented in a fetched
        //       Meta source nor typed by the official SDK; migrated templates are read from "data"
        //       (falling back to "migrated_templates") and per-template failures are not surfaced.
        var data = response.getJSONArray("data");
        if (data == null) {
            data = response.getJSONArray("migrated_templates");
        }
        var result = new ArrayList<CloudMessageTemplate>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseTemplate(data.getJSONObject(index)));
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "migrated {0} message templates from waba {1}",
                result.size(), sourceWabaId);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudTemplateComparison compareMessageTemplates(CloudMessageTemplateComparisonRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        var parameters = Map.of(
                "template_ids", String.join(",", request.comparisonTemplateIds()),
                "start", String.valueOf(request.start().getEpochSecond()),
                "end", String.valueOf(request.end().getEpochSecond()));
        var response = api.get(request.templateId() + "/compare", parameters);
        return parseTemplateComparison(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudUpsertedTemplate> upsertMessageTemplates(CloudAuthenticationTemplateUpsert template) {
        Objects.requireNonNull(template, "template must not be null");
        var body = new JSONObject();
        body.put("name", template.name());
        body.put("category", "AUTHENTICATION");
        var languages = new JSONArray();
        languages.addAll(template.languages());
        body.put("languages", languages);
        var components = new JSONArray();
        var bodyComponent = new JSONObject();
        bodyComponent.put("type", "BODY");
        template.addSecurityRecommendation()
                .ifPresent(value -> bodyComponent.put("add_security_recommendation", value));
        components.add(bodyComponent);
        var footerComponent = new JSONObject();
        footerComponent.put("type", "FOOTER");
        template.codeExpirationMinutes()
                .ifPresent(value -> footerComponent.put("code_expiration_minutes", value));
        components.add(footerComponent);
        components.add(upsertButtonsComponent(template.otpButton()));
        body.put("components", components);
        template.messageSendTtlSeconds()
                .ifPresent(value -> body.put("message_send_ttl_seconds", value));
        var response = api.post(requireWaba() + "/upsert_message_templates", body);
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudUpsertedTemplate>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseUpsertedTemplate(data.getJSONObject(index)));
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "upserted authentication template {0}, {1} languages",
                template.name(), result.size());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudMessageTemplate createTemplateFromLibrary(CloudTemplateLibraryAdoption adoption) {
        Objects.requireNonNull(adoption, "adoption must not be null");
        var category = adoption.category();
        var body = new JSONObject();
        body.put("name", adoption.name());
        body.put("language", adoption.language());
        body.put("category", category.token());
        body.put("library_template_name", adoption.libraryTemplateName());
        if (!adoption.libraryButtons().isEmpty()) {
            var inputs = new JSONArray();
            for (var button : adoption.libraryButtons()) {
                var input = new JSONObject();
                input.put("type", button.type().name());
                button.url().ifPresent(url -> {
                    var urlJson = new JSONObject();
                    urlJson.put("base_url", url);
                    input.put("url", urlJson);
                });
                button.phoneNumber().ifPresent(phone -> input.put("phone_number", phone));
                button.otpType().ifPresent(otp -> input.put("otp_type", otp));
                inputs.add(input);
            }
            body.put("library_template_button_inputs", inputs);
        }
        var response = api.post(requireWaba() + "/message_templates", body);
        var responseCategory = response.getString("category");
        var created = new CloudMessageTemplate(response.getString("id"), adoption.name(), adoption.language(),
                responseCategory != null ? CloudTemplateCategory.of(responseCategory) : category,
                CloudTemplateStatus.of(response.getString("status")), null);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created template {0} from library template {1}",
                created.id().orElse(null), adoption.libraryTemplateName());
        return created;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudRegistrationResult registerPhoneNumber(String pin) {
        return register(pin, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudRegistrationResult registerPhoneNumber(String pin, CloudRegistrationBackup backup) {
        Objects.requireNonNull(backup, "backup must not be null");
        return register(pin, backup);
    }

    /**
     * Registers the phone number, optionally attaching a restore backup.
     *
     * @param pin    the six-digit two-step verification PIN to set during registration
     * @param backup the backup material to restore, or {@code null} for the plain registration form
     * @return the registration result
     */
    private CloudRegistrationResult register(String pin, CloudRegistrationBackup backup) {
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("pin", pin);
        if (backup != null) {
            var backupJson = new JSONObject();
            backupJson.put("data", backup.data());
            backup.password().ifPresent(password -> backupJson.put("password", password));
            body.put("backup", backupJson);
        }
        var response = api.post(store.phoneNumberId() + "/register", body);
        var result = new CloudRegistrationResult(!response.containsKey("success") || response.getBooleanValue("success"), null);
        if (Log.INFO) LOGGER.log(Level.INFO, "phone number registration completed, success {0}", result.success());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deregisterPhoneNumber() {
        api.post(store.phoneNumberId() + "/deregister", new JSONObject());
        if (Log.INFO) LOGGER.log(Level.INFO, "phone number deregistered");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestVerificationCode(CloudVerificationMethod method, Locale language) {
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(language, "language must not be null");
        api.postForm(store.phoneNumberId() + "/request_code",
                Map.of("code_method", method.name(), "language", language.toString()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "requested verification code, method {0}", method);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void verifyCode(String code) {
        api.postForm(store.phoneNumberId() + "/verify_code", Map.of("code", code));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "verified code {0}", new LogRedactable.Code(code));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editTwoStepPin(String pin) {
        var body = new JSONObject();
        body.put("pin", pin);
        api.post(store.phoneNumberId(), body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited two-step verification pin");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudBusinessEncryption> queryBusinessEncryption() {
        var response = api.get(store.phoneNumberId() + "/whatsapp_business_encryption", Map.of());
        var data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        var node = data.getJSONObject(0);
        var publicKey = node.getString("business_public_key");
        if (publicKey == null) {
            return Optional.empty();
        }
        return Optional.of(new CloudBusinessEncryption(publicKey,
                enumOrNull(node.getString("business_public_key_signature_status"),
                        CloudBusinessEncryptionSignatureStatus::of)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editBusinessEncryption(CloudBusinessEncryption encryption) {
        Objects.requireNonNull(encryption, "encryption must not be null");
        api.postForm(store.phoneNumberId() + "/whatsapp_business_encryption",
                Map.of("business_public_key", encryption.businessPublicKey()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited business encryption public key");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudPhoneNumber queryPhoneNumber() {
        var response = api.get(store.phoneNumberId(), Map.of("fields", PHONE_NUMBER_FIELDS));
        return parsePhoneNumber(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudPhoneNumber> queryPhoneNumbers() {
        return queryPhoneNumbers(Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudPhoneNumber> queryPhoneNumbers(int limit) {
        return queryPhoneNumbers(Map.of("limit", String.valueOf(limit)));
    }

    /**
     * Lists the phone numbers of the WhatsApp Business Account with the given query parameters.
     *
     * @param parameters the query parameters
     * @return the phone numbers
     */
    private List<CloudPhoneNumber> queryPhoneNumbers(Map<String, String> parameters) {
        var merged = new HashMap<>(parameters);
        merged.putIfAbsent("fields", PHONE_NUMBER_FIELDS);
        var response = api.get(requireWaba() + "/phone_numbers", merged);
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudPhoneNumber>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parsePhoneNumber(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String addPhoneNumber(CloudPhoneNumberAdd add) {
        Objects.requireNonNull(add, "add must not be null");
        var body = new JSONObject();
        body.put("cc", add.countryCode());
        body.put("phone_number", add.phoneNumber());
        body.put("verified_name", add.verifiedName());
        var response = api.post(requireWaba() + "/phone_numbers", body);
        var phoneNumberId = response.getString("id");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "added phone number {0}, id {1}",
                new LogRedactable.Phone(add.phoneNumber()), phoneNumberId);
        return phoneNumberId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudLocalStorageSettings queryLocalStorageSettings() {
        requireVersion(PHONE_LOCAL_STORAGE_MIN, "queryLocalStorageSettings");
        var response = api.get(store.phoneNumberId() + "/settings",
                Map.of("fields", "storage_configuration"));
        var storage = response.getJSONObject("storage_configuration");
        if (storage == null) {
            return new CloudLocalStorageSettings(null, null, null);
        }
        var retentionMinutes = storage.containsKey("retention_minutes")
                ? storage.getInteger("retention_minutes")
                : null;
        return new CloudLocalStorageSettings(
                enumOrNull(storage.getString("status"), CloudLocalStorageStatus::of),
                storage.getString("data_localization_region"),
                retentionMinutes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLocalStorageSettings(CloudLocalStorageSettings settings) {
        requireVersion(PHONE_LOCAL_STORAGE_MIN, "updateLocalStorageSettings");
        Objects.requireNonNull(settings, "settings must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        var storage = new JSONObject();
        settings.status().ifPresent(status -> storage.put("status", status.token()));
        settings.dataLocalizationRegion().ifPresent(region -> storage.put("data_localization_region", region));
        settings.retentionMinutes().ifPresent(minutes -> storage.put("retention_minutes", minutes));
        body.put("storage_configuration", storage);
        api.post(store.phoneNumberId() + "/settings", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "updated local storage settings");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableCalling() {
        applyCallingStatus("enabled");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void disableCalling() {
        applyCallingStatus("disabled");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> startCall(JidProvider recipient, CloudCallSession session, String callbackData) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(session, "session must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("to", recipient.toJid().user());
        body.put("action", "connect");
        body.put("session", sessionJson(session));
        if (callbackData != null) {
            body.put("biz_opaque_callback_data", callbackData);
        }
        var response = api.post(store.phoneNumberId() + "/calls", body);
        var calls = response.getJSONArray("calls");
        if (calls == null || calls.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "call connect request to {0} returned no call id", recipient.toJid());
            return Optional.empty();
        }
        var callId = calls.getJSONObject(0).getString("id");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "started call {0} to {1}", callId, recipient.toJid());
        return Optional.ofNullable(callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preacceptCall(String callId, CloudCallSession session) {
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(session, "session must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("call_id", callId);
        body.put("action", "pre_accept");
        body.put("session", sessionJson(session));
        api.post(store.phoneNumberId() + "/calls", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state -> pre_accept", callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void acceptCall(String callId, CloudCallSession session, String callbackData) {
        Objects.requireNonNull(callId, "callId must not be null");
        Objects.requireNonNull(session, "session must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("call_id", callId);
        body.put("action", "accept");
        body.put("session", sessionJson(session));
        if (callbackData != null) {
            body.put("biz_opaque_callback_data", callbackData);
        }
        api.post(store.phoneNumberId() + "/calls", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state -> accept", callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rejectCall(String callId) {
        Objects.requireNonNull(callId, "callId must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("call_id", callId);
        body.put("action", "reject");
        api.post(store.phoneNumberId() + "/calls", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state -> reject", callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void terminateCall(String callId) {
        Objects.requireNonNull(callId, "callId must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("call_id", callId);
        body.put("action", "terminate");
        api.post(store.phoneNumberId() + "/calls", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} state -> terminate", callId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendCallPermissionRequest(JidProvider recipient, String bodyText) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(bodyText, "bodyText must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        var action = new JSONObject();
        action.put("name", "call_permission_request");
        var text = new JSONObject();
        text.put("text", bodyText);
        var interactive = new JSONObject();
        interactive.put("type", "call_permission_request");
        interactive.put("action", action);
        interactive.put("body", text);
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent call permission request to {0}, id {1}",
                recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudCallPermission queryCallPermission(JidProvider user) {
        requireVersion(CALL_PERMISSIONS_MIN, "queryCallPermission");
        Objects.requireNonNull(user, "user must not be null");
        var response = api.get(store.phoneNumberId() + "/call_permissions",
                Map.of("user_wa_id", user.toJid().user()));
        var permission = response.getJSONObject("permission");
        String status = null;
        Instant expiration = null;
        if (permission != null) {
            status = permission.getString("status");
            var expirationValue = permission.getLong("expiration_time");
            if (expirationValue != null) {
                expiration = Instant.ofEpochSecond(expirationValue);
            }
        }
        var actions = new ArrayList<CloudCallPermission.Action>();
        var array = response.getJSONArray("actions");
        if (array != null) {
            for (var index = 0; index < array.size(); index++) {
                actions.add(parseCallPermissionAction(array.getJSONObject(index)));
            }
        }
        var result = new CloudCallPermission(status == null ? "no_permission" : status, expiration, actions);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "queried call permission for {0}, status {1}",
                user.toJid(), result.status());
        return result;
    }

    /**
     * Parses a single {@code actions} entry into a {@link CloudCallPermission.Action}.
     *
     * @param node the action entry
     * @return the parsed action
     */
    private static CloudCallPermission.Action parseCallPermissionAction(JSONObject node) {
        var limits = new ArrayList<CloudCallPermission.Limit>();
        var array = node.getJSONArray("limits");
        if (array != null) {
            for (var index = 0; index < array.size(); index++) {
                var limit = array.getJSONObject(index);
                Instant limitExpiration = null;
                var expirationValue = limit.getLong("limit_expiration_time");
                if (expirationValue != null) {
                    limitExpiration = Instant.ofEpochSecond(expirationValue);
                }
                limits.add(new CloudCallPermission.Limit(
                        limit.getString("time_period"),
                        limit.getIntValue("max_allowed"),
                        limit.getIntValue("current_usage"),
                        limitExpiration));
            }
        }
        return new CloudCallPermission.Action(
                node.getString("action_name"),
                node.getBooleanValue("can_perform_action"),
                limits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudCallSettings queryCallSettings() {
        var parameters = Map.of("fields", "calling", "include_sip_credentials", "true");
        var response = api.get(store.phoneNumberId() + "/settings", parameters);
        var calling = response.getJSONObject("calling");
        if (calling == null) {
            return new CloudCallSettings(null, null, null, null, null, null, null);
        }
        return parseCallSettings(calling);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCallSettings(CloudCallSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        var calling = new JSONObject();
        settings.status().ifPresent(value -> calling.put("status", value));
        settings.callIconVisibility().ifPresent(value -> calling.put("call_icon_visibility", value));
        settings.callIcons().ifPresent(icons -> {
            var iconsJson = new JSONObject();
            var countries = new JSONArray();
            countries.addAll(icons.restrictToUserCountries());
            iconsJson.put("restrict_to_user_countries", countries);
            calling.put("call_icons", iconsJson);
        });
        settings.callbackPermissionStatus().ifPresent(value -> calling.put("callback_permission_status", value));
        settings.srtpKeyExchangeProtocol().ifPresent(value -> calling.put("srtp_key_exchange_protocol", value));
        settings.callHours().ifPresent(hours -> calling.put("call_hours", callHoursBody(hours)));
        settings.sip().ifPresent(sip -> calling.put("sip", sipBody(sip)));
        body.put("calling", calling);
        api.post(store.phoneNumberId() + "/settings", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "updated call settings");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribeApp() {
        api.post(requireWaba() + "/subscribed_apps", new JSONObject());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "subscribed app to waba webhooks");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudSubscribedApp> querySubscribedApps() {
        var response = api.get(requireWaba() + "/subscribed_apps", Map.of());
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudSubscribedApp>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                var app = data.getJSONObject(index).getJSONObject("whatsapp_business_api_data");
                if (app != null && app.getString("id") != null) {
                    result.add(new CloudSubscribedApp(
                            app.getString("id"),
                            app.getString("name"),
                            app.getString("override_callback_uri")));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribeApp() {
        api.delete(requireWaba() + "/subscribed_apps", Map.of());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unsubscribed app from waba webhooks");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFlow createFlow(CloudFlow flow) {
        var body = new JSONObject();
        body.put("name", flow.name());
        if (!flow.categories().isEmpty()) {
            var categories = new JSONArray();
            categories.addAll(flow.categories());
            body.put("categories", categories);
        }
        var response = api.post(requireWaba() + "/flows", body);
        var created = new CloudFlow(response.getString("id"), flow.name(), CloudFlowStatus.DRAFT, flow.categories());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created flow {0}", created.id().orElse(null));
        return created;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudFlow> queryFlows() {
        return queryFlows(Map.of());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudFlow> queryFlows(int limit) {
        return queryFlows(Map.of("limit", String.valueOf(limit)));
    }

    /**
     * Lists the Flows of the WhatsApp Business Account with the given query parameters.
     *
     * @param parameters the query parameters
     * @return the flows
     */
    private List<CloudFlow> queryFlows(Map<String, String> parameters) {
        var response = api.get(requireWaba() + "/flows", parameters);
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudFlow>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseFlow(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudFlow> queryAllFlows() {
        var waba = requireWaba();
        var collected = new ArrayList<CloudFlow>();
        String after = null;
        var restarted = false;
        while (true) {
            var parameters = new HashMap<String, String>();
            parameters.put("limit", "100");
            if (after != null) {
                parameters.put("after", after);
            }
            JSONObject response;
            try {
                response = api.get(waba + "/flows", parameters);
            } catch (WhatsAppCloudApiException exception) {
                // A stale cursor cannot be recovered in place; Graph cursors are not durable. Restart
                // once from the first page rather than propagating the rejection.
                if (after != null && !restarted && exception.code().orElse(0) == 100) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "stale flow listing cursor, restarting from first page");
                    collected.clear();
                    after = null;
                    restarted = true;
                    continue;
                }
                throw exception;
            }
            var data = response.getJSONArray("data");
            if (data != null) {
                for (var index = 0; index < data.size(); index++) {
                    collected.add(parseFlow(data.getJSONObject(index)));
                }
            }
            var paging = response.getJSONObject("paging");
            var cursors = paging == null ? null : paging.getJSONObject("cursors");
            var nextAfter = cursors == null ? null : cursors.getString("after");
            if (paging == null || paging.getString("next") == null || nextAfter == null || nextAfter.equals(after)) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "listed all flows, count {0}", collected.size());
                return collected;
            }
            after = nextAfter;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishFlow(String flowId) {
        api.post(flowId + "/publish", new JSONObject());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "published flow {0}", flowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deprecateFlow(String flowId) {
        api.post(flowId + "/deprecate", new JSONObject());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deprecated flow {0}", flowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFlowDetails queryFlow(String flowId) {
        var parameters = Map.of("fields",
                "id,name,status,categories,validation_errors,json_version,data_api_version,"
                        + "endpoint_uri,preview,application,health_status,whatsapp_business_account,updated_at");
        var response = api.get(flowId, parameters);
        return parseFlowDetails(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editFlowMetadata(CloudFlowMetadataEdit edit) {
        Objects.requireNonNull(edit, "edit must not be null");
        var body = new JSONObject();
        edit.name().ifPresent(name -> body.put("name", name));
        if (!edit.categories().isEmpty()) {
            var array = new JSONArray();
            array.addAll(edit.categories());
            body.put("categories", array);
        }
        edit.endpointUri().ifPresent(endpointUri -> body.put("endpoint_uri", endpointUri));
        edit.applicationId().ifPresent(applicationId -> body.put("application_id", applicationId));
        api.post(edit.flowId(), body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited flow metadata {0}", edit.flowId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFlow(String flowId) {
        api.delete(flowId, Map.of());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted flow {0}", flowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFlowJsonUploadResult uploadFlowJson(String flowId, byte[] flowJson) {
        var response = api.uploadFormFile(
                flowId + "/assets",
                Map.of("name", "flow.json", "asset_type", "FLOW_JSON", "messaging_product", "whatsapp"),
                "file",
                "flow.json",
                "application/json",
                flowJson);
        var success = response.getBooleanValue("success");
        var validationErrors = new ArrayList<CloudFlowValidationError>();
        var errors = response.getJSONArray("validation_errors");
        if (errors != null) {
            for (var index = 0; index < errors.size(); index++) {
                validationErrors.add(parseFlowValidationError(errors.getJSONObject(index)));
            }
        }
        if (success) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "uploaded flow json for {0}, size {1}", flowId, flowJson.length);
        } else if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "flow json upload for {0} failed validation, {1} errors",
                    flowId, validationErrors.size());
        }
        return new CloudFlowJsonUploadResult(success, validationErrors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudFlowAsset> queryFlowAssets(String flowId) {
        var response = api.get(flowId + "/assets", Map.of());
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudFlowAsset>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseFlowAsset(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFlowPreview queryFlowPreview(String flowId) {
        var response = api.get(flowId, Map.of("fields", "preview.invalidate(false)"));
        var preview = response.getJSONObject("preview");
        var source = preview != null ? preview : response;
        return new CloudFlowPreview(
                source.getString("preview_url"),
                parseIsoInstant(source.getString("expires_at")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudFlowMigrationResult migrateFlows(String sourceWabaId, List<String> sourceFlowNames) {
        Objects.requireNonNull(sourceFlowNames, "sourceFlowNames must not be null");
        var body = new JSONObject();
        body.put("source_waba_id", Objects.requireNonNull(sourceWabaId, "sourceWabaId must not be null"));
        if (!sourceFlowNames.isEmpty()) {
            var array = new JSONArray();
            array.addAll(sourceFlowNames);
            body.put("source_flow_names", array);
        }
        var response = api.post(requireWaba() + "/migrate_flows", body);
        var migrated = new ArrayList<CloudFlowMigrationResult.Migrated>();
        var migratedArray = response.getJSONArray("migrated_flows");
        if (migratedArray != null) {
            for (var index = 0; index < migratedArray.size(); index++) {
                var node = migratedArray.getJSONObject(index);
                migrated.add(new CloudFlowMigrationResult.Migrated(
                        node.getString("source_name"),
                        node.getString("source_id"),
                        node.getString("migrated_id")));
            }
        }
        var failed = new ArrayList<CloudFlowMigrationResult.Failed>();
        var failedArray = response.getJSONArray("failed_flows");
        if (failedArray != null) {
            for (var index = 0; index < failedArray.size(); index++) {
                var node = failedArray.getJSONObject(index);
                failed.add(new CloudFlowMigrationResult.Failed(
                        node.getString("source_name"),
                        node.getString("error_code"),
                        node.getString("error_message")));
            }
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "migrated flows from waba {0}, {1} migrated, {2} failed",
                sourceWabaId, migrated.size(), failed.size());
        return new CloudFlowMigrationResult(migrated, failed);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudQrCode createQrCode(String prefilledMessage) {
        var body = new JSONObject();
        body.put("prefilled_message", prefilledMessage);
        body.put("generate_qr_image", "PNG");
        var response = api.post(store.phoneNumberId() + "/message_qrdls", body);
        var qrCode = parseQrCode(response);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created qr code {0}", qrCode.code());
        return qrCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudQrCode> queryQrCodes() {
        var response = api.get(store.phoneNumberId() + "/message_qrdls", Map.of());
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudQrCode>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parseQrCode(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteQrCode(String code) {
        api.delete(store.phoneNumberId() + "/message_qrdls/" + code, Map.of());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted qr code {0}", code);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudMessagingAnalytics queryMessagingAnalytics(CloudMessagingAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var expansion = new StringBuilder("analytics.start(")
                .append(query.start().getEpochSecond())
                .append(").end(")
                .append(query.end().getEpochSecond())
                .append(").granularity(")
                .append(query.granularity().name())
                .append(')');
        appendStringArray(expansion, ".phone_numbers", query.phoneNumbers());
        appendRawArray(expansion, ".product_types", query.productTypes());
        appendStringArray(expansion, ".country_codes", query.countryCodes());
        var response = api.get(requireWaba(), Map.of("fields", expansion.toString()));
        return parseMessagingAnalytics(response.getJSONObject("analytics"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudConversationAnalytics queryConversationAnalytics(CloudConversationAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var expansion = new StringBuilder("conversation_analytics.start(")
                .append(query.start().getEpochSecond())
                .append(").end(")
                .append(query.end().getEpochSecond())
                .append(").granularity(")
                .append(query.granularity().name())
                .append(')');
        appendStringArray(expansion, ".phone_numbers", query.phoneNumbers());
        appendStringArray(expansion, ".country_codes", query.countryCodes());
        appendEnumArray(expansion, ".metric_types", query.metricTypes());
        appendEnumArray(expansion, ".conversation_categories", query.conversationCategories());
        appendEnumArray(expansion, ".conversation_types", query.conversationTypes());
        appendEnumArray(expansion, ".conversation_directions", query.conversationDirections());
        appendEnumArray(expansion, ".dimensions", query.dimensions());
        var response = api.get(requireWaba(), Map.of("fields", expansion.toString()));
        return parseConversationAnalytics(response.getJSONObject("conversation_analytics"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudPricingAnalytics queryPricingAnalytics(CloudPricingAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var expansion = new StringBuilder("pricing_analytics.start(")
                .append(query.start().getEpochSecond())
                .append(").end(")
                .append(query.end().getEpochSecond())
                .append(").granularity(")
                .append(query.granularity().name())
                .append(')');
        appendStringArray(expansion, ".phone_numbers", query.phoneNumbers());
        appendStringArray(expansion, ".country_codes", query.countryCodes());
        appendEnumArray(expansion, ".metric_types", query.metricTypes());
        appendEnumArray(expansion, ".dimensions", query.dimensions());
        appendEnumArray(expansion, ".pricing_types", query.pricingTypes());
        appendEnumArray(expansion, ".pricing_categories", query.pricingCategories());
        appendStringArray(expansion, ".tiers", query.tiers());
        var response = api.get(requireWaba(), Map.of("fields", expansion.toString()));
        return parsePricingAnalytics(response.getJSONObject("pricing_analytics"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudTemplateAnalytics> queryTemplateAnalytics(CloudTemplateAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return queryTemplateAnalyticsEdge("template_analytics", "template_ids", query.start(), query.end(),
                query.templateIds(), query.metricTypes(), query.productType().orElse(null),
                query.useBusinessAccountTimezone());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudTemplateAnalytics> queryTemplateGroupAnalytics(CloudTemplateGroupAnalyticsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return queryTemplateAnalyticsEdge("template_group_analytics", "template_group_ids", query.start(),
                query.end(), query.templateGroupIds(), query.metricTypes(), query.productType().orElse(null),
                query.useBusinessAccountTimezone());
    }

    /**
     * Queries a template-analytics WABA edge and flattens its {@code data[].data_points[]} envelope.
     *
     * <p>The {@code template_analytics} and {@code template_group_analytics} edges share a request shape
     * and response shape, differing only in the root field name and the id-list segment key, so both are
     * served from here. When {@code metricTypes} is empty the default set {@code SENT, DELIVERED, READ,
     * CLICKED} is requested; {@code productType} defaults to {@code CLOUD_API} when {@code null} and the
     * business-account timezone segment is omitted when {@code useBusinessAccountTimezone} is {@code false}.
     *
     * @param field                      the root field-expansion name of the edge
     * @param idKey                      the id-list segment key carrying {@code ids}
     * @param start                      the inclusive start of the window
     * @param end                        the exclusive end of the window
     * @param ids                        the template or template-group ids to report on
     * @param metricTypes                the metrics to request, or empty for the default set
     * @param productType                the messaging product to scope to, or {@code null} for {@code CLOUD_API}
     * @param useBusinessAccountTimezone whether to bucket in the account timezone
     * @return the flattened per-bucket data points
     */
    private List<CloudTemplateAnalytics> queryTemplateAnalyticsEdge(String field, String idKey, Instant start,
                                                                    Instant end, List<String> ids,
                                                                    List<CloudTemplateAnalytics.MetricType> metricTypes,
                                                                    CloudTemplateAnalytics.ProductType productType,
                                                                    boolean useBusinessAccountTimezone) {
        var types = metricTypes == null || metricTypes.isEmpty()
                ? List.of("SENT", "DELIVERED", "READ", "CLICKED")
                : metricTypes.stream().map(Enum::name).toList();
        var product = productType == null ? CloudTemplateAnalytics.ProductType.CLOUD_API : productType;
        var expansion = new StringBuilder(field)
                .append(".start(").append(start.getEpochSecond())
                .append(").end(").append(end.getEpochSecond())
                .append(").granularity(DAILY)")
                .append('.').append(idKey).append("([").append(String.join(",", ids)).append("])")
                .append(".metric_types([").append(String.join(",", types)).append("])")
                .append(".product_type(").append(product.name()).append(')');
        if (useBusinessAccountTimezone) {
            expansion.append(".use_waba_timezone(true)");
        }
        var response = api.get(requireWaba(), Map.of("fields", expansion.toString()));
        var analytics = response.getJSONObject(field);
        var result = new ArrayList<CloudTemplateAnalytics>();
        if (analytics == null) {
            return result;
        }
        var data = analytics.getJSONArray("data");
        if (data == null) {
            return result;
        }
        for (var di = 0; di < data.size(); di++) {
            var points = data.getJSONObject(di).getJSONArray("data_points");
            if (points == null) {
                continue;
            }
            for (var pi = 0; pi < points.size(); pi++) {
                result.add(parseTemplateAnalytics(points.getJSONObject(pi)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editConversationalAutomation(CloudConversationalAutomation automation) {
        Objects.requireNonNull(automation, "automation must not be null");
        var body = new JSONObject();
        automation.enableWelcomeMessage().ifPresent(value -> body.put("enable_welcome_message", value));
        if (!automation.prompts().isEmpty()) {
            var prompts = new JSONArray();
            prompts.addAll(automation.prompts());
            body.put("prompts", prompts);
        }
        if (!automation.commands().isEmpty()) {
            var commands = new JSONArray();
            for (var command : automation.commands()) {
                var entry = new JSONObject();
                entry.put("command_name", command.name());
                entry.put("command_description", command.description());
                commands.add(entry);
            }
            body.put("commands", commands);
        }
        api.post(store.phoneNumberId() + "/conversational_automation", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited conversational automation");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudConversationalAutomation> queryConversationalAutomation() {
        // The conversational_automation stanza-field wrapper is confirmed against a live response: the GET
        // returns {"conversational_automation":{"prompts":[...],"commands":[{"command_name":...,
        // "command_description":...}],"enable_welcome_message":...,"id":...},"id":...}, and just {"id":...}
        // when the stanza is unset.
        var response = api.get(store.phoneNumberId(), Map.of("fields", "conversational_automation"));
        var node = response.getJSONObject("conversational_automation");
        if (node == null) {
            return Optional.empty();
        }
        return Optional.of(parseConversationalAutomation(node));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudPaymentConfiguration> queryPaymentConfigurations() {
        var response = api.get(requireWaba() + "/payment_configurations", Map.of());
        var data = response.getJSONArray("data");
        var result = new ArrayList<CloudPaymentConfiguration>();
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                result.add(parsePaymentConfiguration(data.getJSONObject(index)));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudPaymentConfiguration> queryPaymentConfiguration(String name) {
        Objects.requireNonNull(name, "name must not be null");
        var response = api.get(requireWaba() + "/payment_configuration",
                Map.of("configuration_name", name));
        var data = response.getJSONArray("data");
        if (data != null) {
            if (data.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(parsePaymentConfiguration(data.getJSONObject(0)));
        }
        if (response.getString("configuration_name") == null) {
            return Optional.empty();
        }
        return Optional.of(parsePaymentConfiguration(response));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPaymentConfiguration(CloudPaymentConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration must not be null");
        var body = new JSONObject();
        // TODO: unverified - payments is region-gated; request/response body field set is from the
        //  fb-sdk edge + the payment_configuration_update webhook value
        body.put("configuration_name", configuration.configurationName());
        configuration.providerName().ifPresent(value -> body.put("provider_name", value));
        configuration.providerMerchantId().ifPresent(value -> body.put("provider_mid", value));
        api.post(requireWaba() + "/payment_configuration", body);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "created payment configuration {0}", configuration.configurationName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deletePaymentConfiguration(String name) {
        Objects.requireNonNull(name, "name must not be null");
        api.delete(requireWaba() + "/payment_configuration",
                Map.of("configuration_name", name));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted payment configuration {0}", name);
    }

    /**
     * Parses a payment-configuration stanza into a {@link CloudPaymentConfiguration}.
     *
     * @param node the payment-configuration stanza
     * @return the parsed payment configuration
     */
    private static CloudPaymentConfiguration parsePaymentConfiguration(JSONObject node) {
        // TODO: unverified - payments is region-gated; request/response body field set is from the
        //  fb-sdk edge + the payment_configuration_update webhook value
        return new CloudPaymentConfiguration(
                node.getString("configuration_name"),
                node.getString("provider_name"),
                node.getString("provider_mid"),
                node.getString("status"),
                epochInstant(node.getLong("created_timestamp")),
                epochInstant(node.getLong("updated_timestamp")));
    }

    /**
     * Projects an epoch-seconds value onto an {@link Instant}.
     *
     * @param epochSeconds the Unix second timestamp, or {@code null} when absent
     * @return the instant, or {@code null} when {@code epochSeconds} is {@code null}
     */
    private static Instant epochInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudCommerceSettings queryCommerceSettings() {
        var response = api.get(store.phoneNumberId() + "/whatsapp_commerce_settings", Map.of());
        var data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return new CloudCommerceSettings(null, null, null);
        }
        var node = data.getJSONObject(0);
        var cartEnabled = node.containsKey("is_cart_enabled") ? node.getBooleanValue("is_cart_enabled") : null;
        var catalogVisible = node.containsKey("is_catalog_visible") ? node.getBooleanValue("is_catalog_visible") : null;
        return new CloudCommerceSettings(node.getString("id"), cartEnabled, catalogVisible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void editCommerceSettings(CloudCommerceSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");
        var parameters = new HashMap<String, String>();
        settings.cartEnabled().ifPresent(value -> parameters.put("is_cart_enabled", String.valueOf(value)));
        settings.catalogVisible().ifPresent(value -> parameters.put("is_catalog_visible", String.valueOf(value)));
        if (parameters.isEmpty()) {
            throw new IllegalArgumentException("settings must carry at least one field to update");
        }
        api.postForm(store.phoneNumberId() + "/whatsapp_commerce_settings", parameters);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "edited commerce settings");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<CloudProductCatalog> queryConnectedProductCatalog() {
        var response = api.get(requireWaba() + "/product_catalogs", Map.of());
        var data = response.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Optional.empty();
        }
        var node = data.getJSONObject(0);
        return Optional.of(new CloudProductCatalog(node.getString("id"), node.getString("name")));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendProduct(JidProvider recipient, CloudProductMessage product) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(product, "product must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        var action = new JSONObject();
        action.put("catalog_id", product.catalogId());
        action.put("product_retailer_id", product.productRetailerId());
        var interactive = new JSONObject();
        interactive.put("type", "product");
        interactive.put("action", action);
        product.body().ifPresent(text -> {
            var bodyJson = new JSONObject();
            bodyJson.put("text", text);
            interactive.put("body", bodyJson);
        });
        product.footer().ifPresent(text -> {
            var footerJson = new JSONObject();
            footerJson.put("text", text);
            interactive.put("footer", footerJson);
        });
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent product to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendProductList(JidProvider recipient, CloudProductListMessage productList) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(productList, "productList must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        var header = new JSONObject();
        header.put("type", "text");
        header.put("text", productList.header());
        var bodyJson = new JSONObject();
        bodyJson.put("text", productList.body());
        var sections = new JSONArray();
        for (var section : productList.sections()) {
            var sectionJson = new JSONObject();
            sectionJson.put("title", section.title());
            var items = new JSONArray();
            for (var retailerId : section.productRetailerIds()) {
                var item = new JSONObject();
                item.put("product_retailer_id", retailerId);
                items.add(item);
            }
            sectionJson.put("product_items", items);
            sections.add(sectionJson);
        }
        var action = new JSONObject();
        action.put("catalog_id", productList.catalogId());
        action.put("sections", sections);
        var interactive = new JSONObject();
        interactive.put("type", "product_list");
        interactive.put("header", header);
        interactive.put("body", bodyJson);
        interactive.put("action", action);
        productList.footer().ifPresent(text -> {
            var footerJson = new JSONObject();
            footerJson.put("text", text);
            interactive.put("footer", footerJson);
        });
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent product list to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendCatalog(JidProvider recipient, CloudCatalogMessage catalog) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(catalog, "catalog must not be null");
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        var action = new JSONObject();
        action.put("name", "catalog_message");
        catalog.thumbnailProductRetailerId().ifPresent(thumbnail -> {
            var parameters = new JSONObject();
            parameters.put("thumbnail_product_retailer_id", thumbnail);
            action.put("parameters", parameters);
        });
        var bodyJson = new JSONObject();
        bodyJson.put("text", catalog.body());
        var interactive = new JSONObject();
        interactive.put("type", "catalog_message");
        interactive.put("action", action);
        interactive.put("body", bodyJson);
        catalog.footer().ifPresent(text -> {
            var footerJson = new JSONObject();
            footerJson.put("text", text);
            interactive.put("footer", footerJson);
        });
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent catalog to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addNewMessageListener(NewMessageListener<? super CloudWhatsAppClient> listener) {
        return addShared(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addMessageStatusListener(MessageStatusListener<? super CloudWhatsAppClient> listener) {
        return addShared(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addMessageDeletedListener(MessageDeletedListener<? super CloudWhatsAppClient> listener) {
        return addShared(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addMessageEchoListener(CloudMessageEchoListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addCallListener(CloudCallListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addCallStatusListener(CloudCallStatusListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addCallPermissionListener(CloudCallPermissionListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addTemplateStatusListener(CloudTemplateStatusListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addTemplateQualityListener(CloudTemplateQualityListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addTemplateCategoryListener(CloudTemplateCategoryListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addTemplateComponentsListener(CloudTemplateComponentsListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addTemplatePauseListener(CloudTemplatePauseListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addSecurityListener(CloudSecurityListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addPaymentConfigurationListener(CloudPaymentConfigurationListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addPhoneNumberListener(CloudPhoneNumberListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addAccountUpdateListener(CloudAccountUpdateListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addBusinessCapabilityListener(CloudBusinessCapabilityListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addUserPreferenceListener(CloudUserPreferenceListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addFlowListener(CloudFlowListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addHistoryListener(CloudHistoryListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addAppStateSyncListener(CloudAppStateSyncListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addMessagePricingListener(CloudMessagePricingListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addAccountSettingsListener(CloudAccountSettingsListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addSystemListener(CloudSystemListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addLoggedInListener(LoggedInListener<? super CloudWhatsAppClient> listener) {
        return addShared(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addDisconnectedListener(DisconnectedListener<? super CloudWhatsAppClient> listener) {
        return addShared(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addWebhookReceivedListener(CloudWebhookReceivedListener listener) {
        return addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWhatsAppClient addErrorListener(CloudErrorListener listener) {
        return addListener(listener);
    }

    /**
     * Dispatches a verified webhook envelope to the registered listeners.
     *
     * @param envelope the webhook envelope
     */
    private void dispatchEnvelope(JSONObject envelope) {
        forEach(CloudWebhookReceivedListener.class, listener -> listener.onWebhookReceived(this, envelope));
        var entries = envelope.getJSONArray("entry");
        if (entries == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "webhook envelope carried no entries");
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dispatching webhook envelope, {0} entries", entries.size());
        for (var entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            var changes = entries.getJSONObject(entryIndex).getJSONArray("changes");
            if (changes == null) {
                continue;
            }
            for (var changeIndex = 0; changeIndex < changes.size(); changeIndex++) {
                var change = changes.getJSONObject(changeIndex);
                dispatchChange(change.getString("field"), change.getJSONObject("value"));
            }
        }
    }

    /**
     * Dispatches a single webhook change to the listeners that match its field.
     *
     * @param field the change field
     * @param value the change value
     */
    private void dispatchChange(String field, JSONObject value) {
        if (field == null || value == null) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "dispatching webhook change, field {0}", field);
        switch (field) {
            case "messages" -> dispatchMessages(value);
            case "smb_message_echoes" -> dispatchEchoes(value);
            case "calls" -> dispatchCalls(value);
            case "message_template_status_update" -> {
                var update = CloudWebhookDecoder.decodeTemplateStatus(value);
                forEach(CloudTemplateStatusListener.class, listener -> listener.onTemplateStatus(this, update));
            }
            case "message_template_quality_update" -> {
                var update = CloudWebhookDecoder.decodeTemplateQuality(value);
                forEach(CloudTemplateQualityListener.class, listener -> listener.onTemplateQuality(this, update));
            }
            case "template_category_update" -> {
                var update = CloudWebhookDecoder.decodeTemplateCategory(value);
                forEach(CloudTemplateCategoryListener.class, listener -> listener.onTemplateCategory(this, update));
            }
            case "phone_number_name_update" -> {
                var update = CloudWebhookDecoder.decodePhoneNumberName(value);
                forEach(CloudPhoneNumberListener.class, listener -> listener.onPhoneNumberUpdate(this, update));
            }
            case "phone_number_quality_update" -> {
                var update = CloudWebhookDecoder.decodePhoneNumberQuality(value);
                forEach(CloudPhoneNumberListener.class, listener -> listener.onPhoneNumberUpdate(this, update));
            }
            case "account_update", "account_alerts", "account_review_update" -> {
                var update = CloudWebhookDecoder.decodeAccountUpdate(value);
                forEach(CloudAccountUpdateListener.class, listener -> listener.onAccountUpdate(this, update));
            }
            case "business_capability_update" -> {
                var update = CloudWebhookDecoder.decodeBusinessCapability(value);
                forEach(CloudBusinessCapabilityListener.class, listener -> listener.onBusinessCapabilityUpdate(this, update));
            }
            case "user_preferences" -> {
                for (var update : CloudWebhookDecoder.decodeUserPreferences(value)) {
                    forEach(CloudUserPreferenceListener.class, listener -> listener.onUserPreference(this, update));
                }
            }
            case "flows" -> {
                var update = CloudWebhookDecoder.decodeFlowStatus(value);
                forEach(CloudFlowListener.class, listener -> listener.onFlowStatus(this, update));
            }
            case "message_template_components_update" -> {
                var update = CloudWebhookDecoder.decodeTemplateComponents(value);
                forEach(CloudTemplateComponentsListener.class, listener -> listener.onTemplateComponents(this, update));
            }
            case "message_template_pause", "message_template_unpause" -> {
                var update = CloudWebhookDecoder.decodeTemplatePause(value);
                forEach(CloudTemplatePauseListener.class, listener -> listener.onTemplatePause(this, update));
            }
            case "security" -> {
                var update = CloudWebhookDecoder.decodeSecurity(value);
                forEach(CloudSecurityListener.class, listener -> listener.onSecurity(this, update));
            }
            case "payment_configuration_update" -> {
                var update = CloudWebhookDecoder.decodePaymentConfiguration(value);
                forEach(CloudPaymentConfigurationListener.class, listener -> listener.onPaymentConfiguration(this, update));
            }
            case "history" -> {
                for (var chunk : CloudWebhookDecoder.decodeHistory(value)) {
                    forEach(CloudHistoryListener.class, listener -> listener.onHistorySync(this, chunk));
                }
            }
            case "smb_app_state_sync" -> {
                for (var contact : CloudWebhookDecoder.decodeAppStateSync(value)) {
                    forEach(CloudAppStateSyncListener.class, listener -> listener.onAppStateSync(this, contact));
                }
            }
            case "account_settings_update" -> {
                var settings = CloudWebhookDecoder.decodeAccountSettings(value);
                if (settings != null) {
                    forEach(CloudAccountSettingsListener.class, listener -> listener.onAccountSettings(this, settings));
                }
            }
            default -> {
                // Unmodelled field; the raw envelope was already delivered to the webhook listeners.
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "unmodelled webhook change field {0}", field);
            }
        }
    }

    /**
     * Decodes and dispatches inbound messages and outbound statuses of a {@code messages} change.
     *
     * @param value the change value
     */
    private void dispatchMessages(JSONObject value) {
        reportUnmappedContent(value);
        for (var info : CloudWebhookDecoder.decodeMessages(value)) {
            info.key().parentJid().ifPresent(chat ->
                    info.key().id().ifPresent(id -> store.recordLastInboundMessageId(chat.toString(), id)));
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received message {0} from {1}",
                    info.key().id().orElse(null), info.key().parentJid().orElse(null));
            forEach(NewMessageListener.class, listener -> listener.onNewMessage(this, info));
        }
        for (var status : CloudWebhookDecoder.decodeStatuses(value)) {
            if (status.deleted()) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "message {0} deleted", status.info().key().id().orElse(null));
                forEach(MessageDeletedListener.class,
                        listener -> listener.onMessageDeleted(this, status.info(), true));
            } else {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "message {0} status update", status.info().key().id().orElse(null));
                forEach(MessageStatusListener.class,
                        listener -> listener.onMessageStatus(this, status.info()));
            }
            if (status.pricing() != null) {
                forEach(CloudMessagePricingListener.class,
                        listener -> listener.onMessagePricing(this, status.info().key(), status.pricing()));
            }
        }
        for (var reply : CloudWebhookDecoder.decodeCallPermissionReplies(value)) {
            forEach(CloudCallPermissionListener.class, listener -> listener.onCallPermission(this, reply));
        }
        for (var update : CloudWebhookDecoder.decodeSystemUpdates(value)) {
            forEach(CloudSystemListener.class, listener -> listener.onSystemUpdate(this, update));
        }
    }

    /**
     * Routes any inbound message whose content type has no decode to the error listeners.
     *
     * <p>An unmapped content type otherwise reaches the embedder as a message with an empty container,
     * silently dropping the content. Surfacing the type through the error listeners makes the gap
     * observable so a missing decode can be added.
     *
     * @param value the {@code messages} change value
     */
    private void reportUnmappedContent(JSONObject value) {
        var messages = value.getJSONArray("messages");
        if (messages == null) {
            return;
        }
        for (var index = 0; index < messages.size(); index++) {
            var message = messages.getJSONObject(index);
            var type = message.getString("type");
            // TODO: an unmapped inbound content type is delivered with an empty container; add its decode
            //       in CloudWebhookDecoder.decodeContent (and its mapped type) when the type is modelled.
            if (!CloudWebhookDecoder.isMappedContentType(type)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unmapped inbound message content type {0}", type);
                fireError(new IllegalStateException("Unmapped inbound message content type: " + type));
            }
        }
    }

    /**
     * Decodes and dispatches the business message echoes of an {@code smb_message_echoes} change.
     *
     * @param value the change value
     */
    private void dispatchEchoes(JSONObject value) {
        for (var info : CloudWebhookDecoder.decodeMessageEchoes(value)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received message echo {0}", info.key().id().orElse(null));
            forEach(CloudMessageEchoListener.class, listener -> listener.onMessageEcho(this, info));
        }
    }

    /**
     * Decodes and dispatches the calling events of a {@code calls} change.
     *
     * @param value the change value
     */
    private void dispatchCalls(JSONObject value) {
        for (var event : CloudWebhookDecoder.decodeCalls(value)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received call signaling {0} from {1}",
                    event.callId(), new LogRedactable.Phone(event.from()));
            forEach(CloudCallListener.class, listener -> listener.onCall(this, event));
        }
        for (var event : CloudWebhookDecoder.decodeCallStatuses(value)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "received call status {0} for call {1}",
                    event.status(), event.callId());
            forEach(CloudCallStatusListener.class, listener -> listener.onCallStatus(this, event));
        }
    }

    /**
     * Invokes the given action for each registered listener that matches the listener type, routing
     * any failure to the error listeners and the error handler.
     *
     * @param type   the listener type to match
     * @param action the action to run for each matching listener
     * @param <T>    the listener type
     */
    private <T extends WhatsAppListener> void forEach(Class<T> type, Consumer<T> action) {
        for (var listener : listeners) {
            if (type.isInstance(listener)) {
                try {
                    action.accept(type.cast(listener));
                } catch (RuntimeException exception) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "listener of type " + type.getSimpleName() + " failed", exception);
                    fireError(exception);
                }
            }
        }
    }

    /**
     * Routes a processing failure to the error listeners and the configurable error handler.
     *
     * @param error the failure
     */
    private void fireError(Throwable error) {
        for (var listener : listeners) {
            if (listener instanceof CloudErrorListener errorListener) {
                try {
                    errorListener.onError(this, error);
                } catch (RuntimeException ignored) {
                    // A failing error listener must not mask the original failure.
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudWaba queryBusinessAccount() {
        var response = api.get(requireWaba(), Map.of("fields", WABA_FIELDS));
        return parseWaba(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudWaba> queryOwnedBusinessAccounts() {
        var response = api.get(requireBusiness() + "/owned_whatsapp_business_accounts", Map.of());
        return parseWabaList(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudWaba> queryClientBusinessAccounts() {
        var response = api.get(requireBusiness() + "/client_whatsapp_business_accounts", Map.of());
        return parseWabaList(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addBusinessAccountUser(String businessUserId, Set<CloudBusinessAccountUserTask> tasks) {
        Objects.requireNonNull(businessUserId, "businessUserId must not be null");
        Objects.requireNonNull(tasks, "tasks must not be null");
        var taskArray = new JSONArray();
        for (var task : tasks) {
            taskArray.add(task.name());
        }
        api.postForm(requireWaba() + "/assigned_users",
                Map.of("user", businessUserId, "tasks", taskArray.toString()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "added business account user {0}, {1} tasks", businessUserId, tasks.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeBusinessAccountUser(String businessUserId) {
        Objects.requireNonNull(businessUserId, "businessUserId must not be null");
        api.delete(requireWaba() + "/assigned_users", Map.of("user", businessUserId));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removed business account user {0}", businessUserId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CloudWabaAssignedUser> queryBusinessAccountUsers() {
        var response = api.get(requireWaba() + "/assigned_users", Map.of("business", requireBusiness()));
        var data = response.getJSONArray("data");
        if (data == null) {
            return List.of();
        }
        var result = new ArrayList<CloudWabaAssignedUser>();
        for (var i = 0; i < data.size(); i++) {
            var node = data.getJSONObject(i);
            var tasksArray = node.getJSONArray("tasks");
            var tasks = new ArrayList<CloudBusinessAccountUserTask>();
            if (tasksArray != null) {
                for (var j = 0; j < tasksArray.size(); j++) {
                    tasks.add(CloudBusinessAccountUserTask.of(tasksArray.getString(j)));
                }
            }
            result.add(new CloudWabaAssignedUser(node.getString("id"), node.getString("name"), tasks));
        }
        return List.copyOf(result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudOAuthToken exchangeSignupCode(CloudSignupCodeExchange exchange) {
        Objects.requireNonNull(exchange, "exchange must not be null");
        var credentials = exchange.credentials();
        var response = api.getUnauthenticated("oauth/access_token", Map.of(
                "client_id", credentials.appId(),
                "client_secret", credentials.appSecret(),
                "redirect_uri", exchange.redirectUri(),
                "code", exchange.code()));
        var token = parseOAuthToken(response);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "exchanged signup code for access token {0}",
                new LogRedactable.Token(token.accessToken()));
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudOAuthToken exchangeLongLivedToken(CloudAppCredentials credentials, String shortLivedToken) {
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(shortLivedToken, "shortLivedToken must not be null");
        var response = api.getUnauthenticated("oauth/access_token", Map.of(
                "grant_type", "fb_exchange_token",
                "client_id", credentials.appId(),
                "client_secret", credentials.appSecret(),
                "fb_exchange_token", shortLivedToken));
        var token = parseOAuthToken(response);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "exchanged short-lived token for access token {0}",
                new LogRedactable.Token(token.accessToken()));
        return token;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudTokenInspection inspectToken(String token, String appAccessToken) {
        Objects.requireNonNull(token, "token must not be null");
        Objects.requireNonNull(appAccessToken, "appAccessToken must not be null");
        var response = api.getUnauthenticated("debug_token", Map.of(
                "input_token", token,
                "access_token", appAccessToken));
        var data = response.getJSONObject("data");
        if (data == null) {
            throw new WhatsAppCloudApiException("debug_token returned no data");
        }
        var scopesArray = data.getJSONArray("scopes");
        var scopes = new ArrayList<String>();
        if (scopesArray != null) {
            for (var i = 0; i < scopesArray.size(); i++) {
                scopes.add(scopesArray.getString(i));
            }
        }
        var issuedAt = data.containsKey("issued_at")
                ? Instant.ofEpochSecond(data.getLong("issued_at")) : null;
        var expiresAt = data.containsKey("expires_at") && data.getLong("expires_at") != 0
                ? Instant.ofEpochSecond(data.getLong("expires_at")) : null;
        var inspection = new CloudTokenInspection(
                data.getString("app_id"),
                data.getString("type"),
                data.getString("application"),
                data.getBooleanValue("is_valid"),
                issuedAt,
                expiresAt,
                scopes,
                data.getString("user_id"));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "inspected token {0}, valid {1}", new LogRedactable.Token(token), inspection.valid());
        return inspection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CloudCreditAllocation shareCreditLine(String extendedCreditId, String businessAccountId, Currency currency) {
        Objects.requireNonNull(extendedCreditId, "extendedCreditId must not be null");
        Objects.requireNonNull(businessAccountId, "businessAccountId must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        var body = new JSONObject();
        body.put("waba_id", businessAccountId);
        body.put("waba_currency", currency.getCurrencyCode());
        var response = api.post(extendedCreditId + "/whatsapp_credit_sharing_and_attach", body);
        var allocation = new CloudCreditAllocation(
                response.getString("allocation_config_id"),
                response.getString("waba_id"));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "shared credit line to waba {0}", businessAccountId);
        return allocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendOrderDetails(JidProvider recipient, CloudOrderDetailsMessage order) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(order, "order must not be null");
        var parameters = new JSONObject();
        parameters.put("reference_id", order.referenceId());
        parameters.put("type", order.type().token());
        parameters.put("currency", order.currency());
        parameters.put("total_amount", amountJson(order.totalAmount()));
        switch (order.payment()) {
            case CloudOrderPayment.India india -> {
                parameters.put("payment_type", india.paymentType());
                parameters.put("payment_configuration", india.configurationName());
            }
            case CloudOrderPayment.Gateway gateway -> {
                if (!gateway.settings().isEmpty()) {
                    var settings = new JSONArray();
                    for (var setting : gateway.settings()) {
                        settings.add(paymentSettingJson(setting));
                    }
                    parameters.put("payment_settings", settings);
                }
            }
        }
        parameters.put("order", orderJson(order.order()));
        var action = new JSONObject();
        action.put("name", "review_and_pay");
        action.put("parameters", parameters);
        var bodyJson = new JSONObject();
        bodyJson.put("text", order.bodyText());
        var interactive = new JSONObject();
        interactive.put("type", "order_details");
        interactive.put("body", bodyJson);
        order.footerText().ifPresent(value -> {
            var footer = new JSONObject();
            footer.put("text", value);
            interactive.put("footer", footer);
        });
        order.headerImageLink().ifPresent(value -> {
            var image = new JSONObject();
            image.put("link", value);
            var header = new JSONObject();
            header.put("type", "image");
            header.put("image", image);
            interactive.put("header", header);
        });
        interactive.put("action", action);
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent order details to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageKey sendOrderStatus(JidProvider recipient, CloudOrderStatusMessage status) {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(status, "status must not be null");
        var orderJson = new JSONObject();
        orderJson.put("status", status.status().token());
        status.description().ifPresent(value -> orderJson.put("description", value));
        var parameters = new JSONObject();
        parameters.put("reference_id", status.referenceId());
        parameters.put("order", orderJson);
        var action = new JSONObject();
        action.put("name", "review_order");
        action.put("parameters", parameters);
        var bodyJson = new JSONObject();
        bodyJson.put("text", status.bodyText());
        var interactive = new JSONObject();
        interactive.put("type", "order_status");
        interactive.put("body", bodyJson);
        status.footerText().ifPresent(value -> {
            var footer = new JSONObject();
            footer.put("text", value);
            interactive.put("footer", footer);
        });
        interactive.put("action", action);
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", recipient.toJid().user());
        body.put("type", "interactive");
        body.put("interactive", interactive);
        var response = api.post(store.phoneNumberId() + "/messages", body);
        var key = new MessageKeyBuilder()
                .id(firstMessageId(response))
                .parentJid(recipient.toJid())
                .fromMe(true)
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sent order status to {0}, id {1}", recipient.toJid(), key.id().orElse(null));
        return key;
    }

    /**
     * Serializes a monetary amount as its {@code value}/{@code offset} object.
     *
     * @param amount the amount to serialize
     * @return the JSON amount object
     */
    private static JSONObject amountJson(CloudOrderAmount amount) {
        var json = new JSONObject();
        json.put("value", amount.value());
        json.put("offset", amount.offset());
        return json;
    }

    /**
     * Serializes a monetary amount with an optional description.
     *
     * @param amount      the amount to serialize
     * @param description the description, or {@code null} when none
     * @return the JSON amount object
     */
    private static JSONObject amountJson(CloudOrderAmount amount, String description) {
        var json = amountJson(amount);
        if (description != null) {
            json.put("description", description);
        }
        return json;
    }

    /**
     * Serializes a payment setting as its {@code type}/{@code payment_gateway} object.
     *
     * @param setting the setting to serialize
     * @return the JSON payment-setting object
     */
    private static JSONObject paymentSettingJson(CloudOrderPaymentSetting setting) {
        var json = new JSONObject();
        json.put("type", setting.type());
        if (setting.gatewayType().isPresent() || setting.configurationName().isPresent()) {
            var gateway = new JSONObject();
            setting.gatewayType().ifPresent(value -> gateway.put("type", value));
            setting.configurationName().ifPresent(value -> gateway.put("configuration_name", value));
            json.put(setting.type(), gateway);
        }
        return json;
    }

    /**
     * Serializes an order breakdown as the {@code order} object of an {@code order_details} message.
     *
     * @param order the order to serialize
     * @return the JSON order object
     */
    private static JSONObject orderJson(CloudOrder order) {
        var json = new JSONObject();
        json.put("status", order.status().token());
        order.catalogId().ifPresent(value -> json.put("catalog_id", value));
        var items = new JSONArray();
        for (var item : order.items()) {
            items.add(itemJson(item));
        }
        json.put("items", items);
        json.put("subtotal", amountJson(order.subtotal()));
        order.tax().ifPresent(value -> json.put("tax", amountJson(value.amount(), value.description().orElse(null))));
        order.shipping().ifPresent(value ->
                json.put("shipping", amountJson(value.amount(), value.description().orElse(null))));
        order.discount().ifPresent(value ->
                json.put("discount", amountJson(value.amount(), value.description().orElse(null))));
        return json;
    }

    /**
     * Serializes a single order line item.
     *
     * @param item the item to serialize
     * @return the JSON item object
     */
    private static JSONObject itemJson(CloudOrderItem item) {
        var json = new JSONObject();
        json.put("retailer_id", item.retailerId());
        json.put("name", item.name());
        json.put("amount", amountJson(item.amount()));
        json.put("quantity", item.quantity());
        item.saleAmount().ifPresent(value -> json.put("sale_amount", amountJson(value)));
        return json;
    }

    /**
     * Parses a WhatsApp Business Account stanza into a {@link CloudWaba}.
     *
     * @param node the account stanza
     * @return the parsed account, with absent optional fields when the stanza omitted them
     */
    private static CloudWaba parseWaba(JSONObject node) {
        return new CloudWaba(
                node.getString("id"),
                node.getString("name"),
                node.getString("currency"),
                node.getString("timezone_id"),
                node.getString("message_template_namespace"),
                node.getString("country"),
                node.getString("business_verification_status"),
                enumOrNull(node.getString("account_review_status"), CloudWabaReviewStatus::of),
                node.getString("status"),
                enumOrNull(node.getString("ownership_type"), CloudWabaOwnershipType::of));
    }

    /**
     * Parses a {@code data} array of WhatsApp Business Account nodes, preserving their order.
     *
     * @param response the listing response
     * @return the parsed accounts, empty when the response carried no data
     */
    private static List<CloudWaba> parseWabaList(JSONObject response) {
        var data = response.getJSONArray("data");
        if (data == null) {
            return List.of();
        }
        var result = new ArrayList<CloudWaba>();
        for (var i = 0; i < data.size(); i++) {
            result.add(parseWaba(data.getJSONObject(i)));
        }
        return List.copyOf(result);
    }

    /**
     * Parses a Facebook Login token response into a {@link CloudOAuthToken}.
     *
     * @param response the token response
     * @return the parsed token
     */
    private static CloudOAuthToken parseOAuthToken(JSONObject response) {
        var expiresIn = response.containsKey("expires_in")
                ? Duration.ofSeconds(response.getLongValue("expires_in")) : null;
        return new CloudOAuthToken(
                response.getString("access_token"),
                response.getString("token_type"),
                expiresIn);
    }

    /**
     * Returns the business portfolio id, requiring it to be configured.
     *
     * @return the business portfolio id
     * @throws IllegalStateException if no business portfolio id was configured
     */
    private String requireBusiness() {
        return store.businessId().orElseThrow(
                () -> new IllegalStateException("operation requires a businessId"));
    }

    /**
     * Returns the WhatsApp Business Account id, requiring it to be configured.
     *
     * @return the WABA id
     * @throws IllegalStateException if no WABA id was configured
     */
    private String requireWaba() {
        return store.whatsappBusinessAccountId().orElseThrow(
                () -> new IllegalStateException("operation requires a whatsappBusinessAccountId"));
    }

    /**
     * Returns the Meta app id, requiring it to be configured.
     *
     * @return the Meta app id
     * @throws IllegalStateException if no app id was configured
     */
    private String requireApp() {
        return store.appId().orElseThrow(
                () -> new IllegalStateException("operation requires an appId"));
    }

    /**
     * Posts a calling-status change to the phone number's settings edge.
     *
     * @param status the calling status, {@code "ENABLED"} or {@code "DISABLED"}
     */
    private void applyCallingStatus(String status) {
        var settings = new JSONObject();
        var calling = new JSONObject();
        calling.put("status", status);
        settings.put("calling", calling);
        api.post(store.phoneNumberId() + "/settings", settings);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "calling status -> {0}", status);
    }

    /**
     * Rejects an operation whose minimum Cloud API version is newer than the configured version.
     *
     * @param minimum   the minimum version the operation requires
     * @param operation the operation name surfaced on the thrown exception
     * @throws WhatsAppCloudUnsupportedVersionException if the configured version is older
     *                                                                 than {@code minimum}
     */
    private void requireVersion(CloudApiVersion minimum, String operation) {
        if (!apiVersion.isAtLeast(minimum)) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "operation {0} requires api version {1}, configured {2}",
                    operation, minimum, apiVersion);
            throw new WhatsAppCloudUnsupportedVersionException(operation, minimum, apiVersion);
        }
    }

    /**
     * Appends a {@code .key(["a","b"])} field-expansion segment when the value list is non-empty.
     *
     * @param target the field-expansion buffer to append to
     * @param key    the segment key, including its leading dot
     * @param values the string values, or {@code null}/empty to append nothing
     */
    private static void appendStringArray(StringBuilder target, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        target.append(key).append("([");
        for (var index = 0; index < values.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append('"').append(values.get(index)).append('"');
        }
        target.append("])");
    }

    /**
     * Appends a {@code .key([a,b])} field-expansion segment with unquoted values when the list is
     * non-empty.
     *
     * @param target the field-expansion buffer to append to
     * @param key    the segment key, including its leading dot
     * @param values the raw values, or {@code null}/empty to append nothing
     */
    private static void appendRawArray(StringBuilder target, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        target.append(key).append("([").append(String.join(",", values)).append("])");
    }

    /**
     * Appends a {@code .key([X,Y])} field-expansion segment with enum-name tokens when the list is
     * non-empty.
     *
     * @param target the field-expansion buffer to append to
     * @param key    the segment key, including its leading dot
     * @param values the enum values, or {@code null}/empty to append nothing
     * @param <E>    the enum type
     */
    private static <E extends Enum<E>> void appendEnumArray(StringBuilder target, String key, List<E> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        target.append(key).append("([");
        for (var index = 0; index < values.size(); index++) {
            if (index > 0) {
                target.append(',');
            }
            target.append(values.get(index).name());
        }
        target.append("])");
    }

    /**
     * Parses the {@code analytics} stanza of a messaging-analytics response.
     *
     * @param node the {@code analytics} stanza, or {@code null} when absent
     * @return the parsed messaging analytics, empty when {@code stanza} is {@code null}
     */
    private static CloudMessagingAnalytics parseMessagingAnalytics(JSONObject node) {
        if (node == null) {
            return new CloudMessagingAnalytics(null, null, null, null);
        }
        var phoneNumbers = stringList(node.getJSONArray("phone_numbers"));
        var countryCodes = stringList(node.getJSONArray("country_codes"));
        var granularity = node.getString("granularity");
        var points = new ArrayList<CloudMessagingAnalytics.DataPoint>();
        var dataPoints = node.getJSONArray("data_points");
        if (dataPoints != null) {
            for (var index = 0; index < dataPoints.size(); index++) {
                var point = dataPoints.getJSONObject(index);
                points.add(new CloudMessagingAnalytics.DataPoint(
                        Instant.ofEpochSecond(point.getLongValue("start")),
                        Instant.ofEpochSecond(point.getLongValue("end")),
                        point.getIntValue("sent"),
                        point.getIntValue("delivered")));
            }
        }
        return new CloudMessagingAnalytics(phoneNumbers, countryCodes, granularity, points);
    }

    /**
     * Parses the {@code conversation_analytics} stanza of a conversation-analytics response, flattening the
     * {@code data[].data_points[]} envelope.
     *
     * @param node the {@code conversation_analytics} stanza, or {@code null} when absent
     * @return the parsed conversation analytics, empty when {@code stanza} is {@code null}
     */
    private static CloudConversationAnalytics parseConversationAnalytics(JSONObject node) {
        var points = new ArrayList<CloudConversationAnalytics.DataPoint>();
        var data = node == null ? null : node.getJSONArray("data");
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                var dataPoints = data.getJSONObject(index).getJSONArray("data_points");
                if (dataPoints == null) {
                    continue;
                }
                for (var pointIndex = 0; pointIndex < dataPoints.size(); pointIndex++) {
                    var point = dataPoints.getJSONObject(pointIndex);
                    points.add(new CloudConversationAnalytics.DataPoint(
                            Instant.ofEpochSecond(point.getLongValue("start")),
                            Instant.ofEpochSecond(point.getLongValue("end")),
                            point.getLongValue("conversation"),
                            point.getDoubleValue("cost"),
                            point.getString("phone_number"),
                            point.getString("country"),
                            point.getString("conversation_type"),
                            point.getString("conversation_direction"),
                            point.getString("conversation_category")));
                }
            }
        }
        return new CloudConversationAnalytics(points);
    }

    /**
     * Parses the {@code pricing_analytics} stanza of a pricing-analytics response, flattening the
     * {@code data[].data_points[]} envelope.
     *
     * @param node the {@code pricing_analytics} stanza, or {@code null} when absent
     * @return the parsed pricing analytics, empty when {@code stanza} is {@code null}
     */
    private static CloudPricingAnalytics parsePricingAnalytics(JSONObject node) {
        var points = new ArrayList<CloudPricingAnalytics.DataPoint>();
        var data = node == null ? null : node.getJSONArray("data");
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                // TODO: Request parameters and all request enums are confirmed against the official
                //  facebook-python-business-sdk; only the response data_points leaf field names remain
                //  untyped by Meta's own API spec (both official SDKs return a generic object), so they
                //  are best-effort
                var dataPoints = data.getJSONObject(index).getJSONArray("data_points");
                if (dataPoints == null) {
                    continue;
                }
                for (var pointIndex = 0; pointIndex < dataPoints.size(); pointIndex++) {
                    var point = dataPoints.getJSONObject(pointIndex);
                    points.add(new CloudPricingAnalytics.DataPoint(
                            Instant.ofEpochSecond(point.getLongValue("start")),
                            Instant.ofEpochSecond(point.getLongValue("end")),
                            point.getLongValue("volume"),
                            point.getDoubleValue("cost"),
                            point.getString("pricing_type"),
                            point.getString("pricing_category"),
                            point.getString("phone_number"),
                            point.getString("country"),
                            point.getString("tier")));
                }
            }
        }
        return new CloudPricingAnalytics(points);
    }

    /**
     * Parses a single {@code data_points[]} entry of a template-analytics response.
     *
     * @param point the data-point stanza
     * @return the parsed template analytics
     */
    private static CloudTemplateAnalytics parseTemplateAnalytics(JSONObject point) {
        // TODO: Request parameters and all request enums are confirmed against the official
        //  facebook-python-business-sdk; only the response data_points leaf field names remain
        //  untyped by Meta's own API spec (both official SDKs return a generic object), so they
        //  are best-effort
        var clicks = new ArrayList<CloudTemplateAnalytics.ButtonClick>();
        var clicked = point.getJSONArray("clicked");
        if (clicked != null) {
            for (var index = 0; index < clicked.size(); index++) {
                var click = clicked.getJSONObject(index);
                clicks.add(new CloudTemplateAnalytics.ButtonClick(
                        click.getString("type"),
                        click.getString("button_content"),
                        click.getLongValue("count")));
            }
        }
        return new CloudTemplateAnalytics(
                point.getString("template_id"),
                Instant.ofEpochSecond(point.getLongValue("start")),
                Instant.ofEpochSecond(point.getLongValue("end")),
                point.getLong("sent"),
                point.getLong("delivered"),
                point.getLong("read"),
                clicks);
    }

    /**
     * Copies a JSON string array into a mutable {@link List}.
     *
     * @param array the JSON array, or {@code null} when absent
     * @return the list of strings, empty when {@code array} is {@code null}
     */
    private static List<String> stringList(JSONArray array) {
        var result = new ArrayList<String>();
        if (array != null) {
            for (var index = 0; index < array.size(); index++) {
                result.add(array.getString(index));
            }
        }
        return result;
    }

    /**
     * Builds the {@code session} object of a calling signaling request.
     *
     * @param session the session description
     * @return the {@code session} JSON object
     */
    private static JSONObject sessionJson(CloudCallSession session) {
        var json = new JSONObject();
        json.put("sdp_type", session.sdpType().token());
        json.put("sdp", session.sdp());
        return json;
    }

    /**
     * Builds the {@code call_hours} object of a calling-settings update.
     *
     * @param hours the business-hours configuration
     * @return the {@code call_hours} JSON object
     */
    private static JSONObject callHoursBody(CloudCallHours hours) {
        var json = new JSONObject();
        hours.status().ifPresent(value -> json.put("status", value));
        hours.timezoneId().ifPresent(value -> json.put("timezone_id", value));
        var weekly = new JSONArray();
        for (var slot : hours.weeklyOperatingHours()) {
            var slotJson = new JSONObject();
            slotJson.put("day_of_week", slot.dayOfWeek());
            slotJson.put("open_time", slot.openTime());
            slotJson.put("close_time", slot.closeTime());
            weekly.add(slotJson);
        }
        json.put("weekly_operating_hours", weekly);
        if (!hours.holidaySchedule().isEmpty()) {
            var holidays = new JSONArray();
            for (var slot : hours.holidaySchedule()) {
                var slotJson = new JSONObject();
                slotJson.put("date", slot.date());
                slotJson.put("start_time", slot.startTime());
                slotJson.put("end_time", slot.endTime());
                holidays.add(slotJson);
            }
            json.put("holiday_schedule", holidays);
        }
        return json;
    }

    /**
     * Builds the {@code sip} object of a calling-settings update.
     *
     * @param sip the SIP bridge configuration
     * @return the {@code sip} JSON object
     */
    private static JSONObject sipBody(CloudCallSettings.Sip sip) {
        var json = new JSONObject();
        sip.status().ifPresent(value -> json.put("status", value));
        var servers = new JSONArray();
        for (var server : sip.servers()) {
            var serverJson = new JSONObject();
            server.hostname().ifPresent(value -> serverJson.put("hostname", value));
            server.port().ifPresent(value -> serverJson.put("port", value));
            if (!server.requestUriUserParams().isEmpty()) {
                serverJson.put("request_uri_user_params", new JSONObject(new LinkedHashMap<>(server.requestUriUserParams())));
            }
            servers.add(serverJson);
        }
        json.put("servers", servers);
        return json;
    }

    /**
     * Parses a {@code calling} settings object into a {@link CloudCallSettings}.
     *
     * @param calling the {@code calling} object
     * @return the parsed configuration
     */
    private CloudCallSettings parseCallSettings(JSONObject calling) {
        var status = calling.getString("status");
        var iconVisibility = calling.getString("call_icon_visibility");
        CloudCallSettings.CallIcons callIcons = null;
        var iconsNode = calling.getJSONObject("call_icons");
        if (iconsNode != null) {
            var countries = new ArrayList<String>();
            var countryArray = iconsNode.getJSONArray("restrict_to_user_countries");
            if (countryArray != null) {
                for (var index = 0; index < countryArray.size(); index++) {
                    countries.add(countryArray.getString(index));
                }
            }
            callIcons = new CloudCallSettings.CallIcons(countries);
        }
        var callbackStatus = calling.getString("callback_permission_status");
        var srtp = calling.getString("srtp_key_exchange_protocol");
        var callHours = parseCallHours(calling.getJSONObject("call_hours"));
        var sip = parseSip(calling.getJSONObject("sip"));
        return new CloudCallSettings(status, iconVisibility, callIcons, callbackStatus, srtp, callHours, sip);
    }

    /**
     * Parses a {@code call_hours} object into a {@link CloudCallHours}.
     *
     * @param hours the {@code call_hours} object, or {@code null}
     * @return the parsed business-hours configuration, or {@code null} when the input is {@code null}
     */
    private static CloudCallHours parseCallHours(JSONObject hours) {
        if (hours == null) {
            return null;
        }
        var weekly = new ArrayList<CloudCallHours.WeeklyOperatingHours>();
        var weeklyArray = hours.getJSONArray("weekly_operating_hours");
        if (weeklyArray != null) {
            for (var index = 0; index < weeklyArray.size(); index++) {
                var slot = weeklyArray.getJSONObject(index);
                weekly.add(new CloudCallHours.WeeklyOperatingHours(
                        slot.getString("day_of_week"),
                        slot.getString("open_time"),
                        slot.getString("close_time")));
            }
        }
        var holidays = new ArrayList<CloudCallHours.HolidaySchedule>();
        var holidayArray = hours.getJSONArray("holiday_schedule");
        if (holidayArray != null) {
            for (var index = 0; index < holidayArray.size(); index++) {
                var slot = holidayArray.getJSONObject(index);
                holidays.add(new CloudCallHours.HolidaySchedule(
                        slot.getString("date"),
                        slot.getString("start_time"),
                        slot.getString("end_time")));
            }
        }
        return new CloudCallHours(hours.getString("status"), hours.getString("timezone_id"), weekly, holidays);
    }

    /**
     * Parses a {@code sip} object into a {@link CloudCallSettings.Sip}.
     *
     * @param sip the {@code sip} object, or {@code null}
     * @return the parsed SIP configuration, or {@code null} when the input is {@code null}
     */
    private static CloudCallSettings.Sip parseSip(JSONObject sip) {
        if (sip == null) {
            return null;
        }
        var servers = new ArrayList<CloudCallSettings.SipServer>();
        var serverArray = sip.getJSONArray("servers");
        if (serverArray != null) {
            for (var index = 0; index < serverArray.size(); index++) {
                var server = serverArray.getJSONObject(index);
                var port = server.containsKey("port") ? server.getInteger("port") : null;
                var appId = server.containsKey("app_id") ? server.getInteger("app_id") : null;
                var params = new LinkedHashMap<String, String>();
                var paramsNode = server.getJSONObject("request_uri_user_params");
                if (paramsNode != null) {
                    for (var key : paramsNode.keySet()) {
                        params.put(key, paramsNode.getString(key));
                    }
                }
                servers.add(new CloudCallSettings.SipServer(
                        server.getString("hostname"), port, server.getString("sip_user_password"),
                        params, appId));
            }
        }
        return new CloudCallSettings.Sip(sip.getString("status"), servers);
    }

    /**
     * Builds the {@code block_users} request body for a single contact.
     *
     * @param contact the contact
     * @return the request body
     */
    private static JSONObject blockUsersBody(JidProvider contact) {
        var body = new JSONObject();
        body.put("messaging_product", "whatsapp");
        var users = new JSONArray();
        var user = new JSONObject();
        user.put("user", contact.toJid().user());
        users.add(user);
        body.put("block_users", users);
        return body;
    }

    /**
     * Extracts the {@code wamid} of the first sent message in a messages response.
     *
     * @param response the messages response
     * @return the message id, or {@code null} when absent
     */
    private static String firstMessageId(JSONObject response) {
        var messages = response.getJSONArray("messages");
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.getJSONObject(0).getString("id");
    }

    /**
     * Parses a phone-number stanza into a {@link CloudPhoneNumber}.
     *
     * @param node the phone-number stanza
     * @return the parsed phone number
     */
    private CloudPhoneNumber parsePhoneNumber(JSONObject node) {
        var throughput = node.getJSONObject("throughput");
        var throughputLevel = throughput != null ? throughput.getString("level") : null;
        var official = node.containsKey("is_official_business_account")
                ? node.getBooleanValue("is_official_business_account") : null;
        return new CloudPhoneNumber(
                node.getString("id") != null ? node.getString("id") : store.phoneNumberId(),
                node.getString("display_phone_number"),
                node.getString("verified_name"),
                enumOrNull(node.getString("quality_rating"), CloudPhoneNumberQualityRating::of),
                enumOrNull(node.getString("code_verification_status"), CloudPhoneNumberCodeVerificationStatus::of),
                enumOrNull(node.getString("status"), CloudPhoneNumberStatus::of),
                enumOrNull(node.getString("name_status"), CloudPhoneNumberNameStatus::of),
                enumOrNull(node.getString("new_name_status"), CloudPhoneNumberNameStatus::of),
                enumOrNull(node.getString("messaging_limit_tier"), CloudMessagingLimitTier::of),
                enumOrNull(throughputLevel, CloudThroughputLevel::of),
                enumOrNull(node.getString("platform_type"), CloudPhoneNumberPlatformType::of),
                node.getString("certificate"),
                official,
                enumOrNull(node.getString("account_mode"), CloudPhoneNumberAccountMode::of),
                node.getString("new_certificate"));
    }

    /**
     * Maps a wire token through an enum factory, preserving absence as {@code null} so the model
     * exposes the field as an empty optional rather than the enum's {@code UNKNOWN} sentinel.
     *
     * @param token   the wire token, or {@code null} when the stanza omitted it
     * @param factory the enum {@code of(String)} factory
     * @param <E>     the enum type
     * @return the resolved enum constant, or {@code null} when {@code token} is {@code null}
     */
    private static <E> E enumOrNull(String token, Function<String, E> factory) {
        return token == null ? null : factory.apply(token);
    }

    /**
     * Parses a template stanza into a {@link CloudMessageTemplate}.
     *
     * @param node the template stanza
     * @return the parsed template
     */
    private static CloudMessageTemplate parseTemplate(JSONObject node) {
        return new CloudMessageTemplate(
                node.getString("id"),
                node.getString("name"),
                node.getString("language"),
                CloudTemplateCategory.of(node.getString("category")),
                CloudTemplateStatus.of(node.getString("status")),
                CloudMessageEncoder.decodeTemplateComponents(node.getJSONArray("components")));
    }

    /**
     * Builds the {@code BUTTONS} component of an authentication-template upsert from an OTP button.
     *
     * <p>The OTP button's text and autofill label are intentionally not emitted; the server localizes
     * them per language. {@code ZERO_TAP} buttons emit the zero-tap terms acknowledgement, and buttons
     * carrying supported apps emit them as an array of {@code package_name}/{@code signature_hash}
     * objects.
     *
     * @param button the OTP button to encode
     * @return the {@code BUTTONS} component object
     */
    private static JSONObject upsertButtonsComponent(CloudOtpButton button) {
        var otp = new JSONObject();
        otp.put("type", "OTP");
        List<CloudOtpButton.App> supportedApps = switch (button) {
            case CloudOtpButton.CopyCode copyCode -> {
                otp.put("otp_type", "COPY_CODE");
                yield List.of();
            }
            case CloudOtpButton.OneTap oneTap -> {
                otp.put("otp_type", "ONE_TAP");
                yield oneTap.supportedApps();
            }
            case CloudOtpButton.ZeroTap zeroTap -> {
                otp.put("otp_type", "ZERO_TAP");
                otp.put("zero_tap_terms_accepted", zeroTap.zeroTapTermsAccepted());
                yield zeroTap.supportedApps();
            }
        };
        if (!supportedApps.isEmpty()) {
            var apps = new JSONArray();
            for (var app : supportedApps) {
                var appJson = new JSONObject();
                appJson.put("package_name", app.packageName());
                appJson.put("signature_hash", app.signatureHash());
                apps.add(appJson);
            }
            otp.put("supported_apps", apps);
        }
        var buttons = new JSONArray();
        buttons.add(otp);
        var component = new JSONObject();
        component.put("type", "BUTTONS");
        component.put("buttons", buttons);
        return component;
    }

    /**
     * Parses one upsert-response {@code data} entry into a {@link CloudUpsertedTemplate}.
     *
     * @param node the data entry
     * @return the parsed upserted-template entry
     */
    private static CloudUpsertedTemplate parseUpsertedTemplate(JSONObject node) {
        var category = node.getString("category");
        return new CloudUpsertedTemplate(
                node.getString("id"),
                CloudTemplateStatus.of(node.getString("status")),
                node.getString("language"),
                category == null ? null : CloudTemplateCategory.of(category));
    }

    /**
     * Parses a template-comparison metric array into a {@link CloudTemplateComparison}.
     *
     * <p>The response {@code data} array carries one entry per metric. {@code BLOCK_RATE} contributes the
     * {@code order_by_relative_metric} ordering, {@code MESSAGE_SENDS} contributes the
     * {@code number_values} send counts, and {@code TOP_BLOCK_REASON} contributes the
     * {@code string_values} block reasons. Any metric may be absent.
     *
     * @param response the comparison response
     * @return the flattened comparison
     */
    private static CloudTemplateComparison parseTemplateComparison(JSONObject response) {
        var blockRateOrder = new ArrayList<String>();
        var timesSent = new LinkedHashMap<String, Long>();
        var topBlockReason = new LinkedHashMap<String, CloudTemplateBlockReason>();
        var data = response.getJSONArray("data");
        if (data != null) {
            for (var index = 0; index < data.size(); index++) {
                var metric = data.getJSONObject(index);
                switch (metric.getString("metric")) {
                    case "BLOCK_RATE" -> {
                        var order = metric.getJSONArray("order_by_relative_metric");
                        if (order != null) {
                            for (var oi = 0; oi < order.size(); oi++) {
                                blockRateOrder.add(order.getString(oi));
                            }
                        }
                    }
                    case "MESSAGE_SENDS" -> {
                        var values = metric.getJSONArray("number_values");
                        if (values != null) {
                            for (var vi = 0; vi < values.size(); vi++) {
                                var entry = values.getJSONObject(vi);
                                timesSent.put(entry.getString("key"), entry.getLong("value"));
                            }
                        }
                    }
                    case "TOP_BLOCK_REASON" -> {
                        var values = metric.getJSONArray("string_values");
                        if (values != null) {
                            for (var vi = 0; vi < values.size(); vi++) {
                                var entry = values.getJSONObject(vi);
                                topBlockReason.put(entry.getString("key"),
                                        CloudTemplateBlockReason.of(entry.getString("value")));
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        var perTemplate = new LinkedHashMap<String, CloudTemplateComparison.Metrics>();
        var keys = new LinkedHashSet<String>();
        keys.addAll(timesSent.keySet());
        keys.addAll(topBlockReason.keySet());
        for (var key : keys) {
            perTemplate.put(key, new CloudTemplateComparison.Metrics(
                    timesSent.getOrDefault(key, 0L), topBlockReason.get(key)));
        }
        return new CloudTemplateComparison(blockRateOrder, perTemplate);
    }

    /**
     * Parses a conversational automation object returned by the Graph API.
     *
     * @param node the JSON object carrying the conversational automation fields
     * @return the parsed conversational automation
     */
    private static CloudConversationalAutomation parseConversationalAutomation(JSONObject node) {
        var enableWelcome = node.containsKey("enable_welcome_message")
                ? node.getBooleanValue("enable_welcome_message")
                : null;
        var prompts = new ArrayList<String>();
        var promptsArray = node.getJSONArray("prompts");
        if (promptsArray != null) {
            for (var index = 0; index < promptsArray.size(); index++) {
                prompts.add(promptsArray.getString(index));
            }
        }
        var commands = new ArrayList<CloudConversationalAutomation.Command>();
        var commandsArray = node.getJSONArray("commands");
        if (commandsArray != null) {
            for (var index = 0; index < commandsArray.size(); index++) {
                var entry = commandsArray.getJSONObject(index);
                commands.add(new CloudConversationalAutomation.Command(
                        entry.getString("command_name"),
                        entry.getString("command_description")));
            }
        }
        return new CloudConversationalAutomation(enableWelcome, prompts, commands);
    }

    /**
     * Parses a flow stanza into a {@link CloudFlow}.
     *
     * @param node the flow stanza
     * @return the parsed flow
     */
    private static CloudFlow parseFlow(JSONObject node) {
        var categories = new ArrayList<String>();
        var array = node.getJSONArray("categories");
        if (array != null) {
            for (var index = 0; index < array.size(); index++) {
                categories.add(array.getString(index));
            }
        }
        var status = node.getString("status");
        return new CloudFlow(node.getString("id"), node.getString("name"),
                status == null ? null : CloudFlowStatus.of(status), categories);
    }

    /**
     * Parses a rich flow stanza into a {@link CloudFlowDetails}.
     *
     * @param node the flow stanza
     * @return the parsed flow detail view
     */
    private static CloudFlowDetails parseFlowDetails(JSONObject node) {
        var categories = new ArrayList<String>();
        var categoryArray = node.getJSONArray("categories");
        if (categoryArray != null) {
            for (var index = 0; index < categoryArray.size(); index++) {
                categories.add(categoryArray.getString(index));
            }
        }
        var validationErrors = new ArrayList<CloudFlowValidationError>();
        var errorArray = node.getJSONArray("validation_errors");
        if (errorArray != null) {
            for (var index = 0; index < errorArray.size(); index++) {
                validationErrors.add(parseFlowValidationError(errorArray.getJSONObject(index)));
            }
        }
        CloudFlowPreview preview = null;
        var previewNode = node.getJSONObject("preview");
        if (previewNode != null) {
            preview = new CloudFlowPreview(
                    previewNode.getString("preview_url"),
                    parseIsoInstant(previewNode.getString("expires_at")));
        }
        CloudFlowApplication application = null;
        var applicationNode = node.getJSONObject("application");
        if (applicationNode != null) {
            application = new CloudFlowApplication(
                    applicationNode.getString("id"),
                    applicationNode.getString("name"),
                    applicationNode.getString("link"));
        }
        var healthNode = node.getJSONObject("health_status");
        var healthToken = healthNode != null ? healthNode.getString("can_send_message") : null;
        var healthStatus = healthToken == null ? null : CloudFlowEndpointAvailability.of(healthToken);
        var wabaNode = node.getJSONObject("whatsapp_business_account");
        var wabaId = wabaNode != null ? wabaNode.getString("id") : null;
        var status = node.getString("status");
        return new CloudFlowDetails(
                node.getString("id"),
                node.getString("name"),
                status == null ? null : CloudFlowStatus.of(status),
                categories,
                validationErrors,
                node.getString("json_version"),
                node.getString("data_api_version"),
                node.getString("endpoint_uri"),
                preview,
                application,
                healthStatus,
                wabaId,
                parseIsoInstant(node.getString("updated_at")));
    }

    /**
     * Parses an ISO-8601 timestamp string into an {@link Instant}, tolerating Graph's offset variants.
     *
     * @param value the ISO-8601 timestamp, or {@code null}
     * @return the instant, or {@code null} when the value is {@code null} or unparseable
     */
    private static Instant parseIsoInstant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // not an extended-offset ISO string; try the basic-offset and zulu forms below
        }
        try {
            return OffsetDateTime.parse(value,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]Z")).toInstant();
        } catch (DateTimeParseException ignored) {
            // not a basic-offset string; try the zulu form below
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * Parses a flow validation-error stanza into a {@link CloudFlowValidationError}.
     *
     * @param node the validation-error stanza
     * @return the parsed validation error
     */
    private static CloudFlowValidationError parseFlowValidationError(JSONObject node) {
        CloudFlowValidationError.Span span = null;
        if (node.containsKey("line_start") && node.containsKey("line_end")
                && node.containsKey("column_start") && node.containsKey("column_end")) {
            span = new CloudFlowValidationError.Span(
                    node.getIntValue("line_start"), node.getIntValue("line_end"),
                    node.getIntValue("column_start"), node.getIntValue("column_end"));
        }
        return new CloudFlowValidationError(
                node.getString("error"),
                node.getString("error_type"),
                node.getString("message"),
                span);
    }

    /**
     * Parses a flow-asset stanza into a {@link CloudFlowAsset}.
     *
     * @param node the asset stanza
     * @return the parsed asset
     */
    private static CloudFlowAsset parseFlowAsset(JSONObject node) {
        return new CloudFlowAsset(
                node.getString("name"),
                CloudFlowAssetType.of(node.getString("asset_type")),
                node.getString("download_url"));
    }

    /**
     * Parses a QR short-link stanza into a {@link CloudQrCode}.
     *
     * @param node the QR stanza
     * @return the parsed QR short-link
     */
    private static CloudQrCode parseQrCode(JSONObject node) {
        return new CloudQrCode(
                node.getString("code"),
                node.getString("prefilled_message"),
                node.getString("deep_link_url"),
                node.getString("qr_image_url"));
    }

    /**
     * Parses a business-profile stanza into a {@link BusinessProfile}.
     *
     * @param node the business-profile stanza
     * @return the parsed business profile
     */
    private BusinessProfile parseBusinessProfile(JSONObject node) {
        var websites = new ArrayList<URI>();
        var array = node.getJSONArray("websites");
        if (array != null) {
            for (var index = 0; index < array.size(); index++) {
                websites.add(URI.create(array.getString(index)));
            }
        }
        return new BusinessProfileBuilder()
                .jid(Jid.of(store.phoneNumberId(), JidServer.user()))
                .description(node.getString("description"))
                .address(node.getString("address"))
                .email(node.getString("email"))
                .websites(websites)
                .build();
    }
}
