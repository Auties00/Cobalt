package com.github.auties00.cobalt.wire.linked.payment;

import java.util.List;
import java.util.Objects;

/**
 * Sealed disjunction over the v3-payments-Terms-of-Service consumer
 * variants the WhatsApp Business client can accept.
 *
 * <p>WhatsApp's payments service is split into two regional consumer
 * surfaces: a Brazilian (FBPAY) consumer and an Indian UPI consumer.
 * Each surface exposes its own ToS document with its own set of
 * additional notices the user must acknowledge alongside the main ToS;
 * this disjunction lets the caller pick which surface they are
 * accepting on behalf of.
 *
 * <p>Both variants carry a list of 1..10 notice literals that the
 * relay validates against a per-surface dictionary: BR consumers must
 * pick from the {@code br_p2p_consent}, {@code br_pay_privacy_policy},
 * {@code br_pay_tos}, and {@code br_pay_wa_tos} literals; UPI
 * consumers must pick from the {@code pay_tos_v3} and
 * {@code upi_pay_privacy_policy} literals.
 */
public sealed interface PaymentsTosV3ConsumerVariant permits PaymentsTosV3ConsumerVariant.BrConsumer, PaymentsTosV3ConsumerVariant.UpiConsumer {

    /**
     * Brazilian (FBPAY) consumer ToS-v3 acceptance variant.
     */
    final class BrConsumer implements PaymentsTosV3ConsumerVariant {
        /**
         * The 1..10 BR-consumer notice literals.
         */
        private final List<String> additionalNotices;

        /**
         * Constructs a new BR-consumer variant.
         *
         * @param additionalNotices the notice literals; never
         *                          {@code null}, never empty, never more
         *                          than 10 entries
         * @throws NullPointerException     if {@code additionalNotices}
         *                                  is {@code null}
         * @throws IllegalArgumentException if {@code additionalNotices}
         *                                  is empty or has more than 10
         *                                  entries
         */
        public BrConsumer(List<String> additionalNotices) {
            Objects.requireNonNull(additionalNotices, "additionalNotices cannot be null");
            if (additionalNotices.isEmpty() || additionalNotices.size() > 10) {
                throw new IllegalArgumentException("additionalNotices must contain 1..10 entries");
            }
            this.additionalNotices = List.copyOf(additionalNotices);
        }

        /**
         * Returns the BR-consumer notice literals.
         *
         * @return an unmodifiable list of literals; never {@code null}
         */
        public List<String> additionalNotices() {
            return additionalNotices;
        }

        /**
         * Returns whether this variant equals another.
         *
         * @param obj the object to compare to
         * @return {@code true} when {@code obj} is a {@code BrConsumer}
         *         carrying the same notice list
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (BrConsumer) obj;
            return Objects.equals(this.additionalNotices, that.additionalNotices);
        }

        /**
         * Returns the hash code of this variant.
         *
         * @return the hash of the notice list
         */
        @Override
        public int hashCode() {
            return Objects.hash(additionalNotices);
        }

        /**
         * Returns a human-readable description of this variant.
         *
         * @return a string carrying the notice list
         */
        @Override
        public String toString() {
            return "PaymentsTosV3ConsumerVariant.BrConsumer[additionalNotices=" + additionalNotices + ']';
        }
    }

    /**
     * Indian UPI consumer ToS-v3 acceptance variant.
     */
    final class UpiConsumer implements PaymentsTosV3ConsumerVariant {
        /**
         * The 1..10 UPI-consumer notice literals.
         */
        private final List<String> additionalNotices;

        /**
         * Constructs a new UPI-consumer variant.
         *
         * @param additionalNotices the notice literals; never
         *                          {@code null}, never empty, never more
         *                          than 10 entries
         * @throws NullPointerException     if {@code additionalNotices}
         *                                  is {@code null}
         * @throws IllegalArgumentException if {@code additionalNotices}
         *                                  is empty or has more than 10
         *                                  entries
         */
        public UpiConsumer(List<String> additionalNotices) {
            Objects.requireNonNull(additionalNotices, "additionalNotices cannot be null");
            if (additionalNotices.isEmpty() || additionalNotices.size() > 10) {
                throw new IllegalArgumentException("additionalNotices must contain 1..10 entries");
            }
            this.additionalNotices = List.copyOf(additionalNotices);
        }

        /**
         * Returns the UPI-consumer notice literals.
         *
         * @return an unmodifiable list of literals; never {@code null}
         */
        public List<String> additionalNotices() {
            return additionalNotices;
        }

        /**
         * Returns whether this variant equals another.
         *
         * @param obj the object to compare to
         * @return {@code true} when {@code obj} is a {@code UpiConsumer}
         *         carrying the same notice list
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (UpiConsumer) obj;
            return Objects.equals(this.additionalNotices, that.additionalNotices);
        }

        /**
         * Returns the hash code of this variant.
         *
         * @return the hash of the notice list
         */
        @Override
        public int hashCode() {
            return Objects.hash(additionalNotices);
        }

        /**
         * Returns a human-readable description of this variant.
         *
         * @return a string carrying the notice list
         */
        @Override
        public String toString() {
            return "PaymentsTosV3ConsumerVariant.UpiConsumer[additionalNotices=" + additionalNotices + ']';
        }
    }
}
