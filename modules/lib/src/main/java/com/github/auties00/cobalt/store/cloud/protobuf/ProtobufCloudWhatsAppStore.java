package com.github.auties00.cobalt.store.cloud.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.mixin.PathMixin;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.util.BufferedProtobufOutputStream;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.builtin.ProtobufLazyMixin;
import it.auties.protobuf.model.ProtobufType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

/**
 * The protobuf-backed implementation of {@link CloudWhatsAppStore}.
 *
 * <p>This is a {@code @ProtobufMessage} so a Cloud session can be serialised and restored; the contract
 * of every accessor is specified on {@link CloudWhatsAppStore}. Instances are built through the generated
 * {@link ProtobufCloudWhatsAppStoreBuilder}.
 */
@ProtobufMessage
public final class ProtobufCloudWhatsAppStore implements CloudWhatsAppStore {
    /**
     * The system-user access token authenticating every request.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String accessToken;

    /**
     * The phone number id whose edges send messages and manage the profile.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String phoneNumberId;

    /**
     * The WhatsApp Business Account id used by template and phone-number management edges, or
     * {@code null} when those operations are not used.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String whatsappBusinessAccountId;

    /**
     * The business portfolio id used by partner onboarding edges, or {@code null} when unused.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String businessId;

    /**
     * The graph API version segment, for example {@code v23.0}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    String apiVersion;

    /**
     * The app secret used for {@code appsecret_proof} and webhook signature verification, or
     * {@code null} when proofs and signature checks are disabled.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    String appSecret;

    /**
     * The webhook verify token echoed during the subscription handshake, or {@code null} when the
     * built-in receiver is disabled.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String webhookVerifyToken;

    /**
     * The bind address of the webhook receiver, or {@code null} to bind the wildcard address.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    String webhookBindAddress;

    /**
     * The TCP port of the webhook receiver, or {@code null} when the built-in receiver is disabled.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.INT32)
    Integer webhookPort;

    /**
     * The URL path the webhook receiver listens on.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    String webhookPath;

    /**
     * The Meta app id addressed by the Resumable Upload API, or {@code null} when resumable uploads
     * are not used.
     *
     * <p>The Resumable Upload API creates an upload session under the {@code /{APP_ID}/uploads} edge,
     * so unlike the phone-number and WABA edges it is keyed by the application rather than a business
     * asset. It is unset until configured because the other Cloud edges do not need it.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    String appId;

    /**
     * The id of the last inbound message seen per chat, keyed by the bare chat JID string.
     *
     * <p>The Cloud transport marks a chat as read by posting a {@code status: read} update for a message
     * id rather than for a chat, so {@link com.github.auties00.cobalt.client.cloud.CloudWhatsAppClient#markChatAsRead}
     * needs the last inbound message id of the chat. Holding the mapping here rather than in client
     * memory lets it survive a serialise/restore cycle, so a restored client can still mark a chat read
     * for a message that arrived before the restore.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.STRING)
    final ConcurrentMap<String, String> lastInboundMessageIdByChat;

    /**
     * The session directory this store serialises to, or {@code null} when the store has no durable
     * backing (the transient variant).
     *
     * <p>Persisted as a {@link ProtobufType#STRING} through {@link PathMixin} so a restored store reopens
     * the same directory without a separate wiring step; the persistent factory supplies it through the
     * builder on creation and the deserializer restores it on load. The transient factory leaves it unset
     * so {@link #save()} and {@link #delete()} are no-ops.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING, mixins = {PathMixin.class, ProtobufLazyMixin.class})
    Path storeDirectory;

    /**
     * The logger for {@link ProtobufCloudWhatsAppStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufCloudWhatsAppStore.class);

    /**
     * The name of the metadata file written under the session directory.
     */
    private static final String STORE_FILE = "store.proto";

