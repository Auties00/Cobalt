package com.github.auties00.cobalt.calls.signaling.receive;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.call.CallEndReason;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Buffers early or out of order inbound call signaling per call identifier and tracks the per call
 * identifier transaction id, terminate reason, and accepted elsewhere flag used to suppress stale or
 * replayed messages.
 *
 * <p>The voip engine receives signaling for a call before the local call object exists (the offer
 * itself, or transport and rekey messages that race ahead of the offer's handling) and must both
 * replay those messages in arrival order once the call is created and reject messages whose
 * transaction id is older than the newest one already seen. This buffer reproduces that two part
 * mechanism: a bounded ring records, per call identifier, the latest transaction id (newest wins),
 * the mapped terminate reason, and whether the call was answered or declined on another device of the
 * same account, while a per call identifier message list holds the not yet routable {@link Stanza}
 * payloads in arrival order for later replay.
 *
 * <p>The transaction id ring is bounded to thirty slots keyed by call identifier; a call identifier
 * not yet present evicts the oldest slot when the ring is full, matching the fixed ring that wraps its
 * write cursor modulo thirty. The buffered message list is keyed by the same call identifier and is
 * unbounded per call (it is drained when the call object is created and discarded if the call never
 * materialises), so a flood of early messages for one identifier grows only that identifier's list and
 * the ring slot is reused rather than duplicated.
 *
 * <p>Transaction id ordering decisions go through {@link #isStaleTransactionId(String, int)}: a
 * message whose transaction id is strictly less than the recorded latest is stale and must be
 * dropped; a message whose transaction id is greater advances the recorded latest. The terminate
 * reason and accepted elsewhere flag let a late offer be answered with the already known outcome
 * (terminated, or accepted elsewhere) instead of ringing.
 *
 * <p>This type is internally synchronized: the socket reader, the per call ordered handler chain, and
 * the call creation path all touch it, so every public method holds the instance monitor for the
 * duration of its ring or list mutation. Buffered {@link Stanza} payloads are immutable, so handing a
 * drained list to the caller leaks no mutable state.
 *
 * @implNote This implementation models the absent transaction id as {@code -1} rather than the wire
 * sentinel because recorded transaction ids are nonnegative, and holds the immutable {@link Stanza}
 * payload directly instead of copying it, so buffering a message allocates no defensive copy. The
 * fixed ring write cursor is reproduced by an insertion ordered {@link LinkedHashMap} whose
 * {@code removeEldestEntry} override evicts the eldest identifier when a new one arrives at capacity.
 */
public final class CallMessageBuffer {
    /**
     * The logger for {@link CallMessageBuffer}.
     */
    private static final System.Logger LOGGER = Log.get(CallMessageBuffer.class);

    /**
     * The number of call identifier slots in the transaction id ring.
     *
     * <p>The ring wraps its write cursor modulo this value, so at most this many distinct call
     * identifiers carry recorded transaction id, terminate reason, and accepted elsewhere state at
     * once; the oldest is evicted when a new identifier arrives at capacity.
     */
    private static final int RING_SLOTS = 30;

    /**
     * The sentinel transaction id meaning no transaction id has been recorded for a call identifier.
     *
     * <p>Absence is modelled as {@code -1} because the recorded transaction ids are nonnegative wire
     * values.
     */
    private static final int NO_TRANSACTION_ID = -1;

    /**
     * Records the transaction id, terminate reason, and accepted elsewhere flag for one ring slot.
     *
     * @param transactionId      the latest recorded transaction id, or {@code -1} when none has been
     *                           recorded
     * @param terminateReason    the recorded terminate reason, or {@code null} when none has been
     *                           recorded
     * @param acceptedElsewhere  whether the call was answered or declined on another device of the
     *                           same account
     */
    private record RingSlot(int transactionId, CallEndReason terminateReason, boolean acceptedElsewhere) {
        /**
         * The empty slot carrying no transaction id, no terminate reason, and a cleared accepted
         * elsewhere flag.
         */
        private static final RingSlot EMPTY = new RingSlot(NO_TRANSACTION_ID, null, false);

        /**
         * Returns a copy of this slot with the transaction id replaced.
         *
         * @param transactionId the new latest transaction id
         * @return a slot carrying the new transaction id and this slot's other fields
         */
        private RingSlot withTransactionId(int transactionId) {
            return new RingSlot(transactionId, terminateReason, acceptedElsewhere);
        }

        /**
         * Returns a copy of this slot with the terminate reason replaced.
         *
         * @param terminateReason the new terminate reason
         * @return a slot carrying the new terminate reason and this slot's other fields
         */
        private RingSlot withTerminateReason(CallEndReason terminateReason) {
            return new RingSlot(transactionId, terminateReason, acceptedElsewhere);
        }

        /**
         * Returns a copy of this slot with the accepted elsewhere flag replaced.
         *
         * @param acceptedElsewhere the new accepted elsewhere flag
         * @return a slot carrying the new flag and this slot's other fields
         */
        private RingSlot withAcceptedElsewhere(boolean acceptedElsewhere) {
            return new RingSlot(transactionId, terminateReason, acceptedElsewhere);
        }
    }

    /**
     * Holds the ring slots keyed by call identifier, ordered by insertion so the oldest key is
     * evicted when the ring reaches {@link #RING_SLOTS}.
     *
     * <p>This is an insertion ordered {@link LinkedHashMap} whose {@code removeEldestEntry} override
     * evicts the eldest key only when a new key pushes the size past {@link #RING_SLOTS}; because the
     * map is not in access order, a lookup or an in place slot update never reorders or evicts,
     * matching the fixed ring write cursor that overwrites by slot rather than by recency.
     */
    private final Map<String, RingSlot> ring;

    /**
     * Holds the buffered, not yet routable message payloads per call identifier in arrival order.
     */
    private final Map<String, List<Stanza>> bufferedMessages;

    /**
     * Constructs an empty buffer with no recorded ring slots and no buffered messages.
     */
    public CallMessageBuffer() {
        this.ring = new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RingSlot> eldest) {
                return size() > RING_SLOTS;
            }
        };
        this.bufferedMessages = new HashMap<>();
    }

    /**
     * Records the transaction id of a message for a call identifier, keeping the newest.
     *
     * <p>Inserts a ring slot for the call identifier when none exists (evicting the oldest slot when
     * the ring is full), then advances the recorded transaction id to {@code transactionId} only when
     * it is greater than the recorded latest, so an out of order older message never lowers the
     * recorded value. A negative {@code transactionId} is ignored because the wire transaction ids are
     * nonnegative.
     *
     * @param callId        the call identifier
     * @param transactionId the transaction id carried by the message
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized void recordTransactionId(String callId, int transactionId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        if (transactionId < 0) {
            return;
        }
        var slot = slotFor(callId);
        if (transactionId > slot.transactionId()) {
            ring.put(callId, slot.withTransactionId(transactionId));
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "call {0} transaction id advanced to {1}", callId, transactionId);
            }
        }
    }

    /**
     * Returns the latest recorded transaction id for a call identifier, if any.
     *
     * @param callId the call identifier
     * @return an {@link OptionalInt} holding the recorded transaction id, or empty when none has been
     *         recorded for the identifier
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized OptionalInt transactionId(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var slot = ring.get(callId);
        if (slot == null || slot.transactionId() == NO_TRANSACTION_ID) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(slot.transactionId());
    }

    /**
     * Returns whether a message's transaction id is stale for a call identifier.
     *
     * <p>A transaction id is stale when it is strictly less than the latest transaction id already
     * recorded for the call identifier; such a message has been superseded and must be dropped. A
     * transaction id equal to or greater than the recorded latest, and any transaction id for a call
     * identifier with no recorded transaction id, is not stale. This method does not advance the
     * recorded transaction id; the caller advances it through {@link #recordTransactionId(String, int)}
     * once it decides to process the message.
     *
     * @param callId        the call identifier
     * @param transactionId the transaction id carried by the message
     * @return {@code true} when the transaction id is older than the recorded latest, {@code false}
     *         otherwise
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized boolean isStaleTransactionId(String callId, int transactionId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var slot = ring.get(callId);
        if (slot == null || slot.transactionId() == NO_TRANSACTION_ID) {
            return false;
        }
        var stale = transactionId < slot.transactionId();
        if (Log.DEBUG && stale) {
            LOGGER.log(Level.DEBUG, "call {0} transaction id {1} is stale, latest is {2}",
                    callId, transactionId, slot.transactionId());
        }
        return stale;
    }

    /**
     * Records the terminate reason for a call identifier.
     *
     * <p>Inserts a ring slot for the call identifier when none exists (evicting the oldest slot when
     * the ring is full), then stores the mapped terminate reason so a late offer for the same call can
     * be answered as already terminated rather than ringing.
     *
     * @param callId the call identifier
     * @param reason the terminate reason to record
     * @throws NullPointerException if {@code callId} or {@code reason} is {@code null}
     */
    public synchronized void recordTerminateReason(String callId, CallEndReason reason) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        ring.put(callId, slotFor(callId).withTerminateReason(reason));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} terminate reason recorded: {1}", callId, reason);
    }

    /**
     * Returns the recorded terminate reason for a call identifier, if any.
     *
     * @param callId the call identifier
     * @return an {@link Optional} holding the recorded terminate reason, or empty when none has been
     *         recorded for the identifier
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized Optional<CallEndReason> terminateReason(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var slot = ring.get(callId);
        return slot == null ? Optional.empty() : Optional.ofNullable(slot.terminateReason());
    }

    /**
     * Records that a call was answered or declined on another device of the same account.
     *
     * <p>Inserts a ring slot for the call identifier when none exists (evicting the oldest slot when
     * the ring is full), then sets the accepted elsewhere flag so a late offer for the same call is
     * not surfaced to the user as ringing.
     *
     * @param callId the call identifier
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized void recordAcceptedElsewhere(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        ring.put(callId, slotFor(callId).withAcceptedElsewhere(true));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} recorded as accepted elsewhere", callId);
    }

    /**
     * Returns whether a call was answered or declined on another device of the same account.
     *
     * @param callId the call identifier
     * @return {@code true} when the accepted elsewhere flag is set for the identifier, {@code false}
     *         otherwise
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized boolean isAcceptedElsewhere(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var slot = ring.get(callId);
        return slot != null && slot.acceptedElsewhere();
    }

    /**
     * Buffers a not yet routable message payload for a call identifier.
     *
     * <p>Appends the payload to the call identifier's message list in arrival order; the list is
     * drained in the same order by {@link #drainBufferedMessages(String)} once the call object exists.
     * The payload is the immutable {@code <call>} child {@link Stanza}, so no defensive copy is made.
     *
     * @param callId  the call identifier the payload belongs to
     * @param payload the {@code <call>} child stanza to buffer
     * @throws NullPointerException if {@code callId} or {@code payload} is {@code null}
     */
    public synchronized void buffer(String callId, Stanza payload) {
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        var messages = bufferedMessages.computeIfAbsent(callId, ignored -> new ArrayList<>());
        messages.add(payload);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "call {0} buffered message, pending count={1}", callId, messages.size());
    }

    /**
     * Removes and returns every buffered payload for a call identifier in arrival order.
     *
     * <p>The caller replays the returned payloads through the normal dispatch path once the call
     * object exists; subsequent calls for the same identifier return an empty list until new payloads
     * are buffered. The returned list is the drained list the caller owns exclusively, wrapped
     * unmodifiable; it is no longer referenced by this buffer.
     *
     * @param callId the call identifier whose buffered payloads to drain
     * @return the buffered payloads in arrival order, or an empty list when none are buffered
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized List<Stanza> drainBufferedMessages(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var messages = bufferedMessages.remove(callId);
        if (Log.DEBUG && messages != null && !messages.isEmpty()) {
            LOGGER.log(Level.DEBUG, "call {0} drained {1} buffered messages", callId, messages.size());
        }
        return messages == null ? List.of() : Collections.unmodifiableList(messages);
    }

    /**
     * Returns whether any payloads are buffered for a call identifier.
     *
     * @param callId the call identifier
     * @return {@code true} when at least one payload is buffered for the identifier, {@code false}
     *         otherwise
     * @throws NullPointerException if {@code callId} is {@code null}
     */
    public synchronized boolean hasBufferedMessages(String callId) {
        Objects.requireNonNull(callId, "callId cannot be null");
        var messages = bufferedMessages.get(callId);
        return messages != null && !messages.isEmpty();
    }

    /**
     * Clears every ring slot and buffered message.
     *
     * <p>Invoked on socket teardown so a new connection starts with no recorded transaction ids,
     * terminate reasons, accepted elsewhere flags, or buffered messages from the previous connection.
     */
    public synchronized void reset() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "resetting call message buffer: {0} ring slots, {1} pending call ids",
                    ring.size(), bufferedMessages.size());
        }
        ring.clear();
        bufferedMessages.clear();
    }

    /**
     * Returns the ring slot for a call identifier, inserting an empty slot and evicting the oldest
     * identifier when the ring is at capacity.
     *
     * <p>A newly inserted identifier is appended in insertion order; when the ring already holds
     * {@link #RING_SLOTS} identifiers, the map's {@code removeEldestEntry} override removes the eldest
     * identifier and its slot as the new one is inserted so the ring never exceeds its bound. An
     * identifier already present is returned unchanged and does not refresh its insertion position,
     * matching the fixed ring write cursor that overwrites by slot rather than by recency.
     *
     * @param callId the call identifier
     * @return the existing or newly inserted ring slot for the identifier
     */
    private RingSlot slotFor(String callId) {
        var existing = ring.get(callId);
        if (existing != null) {
            return existing;
        }
        ring.put(callId, RingSlot.EMPTY);
        return RingSlot.EMPTY;
    }
}
