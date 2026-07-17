package com.github.auties00.cobalt.wire.stanza.smax.account;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import java.util.List;
import java.util.Objects;

/**
 * Models the payments-jurisdiction half of a
 * {@link SmaxAccountSetPaymentsTOSv3Request} payload.
 *
 * <p>A consumer variant is either the Brazilian-FBPAY consumer
 * ({@link BrConsumer}) or the Indian-UPI consumer ({@link UpiConsumer}).
 * Callers preparing a ToS-v3 acceptance pick {@link BrConsumer} when
 * accepting Brazilian payment terms or {@link UpiConsumer} when accepting
 * Indian payment terms. The shared structure (a 1..10 list of
 * {@code <additional_notice notice=...>} children) is identical between
 * variants; only the allowed notice literals differ, and the relay rejects
 * literals outside the per-variant set documented on
 * {@link SmaxAccountSetPaymentsTOSv3Response.Success}.
 *
 * @implNote
 * This implementation models the disjunction as a sealed interface with two
 * final implementations; each variant is constructed directly with a list of
 * pre-validated notice literals rather than going through a smax-mixin
 * builder.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerOrSetPaymentsTOSv3UPIConsumerPaymentsTOSv3MixinGroup")
public sealed interface SmaxAccountSetPaymentsTOSv3ConsumerVariant permits SmaxAccountSetPaymentsTOSv3ConsumerVariant.BrConsumer, SmaxAccountSetPaymentsTOSv3ConsumerVariant.UpiConsumer {

    /**
     * Models the Brazilian-FBPAY consumer variant of a ToS-v3 acceptance.
     *
     * <p>Callers pick this variant when accepting Brazilian payment terms;
     * {@link SmaxAccountSetPaymentsTOSv3Request#toStanza()} stamps
     * {@code service="FBPAY"} onto the outbound {@code <accept_pay/>} element.
     * Acceptable notice literals are the four entries enforced by
     * {@link SmaxAccountSetPaymentsTOSv3Response.Success}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutAccountSetPaymentsTOSv3BRConsumerPaymentsTOSv3Mixin")
    final class BrConsumer implements SmaxAccountSetPaymentsTOSv3ConsumerVariant {
        /**
         * Holds the 1..10 Brazilian-FBPAY notice literals attached as
         * {@code <additional_notice notice=...>} children.
         *
         * <p>Each entry is expected to be one of {@code "br_p2p_consent"},
         * {@code "br_pay_privacy_policy"}, {@code "br_pay_tos"}, or
         * {@code "br_pay_wa_tos"}. The constructor validates the list bounds
         * but not the literals; the relay rejects unknown entries on receipt.
         */
        private final List<String> additionalNotices;

        /**
         * Constructs a Brazilian-FBPAY variant carrying a pre-validated notice
         * list.
         *
         * @implNote
         * This implementation defensively copies via
         * {@link List#copyOf(java.util.Collection)} so callers can mutate the
         * input list after construction without affecting the variant.
         *
         * @param additionalNotices the notice literals; must contain between
         *                          {@code 1} and {@code 10} entries
         * @throws NullPointerException     if {@code additionalNotices} is
         *                                  {@code null}
         * @throws IllegalArgumentException if {@code additionalNotices} is
         *                                  empty or exceeds {@code 10} entries
         */
        public BrConsumer(List<String> additionalNotices) {
            Objects.requireNonNull(additionalNotices, "additionalNotices cannot be null");
            if (additionalNotices.isEmpty() || additionalNotices.size() > 10) {
                throw new IllegalArgumentException("additionalNotices must contain 1..10 entries");
            }
            this.additionalNotices = List.copyOf(additionalNotices);
        }

        /**
         * Returns the Brazilian-FBPAY notice literals carried by this variant.
         *
         * <p>Read by {@link SmaxAccountSetPaymentsTOSv3Request#toStanza()} when
         * fanning the entries into {@code <additional_notice/>} children.
         *
         * @return an unmodifiable list of {@code 1..10} literals; never
         *         {@code null}
         */
        public List<String> additionalNotices() {
            return additionalNotices;
        }

        /**
         * Compares this variant to another for value equality on the notice
         * list.
         *
         * <p>Two {@link BrConsumer} instances are equal iff their notice lists
         * are equal element-wise.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link BrConsumer} with
         *         the same notice list
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(additionalNotices);
        }

        /**
         * Returns a debug-friendly representation listing the notice literals.
         *
         * <p>The format is intended for logging and is not part of any
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxAccountSetPaymentsTOSv3ConsumerVariant.BrConsumer[additionalNotices=" + additionalNotices + ']';
        }
    }

    /**
     * Models the Indian-UPI consumer variant of a ToS-v3 acceptance.
     *
     * <p>Callers pick this variant when accepting Indian UPI payment terms;
     * {@link SmaxAccountSetPaymentsTOSv3Request#toStanza()} stamps
     * {@code service="UPI"} onto the outbound {@code <accept_pay/>} element.
     * Acceptable notice literals are the two entries enforced by
     * {@link SmaxAccountSetPaymentsTOSv3Response.Success}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutAccountSetPaymentsTOSv3UPIConsumerPaymentsTOSv3Mixin")
    final class UpiConsumer implements SmaxAccountSetPaymentsTOSv3ConsumerVariant {
        /**
         * Holds the 1..10 Indian-UPI notice literals attached as
         * {@code <additional_notice notice=...>} children.
         *
         * <p>Each entry is expected to be one of {@code "pay_tos_v3"} or
         * {@code "upi_pay_privacy_policy"}. The constructor validates the list
         * bounds but not the literals; the relay rejects unknown entries on
         * receipt.
         */
        private final List<String> additionalNotices;

        /**
         * Constructs an Indian-UPI variant carrying a pre-validated notice
         * list.
         *
         * @implNote
         * This implementation defensively copies via
         * {@link List#copyOf(java.util.Collection)} so callers can mutate the
         * input list after construction without affecting the variant.
         *
         * @param additionalNotices the notice literals; must contain between
         *                          {@code 1} and {@code 10} entries
         * @throws NullPointerException     if {@code additionalNotices} is
         *                                  {@code null}
         * @throws IllegalArgumentException if {@code additionalNotices} is
         *                                  empty or exceeds {@code 10} entries
         */
        public UpiConsumer(List<String> additionalNotices) {
            Objects.requireNonNull(additionalNotices, "additionalNotices cannot be null");
            if (additionalNotices.isEmpty() || additionalNotices.size() > 10) {
                throw new IllegalArgumentException("additionalNotices must contain 1..10 entries");
            }
            this.additionalNotices = List.copyOf(additionalNotices);
        }

        /**
         * Returns the Indian-UPI notice literals carried by this variant.
         *
         * <p>Read by {@link SmaxAccountSetPaymentsTOSv3Request#toStanza()} when
         * fanning the entries into {@code <additional_notice/>} children.
         *
         * @return an unmodifiable list of {@code 1..10} literals; never
         *         {@code null}
         */
        public List<String> additionalNotices() {
            return additionalNotices;
        }

        /**
         * Compares this variant to another for value equality on the notice
         * list.
         *
         * <p>Two {@link UpiConsumer} instances are equal iff their notice
         * lists are equal element-wise.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link UpiConsumer} with
         *         the same notice list
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(additionalNotices);
        }

        /**
         * Returns a debug-friendly representation listing the notice literals.
         *
         * <p>The format is intended for logging and is not part of any
         * contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxAccountSetPaymentsTOSv3ConsumerVariant.UpiConsumer[additionalNotices=" + additionalNotices + ']';
        }
    }
}
