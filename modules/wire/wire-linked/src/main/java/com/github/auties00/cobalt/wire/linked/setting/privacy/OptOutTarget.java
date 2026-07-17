package com.github.auties00.cobalt.wire.linked.setting.privacy;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * Identifies the business surface targeted by a marketing-message
 * opt-out entry.
 *
 * <p>WhatsApp models opt-out membership against either a single
 * business user (a normal business JID) or against an entire brand
 * grouping multiple business JIDs. This sealed interface exposes the
 * same disjunction at the call-site so that consumers can pattern
 * match on the kind of target rather than juggling several optional
 * fields.
 *
 * <p>The {@link User} arm wraps the JID of a single business user.
 * The {@link Brand} arm wraps a brand identifier and, when known to
 * the relay, the JID of a representative business user belonging to
 * that brand.
 */
public sealed interface OptOutTarget permits OptOutTarget.User, OptOutTarget.Brand {
    /**
     * The single-business-user arm of the disjunction.
     *
     * <p>Carries the JID of the targeted business user.
     */
    final class User implements OptOutTarget {
        /**
         * The targeted business user JID.
         */
        private final Jid jid;

        /**
         * Constructs a new {@code User} target.
         *
         * @param jid the targeted business user JID; never {@code null}
         * @throws NullPointerException if {@code jid} is {@code null}
         */
        public User(Jid jid) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        }

        /**
         * Returns the targeted business user JID.
         *
         * @return the JID; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof User that && Objects.equals(jid, that.jid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jid);
        }

        @Override
        public String toString() {
            return "OptOutTarget.User[jid=" + jid + ']';
        }
    }

    /**
     * The brand arm of the disjunction.
     *
     * <p>Carries the brand identifier and, optionally, the JID of a
     * representative business user belonging to the brand.
     */
    final class Brand implements OptOutTarget {
        /**
         * The brand identifier as returned by the relay.
         */
        private final String brandId;

        /**
         * The optional JID of a representative business user belonging
         * to the brand.
         */
        private final Jid representativeJid;

        /**
         * Constructs a new {@code Brand} target.
         *
         * @param brandId           the brand identifier; never
         *                          {@code null}
         * @param representativeJid the optional representative JID; may
         *                          be {@code null}
         * @throws NullPointerException if {@code brandId} is
         *                              {@code null}
         */
        public Brand(String brandId, Jid representativeJid) {
            this.brandId = Objects.requireNonNull(brandId, "brandId cannot be null");
            this.representativeJid = representativeJid;
        }

        /**
         * Returns the brand identifier.
         *
         * @return the brand identifier; never {@code null}
         */
        public String brandId() {
            return brandId;
        }

        /**
         * Returns the optional representative JID.
         *
         * @return an {@link Optional} carrying the representative JID,
         *         or empty when the relay omitted it
         */
        public Optional<Jid> representativeJid() {
            return Optional.ofNullable(representativeJid);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Brand that
                    && Objects.equals(brandId, that.brandId)
                    && Objects.equals(representativeJid, that.representativeJid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(brandId, representativeJid);
        }

        @Override
        public String toString() {
            return "OptOutTarget.Brand[brandId=" + brandId
                    + ", representativeJid=" + representativeJid + ']';
        }
    }
}
