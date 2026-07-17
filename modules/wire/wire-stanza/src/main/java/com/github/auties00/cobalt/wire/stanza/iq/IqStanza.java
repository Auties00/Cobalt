package com.github.auties00.cobalt.wire.stanza.iq;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Roots the sealed hierarchy of every typed legacy {@code <iq>} operation modelled in Cobalt.
 *
 * <p>Legacy IQ denotes the older WhatsApp Web pattern that builds an {@code <iq>} stanza
 * directly via the {@code WADeprecatedSendIq.deprecatedSendIq} dispatcher rather than going
 * through the SMAX typed-stanza-builder framework. The pattern predates the SMAX rewrite and
 * remains in use across the {@code WAWeb*Job} module family (unpair, profile-picture upload,
 * push-server settings, media-conn refresh, set-about, and similar operations).
 *
 * <p>Cobalt models every legacy-IQ operation as a pair of top-level types: a final
 * {@code Iq<Op>Request} class implementing {@link Request} and an {@code Iq<Op>Response}
 * sealed interface implementing {@link Response} whose permits enumerate the documented reply
 * variants. The structural shape mirrors the {@code SmaxStanza} hierarchy; the divergence is
 * source provenance, a {@code WAWeb*Job} (or matching {@code WADeprecatedWapParser} parser)
 * rather than a {@code WASmax*RPC} dispatcher.
 *
 * <p>The hierarchy permits exactly two participants, {@link Request} and {@link Response}, so
 * every legacy-IQ operation handle is statically classified as one or the other. Embedders work
 * with concrete subtypes such as {@code IqUnpairDeviceRequest} or {@code IqSetAboutResponse};
 * this root exists so the dispatcher can accept a single {@link Request} parameter and so
 * reflective tooling can enumerate the legacy-IQ surface from one entry point.
 */
@WhatsAppWebModule(moduleName = "WADeprecatedSendIq")
public sealed interface IqStanza permits IqStanza.Request, IqStanza.Response {
    /**
     * Marks the outbound side of a legacy-IQ operation, implemented by every concrete
     * {@code Iq<Op>Request} type.
     *
     * <p>A request is dispatched by passing the typed instance to the client's {@code sendNode}
     * entry point that accepts an {@link IqStanza.Request}; the dispatcher invokes
     * {@link #toStanza()} to materialise the canonical {@code <iq>} envelope before writing it to
     * the socket. This interface carries no parser hook; the matching
     * {@code Iq<Op>Response.of(Stanza, Stanza)} static is the inverse direction.
     */
    non-sealed interface Request extends IqStanza {
        /**
         * Builds the outbound IQ stanza for this request.
         *
         * <p>Each concrete implementation serialises its typed fields into the canonical
         * {@code <iq>} envelope expected by the relay, including the {@code xmlns}, {@code to},
         * and {@code type} attributes; the returned {@link StanzaBuilder} is ready to be sent
         * without further mutation.
         *
         * @implSpec
         * Implementations must return a non-{@code null} {@link StanzaBuilder} describing a single
         * {@code <iq>} root stanza. The builder must not be mutated by the caller before dispatch;
         * the dispatcher reads it once and releases it.
         *
         * @return the outbound stanza builder, never {@code null}
         */
        StanzaBuilder toStanza();
    }

    /**
     * Marks the inbound side of a legacy-IQ operation, implemented by every concrete
     * {@code Iq<Op>Response} sealed interface or its individual variant classes.
     *
     * <p>This type exists purely as the closed counterpart of {@link Request} so the entire
     * legacy-IQ surface can be reasoned about as a single sealed hierarchy; concrete variants
     * are obtained through each operation's {@code Iq<Op>Response.of(Stanza, Stanza)} static.
     */
    non-sealed interface Response extends IqStanza {
    }
}
