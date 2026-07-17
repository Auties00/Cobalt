package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.protobuf.*;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import com.github.auties00.cobalt.util.BufferedProtobufInputStream;
import com.github.auties00.cobalt.util.BufferedProtobufOutputStream;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The {@link ProtobufWhatsAppStore} that persists session metadata to a single protobuf file on disk
 * and offloads every message body to an embedded {@link PersistentMessageStore MVStore}.
 *
 * <p>Data layout under the session directory:
 * <ul>
 *   <li>{@code store.proto} holds the aggregate inherited from {@link ProtobufWhatsAppStore} (the five
 *       nested sub-stores, the retained runtime scalars) plus the {@link PersistentLinkedWhatsAppChatStore} at index
 *       82, whose chat and newsletter entries carry metadata only; message bodies live in MVStore.</li>
 *   <li>{@code messages.mv} holds the MVStore message file.</li>
 * </ul>
 *
 * @apiNote
 * Cobalt embedders obtain a {@link PersistentStore} indirectly through
 * {@link LinkedWhatsAppStoreFactory#persistent()}; the class is package-private so the persistence strategy
 * is not part of the public API surface.
 *
 * @implNote
 * This implementation captures {@link #hashCode()} into {@link #storeHashCode} at the end of each
 * successful {@link #save()} and short-circuits subsequent saves when nothing has changed. The MVStore
 * facade is owned by the {@link PersistentLinkedWhatsAppChatStore} and attached after construction.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@ProtobufMessage
final class PersistentStore extends ProtobufWhatsAppStore {
    /**
     * The logger for {@link PersistentStore}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentStore.class);

    /**
     * The name of the metadata file written under the session directory.
     */
    private static final String STORE_FILE = "store.proto";

    /**
     * The name of the MVStore message file under the session directory.
     */
    private static final String MESSAGES_FILE = "messages.mv";

    /**
     * The name of the MVStore WAM buffer file under the session directory.
     */
    private static final String WAM_BUFFERS_FILE = "wam.mv";

    /**
     * The persistence-variant WAM sub-store holding the serializable telemetry bookkeeping; its staged
     * event buffers offload to the independent {@link PersistentWamBuffersStore} wired by
     * {@link #setWamBuffersStore(PersistentWamBuffersStore)}.
     *
     * @implNote
     * This field carries index 11, the index the WAM sub-store held on {@link ProtobufWhatsAppStore}
     * before it became persistence-variant, so a {@code store.proto} written by the previous design still
     * decodes its telemetry bookkeeping unchanged.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.MESSAGE)
    final PersistentLinkedWhatsAppWamStore wamStore;

    /**
     * The independent MVStore facade staged WAM event buffers offload to; wired by
     * {@link #setWamBuffersStore(PersistentWamBuffersStore)}, not persisted, {@code null} until attached.
     *
     * @implNote
     * This implementation holds the reference directly rather than reaching it through {@link #wamStore}
     * because the sub-store exposes only its buffer-delegation surface, not the facade's lifecycle; the
     * session owns the durability checkpoints and teardown driven from {@link #await()}, {@link #close()}
     * and {@link #delete()}.
     */
    private volatile PersistentWamBuffersStore wamBuffersStore;

    /**
     * The persistence-variant chat sub-store holding the MVStore-backed chats and newsletters.
     */
    @ProtobufProperty(index = 82, type = ProtobufType.MESSAGE)
    final PersistentLinkedWhatsAppChatStore chatStore;

    /**
     * The quiet period, in milliseconds, the debounced flusher waits after the most recent
     * {@link #save()} request before it serialises the snapshot.
     *
     * <p>A burst of saves that arrive closer together than this never triggers more than the
     * trailing serialisation, so an offline catch-up that decrypts hundreds of messages back to
     * back collapses to a handful of writes instead of one per message.
     */
    private static final long FLUSH_QUIET_PERIOD_MILLIS = 250;

    /**
     * The maximum delay, in milliseconds, between the first pending {@link #save()} request and
     * the serialisation that clears it.
     *
     * <p>Caps the latency of the trailing write when {@link #save()} requests keep arriving faster
     * than {@link #FLUSH_QUIET_PERIOD_MILLIS}, so a continuous stream still persists at least once
     * per this interval rather than starving until the stream stops.
     */
    private static final long FLUSH_MAX_DELAY_MILLIS = 2000;

    /**
     * The hash code captured at the end of the last successful serialisation.
     */
    private volatile Integer storeHashCode;

    /**
     * The monitor guarding the debounced-flush coordination fields ({@link #flushPending},
     * {@link #flushClosed}, {@link #firstPendingNanos}, {@link #lastRequestNanos},
     * {@link #flusherThread}, {@link #flushShutdownHook}).
     */
    private final Object flushLock = new Object();

    /**
     * Whether a {@link #save()} request has arrived that the debounced flusher has not yet
     * serialised. Guarded by {@link #flushLock}.
     */
    private boolean flushPending;

    /**
     * Whether the debounced flusher has been stopped (by {@link #delete()}), after which
     * {@link #save()} requests are ignored. Guarded by {@link #flushLock}.
     */
    private boolean flushClosed;

    /**
     * The {@link System#nanoTime()} stamp of the {@link #save()} request that started the current
     * pending window, used against {@link #FLUSH_MAX_DELAY_MILLIS}. Guarded by {@link #flushLock}.
     */
    private long firstPendingNanos;

    /**
     * The {@link System#nanoTime()} stamp of the most recent {@link #save()} request, used against
     * {@link #FLUSH_QUIET_PERIOD_MILLIS}. Guarded by {@link #flushLock}.
     */
    private long lastRequestNanos;

    /**
     * The dedicated daemon thread that debounces and serialises pending snapshots, lazily started
     * by the first {@link #save()}. Guarded by {@link #flushLock}.
     */
    private Thread flusherThread;

    /**
     * Constructs a {@code PersistentStore} from its composed sub-stores and the persistent chat store.
     *
     * @apiNote
     * Package-private and intended for the generated {@code PersistentStoreBuilder} and the protobuf
     * deserialiser. The MVStore facade is wired by {@link PersistentLinkedWhatsAppStoreFactory} via
     * {@link #setMessageStore(PersistentMessageStore)} immediately after construction.
     *
     * @param signalStore           the signal sub-store
     * @param accountStore          the account sub-store
     * @param contactStore          the contact sub-store
     * @param syncStore             the sync sub-store
     * @param settingsStore         the settings sub-store
     * @param directory             the session directory
     * @param webSessionStore       the web-GraphQL credential sub-store, or {@code null} for an empty one
     * @param wamStore              the persistent WAM sub-store, or {@code null} for an empty one
     * @param chatStore             the persistent chat sub-store, or {@code null} for an empty one
     */
    PersistentStore(ProtobufLinkedWhatsAppSignalStore signalStore, ProtobufLinkedWhatsAppAccountStore accountStore, ProtobufLinkedWhatsAppContactStore contactStore, ProtobufLinkedWhatsAppSyncStore syncStore, ProtobufLinkedWhatsAppSettingsStore settingsStore, Path directory, ProtobufLinkedWebSessionStore webSessionStore, PersistentLinkedWhatsAppWamStore wamStore, PersistentLinkedWhatsAppChatStore chatStore) {
        var resolvedChatStore = chatStore != null ? chatStore : new PersistentLinkedWhatsAppChatStore(null, null, null, null);
        super(signalStore, accountStore, contactStore, syncStore, settingsStore, directory, webSessionStore, resolvedChatStore);
        this.chatStore = resolvedChatStore;
        this.wamStore = wamStore != null ? wamStore : new PersistentLinkedWhatsAppWamStoreBuilder().build();
    }

    @Override
    public PersistentLinkedWhatsAppChatStore chatStore() {
        return chatStore;
    }

    @Override
    public PersistentLinkedWhatsAppWamStore wamStore() {
        return wamStore;
    }

    /**
     * Sets the MVStore message facade on the persistent chat sub-store.
     *
     * <p>The facade opens only after this store is built or deserialised, so it cannot be a constructor
     * argument; the factory sets it here once, before the store is handed back.
     *
     * @apiNote
     * Called by {@link PersistentLinkedWhatsAppStoreFactory} after construction or deserialisation. The
     * independent WAM buffer facade is wired separately by
     * {@link #setWamBuffersStore(PersistentWamBuffersStore)}.
     *
     * @param messageStore the freshly opened MVStore message facade
     */
    void setMessageStore(PersistentMessageStore messageStore) {
        chatStore.setMessageStore(messageStore);
    }

    /**
     * Sets the independent MVStore WAM buffer facade, holding it for the session lifecycle and wiring the
     * persistent WAM sub-store's buffer delegation to it.
     *
     * <p>The facade opens only after this store is built or deserialised, so it cannot be a constructor
     * argument; the factory sets it here once, before the store is handed back.
     *
     * @apiNote
     * Called by {@link PersistentLinkedWhatsAppStoreFactory} after construction or deserialisation,
     * alongside {@link #setMessageStore(PersistentMessageStore)}.
     *
     * @param wamBuffersStore the freshly opened MVStore WAM buffer facade
     */
    void setWamBuffersStore(PersistentWamBuffersStore wamBuffersStore) {
        this.wamBuffersStore = wamBuffersStore;
        wamStore.setBuffersStore(wamBuffersStore);
    }

    /**
     * Returns the MVStore facade owned by the persistent chat sub-store.
     *
     * @return the message store, or {@code null} if not yet attached
     */
    PersistentMessageStore messageStore() {
        return chatStore.messageStore();
    }

    /**
     * Returns the path to the MVStore message file for the given session.
     *
     * @param clientType    the client type
     * @param baseDirectory the root directory under which per-session folders are created
     * @param sessionId     the session UUID string or phone-number string
     * @return the MVStore message file
     * @throws IOException if the parent session directory cannot be created
     */
    static Path messagesEnvPath(LinkedWhatsAppClientType clientType, Path baseDirectory, String sessionId) throws IOException {
        return getSessionDirectory(clientType, baseDirectory, sessionId)
                .resolve(MESSAGES_FILE);
    }

    /**
     * Returns the path to the MVStore WAM buffer file for the given session.
     *
     * @param clientType    the client type
     * @param baseDirectory the root directory under which per-session folders are created
     * @param sessionId     the session UUID string or phone-number string
     * @return the MVStore WAM buffer file
     * @throws IOException if the parent session directory cannot be created
     */
    static Path wamBuffersEnvPath(LinkedWhatsAppClientType clientType, Path baseDirectory, String sessionId) throws IOException {
        return getSessionDirectory(clientType, baseDirectory, sessionId)
                .resolve(WAM_BUFFERS_FILE);
    }

    /**
     * Returns the path to the {@code store.proto} metadata file for the given session.
     *
     * @param clientType    the client type
     * @param baseDirectory the root directory under which per-session folders are created
     * @param sessionId     the session UUID string or phone-number string
     * @return the metadata file path
     * @throws IOException if the parent session directory cannot be created
     */
    static Path storeFilePath(LinkedWhatsAppClientType clientType, Path baseDirectory, String sessionId) throws IOException {
        return getSessionFile(clientType, baseDirectory, sessionId, STORE_FILE);
    }

    /**
     * Deserializes a {@code store.proto} metadata snapshot into a {@code PersistentStore}.
     *
     * @apiNote
     * Internal helper for {@link PersistentLinkedWhatsAppStoreFactory#load}; the MVStore facade is attached separately.
     *
     * @param storeFile the metadata file
     * @return the deserialized store
     * @throws IOException if the file cannot be read or decoded
     */
    static PersistentStore deserialize(Path storeFile) throws IOException {
        try (var stream = new BufferedProtobufInputStream(storeFile)) {
            return PersistentStoreSpec.decode(stream);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation does not serialise inline. It records a pending request and wakes the
     * dedicated daemon flusher ({@link #runFlushLoop()}), which coalesces a burst of requests into a
     * single {@link #persist()} after {@link #FLUSH_QUIET_PERIOD_MILLIS} of quiet or at most every
     * {@link #FLUSH_MAX_DELAY_MILLIS}. This is what keeps an offline catch-up that decrypts hundreds
     * of messages back to back from rewriting the whole {@code store.proto} once per message; it
     * mirrors WhatsApp Web, which streams per-key writes to IndexedDB rather than snapshotting the
     * world on every change. Durability on teardown is the client lifecycle's responsibility:
     * {@code LiveLinkedWhatsAppClient.disconnect} calls {@link #await()} on a terminal disconnect,
     * which flushes any pending state synchronously, and that disconnect is itself what the client's
     * JVM shutdown hook invokes. A hard kill outside that path can lose at most the last
     * {@link #FLUSH_MAX_DELAY_MILLIS} of ratchet advancement, which the server-side retry-receipt
     * flow re-establishes on the next connection. Requests are ignored once {@link #delete()} has
     * stopped the flusher.
     */
    @Override
    public void save() {
        synchronized (flushLock) {
            if (flushClosed) {
                return;
            }
            var now = System.nanoTime();
            if (!flushPending) {
                flushPending = true;
                firstPendingNanos = now;
            }
            lastRequestNanos = now;
            ensureFlusherStarted();
            flushLock.notifyAll();
        }
    }

    /**
     * Starts the daemon flusher exactly once.
     *
     * @implNote
     * This implementation uses a dedicated platform daemon thread rather than a virtual thread; the
     * thread spends nearly all of its life parked on {@link #flushLock}. The store registers no JVM
     * shutdown hook of its
     * own: the daemon nature keeps it from blocking exit, and durable teardown is driven by the
     * client lifecycle through {@link #await()}. The caller must hold {@link #flushLock}.
     */
    private void ensureFlusherStarted() {
        if (flusherThread != null) {
            return;
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "starting debounced store flusher thread");
        flusherThread = Thread.ofPlatform()
                .name("cobalt-store-flusher")
                .daemon(true)
                .unstarted(this::runFlushLoop);
        flusherThread.start();
    }

    /**
     * Runs the debounce loop on the dedicated flusher thread until {@link #delete()} stops it.
     *
     * <p>Each iteration waits for a pending request, then waits until either
     * {@link #FLUSH_QUIET_PERIOD_MILLIS} has elapsed since the most recent request or
     * {@link #FLUSH_MAX_DELAY_MILLIS} has elapsed since the first pending request, then serialises
     * once via {@link #persist()}. A request that arrives during serialisation re-marks the state
     * pending and is picked up by the next iteration, so no change is lost.
     */
    private void runFlushLoop() {
        while (true) {
            synchronized (flushLock) {
                while (!flushPending && !flushClosed) {
                    try {
                        flushLock.wait();
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (flushClosed) {
                    return;
                }
                while (!flushClosed) {
                    var now = System.nanoTime();
                    var sinceLast = (now - lastRequestNanos) / 1_000_000L;
                    var sinceFirst = (now - firstPendingNanos) / 1_000_000L;
                    if (sinceLast >= FLUSH_QUIET_PERIOD_MILLIS || sinceFirst >= FLUSH_MAX_DELAY_MILLIS) {
                        break;
                    }
                    var waitMillis = Math.min(
                            FLUSH_QUIET_PERIOD_MILLIS - sinceLast,
                            FLUSH_MAX_DELAY_MILLIS - sinceFirst);
                    try {
                        flushLock.wait(Math.max(1, waitMillis));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (flushClosed) {
                    return;
                }
                flushPending = false;
            }
            persist();
        }
    }

    /**
     * Serialises the snapshot now if its content changed since the last successful write.
     *
     * @implNote
     * This implementation uses double-checked locking on {@link #storeHashCode}; the slow path
     * serialises on {@code synchronized(this)}. A write is skipped once {@link #stopFlusher()} has
     * closed the coordinator so a flush racing {@link #delete()} cannot recreate the session
     * directory that delete is removing. IO failures are logged and swallowed because callers cannot
     * meaningfully recover; the next attempt observes the same dirty state.
     */
    private void persist() {
        synchronized (flushLock) {
            if (flushClosed) {
                return;
            }
        }
        var newHashCode = hashCode();
        if (storeHashCode != null && storeHashCode == newHashCode) {
            return;
        }
        synchronized (this) {
            if (storeHashCode != null && storeHashCode == newHashCode) {
                return;
            }
            try {
                serializeStore();
                storeHashCode = newHashCode;
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "serialized store snapshot");
            } catch (IOException error) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "error while serializing store", error);
            }
        }
    }

    /**
     * Clears any pending request and serialises the snapshot synchronously.
     *
     * <p>Used by {@link #await()} and the JVM shutdown hook to make a pending debounced write
     * durable on demand. Safe to call when nothing is pending: {@link #persist()} short-circuits
     * when the content is unchanged.
     */
    private void flushNow() {
        synchronized (flushLock) {
            flushPending = false;
        }
        persist();
    }

    /**
     * Writes the current metadata snapshot to {@code store.proto} via a sibling temp file followed by
     * an atomic move.
     *
     * @throws IOException if the file cannot be created, written, or moved
     */
    private void serializeStore() throws IOException {
        var path = storeFilePath(accountStore().clientType(), directory(), accountStore().uuid().toString());
        var parent = path.getParent();
        Files.createDirectories(parent);
        var tempFile = Files.createTempFile(parent, path.getFileName().toString(), ".tmp");
        try {
            try (var stream = new BufferedProtobufOutputStream(tempFile)) {
                PersistentStoreSpec.encode(this, stream);
            }
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException error) {
            Files.deleteIfExists(tempFile);
            throw error;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation closes the message store and the WAM buffer store, releasing their file handles,
     * before recursively removing the session directory so the remove can succeed on Windows, where an open
     * file cannot be unlinked.
     */
    @Override
    public void delete() throws IOException {
        if (Log.INFO) LOGGER.log(Level.INFO, "deleting session {0}", accountStore().uuid());
        stopFlusher();
        var folderPath = getSessionDirectory(accountStore().clientType(), directory(), accountStore().uuid().toString());
        var messageStore = chatStore.messageStore();
        if (messageStore != null) {
            messageStore.close();
        }
        if (wamBuffersStore != null) {
            wamBuffersStore.close();
        }
        deleteRecursively(folderPath);
    }

    /**
     * Stops the debounced flusher and discards any pending write, used by {@link #delete()} before
     * the session directory is removed.
     *
     * @implNote
     * This implementation marks the coordinator closed (so no further {@link #save()} schedules a
     * write that would recreate the just-deleted file) and wakes the parked flusher so it can
     * observe the closed state and exit.
     */
    private void stopFlusher() {
        synchronized (flushLock) {
            flushClosed = true;
            flushPending = false;
            flushLock.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation serialises any pending debounced snapshot synchronously via
     * {@link #flushNow()} so callers that need the store durable on disk (rather than within the
     * next {@link #FLUSH_MAX_DELAY_MILLIS} window) can block on it, then forces the message store and the
     * WAM buffer store to a durable checkpoint via {@link PersistentMessageStore#commit()} and
     * {@link PersistentWamBuffersStore#commit()} so bodies and buffers written since the last background
     * auto-commit are flushed on the same cadence (including from the client's JVM shutdown hook). The
     * metadata snapshot itself decodes synchronously in {@link PersistentLinkedWhatsAppStoreFactory#load},
     * so there is no load-time work to await.
     */
    @Override
    public void await() {
        flushNow();
        var messageStore = chatStore.messageStore();
        if (messageStore != null) {
            messageStore.commit();
        }
        if (wamBuffersStore != null) {
            wamBuffersStore.commit();
        }
    }

    /**
     * Closes the MVStore message store and WAM buffer store so the session can be shut down without being
     * deleted.
     *
     * @apiNote
     * Called by the factory during ordinary shutdown.
     */
    void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "closing session {0}", accountStore().uuid());
        var messageStore = chatStore.messageStore();
        if (messageStore != null) {
            messageStore.close();
        }
        if (wamBuffersStore != null) {
            wamBuffersStore.close();
        }
    }
}
