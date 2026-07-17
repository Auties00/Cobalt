package com.github.auties00.cobalt.wire.linked.call;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the in-call interactions exchanged during a call to update peer-visible state without
 * touching the media path.
 *
 * <p>An interaction is a small control exchange: an emoji reaction, a raise-hand gesture, a mute
 * request, a keyframe request, or a video upgrade request. This sealed hierarchy enumerates every
 * kind and captures the fields each one carries; {@link #wireKind()} yields the literal that
 * discriminates the kind on the wire. The permitted variants are exhaustive, so a {@code switch} over
 * a {@code CallInteraction} needs no default branch.
 */
public sealed interface CallInteraction
        permits CallInteraction.Reaction,
        CallInteraction.RaiseHand,
        CallInteraction.LowerHand,
        CallInteraction.PeerMuteRequest,
        CallInteraction.KeyFrameRequest,
        CallInteraction.VideoUpgradeRequest {

    /**
     * Returns the {@code kind} literal that discriminates this interaction on the wire.
     *
     * @implSpec Implementations return a stable, non-{@code null} literal unique to their kind; the
     * value is part of the wire contract and must not vary between instances of the same variant.
     * @return the wire kind
     */
    String wireKind();

    /**
     * Represents an emoji reaction broadcast to every other participant.
     *
     * @param emoji the emoji string, typically a single grapheme
     */
    record Reaction(String emoji) implements CallInteraction {
        /**
         * Constructs a reaction, rejecting a {@code null} or empty emoji.
         *
         * @throws NullPointerException     if {@code emoji} is {@code null}
         * @throws IllegalArgumentException if {@code emoji} is empty
         */
        public Reaction {
            Objects.requireNonNull(emoji, "emoji cannot be null");
            if (emoji.isEmpty()) {
                throw new IllegalArgumentException("emoji cannot be empty");
            }
        }

        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "reaction"}
         */
        @Override
        public String wireKind() {
            return "reaction";
        }
    }

    /**
     * Represents a raise-hand gesture, typically shown in group calls when a participant wants to be
     * unmuted.
     */
    record RaiseHand() implements CallInteraction {
        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "raise_hand"}
         */
        @Override
        public String wireKind() {
            return "raise_hand";
        }
    }

    /**
     * Represents a lower-hand gesture that clears a previously-raised hand.
     */
    record LowerHand() implements CallInteraction {
        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "lower_hand"}
         */
        @Override
        public String wireKind() {
            return "lower_hand";
        }
    }

    /**
     * Represents an admin's request that a participant mute themselves.
     *
     * <p>The target participant is identified so the receiving peer can decide whether the request
     * applies to it. The reason is optional explanatory text.
     *
     * @param target the participant being asked to mute, in string form of the peer's JID
     * @param reason optional reason text; may be {@link Optional#empty()}
     */
    record PeerMuteRequest(String target, Optional<String> reason) implements CallInteraction {
        /**
         * Constructs a mute request, rejecting {@code null} fields.
         *
         * @throws NullPointerException if {@code target} or {@code reason} is {@code null}
         */
        public PeerMuteRequest {
            Objects.requireNonNull(target, "target cannot be null");
            Objects.requireNonNull(reason, "reason cannot be null");
        }

        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "peer_mute_request"}
         */
        @Override
        public String wireKind() {
            return "peer_mute_request";
        }
    }

    /**
     * Represents a request that the peer emit an immediate video keyframe.
     *
     * <p>When such a request arrives, the call layer drives the video encoder to emit a keyframe so
     * the next encoded frame lets the peer resynchronize its decoder.
     */
    record KeyFrameRequest() implements CallInteraction {
        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "keyframe_request"}
         */
        @Override
        public String wireKind() {
            return "keyframe_request";
        }
    }

    /**
     * Represents a request to renegotiate an audio-only call into an audio-and-video call.
     *
     * <p>The request carries no payload fields; the receiver decides whether to accept or reject the
     * upgrade.
     */
    record VideoUpgradeRequest() implements CallInteraction {
        /**
         * {@inheritDoc}
         *
         * @return the literal {@code "video_upgrade_request"}
         */
        @Override
        public String wireKind() {
            return "video_upgrade_request";
        }
    }
}
