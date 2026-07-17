package com.github.auties00.cobalt.store.linked.protobuf.temporary;

import com.github.auties00.cobalt.store.linked.protobuf.ProtobufLinkedWhatsAppWamStore;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingCounters;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ProtobufLinkedWhatsAppWamStore} variant that holds staged WAM event buffers in memory.
 *
 * <p>The serializable WAM bookkeeping is carried by the inherited protobuf fields, but the transient
 * store has no disk surface, so staged buffers live in a {@link ConcurrentHashMap} for the process
 * lifetime and are discarded with the store. A restart therefore loses any buffer that was encoded but
 * not yet uploaded, consistent with the transient store discarding every other piece of session state.
 *
 * @implNote
 * This implementation is not a protobuf message; the transient aggregate that owns it is never
 * serialized, so only the inherited bookkeeping fields would round-trip and the in-memory buffer map is
 * dropped with the store. It therefore lives beside {@link TemporaryStore} rather than in the protobuf
 * package the persistent variant needs for its generated spec.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class TemporaryLinkedWhatsAppWamStore extends ProtobufLinkedWhatsAppWamStore {
    /**
     * The staged WAM event buffers keyed by save key; not persisted.
     */
    private final ConcurrentMap<String, byte[]> pendingBuffers;

    /**
     * Constructs an in-memory WAM sub-store with the given bookkeeping and an empty buffer map.
     *
     * @param sequenceNumbersMap          the WAM sequence-number map, or {@code null} for an empty map
     * @param lastDailyStatsTimestamp     the instant of the last daily-stats run, or {@code null}
     * @param chatThreadLoggingSecret     the chat-thread-logging shared secret, or {@code null}
     * @param chatThreadLoggingOffset     the chat-thread-logging day-bucket offset in seconds, or {@code null}
     * @param lastUploadedThreadLoggingTs the watermark instant of the last thread-logging upload, or {@code null}
     * @param threadLoggingPending        the pending thread-logging counter rows, or {@code null} for an empty list
     */
    TemporaryLinkedWhatsAppWamStore(ConcurrentMap<Integer, Integer> sequenceNumbersMap, Instant lastDailyStatsTimestamp,
                                    byte[] chatThreadLoggingSecret, Integer chatThreadLoggingOffset,
                                    Instant lastUploadedThreadLoggingTs,
                                    List<ThreadLoggingCounters> threadLoggingPending) {
        super(sequenceNumbersMap, lastDailyStatsTimestamp, chatThreadLoggingSecret, chatThreadLoggingOffset,
                lastUploadedThreadLoggingTs, threadLoggingPending);
        this.pendingBuffers = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<String> pendingBufferKeys() {
        return new ArrayList<>(pendingBuffers.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putPendingBuffer(String saveKey, byte[] buffer) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        Objects.requireNonNull(buffer, "buffer cannot be null");
        pendingBuffers.put(saveKey, buffer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<byte[]> findPendingBuffer(String saveKey) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        return Optional.ofNullable(pendingBuffers.get(saveKey));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removePendingBuffer(String saveKey) {
        Objects.requireNonNull(saveKey, "saveKey cannot be null");
        return pendingBuffers.remove(saveKey) != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearPendingBuffers() {
        pendingBuffers.clear();
    }
}
