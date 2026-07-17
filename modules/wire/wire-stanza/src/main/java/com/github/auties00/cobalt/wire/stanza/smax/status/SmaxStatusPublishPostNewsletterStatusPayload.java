package com.github.auties00.cobalt.wire.stanza.smax.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.Objects;

/**
 * The addressing mode of a {@link SmaxStatusPublishPostNewsletterStatusRequest}.
 *
 * <p>A publish is keyed either by the target status's server-id ({@link WithServerId}), used for
 * status-reaction and status-reaction-revoke publishes, or by a locally generated stanza-id only
 * ({@link WithClientIdOnly}), used for brand-new status posts such as the status-send and
 * status-revoke flows. Callers pre-build the inner content as a {@link Stanza} before wrapping it in
 * one of the two variants.
 *
 * @implNote
 * This implementation models the WA Web
 * {@code clientPostNewsletterStatusAndServerOrPostNewsletterStatusIDMixinGroup} disjunction as a
 * sealed interface with two final implementations.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishClientPostNewsletterStatusAndServerOrPostNewsletterStatusIDMixinGroup")
public sealed interface SmaxStatusPublishPostNewsletterStatusPayload permits SmaxStatusPublishPostNewsletterStatusPayload.WithServerId, SmaxStatusPublishPostNewsletterStatusPayload.WithClientIdOnly {

    /**
     * The addressing variant for a publish that references a previously published status by its
     * server-id.
     *
     * <p>Callers select this variant for status-reaction and status-reaction-revoke publishes.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishPostNewsletterStatusClientAndServerIDMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishStatusNewsletterReactionStatusNewsletterReactionOrStatusNewsletterReactionRevokeMixinGroup")
    final class WithServerId implements SmaxStatusPublishPostNewsletterStatusPayload {
        /**
         * The locally-generated publish stanza id.
         */
        private final String stanzaId;

        /**
         * The targeted status's server-id.
         */
        private final long statusServerId;

        /**
         * The pre-built reaction or reaction-revoke content payload.
         */
        private final Stanza innerContent;

        /**
         * Constructs a server-id-addressed payload.
         *
         * @param stanzaId       the publish stanza id; never {@code null}
         * @param statusServerId the targeted status's server-id
         * @param innerContent   the variant-shaped inner payload; never {@code null}
         * @throws NullPointerException if {@code stanzaId} or {@code innerContent} is {@code null}
         */
        public WithServerId(String stanzaId, long statusServerId, Stanza innerContent) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.statusServerId = statusServerId;
            this.innerContent = Objects.requireNonNull(innerContent, "innerContent cannot be null");
        }

        /**
         * Returns the publish stanza id.
         *
         * @return the id; never {@code null}
         */
        public String stanzaId() {
            return stanzaId;
        }

        /**
         * Returns the targeted status's server-id.
         *
         * @return the server-id
         */
        public long statusServerId() {
            return statusServerId;
        }

        /**
         * Returns the pre-built inner content stanza.
         *
         * <p>Read by {@link SmaxStatusPublishPostNewsletterStatusRequest#toStanza()} when building
         * the publish stanza.
         *
         * @return the stanza; never {@code null}
         */
        public Stanza innerContent() {
            return innerContent;
        }

        /**
         * Compares this payload to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WithServerId} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WithServerId) obj;
            return this.statusServerId == that.statusServerId
                    && Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.innerContent, that.innerContent);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, statusServerId, innerContent);
        }

        /**
         * Returns a debug-friendly representation of this payload.
         *
         * <p>The format is intended for logging and is not part of any contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxStatusPublishPostNewsletterStatusPayload.WithServerId[stanzaId=" + stanzaId
                    + ", statusServerId=" + statusServerId
                    + ", innerContent=" + innerContent + ']';
        }
    }

    /**
     * The addressing variant for a brand-new status post.
     *
     * <p>Callers select this variant for status posts that do not reference a prior status, namely
     * the send-new and revoke-existing flows.
     */
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishPostNewsletterStatusClientIDMixin")
    @WhatsAppWebModule(moduleName = "WASmaxOutStatusPublishNewsletterClientIdContent")
    final class WithClientIdOnly implements SmaxStatusPublishPostNewsletterStatusPayload {
        /**
         * The locally-generated publish stanza id.
         */
        private final String stanzaId;

        /**
         * The pre-built inner client-id content payload.
         */
        private final Stanza clientIdContent;

        /**
         * Constructs a brand-new-post payload.
         *
         * @param stanzaId        the publish stanza id; never {@code null}
         * @param clientIdContent the inner client-id content stanza; never {@code null}
         * @throws NullPointerException if either argument is {@code null}
         */
        public WithClientIdOnly(String stanzaId, Stanza clientIdContent) {
            this.stanzaId = Objects.requireNonNull(stanzaId, "stanzaId cannot be null");
            this.clientIdContent = Objects.requireNonNull(clientIdContent, "clientIdContent cannot be null");
        }

        /**
         * Returns the publish stanza id.
         *
         * @return the id; never {@code null}
         */
        public String stanzaId() {
            return stanzaId;
        }

        /**
         * Returns the pre-built inner client-id content stanza.
         *
         * @return the stanza; never {@code null}
         */
        public Stanza clientIdContent() {
            return clientIdContent;
        }

        /**
         * Compares this payload to another for value equality.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is a {@link WithClientIdOnly} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (WithClientIdOnly) obj;
            return Objects.equals(this.stanzaId, that.stanzaId)
                    && Objects.equals(this.clientIdContent, that.clientIdContent);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(stanzaId, clientIdContent);
        }

        /**
         * Returns a debug-friendly representation of this payload.
         *
         * <p>The format is intended for logging and is not part of any contract.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxStatusPublishPostNewsletterStatusPayload.WithClientIdOnly[stanzaId=" + stanzaId
                    + ", clientIdContent=" + clientIdContent + ']';
        }
    }
}
