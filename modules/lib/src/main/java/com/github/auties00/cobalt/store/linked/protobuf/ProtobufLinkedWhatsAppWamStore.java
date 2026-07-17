package com.github.auties00.cobalt.store.linked.protobuf;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppWamStore;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.threadlogging.ThreadLoggingCounters;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The protobuf-backed base of the {@link LinkedWhatsAppWamStore} sub-store, holding this session's
 * telemetry bookkeeping.
 *
 * <p>This abstract nested {@code MESSAGE} sub-store of {@link ProtobufWhatsAppStore} owns the
 * strategy-independent WAM state: the per-channel sequence numbers written into every uploaded buffer
 * header, the daily-stats and thread-logging watermarks, and the pending thread-logging counter rows,
 * all serialized inline with the aggregate. The staged event buffers are left abstract because each
 * persistence strategy backs them differently: the persistent variant offloads them to the embedded
 * message store while the transient variant holds them in memory, mirroring how the chat sub-store
 * splits its message backing.
 *
 * @implNote
 * This implementation keeps only serializable WAM bookkeeping; the buffer staging that the previous
 * design resolved through a bound session directory now lives entirely in the persistence-variant
 * subclasses, so this base needs neither the directory nor the account identity.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class ProtobufLinkedWhatsAppWamStore implements LinkedWhatsAppWamStore {
    /**
     * The logger for {@link ProtobufLinkedWhatsAppWamStore}.
     */
    private static final System.Logger LOGGER = Log.get(ProtobufLinkedWhatsAppWamStore.class);

    /**
     * The WAM event sequence numbers per channel for dedup.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MAP, mapKeyType = ProtobufType.INT32, mapValueType = ProtobufType.INT32)
    private final ConcurrentMap<Integer, Integer> sequenceNumbersMap;

    /**
     * The instant of the most recent daily-stats task run, or {@code null} if it has never run; serialized
     * on the wire as an epoch-millis {@code UINT64} via {@link InstantMillisMixin}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant lastDailyStatsTimestamp;

    /**
     * The chat-thread-logging shared secret provisioned from the companion phone, or {@code null} if none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BYTES)
    private byte[] chatThreadLoggingSecret;

    /**
     * The chat-thread-logging day-bucket offset in seconds provisioned from the companion phone, or {@code null} if none.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    private Integer chatThreadLoggingOffset;

    /**
     * The watermark instant of the most recent thread-logging upload pass, or {@code null} if none has
     * completed; serialized on the wire as an epoch-seconds {@code UINT64} via {@link InstantSecondsMixin}.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    private Instant lastUploadedThreadLoggingTs;

    /**
     * The pending thread-logging counter rows, each identified by its own {@code (chatJid, startTs)} pair.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    private final List<ThreadLoggingCounters> threadLoggingPending;

    /**
     * Full protobuf constructor invoked by the generated builder and the deserializer.
     *
     * @param sequenceNumbersMap          the WAM sequence-number map, or {@code null} for an empty map
     * @param lastDailyStatsTimestamp     the instant of the last daily-stats run, or {@code null}
     * @param chatThreadLoggingSecret     the chat-thread-logging shared secret, or {@code null}
     * @param chatThreadLoggingOffset     the chat-thread-logging day-bucket offset in seconds, or {@code null}
     * @param lastUploadedThreadLoggingTs the watermark instant of the last thread-logging upload, or {@code null}
     * @param threadLoggingPending        the pending thread-logging counter rows, or {@code null} for an empty list
     */
    protected ProtobufLinkedWhatsAppWamStore(ConcurrentMap<Integer, Integer> sequenceNumbersMap, Instant lastDailyStatsTimestamp,
                                             byte[] chatThreadLoggingSecret, Integer chatThreadLoggingOffset,
                                             Instant lastUploadedThreadLoggingTs,
                                             List<ThreadLoggingCounters> threadLoggingPending) {
        this.sequenceNumbersMap = Objects.requireNonNullElseGet(sequenceNumbersMap, ConcurrentHashMap::new);
        this.lastDailyStatsTimestamp = lastDailyStatsTimestamp;
        this.chatThreadLoggingSecret = chatThreadLoggingSecret;
        this.chatThreadLoggingOffset = chatThreadLoggingOffset;
        this.lastUploadedThreadLoggingTs = lastUploadedThreadLoggingTs;
        this.threadLoggingPending = threadLoggingPending == null
                ? new CopyOnWriteArrayList<>()
                : new CopyOnWriteArrayList<>(threadLoggingPending);
    }

    /**
     * Returns the live WAM sequence-number map backing this store.
     *
     * @return the live WAM sequence-number map
     */
    protected ConcurrentMap<Integer, Integer> sequenceNumbersMap() {
        return sequenceNumbersMap;
    }

    @Override
    public OptionalInt findSequenceNumber(WamChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");
        var stored = sequenceNumbersMap.get(channel.id());
        return stored == null ? OptionalInt.empty() : OptionalInt.of(stored);
    }

    @Override
    public LinkedWhatsAppWamStore putSequenceNumber(WamChannel channel, int sequenceNumber) {
        Objects.requireNonNull(channel, "channel cannot be null");
        sequenceNumbersMap.put(channel.id(), sequenceNumber);
        return this;
    }

    @Override
    public Optional<Instant> lastDailyStatsTimestamp() {
        return Optional.ofNullable(lastDailyStatsTimestamp);
    }

    @Override
    public LinkedWhatsAppWamStore setLastDailyStatsTimestamp(Instant lastDailyStatsTimestamp) {
        this.lastDailyStatsTimestamp = lastDailyStatsTimestamp;
        return this;
    }

    @Override
    public Optional<byte[]> chatThreadLoggingSecret() {
        return Optional.ofNullable(chatThreadLoggingSecret);
    }

    @Override
    public LinkedWhatsAppWamStore setChatThreadLoggingSecret(byte[] chatThreadLoggingSecret) {
        this.chatThreadLoggingSecret = chatThreadLoggingSecret;
        return this;
    }

    @Override
    public OptionalInt chatThreadLoggingOffset() {
        return chatThreadLoggingOffset == null ? OptionalInt.empty() : OptionalInt.of(chatThreadLoggingOffset);
    }

    @Override
    public LinkedWhatsAppWamStore setChatThreadLoggingOffset(Integer chatThreadLoggingOffset) {
        this.chatThreadLoggingOffset = chatThreadLoggingOffset;
        return this;
    }

    @Override
    public Optional<Instant> lastUploadedThreadLoggingTs() {
        return Optional.ofNullable(lastUploadedThreadLoggingTs);
    }

    @Override
    public LinkedWhatsAppWamStore setLastUploadedThreadLoggingTs(Instant lastUploadedThreadLoggingTs) {
        this.lastUploadedThreadLoggingTs = lastUploadedThreadLoggingTs;
        return this;
    }

    @Override
    public Collection<ThreadLoggingCounters> threadLoggingPending() {
        return List.copyOf(threadLoggingPending);
    }

    @Override
    public LinkedWhatsAppWamStore addThreadLoggingCounters(ThreadLoggingCounters counters) {
        threadLoggingPending.add(counters);
        return this;
    }

    @Override
    public LinkedWhatsAppWamStore removeThreadLoggingCounters(Collection<ThreadLoggingCounters> counters) {
        threadLoggingPending.removeAll(counters);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ProtobufLinkedWhatsAppWamStore that
                            && Objects.equals(sequenceNumbersMap, that.sequenceNumbersMap)
                            && Objects.equals(lastDailyStatsTimestamp, that.lastDailyStatsTimestamp)
                            && Arrays.equals(chatThreadLoggingSecret, that.chatThreadLoggingSecret)
                            && Objects.equals(chatThreadLoggingOffset, that.chatThreadLoggingOffset)
                            && Objects.equals(lastUploadedThreadLoggingTs, that.lastUploadedThreadLoggingTs)
                            && Objects.equals(threadLoggingPending, that.threadLoggingPending);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sequenceNumbersMap, lastDailyStatsTimestamp, Arrays.hashCode(chatThreadLoggingSecret),
                chatThreadLoggingOffset, lastUploadedThreadLoggingTs, threadLoggingPending);
    }
}
