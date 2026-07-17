package com.github.auties00.cobalt.client.linked;
import com.github.auties00.cobalt.client.WhatsAppClientProxy;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.registration.MobileClientRegistration;
import com.github.auties00.cobalt.wire.linked.business.profile.BusinessCategory;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatus;
import com.github.auties00.cobalt.wire.linked.contact.ContactTextStatusBuilder;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPayload;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A fluent builder that constructs {@link LinkedWhatsAppClient} instances.
 *
 * <p>The builder exposes three entry points via
 * {@link #webClient()}, {@link #mobileClient()}, and
 * {@link #customClient()}. Each entry point returns a specialised
 * sub-builder that guides the caller through the steps needed for that
 * client flavour: selecting a store factory, loading or creating a
 * connection, providing a verification handler, and finally producing the
 * {@link LinkedWhatsAppClient}. The specialisations use a sealed class
 * hierarchy so each step exposes only the parameters that apply to it,
 * keeping the surface type-safe.
 *
 * @see LinkedWhatsAppClient
 */

public sealed class LinkedWhatsAppClientBuilder {
    /**
     * The logger for {@link LinkedWhatsAppClientBuilder}.
     */
    private static final System.Logger LOGGER = Log.get(LinkedWhatsAppClientBuilder.class);
    /**
     * The default error handler: prints stack traces to stderr.
     */
    private static final WhatsAppLinkedClientErrorHandler DEFAULT_ERROR_HANDLER = WhatsAppLinkedClientErrorHandler.toTerminal();
    /**
     * The default web verification handler: renders the QR code in the
     * terminal.
     */
    private static final LinkedWhatsAppClientVerificationHandler.Web DEFAULT_WEB_VERIFICATION_HANDLER = LinkedWhatsAppClientVerificationHandler.Web.QrCode.toTerminal();

    /**
     * Package-private constructor; obtain instances via
     * {@link LinkedWhatsAppClient#builder()}.
     */
    LinkedWhatsAppClientBuilder() {

    }

    /**
     * Returns a web client builder backed by a persistent store.
     *
     * <p>The resulting builder follows the web companion linking flow,
     * which authenticates against an existing primary device via QR code
     * or pairing code.
     *
     * @return the web client builder
     */
    public Client.Web webClient() {
        return new Client.Web(LinkedWhatsAppStoreFactory.persistent());
    }

    /**
     * Returns a web client builder backed by the given store factory.
     *
     * @param factory the factory to use for data persistence
     * @return the web client builder
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public Client.Web webClient(LinkedWhatsAppStoreFactory factory) {
        Objects.requireNonNull(factory, "factory must not be null");
        return new Client.Web(factory);
    }

    /**
     * Returns a mobile client builder backed by a persistent store.
     *
     * <p>The resulting builder follows the mobile registration flow,
     * which registers a phone number directly with the WhatsApp servers.
     *
     * @return the mobile client builder
     */
    public Client.Mobile mobileClient() {
        return new Client.Mobile(LinkedWhatsAppStoreFactory.persistent());
    }

    /**
     * Returns a mobile client builder backed by the given store factory.
     *
     * @param factory the factory to use for data persistence
     * @return the mobile client builder
     * @throws NullPointerException if {@code factory} is {@code null}
     */
    public Client.Mobile mobileClient(LinkedWhatsAppStoreFactory factory) {
        Objects.requireNonNull(factory, "factory must not be null");
        return new Client.Mobile(factory);
    }

    /**
     * Returns a low-level builder that bypasses the
     * {@link LinkedWhatsAppStoreFactory} flow and accepts a pre-built
     * {@link LinkedWhatsAppStore} directly.
     *
     * @return the custom client builder
     */
    public Custom customClient() {
        return new Custom();
    }

    /**
     * A builder stage that selects an existing persisted session or
     * provisions a new one, backed by a {@link LinkedWhatsAppStoreFactory}.
     *
     * <p>Sub-types {@link Web} and {@link Mobile} specialise the behaviour
     * to the respective client flavour. Every {@code loadXxx} method
     * offers a parallel {@code loadOrCreateXxx} variant that falls back
     * to provisioning a fresh store when the lookup is not satisfied.
     */
    public static abstract sealed class Client extends LinkedWhatsAppClientBuilder {
        /**
         * The store factory that loads or creates the session on disk.
         */
        final LinkedWhatsAppStoreFactory factory;

        /**
         * Constructs a new {@code Client} stage backed by the given
         * factory.
         *
         * @param factory the store factory; must not be {@code null}
         * @throws NullPointerException if {@code factory} is {@code null}
         */
        private Client(LinkedWhatsAppStoreFactory factory) {
            this.factory = Objects.requireNonNull(factory, "factory must not be null");
        }

        /**
         * Creates a fresh connection identified by a random UUID.
         *
         * @return the next builder stage configured with a brand-new
         *         store
         * @throws IOException if the store cannot be created on disk
         */
        public abstract Options createConnection() throws IOException;

        /**
         * Creates a connection from a six-parts credentials representation.
         *
         * @param sixParts the credentials to load
         * @return the next builder stage
         * @throws NullPointerException if {@code sixParts} is {@code null}
         * @throws IOException if the store cannot be created on disk
         */
        public abstract Options createConnection(LinkedWhatsAppClientSixPartsKeys sixParts) throws IOException;

        /**
         * Loads the most recently serialised connection.
         *
         * @return the next builder stage if a previous connection exists,
         *         empty otherwise
         * @throws IOException if the store cannot be read from disk
         */
        public abstract Optional<Options> loadLatestConnection() throws IOException;

        /**
         * Loads the most recently serialised connection, or provisions a
         * fresh one if none exists yet.
         *
         * @return the next builder stage
         * @throws IOException if the store cannot be read from or written
         *                     to disk
         */
        public abstract Options loadLatestOrCreateConnection() throws IOException;

        /**
         * Loads the connection whose identifier matches {@code uuid}.
         *
         * @param uuid the identifier of the connection to load, or
         *             {@code null} to skip the lookup
         * @return the next builder stage if a matching store was found,
         *         empty otherwise
         * @throws IOException if the store cannot be read from disk
         */
        public abstract Optional<? extends Options> loadConnection(UUID uuid) throws IOException;

        /**
         * Loads the connection whose identifier matches {@code uuid}, or
         * provisions a fresh one if none exists yet.
         *
         * @param uuid the identifier of the connection to load, or
         *             {@code null} to provision under a fresh random UUID
         * @return the next builder stage
         * @throws IOException if the store cannot be read from or written
         *                     to disk
         */
        public abstract Options loadOrCreateConnection(UUID uuid) throws IOException;

        /**
         * Loads the connection whose phone number matches
         * {@code phoneNumber}.
         *
         * @param phoneNumber the phone number associated with the
         *                    connection, or {@code null} to skip the
         *                    lookup
         * @return the next builder stage if a matching store was found,
         *         empty otherwise
         * @throws IOException if the store cannot be read from disk
         */
        public abstract Optional<? extends Options> loadConnection(Long phoneNumber) throws IOException;

        /**
         * Loads the connection whose phone number matches
         * {@code phoneNumber}, or provisions a fresh one if none exists
         * yet.
         *
         * @param phoneNumber the phone number to load, or {@code null} to
         *                    provision under a fresh random UUID
         * @return the next builder stage
         * @throws IOException if the store cannot be read from or written
         *                     to disk
         */
        public abstract Options loadOrCreateConnection(Long phoneNumber) throws IOException;

        /**
         * The {@link LinkedWhatsAppClientType#WEB} specialisation of the
         * {@code Client} stage.
         *
         * <p>Produces {@link Options.Web} instances whose store is tagged
         * as a web companion and whose subsequent verification flow
         * accepts QR codes or pairing codes.
         */
        public static final class Web extends Client {
            /**
             * Package-private constructor used by
             * {@link LinkedWhatsAppClientBuilder#webClient()}.
             *
             * @param factory the store factory for the web client
             */
            private Web(LinkedWhatsAppStoreFactory factory) {
                super(factory);
            }
            
            @Override
            public Options.Web createConnection() throws IOException {
                return loadOrCreateConnection(UUID.randomUUID());
            }
            
            @Override
            public Options.Web loadLatestOrCreateConnection() throws IOException {
                var existingStore = factory.loadLatest(LinkedWhatsAppClientType.WEB);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded latest web connection from store");
                    return new Options.Web(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no existing web connection found, creating new store");
                var newStore = factory.create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
                return new Options.Web(newStore);
            }

            @Override
            public Optional<Options> loadConnection(UUID uuid) throws IOException {
                if (uuid == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadConnection(uuid) called with null uuid, skipping lookup");
                    return Optional.empty();
                }

                var store = factory.loadLatest(LinkedWhatsAppClientType.WEB);
                if (store.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no web connection found for uuid {0}", uuid);
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded web connection for uuid {0}", uuid);
                var result = new Options.Web(store.get());
                return Optional.of(result);
            }

            @Override
            public Options.Web loadOrCreateConnection(UUID uuid) throws IOException {
                if (uuid == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadOrCreateConnection(uuid) called with null uuid, creating new store");
                    var store = factory.create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
                    return new Options.Web(store);
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.WEB, uuid);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing web connection for uuid {0}", uuid);
                    return new Options.Web(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no web connection found for uuid {0}, creating new store", uuid);
                var newStore = factory.create(LinkedWhatsAppClientType.WEB, uuid);
                return new Options.Web(newStore);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) throws IOException {
                if (phoneNumber == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadConnection(phoneNumber) called with null phoneNumber, skipping lookup");
                    return Optional.empty();
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.WEB, phoneNumber);
                if (existingStore.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no web connection found for phone {0}", new LogRedactable.Phone(phoneNumber));
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded web connection for phone {0}", new LogRedactable.Phone(phoneNumber));
                var result = new Options.Web(existingStore.get());
                return Optional.of(result);
            }

            @Override
            public Options.Web loadOrCreateConnection(Long phoneNumber) throws IOException {
                if (phoneNumber == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadOrCreateConnection(phoneNumber) called with null phoneNumber, creating new store");
                    var store = factory.create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
                    return new Options.Web(store);
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.WEB, phoneNumber);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing web connection for phone {0}", new LogRedactable.Phone(phoneNumber));
                    return new Options.Web(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no web connection found for phone {0}, creating new store", new LogRedactable.Phone(phoneNumber));
                var newStore = factory.create(LinkedWhatsAppClientType.WEB, phoneNumber);
                return new Options.Web(newStore);
            }

            @Override
            public Options.Web createConnection(LinkedWhatsAppClientSixPartsKeys sixParts) throws IOException {
                Objects.requireNonNull(sixParts, "sixParts must not be null");
                var existingStore = factory.load(LinkedWhatsAppClientType.WEB, sixParts.phoneNumber());
                if(existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing web connection for phone {0} from six-parts keys", new LogRedactable.Phone(sixParts.phoneNumber()));
                    return new Options.Web(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating new web connection for phone {0} from six-parts keys", new LogRedactable.Phone(sixParts.phoneNumber()));
                var freshStore = factory.create(LinkedWhatsAppClientType.WEB, sixParts);
                return new Options.Web(freshStore);
            }

            @Override
            public Optional<Options> loadLatestConnection() throws IOException {
                var store = factory.loadLatest(LinkedWhatsAppClientType.WEB);
                if (store.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no latest web connection found");
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded latest web connection");
                var result = new Options.Web(store.get());
                return Optional.of(result);
            }
        }

        /**
         * The {@link LinkedWhatsAppClientType#MOBILE} specialisation of the
         * {@code Client} stage.
         *
         * <p>Produces {@link Options.Mobile} instances whose store is
         * tagged as a primary mobile device; the subsequent step lets the
         * caller register a phone number via
         * {@link Options.Mobile#register(long, LinkedWhatsAppClientVerificationHandler.Mobile)}.
         */
        public static final class Mobile extends Client {
            /**
             * Package-private constructor used by
             * {@link LinkedWhatsAppClientBuilder#mobileClient()}.
             *
             * @param factory the store factory for the mobile client
             */
            private Mobile(LinkedWhatsAppStoreFactory factory) {
                super(factory);
            }

            @Override
            public Options.Mobile createConnection() throws IOException {
                return loadOrCreateConnection(UUID.randomUUID());
            }

            @Override
            public Options.Mobile loadLatestOrCreateConnection() throws IOException {
                var existingStore = factory.loadLatest(LinkedWhatsAppClientType.MOBILE);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded latest mobile connection from store");
                    return new Options.Mobile(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no existing mobile connection found, creating new store");
                var newStore = factory.create(LinkedWhatsAppClientType.MOBILE, UUID.randomUUID());
                return new Options.Mobile(newStore);
            }

            @Override
            public Optional<Options> loadConnection(UUID uuid) throws IOException {
                if (uuid == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadConnection(uuid) called with null uuid, skipping lookup");
                    return Optional.empty();
                }

                var store = factory.loadLatest(LinkedWhatsAppClientType.MOBILE);
                if (store.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no mobile connection found for uuid {0}", uuid);
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded mobile connection for uuid {0}", uuid);
                var result = new Options.Mobile(store.get());
                return Optional.of(result);
            }

            @Override
            public Options.Mobile loadOrCreateConnection(UUID uuid) throws IOException {
                if (uuid == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadOrCreateConnection(uuid) called with null uuid, creating new store");
                    var store = factory.create(LinkedWhatsAppClientType.MOBILE, UUID.randomUUID());
                    return new Options.Mobile(store);
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.MOBILE, uuid);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing mobile connection for uuid {0}", uuid);
                    return new Options.Mobile(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no mobile connection found for uuid {0}, creating new store", uuid);
                var newStore = factory.create(LinkedWhatsAppClientType.MOBILE, uuid);
                return new Options.Mobile(newStore);
            }

            @Override
            public Optional<Options> loadConnection(Long phoneNumber) throws IOException {
                if (phoneNumber == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadConnection(phoneNumber) called with null phoneNumber, skipping lookup");
                    return Optional.empty();
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.MOBILE, phoneNumber);
                if (existingStore.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no mobile connection found for phone {0}", new LogRedactable.Phone(phoneNumber));
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded mobile connection for phone {0}", new LogRedactable.Phone(phoneNumber));
                var result = new Options.Mobile(existingStore.get());
                return Optional.of(result);
            }

            @Override
            public Options.Mobile loadOrCreateConnection(Long phoneNumber) throws IOException {
                if (phoneNumber == null) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loadOrCreateConnection(phoneNumber) called with null phoneNumber, creating new store");
                    var store = factory.create(LinkedWhatsAppClientType.MOBILE, UUID.randomUUID());
                    return new Options.Mobile(store);
                }

                var existingStore = factory.load(LinkedWhatsAppClientType.MOBILE, phoneNumber);
                if (existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing mobile connection for phone {0}", new LogRedactable.Phone(phoneNumber));
                    return new Options.Mobile(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no mobile connection found for phone {0}, creating new store", new LogRedactable.Phone(phoneNumber));
                var newStore = factory.create(LinkedWhatsAppClientType.MOBILE, phoneNumber);
                return new Options.Mobile(newStore);
            }

            @Override
            public Options.Mobile createConnection(LinkedWhatsAppClientSixPartsKeys sixParts) throws IOException {
                Objects.requireNonNull(sixParts, "sixParts must not be null");
                var existingStore = factory.load(LinkedWhatsAppClientType.WEB, sixParts.phoneNumber());
                if(existingStore.isPresent()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded existing mobile connection for phone {0} from six-parts keys", new LogRedactable.Phone(sixParts.phoneNumber()));
                    return new Options.Mobile(existingStore.get());
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "creating new mobile connection for phone {0} from six-parts keys", new LogRedactable.Phone(sixParts.phoneNumber()));
                var freshStore = factory.create(LinkedWhatsAppClientType.WEB, sixParts);
                return new Options.Mobile(freshStore);
            }

            @Override
            public Optional<Options> loadLatestConnection() throws IOException {
                var store = factory.loadLatest(LinkedWhatsAppClientType.MOBILE);
                if (store.isEmpty()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "no latest mobile connection found");
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "loaded latest mobile connection");
                var result = new Options.Mobile(store.get());
                return Optional.of(result);
            }
        }
    }

    /**
     * A builder stage that applies session-wide options to a resolved
     * {@link LinkedWhatsAppStore} before the client is materialised.
     *
     * <p>Every fluent setter on this stage writes directly into the
     * underlying store (for things that must be persisted, such as the
     * proxy configuration, device profile, and client version) or into
     * local fields (for handlers, which are not serialised). Concrete
     * {@link Web} and {@link Mobile} specialisations add verification and
     * business-profile options that are only meaningful for their
     * respective flavours.
     */
    public static sealed class Options extends LinkedWhatsAppClientBuilder {
        /**
         * The resolved store on which configuration writes are applied.
         */
        final LinkedWhatsAppStore store;
        /**
         * The error handler installed on the future client.
         */
        WhatsAppLinkedClientErrorHandler errorHandler;

        /**
         * Package-private constructor used by the {@link Client}
         * sub-builder once the store has been resolved.
         *
         * @param store the store to configure; must not be {@code null}
         * @throws NullPointerException if {@code store} is {@code null}
         */
        private Options(LinkedWhatsAppStore store) {
            this.store = Objects.requireNonNull(store, "store must not be null");
        }

        /**
         * Sets the display name advertised by this session.
         *
         * <p>On mobile this is the preferred name that contacts who have
         * not saved the user yet see next to the phone number. On web
         * this is the companion-device name visible in the
         * "Linked Devices" tab.
         *
         * @param name the name to set, or {@code null} to clear it
         * @return this builder, for chaining
         */
        public Options name(String name) {
            store.accountStore().setName(name);
            return this;
        }

        /**
         * Sets the proxy used by the connection.
         *
         * @param proxy the proxy, or {@code null} to use no proxy
         * @return this builder, for chaining
         */
        public Options proxy(WhatsAppClientProxy proxy) {
            store.connectionStore().setProxy(proxy);
            return this;
        }

        /**
         * Sets the device descriptor advertised by the connection.
         *
         * @param device the device, or {@code null} to clear it
         * @return this builder, for chaining
         */
        public Options device(LinkedWhatsAppClientDevice device) {
            store.accountStore().setDevice(device);
            return this;
        }

        /**
         * Sets the WhatsApp client version advertised by the connection.
         *
         * @param clientVersion the client version, or {@code null} to
         *                      keep the default
         * @return this builder, for chaining
         */
        public Options clientVersion(ClientAppVersion clientVersion) {
            store.accountStore().setClientVersion(clientVersion);
            return this;
        }

        /**
         * Sets the release channel advertised by the connection in the
         * handshake client payload.
         *
         * <p>The server resolves per-session feature exposure partly from this
         * channel: {@link ClientPayload.ClientReleaseChannel#BETA} opts the
         * connection into the public beta cohort, whereas the default
         * {@link ClientPayload.ClientReleaseChannel#RELEASE} only sees
         * generally available features. The mobile handshake advertises this
         * stored value directly; the {@link Web} flavour overrides this setter
         * to also toggle the Web/Desktop beta-program enrolment, keeping the
         * stored channel and the enrolment flag in sync.
         *
         * @param releaseChannel the release channel, or {@code null} to keep
         *                       the default {@link ClientPayload.ClientReleaseChannel#RELEASE}
         * @return this builder, for chaining
         */
        public Options releaseChannel(ClientPayload.ClientReleaseChannel releaseChannel) {
            if (releaseChannel != null) {
                store.accountStore().setReleaseChannel(releaseChannel);
            }
            return this;
        }

        /**
         * Sets the error handler that decides how the future client
         * reacts to failures.
         *
         * @param errorHandler the error handler, or {@code null} to use
         *                     the default terminal-printing handler
         * @return this builder, for chaining
         */
        public Options errorHandler(WhatsAppLinkedClientErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * The {@link LinkedWhatsAppClientType#WEB} specialisation of the
         * {@code Options} stage.
         *
         * <p>Adds the {@link #fullHistory()}, {@link #defaultHistory()}, and
         * {@link #discardHistory()} history options (the first two with
         * configurator overloads) and the terminal step methods
         * {@link #unregistered(LinkedWhatsAppClientVerificationHandler.Web.QrCode)},
         * {@link #unregistered(long, LinkedWhatsAppClientVerificationHandler.Web.PairingCode)},
         * and {@link #registered()} that materialise the client.
         */
        public static final class Web extends Options {
            /**
             * Package-private constructor used by {@link Client.Web}.
             *
             * @param store the store resolved by the previous stage
             */
            private Web(LinkedWhatsAppStore store) {
                super(store);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Mobile name(String name) {
                return (Mobile) super.name(name);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Web proxy(WhatsAppClientProxy proxy) {
                return (Web) super.proxy(proxy);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Web device(LinkedWhatsAppClientDevice device) {
                return (Web) super.device(device);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Web errorHandler(WhatsAppLinkedClientErrorHandler errorHandler) {
                return (Web) super.errorHandler(errorHandler);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Web clientVersion(ClientAppVersion clientVersion) {
                return (Web) super.clientVersion(clientVersion);
            }

            /**
             * Declares the Web/Desktop beta-program enrolment advertised in
             * the handshake.
             *
             * <p>Passing {@link ClientPayload.ClientReleaseChannel#BETA} opts
             * this session into the external web beta program; any other value
             * opts out. The enrolment flag and the stored release channel are
             * kept in sync, so the handshake advertises the beta channel while
             * enrolled. Once a session is enrolled (here, through
             * {@link LinkedWhatsAppClient#enableWebBetaEnrollment()}, or via a
             * synced {@code external_web_beta} mutation) it starts on the beta
             * channel automatically, without setting this again.
             *
             * @apiNote
             * This declares enrolment locally only; it does not push the
             * enrolment to the server or to other linked devices. Use
             * {@link LinkedWhatsAppClient#enableWebBetaEnrollment()} on a
             * connected client to enrol and fan the change out.
             *
             * @param releaseChannel the release channel; {@code null} keeps the
             *                       current enrolment
             * @return this builder, for chaining
             */
            @Override
            public Web releaseChannel(ClientPayload.ClientReleaseChannel releaseChannel) {
                if (releaseChannel != null) {
                    var beta = releaseChannel == ClientPayload.ClientReleaseChannel.BETA;
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "setting web release channel, beta={0}", beta);
                    store.syncStore().setExternalWebBeta(beta);
                    store.accountStore().setReleaseChannel(beta ? ClientPayload.ClientReleaseChannel.BETA : ClientPayload.ClientReleaseChannel.RELEASE);
                }
                return this;
            }

            /**
             * Configures the companion to advertise a full history sync over
             * WhatsApp's maximum 365-day window during pairing.
             *
             * <p>The replay can span up to a year of messages, so memory and
             * bandwidth use is higher than {@link #defaultHistory()}.
             *
             * @apiNote
             * Prefer this when the integration needs the largest history
             * WhatsApp Web itself requests; use {@link #fullHistory(Consumer)}
             * to choose a different day window.
             *
             * @return this builder, for chaining
             */
            public Web fullHistory() {
                return fullHistory(history -> {});
            }

            /**
             * Configures the companion to advertise a full history sync,
             * customised through the supplied configurator.
             *
             * <p>The {@link WhatsAppWebHistoryConfigurator.Full} configurator
             * exposes the full-sync day window and the newsletter toggle.
             *
             * @param configurator the full-sync configurator
             * @return this builder, for chaining
             * @throws NullPointerException if {@code configurator} is
             *                              {@code null}
             */
            public Web fullHistory(Consumer<WhatsAppWebHistoryConfigurator.Full> configurator) {
                Objects.requireNonNull(configurator, "configurator must not be null");
                var history = new WhatsAppWebHistoryConfigurator.Full();
                configurator.accept(history);
                applyHistory(true, false, history.newsletters, history.days, null, null, null, null);
                return this;
            }

            /**
             * Configures the companion to advertise the standard
             * (production-default) history sync during pairing.
             *
             * <p>This is also the behaviour applied when no history option is
             * set: the primary ships its default recent window, bounded by the
             * user's per-device setting on the phone.
             *
             * @apiNote
             * The recommended option for most integrations; use
             * {@link #defaultHistory(Consumer)} to tune the storage quota or
             * windowing caps.
             *
             * @return this builder, for chaining
             */
            public Web defaultHistory() {
                return defaultHistory(history -> {});
            }

            /**
             * Configures the companion to advertise the standard history sync,
             * customised through the supplied configurator.
             *
             * <p>The {@link WhatsAppWebHistoryConfigurator.Default}
             * configurator exposes the storage quota, the recent and thumbnail
             * windows, the per-chat message cap, and the newsletter toggle.
             *
             * @param configurator the standard-sync configurator
             * @return this builder, for chaining
             * @throws NullPointerException if {@code configurator} is
             *                              {@code null}
             */
            public Web defaultHistory(Consumer<WhatsAppWebHistoryConfigurator.Default> configurator) {
                Objects.requireNonNull(configurator, "configurator must not be null");
                var history = new WhatsAppWebHistoryConfigurator.Default();
                configurator.accept(history);
                applyHistory(false, false, history.newsletters, null, history.storageQuotaMb, history.recentSyncDays, history.thumbnailSyncDays, history.maxMessagesPerChat);
                return this;
            }

            /**
             * Configures the companion to discard chat history on receipt,
             * keeping only the addressing, cryptographic, and contact-naming
             * data Cobalt needs.
             *
             * <p>The wire request matches {@link #defaultHistory()}; WhatsApp
             * has no zero-history wire mode, so the trim is applied after
             * decoding.
             *
             * @apiNote
             * Use this for real-time-only integrations that do not need past
             * chat history.
             *
             * @return this builder, for chaining
             */
            public Web discardHistory() {
                applyHistory(false, true, true, null, null, null, null, null);
                return this;
            }

            /**
             * Writes the resolved history settings into the
             * {@link com.github.auties00.cobalt.store.linked.LinkedWhatsAppSyncStore},
             * reached by the history step methods.
             *
             * @param requireFullSync    whether to request a full sync
             * @param discard            whether to trim the decoded payload
             * @param newsletters        whether to bootstrap newsletters
             * @param fullSyncDays       the full-sync day window, or {@code null}
             * @param storageQuotaMb     the storage quota in megabytes, or {@code null}
             * @param recentSyncDays     the recent-sync day window, or {@code null}
             * @param thumbnailSyncDays  the thumbnail-sync day window, or {@code null}
             * @param maxMessagesPerChat the per-chat message cap, or {@code null}
             */
            private void applyHistory(boolean requireFullSync, boolean discard, boolean newsletters, Integer fullSyncDays, Integer storageQuotaMb, Integer recentSyncDays, Integer thumbnailSyncDays, Integer maxMessagesPerChat) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "applying history sync options: fullSync={0} discard={1} newsletters={2}", requireFullSync, discard, newsletters);
                var sync = store.syncStore();
                sync.setFullHistorySyncRequired(requireFullSync);
                sync.setHistoryDiscarded(discard);
                sync.setHistoryNewsletters(newsletters);
                sync.setHistoryFullSyncDays(fullSyncDays);
                sync.setHistoryStorageQuotaMb(storageQuotaMb);
                sync.setHistoryRecentSyncDays(recentSyncDays);
                sync.setHistoryThumbnailSyncDays(thumbnailSyncDays);
                sync.setHistoryMaxMessagesPerChat(maxMessagesPerChat);
            }

            /**
             * Builds a web client whose linking ceremony surfaces a QR
             * code through the supplied handler.
             *
             * @param qrHandler the QR code handler
             * @return the configured client
             * @throws NullPointerException if {@code qrHandler} is
             *                              {@code null}
             */
            public LinkedWhatsAppClient unregistered(LinkedWhatsAppClientVerificationHandler.Web.QrCode qrHandler) {
                Objects.requireNonNull(qrHandler, "qrHandler must not be null");
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building unregistered web client with qr code verification");
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new LiveLinkedWhatsAppClient(store, qrHandler, errorHandler);
            }

            /**
             * Builds a web client whose linking ceremony authenticates with a passkey and surfaces a
             * verification code through the supplied handler.
             *
             * @apiNote
             * The passkey itself is asserted by the
             * {@link LinkedWhatsAppClientVerificationHandler.Web#passkeyAuthenticator()
             * authenticator the handler carries}; the handler also presents and confirms the resulting
             * verification code.
             *
             * @param passkeyHandler the passkey verification-code handler
             * @return the configured client
             * @throws NullPointerException if {@code passkeyHandler} is {@code null}
             */
            public LinkedWhatsAppClient unregistered(LinkedWhatsAppClientVerificationHandler.Web.Passkey passkeyHandler) {
                Objects.requireNonNull(passkeyHandler, "passkeyHandler must not be null");
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building unregistered web client with passkey verification");
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new LiveLinkedWhatsAppClient(store, passkeyHandler, errorHandler);
            }

            /**
             * Builds a web client whose linking ceremony surfaces a
             * pairing code through the supplied handler.
             *
             * @param phoneNumber        the phone number of the primary
             *                           account being linked
             * @param pairingCodeHandler the pairing-code handler
             * @return the configured client
             * @throws NullPointerException if {@code pairingCodeHandler}
             *                              is {@code null}
             */
            public LinkedWhatsAppClient unregistered(long phoneNumber, LinkedWhatsAppClientVerificationHandler.Web.PairingCode pairingCodeHandler) {
                Objects.requireNonNull(pairingCodeHandler, "pairingCodeHandler must not be null");
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building unregistered web client with pairing code verification for phone {0}", new LogRedactable.Phone(phoneNumber));
                store.accountStore().setPhoneNumber(phoneNumber);
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new LiveLinkedWhatsAppClient(store, pairingCodeHandler, errorHandler);
            }

            /**
             * Builds a web client for a session that has already been
             * registered, reusing the persisted credentials.
             *
             * @return the configured client if the underlying store is
             *         registered, empty otherwise
             */
            public Optional<LinkedWhatsAppClient> registered() {
                if (!store.accountStore().registered()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "web store is not registered, cannot build client");
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building web client from registered store");
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                var result = new LiveLinkedWhatsAppClient(store, null, errorHandler);
                return Optional.of(result);
            }
        }

        /**
         * The {@link LinkedWhatsAppClientType#MOBILE} specialisation of the
         * {@code Options} stage.
         *
         * <p>Adds mobile-only setters for the account "about" text and
         * business profile (address, geolocation, description, website,
         * email, category) plus the terminal step methods
         * {@link #register(long, LinkedWhatsAppClientVerificationHandler.Mobile)}
         * and {@link #registered()}.
         */
        public static final class Mobile extends Options {
            /**
             * Device attestor captured by {@link #deviceAttestor}. Holds
             * at most one of {@link LinkedWhatsAppClientDeviceAttestor.Android} or
             * {@link LinkedWhatsAppClientDeviceAttestor.Ios}; {@code null} means "no
             * attestor configured" and the registration falls back to
             * the concrete subclass's {@code EMPTY_ATTESTOR} default.
             */
            private LinkedWhatsAppClientDeviceAttestor attestor;

            /**
             * Push client captured by
             * {@link #devicePushClient(LinkedWhatsAppClientDevicePushClient)}, the
             * caller-owned variant. The builder treats this
             * instance as borrowed and never closes it. Mutually
             * exclusive with {@link #pushClientSupplier}: setting
             * either overload clears the other.
             *
             * <p>{@code null} (together with a {@code null}
             * {@link #pushClientSupplier}) means "no push client
             * configured" and the registration falls back to
             * {@link LinkedWhatsAppClientDevicePushClient#noop()}, which emits
             * empty {@code push_token} and {@code push_code} form
             * fields.
             */
            private LinkedWhatsAppClientDevicePushClient pushClient;

            /**
             * Push client supplier captured by
             * {@link #devicePushClient(Supplier)}, the
             * builder-owned variant. The supplier is invoked exactly
             * once at registration time and the produced instance is
             * closed via {@link LinkedWhatsAppClientDevicePushClient#close()}
             * after the registration ceremony finishes (success or
             * failure). Mutually exclusive with {@link #pushClient}:
             * setting either overload clears the other.
             */
            private Supplier<LinkedWhatsAppClientDevicePushClient> pushClientSupplier;

            /**
             * Tracks whether the caller has explicitly selected the
             * device via {@link #device(LinkedWhatsAppClientDevice)}, as opposed to
             * inheriting the default produced by the store factory.
             *
             * <p>{@link #deviceAttestor} consults this flag to decide
             * whether an immediate platform-mismatch check is
             * meaningful: when the device is still the factory default
             * the caller has not expressed an intent yet, so the
             * deferred check in the terminal methods covers it instead.
             */
            private boolean deviceExplicitlySet;

            /**
             * Package-private constructor used by {@link Client.Mobile}.
             *
             * @param store the store resolved by the previous stage
             */
            private Mobile(LinkedWhatsAppStore store) {
                super(store);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Mobile proxy(WhatsAppClientProxy proxy) {
                store.connectionStore().setProxy(proxy);
                return this;
            }

            /**
             * Sets the companion device for the connection.
             *
             * <p>If a device attestor has already been attached via
             * {@link #deviceAttestor}, the new device's platform is
             * validated against the attestor's sealed sub-type and the
             * call raises {@link IllegalArgumentException} on a
             * mismatch. If a push client has already been attached via
             * {@link #devicePushClient}, the new device's platform is
             * also validated against the client's
             * {@link LinkedWhatsAppClientDevicePushClient#supportedPlatforms()} set and the
             * call raises {@link IllegalArgumentException} on a
             * mismatch. The builder also flips an internal flag marking
             * the device as explicitly chosen so that a subsequent
             * {@code deviceAttestor} or {@code devicePushClient} call
             * can itself perform the symmetric check.
             *
             * @param device the companion device, can be null
             * @return the same instance for chaining
             * @throws IllegalArgumentException if an attestor or push
             *                                  client is already set
             *                                  and its platform does
             *                                  not match {@code device}
             */
            @Override
            public Mobile device(LinkedWhatsAppClientDevice device) {
                if (device != null) {
                    requirePlatformMatches(device, attestor);
                    requirePushClientSupportsDevice(device, pushClient);
                }
                store.accountStore().setDevice(device);
                this.deviceExplicitlySet = true;
                return this;
            }

            /**
             * Sets the device attestor that produces the Play Integrity
             * or App Attest payloads (and, on Android, the TEE-backed
             * body signature and the install source) embedded into the
             * upcoming registration requests.
             *
             * <p>If {@link #device(LinkedWhatsAppClientDevice)} has already been
             * called explicitly, the attestor's sealed sub-type is
             * validated against the stored device's platform and the
             * call raises {@link IllegalArgumentException} on a
             * mismatch. When the device is still the factory default
             * the check is deferred to the terminal methods
             * ({@link #register} and {@link #registered()}), which
             * catches the case of an attestor set against the defaulted
             * device.
             *
             * <p>Passing {@code null} clears the attestor and brings
             * the registration back to the low-trust lane (the concrete
             * registration subclass's private {@code EMPTY_ATTESTOR}
             * default).
             *
             * @param attestor the device attestor, or {@code null} to clear
             * @return the same instance for chaining
             * @throws IllegalArgumentException if {@code device(...)}
             *                                  was called explicitly
             *                                  and the stored device's
             *                                  platform does not match
             *                                  {@code attestor}
             */
            public Mobile deviceAttestor(LinkedWhatsAppClientDeviceAttestor attestor) {
                if (deviceExplicitlySet && attestor != null) {
                    requirePlatformMatches(store.accountStore().device(), attestor);
                }
                this.attestor = attestor;
                return this;
            }

            /**
             * Sets the push client that produces the {@code push_token}
             * advertised on every attested endpoint and the
             * {@code push_code} echoed back on {@code /v2/code} when a
             * push-based verification method is in flight.
             *
             * <p>The {@code pushClient} instance stays caller-owned:
             * the builder treats it as a borrowed reference and
             * never invokes {@link LinkedWhatsAppClientDevicePushClient#close()}
             * on it. If you want the builder to own the lifecycle
             * (for example because the client opens a long-lived TLS
             * connection that should be released as soon as the
             * registration ceremony finishes), use the
             * {@link #devicePushClient(Supplier) supplier-based
             * overload} instead.
             *
             * <p>If {@link #device(LinkedWhatsAppClientDevice)} has already been
             * called explicitly, the push client's
             * {@link LinkedWhatsAppClientDevicePushClient#supportedPlatforms()}
             * set is validated against the stored device's platform
             * and the call raises {@link IllegalArgumentException} on
             * a mismatch. When the device is still the factory
             * default the check is deferred to the terminal methods
             * ({@link #register} and {@link #registered()}), which
             * catches the case of a push client set against the
             * defaulted device.
             *
             * <p>Passing {@code null} clears the push client and
             * brings the registration back to the low-trust lane
             * ({@link LinkedWhatsAppClientDevicePushClient#noop()}, which emits
             * empty {@code push_token} and {@code push_code} fields).
             * Calling this overload also clears any supplier
             * previously installed via
             * {@link #devicePushClient(Supplier)}.
             *
             * @param pushClient the push client, or {@code null} to clear
             * @return the same instance for chaining
             * @throws IllegalArgumentException if {@code device(...)}
             *                                  was called explicitly
             *                                  and the stored device's
             *                                  platform is not in
             *                                  {@code pushClient.supportedPlatforms()}
             */
            public Mobile devicePushClient(LinkedWhatsAppClientDevicePushClient pushClient) {
                if (deviceExplicitlySet && pushClient != null) {
                    requirePushClientSupportsDevice(store.accountStore().device(), pushClient);
                }
                this.pushClient = pushClient;
                this.pushClientSupplier = null;
                return this;
            }

            /**
             * Sets a supplier of the push client that produces the
             * {@code push_token} and {@code push_code} form fields.
             *
             * <p>The supplier is invoked exactly once at registration
             * time, and the produced
             * {@link LinkedWhatsAppClientDevicePushClient} instance is owned by
             * the builder: it is closed via
             * {@link LinkedWhatsAppClientDevicePushClient#close()} after the
             * registration ceremony completes (success or failure).
             * Use this overload for clients that open vendor-side
             * resources (an FCM MCS stream, an APNS courier
             * connection) so they are torn down promptly. For
             * caller-managed clients, prefer
             * {@link #devicePushClient(LinkedWhatsAppClientDevicePushClient)}.
             *
             * <p>Because the supplier may carry side effects (opening
             * a network connection, generating a fresh device
             * identifier, etc.) it is not invoked here, which means
             * no immediate {@link LinkedWhatsAppClientDevicePushClient#supportedPlatforms()}
             * validation is possible. The terminal {@link #register}
             * resolves the supplier and validates the produced
             * client's supported platforms before driving the
             * registration; mismatches surface as
             * {@link IllegalArgumentException} at that point.
             *
             * <p>Passing {@code null} clears any supplier previously
             * installed and brings the registration back to the
             * low-trust lane ({@link LinkedWhatsAppClientDevicePushClient#noop()}).
             * Calling this overload also clears any caller-owned
             * client previously installed via
             * {@link #devicePushClient(LinkedWhatsAppClientDevicePushClient)}.
             *
             * @param pushClientSupplier the supplier, or {@code null}
             *                           to clear
             * @return the same instance for chaining
             */
            public Mobile devicePushClient(Supplier<LinkedWhatsAppClientDevicePushClient> pushClientSupplier) {
                this.pushClientSupplier = pushClientSupplier;
                this.pushClient = null;
                return this;
            }

            /**
             * Cross-checks the currently stored device against the
             * currently stored attestor and raises
             * {@link IllegalArgumentException} if they do not agree.
             *
             * <p>Called by the terminal {@link #register} and
             * {@link #registered()} methods so that a caller that set
             * an attestor without also picking a matching device (thus
             * relying on the factory default) still gets a clear error
             * at the point the mismatch matters.
             *
             * @throws IllegalArgumentException if the stored attestor's
             *                                  platform does not match
             *                                  the stored device's
             *                                  platform
             */
            private void validateAttestorMatchesDevice() {
                if (attestor != null) {
                    requirePlatformMatches(store.accountStore().device(), attestor);
                }
            }

            /**
             * Cross-checks the currently stored device against the
             * eager push client installed via
             * {@link #devicePushClient(LinkedWhatsAppClientDevicePushClient)} and
             * raises {@link IllegalArgumentException} if the device's
             * platform is not in
             * {@link LinkedWhatsAppClientDevicePushClient#supportedPlatforms()}.
             *
             * <p>Called by the terminal {@link #register} and
             * {@link #registered()} methods so that a caller that set
             * a push client without also picking a matching device
             * (thus relying on the factory default) still gets a clear
             * error at the point the mismatch matters.
             *
             * <p>The supplier-based variant installed via
             * {@link #devicePushClient(Supplier)} is not handled here:
             * its platform set is unknown until the supplier is
             * resolved, so {@link #register} performs the equivalent
             * check after invoking the supplier.
             *
             * @throws IllegalArgumentException if the stored push
             *                                  client does not list
             *                                  the stored device's
             *                                  platform in its
             *                                  supported set
             */
            private void validatePushClientMatchesDevice() {
                if (pushClient != null) {
                    requirePushClientSupportsDevice(store.accountStore().device(), pushClient);
                }
            }

            /**
             * Validates that the sub-interface of {@code attestor}
             * matches the platform carried by {@code device}.
             *
             * <p>A {@code null} attestor is always accepted, because
             * registration will fall back to the concrete subclass's
             * {@code EMPTY_ATTESTOR} default.
             *
             * @param device the device whose platform the attestor must
             *               match; never {@code null}
             * @param attestor the device attestor to validate, or
             *                 {@code null} to skip the check
             * @throws IllegalArgumentException if the attestor's
             *                                  platform does not match
             *                                  the device's platform
             */
            private static void requirePlatformMatches(LinkedWhatsAppClientDevice device, LinkedWhatsAppClientDeviceAttestor attestor) {
                if (attestor == null) {
                    return;
                }
                var platform = device.platform();
                switch (attestor) {
                    case LinkedWhatsAppClientDeviceAttestor.Android ignored -> {
                        if (platform != ClientPlatformType.ANDROID
                                && platform != ClientPlatformType.ANDROID_BUSINESS) {
                            if (Log.WARNING) LOGGER.log(Level.WARNING, "android attestor requires an android device, got platform {0}", platform);
                            throw new IllegalArgumentException(
                                    "Android attestor requires an Android device, got platform: " + platform);
                        }
                    }
                    case LinkedWhatsAppClientDeviceAttestor.Ios ignored -> {
                        if (platform != ClientPlatformType.IOS
                                && platform != ClientPlatformType.IOS_BUSINESS) {
                            if (Log.WARNING) LOGGER.log(Level.WARNING, "ios attestor requires an ios device, got platform {0}", platform);
                            throw new IllegalArgumentException(
                                    "iOS attestor requires an iOS device, got platform: " + platform);
                        }
                    }
                }
            }

            /**
             * Validates that {@code device}'s platform appears in
             * {@code pushClient.supportedPlatforms()}.
             *
             * <p>A {@code null} push client is always accepted, because
             * registration will fall back to
             * {@link LinkedWhatsAppClientDevicePushClient#noop()}, which accepts every
             * platform.
             *
             * @param device the device whose platform the push client
             *               must support; never {@code null}
             * @param pushClient the push client to validate, or
             *                   {@code null} to skip the check
             * @throws IllegalArgumentException if the push client does
             *                                  not list the device's
             *                                  platform in its
             *                                  supported set
             */
            private static void requirePushClientSupportsDevice(LinkedWhatsAppClientDevice device, LinkedWhatsAppClientDevicePushClient pushClient) {
                if (pushClient == null) {
                    return;
                }
                var platform = device.platform();
                var supported = pushClient.supportedPlatforms();
                if (!supported.contains(platform)) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "push client does not support device platform {0}", platform);
                    throw new IllegalArgumentException(
                            "Push client does not support device platform: " + platform
                                    + " (supported: " + supported + ")");
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Mobile errorHandler(WhatsAppLinkedClientErrorHandler errorHandler) {
                super.errorHandler(errorHandler);
                return this;
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Mobile clientVersion(ClientAppVersion clientVersion) {
                return (Mobile) super.clientVersion(clientVersion);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public Mobile name(String name) {
                return (Mobile) super.name(name);
            }

            /**
             * Sets the text status (the "about" line) attached to the
             * account.
             *
             * @param selfTextStatus the text status, or {@code null} to clear
             *                       it
             * @return this builder, for chaining
             */
            public Mobile selfTextStatus(ContactTextStatus selfTextStatus) {
                store.accountStore().setSelfTextStatus(selfTextStatus);
                return this;
            }

            /**
             * Convenience overload that sets the text status to a plain
             * string with no emoji or ephemeral expiration.
             *
             * @param aboutText the about text, or {@code null} to clear it
             * @return this builder, for chaining
             */
            public Mobile selfTextStatus(String aboutText) {
                var status = aboutText == null ? null : new ContactTextStatusBuilder()
                        .text(aboutText)
                        .build();
                return selfTextStatus(status);
            }

            /**
             * Sets the business address advertised on the account's
             * business profile.
             *
             * @param businessAddress the address, or {@code null} to
             *                        clear it
             * @return this builder, for chaining
             */
            public Mobile businessAddress(String businessAddress) {
                store.accountStore().setBusinessAddress(businessAddress);
                return this;
            }

            /**
             * Sets the longitude component of the business address
             * geolocation.
             *
             * @param businessLongitude the longitude, or {@code null} to
             *                          clear it
             * @return this builder, for chaining
             */
            public Mobile businessLongitude(Double businessLongitude) {
                store.accountStore().setBusinessLongitude(businessLongitude);
                return this;
            }

            /**
             * Sets the latitude component of the business address
             * geolocation.
             *
             * @param businessLatitude the latitude, or {@code null} to
             *                         clear it
             * @return this builder, for chaining
             */
            public Mobile businessLatitude(Double businessLatitude) {
                store.accountStore().setBusinessLatitude(businessLatitude);
                return this;
            }

            /**
             * Sets the business description shown on the account's
             * business profile.
             *
             * @param businessDescription the description, or {@code null}
             *                            to clear it
             * @return this builder, for chaining
             */
            public Mobile businessDescription(String businessDescription) {
                store.accountStore().setBusinessDescription(businessDescription);
                return this;
            }

            /**
             * Sets the business website URLs.
             *
             * @param businessWebsites the website URLs, or an empty list
             *                         to clear them
             * @return this builder, for chaining
             */
            public Mobile businessWebsites(List<URI> businessWebsites) {
                store.accountStore().setBusinessWebsites(businessWebsites);
                return this;
            }

            /**
             * Sets the business contact email address.
             *
             * @param businessEmail the email address, or {@code null} to
             *                      clear it
             * @return this builder, for chaining
             */
            public Mobile businessEmail(String businessEmail) {
                store.accountStore().setBusinessEmail(businessEmail);
                return this;
            }

            /**
             * Sets the business categories advertised on the business
             * profile.
             *
             * @param businessCategories the categories, or an empty list
             *                           to clear them
             * @return this builder, for chaining
             */
            public Mobile businessCategories(List<BusinessCategory> businessCategories) {
                store.accountStore().setBusinessCategories(businessCategories);
                return this;
            }

            /**
             * Builds a mobile client for a session that has already been
             * registered, reusing the persisted credentials.
             *
             * @return the configured client if the underlying store is
             *         registered, empty otherwise
             * @throws IllegalArgumentException if a previously attached
             *                                  attestor or push client
             *                                  does not match the
             *                                  configured device
             *                                  platform
             */
            public Optional<LinkedWhatsAppClient> registered() {
                validateAttestorMatchesDevice();
                validatePushClientMatchesDevice();
                if (!store.accountStore().registered()) {
                    if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mobile store is not registered, cannot build client");
                    return Optional.empty();
                }

                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building mobile client from registered store");
                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                var result = new LiveLinkedWhatsAppClient(store, null, errorHandler);
                return Optional.of(result);
            }

            /**
             * Builds a mobile client and runs the registration ceremony
             * for a session that has not yet been registered.
             *
             * @param phoneNumber  the phone number being registered
             * @param verification the verification handler used to drive
             *                     the OTP exchange
             * @return the configured client
             * @throws NullPointerException     if {@code verification} is
             *                                  {@code null}
             * @throws IllegalArgumentException if the store already
             *                                  carries a different phone
             *                                  number, or if a previously
             *                                  attached attestor or push
             *                                  client does not match the
             *                                  configured device platform
             */
            public LinkedWhatsAppClient register(long phoneNumber, LinkedWhatsAppClientVerificationHandler.Mobile verification) {
                Objects.requireNonNull(verification, "verification must not be null");
                validateAttestorMatchesDevice();
                validatePushClientMatchesDevice();

                var oldPhoneNumber = store.accountStore().phoneNumber();
                if(oldPhoneNumber.isPresent() && oldPhoneNumber.getAsLong() != phoneNumber) {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "phone number mismatch for mobile registration, requested {0}", new LogRedactable.Phone(phoneNumber));
                    throw new IllegalArgumentException("The phone number(" + phoneNumber + ") must match the existing phone number(" + oldPhoneNumber.getAsLong() + ")");
                }else {
                    store.accountStore().setPhoneNumber(phoneNumber);
                }

                if (!store.accountStore().registered()) {
                    if (Log.INFO) LOGGER.log(Level.INFO, "starting mobile registration for phone {0}", new LogRedactable.Phone(phoneNumber));
                    if (pushClientSupplier != null) {
                        try (var ownedPushClient = pushClientSupplier.get()) {
                            requirePushClientSupportsDevice(store.accountStore().device(), ownedPushClient);
                            try (var registration = MobileClientRegistration.newRegistration(store, verification, attestor, ownedPushClient)) {
                                registration.register();
                            }
                        }
                    } else {
                        try (var registration = MobileClientRegistration.newRegistration(store, verification, attestor, pushClient)) {
                            registration.register();
                        }
                    }
                    if (Log.INFO) LOGGER.log(Level.INFO, "mobile registration complete for phone {0}", new LogRedactable.Phone(phoneNumber));
                } else if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "mobile store already registered for phone {0}, skipping registration ceremony", new LogRedactable.Phone(phoneNumber));
                }

                var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
                return new LiveLinkedWhatsAppClient(store, null, errorHandler);
            }
        }
    }

    /**
     * A low-level builder stage that bypasses the
     * {@link LinkedWhatsAppStoreFactory} flow and lets the caller supply a
     * pre-built {@link LinkedWhatsAppStore}.
     *
     * <p>{@code Custom} is useful for test harnesses or for integrators
     * that already own a store (for example, one loaded from an external
     * database). The caller is responsible for ensuring the store's
     * {@link LinkedWhatsAppAccountStore#clientType()} matches the intended flavour and
     * that the keys stored inside it are consistent with any identifiers
     * passed elsewhere in the build chain.
     */
    public static final class Custom extends LinkedWhatsAppClientBuilder {
        /**
         * The externally-supplied store.
         */
        private LinkedWhatsAppStore store;
        /**
         * The error handler to install on the built client.
         */
        private WhatsAppLinkedClientErrorHandler errorHandler;
        /**
         * The web verification handler to install on the built client,
         * only honoured when the store's client type is
         * {@link LinkedWhatsAppClientType#WEB}.
         */
        private LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler;

        /**
         * Package-private constructor used by
         * {@link LinkedWhatsAppClientBuilder#customClient()}.
         */
        private Custom() {

        }

        /**
         * Sets the externally-supplied store backing the client.
         *
         * @param store the store, or {@code null} to leave it unset
         *              (which fails fast at {@link #build()})
         * @return this builder, for chaining
         */
        public Custom store(LinkedWhatsAppStore store) {
            this.store = store;
            return this;
        }

        /**
         * Sets the error handler that decides how the future client
         * reacts to failures.
         *
         * @param errorHandler the error handler, or {@code null} to use
         *                     the default terminal-printing handler
         * @return this builder, for chaining
         */
        public Custom errorHandler(WhatsAppLinkedClientErrorHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        /**
         * Sets the web verification handler used when the supplied store
         * is configured for {@link LinkedWhatsAppClientType#WEB}.
         *
         * @param webVerificationHandler the verification handler, or
         *                               {@code null} to use the default
         *                               terminal-rendering handler
         * @return this builder, for chaining
         */
        public Custom webVerificationSupport(LinkedWhatsAppClientVerificationHandler.Web webVerificationHandler) {
            this.webVerificationHandler = webVerificationHandler;
            return this;
        }

        /**
         * Builds the configured client.
         *
         * @return the configured client
         * @throws NullPointerException if no store has been supplied
         */
        public LinkedWhatsAppClient build() {
            var store = Objects.requireNonNull(this.store, "Expected a valid store");
            var webVerificationHandler = switch (store.accountStore().clientType()) {
                case WEB -> Objects.requireNonNullElse(this.webVerificationHandler, DEFAULT_WEB_VERIFICATION_HANDLER);
                case MOBILE -> null;
            };
            var errorHandler = Objects.requireNonNullElse(this.errorHandler, DEFAULT_ERROR_HANDLER);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "building custom linked client, type={0}", store.accountStore().clientType());
            return new LiveLinkedWhatsAppClient(store, webVerificationHandler, errorHandler);
        }
    }
}