package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Objects;
import java.util.Optional;

/**
 * Selects how a {@link SmaxNewslettersGetNewsletterMessagesRequest} or
 * {@link SmaxNewslettersGetNewsletterStatusesRequest} addresses its target newsletter.
 *
 * <p>Use {@link ByJid} when the caller already knows the newsletter's JID, or {@link ByInvite} when
 * only the public invite-link token is available (the invite-preview flow for a not-yet-joined
 * newsletter). The optional view-role string is forwarded to the relay's ACL projection so the
 * slice respects the caller's role on the newsletter.</p>
 */
@WhatsAppWebModule(moduleName = "WASmaxOutNewslettersQueryNewsletterParams")
public sealed interface SmaxNewslettersGetNewsletterMessagesQueryParams permits SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid, SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite {

    /**
     * Addresses a newsletter directly by its {@link Jid}.
     *
     * <p>The dominant case once the newsletter is in the local subscribed-list. The optional
     * {@code view_role} string narrows the slice to the projection the caller has on the newsletter
     * (subscriber, admin, owner).</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersQueryNewsletterJIDParamsMixin")
    final class ByJid implements SmaxNewslettersGetNewsletterMessagesQueryParams {
        /**
         * The {@link Jid} of the newsletter being addressed.
         */
        private final Jid newsletterJid;

        /**
         * The optional view-role string the relay uses to project the caller's ACL onto the slice.
         */
        private final String viewRole;

        /**
         * Constructs a new JID-addressed query.
         *
         * <p>A {@code null} {@code viewRole} requests no role projection; the relay falls back to the
         * caller's default role on the newsletter.</p>
         *
         * @param newsletterJid the newsletter {@link Jid}; never {@code null}
         * @param viewRole      the optional view-role string; may be {@code null}
         * @throws NullPointerException if {@code newsletterJid} is {@code null}
         */
        public ByJid(Jid newsletterJid, String viewRole) {
            this.newsletterJid = Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
            this.viewRole = viewRole;
        }

        /**
         * Returns the addressed newsletter {@link Jid}.
         *
         * @return the newsletter {@link Jid}; never {@code null}
         */
        public Jid newsletterJid() {
            return newsletterJid;
        }

        /**
         * Returns the optional view-role projection string.
         *
         * @return an {@link Optional} carrying the view-role, or empty when no projection is requested
         */
        public Optional<String> viewRole() {
            return Optional.ofNullable(viewRole);
        }

        /**
         * Compares two variants for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ByJid} carrying equal
         *         {@link #newsletterJid()} and {@link #viewRole()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ByJid) obj;
            return Objects.equals(this.newsletterJid, that.newsletterJid)
                    && Objects.equals(this.viewRole, that.viewRole);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #newsletterJid()} and {@link #viewRole()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(newsletterJid, viewRole);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this variant
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterMessagesQueryParams.ByJid[newsletterJid=" + newsletterJid + ", viewRole=" + viewRole + ']';
        }
    }

    /**
     * Addresses a newsletter by its public invite key.
     *
     * <p>Used by invite-link preview surfaces where the user has only the public link token and has
     * not yet joined the newsletter. The optional {@code view_role} string is rarely set on this
     * variant because non-subscribers do not have a custom role.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutNewslettersQueryNewsletterInviteParamsMixin")
    final class ByInvite implements SmaxNewslettersGetNewsletterMessagesQueryParams {
        /**
         * The opaque public invite token for the newsletter.
         */
        private final String inviteKey;

        /**
         * The optional view-role string the relay uses to project the caller's ACL onto the slice.
         */
        private final String viewRole;

        /**
         * Constructs a new invite-addressed query.
         *
         * <p>A {@code null} {@code viewRole} requests no role projection.</p>
         *
         * @param inviteKey the public invite token; never {@code null}
         * @param viewRole  the optional view-role string; may be {@code null}
         * @throws NullPointerException if {@code inviteKey} is {@code null}
         */
        public ByInvite(String inviteKey, String viewRole) {
            this.inviteKey = Objects.requireNonNull(inviteKey, "inviteKey cannot be null");
            this.viewRole = viewRole;
        }

        /**
         * Returns the invite key.
         *
         * @return the invite key; never {@code null}
         */
        public String inviteKey() {
            return inviteKey;
        }

        /**
         * Returns the optional view-role projection string.
         *
         * @return an {@link Optional} carrying the view-role, or empty when no projection is requested
         */
        public Optional<String> viewRole() {
            return Optional.ofNullable(viewRole);
        }

        /**
         * Compares two variants for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ByInvite} carrying equal
         *         {@link #inviteKey()} and {@link #viewRole()}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ByInvite) obj;
            return Objects.equals(this.inviteKey, that.inviteKey)
                    && Objects.equals(this.viewRole, that.viewRole);
        }

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #inviteKey()} and {@link #viewRole()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(inviteKey, viewRole);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this variant
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterMessagesQueryParams.ByInvite[inviteKey=" + inviteKey + ", viewRole=" + viewRole + ']';
        }
    }
}
