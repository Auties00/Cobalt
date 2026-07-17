package com.github.auties00.cobalt.client.cloud;

import com.github.auties00.cobalt.client.WhatsAppClientBuilder;
import com.github.auties00.cobalt.client.WhatsAppClientProxy;
import com.github.auties00.cobalt.client.WhatsAppClientProxyAuthenticator;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.cloud.CloudApiVersion;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStore;
import com.github.auties00.cobalt.store.cloud.CloudWhatsAppStoreFactory;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.util.Objects;
import java.util.Optional;

/**
 * A fluent builder that constructs {@link CloudWhatsAppClient} instances.
 *
 * <p>The builder is staged, mirroring the {@code LinkedWhatsAppClientBuilder} flow. The root stage
 * resolves the backing store through the {@link CloudWhatsAppStoreFactory} it was constructed with
 * (persistent by default, or the one passed to {@link CloudWhatsAppClient#builder(CloudWhatsAppStoreFactory)}):
 * {@link #loadConnection(String, String)}
 * reopens the session persisted for a phone number id or creates a fresh one from the supplied
 * credentials, while {@link #loadConnection(String)} and {@link #loadLatestConnection()} resume an
 * existing one without provisioning. Each returns an {@link Options} stage that writes the optional Graph
 * configuration (WhatsApp Business Account id, business portfolio id, API version, app secret, app id), the
 * transport configuration (proxy, HTTP client), and the webhook receiver directly into the resolved store.
 * The webhook receiver is its own sub-stage ({@link Options#webhook(String, int)} returning
 * {@link Webhook}) so it is either configured as one explicit decision or not at all.
 *
 * @see CloudWhatsAppClient
 * @see CloudWhatsAppStoreFactory
 * @see WhatsAppClientBuilder#cloudApi()
 */
public sealed class CloudWhatsAppClientBuilder permits CloudWhatsAppClientBuilder.Options {
    /**
     * The logger for {@link CloudWhatsAppClientBuilder}.
     */
    private static final System.Logger LOGGER = Log.get(CloudWhatsAppClientBuilder.class);

    /**
     * The factory that resolves the backing store.
     */
    private final CloudWhatsAppStoreFactory storeFactory;

    /**
     * Package-private constructor backed by the {@link CloudWhatsAppStoreFactory#persistent() persistent}
     * store factory; obtain instances via {@link CloudWhatsAppClient#builder()}.
     */
    CloudWhatsAppClientBuilder() {
        this(CloudWhatsAppStoreFactory.persistent());
    }

    /**
     * Package-private constructor backed by the given store factory; obtain instances via
     * {@link CloudWhatsAppClient#builder(CloudWhatsAppStoreFactory)}.
     *
     * @param storeFactory the factory that resolves the backing store
     * @throws NullPointerException if {@code storeFactory} is {@code null}
     */
    CloudWhatsAppClientBuilder(CloudWhatsAppStoreFactory storeFactory) {
        this.storeFactory = Objects.requireNonNull(storeFactory, "storeFactory must not be null");
    }

