package it.auties.whatsapp.model.privacy;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;

/**
 * A model interface that represents a gdpr account report. This can be queried using
 * {@link Whatsapp#queryGdprAccountInfoStatus()}
 */
public sealed interface GdprAccountReport {
    /**
     * Constructs a pending gdpr report from a timestampSeconds
     *
     * @param timestamp the timestampSeconds in seconds
     * @return a non-null gdpr request
     */
    static Pending ofPending(long timestamp) {
        return new Pending(Clock.parseSeconds(timestamp).orElseGet(() -> ZonedDateTime.now().plusDays(3)));
    }

    /**
     * Constructs a successful gdpr report
     *
     * @return a non-null gdpr request
     */
    static Ready ofReady() {
        return new Ready();
    }

    /**
     * Constructs an erroneous gdpr report
     *
     * @return a non-null gdpr request
     */
    static Error ofError() {
        return new Error();
    }

    /**
     * Returns the type of this report
     *
     * @return a non-null type
     */
    Type type();

    /**
     * The constants of this enumerated type describe the status of a gdpr request
     */
    enum Type {
        /**
         * Pending, should be ready in about three business days
         */
        PENDING,
        /**
         * An error occurred and the report could not be delivered
         */
        ERROR,
        /**
         * The report is ready to be downloaded
         */
        READY
    }

    /**
     * A pending gdpr request
     *
     * @param dateTime the eta for the newsletters of the request
     */
    record Pending(ZonedDateTime dateTime) implements GdprAccountReport {
        @Override
        public Type type() {
            return Type.PENDING;
        }
    }

    /**
     * A successful gdpr request
     */
    record Ready() implements GdprAccountReport {
        @Override
        public Type type() {
            return Type.READY;
        }
    }

    /**
     * An erroneous gdpr request
     */
    record Error() implements GdprAccountReport {
        @Override
        public Type type() {
            return Type.ERROR;
        }
    }
}
