package com.github.auties00.cobalt.wire.stanza.smax.privacy;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;

/**
 * Discriminates each {@code <user/>} child in a LID-addressed disallowed-list reply.
 *
 * <p>The {@link Username} arm seeds the local username cache for username-only contacts; the {@link PnJid} arm
 * primes the LID-to-PN mapping store; and the {@link Empty} arm marks a benign LID entry the relay knows nothing
 * else about.
 *
 * @implNote This implementation collapses the WA Web wire discriminator (a username, phone-number, or empty
 * tagged-union) into a sealed interface whose variant type is the discriminator, so pattern matching replaces a
 * name comparison.
 */
public sealed interface SmaxGetContactBlacklistContactListId
        permits SmaxGetContactBlacklistContactListId.Username, SmaxGetContactBlacklistContactListId.PnJid, SmaxGetContactBlacklistContactListId.Empty {

    /**
     * Carries a WhatsApp username for a LID entry the relay tracks by username.
     *
     * <p>Routed into the local username cache so it is primed before the user opens the privacy settings.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyUsernameMixin")
    final class Username implements SmaxGetContactBlacklistContactListId {
        /**
         * The WhatsApp username string echoed by the relay.
         */
        private final String username;

        /**
         * Constructs a {@code Username} arm.
         *
         * @param username the username; never {@code null}
         * @throws NullPointerException if {@code username} is {@code null}
         */
        public Username(String username) {
            this.username = Objects.requireNonNull(username, "username cannot be null");
        }

        /**
         * Returns the WhatsApp username.
         *
         * <p>The value is the literal echo from the relay and is not further validated by the parser.
         *
         * @return the username; never {@code null}
         */
        public String username() {
            return username;
        }

        /**
         * Compares this arm with another for equality by username.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Username} with an equal username
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Username) obj;
            return Objects.equals(this.username, that.username);
        }

        /**
         * Returns a hash code derived from the username.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(username);
        }

        /**
         * Returns a debug representation carrying the username.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistContactListId.Username[username=" + username + ']';
        }
    }

    /**
     * Carries the legacy phone-number JID paired with a LID entry.
     *
     * <p>Surfaced when the relay still remembers the original phone-number JID; routed into the LID-to-PN cache
     * so later contact-resolution lookups resolve without a round-trip.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyPnJidMixin")
    final class PnJid implements SmaxGetContactBlacklistContactListId {
        /**
         * The legacy phone-number JID echoed alongside the LID entry.
         */
        private final Jid pnJid;

        /**
         * Constructs a {@code PnJid} arm.
         *
         * @param pnJid the echoed PN JID; never {@code null}
         * @throws NullPointerException if {@code pnJid} is {@code null}
         */
        public PnJid(Jid pnJid) {
            this.pnJid = Objects.requireNonNull(pnJid, "pnJid cannot be null");
        }

        /**
         * Returns the legacy phone-number JID.
         *
         * @return the PN JID; never {@code null}
         */
        public Jid pnJid() {
            return pnJid;
        }

        /**
         * Compares this arm with another for equality by PN JID.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link PnJid} with an equal JID
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (PnJid) obj;
            return Objects.equals(this.pnJid, that.pnJid);
        }

        /**
         * Returns a hash code derived from the PN JID.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(pnJid);
        }

        /**
         * Returns a debug representation carrying the PN JID.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistContactListId.PnJid[pnJid=" + pnJid + ']';
        }
    }

    /**
     * Marks a LID-addressed entry the relay knows by LID only, with no PN echo.
     *
     * <p>Carries no state of its own; logged as a benign signal and otherwise ignored.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInPrivacyEmptyContactListIdentifierMixin")
    final class Empty implements SmaxGetContactBlacklistContactListId {
        /**
         * Constructs an {@code Empty} arm.
         *
         * <p>Built when neither {@code username} nor {@code pn_jid} is present on the {@code <user/>} child.
         */
        public Empty() {
        }

        /**
         * Compares this arm with another for equality by runtime type.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an {@link Empty}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            return obj != null && obj.getClass() == this.getClass();
        }

        /**
         * Returns a constant hash code shared by every instance of this stateless arm.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Empty.class.hashCode();
        }

        /**
         * Returns a debug representation of this stateless arm.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "SmaxGetContactBlacklistContactListId.Empty[]";
        }
    }
}
