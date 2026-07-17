package com.github.auties00.cobalt.wire.cloud.phone;

import java.util.Objects;
import java.util.Optional;

/**
 * A phone-number update, decoded from a {@code phone_number_name_update} or
 * {@code phone_number_quality_update} webhook change.
 *
 * <p>The two changes share the phone number they describe but carry different payloads: a
 * {@link Name} update reports the outcome of a display-name review, while a {@link Quality} update
 * reports a quality-rating or messaging-limit transition. The sealed hierarchy makes a
 * pattern-match over the two exhaustive.
 */
public sealed interface CloudPhoneNumberUpdate {
    /**
     * Returns the display phone number the update describes.
     *
     * @return the display phone number
     */
    String displayPhoneNumber();

    /**
     * The outcome of a display-name review, decoded from a {@code phone_number_name_update} change.
     */
    final class Name implements CloudPhoneNumberUpdate {
        /**
         * The display phone number the update describes.
         */
        private final String displayPhoneNumber;

        /**
         * The review decision, for example {@code APPROVED} or {@code REJECTED}.
         */
        private final String decision;

        /**
         * The verified name that was requested, or {@code null} when not reported.
         */
        private final String requestedVerifiedName;

        /**
         * The rejection reason, or {@code null} when the request was not rejected.
         */
        private final String rejectionReason;

        /**
         * Constructs a new display-name review outcome.
         *
         * @param displayPhoneNumber    the display phone number
         * @param decision              the review decision
         * @param requestedVerifiedName the requested verified name, or {@code null}
         * @param rejectionReason       the rejection reason, or {@code null}
         * @throws NullPointerException if {@code displayPhoneNumber} or {@code decision} is
         *                              {@code null}
         */
        public Name(String displayPhoneNumber, String decision, String requestedVerifiedName,
                    String rejectionReason) {
            this.displayPhoneNumber = Objects.requireNonNull(displayPhoneNumber, "displayPhoneNumber must not be null");
            this.decision = Objects.requireNonNull(decision, "decision must not be null");
            this.requestedVerifiedName = requestedVerifiedName;
            this.rejectionReason = rejectionReason;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String displayPhoneNumber() {
            return displayPhoneNumber;
        }

        /**
         * Returns the review decision.
         *
         * @return the decision, for example {@code APPROVED}
         */
        public String decision() {
            return decision;
        }

        /**
         * Returns the verified name that was requested.
         *
         * @return an {@link Optional} carrying the requested name, or empty when not reported
         */
        public Optional<String> requestedVerifiedName() {
            return Optional.ofNullable(requestedVerifiedName);
        }

        /**
         * Returns the rejection reason.
         *
         * @return an {@link Optional} carrying the reason, or empty when the request was not
         *         rejected
         */
        public Optional<String> rejectionReason() {
            return Optional.ofNullable(rejectionReason);
        }
    }

    /**
     * A quality-rating or messaging-limit transition, decoded from a
     * {@code phone_number_quality_update} change.
     */
    final class Quality implements CloudPhoneNumberUpdate {
        /**
         * The display phone number the update describes.
         */
        private final String displayPhoneNumber;

        /**
         * The transition event, for example {@code FLAGGED}, {@code UNFLAGGED}, or
         * {@code UPGRADE}.
         */
        private final String event;

        /**
         * The messaging limit after the transition, or {@code null} when not reported.
         */
        private final CloudMessagingLimitTier currentLimit;

        /**
         * Constructs a new quality transition.
         *
         * @param displayPhoneNumber the display phone number
         * @param event              the transition event
         * @param currentLimit       the messaging limit after the transition, or {@code null}
         * @throws NullPointerException if {@code displayPhoneNumber} or {@code event} is
         *                              {@code null}
         */
        public Quality(String displayPhoneNumber, String event, CloudMessagingLimitTier currentLimit) {
            this.displayPhoneNumber = Objects.requireNonNull(displayPhoneNumber, "displayPhoneNumber must not be null");
            this.event = Objects.requireNonNull(event, "event must not be null");
            this.currentLimit = currentLimit;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String displayPhoneNumber() {
            return displayPhoneNumber;
        }

        /**
         * Returns the transition event.
         *
         * @return the event, for example {@code FLAGGED}
         */
        public String event() {
            return event;
        }

        /**
         * Returns the messaging limit after the transition.
         *
         * @return an {@link Optional} carrying the {@link CloudMessagingLimitTier}, or empty when not
         *         reported
         */
        public Optional<CloudMessagingLimitTier> currentLimit() {
            return Optional.ofNullable(currentLimit);
        }
    }
}
