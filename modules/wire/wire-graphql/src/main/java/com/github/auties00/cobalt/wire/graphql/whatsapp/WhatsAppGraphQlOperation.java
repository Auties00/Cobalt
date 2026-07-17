package com.github.auties00.cobalt.wire.graphql.whatsapp;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Models the sealed root of every {@code graph.whatsapp.com} GraphQL operation in Cobalt.
 *
 * <p>The {@code graph.whatsapp.com} transport is the session-independent carrier WhatsApp Web uses for
 * persisted GraphQL documents that must run without a signed-in session: signup and pre-login reads,
 * guest reads, public catalog reads, ML-model fetches, and Meta-AI search. Unlike the
 * {@code http_relay} and {@code http_comet} carriers, which authenticate with a browser session cookie
 * or a per-user Facebook access token respectively, this transport authenticates with a shared,
 * hardcoded app-level {@code access_token} baked into the release. Each operation is dispatched as a
 * {@code POST} to one of two {@code graph.whatsapp.com} endpoints whose JSON body carries the
 * {@code access_token}, the persisted {@code doc_id}, the nested {@code variables} object, and the
 * remapped locale.
 *
 * <p>This hierarchy mirrors the role {@code MexStanza} plays for the MEX transport, and the
 * {@code WhatsAppWebGraphQlOperation} and {@code FacebookGraphQlOperation} bases play for the other two
 * HTTP carriers: it collapses each compiled GraphQL document into a sealed operation permitting exactly
 * two variants, {@link Request} and {@link Response}. All of these transports share the same
 * persisted-query identity and operation shape; this one differs in carrier ({@code graph.whatsapp.com}
 * host), body encoding (a JSON object whose {@code variables} is a nested value rather than a
 * url-encoded form), and authentication (a shared app token rather than a session cookie or a per-user
 * token). It is not stanza-based, so it lives outside the {@code stanza} package alongside the other
 * HTTP GraphQL transports rather than with the socket-carried operation families.
 *
 * @apiNote The {@code graph.whatsapp.com} transport needs no session or credentials of its own; the
 * shared app token authenticates it, which is why Cobalt can dispatch it before login and outside any
 * signed-in session. Most embedders reach it only for signup, guest, or public-catalog reads. The
 * {@link WhatsAppGraphQlEnvironment#GUEST} environment is the exception: it carries no default token,
 * so its requests must supply one through {@link Request#accessToken()}.
 */
@WhatsAppWebModule(moduleName = "WAWebRelayEnvironment")
@WhatsAppWebModule(moduleName = "WAWebGraphQLConstants")
public sealed interface WhatsAppGraphQlOperation permits WhatsAppGraphQlOperation.Request, WhatsAppGraphQlOperation.Response {
    /**
     * Models the outbound side of a {@code graph.whatsapp.com} GraphQL operation.
     *
     * <p>Every concrete operation's request implements this interface directly. Beyond the persisted
     * document identifier, operation name, and JSON-encoded variables shared by every GraphQL carrier,
     * a request also selects the {@link #environment() environment} to target and may override the
     * {@link #accessToken() access token} the environment would otherwise supply.
     */
    non-sealed interface Request extends WhatsAppGraphQlOperation {
        /**
         * Returns the persisted document identifier the endpoint maps to the server-side compiled
         * GraphQL document for this operation.
         *
         * <p>The identifier is emitted as the {@code doc_id} field of the JSON request body. WhatsApp
         * Web resolves the live id from its persisted-query registry; Cobalt ships the resolved numeric
         * id directly per operation.
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
         * <p>Unlike the url-encoded carriers, which emit this string verbatim, the
         * {@code graph.whatsapp.com} transport parses it back into a JSON value and nests it under the
         * {@code variables} key of the JSON request body. The returned string is the
         * {@code JSON.stringify(variables)} value WhatsApp Web sends; an operation with no variables
         * returns the empty object {@code "{}"}.
         *
         * @return the JSON-encoded variables object, never {@code null}
         */
        String variables();

        /**
         * Returns the {@code graph.whatsapp.com} environment this operation targets.
         *
         * <p>The environment selects the request endpoint, the locale parameter name, and the default
         * shared app token, and it is the {@code environmentType} WhatsApp Web routes through
         * {@code WAWebRelayEnvironment.getEnvironment}.
         *
         * @return the target environment, never {@code null}
         */
        WhatsAppGraphQlEnvironment environment();

        /**
         * Returns an explicit {@code access_token} override for this operation, if any.
         *
         * <p>When present, this token authenticates the request in place of the environment's
         * {@link WhatsAppGraphQlEnvironment#defaultAccessToken() default app token}. When empty, the
         * environment's default is used. It must be present for {@link WhatsAppGraphQlEnvironment#GUEST}
         * operations, which carry no default token.
         *
         * @return an {@link Optional} wrapping the token override, or {@link Optional#empty()} to use
         *         the environment default
         */
        Optional<String> accessToken();
    }

    /**
     * Models the inbound side of a {@code graph.whatsapp.com} GraphQL operation.
     *
     * <p>The endpoint returns a JSON envelope; the client strips the {@code for(;;);} anti-hijack
     * prefix and hands the resulting GraphQL {@code data} object to the concrete response parser. This
     * interface carries no abstract methods; it exists as the closed counterpart of {@link Request} so
     * the entire {@code graph.whatsapp.com} GraphQL surface forms a single sealed hierarchy, and it
     * carries the shared {@link #getTypename(JSONObject)} helper for branching on the GraphQL
     * {@code __typename} discriminator.
     */
    non-sealed interface Response extends WhatsAppGraphQlOperation {
        /**
         * Returns the GraphQL {@code __typename} discriminator carried by a {@code graph.whatsapp.com}
         * GraphQL response object, if any.
         *
         * <p>These responses are GraphQL payloads whose concrete shape is identified by the synthetic
         * {@code __typename} field; concrete responses pull that field through this helper before
         * projecting the rest of the payload onto a domain model.
         *
         * @implNote This implementation mirrors the source {@code obj?.__typename} optional-chaining
         * semantics: a {@code null} {@code obj} or a missing field both collapse to
         * {@link Optional#empty()}.
         *
         * @param obj the GraphQL response JSON object to inspect, may be {@code null}
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
