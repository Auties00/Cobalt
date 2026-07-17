package com.github.auties00.cobalt.wam.model;

import com.github.auties00.cobalt.wam.binary.WamEventDecoder;
import com.github.auties00.cobalt.wam.binary.WamEventEncoder;

/**
 * A base interface for all WhatsApp Metrics (WAM) event types, providing
 * methods to calculate the binary-encoded size, write the event into a
 * {@link WamEventEncoder}, and query event metadata.
 *
 * <p>Each {@code @WamEvent}-annotated interface extends this interface.
 * The annotation processor generates an implementation class that
 * provides high-performance, zero-reflection implementations of all
 * methods using hardcoded literal values from the annotation and direct
 * calls to {@link WamEventEncoder}.
 *
 * @see com.github.auties00.cobalt.wam.annotation.WamEvent
 * @see WamEventEncoder
 * @see WamEventDecoder
 */
public interface WamEventSpec {
    /**
     * Returns the numeric event identifier assigned by WhatsApp.
     *
     * @return the event id
     */
    int id();

    /**
     * Returns the transport channel for this event.
     *
     * @return the channel
     */
    WamChannel channel();

    /**
     * Returns the sampling weight for alpha (internal) builds.
     *
     * @return the alpha build sampling weight
     */
    int alphaWeight();

    /**
     * Returns the sampling weight for beta builds.
     *
     * @return the beta build sampling weight
     */
    int betaWeight();

    /**
     * Returns the sampling weight for release (production) builds.
     *
     * @return the release build sampling weight
     */
    int releaseWeight();

    /**
     * Returns the private-statistics identifier for events on the
     * {@link WamChannel#PRIVATE} channel, or {@code -1} if not
     * applicable.
     *
     * @return the private stats id, or {@code -1}
     */
    int privateStatsId();

    /**
     * Validates this event before commit, checking that required fields
     * are non-{@code null} and any custom conditions are satisfied.
     *
     * <p>The default implementation always returns {@code true}.
     *
     * @return {@code true} if the event is valid and should be committed,
     *         {@code false} if validation failed
     */
    default boolean validate() {
        return true;
    }

    /**
     * Marks this event as committed and returns whether this is the
     * first commit.
     *
     * @return {@code true} if this is the first commit, {@code false}
     *         if already committed
     */
    boolean markCommitted();

    /**
     * Returns the number of bytes required to encode this event in the
     * WAM binary protocol using the static release weight.
     *
     * @return the encoded size in bytes
     */
    default int sizeOf() {
        return sizeOf(releaseWeight());
    }

    /**
     * Returns the number of bytes required to encode this event in the
     * WAM binary protocol using the given weight.
     *
     * @param weight the resolved sampling weight to encode
     * @return the encoded size in bytes
     */
    int sizeOf(int weight);

    /**
     * Writes this event into the given encoder using the static release
     * weight.
     *
     * @param encoder the destination encoder, must not be {@code null}
     */
    default void encode(WamEventEncoder encoder) {
        encode(encoder, releaseWeight());
    }

    /**
     * Writes this event into the given encoder using the given weight.
     *
     * @param encoder the destination encoder, must not be {@code null}
     * @param weight  the resolved sampling weight to encode
     */
    void encode(WamEventEncoder encoder, int weight);
}
