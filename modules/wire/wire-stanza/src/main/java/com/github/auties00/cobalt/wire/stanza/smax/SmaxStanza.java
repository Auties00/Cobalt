package com.github.auties00.cobalt.wire.stanza.smax;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Closes the root of every typed SMAX operation modelled in Cobalt.
 *
 * <p>SMAX is WhatsApp's typed-stanza-builder framework, a layer above raw
 * {@code <iq>}, {@code <presence>}, and {@code <message>} stanzas that
 * replaces ad-hoc stanza construction with a declarative request and response
 * schema. Each SMAX RPC ships as up to three companion modules: an
 * {@code Out*Request} module that defines the outbound stanza shape, one or
 * more {@code In*Response*} modules that parse the documented reply variants
 * ({@code Success}, {@code ClientError}, {@code ServerError}, and per-RPC
 * alternates such as {@code SuccessWithMatch} or {@code MigratedSuccess}),
 * and an aggregating {@code *RPC} module that exposes the {@code sendXxxRPC},
 * {@code castXxxRPC}, or {@code receiveXxxRPC} export used by feature code.
 *
 * <p>Cobalt collapses every SMAX RPC into a pair of independent top-level
 * Java types: a concrete {@code Smax<Op>Request} class implementing
 * {@link Request} and a {@code Smax<Op>Response} sealed interface implementing
 * {@link Response} whose permits enumerate the documented reply variants.
 * {@code Cast}-shape RPCs (one-way outbound) ship only a {@link Request};
 * {@code Receive}-shape RPCs (server-pushed) ship only a {@link Response} with
 * a static {@code of(Stanza)} factory.
 *
 * <p>Concrete requests reach the relay through
 * {@link com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient#sendNode(Request)},
 * and concrete responses are returned through those same call sites; the
 * sealed hierarchy exists so that every SMAX handle can be statically
 * classified as either outbound ({@link Request}) or inbound
 * ({@link Response}).
 */
@WhatsAppWebModule(moduleName = "WAComms")
public sealed interface SmaxStanza permits SmaxStanza.Request, SmaxStanza.Response {
    /**
     * Marks the outbound side of a SMAX operation.
     *
     * <p>Every concrete {@code Smax<Op>Request} class implements this
     * interface. The {@link #toStanza()} factory is the single contract that
     * lets the dispatch path serialise the typed value into the canonical
     * {@code <iq>}, {@code <presence>}, or {@code <message>} envelope expected
     * by the relay without knowing the operation's concrete type.
     */
    non-sealed interface Request extends SmaxStanza {
        /**
         * Returns the outbound SMAX stanza for this request.
         *
         * @implSpec
         * Implementations serialise the request's typed fields into the
         * canonical envelope expected by the relay and return the
         * {@link StanzaBuilder} unbuilt, so the dispatch path can stamp a fresh
         * {@code id} attribute via {@link StanzaBuilder#attribute(String, String)}
         * before {@link StanzaBuilder#build() building} and flushing the stanza.
         * @return the outbound stanza builder; never {@code null}
         */
        StanzaBuilder toStanza();
    }

    /**
     * Marks the inbound side of a SMAX operation.
     *
     * <p>Every concrete {@code Smax<Op>Response} sealed interface, or each of
     * its permitted variant classes, implements this type. It carries no
     * abstract methods and exists purely as the closed counterpart of
     * {@link Request} so the whole SMAX surface can be reasoned about as a
     * single sealed hierarchy.
     */
    non-sealed interface Response extends SmaxStanza {
    }
}
