package com.github.auties00.cobalt.wire.graphql.facebook;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Optional;

/**
 * Models the sealed root of every {@code http_comet} GraphQL operation in Cobalt.
 *
 * <p>The Facebook GraphQL transport is the carrier WhatsApp Web uses for the WhatsApp Business ad-creation and
 * click-to-WhatsApp (CTWA) GraphQL surface, which is served by Meta's graph endpoint rather than by a
 * WhatsApp server. Each operation is dispatched as a {@code POST https://graph.facebook.com/graphql}
 * whose JSON body carries the per-user Facebook {@code access_token}, the persisted {@code doc_id},
 * the JSON-encoded {@code variables}, and the remapped {@code locale}. The {@code access_token} is
 * minted over the WhatsApp socket (no facebook.com browser login is required); the relay maps the
 * {@code doc_id} to a server-side compiled document.
 *
 * <p>This hierarchy mirrors the role {@code MexStanza} plays for the MEX transport and
 * {@link WhatsAppWebGraphQlOperation} plays for the WhatsApp Web GraphQL transport: it
 * collapses each compiled GraphQL document into a sealed operation permitting exactly two variants,
 * {@link Request} and {@link Response}. All three transports share the same persisted-query identity
 * and operation shape; the Facebook GraphQL transport differs in carrier (Meta graph HTTP endpoint), body
 * encoding (a JSON object rather than a url-encoded form or a stanza stanza), and authentication (an
 * explicit Facebook {@code access_token} body field rather than a session cookie or the socket
 * session). The Facebook GraphQL transport is not stanza-based, so it lives outside the {@code stanza} package
 * alongside the WhatsApp Web GraphQL transport rather than with the socket-carried operation families.
 *
 * @apiNote The Facebook GraphQL transport reaches Meta's graph endpoint and serves the WhatsApp Business
 * ads/CTWA surface; it requires a Facebook {@code access_token} minted for the linked account. It is
 * unrelated to core messaging and most embedders never need it.
 */
@WhatsAppWebModule(moduleName = "CometRelay")
@WhatsAppWebModule(moduleName = "WAWebAdsRelayEnvironment")
public sealed interface FacebookGraphQlOperation permits FacebookGraphQlOperation.Request, FacebookGraphQlOperation.Response {
    /**
     * Models the outbound side of a Facebook GraphQL operation.
     *
     * <p>Every concrete operation's request implements this interface directly. A request exposes the
     * persisted document identifier emitted as the {@code doc_id} body field, the GraphQL operation
     * name used for perf telemetry, and the JSON-encoded {@code variables} object emitted as the
     * {@code variables} body field. The Facebook {@code access_token} and the {@code locale} are
     * session-level concerns supplied by the transport client, not by the operation.
     */
    non-sealed interface Request extends FacebookGraphQlOperation {
        /**
         * Returns the persisted document identifier the Meta graph endpoint maps to the server-side
         * compiled GraphQL document for this operation.
         *
         * <p>The identifier is emitted as the {@code doc_id} field of the JSON request body. It is the
         * numeric id exported by the operation's compiled {@code *_facebookRelayOperation} module;
         * Cobalt ships the resolved id directly per operation.
         *
         * @return the persisted document identifier, never {@code null}
         */
        String docId();

        /**
         * Returns the GraphQL operation name carried by this request.
         *
         * <p>The name tags query latency and error metrics on the GraphQL perf tracker; Cobalt keeps
         * it on each request so embedders mirroring that telemetry surface can emit the same tag.
         *
         * @return the GraphQL operation name, never {@code null}
         */
        String name();

        /**
         * Returns the JSON-encoded {@code variables} object for this operation.
         *
         * <p>The returned string is placed under the {@code variables} key of the JSON request body;
         * it is the {@code JSON.stringify(variables)} value WhatsApp Web sends. An operation with no
         * variables returns the empty object {@code "{}"}.
         *
         * @return the JSON-encoded variables object, never {@code null}
         */
        String variables();
    }

    /**
     * Models the inbound side of a Facebook GraphQL operation.
     *
     * <p>The Meta graph endpoint returns a JSON envelope; the client unwraps the GraphQL {@code data}
     * object and hands it to the concrete response parser. This interface carries no abstract methods;
     * it exists as the closed counterpart of {@link Request} so the entire Facebook GraphQL surface forms a
     * single sealed hierarchy, and it carries the shared {@link #getTypename(JSONObject)} helper for
     * branching on the GraphQL {@code __typename} discriminator.
     */
    non-sealed interface Response extends FacebookGraphQlOperation {
        /**
         * Returns the GraphQL {@code __typename} discriminator carried by a Facebook GraphQL response object, if
         * any.
         *
         * <p>Facebook GraphQL responses are GraphQL payloads whose concrete shape is identified by the synthetic
         * {@code __typename} field; concrete responses pull that field through this helper before
         * projecting the rest of the payload onto a domain model.
         *
         * @implNote This implementation mirrors the source {@code obj?.__typename} optional-chaining
         * semantics: a {@code null} {@code obj} or a missing field both collapse to
         * {@link Optional#empty()}.
         *
         * @param obj the Facebook GraphQL response JSON object to inspect, may be {@code null}
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
