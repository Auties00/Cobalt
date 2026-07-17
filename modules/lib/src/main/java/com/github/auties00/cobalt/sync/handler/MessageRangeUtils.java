package com.github.auties00.cobalt.sync.handler;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessage;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageBuilder;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRange;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionMessageRangeBuilder;

import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.*;

/**
 * Utilities for comparing, merging, validating, and rewriting
 * {@link SyncActionMessageRange} objects attached to chat-scoped app state
 * mutations.
 *
 * <p>Chat-scoped sync actions (archive, clear-chat, delete-chat,
 * mark-chat-as-read) carry a message range that pins the action to a specific
 * window of messages by timestamp and key. The helpers here are consumed by
 * the matching handlers ({@link ArchiveChatHandler}, {@link ClearChatHandler},
 * {@link DeleteChatHandler}, {@link MarkChatAsReadHandler}) when they apply
 * incoming mutations, and by their mutation factories when they build the
 * outgoing range.
 *
 * @implNote
 * This implementation omits the WAM critical-event metric emissions that
 * {@code WAWebMessageRangeUtils.validateMessageRange} performs on each
 * invariant failure; Cobalt does not ship the WAM telemetry pipeline. The
 * {@code constructMessageRange}, {@code constructForwardMovingMessageRange}
 * and {@code lockForMessageRangeSync} exports are intentionally not adapted
 * here: they depend on the browser-only active message range infrastructure
 * that Cobalt does not maintain. The minimal outgoing range is derived
 * directly from the chat's newest in-memory message by the
 * {@code buildOutgoingMessageRange} helper on the client, and per-collection
 * serialization is enforced by the patch-push path so no explicit lock is
 * required.
 */
@WhatsAppWebModule(moduleName = "WAWebMessageRangeUtils")
final class MessageRangeUtils {
    /**
     * The logger for {@link MessageRangeUtils}.
     */
    private static final System.Logger LOGGER = Log.get(MessageRangeUtils.class);

    /**
     * The result of comparing two {@link SyncActionMessageRange} instances for
     * enclosure.
     *
     * <p>Returned by
     * {@link #compareMessageRanges(SyncActionMessageRange, SyncActionMessageRange)}
     * and consumed by chat-scoped handlers when reconciling a local pending
     * mutation against an incoming remote mutation; the constants encode which
     * side covers the other so the handler can decide whether to drop the
     * local, drop the remote, or merge.
     *
     * @implNote
     * This implementation mirrors the WA Web {@code MessageRangeEncloseType}
     * members one-for-one; the camel-case-to-screaming-snake rename is purely
     * stylistic.
     */
    @WhatsAppWebModule(moduleName = "WAWebMessageRangeUtils")
    enum EnclosureType {
        /**
         * Range A fully covers every message that range B covers.
         */
        RANGE_A_ENCLOSES_RANGE_B,

        /**
         * Range B fully covers every message that range A covers.
         */
        RANGE_B_ENCLOSES_RANGE_A,

        /**
         * Both ranges cover exactly the same set of messages.
         */
        RANGES_ARE_EQUAL,

        /**
         * Neither range encloses the other; the ranges either partially
         * overlap or are disjoint.
         */
        RANGES_NOT_ENCLOSING
    }

    /**
     * Prevents instantiation of this utility class whose members are all
     * {@code static}.
     */
    private MessageRangeUtils() {
    }

