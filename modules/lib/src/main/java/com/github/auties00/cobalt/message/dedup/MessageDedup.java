package com.github.auties00.cobalt.message.dedup;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.message.receive.stanza.MessageReceiveEncryptedPayload;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Suppresses duplicate work on in-flight inbound messages during the
 * offline-delivery replay window.
 *
 * <p>When the WhatsApp socket reconnects, the server replays every message
 * that was not acknowledged before the disconnect. Without a guard the same
 * message would be processed twice: once from the replay stream and once from
 * any queued stanza that had already arrived during the previous session. The
 * inbound dispatch pipeline tests a composite key with {@link #isPending(String)}
 * (or the {@link MessageKey}, {@link Instant}, payload-list overload) before
 * spending work on a stanza, registers the key via {@link #add(String)} to
 * take ownership, and releases it with {@link #remove(String)} once the work
 * completes or throws. The whole cache is dropped in bulk by
 * {@link #maybeClear(int)} when the offline-delivery counter reaches zero. The
 * cache is backed by a single map whose entries each hold a reference count,
 * so multiple cooperating call sites can claim the same key without premature
 * eviction. A single instance is owned by
 * {@link com.github.auties00.cobalt.message.receive.MessageReceivingService}
 * and shared across the pipeline.
 *
 * @implNote This implementation holds the cache in a process-local
 * {@link ConcurrentMap} and drops it in full on JVM restart, where WA Web's
 * cache is dropped on tab reload.
 */
@WhatsAppWebModule(moduleName = "WAWebMessageDedupUtils")
public final class MessageDedup {
    /**
     * The logger for {@link MessageDedup}.
     */
    private static final System.Logger LOGGER = Log.get(MessageDedup.class);

    /**
     * Maps each composite dedup key to its outstanding reference count.
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = {"addPendingMessage", "hasPendingMessage", "maybeClearPendingMessages"},
            adaptation = WhatsAppAdaptation.ADAPTED)
    private final ConcurrentMap<String, Integer> pending;

    /**
     * Constructs an empty dedup cache.
     */
    public MessageDedup() {
        this.pending = new ConcurrentHashMap<>();
    }

    /**
     * Returns whether the pending-message cache is currently enabled by the
     * server-side AB prop.
     *
     * <p>The dispatch pipeline gates registration on this predicate so the
     * server can roll the dedup feature back without a client redeploy; callers
     * check it before invoking {@link #add(String)} or its composite overload.
     *
     * @implNote This implementation reads the
     * {@link ABProp#WEB_PENDING_MESSAGE_CACHE_ENABLED} flag through
     * {@link ABPropsService#getBool(ABProp)}, which returns the server's most
     * recent value and defaults to {@code false} when the prop has never been
     * seen.
     *
     * @param abPropsService the AB props service consulted for the
     *                       {@code web_pending_message_cache_enabled} flag
     * @return {@code true} when the server has flipped the
     *         {@code web_pending_message_cache_enabled} AB prop on
     * @throws NullPointerException if {@code abPropsService} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "isPengingMessageCacheEnabled",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static boolean isCacheEnabled(ABPropsService abPropsService) {
        Objects.requireNonNull(abPropsService, "abPropsService");
        return abPropsService.getBool(ABProp.WEB_PENDING_MESSAGE_CACHE_ENABLED);
    }

    /**
     * Registers the given composite key as pending and returns its new
     * reference count.
     *
     * <p>The key starts with reference count {@code 1}; a re-registration
     * atomically increments the existing count. Use this overload when the
     * composite key has already been built, for example when caching the same
     * key from two different call sites; for the canonical
     * message-key/timestamp/encs derivation use
     * {@link #add(MessageKey, Instant, List)}.
     *
     * @param key the composite dedup key produced by
     *            {@link PendingMessageKey#create(MessageKey, Instant, List)}
     * @return the new reference count for {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "addPendingMessage",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public int add(String key) {
        Objects.requireNonNull(key, "key");

        var newCount = pending.merge(key, 1, Integer::sum);

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dedup add message key={0} total={1}", new LogRedactable.User(key), newCount);
        }

        return newCount;
    }

    /**
     * Registers an incoming message as pending under its canonical composite
     * key and returns the new reference count.
     *
     * <p>Derives the composite key via
     * {@link PendingMessageKey#create(MessageKey, Instant, List)} and delegates
     * to {@link #add(String)}.
     *
     * @param key       the logical message key
     * @param timestamp the message timestamp, serialised as Unix epoch seconds
     * @param encs      the encrypted payloads carried on the incoming
     *                  {@code <message>} stanza
     * @return the new reference count for the derived composite key
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "addPendingMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int add(MessageKey key, Instant timestamp, List<MessageReceiveEncryptedPayload> encs) {
        return add(PendingMessageKey.create(key, timestamp, encs));
    }

    /**
     * Returns whether the given composite key is currently registered as
     * pending.
     *
     * <p>The inbound dispatch loop uses this to decide whether to skip a stanza
     * whose decryption is already in progress (or has already completed) on
     * another execution path. A debug log entry is emitted on a cache hit.
     *
     * @param key the composite dedup key
     * @return {@code true} if at least one outstanding reference exists for
     *         {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "hasPendingMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isPending(String key) {
        Objects.requireNonNull(key, "key");

        var count = pending.get(key);
        if (count == null) {
            return false;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "dedup pending hit key={0} total={1}", new LogRedactable.User(key), count);
        }
        return true;
    }

    /**
     * Returns whether the canonical composite key for an incoming message is
     * already registered.
     *
     * <p>Derives the composite key from the same triple
     * {@link #add(MessageKey, Instant, List)} uses and delegates to
     * {@link #isPending(String)}, so the caller need not retain the composite
     * string between the {@code isPending}/{@code add} pair.
     *
     * @param key       the logical message key
     * @param timestamp the message timestamp, serialised as Unix epoch seconds
     * @param encs      the encrypted payloads carried on the incoming
     *                  {@code <message>} stanza
     * @return {@code true} if the derived composite key is registered
     * @throws NullPointerException if any argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "hasPendingMessage",
            adaptation = WhatsAppAdaptation.DIRECT)
    public boolean isPending(MessageKey key, Instant timestamp, List<MessageReceiveEncryptedPayload> encs) {
        return isPending(PendingMessageKey.create(key, timestamp, encs));
    }

    /**
     * Drops every entry in the cache when the supplied offline-delivery counter
     * reaches zero.
     *
     * <p>Called from the offline-info-bulletin handler with the
     * remaining-message counter the server reports. Once the offline-delivery
     * phase ends no pending message id is at risk of being duplicated, so the
     * memory is released in bulk rather than per entry. The cache is cleared
     * only when {@code count} is exactly zero.
     *
     * @param count the remaining-message counter reported by the server
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "maybeClearPendingMessages",
            adaptation = WhatsAppAdaptation.DIRECT)
    public void maybeClear(int count) {
        if (count == 0) {
            if (!pending.isEmpty() && Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "dedup cache cleared entries={0}", pending.size());
            }
            pending.clear();
        }
    }

    /**
     * Unconditionally empties the cache.
     *
     * <p>Serves callers that have verified the offline-delivery counter
     * externally and want to drop the cache without rechecking; most callers
     * prefer {@link #maybeClear(int)} so the guard stays in one place.
     */
    public void clear() {
        maybeClear(0);
    }

    /**
     * Decrements the reference count for the given composite key and removes
     * the entry once the count reaches zero.
     *
     * <p>Pairs with {@link #add(String)} or
     * {@link #add(MessageKey, Instant, List)}; calling code releases a key when
     * the work that took ownership has either completed or thrown. A removal
     * for an unknown key is a no-op.
     *
     * @param key the composite dedup key
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public void remove(String key) {
        Objects.requireNonNull(key, "key");

        var remaining = pending.compute(key, (_, count) -> {
            if (count == null) {
                return null;
            }
            var decremented = count - 1;
            return decremented <= 0 ? null : decremented;
        });

        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "dedup remove key={0} remaining={1}", new LogRedactable.User(key), remaining);
        }
    }

    /**
     * Returns the number of distinct composite keys currently in the cache.
     *
     * <p>Surfaces the {@link ConcurrentMap#size()} of the underlying refcount
     * map, which is the count of distinct keys rather than the sum of their
     * reference counts; useful for diagnostics and for assertions in dedup
     * tests.
     *
     * @return the cache size
     */
    @WhatsAppWebExport(moduleName = "WAWebMessageDedupUtils", exports = "maybeClearPendingMessages",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public int size() {
        return pending.size();
    }
}
