package com.github.auties00.cobalt.calls.engine.participant;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads a call's participant set as immutable {@link ParticipantView} snapshots.
 *
 * <p>This is the flyweight seam the rest of the call engine uses to read participants
 * without touching the mutable {@link CallParticipant} aggregates or holding the
 * membership lock. An implementation snapshots its participants into {@link ParticipantView}
 * instances under its own lock and exposes them through this provider; every query method
 * here is defined in terms of {@link #views()} and {@link #selfView()}, so an implementation
 * only has to supply those two and the validity primitive {@link #isValid()}.
 *
 * <p>The query methods cover the self view, the first connected peer, lookup by active
 * device JID, the allocated participant count, and per stream subscriber counts. They never
 * return {@code null}; an absent participant is reported as an empty {@link Optional} or, for
 * the self view, as {@link ParticipantView#invalid()}.
 *
 * @implSpec An implementation MUST return snapshots taken under whatever lock guards its
 * participant set, so that each returned {@link ParticipantView} is internally consistent;
 * it MUST NOT leak a {@link CallParticipant} reference. {@link #views()} MUST return the
 * snapshots of every currently allocated participant (peers, self, and extensions), and
 * {@link #selfView()} MUST return the snapshot of the local participant, or
 * {@link ParticipantView#invalid()} when there is no self participant. The default query
 * methods rely on these contracts and re snapshot on each call, so a caller that needs a
 * stable set across several queries must capture {@link #views()} once.
 * @see ParticipantView
 * @see CallParticipant
 */
public interface ParticipantProvider {
    /**
     * Returns whether this provider is backed by a live participant set.
     *
     * @implSpec An implementation MUST return {@code true} only while it has a valid
     * participant set to snapshot, which requires that both the provider and its underlying
     * group info are present.
     * @return {@code true} if the provider can produce participant snapshots
     */
    boolean isValid();

    /**
     * Returns immutable snapshots of every currently allocated participant.
     *
     * @implSpec An implementation MUST take each snapshot under the lock guarding its
     * participant set and MUST include peers, the self participant, and any extensions. The
     * returned list MUST NOT contain a {@code null} element and SHOULD be unmodifiable.
     * @return the snapshots of all allocated participants; never {@code null}
     */
    List<ParticipantView> views();

    /**
     * Returns an immutable snapshot of the local (self) participant.
     *
     * @implSpec An implementation MUST return the snapshot of the local participant, or
     * {@link ParticipantView#invalid()} when there is no self participant; it MUST NOT
     * return {@code null}.
     * @return the self snapshot, or {@link ParticipantView#invalid()} if there is no self
     *         participant; never {@code null}
     */
    ParticipantView selfView();

    /**
     * Returns the number of currently allocated participants.
     *
     * <p>This counts every snapshot {@link #views()} returns, which includes peers, self,
     * and extensions.
     *
     * @return the number of allocated participants
     */
    default int allocatedParticipantCount() {
        return views().size();
    }

    /**
     * Returns the first connected peer participant.
     *
     * <p>A connected peer is a view that is valid, not the self participant, not an
     * extension, and {@linkplain ParticipantView#connected() connected}. This is the
     * canonical peer accessor for a two party call.
     *
     * @return an {@code Optional} holding the first connected peer view, or empty if none
     *         is connected
     */
    default Optional<ParticipantView> firstConnectedPeer() {
        var self = selfView();
        for (var view : views()) {
            if (view.isConnectedPeer() && !isSameParticipant(view, self)) {
                return Optional.of(view);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the first active peer participant.
     *
     * <p>An active peer is a view that is valid, not an extension, and
     * {@linkplain ParticipantView#active() active}, excluding the self participant. Unlike
     * {@link #firstConnectedPeer()} this admits transitional active states, not only the
     * fully connected state.
     *
     * @return an {@code Optional} holding the first active peer view, or empty if none is
     *         active
     */
    default Optional<ParticipantView> firstActivePeer() {
        var self = selfView();
        for (var view : views()) {
            if (view.valid() && view.active() && !view.extension() && !isSameParticipant(view, self)) {
                return Optional.of(view);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the view of the active participant using the given device JID.
     *
     * <p>The match is on the view's {@linkplain ParticipantView#deviceJid() active device
     * JID}; only valid, active views are considered.
     *
     * @param deviceJid the active device JID to look up; never {@code null}
     * @return an {@code Optional} holding the matching view, or empty if no active
     *         participant uses that device JID
     * @throws NullPointerException if {@code deviceJid} is {@code null}
     */
    default Optional<ParticipantView> activeParticipantByDeviceJid(Jid deviceJid) {
        Objects.requireNonNull(deviceJid, "deviceJid cannot be null");
        for (var view : views()) {
            if (view.valid() && view.active() && deviceJid.equals(view.deviceJid())) {
                return Optional.of(view);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the number of participants subscribed to the given encoded video stream id.
     *
     * <p>This counts every valid view whose
     * {@linkplain ParticipantView#subscribedVideoStreamId() subscribed encoded video stream
     * id} equals {@code streamId}.
     *
     * @param streamId the encoded video stream id to count subscribers for
     * @return the number of subscribers to the stream
     */
    default int videoStreamSubscriberCount(int streamId) {
        var count = 0;
        for (var view : views()) {
            if (view.valid() && view.subscribedVideoStreamId() == streamId) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns whether two views denote the same participant by user JID.
     *
     * <p>This compares the views' user JIDs, treating two views with no user JID as not the
     * same participant, and is used to exclude the self participant from peer iteration.
     *
     * @param view the candidate view
     * @param self the self view to compare against
     * @return {@code true} if both views carry the same user JID and it is not {@code null}
     */
    private static boolean isSameParticipant(ParticipantView view, ParticipantView self) {
        return view.userJid() != null && view.userJid().equals(self.userJid());
    }
}
