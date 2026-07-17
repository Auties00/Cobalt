package com.github.auties00.cobalt.wire.linked.business.ctwa;

import java.util.Objects;

/**
 * Outcome of a silent Click-to-WhatsApp access-token nonce probe.
 *
 * <p>Before kicking off a full CTWA recovery prompt, the WhatsApp Business
 * client first asks the relay whether the locally-cached credentials are
 * still good enough to mint an access token without user interaction. The
 * relay answers in one of two shapes: it either issues the silent nonce
 * directly ({@link Issued}) — which means the client can quietly refresh
 * its token — or it refuses and tells the client which email address must
 * confirm account ownership before a new nonce can be issued
 * ({@link RecoveryRequired}).
 *
 * <p>Callers pattern-match on the concrete subtype to decide whether to
 * proceed with the silent refresh or to surface the recovery prompt with
 * the correct inbox.
 */
public sealed interface CtwaSilentNonceResult permits CtwaSilentNonceResult.Issued, CtwaSilentNonceResult.RecoveryRequired {

    /**
     * The relay accepted the probe and minted a silent nonce.
     *
     * <p>The nonce itself is not surfaced because the WhatsApp client
     * consumes it internally to refresh the access token; the only
     * information the caller needs is that the silent path succeeded.
     */
    final class Issued implements CtwaSilentNonceResult {
        /**
         * Constructs a new issued outcome.
         */
        public Issued() {
        }

        /**
         * Returns whether this object equals another.
         *
         * @param obj the object to compare to
         * @return {@code true} when {@code obj} is also an {@code Issued}
         *         instance
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns the hash code of this outcome.
         *
         * @return a constant hash matching all {@code Issued} instances
         */
        @Override
        public int hashCode() {
            return Issued.class.hashCode();
        }

        /**
         * Returns a human-readable description of this outcome.
         *
         * @return a constant string description
         */
        @Override
        public String toString() {
            return "CtwaSilentNonceResult.Issued[]";
        }
    }

    /**
     * The relay refused to mint a silent nonce because the user must
     * first confirm account ownership via an email-recovery code.
     *
     * <p>Carries the email address the relay just dispatched the
     * recovery code to, so the UI can route the user toward the correct
     * inbox.
     */
    final class RecoveryRequired implements CtwaSilentNonceResult {
        /**
         * The email address the recovery code was sent to.
         */
        private final String email;

        /**
         * Constructs a new recovery-required outcome.
         *
         * @param email the email address the recovery code was
         *              dispatched to; never {@code null}
         * @throws NullPointerException if {@code email} is {@code null}
         */
        public RecoveryRequired(String email) {
            this.email = Objects.requireNonNull(email, "email cannot be null");
        }

        /**
         * Returns the recovery-code email address.
         *
         * @return the email address; never {@code null}
         */
        public String email() {
            return email;
        }

        /**
         * Returns whether this outcome equals another.
         *
         * @param obj the object to compare to
         * @return {@code true} when {@code obj} is a
         *         {@code RecoveryRequired} carrying the same email
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (RecoveryRequired) obj;
            return Objects.equals(this.email, that.email);
        }

        /**
         * Returns the hash code of this outcome.
         *
         * @return the hash of the recovery email
         */
        @Override
        public int hashCode() {
            return Objects.hash(email);
        }

        /**
         * Returns a human-readable description of this outcome.
         *
         * @return a string carrying the recovery email
         */
        @Override
        public String toString() {
            return "CtwaSilentNonceResult.RecoveryRequired[email=" + email + ']';
        }
    }
}