    /**
     * Loads the connection for the Cloud credentials through the store factory, creating a fresh one if
     * none is persisted yet.
     *
     * <p>When a session is already persisted for {@code phoneNumberId} it is reopened (preserving the
     * stored configuration and per-chat read markers) and its access token is refreshed to
     * {@code accessToken} to absorb a rotated token; otherwise a fresh session is created and recorded.
     * The store is resolved through the factory the builder was constructed with, which is persistent
     * unless {@link CloudWhatsAppClient#builder(CloudWhatsAppStoreFactory)} supplied
     * {@link CloudWhatsAppStoreFactory#temporary()} for a RAM-only session.
     *
     * @param accessToken   the system-user access token
     * @param phoneNumberId the operating phone number id
     * @return the next builder stage backed by the resolved store
     * @throws NullPointerException if {@code accessToken} or {@code phoneNumberId} is {@code null}
     * @throws IOException          if the store cannot be read or created
     */
    public Options loadConnection(String accessToken, String phoneNumberId) throws IOException {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        Objects.requireNonNull(phoneNumberId, "phoneNumberId must not be null");
        var existing = storeFactory.load(phoneNumberId);
        if (existing.isPresent()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resuming persisted connection for phone number {0}", new LogRedactable.Phone(phoneNumberId));
            return new Options(existing.get().setAccessToken(accessToken));
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating fresh connection for phone number {0}", new LogRedactable.Phone(phoneNumberId));
        return new Options(storeFactory.create(accessToken, phoneNumberId));
    }

    /**
     * Loads the connection whose phone number id matches {@code phoneNumberId}.
     *
     * @param phoneNumberId the phone number id of the connection to load
     * @return the next builder stage if a matching store was found, empty otherwise
     * @throws NullPointerException if {@code phoneNumberId} is {@code null}
     * @throws IOException          if the store cannot be read
     */
    public Optional<Options> loadConnection(String phoneNumberId) throws IOException {
        Objects.requireNonNull(phoneNumberId, "phoneNumberId must not be null");
        var result = storeFactory.load(phoneNumberId).map(Options::new);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded connection for phone number {0}: {1}", new LogRedactable.Phone(phoneNumberId), result.isPresent());
        return result;
    }

    /**
     * Loads the most recently created or loaded connection.
     *
     * @return the next builder stage if a previous connection exists, empty otherwise
     * @throws IOException if the store cannot be read
     */
    public Optional<Options> loadLatestConnection() throws IOException {
        var result = storeFactory.loadLatest().map(Options::new);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded latest connection: {0}", result.isPresent());
        return result;
    }

    /**
     * The factory-backed builder stage.
     *
     * <p>Collects the transport configuration (proxy, HTTP client) and writes the optional Graph
     * configuration and the webhook receiver directly into the store resolved by the previous stage,
     * then produces the client with {@link #build()}.
     */
    public static sealed class Options extends CloudWhatsAppClientBuilder permits Webhook {
        /**
         * The default webhook URL path.
         */
        static final String DEFAULT_WEBHOOK_PATH = "/webhook";

        /**
         * The store resolved by the previous stage, into which configuration writes are applied.
         */
        final CloudWhatsAppStore store;

        /**
         * The proxy routing outbound Graph traffic, or {@code null} for a direct connection.
         */
        private WhatsAppClientProxy proxy;

        /**
         * The HTTP client used by the transport, or {@code null} to build a default-configured one.
         */
        private HttpClient httpClient;

        /**
         * Constructs the stage around the resolved store.
         *
         * @param store the store to configure
         */
        Options(CloudWhatsAppStore store) {
            this.store = store;
        }

        /**
         * Sets the proxy that routes outbound Graph traffic.
         *
         * <p>The Cloud transport rides {@code java.net.http}, which supports only HTTP {@code CONNECT}
         * proxies; supplying a SOCKS proxy fails at build time. The proxy applies to the default-built
         * HTTP client; when an explicit {@link #httpClient(HttpClient)} is supplied, its own proxy
         * configuration wins.
         *
         * @param proxy the proxy, or {@code null} for a direct connection
         * @return this builder, for chaining
         */
        public Options proxy(WhatsAppClientProxy proxy) {
            this.proxy = proxy;
            return this;
        }

        /**
         * Sets the HTTP client used by the transport.
         *
         * @param httpClient the HTTP client, or {@code null} to build a default-configured one
         * @return this builder, for chaining
         */
        public Options httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the WhatsApp Business Account id used by template and phone-number management edges.
         *
         * @param whatsappBusinessAccountId the WhatsApp Business Account id, or {@code null} to leave it
         *                                  unset
         * @return this builder, for chaining
         */
        public Options whatsappBusinessAccountId(String whatsappBusinessAccountId) {
            store.setWhatsappBusinessAccountId(whatsappBusinessAccountId);
            return this;
        }

        /**
         * Sets the business portfolio id used by partner onboarding edges.
         *
         * @param businessId the business portfolio id, or {@code null} to leave it unset
         * @return this builder, for chaining
         */
        public Options businessId(String businessId) {
            store.setBusinessId(businessId);
            return this;
        }

        /**
         * Sets the graph API version targeted by every request.
         *
         * @param apiVersion the API version, or {@code null} to keep the default
         *                   ({@link CloudApiVersion#DEFAULT})
         * @return this builder, for chaining
         */
        public Options apiVersion(CloudApiVersion apiVersion) {
            if (apiVersion != null) {
                store.setApiVersion(apiVersion.version());
            }
            return this;
        }

        /**
         * Sets the app secret used for {@code appsecret_proof} and webhook signature verification.
         *
         * @param appSecret the app secret, or {@code null} to disable proofs and signature checks
         * @return this builder, for chaining
         */
        public Options appSecret(String appSecret) {
            store.setAppSecret(appSecret);
            return this;
        }

        /**
         * Sets the Meta app id used by the Resumable Upload API.
         *
         * <p>The Resumable Upload API creates an upload session under the {@code /{APP_ID}/uploads} edge,
         * so it is required only for resumable media uploads and is otherwise optional.
         *
         * @param appId the Meta app id, or {@code null} to leave it unset
         * @return this builder, for chaining
         */
        public Options appId(String appId) {
            store.setAppId(appId);
            return this;
        }

        /**
         * Configures the built-in webhook receiver and moves to the webhook sub-stage.
         *
         * <p>The verify token is echoed during the subscription handshake and the port is where the
         * receiver binds; the path defaults to {@code /webhook} until {@link Webhook#path(String)}
         * overrides it. Skipping this step leaves the client send-only.
         *
         * @param verifyToken the webhook verify token
         * @param port        the TCP port to bind
         * @return the webhook sub-stage
         * @throws NullPointerException if {@code verifyToken} is {@code null}
         */
        public Webhook webhook(String verifyToken, int port) {
            Objects.requireNonNull(verifyToken, "verifyToken must not be null");
            store.setWebhookVerifyToken(verifyToken);
            store.setWebhookPort(port);
            store.setWebhookPath(DEFAULT_WEBHOOK_PATH);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "configuring webhook receiver on port {0}", port);
            return new Webhook(store);
        }

        /**
         * Persists the configured store and builds the Cloud client.
         *
         * @return the configured client
         * @throws IllegalArgumentException if a SOCKS proxy was supplied
         */
        public CloudWhatsAppClient build() {
            store.save();
            var resolvedHttpClient = httpClient != null ? httpClient : buildHttpClient();
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "assembled cloud whatsapp client");
            return new LiveCloudWhatsAppClient(store, resolvedHttpClient);
        }