    /**
     * Constructs a new Cloud store.
     *
     * @param accessToken               the system-user access token
     * @param phoneNumberId             the operating phone number id
     * @param whatsappBusinessAccountId the WhatsApp Business Account id, or {@code null}
     * @param businessId                the business portfolio id, or {@code null}
     * @param apiVersion                the graph API version segment
     * @param appSecret                 the app secret, or {@code null}
     * @param webhookVerifyToken        the webhook verify token, or {@code null}
     * @param webhookBindAddress        the webhook bind address, or {@code null}
     * @param webhookPort               the webhook port, or {@code null} to disable the receiver
     * @param webhookPath               the webhook URL path
     * @param appId                     the Meta app id used by the Resumable Upload API, or {@code null}
     * @param lastInboundMessageIdByChat the last inbound message id per chat, or {@code null} for an
     *                                   empty map
     * @param storeDirectory            the session directory this store serialises to, or {@code null}
     *                                  for the transient variant
     */
    ProtobufCloudWhatsAppStore(String accessToken, String phoneNumberId, String whatsappBusinessAccountId,
                               String businessId, String apiVersion, String appSecret, String webhookVerifyToken,
                               String webhookBindAddress, Integer webhookPort, String webhookPath, String appId,
                               ConcurrentMap<String, String> lastInboundMessageIdByChat, Path storeDirectory) {
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.whatsappBusinessAccountId = whatsappBusinessAccountId;
        this.businessId = businessId;
        this.apiVersion = apiVersion;
        this.appSecret = appSecret;
        this.webhookVerifyToken = webhookVerifyToken;
        this.webhookBindAddress = webhookBindAddress;
        this.webhookPort = webhookPort;
        this.webhookPath = webhookPath;
        this.appId = appId;
        this.lastInboundMessageIdByChat = Objects.requireNonNullElseGet(lastInboundMessageIdByChat, ConcurrentHashMap::new);
        this.storeDirectory = storeDirectory;
    }

    @Override
    public String accessToken() {
        return accessToken;
    }

    @Override
    public String phoneNumberId() {
        return phoneNumberId;
    }

    @Override
    public Optional<String> whatsappBusinessAccountId() {
        return Optional.ofNullable(whatsappBusinessAccountId);
    }

    @Override
    public Optional<String> businessId() {
        return Optional.ofNullable(businessId);
    }

    @Override
    public String apiVersion() {
        return apiVersion;
    }

    @Override
    public Optional<String> appSecret() {
        return Optional.ofNullable(appSecret);
    }

    @Override
    public Optional<String> webhookVerifyToken() {
        return Optional.ofNullable(webhookVerifyToken);
    }

    @Override
    public Optional<String> webhookBindAddress() {
        return Optional.ofNullable(webhookBindAddress);
    }

    @Override
    public OptionalInt webhookPort() {
        return webhookPort == null || webhookPort == 0 ? OptionalInt.empty() : OptionalInt.of(webhookPort);
    }

    @Override
    public String webhookPath() {
        return webhookPath;
    }

    @Override
    public Optional<String> appId() {
        return Optional.ofNullable(appId);
    }

    @Override
    public Map<String, String> lastInboundMessageIdByChat() {
        return lastInboundMessageIdByChat;
    }

    @Override
    public void recordLastInboundMessageId(String chatJid, String messageId) {
        Objects.requireNonNull(chatJid, "chatJid must not be null");
        Objects.requireNonNull(messageId, "messageId must not be null");
        lastInboundMessageIdByChat.put(chatJid, messageId);
    }

    @Override
    public Optional<String> lastInboundMessageId(String chatJid) {
        Objects.requireNonNull(chatJid, "chatJid must not be null");
        return Optional.ofNullable(lastInboundMessageIdByChat.get(chatJid));
    }

    @Override
    public boolean hasWebhookReceiver() {
        return webhookPort != null && webhookPort != 0 && webhookVerifyToken != null;
    }

