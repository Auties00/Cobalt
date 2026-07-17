package com.github.auties00.cobalt.wire.stanza.mex;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Optional;

/**
 * Models the sealed root of every MEX (Media Exchange) operation in Cobalt.
 *
 * <p>MEX is WhatsApp Web's GraphQL-over-XMPP transport. Each operation is dispatched by wrapping
 * its GraphQL variables in the canonical {@code <iq xmlns="w:mex"><query query_id="..."/></iq>}
 * stanza addressed to {@link JidServer#user()} ({@code s.whatsapp.net}); the relay maps the
 * {@code query_id} to a server-side persisted document and never sees the GraphQL text.
 *
 * <p>The hierarchy collapses each WhatsApp Web GraphQL-document-plus-dispatcher pair into a sealed
 * operation permitting exactly two variants, {@link Request} and {@link Response}. A concrete
 * operation further selects a payload encoding by implementing {@link Request.Json} or
 * {@link Request.Argo} on the request side and the matching {@link Response.Json} or
 * {@link Response.Argo} on the response side.
 */
@WhatsAppWebModule(moduleName = "WAWebMexClient")
public sealed interface MexStanza permits MexStanza.Request, MexStanza.Response {
    /**
     * Models the outbound side of a MEX operation.
     *
     * <p>Every concrete operation's request implements either {@link Json} or {@link Argo}
     * depending on the payload encoding; this interface is never implemented directly. A request
     * exposes the persisted query identifier, the GraphQL operation name, and a factory for the
     * outbound IQ stanza.
     */
    sealed interface Request extends MexStanza {
        /**
         * Returns the compiled GraphQL query identifier used to look up the persisted document for
         * this operation.
         *
         * <p>The identifier maps to a server-side persisted operation; the relay resolves it
         * without ever receiving the GraphQL text. It is emitted as the {@code query_id} attribute
         * of the {@code <query>} stanza built by {@link #toStanza()}.
         *
         * @return the GraphQL query identifier, never {@code null}
         */
        String id();

        /**
         * Returns the GraphQL operation name carried by this request.
         *
         * <p>The name tags query latency and error metrics on WhatsApp Web's MEX perf tracker;
         * Cobalt keeps it on each request so embedders mirroring that telemetry surface can emit
         * the same tag.
         *
         * @return the GraphQL operation name, never {@code null}
         */
        String name();

        /**
         * Builds the outbound MEX IQ stanza for this request.
         *
         * <p>Implementations serialise their GraphQL variables, JSON for {@link Json} or
         * Argo-encoded bytes for {@link Argo}, and wrap them through the corresponding
         * {@link Json#createMexNode(String, String)} or {@link Argo#createMexNode(String, byte[])}
         * helper. The returned {@link StanzaBuilder} may be mutated further, for example to set a
         * custom IQ id, before {@link StanzaBuilder#build()} is called.
         *
         * @return the outbound stanza builder, never {@code null}
         */
        StanzaBuilder toStanza();

        /**
         * Marks MEX requests whose GraphQL variables are JSON-encoded.
         *
         * <p>JSON is the encoding used by the entire MEX surface currently modelled in Cobalt,
         * including catalog, order, product collection, community subgroup and ownership-transfer
         * queries. New operations default to this variant unless they are paired with a binary
         * Argo query document.
         */
        @WhatsAppWebModule(moduleName = "WAWebMexClient")
        @WhatsAppWebModule(moduleName = "WAWebMexNativeClient")
        @WhatsAppWebModule(moduleName = "WAWebMexRelayEnvironment")
        non-sealed interface Json extends Request {
            /**
             * Builds the {@code <iq xmlns="w:mex">} envelope that wraps a JSON-encoded GraphQL
             * query.
             *
             * <p>The {@code query_id} attribute is the compiled-document identifier, the body is
             * the {@code {"variables": ...}} JSON string, and the envelope is addressed to
             * {@link JidServer#user()} with {@code type="get"}. The returned {@link StanzaBuilder} is
             * not yet built, so callers may still mutate its attributes.
             *
             * @implNote This implementation keeps the JSON payload as a {@link String} where
             * WhatsApp Web wraps it in a binary byte view before serialising; Cobalt's
             * {@link StanzaBuilder} accepts the {@link String} directly.
             *
             * @param queryId     the numeric query identifier assigned to the compiled GraphQL
             *                    operation
             * @param jsonPayload the JSON string containing the serialised {@code variables}
             *                    envelope
             * @return a {@link StanzaBuilder} prepared for the IQ stanza, never {@code null}
             */
            @WhatsAppWebExport(moduleName = "WAWebMexClient", exports = "fetchQuery",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WAWebMexNativeClient", exports = "fetchQuery",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            static StanzaBuilder createMexNode(String queryId, String jsonPayload) {
                var queryNode = new StanzaBuilder()
                        .description("query")
                        .attribute("query_id", queryId)
                        .content(jsonPayload)
                        .build();

                return new StanzaBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:mex")
                        .attribute("to", JidServer.user())
                        .attribute("type", "get")
                        .content(queryNode);
            }
        }

