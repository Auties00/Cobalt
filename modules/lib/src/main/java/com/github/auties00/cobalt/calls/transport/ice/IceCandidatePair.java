package com.github.auties00.cobalt.calls.transport.ice;

import java.util.Objects;

/**
 * Holds one ICE candidate pair in the checklist: a local and a remote {@link IceCandidate} with the
 * RFC 8445 pair priority and the connectivity check {@link IceCheckState state}.
 *
 * <p>ICE forms a pair from each compatible local and remote candidate, computes the pair priority from
 * the two candidate priorities and the controlling role, sorts the checklist by that priority, and runs
 * connectivity checks pair by pair. This class is one mutable checklist entry: its candidates and
 * priority are fixed at construction, while its {@link #state() state} advances as its check runs and a
 * controlling agent records nomination through {@link #nominate()}.
 *
 * <p>A pair is rejected before it enters the checklist when its candidates use different
 * {@link IceCandidate.Protocol protocols} or name the same address; that filtering is the agent's, and
 * {@link #isCompatible(IceCandidate, IceCandidate)} exposes the test.
 *
 * <p>The pair is mutated only by the single ICE thread and is not thread safe.
 *
 * @implNote This implementation computes the pair priority from the RFC 8445 formula
 *           {@code 2^32 * min(G, D) + 2 * max(G, D) + (G > D ? 1 : 0)}, where {@code G} is the
 *           controlling agent's candidate priority and {@code D} the controlled agent's; the checklist
 *           is sorted descending by that priority.
 */
public final class IceCandidatePair {
    /**
     * Holds the local candidate of the pair.
     */
    private final IceCandidate local;

    /**
     * Holds the remote candidate of the pair.
     */
    private final IceCandidate remote;

    /**
     * Holds the RFC 8445 pair priority computed at construction.
     */
    private final long priority;

    /**
     * Holds the connectivity check state, advancing from {@link IceCheckState#FROZEN}.
     */
    private IceCheckState state;

    /**
     * Holds whether a controlling agent has nominated this pair to carry media.
     */
    private boolean nominated;

    /**
     * Constructs a pair from its candidates and the controlling role, computing the pair priority.
     *
     * <p>The new pair starts {@link IceCheckState#FROZEN} and not nominated.
     *
     * @param local       the local candidate
     * @param remote      the remote candidate
     * @param controlling whether the local agent is the controlling agent, which selects the priority
     *                    tiebreaker
     * @throws NullPointerException if {@code local} or {@code remote} is {@code null}
     */
    public IceCandidatePair(IceCandidate local, IceCandidate remote, boolean controlling) {
        this.local = Objects.requireNonNull(local, "local cannot be null");
        this.remote = Objects.requireNonNull(remote, "remote cannot be null");
        this.priority = computePairPriority(local.priority(), remote.priority(), controlling);
        this.state = IceCheckState.FROZEN;
    }

    /**
     * Computes the RFC 8445 candidate pair priority.
     *
     * <p>With {@code controlling} the local priority is {@code G} and the remote is {@code D},
     * otherwise the roles swap; the priority is
     * {@code 2^32 * min(G, D) + 2 * max(G, D) + (G > D ? 1 : 0)}.
     *
     * @param localPriority  the local candidate priority
     * @param remotePriority the remote candidate priority
     * @param controlling    whether the local agent is controlling
     * @return the pair priority
     */
    public static long computePairPriority(long localPriority, long remotePriority, boolean controlling) {
        var g = controlling ? localPriority : remotePriority;
        var d = controlling ? remotePriority : localPriority;
        var min = Math.min(g, d);
        var max = Math.max(g, d);
        return (min << 32) + (2 * max) + (g > d ? 1 : 0);
    }

    /**
     * Returns whether two candidates may form a pair.
     *
     * <p>They are compatible when they use the same {@link IceCandidate.Protocol protocol} and do not
     * name the same transport address; an incompatible combination is never added to the checklist.
     *
     * @param local  the local candidate
     * @param remote the remote candidate
     * @return {@code true} when the candidates may form a pair
     * @throws NullPointerException if {@code local} or {@code remote} is {@code null}
     */
    public static boolean isCompatible(IceCandidate local, IceCandidate remote) {
        Objects.requireNonNull(local, "local cannot be null");
        Objects.requireNonNull(remote, "remote cannot be null");
        if (local.protocol() != remote.protocol()) {
            return false;
        }
        return !local.address().equals(remote.address());
    }

    /**
     * Returns the local candidate of the pair.
     *
     * @return the local candidate
     */
    public IceCandidate local() {
        return local;
    }

    /**
     * Returns the remote candidate of the pair.
     *
     * @return the remote candidate
     */
    public IceCandidate remote() {
        return remote;
    }

    /**
     * Returns the RFC 8445 pair priority.
     *
     * @return the pair priority
     */
    public long priority() {
        return priority;
    }

    /**
     * Returns the current connectivity check state.
     *
     * @return the check state
     */
    public IceCheckState state() {
        return state;
    }

    /**
     * Sets the connectivity check state.
     *
     * @param state the new check state; never {@code null}
     * @throws NullPointerException if {@code state} is {@code null}
     */
    public void setState(IceCheckState state) {
        this.state = Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * Returns whether a controlling agent has nominated this pair.
     *
     * @return {@code true} when the pair is nominated to carry media
     */
    public boolean nominated() {
        return nominated;
    }

    /**
     * Marks this pair as nominated to carry media.
     *
     * <p>A controlling agent calls this when it sends or confirms a {@code USE-CANDIDATE} for the pair.
     */
    public void nominate() {
        this.nominated = true;
    }

    @Override
    public String toString() {
        return "IceCandidatePair[local=" + local.address()
                + ", remote=" + remote.address()
                + ", priority=0x" + Long.toHexString(priority)
                + ", state=" + state
                + ", nominated=" + nominated + ']';
    }
}
