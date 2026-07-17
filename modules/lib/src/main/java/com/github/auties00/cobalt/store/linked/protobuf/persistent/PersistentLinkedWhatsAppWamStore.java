package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.protobuf.ProtobufLinkedWhatsAppWamStore;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingCounters;
import it.auties.protobuf.annotation.ProtobufMessage;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ProtobufLinkedWhatsAppWamStore} variant that offloads staged WAM event buffers to an
 * independent embedded MVStore.
 *
 * <p>The serializable WAM bookkeeping (sequence numbers, watermarks, pending thread-logging rows) is
 * persisted inline with the aggregate by the inherited protobuf fields; the bulk event buffers are held
 * in the {@link PersistentWamBuffersStore} the persistent store wires in by
 * {@link #setBuffersStore(PersistentWamBuffersStore)} after that store is opened, so a large staging
 * backlog never inflates the {@code store.proto} snapshot. This mirrors how the persistent chat sub-store
 * offloads message bodies to its own MVStore facade, though the WAM buffers land in a separate file.
 *
 * @implNote
 * The buffers store is attached post-construction because it opens only after the session store is
 * deserialized or built; before it is attached the buffer accessors degrade to empty results, the same
 * window in which the persistent chat store has no message store attached.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class PersistentLinkedWhatsAppWamStore extends ProtobufLinkedWhatsAppWamStore {
    /**
     * The logger for {@link PersistentLinkedWhatsAppWamStore}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentLinkedWhatsAppWamStore.class);

    /**
     * The buffers store staged buffers are offloaded to; wired by
     * {@link #setBuffersStore(PersistentWamBuffersStore)}, not persisted, {@code null} until attached.
     */
    private volatile PersistentWamBuffersStore buffersStore;

    /**
     * Constructs a persistent WAM sub-store with the given protobuf-decoded bookkeeping.
     *
     * @param sequenceNumbersMap          the WAM sequence-number map, or {@code null} for an empty map
     * @param lastDailyStatsTimestamp     the instant of the last daily-stats run, or {@code null}
     * @param chatThreadLoggingSecret     the chat-thread-logging shared secret, or {@code null}
     * @param chatThreadLoggingOffset     the chat-thread-logging day-bucket offset in seconds, or {@code null}
     * @param lastUploadedThreadLoggingTs the watermark instant of the last thread-logging upload, or {@code null}
     * @param threadLoggingPending        the pending thread-logging counter rows, or {@code null} for an empty list
     */
    PersistentLinkedWhatsAppWamStore(ConcurrentMap<Integer, Integer> sequenceNumbersMap, Instant lastDailyStatsTimestamp,
                                     byte[] chatThreadLoggingSecret, Integer chatThreadLoggingOffset,
                                     Instant lastUploadedThreadLoggingTs,
                                     List<ThreadLoggingCounters> threadLoggingPending) {
        super(sequenceNumbersMap, lastDailyStatsTimestamp, chatThreadLoggingSecret, chatThreadLoggingOffset,
                lastUploadedThreadLoggingTs, threadLoggingPending);
    }

    @Override
    protected ConcurrentMap<Integer, Integer> sequenceNumbersMap() {
        return super.sequenceNumbersMap();
    }

    /**
     * Sets the buffers store that staged buffers are offloaded to.
     *
     * <p>The buffers store opens only after this store is built or deserialised, so it cannot be a
     * constructor argument; the persistent store sets it once, before the store is handed back.
     *
     * @apiNote
     * Called by {@link PersistentStore} once the buffers store is opened, the same post-construction
     * wiring step by which the persistent chat sub-store receives its message store.
     *
     * @param buffersStore the freshly opened buffers store, never {@code null}
     */
    void setBuffersStore(PersistentWamBuffersStore buffersStore) {
        this.buffersStore = Objects.requireNonNull(buffersStore, "buffersStore cannot be null");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "wam buffers store set");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns an empty collection until the buffers store is attached; a caller that
     * enumerates buffers before the message store opens sees none.
     */
    @Override
    public Collection<String> pendingBufferKeys() {
        var store = buffersStore;
        return store == null ? List.of() : store.wamBufferKeys();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation drops the buffer when the buffers store is not yet attached; the telemetry
     * pipeline only stages buffers after {@link com.github.auties00.cobalt.wam.WamService} initializes,
     * well past the attach point.
     */
    @Override
    public void putPendingBuffer(String saveKey, byte[] buffer) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");
        var store = buffersStore;
        if (store == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "dropping wam buffer {0}: no buffers store attached", saveKey);
            return;
        }
        store.putWamBuffer(saveKey, buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> findPendingBuffer(String saveKey) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        var store = buffersStore;
        return store == null ? Optional.empty() : store.findWamBuffer(saveKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removePendingBuffer(String saveKey) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        var store = buffersStore;
        return store != null && store.removeWamBuffer(saveKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearPendingBuffers() {
        var store = buffersStore;
        if (store != null) {
            store.clearWamBuffers();
        }
    }
}
