package com.github.auties00.cobalt.wire.stanza.smax.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * The inbound family of relay notifications carrying a freshly-pushed
 * CTWA biz access-token nonce.
 *
 * <p>A {@code <notification type="business"/>} stanza with a {@code <wa_ad_account_nonce/>} child
 * delivers the nonce that unblocks an in-flight {@link SmaxRequestSilentNonceResponse.Success}
 * outcome. The family carries a single permit because receive-shape SMAX RPCs have no outbound
 * counterpart.
 */
public sealed interface SmaxNonceNotificationResponse extends SmaxStanza.Response permits SmaxNonceNotificationResponse.Notification {

    /**
     * Tries to parse the supplied stanza as a {@link Notification}.
     *
     * <p>Returns {@link Optional#empty()} when the stanza does not match the documented shape so
     * the dispatcher can fall through to other branches.
     *
     * @param stanza the inbound notification stanza; never
     *             {@code null}
     * @return an {@link Optional} carrying the parsed notification,
     *         or {@link Optional#empty()} when the stanza does not
     *         match
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizCtwaAdAccountNonceNotificationRPC",
            exports = "receiveNonceNotificationRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<Notification> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        return Notification.of(stanza);
    }

    /**
     * The single permitted relay-pushed nonce notification.
     *
     * <p>Carries the freshly-issued nonce plus the standard envelope echoes; the consumer extracts
     * {@link #nonce()} and forwards it to the access-token nonce manager.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountNonceNotificationRequest")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaAdAccountServerNotificationMixin")
    final class Notification implements SmaxNonceNotificationResponse {
        /**
         * The optional {@code to} attribute (the local user JID);
         * {@code null} when the relay broadcasts to every linked
         * device.
         */
        private final Jid to;

        /**
         * The freshly-pushed nonce token content of the
         * {@code <wa_ad_account_nonce>} child.
         */
        private final String nonce;

        /**
         * Constructs a new notification.
         *
         * <p>Called by {@link #of(Stanza)} after validating the stanza envelope.
         *
         * @param to    the optional target user JID; may be
         *              {@code null}
         * @param nonce the nonce content; never {@code null}
         * @throws NullPointerException if {@code nonce} is
         *                              {@code null}
         */
        public Notification(Jid to, String nonce) {
            this.to = to;
            this.nonce = Objects.requireNonNull(nonce, "nonce cannot be null");
        }

        /**
         * Returns the optional target user JID.
         *
         * <p>Returns {@link Optional#empty()} when the relay broadcasts the nonce to all linked
         * devices without a specific {@code to} target.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> to() {
            return Optional.ofNullable(to);
        }

        /**
         * Returns the pushed nonce token.
         *
         * <p>Forwarded by the caller to the access-token nonce manager, which resolves the
         * in-flight nonce-fetch outcome.
         *
         * @return the nonce; never {@code null}
         */
        public String nonce() {
            return nonce;
        }

        /**
         * Tries to parse a notification from the supplied stanza.
         *
         * <p>Accepts only stanzas tagged
         * {@code <notification type="business" from="s.whatsapp.net">} carrying a
         * {@code <wa_ad_account_nonce>} child with non-empty content; everything else returns
         * {@link Optional#empty()}.
         *
         * @param stanza the inbound notification stanza
         * @return an {@link Optional} carrying the parsed
         *         notification, or {@link Optional#empty()} when
         *         the stanza shape does not match
         * @throws NullPointerException if {@code stanza} is
         *                              {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountNonceNotificationRequest",
                exports = "parseNonceNotificationRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaAdAccountServerNotificationMixin",
                exports = "parseServerNotificationMixin",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Notification> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("notification")) {
                return Optional.empty();
            }
            var fromValue = stanza.getAttributeAsString("from").orElse(null);
            if (!"s.whatsapp.net".equals(fromValue)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "business")) {
                return Optional.empty();
            }
            var nonceNode = stanza.getChild("wa_ad_account_nonce").orElse(null);
            if (nonceNode == null) {
                return Optional.empty();
            }
            var nonce = nonceNode.toContentString().orElse(null);
            if (nonce == null) {
                return Optional.empty();
            }
            var to = stanza.getAttributeAsJid("to").orElse(null);
            return Optional.of(new Notification(to, nonce));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Notification) obj;
            return Objects.equals(this.to, that.to)
                    && Objects.equals(this.nonce, that.nonce);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hash(to, nonce);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "SmaxNonceNotificationResponse.Notification[to=" + to
                    + ", nonce=" + nonce + ']';
        }
    }
}
