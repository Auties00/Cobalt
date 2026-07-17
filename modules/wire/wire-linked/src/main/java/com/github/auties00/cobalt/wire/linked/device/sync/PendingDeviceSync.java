package com.github.auties00.cobalt.wire.linked.device.sync;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a device-list synchronization request that failed to complete and
 * must be retried on the next reconnection.
 *
 * <p>Whenever Cobalt interacts with a user (sending a message, checking
 * encryption keys, fetching business profiles, and so on) it needs the
 * up-to-date list of that user's companion devices. Devices can be primary
 * phones, linked web or desktop clients, or linked companion apps. When the
 * device-list fetch fails (network drop, server busy, temporary rate limit)
 * the request is persisted as a {@code PendingDeviceSync} so that it can be
 * replayed once the client is healthy again.
 *
 * <p>Each pending entry groups together the set of user JIDs that were part of
 * the original batched fetch, the business context that triggered it (used for
 * logging and metrics), a creation timestamp, and a retry counter that caps
 * how many times the batch is attempted before being abandoned.
 *
 * <p>Pending entries age out after a fixed window so that stale requests from
 * previous sessions do not keep re-firing indefinitely, and they are dropped
 * once the retry budget is exhausted.
 */
@ProtobufMessage
public final class PendingDeviceSync implements Serializable {
    /**
     * Maximum number of times a pending sync is retried before being
     * discarded.
     *
     * <p>After this many attempts the batch is considered permanently
     * failed and is removed from the persistence store.
     */
    private static final int MAX_RETRIES = 3;

    /**
     * Time-to-live window after which a pending sync is treated as expired.
     *
     * <p>Entries older than this are dropped regardless of the retry count,
     * since the underlying device list for the involved users has very
     * likely changed in the meantime and a fresh fetch is more useful
     * than replaying the stale batch.
     */
    private static final Duration EXPIRY_DURATION = Duration.ofHours(24);

    /**
     * The user JIDs whose device lists need to be refreshed.
     *
     * <p>Device-list syncs are typically batched by business operation
     * (for example "all recipients of this group send" or "every chat
     * participant the user just opened"), so a single pending entry can
     * cover several users that were requested together.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<Jid> userJids;

    /**
     * A short tag describing the caller that originally requested the sync.
     *
     * <p>This is a free-form identifier used for logging, metrics and
     * debugging. Typical values include the name of the feature or job
     * that issued the original fetch (for example "message-send" or
     * "group-open").
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String context;

    /**
     * Instant at which the pending entry was first recorded.
     *
     * <p>Used together with {@link #EXPIRY_DURATION} to detect entries
     * that are too old to be worth retrying.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    final Instant timestamp;

    /**
     * Number of retry attempts that have already been made for this batch.
     *
     * <p>Starts at zero when the entry is first created and is incremented
     * each time the replay is kicked off. When it reaches
     * {@link #MAX_RETRIES} the entry is considered exhausted.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.INT32)
    final int retryCount;

    /**
     * The {@code id} of the account-sync {@code <devices>} notification whose ack
     * was deferred until this sync completes, or {@code null} when the sync
     * carries no deferred ack.
     *
     * <p>Set only for entries queued by the account-sync devices handler during
     * resume-from-restart, where the inline device list is not trusted and the
     * notification must not be acknowledged before its follow-up device fetch has
     * actually run. When the replayed sync succeeds the matching ack is shipped.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String notificationId;

    /**
     * The {@code from} of the deferred-ack notification (the account that owns the
     * device list), or {@code null} when the sync carries no deferred ack.
     *
     * <p>Used as the {@code to} target of the deferred ack shipped once the sync
     * completes; meaningful only when {@link #notificationId} is non-{@code null}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final Jid notificationFrom;

    /**
     * Creates a new pending device-sync entry with the given fields.
     *
     * <p>This constructor is package-private and is intended to be used by
     * the protobuf framework and by the internal {@link #nextRetry()}
     * helper. External callers should use the {@link #of(Collection, String)}
     * factory method, which copies the user list defensively and stamps a
     * fresh creation time, or {@link #ofDeferredAck(Collection, String, String, Jid)}
     * when the entry must also carry a deferred notification ack.
     *
     * @param userJids         the user JIDs whose device lists need to be refreshed
     * @param context          a short tag identifying the caller
     * @param timestamp        the instant at which this entry was first recorded
     * @param retryCount       the number of retries already attempted
     * @param notificationId   the deferred-ack notification id, or {@code null}
     * @param notificationFrom the deferred-ack notification sender, or {@code null}
     */
    PendingDeviceSync(List<Jid> userJids, String context, Instant timestamp, int retryCount, String notificationId, Jid notificationFrom) {
        this.userJids = userJids;
        this.context = context;
        this.timestamp = timestamp;
        this.retryCount = retryCount;
        this.notificationId = notificationId;
        this.notificationFrom = notificationFrom;
    }

    /**
     * Creates a fresh pending sync entry for the given users and caller
     * context.
     *
     * <p>The timestamp is set to {@link Instant#now()} and the retry counter
     * starts at zero. The supplied collection is defensively copied so later
     * mutations by the caller do not affect the stored entry.
     *
     * @param userJids the user JIDs whose device lists need to be refreshed
     * @param context  a short tag identifying the caller
     * @return a new pending sync ready to be persisted
     */
    public static PendingDeviceSync of(Collection<Jid> userJids, String context) {
        return new PendingDeviceSync(List.copyOf(userJids), context, Instant.now(), 0, null, null);
    }

