package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.listener.WhatsAppListener;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.mixin.PathMixin;
import com.github.auties00.cobalt.store.linked.*;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.builtin.ProtobufLazyMixin;
import it.auties.protobuf.model.ProtobufType;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
 * The in-memory backbone shared by every {@link LinkedWhatsAppStore} persistence strategy.
 *
 * <p>This abstract aggregate composes the domain sub-stores ({@link LinkedWhatsAppSignalStore}, {@link LinkedWhatsAppAccountStore},
 * {@link LinkedWhatsAppContactStore}, {@link LinkedWhatsAppChatStore}, {@link LinkedWhatsAppSyncStore}, {@link LinkedWhatsAppSettingsStore},
 * {@link LinkedWhatsAppBusinessStore}, {@link LinkedWebSessionStore}, {@link LinkedWhatsAppConnectionStore}, {@link LinkedWhatsAppWamStore}) and
 * exposes them through accessors. It does not delegate domain operations; callers reach domain state
 * through the relevant sub-store. The facade itself owns only the registered listeners and the on-disk
 * directory shape.
 *
 * <p>The chat sub-store is the only domain whose backing differs per persistence strategy, so it is
 * supplied by the concrete subclass through {@link #chatStore()}; the other persisted sub-stores are
 * nested {@code MESSAGE} fields serialised inline with this aggregate ({@link LinkedWhatsAppBusinessStore} and
 * {@link LinkedWhatsAppConnectionStore} are transient and never serialised).
 *
 * @implSpec
 * Concrete subclasses provide the variant {@link #chatStore()} and the persistence lifecycle
 * ({@link #await()}, {@link #save()}, {@link #delete()}).
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
public abstract class ProtobufWhatsAppStore implements LinkedWhatsAppStore {
    /**
     * The logger for {@link ProtobufWhatsAppStore}, used by this class's static members.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufWhatsAppStore.class);

    /**
     * The time-to-live for a cached device-list entry before it is considered stale.
     *
     * @apiNote
     * Limits how long stale per-recipient device fan-out lists may be reused for outgoing message
     * sessions; one day matches the refresh cadence the companion fleet honours.
     */
    public static final Duration DEVICE_TTL = Duration.ofDays(1);

    /**
     * The Signal-protocol cryptographic sub-store.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWhatsAppSignalStore signalStore;

    /**
     * The account-identity and profile sub-store.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWhatsAppAccountStore accountStore;

    /**
     * The address-book and per-peer-device sub-store.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWhatsAppContactStore contactStore;

    /**
     * The app-state-sync and feature-flag sub-store.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWhatsAppSyncStore syncStore;

    /**
     * The user-preference and settings sub-store.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWhatsAppSettingsStore settingsStore;

    /**
     * The on-disk directory under which this store persists, or {@code null} for an in-memory store.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING, mixins = {PathMixin.class, ProtobufLazyMixin.class})
    private final Path directory;

    /**
     * The web-GraphQL credential sub-store holding the WhatsApp Web GraphQL session cookie/lsd and the Facebook GraphQL token.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.MESSAGE)
    private final ProtobufLinkedWebSessionStore webSessionStore;

    /**
     * The WhatsApp Business and payments sub-store; not persisted (all-transient state).
     */
    private final ProtobufLinkedWhatsAppBusinessStore businessStore;

    /**
     * The connection-runtime sub-store (proxy, offline-resume, routing); not persisted (all-transient state).
     */
    private final ProtobufLinkedWhatsAppConnectionStore connectionStore;

    /**
     * The concurrent set of registered event listeners; not persisted.
     */
    private final KeySetView<WhatsAppListener, Boolean> listeners;

    /**
     * The per-concrete-class logger instance for this store; not persisted.
     *
     * @implNote
     * This implementation resolves the logger from {@link #getClass()} rather than sharing
     * {@link #LOGGER}, so log lines from a concrete subclass (for example {@code PersistentStore})
     * carry that subclass's name rather than this abstract class's name.
     */
    protected final System.Logger logger;

    /**
     * Constructs the aggregate from its composed sub-stores and retained runtime state.
     *
     * @implNote
     * This implementation wires the account sub-store into the signal and contact sub-stores (for
     * self-address resolution), wires the contact sub-store into the chat sub-store (for
     * phone-number/LID chat resolution), and allocates the business and connection sub-stores. The chat
     * and WAM sub-stores are not retained here; each concrete subclass owns the variant-typed field
     * backing {@link #chatStore()} and {@link #wamStore()}. The chat sub-store is taken as an argument
     * only so this constructor can wire its contact reference; the WAM sub-store needs no super-side
     * wiring, so it is not passed here.
     *
     * @param signalStore     the signal sub-store, never {@code null}
     * @param accountStore    the account sub-store, never {@code null}
     * @param contactStore    the contact sub-store, never {@code null}
     * @param syncStore       the sync sub-store, never {@code null}
     * @param settingsStore   the settings sub-store, never {@code null}
     * @param directory       the session directory, or {@code null} for in-memory
     * @param webSessionStore the web-GraphQL credential sub-store, or {@code null} for an empty one
     * @param chatStore       the persistence-variant chat sub-store, never {@code null}
     */
    protected ProtobufWhatsAppStore(ProtobufLinkedWhatsAppSignalStore signalStore, ProtobufLinkedWhatsAppAccountStore accountStore, ProtobufLinkedWhatsAppContactStore contactStore, ProtobufLinkedWhatsAppSyncStore syncStore, ProtobufLinkedWhatsAppSettingsStore settingsStore, Path directory, ProtobufLinkedWebSessionStore webSessionStore, ProtobufLinkedWhatsAppChatStore chatStore) {
        this.signalStore = Objects.requireNonNull(signalStore, "signalStore cannot be null");
        this.accountStore = Objects.requireNonNull(accountStore, "accountStore cannot be null");
        this.contactStore = Objects.requireNonNull(contactStore, "contactStore cannot be null");
        this.syncStore = Objects.requireNonNull(syncStore, "syncStore cannot be null");
        this.settingsStore = Objects.requireNonNull(settingsStore, "settingsStore cannot be null");
        Objects.requireNonNull(chatStore, "chatStore cannot be null");
        this.directory = directory;
        this.webSessionStore = Objects.requireNonNullElseGet(webSessionStore, () -> new ProtobufLinkedWebSessionStore(null, null));
        this.businessStore = new ProtobufLinkedWhatsAppBusinessStore();
        this.connectionStore = new ProtobufLinkedWhatsAppConnectionStore();
        this.signalStore.setAccount(accountStore);
        this.contactStore.setAccount(accountStore);
        chatStore.setContacts(contactStore);
        this.listeners = ConcurrentHashMap.newKeySet();
        this.logger = Log.get(this.getClass());
    }

    @Override
    public ProtobufLinkedWhatsAppSignalStore signalStore() {
        return signalStore;
    }

    @Override
    public ProtobufLinkedWhatsAppAccountStore accountStore() {
        return accountStore;
    }

    @Override
    public ProtobufLinkedWhatsAppContactStore contactStore() {
        return contactStore;
    }

    @Override
    public ProtobufLinkedWhatsAppSyncStore syncStore() {
        return syncStore;
    }

    @Override
    public ProtobufLinkedWhatsAppSettingsStore settingsStore() {
        return settingsStore;
    }

    @Override
    public ProtobufLinkedWhatsAppBusinessStore businessStore() {
        return businessStore;
    }

    @Override
    public ProtobufLinkedWebSessionStore webSessionStore() {
        return webSessionStore;
    }

    @Override
    public ProtobufLinkedWhatsAppConnectionStore connectionStore() {
        return connectionStore;
    }

    @Override
    public abstract ProtobufLinkedWhatsAppWamStore wamStore();

    @Override
    public abstract ProtobufLinkedWhatsAppChatStore chatStore();

    /**
     * Returns the on-disk directory under which this store persists, or {@code null} if in-memory.
     *
     * @return the persistence directory, or {@code null}
     */
    public Path directory() {
        return directory;
    }

    @Override
    public WhatsAppListener addListener(WhatsAppListener listener) {
        listeners.add(listener);
        if (Log.DEBUG) logger.log(Level.DEBUG, "listener registered: {0}", listener.getClass().getSimpleName());
        return listener;
    }

    @Override
    public boolean removeListener(WhatsAppListener listener) {
        var removed = listeners.remove(listener);
        if (Log.DEBUG) {
            logger.log(Level.DEBUG, "listener removed: {0}, was registered={1}",
                    listener.getClass().getSimpleName(), removed);
        }
        return removed;
    }

    @Override
    public Collection<WhatsAppListener> listeners() {
        return List.copyOf(listeners);
    }

    /**
     * Resolves the home directory for {@code type} under {@code baseDirectory}, creating it if necessary.
     *
     * @param type          the client type
     * @param baseDirectory the base storage directory
     * @return the resolved home directory, guaranteed to exist
     * @throws IOException if the directory cannot be created
     */
    public static Path getHomeDirectory(LinkedWhatsAppClientType type, Path baseDirectory) throws IOException {
        var id = switch (type) {
            case WEB -> "web";
            case MOBILE -> "mobile";
        };
        var result = baseDirectory.resolve(id);
        Files.createDirectories(result);
        return result;
    }

    /**
     * Resolves the session directory identified by {@code path}, creating it if necessary.
     *
     * @param clientType    the client type that owns the home directory layer
     * @param baseDirectory the base storage directory
     * @param path          the session identifier
     * @return the resolved session directory, guaranteed to exist
     * @throws IOException if the directory cannot be created
     */
    public static Path getSessionDirectory(LinkedWhatsAppClientType clientType, Path baseDirectory, String path) throws IOException {
        var result = getHomeDirectory(clientType, baseDirectory)
                .resolve(path);
        Files.createDirectories(result);
        return result;
    }

    /**
     * Resolves the path of {@code fileName} inside the session directory identified by {@code uuid}.
     *
     * @param clientType    the client type that owns the home directory layer
     * @param baseDirectory the base storage directory
     * @param uuid          the session identifier
     * @param fileName      the file name inside the session
     * @return the resolved path
     * @throws IOException if the parent directories cannot be created
     */
    public static Path getSessionFile(LinkedWhatsAppClientType clientType, Path baseDirectory, String uuid, String fileName) throws IOException {
        return getSessionDirectory(clientType, baseDirectory, uuid)
                .resolve(fileName);
    }

    /**
     * Recursively deletes {@code path} and everything underneath it.
     *
     * @param path the path to delete
     * @throws IOException if any filesystem operation fails
     */
    public static void deleteRecursively(Path path) throws IOException {
        if (Files.notExists(path)) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "recursively deleting session directory tree");
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ProtobufWhatsAppStore that
                            && Objects.equals(signalStore, that.signalStore)
                            && Objects.equals(accountStore, that.accountStore)
                            && Objects.equals(contactStore, that.contactStore)
                            && Objects.equals(syncStore, that.syncStore)
                            && Objects.equals(settingsStore, that.settingsStore)
                            && Objects.equals(chatStore(), that.chatStore())
                            && Objects.equals(directory, that.directory)
                            && Objects.equals(webSessionStore, that.webSessionStore)
                            && Objects.equals(wamStore(), that.wamStore());
    }

    @Override
    public int hashCode() {
        return Objects.hash(signalStore, accountStore, contactStore, syncStore, settingsStore, chatStore(),
                directory, webSessionStore, wamStore());
    }
}