    @Override
    public ProtobufCloudWhatsAppStore setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setWhatsappBusinessAccountId(String whatsappBusinessAccountId) {
        this.whatsappBusinessAccountId = whatsappBusinessAccountId;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setBusinessId(String businessId) {
        this.businessId = businessId;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setAppSecret(String appSecret) {
        this.appSecret = appSecret;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setAppId(String appId) {
        this.appId = appId;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setWebhookVerifyToken(String webhookVerifyToken) {
        this.webhookVerifyToken = webhookVerifyToken;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setWebhookBindAddress(String webhookBindAddress) {
        this.webhookBindAddress = webhookBindAddress;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setWebhookPort(Integer webhookPort) {
        this.webhookPort = webhookPort;
        return this;
    }

    @Override
    public ProtobufCloudWhatsAppStore setWebhookPath(String webhookPath) {
        this.webhookPath = webhookPath;
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation serialises the whole snapshot synchronously to {@code store.proto} via a
     * sibling temp file and an atomic move, so a crash mid-write never leaves a truncated file. The
     * snapshot is tiny (credentials plus the per-chat read markers), so there is no debounced flusher
     * as on the Linked store. Returns without writing when no session directory is attached, and logs
     * and swallows any {@link IOException} so a caller on a mutation path is never forced to handle it.
     */
    @Override
    public void save() {
        var directory = storeDirectory;
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
            var path = directory.resolve(STORE_FILE);
            var tempFile = Files.createTempFile(directory, STORE_FILE, ".tmp");
            try {
                try (var stream = new BufferedProtobufOutputStream(tempFile)) {
                    ProtobufCloudWhatsAppStoreSpec.encode(this, stream);
                }
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "saved cloud store snapshot");
            } catch (IOException | RuntimeException error) {
                Files.deleteIfExists(tempFile);
                throw error;
            }
        } catch (IOException error) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to save cloud store snapshot", error);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation recursively removes the session directory and detaches the durable backing
     * so a subsequent {@link #save()} cannot recreate the just-deleted file. Returns without error when
     * no session directory is attached.
     */
    @Override
    public void delete() throws IOException {
        var directory = storeDirectory;
        if (directory == null) {
            return;
        }
        storeDirectory = null;
        if (Files.notExists(directory)) {
            return;
        }
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    throw new UncheckedIOException(error);
                }
            });
        } catch (UncheckedIOException error) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to delete cloud store directory", error.getCause());
            throw error.getCause();
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "deleted cloud store directory");
    }

    /**
     * Returns whether two Cloud stores carry the same configuration.
     *
     * @param other the object to compare with
     * @return {@code true} if {@code other} is a {@code CloudWhatsAppStore} with equal fields
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof ProtobufCloudWhatsAppStore that
                && Objects.equals(accessToken, that.accessToken)
                && Objects.equals(phoneNumberId, that.phoneNumberId)
                && Objects.equals(whatsappBusinessAccountId, that.whatsappBusinessAccountId)
                && Objects.equals(businessId, that.businessId)
                && Objects.equals(apiVersion, that.apiVersion)
                && Objects.equals(appSecret, that.appSecret)
                && Objects.equals(webhookVerifyToken, that.webhookVerifyToken)
                && Objects.equals(webhookBindAddress, that.webhookBindAddress)
                && Objects.equals(webhookPort, that.webhookPort)
                && Objects.equals(webhookPath, that.webhookPath)
                && Objects.equals(appId, that.appId)
                && Objects.equals(lastInboundMessageIdByChat, that.lastInboundMessageIdByChat)
                && Objects.equals(storeDirectory, that.storeDirectory);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code of this store's configuration
     */
    @Override
    public int hashCode() {
        return Objects.hash(accessToken, phoneNumberId, whatsappBusinessAccountId, businessId, apiVersion,
                appSecret, webhookVerifyToken, webhookBindAddress, webhookPort, webhookPath, appId,
                lastInboundMessageIdByChat, storeDirectory);
    }
}
