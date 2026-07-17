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
 * Models the sealed inbound reply hierarchy for the
 * {@code WASmaxBizCtwaActionBannerSuggestionRPC} SMAX receive RPC.
 * <p>
 * Carries the single {@link Notification} permit because the RPC is
 * {@link SmaxStanza.Response}-shape (server-pushed only, with no outbound
 * counterpart). The notification surfaces the click-to-WhatsApp "suggested
 * banner" panel for a {@code <notification type="business">} stanza bearing a
 * {@code <ctwa_suggestion/>} child.
 */
public sealed interface SmaxBannerSuggestionResponse extends SmaxStanza.Response
        permits SmaxBannerSuggestionResponse.Notification {

    /**
     * Parses an inbound CTWA-banner-suggestion notification.
     * <p>
     * Equivalent to {@link Notification#of(Stanza)}; the factory exists at the
     * sealed-interface level so callers can reach it without naming the
     * permitted concrete type. Returns empty when the stanza shape does not
     * match.
     *
     * @param stanza the inbound notification stanza received from the relay; never {@code null}
     * @return an {@link Optional} carrying the parsed notification, or empty when the stanza does not match
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBizCtwaActionBannerSuggestionRPC",
            exports = "receiveBannerSuggestionRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<Notification> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        return Notification.of(stanza);
    }

    /**
     * Models the server-pushed {@code Notification} variant carrying the banner
     * suggestion plus envelope echoes.
     * <p>
     * Wraps the parsed {@code <notification type="business">} envelope (server
     * JID, optional target user JID) together with the {@code target_entity_id}
     * carried on the {@code <ctwa_suggestion/>} child and the optional
     * {@link SmaxBannerSuggestionBanner banner} grandchild. Consumers branch on
     * {@link #banner()}: an absent banner is treated as missing banner data, a
     * present banner with {@link SmaxBannerSuggestionFalseTrueFlag#TRUE
     * revoked=true} is dismissed, and a present non-revoked banner drives the
     * full rich-content panel.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest")
    @WhatsAppWebModule(moduleName = "WASmaxInBizCtwaActionServerNotificationMixin")
    final class Notification implements SmaxBannerSuggestionResponse {
        /**
         * Holds the {@code from} attribute, always the literal
         * {@code s.whatsapp.net} server JID.
         */
        private final Jid from;

        /**
         * Holds the optional {@code to} user JID; absent when the relay
         * broadcasts to every linked device.
         */
        private final Jid to;

        /**
         * Holds the {@code type} attribute, always the literal
         * {@code "business"}.
         */
        private final String type;

        /**
         * Holds the {@code target_entity_id} attribute on the
         * {@code <ctwa_suggestion/>} child, identifying the source CTWA entity
         * (ad, account, message thread). The trailing {@code -<bannerType>}
         * suffix discriminates between {@code recreate_ad} and
         * {@code manage_ads} banner kinds.
         */
        private final String targetEntityId;

        /**
         * Holds the optional {@code <banner/>} grandchild; absent when the relay
         * omitted the payload.
         */
        private final SmaxBannerSuggestionBanner banner;

        /**
         * Constructs a notification from already-validated envelope and payload
         * values.
         * <p>
         * Callers normally obtain a notification by parsing a stanza via
         * {@link #of(Stanza)}; this constructor is exposed for tests and for
         * hand-built fixtures.
         *
         * @param from           the server JID; never {@code null}
         * @param to             the optional target user JID; may be {@code null}
         * @param type           the notification type literal; never {@code null}
         * @param targetEntityId the CTWA target-entity identifier; never {@code null}
         * @param banner         the optional banner projection; may be {@code null}
         * @throws NullPointerException if {@code from}, {@code type}, or {@code targetEntityId} is {@code null}
         */
        public Notification(Jid from, Jid to, String type, String targetEntityId, SmaxBannerSuggestionBanner banner) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.to = to;
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.targetEntityId = Objects.requireNonNull(targetEntityId, "targetEntityId cannot be null");
            this.banner = banner;
        }

        /**
         * Returns the server JID that emitted the notification.
         * <p>
         * Always the literal {@code s.whatsapp.net} JID; the parser rejects any
         * other value before this getter is reachable.
         *
         * @return the JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the optional target user JID.
         * <p>
         * Empty when the relay broadcasts the notification to every linked
         * device of the account.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> to() {
            return Optional.ofNullable(to);
        }

        /**
         * Returns the notification type literal.
         * <p>
         * Always the literal {@code "business"}; the parser rejects any other
         * value before this getter is reachable.
         *
         * @return the type; never {@code null}
         */
        public String type() {
            return type;
        }

        /**
         * Returns the CTWA target-entity identifier.
         * <p>
         * Identifies the source CTWA ad, account, or thread; consumers split on
         * {@code "-"} and use the trailing suffix to pick the banner-kind
         * logging slug.
         *
         * @return the id; never {@code null}
         */
        public String targetEntityId() {
            return targetEntityId;
        }

        /**
         * Returns the optional banner projection.
         * <p>
         * Empty when the relay omitted the {@code <banner/>} grandchild;
         * consumers treat that as a missing-banner-data event and skip
         * rendering.
         *
         * @return an {@link Optional} carrying the banner, or empty
         */
        public Optional<SmaxBannerSuggestionBanner> banner() {
            return Optional.ofNullable(banner);
        }

        /**
         * Parses a notification from a {@code <notification type="business">}
         * stanza.
         * <p>
         * Returns empty when the stanza tag is wrong, when the {@code from}
         * attribute is not the literal {@code s.whatsapp.net} server JID, when
         * the {@code type} attribute is not the literal {@code "business"}, when
         * the mandatory {@code <ctwa_suggestion/>} child or its
         * {@code target_entity_id} attribute is missing, or when the optional
         * {@code <banner/>} grandchild is present but fails to parse.
         *
         * @implNote This implementation checks the literal {@code from}
         * server-JID before the {@link Jid} resolution and checks the literal
         * {@code type="business"} before the {@code <ctwa_suggestion/>} child
         * lookup; the optional {@code to} attribute is fetched after the
         * {@code type} check and never causes parse failure.
         * @param stanza the candidate {@code <notification/>} stanza; never {@code null}
         * @return an {@link Optional} carrying the parsed notification, or empty when parsing fails
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionBannerSuggestionRequest",
                exports = "parseBannerSuggestionRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBizCtwaActionServerNotificationMixin",
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
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            var to = stanza.getAttributeAsJid("to").orElse(null);
            var type = stanza.getAttributeAsString("type").orElse(null);
            if (!"business".equals(type)) {
                return Optional.empty();
            }
            var ctwaSuggestion = stanza.getChild("ctwa_suggestion").orElse(null);
            if (ctwaSuggestion == null) {
                return Optional.empty();
            }
            var targetEntityId = ctwaSuggestion.getAttributeAsString("target_entity_id").orElse(null);
            if (targetEntityId == null) {
                return Optional.empty();
            }
            SmaxBannerSuggestionBanner banner = null;
            var bannerNode = ctwaSuggestion.getChild("banner").orElse(null);
            if (bannerNode != null) {
                var parsed = SmaxBannerSuggestionBanner.of(bannerNode);
                if (parsed.isEmpty()) {
                    return Optional.empty();
                }
                banner = parsed.get();
            }
            return Optional.of(new Notification(from, to, type, targetEntityId, banner));
        }

        /**
         * Compares this notification to {@code obj} for structural equality on
         * the envelope echoes and the banner projection.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Notification}
         *         with matching {@link #from()}, {@link #to()}, {@link #type()},
         *         {@link #targetEntityId()}, and {@link #banner()}
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
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.to, that.to)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.targetEntityId, that.targetEntityId)
                    && Objects.equals(this.banner, that.banner);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash of the envelope echoes and banner
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, to, type, targetEntityId, banner);
        }

        /**
         * Returns a debug-friendly rendering naming the envelope echoes and the
         * banner.
         *
         * @return a record-style string with all five slot values
         */
        @Override
        public String toString() {
            return "SmaxBannerSuggestionResponse.Notification[from=" + from
                    + ", to=" + to
                    + ", type=" + type
                    + ", targetEntityId=" + targetEntityId
                    + ", banner=" + banner + ']';
        }
    }
}
