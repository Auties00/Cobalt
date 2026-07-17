package com.github.auties00.cobalt.wire.graphql.whatsappWeb;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Models the sealed root of every {@code http_relay} GraphQL operation in Cobalt.
 *
 * <p>The WhatsApp Web GraphQL transport is WhatsApp Web's HTTP carrier for persisted GraphQL documents. Unlike the
 * MEX transport, which wraps a query in an {@code <iq xmlns="w:mex">} stanza over the encrypted
 * socket, the WhatsApp Web GraphQL transport issues a same-origin {@code POST https://web.whatsapp.com/graphql/}
 * whose {@code application/x-www-form-urlencoded} body carries the persisted {@code doc_id}, the
 * JSON-encoded {@code variables}, and the remapped {@code locale}. The relay maps the {@code doc_id}
 * to a server-side compiled document and never receives the GraphQL text on the wire.
 *
 * <p>This hierarchy mirrors the role {@code MexStanza} plays for the MEX transport: it collapses
 * each WhatsApp Web GraphQL-document-plus-dispatcher pair into a sealed operation permitting exactly
 * two variants, {@link Request} and {@link Response}. Both transports share the same persisted-query
 * identity and operation shape; they differ only in carrier (HTTP versus socket), body encoding
 * (url-encoded form versus stanza stanza), and authentication (the WhatsApp Web session cookie versus
 * the already-authenticated socket session). The WhatsApp Web GraphQL transport is not stanza-based, so it lives
 * outside the {@code stanza} package alongside the Facebook GraphQL transport rather than with the socket-carried
 * operation families.
 *
 * @apiNote The WhatsApp Web GraphQL transport authenticates with the WhatsApp Web browser session cookie established
 * by the canonical {@code /auth/token/} exchange, not with an explicit token; embedders that drive
 * the WhatsApp Web GraphQL transport from a non-browser session must supply that cookie and the FB X-Controller
 * request parameters out of band. Prefer the MEX transport when an operation is available over both.
 */
@WhatsAppWebModule(moduleName = "WAWebRelayClient")
@WhatsAppWebModule(moduleName = "WAWebRelayEnvironment")
public sealed interface WhatsAppWebGraphQlOperation permits WhatsAppWebGraphQlOperation.Request, WhatsAppWebGraphQlOperation.Response {
    /**
     * Models the outbound side of a WhatsApp Web GraphQL operation.
     *
     * <p>Every concrete operation's request implements this interface directly. A request exposes the
     * persisted document identifier emitted as the {@code doc_id} form field, the GraphQL operation
     * name used for persisted-query resolution and perf telemetry, and the JSON-encoded
     * {@code variables} object emitted as the {@code variables} form field.
     */
    non-sealed interface Request extends WhatsAppWebGraphQlOperation {
        /**
         * Returns the persisted document identifier the relay maps to the server-side compiled
         * GraphQL document for this operation.
         *
         * <p>The identifier is emitted as the {@code doc_id} field of the url-encoded request body.
         * WhatsApp Web resolves the live id as {@code WAWebGraphQLPersistedQueries.PersistedQueries[name]}
         * when present and otherwise falls back to the operation's own {@code params.id}; Cobalt ships
         * the resolved numeric id directly per operation.
         *
         * @return the persisted document identifier, never {@code null}
         */
        String docId();

        /**
         * Returns the GraphQL operation name carried by this request.
         *
         * <p>The name is the persisted-query lookup key on WhatsApp Web and tags query latency and
         * error metrics on the GraphQL perf tracker; Cobalt keeps it on each request so embedders
         * mirroring that telemetry surface can emit the same tag.
         *
         * @return the GraphQL operation name, never {@code null}
         */
        String name();

        /**
         * Returns the JSON-encoded {@code variables} object for this operation.
         *
         * <p>The returned string is emitted verbatim as the {@code variables} field of the url-encoded
         * request body; it is the {@code JSON.stringify(variables)} value WhatsApp Web sends. An
         * operation with no variables returns the empty object {@code "{}"}.
         *
         * @return the JSON-encoded variables object, never {@code null}
         */
        String variables();
    }

    /**
     * Models the inbound side of a WhatsApp Web GraphQL operation.
     *
     * <p>The WhatsApp Web GraphQL transport returns a JSON envelope; the client strips the FB {@code for(;;);}
     * anti-hijack prefix, unwraps an optional {@code payload} wrapper, and hands the resulting GraphQL
     * {@code data} object to the concrete response parser. This interface carries no abstract methods;
     * it exists as the closed counterpart of {@link Request} so the entire WhatsApp Web GraphQL surface forms a
     * single sealed hierarchy, and it carries the shared {@link #getTypename(JSONObject)} helper for
     * branching on the GraphQL {@code __typename} discriminator.
     */
    non-sealed interface Response extends WhatsAppWebGraphQlOperation {
        /**
         * Returns the GraphQL {@code __typename} discriminator carried by a WhatsApp Web GraphQL response object, if
         * any.
         *
         * <p>WhatsApp Web GraphQL responses are GraphQL payloads whose concrete shape is identified by the synthetic
         * {@code __typename} field injected by the relay; concrete responses pull that field through
         * this helper before projecting the rest of the payload onto a domain model.
         *
         * @implNote This implementation mirrors the source {@code obj?.__typename} optional-chaining
         * semantics: a {@code null} {@code obj} or a missing field both collapse to
         * {@link Optional#empty()}.
         *
         * @param obj the WhatsApp Web GraphQL response JSON object to inspect, may be {@code null}
         * @return an {@link Optional} wrapping the {@code __typename} string, or
         *         {@link Optional#empty()} if {@code obj} is {@code null} or does not expose that field
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
}
