package com.github.auties00.cobalt.wam.annotation;

import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamEventSpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A program element annotated {@code @WamEvent} is a WhatsApp Metrics
 * (WAM) event definition.
 *
 * <p>The annotated type must be a {@code public interface} that extends
 * {@link com.github.auties00.cobalt.wire.wam.event.WamEventSpec WamEventSpec}
 * and declares getter methods annotated with {@link WamProperty}.
 * Each method must return an {@code Optional}, {@code OptionalLong},
 * or {@code OptionalDouble}.
 *
 * <p>At compile time an annotation processor generates a companion
 * {@code *Impl} class that implements the interface and provides
 * high-performance, zero-reflection {@code sizeOf()} and
 * {@code encode()} methods that write the event into a WAM buffer
 * using the custom TLV wire protocol. Only non-{@code null} values
 * are written; the last non-{@code null} field sets the termination
 * ({@code LAST}) flag.
 *
 * <p>Example usage:
 * <pre>{@code
 *     @WamEvent(id = 2172)
 *     public interface SendDocumentEvent extends WamEventSpec {
 *         @WamProperty(index = 1, type = WamType.FLOAT)
 *         OptionalDouble documentSize();
 *
 *         @WamProperty(index = 2, type = WamType.ENUM)
 *         Optional<DocumentType> documentType();
 *
 *         @WamProperty(index = 3, type = WamType.STRING)
 *         Optional<String> documentExt();
 *     }
 * }</pre>
 *
 * @see WamProperty
 * @see WamChannel
 * @see WamEventSpec
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface WamEvent {
    /**
     * Returns the numeric event identifier assigned by WhatsApp.
     *
     * <p>This value is written into the event marker in the WAM binary
     * buffer and uniquely identifies the event type on the server side.
     *
     * @return the event id, for example {@code 2172} for the
     *         {@code SendDocument} event
     */
    int id();

    /**
     * Returns the transport channel for this event.
     *
     * <p>The channel determines both the upload path and the byte value
     * written to the WAM buffer header.
     *
     * @return the channel, defaulting to {@link WamChannel#REGULAR}
     */
    WamChannel channel() default WamChannel.REGULAR;

    /**
     * Returns the sampling weight for alpha (internal) builds.
     *
     * <p>A value of {@code 1} means every occurrence is logged; a value
     * of {@code 100} means roughly one in every hundred occurrences is
     * logged. The selected weight is written as a negative value into
     * the event marker so the server can apply statistical correction.
     *
     * @return the alpha build sampling weight, defaulting to {@code 1}
     */
    int alphaWeight() default 1;

    /**
     * Returns the sampling weight for beta builds.
     *
     * @return the beta build sampling weight, defaulting to {@code 1}
     * @see #alphaWeight()
     */
    int betaWeight() default 1;

    /**
     * Returns the sampling weight for release (production) builds.
     *
     * @return the release build sampling weight, defaulting to {@code 1}
     * @see #alphaWeight()
     */
    int releaseWeight() default 1;

    /**
     * Returns the private-statistics identifier integer for events on the
     * {@link WamChannel#PRIVATE} channel.
     *
     * <p>Private-channel events are correlated using rotating pseudonymous
     * identifiers. This value selects which rotation group the event
     * belongs to. A value of {@code -1} indicates no private statistics
     * identifier, which is the default for non-private events.
     *
     * @return the private stats id integer, or {@code -1} if not
     *         applicable
     */
    int privateStatsId() default -1;
}
