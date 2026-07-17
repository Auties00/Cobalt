package com.github.auties00.cobalt.wire.stanza.smax.clientexpiration;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Models the closed family of inbound projections of the server-pushed
 * client-expiration broadcast.
 *
 * <p>Surfaces the {@code <ib from="s.whatsapp.net"><client_expiration t?/></ib>}
 * push that the relay sends when an obsolete client build approaches its
 * forced-update deadline. The push carries an optional Unix-epoch cutoff;
 * {@link com.github.auties00.cobalt.stream.control.InfoBulletinStreamHandler}
 * consumes the {@link #of(Stanza)} parser and exposes the deadline to embedders
 * over {@code LinkedWhatsAppClientListener}. The
 * hierarchy is receive-only and has a single {@link Inbound} permit.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleInfoBulletin")
public sealed interface SmaxClientExpirationResponse extends SmaxStanza.Response
        permits SmaxClientExpirationResponse.Inbound {

    /**
     * Parses the given stanza into the sealed family's single permit.
     *
     * <p>Delegates to {@link Inbound#of(Stanza)} and returns
     * {@link Optional#empty()} when the stanza does not match the
     * {@code <ib><client_expiration/></ib>} shape.
     *
     * @param stanza the inbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed projection, or empty when
     *         the stanza shape does not match the schema
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxClientExpirationClientExpirationRPC",
            exports = "receiveClientExpirationRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxClientExpirationResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        return Inbound.of(stanza);
    }

    /**
     * Represents the parsed projection of a single
     * {@code <ib from="s.whatsapp.net"><client_expiration t?/></ib>} push.
     *
     * <p>Carries the optional Unix-epoch cutoff after which the local client
     * build will no longer be allowed to connect. Consumers compare the cutoff
     * against the wall clock and route to the upgrade-prompt UI when it is in
     * the past or imminent.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInClientExpirationClientExpirationRequest")
    final class Inbound implements SmaxClientExpirationResponse {
        /**
         * Holds the {@code from} JID echoed on the {@code <ib>} envelope.
         *
         * @implNote
         * This implementation always resolves to the literal
         * {@code s.whatsapp.net} server JID; {@link #of(Stanza)} rejects any other
         * {@code from} value, so the field is effectively constant-shaped.
         */
        private final Jid from;

        /**
         * Holds the optional non-negative Unix-epoch expiration cutoff.
         *
         * <p>Stays {@code null} when the {@code <client_expiration>} child omits
         * the {@code t} attribute; {@link #clientExpirationT()} maps that
         * absence to {@link Optional#empty()}.
         */
        private final Long clientExpirationT;

        /**
         * Constructs a new inbound projection from a validated stanza.
         *
         * <p>Invoked by {@link #of(Stanza)} once the stanza shape has been
         * checked; embedders typically do not instantiate it directly.
         *
         * @param from the {@code from} JID; never {@code null}
         * @param clientExpirationT the optional cutoff timestamp in epoch
         *                          seconds; may be {@code null}
         * @throws NullPointerException if {@code from} is {@code null}
         */
        public Inbound(Jid from, Long clientExpirationT) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.clientExpirationT = clientExpirationT;
        }

        /**
         * Returns the {@code from} JID echoed on the {@code <ib>} envelope.
         *
         * <p>Always equal to {@link Jid#userServer()} after a successful parse.
         *
         * @return the JID; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the optional expiration cutoff.
         *
         * <p>Carries the value of the {@code <client_expiration t="..."/>}
         * attribute in seconds since the Unix epoch; embedders compare it
         * against the wall clock to decide whether to surface the upgrade-prompt
         * UI. An absent {@code t} is preserved as {@link Optional#empty()},
         * signalling "no deadline yet known" rather than "no expiration".
         *
         * @return an {@link Optional} carrying the cutoff in epoch seconds, or
         *         empty when the relay omitted it
         */
        public Optional<Long> clientExpirationT() {
            return Optional.ofNullable(clientExpirationT);
        }

        /**
         * Parses an {@link Inbound} projection from the given stanza.
         *
         * <p>Returns {@link Optional#empty()} when any precondition fails: the
         * envelope must be an {@code <ib>}, must carry
         * {@code from="s.whatsapp.net"}, must have a {@code <client_expiration/>}
         * child, and when {@code t} is present it must be a non-negative
         * integer. Otherwise returns the projection with the parsed cutoff.
         *
         * @implNote
         * This implementation locates the {@code <client_expiration>} child via
         * {@link Stanza#getChild(String)} rather than a flattening helper, which
         * is sufficient because the stanza tree is already flat at this depth.
         *
         * @param stanza the inbound stanza; never {@code null}
         * @return an {@link Optional} carrying the parsed projection, or empty
         *         when the stanza shape does not match the schema
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInClientExpirationClientExpirationRequest",
                exports = "parseClientExpirationRequest",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Inbound> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("ib")) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null || !Jid.userServer().equals(from)) {
                return Optional.empty();
            }
            var expirationNode = stanza.getChild("client_expiration").orElse(null);
            if (expirationNode == null) {
                return Optional.empty();
            }
            var clientExpirationT = expirationNode.getAttributeAsLong("t");
            if (clientExpirationT.isPresent() && clientExpirationT.getAsLong() < 0) {
                return Optional.empty();
            }
            var clientExpirationValue = clientExpirationT.isPresent()
                    ? Long.valueOf(clientExpirationT.getAsLong())
                    : null;
            return Optional.of(new Inbound(from, clientExpirationValue));
        }

        /**
         * Returns whether the given object is an {@link Inbound} with equal
         * {@code from} and cutoff timestamp.
         *
         * @param obj the candidate; may be {@code null}
         * @return {@code true} when both fields match
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Inbound) obj;
            return Objects.equals(this.from, that.from)
                    && Objects.equals(this.clientExpirationT, that.clientExpirationT);
        }

        /**
         * Returns a hash code derived from {@code from} and the cutoff
         * timestamp.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, clientExpirationT);
        }

        /**
         * Returns a debug-friendly textual representation of this projection.
         *
         * @return the textual representation
         */
        @Override
        public String toString() {
            return "SmaxClientExpirationResponse.Inbound[from=" + from
                    + ", clientExpirationT=" + clientExpirationT + ']';
        }
    }
}
