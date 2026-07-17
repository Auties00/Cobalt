package com.github.auties00.cobalt.graphql;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Dispatches {@link WhatsAppGraphQlOperation.Request} operations over the {@code graph.whatsapp.com}
 * GraphQL transport.
 *
 * <p>This is the third of WhatsApp Web's persisted-GraphQL carriers, alongside the
 * {@link WhatsAppWebGraphQlClient} {@code http_relay} transport and the {@link FacebookGraphQlClient}
 * {@code http_comet} transport. Its defining trait is that it authenticates with a shared, hardcoded
 * app-level {@code access_token} baked into the release rather than with a per-user session, so it is
 * session-independent: WhatsApp Web reaches it before login and outside any signed-in session for
 * signup and pre-login reads, guest reads, public catalog reads, ML-model fetches, and Meta-AI search.
 *
 * <p>The transport issues a {@code POST} to one of two {@code graph.whatsapp.com} endpoints selected by
 * the operation's {@link WhatsAppGraphQlEnvironment}, with {@code Accept: application/json} and
 * {@code Content-Type: application/json}. The JSON body is
 * {@code {access_token, doc_id, variables, <localeParam>}}, where {@code variables} is a nested JSON
 * object (the operation's JSON-encoded {@link WhatsAppGraphQlOperation.Request#variables()} string
 * parsed back into a value) and {@code <localeParam>} is {@code "locale"} for the
 * {@link WhatsAppGraphQlEnvironment#WWW} environment and {@code "lang"} for
 * {@link WhatsAppGraphQlEnvironment#GUEST} and {@link WhatsAppGraphQlEnvironment#CATALOG}. The response
 * is read as text, the leading {@code for(;;);} anti-hijack prefix is stripped through
 * {@link WhatsAppGraphQlHttpSupport#stripXssiPrefix(String)}, the JSON is parsed, and the GraphQL
 * {@code data} object is returned. GraphQL and HTTP errors are surfaced as
 * {@link WhatsAppServerRuntimeException}.
 *
 * <p>The {@code access_token} is resolved from the operation:
 * {@link WhatsAppGraphQlOperation.Request#accessToken()} takes precedence when present, otherwise the
 * environment's {@link WhatsAppGraphQlEnvironment#defaultAccessToken() default app token} is used. The
 * {@link WhatsAppGraphQlEnvironment#GUEST} environment carries no default token, so a guest request
 * that supplies none fails fast, matching WhatsApp Web's "Missing WhatsApp guest GraphQL access token"
 * guard.
 *
 * @implNote The two shared app tokens ({@link #WWW_ACCESS_TOKEN}, {@link #CATALOG_ACCESS_TOKEN}) and
 * the two endpoints ({@link #DEFAULT_ENDPOINT}, {@link #CATALOG_ENDPOINT}) are public per-release
 * constants recovered from {@code WAWebGraphQLConstants} in the bundle and may drift across releases.
 * Unlike {@link FacebookGraphQlClient}, this transport never sends the {@code X-WA-Device-ID} header:
 * in WhatsApp Web that header is added only for the {@code facebook} environment, never for the
 * {@code graph.whatsapp.com} ones.
 */
@WhatsAppWebModule(moduleName = "WAWebRelayEnvironment")
@WhatsAppWebModule(moduleName = "WAWebGraphQLConstants")
public final class WhatsAppGraphQlClient {
    /**
     * The {@code graph.whatsapp.com} endpoint targeted by the {@link WhatsAppGraphQlEnvironment#WWW}
     * and {@link WhatsAppGraphQlEnvironment#GUEST} environments.
     */
    public static final String DEFAULT_ENDPOINT = "https://graph.whatsapp.com/graphql/";

    /**
     * The {@code graph.whatsapp.com} endpoint targeted by the {@link WhatsAppGraphQlEnvironment#CATALOG}
     * environment.
     */
    public static final String CATALOG_ENDPOINT = "https://graph.whatsapp.com/graphql/catalog";

    /**
     * The shared app-level {@code access_token} for the {@link WhatsAppGraphQlEnvironment#WWW}
     * environment.
     *
     * <p>A public per-release constant recovered from {@code WAWebGraphQLConstants}; it authenticates
     * every {@code whatsapp_www} request without a per-user session.
     */
    public static final String WWW_ACCESS_TOKEN = "WA|368348580915920|f740dc6ab59f4466ba09052010768cc1";

    /**
     * The shared app-level {@code access_token} for the {@link WhatsAppGraphQlEnvironment#CATALOG}
     * environment.
     *
     * <p>A public per-release constant recovered from {@code WAWebGraphQLConstants}; it authenticates
     * every {@code whatsapp_catalog} request without a per-user session.
     */
    public static final String CATALOG_ACCESS_TOKEN = "WA|787118555984857|7bb1544a3599aa180ac9a3f7688ba243";

    /**
     * The unauthorized GraphQL error code {@code graph.whatsapp.com} returns when the app token is
     * missing, malformed, or no longer accepted for the requested environment.
     */
    private static final int UNAUTHORIZED_ERROR_CODE = 1675002;

    /**
     * The HTTP client used for the {@code POST}, reused across dispatches for connection pooling.
     */
    private final HttpClient httpClient;

    /**
     * The remapped locale (for example {@code en_US}) sent under the environment's locale parameter.
     */
    private final String locale;

    /**
     * Constructs a WhatsApp GraphQL client backed by a default-configured {@link HttpClient}.
     *
     * @param locale the remapped locale, for example {@code en_US}
     * @throws NullPointerException if {@code locale} is {@code null}
     */
    public WhatsAppGraphQlClient(String locale) {
        this(HttpClient.newHttpClient(), locale);
    }

    /**
     * Constructs a WhatsApp GraphQL client backed by a caller-supplied {@link HttpClient}.
     *
     * <p>Intended for tests that drive the client with a recording {@link HttpClient} stub, or for
     * embedders that want to share a connection pool with other Cobalt subsystems.
     *
     * @param httpClient the HTTP client to use
     * @param locale     the remapped locale, for example {@code en_US}
     * @throws NullPointerException if {@code httpClient} or {@code locale} is {@code null}
     */
    public WhatsAppGraphQlClient(HttpClient httpClient, String locale) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
    }

    /**
     * Dispatches the given WhatsApp GraphQL operation and returns the unwrapped GraphQL {@code data} object.
     *
     * <p>Resolves the endpoint, locale parameter name, and {@code access_token} from the request's
     * {@link WhatsAppGraphQlOperation.Request#environment() environment} and optional
     * {@link WhatsAppGraphQlOperation.Request#accessToken() token override}, encodes the JSON body,
     * POSTs it, strips the {@code for(;;);} prefix from the response, and returns the GraphQL
     * {@code data} object (or the whole response when it carries no {@code data} wrapper). A non-2xx
     * status, an unparsable body, or a non-empty {@code errors} array each raise
     * {@link WhatsAppServerRuntimeException}.
     *
     * @param request the WhatsApp GraphQL operation to dispatch
     * @return the unwrapped GraphQL {@code data} object, never {@code null}
     * @throws NullPointerException           if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request targets {@link WhatsAppGraphQlEnvironment#GUEST}
     *                                        without an access token, the transport fails, the body
     *                                        cannot be parsed, or the endpoint reports GraphQL errors
     */
    @WhatsAppWebExport(moduleName = "WAWebRelayEnvironment", exports = "getEnvironment",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public JSONObject send(WhatsAppGraphQlOperation.Request request) {
        Objects.requireNonNull(request, "request must not be null");

        var environment = request.environment();
        var accessToken = request.accessToken()
                .or(environment::defaultAccessToken)
                .orElseThrow(() -> new WhatsAppServerRuntimeException("Missing WhatsApp guest GraphQL access token"));

        var httpRequest = HttpRequest.newBuilder(URI.create(environment.endpoint()))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(encodeBody(request, environment, accessToken)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new WhatsAppServerRuntimeException("WhatsApp GraphQL request failed for " + request.name(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WhatsAppServerRuntimeException("WhatsApp GraphQL request interrupted for " + request.name(), exception);
        }

        return parse(request, response);
    }

    /**
     * Encodes the JSON request body for the given operation, environment, and resolved token.
     *
     * <p>The body is {@code {access_token, doc_id, variables, <localeParam>}} in that order, with
     * {@code variables} nested as a JSON value: the operation's
     * {@link WhatsAppGraphQlOperation.Request#variables()} string is parsed back into an object rather
     * than embedded as a string, and the locale is emitted under the environment's
     * {@link WhatsAppGraphQlEnvironment#localeParameterName() locale parameter name}.
     *
     * @param request     the WhatsApp GraphQL operation being dispatched
     * @param environment the target environment supplying the locale parameter name
     * @param accessToken the resolved app-level or caller-supplied access token
     * @return the {@code application/json} body string
     */
    private String encodeBody(WhatsAppGraphQlOperation.Request request, WhatsAppGraphQlEnvironment environment, String accessToken) {
        var body = new LinkedHashMap<String, Object>();
        body.put("access_token", accessToken);
        body.put("doc_id", request.docId());
        body.put("variables", JSON.parse(request.variables()));
        body.put(environment.localeParameterName(), locale);
        return JSON.toJSONString(body);
    }

    /**
     * Parses a WhatsApp GraphQL HTTP response into the unwrapped GraphQL {@code data} object.
     *
     * <p>Strips the {@code for(;;);} prefix, parses the JSON, reads the {@code error} object's message
     * on a non-2xx status, throws on a non-empty {@code errors} array, and otherwise returns the
     * GraphQL {@code data} object, falling back to the whole response when there is no {@code data}
     * wrapper. There is no {@code payload} envelope here; that unwrap is specific to the
     * {@link WhatsAppWebGraphQlClient} transport.
     *
     * @param request  the WhatsApp GraphQL operation that produced the response, used for error messages
     * @param response the HTTP response
     * @return the unwrapped GraphQL {@code data} object, never {@code null}
     * @throws WhatsAppServerRuntimeException if the body is unparsable, the status is non-2xx, or the
     *                                        endpoint reports GraphQL errors
     */
    private JSONObject parse(WhatsAppGraphQlOperation.Request request, HttpResponse<String> response) {
        var text = WhatsAppGraphQlHttpSupport.stripXssiPrefix(response.body());
        JSONObject json;
        try {
            json = JSON.parseObject(text);
        } catch (RuntimeException exception) {
            throw new WhatsAppServerRuntimeException("failed to parse WhatsApp GraphQL response for " + request.name(), exception);
        }
        if (json == null) {
            throw new WhatsAppServerRuntimeException("empty WhatsApp GraphQL response for " + request.name());
        }

        var status = response.statusCode();
        if (status < 200 || status >= 300) {
            var error = json.getJSONObject("error");
            var detail = error != null ? error.getString("message") : status + " " + response.body();
            throw new WhatsAppServerRuntimeException("WhatsApp GraphQL request for " + request.name() + " failed: " + detail);
        }

        var errors = json.getJSONArray("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new WhatsAppServerRuntimeException("WhatsApp GraphQL request for " + request.name() + " returned errors: " + describeErrors(errors));
        }

        var data = json.getJSONObject("data");
        return data != null ? data : json;
    }

    /**
     * Renders a WhatsApp GraphQL error array into a compact, single-line diagnostic string.
     *
     * <p>Each entry contributes its {@code code} and {@code message}; the unauthorized code
     * ({@link #UNAUTHORIZED_ERROR_CODE}) is annotated so a rejected app token is easy to spot.
     *
     * @param errors the {@code errors} array from the WhatsApp GraphQL response
     * @return a human-readable summary of the error codes and messages
     */
    private static String describeErrors(JSONArray errors) {
        var summary = new StringBuilder();
        for (var i = 0; i < errors.size(); i++) {
            var error = errors.getJSONObject(i);
            if (error == null) {
                continue;
            }
            if (!summary.isEmpty()) {
                summary.append("; ");
            }
            var code = error.getInteger("code");
            summary.append('[').append(code).append("] ").append(error.getString("message"));
            if (code != null && code == UNAUTHORIZED_ERROR_CODE) {
                summary.append(" (unauthorized; app access token missing or rejected)");
            }
        }
        return summary.toString();
    }
}
