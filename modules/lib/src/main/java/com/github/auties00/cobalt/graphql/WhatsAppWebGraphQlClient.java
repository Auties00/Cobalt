package com.github.auties00.cobalt.graphql;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Dispatches {@link WhatsAppWebGraphQlOperation.Request} operations over WhatsApp Web's {@code http_relay}
 * GraphQL transport.
 *
 * <p>The client mirrors the {@code whatsapp_web} branch of {@code WAWebRelayEnvironment}: it issues a
 * {@code POST https://web.whatsapp.com/graphql/} whose {@code application/x-www-form-urlencoded} body
 * carries the FB X-Controller anti-CSRF parameters followed by the GraphQL {@code variables},
 * {@code doc_id} and {@code locale}; reads the text response; strips the leading {@code for(;;);}
 * anti-hijack prefix; unwraps an optional {@code payload} envelope; and returns the GraphQL
 * {@code data} object. GraphQL and HTTP errors are surfaced as {@link WhatsAppServerRuntimeException}.
 *
 * <p>Authentication is the WhatsApp Web browser session, which is not a token the client computes: it
 * is the HttpOnly session cookie deposited by the canonical {@code /auth/token/} exchange, paired
 * with the {@code lsd} anti-CSRF token from the page bootstrap. Both are supplied to this client as
 * constructor inputs; the {@code jazoest} checksum is derived from {@code lsd} via
 * {@link WhatsAppGraphQlHttpSupport#jazoest}.
 *
 * @implNote This implementation sends only the minimal viable X-Controller parameter set
 * ({@code __a}, {@code __user}, {@code lsd}, {@code jazoest}) plus the {@code X-FB-LSD} and
 * {@code X-ASBD-ID} headers and the session cookie. WhatsApp Web additionally sends bootloader and
 * telemetry parameters ({@code __dyn}, {@code __csr}, {@code __hsdp}, {@code __spin_*}, and the like)
 * that optimise module delivery rather than gate the request; they are omitted here. The endpoint,
 * the {@code X-ASBD-ID} constant ({@value #X_ASBD_ID}) and {@code __user="0"} were recovered from a
 * live capture and may drift across releases.
 */
@WhatsAppWebModule(moduleName = "WAWebRelayEnvironment")
@WhatsAppWebModule(moduleName = "WAWebXControllerFetchUtils")
public final class WhatsAppWebGraphQlClient {
    /**
     * The fixed same-origin X-Controller endpoint every WhatsApp Web GraphQL operation targets.
     */
    private static final URI ENDPOINT = URI.create("https://web.whatsapp.com/graphql/");

    /**
     * The anti-scraping bot-detection id sent as the {@code X-ASBD-ID} header.
     *
     * <p>A public per-release constant rather than a per-user secret; recovered from a live capture.
     */
    private static final String X_ASBD_ID = "359341";

    /**
     * The unauthorized GraphQL error code the relay returns when the session cookie is missing or
     * stale.
     */
    private static final int UNAUTHORIZED_ERROR_CODE = 1675002;

    /**
     * The HTTP client used for the {@code POST}, reused across dispatches for connection pooling.
     */
    private final HttpClient httpClient;

    /**
     * The {@code Cookie} header value carrying the WhatsApp Web session cookie established by the
     * canonical {@code /auth/token/} exchange.
     */
    private final String sessionCookie;

    /**
     * The {@code lsd} anti-CSRF token from the page bootstrap, sent as the {@code lsd} body field and
     * the {@code X-FB-LSD} header.
     */
    private final String lsdToken;

    /**
     * The remapped locale (for example {@code en_US}) sent as the {@code locale} body field.
     */
    private final String locale;

    /**
     * Constructs a WhatsApp Web GraphQL client backed by a default-configured {@link HttpClient}.
     *
     * @param sessionCookie the {@code Cookie} header value carrying the WhatsApp Web session cookie
     * @param lsdToken      the {@code lsd} anti-CSRF token from the page bootstrap
     * @param locale        the remapped locale, for example {@code en_US}
     * @throws NullPointerException if any argument is {@code null}
     */
    public WhatsAppWebGraphQlClient(String sessionCookie, String lsdToken, String locale) {
        this(HttpClient.newHttpClient(), sessionCookie, lsdToken, locale);
    }

    /**
     * Constructs a WhatsApp Web GraphQL client backed by a caller-supplied {@link HttpClient}.
     *
     * <p>Intended for tests that drive the client with a recording {@link HttpClient} stub, or for
     * embedders that want to share a connection pool with other Cobalt subsystems.
     *
     * @param httpClient    the HTTP client to use
     * @param sessionCookie the {@code Cookie} header value carrying the WhatsApp Web session cookie
     * @param lsdToken      the {@code lsd} anti-CSRF token from the page bootstrap
     * @param locale        the remapped locale, for example {@code en_US}
     * @throws NullPointerException if any argument is {@code null}
     */
    public WhatsAppWebGraphQlClient(HttpClient httpClient, String sessionCookie, String lsdToken, String locale) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.sessionCookie = Objects.requireNonNull(sessionCookie, "sessionCookie must not be null");
        this.lsdToken = Objects.requireNonNull(lsdToken, "lsdToken must not be null");
        this.locale = Objects.requireNonNull(locale, "locale must not be null");
    }

    /**
     * Dispatches the given WhatsApp Web GraphQL operation and returns the unwrapped GraphQL {@code data} object.
     *
     * <p>Encodes the url-encoded request body, POSTs it to {@link #ENDPOINT} with the session cookie
     * and X-Controller headers, strips the {@code for(;;);} prefix from the response, unwraps an
     * optional {@code payload} envelope, and returns the resulting object whose top-level fields are
     * the GraphQL {@code data} selections. A non-2xx status, an unparsable body, or a non-empty
     * {@code errors} array each raise {@link WhatsAppServerRuntimeException}.
     *
     * @param request the WhatsApp Web GraphQL operation to dispatch
     * @return the unwrapped GraphQL {@code data} object, never {@code null}
     * @throws NullPointerException         if {@code request} is {@code null}
     * @throws WhatsAppServerRuntimeException if the transport fails, the body cannot be parsed, or the
     *                                      relay reports GraphQL errors
     */
    @WhatsAppWebExport(moduleName = "WAWebRelayEnvironment", exports = "getEnvironment",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public JSONObject send(WhatsAppWebGraphQlOperation.Request request) {
        Objects.requireNonNull(request, "request must not be null");

        var httpRequest = HttpRequest.newBuilder(ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-FB-LSD", lsdToken)
                .header("X-ASBD-ID", X_ASBD_ID)
                .header("Cookie", sessionCookie)
                .header("Origin", "https://web.whatsapp.com")
                .header("Referer", "https://web.whatsapp.com/")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .POST(HttpRequest.BodyPublishers.ofString(encodeBody(request)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL request failed for " + request.name(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL request interrupted for " + request.name(), exception);
        }

        return parse(request, response);
    }

    /**
     * Encodes the url-encoded request body for the given operation.
     *
     * <p>The field order matches WhatsApp Web: the anti-CSRF parameters first, then the GraphQL
     * {@code variables}, {@code doc_id} and {@code locale}.
     *
     * @param request the WhatsApp Web GraphQL operation being dispatched
     * @return the {@code application/x-www-form-urlencoded} body string
     */
    private String encodeBody(WhatsAppWebGraphQlOperation.Request request) {
        var params = new LinkedHashMap<String, String>();
        params.put("__a", "1");
        params.put("__user", "0");
        params.put("lsd", lsdToken);
        params.put("jazoest", WhatsAppGraphQlHttpSupport.jazoest(lsdToken));
        params.put("variables", request.variables());
        params.put("doc_id", request.docId());
        params.put("locale", locale);

        var body = new StringBuilder();
        for (var entry : params.entrySet()) {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    /**
     * Parses a WhatsApp Web GraphQL HTTP response into the unwrapped GraphQL {@code data} object.
     *
     * @param request  the WhatsApp Web GraphQL operation that produced the response, used for error messages
     * @param response the HTTP response
     * @return the unwrapped GraphQL {@code data} object, never {@code null}
     * @throws WhatsAppServerRuntimeException if the body is unparsable, the status is non-2xx, or the
     *                                      relay reports GraphQL errors
     */
    private JSONObject parse(WhatsAppWebGraphQlOperation.Request request, HttpResponse<String> response) {
        var text = WhatsAppGraphQlHttpSupport.stripXssiPrefix(response.body());
        JSONObject json;
        try {
            json = JSON.parseObject(text);
        } catch (RuntimeException exception) {
            throw new WhatsAppServerRuntimeException("failed to parse WhatsApp Web GraphQL response for " + request.name(), exception);
        }
        if (json == null) {
            throw new WhatsAppServerRuntimeException("empty WhatsApp Web GraphQL response for " + request.name());
        }

        var payload = json.getJSONObject("payload");
        var result = payload != null ? payload : json;

        var status = response.statusCode();
        if (status < 200 || status >= 300) {
            var error = result.getJSONObject("error");
            var detail = error != null ? error.getString("message") : status + " " + response.body();
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL request for " + request.name() + " failed: " + detail);
        }

        var errors = result.getJSONArray("errors");
        if (errors != null && !errors.isEmpty()) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL request for " + request.name() + " returned errors: " + describeErrors(errors));
        }
        return result;
    }

    /**
     * Renders a WhatsApp Web GraphQL error array into a compact, single-line diagnostic string.
     *
     * @param errors the {@code errors} array from the WhatsApp Web GraphQL response
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
                summary.append(" (unauthorized; session cookie missing or stale)");
            }
        }
        return summary.toString();
    }
}