    /**
     * Compares two message ranges and returns their {@link EnclosureType}
     * relationship.
     *
     * <p>The two ranges are passed in the order they came from the wire;
     * nothing in the result implies a preferred side.
     *
     * @implNote
     * This implementation evaluates
     * {@link #encloses(SyncActionMessageRange, SyncActionMessageRange)} in both
     * directions and combines the two booleans into the four
     * {@link EnclosureType} values.
     *
     * @param rangeA the first range
     * @param rangeB the second range
     * @return the {@link EnclosureType} describing how {@code rangeA} and
     *         {@code rangeB} relate
     */
    @WhatsAppWebExport(
            moduleName = "WAWebMessageRangeUtils",
            exports = "compareMessageRanges",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    static EnclosureType compareMessageRanges(SyncActionMessageRange rangeA, SyncActionMessageRange rangeB) {
        var aEnclosesB = encloses(rangeA, rangeB);
        var bEnclosesA = encloses(rangeB, rangeA);
        EnclosureType result;
        if (aEnclosesB && bEnclosesA) {
            result = EnclosureType.RANGES_ARE_EQUAL;
        } else if (aEnclosesB) {
            result = EnclosureType.RANGE_A_ENCLOSES_RANGE_B;
        } else if (bEnclosesA) {
            result = EnclosureType.RANGE_B_ENCLOSES_RANGE_A;
        } else {
            result = EnclosureType.RANGES_NOT_ENCLOSING;
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "compare message ranges: {0}", result);
        return result;
    }

    /**
     * Merges two message ranges into a new range covering the union of both.
     *
     * <p>The result has the later {@link SyncActionMessageRange#lastMessageTimestamp()},
     * the deduplicated message list after the threshold, and the later
     * {@link SyncActionMessageRange#lastSystemMessageTimestamp()} when it
     * strictly exceeds the merged {@code lastMessageTimestamp}.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code mergeMessageRanges} exactly:
     * <ul>
     *   <li>{@code lastMessageTimestamp} is the maximum of both inputs;</li>
     *   <li>messages are merged via {@link #mergeMessages(List, List, long)}
     *       which keeps only entries with timestamp greater than or equal to
     *       the merged value, deduplicated by key id with the higher timestamp
     *       winning;</li>
     *   <li>{@code lastSystemMessageTimestamp} is propagated only when at least
     *       one input has it AND the merged value strictly exceeds the merged
     *       {@code lastMessageTimestamp}.</li>
     * </ul>
     *
     * @param rangeA the first range
     * @param rangeB the second range
     * @return a new range covering the union of {@code rangeA} and
     *         {@code rangeB}
     */
    @WhatsAppWebExport(
            moduleName = "WAWebMessageRangeUtils",
            exports = "mergeMessageRanges",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    static SyncActionMessageRange mergeMessageRanges(SyncActionMessageRange rangeA, SyncActionMessageRange rangeB) {
        var aLastTimestamp = toEpochSeconds(rangeA.lastMessageTimestamp().orElse(null));
        var bLastTimestamp = toEpochSeconds(rangeB.lastMessageTimestamp().orElse(null));
        var maxTimestamp = Math.max(aLastTimestamp, bLastTimestamp);

        var mergedMessages = mergeMessages(rangeA.messages(), rangeB.messages(), maxTimestamp);

        var builder = new SyncActionMessageRangeBuilder()
                .messages(mergedMessages);

        if (maxTimestamp != 0) {
            builder.lastMessageTimestamp(Instant.ofEpochSecond(maxTimestamp));
        }

        var aSystemTimestamp = toEpochSeconds(rangeA.lastSystemMessageTimestamp().orElse(null));
        var bSystemTimestamp = toEpochSeconds(rangeB.lastSystemMessageTimestamp().orElse(null));
        if (aSystemTimestamp != 0 || bSystemTimestamp != 0) {
            var maxSystemTimestamp = Math.max(aSystemTimestamp, bSystemTimestamp);
            if (maxSystemTimestamp > maxTimestamp) {
                builder.lastSystemMessageTimestamp(Instant.ofEpochSecond(maxSystemTimestamp));
            }
        }

        if (Log.TRACE)
            LOGGER.log(Level.TRACE, "merge message ranges: {0} + {1} messages -> {2} merged", rangeA.messages().size(), rangeB.messages().size(), mergedMessages.size());
        return builder.build();
    }

    /**
     * Validates a message range and returns it unchanged when the input is
     * non-{@code null}.
     *
     * <p>This is the pre-apply gate chat-scoped handlers call before applying
     * a mutation. Cobalt does not ship the field-level invariant checks WA Web
     * emits as WAM telemetry, so the only observable outcome is {@code null}
     * in / {@code null} out; any non-{@code null} input is returned as-is.
     *
     * @implNote
     * This implementation drops every per-invariant metric upload from
     * {@code WAWebMessageRangeUtils.validateMessageRange}: the metric pipeline
     * is not implemented in Cobalt. The field-level checks the WA Web validator
     * performs (system-timestamp bound, 1000-message limit,
     * key/remoteJid/fromMe/id presence) are deferred to the calling handler,
     * which already null-guards the range via {@code Optional.orElse(null)} on
     * the action.
     *
     * @param messageRange the message range to validate; may be {@code null}
     * @return the same range when {@code messageRange} is non-{@code null};
     *         otherwise {@code null}
     */
    @WhatsAppWebExport(
            moduleName = "WAWebMessageRangeUtils",
            exports = "validateMessageRange",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    static SyncActionMessageRange validateMessageRange(SyncActionMessageRange messageRange) {
        if (messageRange == null) {
            return null;
        }
        return messageRange;
    }

    /**
     * Returns a copy of the given message range with every message key's
     * {@code parentJid} replaced by {@code remoteJid}.
     *
     * <p>Consumed by {@link ClearChatHandler} and {@link DeleteChatHandler} to
     * swap the per-message remote JID for the resolved local chat JID before
     * applying the range to the chat database. The action's mutation index can
     * reference a JID different from the canonical chat id; the rewrite keeps
     * every downstream consumer indexing into the same chat. Both
     * {@link SyncActionMessageRange#lastMessageTimestamp()} and
     * {@link SyncActionMessageRange#lastSystemMessageTimestamp()} are carried
     * through unchanged.
     *
     * @implNote
     * This implementation mirrors WA Web's {@code replaceMessageRangeRemoteJid}
     * by rebuilding each
     * {@link com.github.auties00.cobalt.wire.core.message.MessageKey} with the
     * supplied {@link Jid} stamped into {@code parentJid}, preserving the
     * existing {@code fromMe}, {@code id}, and {@code senderJid} fields.
     *
     * @param remoteJid    the JID to stamp into every message key
     * @param messageRange the range whose messages should be rewritten
     * @return a new range with the same timestamps and rewritten message keys
     */
    @WhatsAppWebExport(
            moduleName = "WAWebMessageRangeUtils",
            exports = "replaceMessageRangeRemoteJid",
            adaptation = WhatsAppAdaptation.DIRECT
    )
    static SyncActionMessageRange replaceMessageRangeRemoteJid(Jid remoteJid, SyncActionMessageRange messageRange) {
        var rewrittenMessages = new ArrayList<SyncActionMessage>(messageRange.messages().size());
        for (var msg : messageRange.messages()) {
            var existingKey = msg.key().orElse(null);
            var newKeyBuilder = new MessageKeyBuilder()
                    .parentJid(remoteJid);
            if (existingKey != null) {
                newKeyBuilder.fromMe(existingKey.fromMe());
                existingKey.id().ifPresent(newKeyBuilder::id);
                existingKey.senderJid().ifPresent(newKeyBuilder::senderJid);
            }
            var newMsgBuilder = new SyncActionMessageBuilder()
                    .key(newKeyBuilder.build());
            msg.timestamp().ifPresent(newMsgBuilder::timestamp);
            rewrittenMessages.add(newMsgBuilder.build());
        }

        var builder = new SyncActionMessageRangeBuilder()
                .messages(rewrittenMessages);
        messageRange.lastMessageTimestamp().ifPresent(builder::lastMessageTimestamp);
        messageRange.lastSystemMessageTimestamp().ifPresent(builder::lastSystemMessageTimestamp);
        if (Log.TRACE)
            LOGGER.log(Level.TRACE, "replace message range remote jid: {0}, {1} messages rewritten", remoteJid, rewrittenMessages.size());
        return builder.build();
    }

    /**
     * Concatenates and deduplicates two message lists, keeping only entries
     * whose timestamp is greater than or equal to {@code maxTimestamp}.
     *
     * @implNote
     * This implementation mirrors the WA Web {@code mergeMessages} helper. For
     * every key id, the message with the higher timestamp wins; messages whose
     * timestamp falls below the threshold are dropped. A missing timestamp is
     * treated as {@code 0}.
     *
     * @param messagesA    the first message list
     * @param messagesB    the second message list
     * @param maxTimestamp the threshold timestamp (epoch seconds); entries
     *                     strictly below this value are filtered out
     * @return the merged and deduplicated message list
     */
    private static List<SyncActionMessage> mergeMessages(List<SyncActionMessage> messagesA, List<SyncActionMessage> messagesB, long maxTimestamp) {
        var byKeyId = new LinkedHashMap<String, SyncActionMessage>();
        var combined = new ArrayList<SyncActionMessage>(messagesA.size() + messagesB.size());
        combined.addAll(messagesA);
        combined.addAll(messagesB);

        for (var msg : combined) {
            var keyId = msg.key()
                    .flatMap(key -> key.id())
                    .orElse("");
            var msgTimestamp = toEpochSeconds(msg.timestamp().orElse(null));

            if (msgTimestamp >= maxTimestamp) {
                var existing = byKeyId.get(keyId);
                if (existing != null) {
                    var existingTimestamp = toEpochSeconds(existing.timestamp().orElse(null));
                    if (existingTimestamp < msgTimestamp) {
                        byKeyId.put(keyId, msg);
                    }
                } else {
                    byKeyId.put(keyId, msg);
                }
            }
        }

        return new ArrayList<>(byKeyId.values());
    }

    /**
     * Returns whether {@code encloser} fully covers every message that
     * {@code enclosed} references.
     *
     * <p>A message in {@code enclosed} is accounted for when its key id is
     * present in {@code encloser}'s message list, OR when its timestamp is
     * strictly less than {@code encloser}'s
     * {@link SyncActionMessageRange#lastMessageTimestamp()}. A message with no
     * key id and no timestamp is unconditionally treated as not-enclosed.
     *
     * @implNote
     * This implementation mirrors the WA Web enclosure helper bit-for-bit.
     *
     * @param encloser the range that should enclose
     * @param enclosed the range that should be enclosed
     * @return {@code true} if {@code encloser} encloses {@code enclosed}
     */
    private static boolean encloses(SyncActionMessageRange encloser, SyncActionMessageRange enclosed) {
        var encloserLastTimestamp = toEpochSeconds(encloser.lastMessageTimestamp().orElse(null));

        var encloserKeyIds = new HashSet<String>();
        for (var msg : encloser.messages()) {
            msg.key()
                    .flatMap(key -> key.id())
                    .ifPresent(encloserKeyIds::add);
        }

        for (var msg : enclosed.messages()) {
            var keyId = msg.key()
                    .flatMap(key -> key.id())
                    .orElse(null);
            var msgTimestamp = msg.timestamp().orElse(null);

            if (keyId != null && encloserKeyIds.contains(keyId)) {
                continue;
            }

            if (msgTimestamp == null) {
                return false;
            }

            if (encloserLastTimestamp <= toEpochSeconds(msgTimestamp)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Converts an {@link Instant} to epoch seconds, treating {@code null} as
     * {@code 0}.
     *
     * @implNote
     * This implementation matches the WA Web convention of treating a missing
     * timestamp as {@code 0} rather than throwing.
     *
     * @param instant the instant to convert; may be {@code null}
     * @return the epoch seconds, or {@code 0} when {@code instant} is
     *         {@code null}
     */
    private static long toEpochSeconds(Instant instant) {
        return instant != null ? instant.getEpochSecond() : 0;
    }
}
