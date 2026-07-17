package com.github.auties00.cobalt.calls.signaling.relay;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the bounded, deduplicated relay list of a single call.
 *
 * <p>A call maintains at most {@value #MAX_RELAYS} relay candidates. Candidates are inserted or
 * updated through {@link #addOrUpdate(RelayCandidate)}, which keys on the relay identity pair (the
 * {@link RelayCandidate#relayId() relay id} and the {@link RelayCandidate#protoAfFlag() protocol
 * address family flag}): a candidate whose pair already exists replaces the stored one in place, and a
 * candidate with a new pair is appended. Two conditions are refused: a candidate whose pair matches a
 * stored relay but whose {@link RelayCandidate#portByte() port byte} differs, and a new candidate
 * offered once the list already holds {@value #MAX_RELAYS} entries.
 *
 * <p>Insertion order is preserved: an update keeps the existing slot, and an append goes to the end.
 * The list is not thread safe; the call's relay list is mutated only behind the call's lock. Iteration
 * runs over the {@link RelayCandidate} entries in insertion order.
 *
 * <p>The identity pair is the two bytes the dedupe scan compares, the {@link RelayCandidate#relayId()
 * relay id} and the {@link RelayCandidate#protoAfFlag() protocol address family flag}. The
 * {@link RelayCandidate#portByte() port byte} is not part of the match; on a matched relay a differing
 * port byte is a consistency failure rather than a distinct relay. The auth and encrypted relay tokens
 * of a matched slot are overwritten in place without comparison, so a token byte difference alone does
 * not cause a rejection. A candidate carrying neither an IPv4 nor an IPv6 address is rejected upstream
 * by the {@link RelayCandidate} constructor.
 *
 * @see RelayCandidate
 */
public final class RelayCandidateList implements Iterable<RelayCandidate> {
    /**
     * The maximum number of relay candidates a call may hold.
     */
    public static final int MAX_RELAYS = 8;

    /**
     * Holds the relay candidates in insertion order.
     *
     * <p>Backed by an {@link ArrayList} bounded to {@value #MAX_RELAYS} entries; the dedupe scan and
     * the slot preserving update both operate over this list.
     */
    private final List<RelayCandidate> candidates;

    /**
     * Constructs an empty relay list.
     */
    public RelayCandidateList() {
        this.candidates = new ArrayList<>(MAX_RELAYS);
    }

    /**
     * Inserts or updates a relay candidate, keyed by its relay identity pair.
     *
     * <p>When a stored candidate shares the given candidate's relay identity pair (its
     * {@link RelayCandidate#relayId() relay id} and {@link RelayCandidate#protoAfFlag() protocol
     * address family flag}), the stored candidate is replaced in place, preserving its slot; when no
     * stored candidate matches, the given candidate is appended. Before replacing, the
     * {@link RelayCandidate#portByte() port byte} is checked for consistency: a matched relay whose port
     * byte differs is rejected, because the same relay may not carry a different port byte. The tokens
     * of a matched slot are not compared; a matched candidate simply overwrites the stored one.
     *
     * @param candidate the candidate to insert or update; never {@code null}
     * @throws NullPointerException     if {@code candidate} is {@code null}
     * @throws IllegalStateException    if the candidate has a new identity pair and the list already
     *                                  holds {@value #MAX_RELAYS} entries
     * @throws IllegalArgumentException if the candidate matches a stored relay's identity pair but
     *                                  carries a different port byte
     */
    public void addOrUpdate(RelayCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        for (var index = 0; index < candidates.size(); index++) {
            var existing = candidates.get(index);
            if (sameRelay(existing, candidate)) {
                if (existing.portByte() != candidate.portByte()) {
                    throw new IllegalArgumentException("relay token cannot be different for the same relay");
                }
                candidates.set(index, candidate);
                return;
            }
        }
        if (candidates.size() >= MAX_RELAYS) {
            throw new IllegalStateException("relay list is full");
        }
        candidates.add(candidate);
    }

    /**
     * Returns the stored candidate sharing the relay identity pair of the given candidate, if any.
     *
     * <p>The identity pair is the {@link RelayCandidate#relayId() relay id} and the
     * {@link RelayCandidate#protoAfFlag() protocol address family flag}; the
     * {@link RelayCandidate#portByte() port byte} is not part of the match, so a stored relay with the
     * same pair but a different port byte is still returned.
     *
     * @param candidate the candidate whose identity pair to match; never {@code null}
     * @return an {@link Optional} holding the matching stored candidate, or empty when none matches
     * @throws NullPointerException if {@code candidate} is {@code null}
     */
    public Optional<RelayCandidate> find(RelayCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate cannot be null");
        for (var existing : candidates) {
            if (sameRelay(existing, candidate)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether two candidates share the relay identity pair the list deduplicates on.
     *
     * <p>The pair is the {@link RelayCandidate#relayId() relay id} and the
     * {@link RelayCandidate#protoAfFlag() protocol address family flag}; the
     * {@link RelayCandidate#portByte() port byte} is deliberately excluded, because a differing port
     * byte on a matched relay is a rejection rather than a distinct relay.
     *
     * @param left  the stored candidate; never {@code null}
     * @param right the candidate being inserted or looked up; never {@code null}
     * @return {@code true} when both candidates carry the same relay id and protocol address family flag
     */
    private static boolean sameRelay(RelayCandidate left, RelayCandidate right) {
        return left.relayId() == right.relayId()
                && left.protoAfFlag() == right.protoAfFlag();
    }

    /**
     * Returns the number of relay candidates currently held.
     *
     * @return the candidate count, never greater than {@value #MAX_RELAYS}
     */
    public int size() {
        return candidates.size();
    }

    /**
     * Returns whether this list holds no relay candidates.
     *
     * @return {@code true} when the list is empty
     */
    public boolean isEmpty() {
        return candidates.isEmpty();
    }

    /**
     * Returns whether this list cannot accept a candidate with a new relay identity pair.
     *
     * @return {@code true} when the list already holds {@value #MAX_RELAYS} entries
     */
    public boolean isFull() {
        return candidates.size() >= MAX_RELAYS;
    }

    /**
     * Returns an unmodifiable, point-in-time snapshot of the relay candidates in insertion order.
     *
     * <p>The returned list is detached from the backing store: a candidate inserted or updated through
     * {@link #addOrUpdate(RelayCandidate)} after this call is not reflected in a list already returned. This
     * matches {@link #iterator()}, which likewise iterates a snapshot. WhatsApp exposes no observable or live
     * relay candidate collection; the relay set is internal transport engine state consumed point-in-time, so
     * a snapshot is the faithful shape.
     *
     * @return the candidates; never {@code null}, possibly empty
     */
    public List<RelayCandidate> candidates() {
        return List.copyOf(candidates);
    }

    /**
     * Returns an iterator over the relay candidates in insertion order.
     *
     * <p>The iterator is read only; it does not support {@link Iterator#remove()}.
     *
     * @return an iterator over an immutable snapshot of the candidates
     */
    @Override
    public Iterator<RelayCandidate> iterator() {
        return List.copyOf(candidates).iterator();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayCandidateList that
                && this.candidates.equals(that.candidates));
    }

    @Override
    public int hashCode() {
        return candidates.hashCode();
    }

    @Override
    public String toString() {
        return "RelayCandidateList[size=" + candidates.size() + ", candidates=" + candidates + ']';
    }
}