    /**
     * Creates a fresh pending sync entry that also carries a deferred notification
     * ack to be shipped once the sync completes.
     *
     * <p>Used by the account-sync devices handler when it queues a self device
     * sync during resume-from-restart: the originating notification's ack is held
     * back (not sent immediately) and is shipped by the replay only after the
     * device fetch succeeds, so a crash before completion leaves the notification
     * unacknowledged and the server replays it.
     *
     * @param userJids         the user JIDs whose device lists need to be refreshed
     * @param context          a short tag identifying the caller
     * @param notificationId   the {@code id} of the notification whose ack is deferred
     * @param notificationFrom the {@code from} of that notification, used as the ack target
     * @return a new pending sync carrying the deferred ack
     */
    public static PendingDeviceSync ofDeferredAck(Collection<Jid> userJids, String context, String notificationId, Jid notificationFrom) {
        return new PendingDeviceSync(List.copyOf(userJids), context, Instant.now(), 0, notificationId, notificationFrom);
    }

    /**
     * Returns a copy of this entry with the retry counter incremented by one.
     *
     * <p>The user list, caller context and original timestamp are preserved
     * unchanged. The original timestamp is deliberately kept so that the
     * {@link #isExpired()} window is measured from when the entry was
     * first recorded rather than from the latest retry.
     *
     * @return a new entry representing the next retry attempt
     */
    public PendingDeviceSync nextRetry() {
        return new PendingDeviceSync(userJids, context, timestamp, retryCount + 1, notificationId, notificationFrom);
    }

    /**
     * Returns whether this batch still has retries left.
     *
     * <p>Callers should check this before replaying the sync; once the
     * returned value is {@code false} the entry should be discarded
     * rather than retried.
     *
     * @return {@code true} if the retry budget has not yet been exhausted
     */
    public boolean shouldRetry() {
        return retryCount < MAX_RETRIES;
    }

    /**
     * Returns whether this entry has aged beyond the configured expiry
     * window.
     *
     * <p>Expired entries should be dropped without being replayed because
     * their device lists are almost certainly stale.
     *
     * @return {@code true} if the entry is older than 24 hours
     */
    public boolean isExpired() {
        return Duration.between(timestamp, Instant.now())
                       .compareTo(EXPIRY_DURATION) >= 0;
    }

    /**
     * Returns the user JIDs that were part of the original failed batch.
     *
     * <p>The returned list is unmodifiable.
     *
     * @return the user JIDs whose device lists still need to be refreshed
     */
    public List<Jid> userJids() {
        return userJids;
    }

    /**
     * Returns the short tag that identifies the caller that originally
     * requested the sync.
     *
     * @return the caller context string
     */
    public String context() {
        return context;
    }

    /**
     * Returns the instant at which this entry was first recorded.
     *
     * @return the creation timestamp
     */
    public Instant timestamp() {
        return timestamp;
    }

    /**
     * Returns how many retry attempts have already been made for this batch.
     *
     * @return the current retry count, starting at zero
     */
    public int retryCount() {
        return retryCount;
    }

    /**
     * Returns the {@code id} of the notification whose ack was deferred until this
     * sync completes.
     *
     * @return the deferred-ack notification id, or {@code null} when the entry
     *         carries no deferred ack
     */
    public String notificationId() {
        return notificationId;
    }

    /**
     * Returns the {@code from} of the deferred-ack notification, used as the ack
     * target once the sync completes.
     *
     * @return the deferred-ack notification sender, or {@code null} when the entry
     *         carries no deferred ack
     */
    public Jid notificationFrom() {
        return notificationFrom;
    }

    /**
     * Compares this entry to another object for value equality.
     *
     * <p>Two entries are equal when they carry the same user JIDs, the
     * same caller context, the same creation timestamp, the same retry
     * count, and the same deferred-ack notification id and sender.
     *
     * @param o the object to compare against
     * @return {@code true} if the two entries are value-equal
     */
    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof PendingDeviceSync that
                            && retryCount == that.retryCount
                            && Objects.equals(userJids, that.userJids)
                            && Objects.equals(context, that.context)
                            && Objects.equals(timestamp, that.timestamp)
                            && Objects.equals(notificationId, that.notificationId)
                            && Objects.equals(notificationFrom, that.notificationFrom);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code derived from all fields
     */
    @Override
    public int hashCode() {
        return Objects.hash(userJids, context, timestamp, retryCount, notificationId, notificationFrom);
    }

    /**
     * Returns a developer-friendly string representation suitable for
     * logging.
     *
     * <p>The format includes all four fields and is intended for diagnostic
     * output only; it is not part of the persistence format.
     *
     * @return a human-readable representation of this entry
     */
    @Override
    public String toString() {
        return "PendingDeviceSync[" +
               "userJids=" + userJids + ", " +
               "context=" + context + ", " +
               "timestamp=" + timestamp + ", " +
               "retryCount=" + retryCount + ", " +
               "notificationId=" + notificationId + ", " +
               "notificationFrom=" + notificationFrom + ']';
    }

}