        /**
         * Builds the default HTTP client, applying the configured proxy.
         *
         * @return the HTTP client
         * @throws IllegalArgumentException if a SOCKS proxy was supplied
         */
        private HttpClient buildHttpClient() {
            if (proxy == null) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building http client with no proxy");
                return HttpClient.newHttpClient();
            }
            if (proxy instanceof WhatsAppClientProxy.Socks) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "socks proxy rejected for the cloud transport");
                throw new IllegalArgumentException("The Cloud transport supports only HTTP proxies");
            }
            var builder = HttpClient.newBuilder()
                    .proxy(ProxySelector.of(new InetSocketAddress(proxy.host(), proxy.port())));
            if (proxy.authenticator().orElse(null) instanceof WhatsAppClientProxyAuthenticator.Http.Basic basic) {
                builder.authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(basic.username(), basic.password().toCharArray());
                    }
                });
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building http client with proxy {0}:{1}", proxy.host(), proxy.port());
            return builder.build();
        }
    }

    /**
     * The webhook-receiver builder sub-stage.
     *
     * <p>Reached through {@link Options#webhook(String, int)}; refines the receiver with an optional bind
     * address and URL path, then produces the client with the inherited {@link Options#build()}.
     */
    public static final class Webhook extends Options {
        /**
         * Constructs the sub-stage around the shared store.
         *
         * @param store the store resolved by the previous stage
         */
        Webhook(CloudWhatsAppStore store) {
            super(store);
        }

        /**
         * Sets the webhook bind address.
         *
         * @param bindAddress the bind address, or {@code null} to bind the wildcard address
         * @return this builder, for chaining
         */
        public Webhook bindAddress(String bindAddress) {
            store.setWebhookBindAddress(bindAddress);
            return this;
        }

        /**
         * Sets the webhook URL path.
         *
         * @param path the URL path, or {@code null} to keep the default {@code /webhook}
         * @return this builder, for chaining
         */
        public Webhook path(String path) {
            if (path != null) {
                store.setWebhookPath(path);
            }
            return this;
        }
    }
}
