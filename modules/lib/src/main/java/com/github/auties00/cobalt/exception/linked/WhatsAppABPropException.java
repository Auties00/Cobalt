package com.github.auties00.cobalt.exception.linked;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;

import java.time.Duration;
import java.util.Objects;

/**
 * Sealed root for failures raised while reading an A/B configuration
 * property ("AB prop").
 *
 * <p>WhatsApp ships A/B test values from the server to drive feature flags,
 * rate limits, and rollout percentages. Each property is keyed by a numeric
 * configuration code and read from the client by specifying the expected
 * Java type ({@link Boolean}, {@link Integer}, {@link Long}, {@link Double},
 * or {@link String}). The nested subtypes enumerate the two ways such a read
 * can fail: the first server sync did not land before the blocking read
 * timed out ({@link SyncTimeout}), or the delivered string could not be
 * decoded as the requested type ({@link TypeMismatch}). Both failures are
 * local to a single lookup and leave the encrypted Noise channel intact.
 *
 * @apiNote
 * Most embedders observe these exceptions through the configured
 * {@code WhatsAppClientErrorHandler} rather than through {@code try}/{@code catch}
 * blocks around individual reads; pattern-match on the concrete subtype
 * there to distinguish a stalled sync from a decode failure.
 *
 * @implNote
 * This implementation has {@link #toErrorResult()} return
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} for every subtype: an AB
 * prop read failure never invalidates the Noise session, so the session keeps
 * running. WhatsApp Web performs the same reads inline and silently returns
 * the compiled default; Cobalt surfaces the failure instead so the embedder
 * can choose between logging it and falling back.
 *
 * @see SyncTimeout
 * @see TypeMismatch
 */
public abstract sealed class WhatsAppABPropException
        extends WhatsAppLinkedException
        permits WhatsAppABPropException.SyncTimeout,
                WhatsAppABPropException.TypeMismatch {

    /**
     * Constructs a new AB prop exception with the specified detail message.
     *
     * @param message the detail message describing the AB prop read failure
     */
    protected WhatsAppABPropException(String message) {
        super(message);
    }

    /**
     * Constructs a new AB prop exception with the specified detail message and cause.
     *
     * @param message the detail message describing the AB prop read failure
     * @param cause   the underlying cause of the failure
     */
    protected WhatsAppABPropException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns
     * {@link WhatsAppLinkedClientErrorResult#DISCARD}: an AB prop read failure is
     * local to one lookup and never invalidates the Noise session, so the
     * session keeps running.
     */
    @Override
    public final WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Thrown when a blocking A/B configuration property read waits past its
     * timeout without the first sync completing.
     *
     * <p>A blocking AB prop read waits on the first server sync for up to a
     * configured timeout so it can return the live value the server delivered
     * rather than the compiled default. When that window elapses before any
     * sync completes, the read raises this exception instead of silently
     * returning the default, so the stall surfaces at the read site rather
     * than as a wrong-looking flag value observed later. It carries the
     * {@link #timeout()} that elapsed.
     *
     * @apiNote
     * The usual cause is a blocking read issued before {@code sync()} has been
     * triggered on the same thread, so the read waits for a sync that its own
     * thread has not yet reached. Read such props off-thread or after the
     * sync, or use the non-blocking {@code getX(prop, false)} overload that
     * returns the default until the first sync lands.
     */
    public static final class SyncTimeout extends WhatsAppABPropException {

        /**
         * The timeout that elapsed while waiting for the first AB props sync.
         */
        private final Duration timeout;

        /**
         * Constructs a new AB props sync timeout exception.
         *
         * @param timeout the timeout that elapsed while waiting for the first sync
         * @param cause   the underlying timeout fault
         * @throws NullPointerException if {@code timeout} is {@code null}
         */
        public SyncTimeout(Duration timeout, Throwable cause) {
            super("Timed out after " + Objects.requireNonNull(timeout, "timeout cannot be null")
                    + " waiting for the first AB props sync", cause);
            this.timeout = timeout;
        }

        /**
         * Returns the timeout that elapsed while waiting for the first AB props
         * sync.
         *
         * @return the timeout, never {@code null}
         */
        public Duration timeout() {
            return timeout;
        }
    }

    /**
     * Thrown when an A/B configuration property cannot be decoded as the type
     * the caller asked for.
     *
     * <p>Each property is keyed by a numeric configuration code and read from
     * the client by specifying the expected Java type ({@link Boolean},
     * {@link Integer}, {@link Long}, {@link Double}, or {@link String}). This
     * exception is raised when the raw string the server delivered cannot be
     * parsed as that expected type. It carries the {@link #configCode()}, the
     * {@link #expectedType()}, and the {@link #actualValue()} that failed to
     * convert.
     *
     * @apiNote
     * The failure is local to one configuration lookup, so a caller that
     * catches it can fall back to a default value and continue.
     */
    public static final class TypeMismatch extends WhatsAppABPropException {

        /**
         * The numeric configuration code identifying the AB prop that could
         * not be decoded.
         */
        private final int configCode;

        /**
         * The Java type the caller asked the AB prop to be decoded as.
         */
        private final Class<?> expectedType;

        /**
         * The raw string value delivered by the server, exactly as received.
         */
        private final String actualValue;

        /**
         * Constructs a new AB prop type mismatch exception.
         *
         * @param configCode   the numeric configuration code identifying the AB prop
         * @param expectedType the type that was expected but could not be obtained
         * @param actualValue  the raw string value that could not be converted
         * @throws NullPointerException if {@code expectedType} or {@code actualValue} is {@code null}
         */
        public TypeMismatch(int configCode, Class<?> expectedType, String actualValue) {
            super(String.format(
                    "AB prop type mismatch: code=%d, expected=%s, actualValue='%s'",
                    configCode,
                    Objects.requireNonNull(expectedType, "expectedType cannot be null").getSimpleName(),
                    Objects.requireNonNull(actualValue, "actualValue cannot be null")
            ));
            this.configCode = configCode;
            this.expectedType = expectedType;
            this.actualValue = actualValue;
        }

        /**
         * Returns the numeric configuration code of the AB prop whose value
         * could not be decoded.
         *
         * @return the configuration code
         */
        public int configCode() {
            return configCode;
        }

        /**
         * Returns the Java type the caller requested when reading the AB prop.
         *
         * @return the expected type, never {@code null}
         */
        public Class<?> expectedType() {
            return expectedType;
        }

        /**
         * Returns the raw string value the server delivered for this AB prop.
         *
         * @return the actual value, never {@code null}
         */
        public String actualValue() {
            return actualValue;
        }
    }
}