        /**
         * Marks MEX requests whose GraphQL variables are encoded with the Argo binary format rather
         * than JSON.
         *
         * <p>The wire envelope is identical to {@link Json}: an {@code <iq xmlns="w:mex">} stanza
         * wrapping a {@code <query>} stanza tagged with {@code query_id}; only the body bytes differ.
         * No MEX operation currently modelled in Cobalt uses this encoding, but the variant keeps
         * the sealed hierarchy open for future Argo query document modules.
         */
        @WhatsAppWebModule(moduleName = "WAWebMexClient")
        @WhatsAppWebModule(moduleName = "WAWebMexNativeClient")
        @WhatsAppWebModule(moduleName = "WAWebMexRelayEnvironment")
        non-sealed interface Argo extends Request {
            /**
             * Builds the {@code <iq xmlns="w:mex">} envelope that wraps an Argo-encoded GraphQL
             * query.
             *
             * <p>The {@code query_id} attribute is the compiled-document identifier, the body is
             * the Argo-encoded {@code variables} payload, and the envelope is addressed to
             * {@link JidServer#user()} with {@code type="get"}. The returned {@link StanzaBuilder} is
             * not yet built, so callers may still mutate its attributes.
             *
             * @implNote This implementation reuses the {@link Json} variant's envelope shape; only
             * the body content type differs.
             *
             * @param queryId     the numeric query identifier assigned to the compiled GraphQL
             *                    operation
             * @param argoPayload the Argo-encoded GraphQL variables
             * @return a {@link StanzaBuilder} prepared for the IQ stanza, never {@code null}
             */
            @WhatsAppWebExport(moduleName = "WAWebMexClient", exports = "fetchQuery",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            @WhatsAppWebExport(moduleName = "WAWebMexNativeClient", exports = "fetchQuery",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            static StanzaBuilder createMexNode(String queryId, byte[] argoPayload) {
                var queryNode = new StanzaBuilder()
                        .description("query")
                        .attribute("query_id", queryId)
                        .content(argoPayload)
                        .build();

                return new StanzaBuilder()
                        .description("iq")
                        .attribute("xmlns", "w:mex")
                        .attribute("to", JidServer.user())
                        .attribute("type", "get")
                        .content(queryNode);
            }
        }
    }

    /**
     * Models the inbound side of a MEX operation.
     *
     * <p>Every concrete operation's response implements either {@link Json} or {@link Argo}
     * depending on the payload encoding. This interface carries no abstract methods; it exists as
     * the closed counterpart of {@link Request} so the entire MEX surface forms a single sealed
     * hierarchy.
     */
    sealed interface Response extends MexStanza {
        /**
         * Marks MEX responses whose payloads decode from JSON.
         *
         * <p>Carries the shared {@link #getTypename(JSONObject)} helper for branching on the
         * GraphQL {@code __typename} discriminator injected by the relay; concrete responses pull
         * that field through this helper before projecting the payload onto a domain model.
         */
        @WhatsAppWebModule(moduleName = "WAWebMexGetTypename")
        non-sealed interface Json extends Response {
            /**
             * Returns the GraphQL {@code __typename} discriminator carried by a MEX response
             * object, if any.
             *
             * <p>MEX responses are GraphQL payloads whose concrete shape is identified by the
             * synthetic {@code __typename} field injected by the relay. For example a group query
             * envelope carries one of {@code "XWA2Group"}, {@code "XWA2CommunityGroup"},
             * {@code "XWA2CommunityDefaultSubGroup"} or {@code "XWA2CommunitySubGroup"} so callers
             * can branch on the underlying entity before projecting the rest of the payload onto a
             * domain model.
             *
             * @implNote This implementation mirrors the source {@code obj?.__typename}
             * optional-chaining semantics: a {@code null} {@code obj} or a missing field both
             * collapse to {@link Optional#empty()}.
             *
             * @param obj the MEX response JSON object to inspect, may be {@code null}
             * @return an {@link Optional} wrapping the {@code __typename} string, or
             *         {@link Optional#empty()} if {@code obj} is {@code null} or does not expose
             *         that field
             */
            @WhatsAppWebExport(moduleName = "WAWebMexGetTypename", exports = "getTypename",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            static Optional<String> getTypename(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }
                return Optional.ofNullable(obj.getString("__typename"));
            }
        }

        /**
         * Marks MEX responses whose payloads decode from Argo binary.
         *
         * <p>Companion to {@link Request.Argo} on the inbound side. No MEX response currently
         * modelled in Cobalt uses this encoding; the variant keeps the sealed hierarchy open for
         * future Argo-encoded responses.
         */
        non-sealed interface Argo extends Response {
        }
    }

}
