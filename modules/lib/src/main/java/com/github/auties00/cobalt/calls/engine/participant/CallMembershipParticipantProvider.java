package com.github.auties00.cobalt.calls.engine.participant;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ParticipantProvider} backed by a {@link CallMembership}'s per slot {@link CallParticipant}
 * aggregates.
 *
 * <p>This is the read seam {@link CallMembership#participantProvider()} hands out. It owns no participant
 * state of its own and holds only a reference to the membership manager, forwarding each query to the
 * manager's snapshot primitives. Every read takes a fresh snapshot under the membership lock, so a returned
 * {@link ParticipantView} is internally consistent and no live {@link CallParticipant} reference escapes; the
 * wider engine reads participants only through these snapshots. The default query accessors of
 * {@link ParticipantProvider} (first connected peer, active participant by device JID, per stream subscriber
 * count, allocated count) ride on the three primitives this class supplies: {@link #views()},
 * {@link #selfView()}, and {@link #isValid()}.
 *
 * <p>This provider is a thin view over its membership manager: it is as thread safe as the manager, since
 * every method it implements forwards to a manager method that takes the per call membership lock. It is
 * cheap to construct, so a caller may obtain a fresh one per use rather than caching it.
 *
 * @see CallMembership#participantProvider()
 * @see ParticipantProvider
 */
final class CallMembershipParticipantProvider implements ParticipantProvider {
    /**
     * The membership manager whose per slot aggregates this provider snapshots on each read.
     */
    private final CallMembership membership;

    /**
     * Constructs a provider over the given membership manager.
     *
     * @param membership the membership manager to read participant snapshots from
     * @throws NullPointerException if {@code membership} is {@code null}
     */
    CallMembershipParticipantProvider(CallMembership membership) {
        this.membership = Objects.requireNonNull(membership, "membership cannot be null");
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reports validity as {@link CallMembership#hasParticipants()}, so the
     * provider is valid exactly while the membership holds at least one allocated slot.
     */
    @Override
    public boolean isValid() {
        return membership.hasParticipants();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns {@link CallMembership#participantViews()}, a fresh snapshot taken
     * under the membership lock so the returned list of {@link ParticipantView} is internally consistent and
     * exposes no live {@link CallParticipant} reference.
     */
    @Override
    public List<ParticipantView> views() {
        return membership.participantViews();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns {@link CallMembership#participantSelfView()}, the snapshot of the
     * local participant taken under the membership lock.
     */
    @Override
    public ParticipantView selfView() {
        return membership.participantSelfView();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation delegates to {@link CallMembership#firstActivePeerView()}, which scans the
     * self slot and every member slot under a single membership lock acquisition, so the returned peer is
     * selected against a self snapshot taken in the same critical section rather than the two separate lock
     * acquisitions the {@link ParticipantProvider#firstActivePeer() default} takes through {@link #selfView()}
     * and {@link #views()}. Running the participant projection and the in call control dispatch under one lock
     * (in {@code LifecycleController.handlePeerVideoState} and {@code handlePeerScreenShare}) is a separate
     * lifecycle controller change and is not folded in here.
     */
    @Override
    public Optional<ParticipantView> firstActivePeer() {
        return membership.firstActivePeerView();
    }
}
