package com.github.auties00.cobalt.graphql;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.exception.linked.WhatsAppServerRuntimeException;
import com.github.auties00.cobalt.graphql.WhatsAppGraphQlHttpSupport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Acquires the WhatsApp Web {@code lsd} anti-CSRF token and the HttpOnly session cookie that the
 * {@code http_relay} transport needs to authenticate {@code POST /graphql/}.
 *
 * <p>This is the HTTP side of the relay credential-acquisition design (PATH A). It performs two steps
 * against {@code https://web.whatsapp.com}:
 * <ul>
 * <li>{@link #fetchLsd()} issues a browser-like {@code GET /} and scrapes the {@code lsd} token from
 * the bootstrap HTML, capturing any {@code Set-Cookie} bootstrap cookies into its private cookie jar;
 * <li>{@link #exchange(WhatsAppWebGraphQlCanonicalCredentials, String)} adapts
 * {@code WAWebCanonicalTokenExchange.exchangeNonceForToken} / {@code storeCanonicalCredentials}: it
 * POSTs the canonical credentials to the {@code /auth/token/} X-Controller route
 * ({@code WAXWhatsAppWebAuthControllerRouteBuilder}), and the server deposits the HttpOnly session
 * cookie via {@code Set-Cookie}, again captured into the jar.
 * </ul>
 *
 * <p>The session cookie is never read by name: it is HttpOnly and domain-scoped to
 * {@code .web.whatsapp.com}, so the client only ever replays the whole jar via {@link #cookieHeader()}.
 * That rendered {@code Cookie} header plus the {@code lsd} are what
 * {@code WhatsAppClient.establishWhatsAppWebGraphQlSession(cookie, lsd)} consumes.
 *
 * <p>The client owns its own {@link HttpClient} configured with an {@link CookieManager} so cookies
 * accumulate opaquely across the two requests; it follows redirects normally.
 *
 * @implNote Every request carries the browser {@code Sec-Fetch-*} metadata headers: the edge in front
 * of {@code web.whatsapp.com} enforces a Fetch-Metadata resource-isolation policy keyed on
 * {@code Sec-Fetch-Site} and rejects a request that omits it with {@code 400} before any application
 * logic runs, so {@link #fetchLsd()} sends the top-level-navigation set ({@code Sec-Fetch-Site: none},
 * {@code navigate}, {@code document}) and {@link #exchange(WhatsAppWebGraphQlCanonicalCredentials, String)} sends the
 * same-origin XHR set ({@code Sec-Fetch-Site: same-origin}, {@code cors}, {@code empty}) with
 * {@code Origin}/{@code Referer}. With those headers the {@code GET /} returns the full bootstrap HTML;
 * the {@code lsd} token is scraped with the primary {@code ["LSD",[],{"token":"..."}]} pattern and two
 * fallbacks, so a future bootstrap shape is widened there. The {@code /auth/token/} POST sends only the
 * minimal X-Controller parameter set ({@code __a}, {@code __user}, {@code lsd}, {@code jazoest}) plus
 * the four exchange fields, mirroring what the relay capture proved sufficient on {@code /graphql/};
 * if the server rejects the minimal set on {@code /auth/token/}, the FB bootloader parameters
 * ({@code __dyn}, {@code __csr}, and the like) would need to be added.
 */
@WhatsAppWebModule(moduleName = "WAWebCanonicalTokenExchange")
@WhatsAppWebModule(moduleName = "WAXWhatsAppWebAuthControllerRouteBuilder")
public final class WhatsAppWebGraphQlBootstrapClient {
    /**
     * The WhatsApp Web origin every bootstrap and exchange request targets.
     */
    private static final URI BASE = URI.create("https://web.whatsapp.com/");

    /**
     * The {@code /auth/token/} canonical exchange route.
     */
    private static final URI AUTH_TOKEN_ENDPOINT = URI.create("https://web.whatsapp.com/auth/token/");

    /**
     * A browser-like {@code User-Agent} sent on the bootstrap {@code GET} so the server returns the
     * real HTML page rather than an API or bot response.
     */
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0.0.0 Safari/537.36";

    /**
     * The anti-scraping bot-detection id sent as the {@code X-ASBD-ID} header, matching the WhatsApp Web GraphQL POST.
     */
    private static final String X_ASBD_ID = "359341";

    /**
     * The primary matcher for the FB {@code ServerJS} {@code LSD} module define embedding the token.
     */
    private static final Pattern LSD_PRIMARY =
            Pattern.compile("\\[\"LSD\",\\[\\],\\{\"token\":\"([^\"]+)\"\\}");

    /**
     * A fallback matcher for an {@code "lsd":{"token":"..."}} field shape.
     */
    private static final Pattern LSD_FALLBACK_FIELD =
            Pattern.compile("\"lsd\":\\{\"token\":\"([^\"]+)\"");

    /**
     * A fallback matcher for a bare {@code "token":"..."} appearing close after an {@code "LSD"} marker.
     *
     * <p>Applied only after the more specific patterns miss; the {@code [\s\S]} class lets the
     * intervening JSON span newlines.
     */
    private static final Pattern LSD_FALLBACK_NEAR_MARKER =
            Pattern.compile("\"LSD\"[\\s\\S]{0,64}?\"token\":\"([^\"]+)\"");

    /**
     * The HTTP client owning the {@link #cookieManager} cookie jar, reused for both requests.
     */
    private final HttpClient httpClient;

    /**
     * The cookie jar that opaquely captures every {@code Set-Cookie} the bootstrap {@code GET} and the
     * {@code /auth/token/} POST deposit, including the HttpOnly session cookie.
     */
    private final CookieManager cookieManager;

    /**
     * Constructs a self-contained bootstrap client with a fresh cookie jar.
     *
     * <p>The client builds its own {@link HttpClient} so the accumulated cookies are isolated from
     * other Cobalt HTTP traffic; the jar accepts all cookies because the session cookie is never read
     * by name, only replayed wholesale.
     */
    public WhatsAppWebGraphQlBootstrapClient() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Fetches the WhatsApp Web bootstrap page and scrapes the {@code lsd} anti-CSRF token from it.
     *
     * <p>Issues {@code GET https://web.whatsapp.com/} with a browser-like {@code User-Agent}, follows
     * redirects, and captures any bootstrap {@code Set-Cookie} into the jar so those cookies ride along
     * on the later {@code /auth/token/} POST. The body is scanned for the {@code lsd} token using the
     * primary {@code ["LSD",[],{"token":"..."}]} pattern, then two fallbacks.
     *
     * @return the scraped {@code lsd} token
     * @throws WhatsAppServerRuntimeException if the request fails, returns a non-2xx status, or the
     *                                        token cannot be located in the body
     */
    @WhatsAppWebExport(moduleName = "WAWebXControllerFetchUtils", exports = "getLSDToken",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public String fetchLsd() {
        var request = HttpRequest.newBuilder(BASE)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-User", "?1")
                .header("Upgrade-Insecure-Requests", "1")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL bootstrap GET failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL bootstrap GET interrupted", exception);
        }

        var status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL bootstrap GET returned status " + status);
        }

        var lsd = extractLsd(response.body());
        if (lsd == null) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL bootstrap response did not contain an lsd token");
        }
        return lsd;
    }

    /**
     * Extracts the {@code lsd} token from a bootstrap HTML body, or returns {@code null} when absent.
     *
     * <p>Tries the primary {@code ["LSD",[],{"token":"..."}]} pattern first, then the
     * {@code "lsd":{"token":"..."}} field shape, then a bare {@code "token":"..."} appearing near an
     * {@code "LSD"} marker.
     *
     * @param body the bootstrap HTML body
     * @return the matched token, or {@code null} when no pattern matches
     */
    private static String extractLsd(String body) {
        var primary = LSD_PRIMARY.matcher(body);
        if (primary.find()) {
            return primary.group(1);
        }
        var field = LSD_FALLBACK_FIELD.matcher(body);
        if (field.find()) {
            return field.group(1);
        }
        var near = LSD_FALLBACK_NEAR_MARKER.matcher(body);
        if (near.find()) {
            return near.group(1);
        }
        return null;
    }

    /**
     * Exchanges canonical credentials for the HttpOnly session cookie at {@code /auth/token/}.
     *
     * <p>Adapts {@code WAWebCanonicalTokenExchange}: POSTs an
     * {@code application/x-www-form-urlencoded} body carrying the four exchange fields
     * ({@code access_token}, {@code nonce}, {@code user_id} = {@code fbid}, {@code device_id}) plus the
     * X-Controller parameters ({@code __a=1}, {@code __user=0}, {@code lsd}, {@code jazoest}), with the
     * {@code X-FB-LSD} and {@code X-ASBD-ID} headers and the ambient cookie jar. The {@code access_token}
     * and {@code nonce} fields are omitted only when {@code null}; an empty string is sent verbatim,
     * matching WhatsApp Web's {@code i ?? ""} / {@code c ?? ""}. On success the server deposits the
     * session cookie via {@code Set-Cookie}, captured into the jar for {@link #cookieHeader()}.
     *
     * <p>The response body is the {@code for(;;);}-prefixed JSON; success is HTTP-ok plus a parsed
     * {@code payload.status == "success"}, where {@code payload} may be the top-level object or nested
     * under a {@code payload} key (the same unwrap {@code WhatsAppWebGraphQlClient} performs).
     *
     * @param credentials the canonical credentials, with {@link WhatsAppWebGraphQlCanonicalCredentials#deviceId()} already
     *                    stitched in via {@link WhatsAppWebGraphQlCanonicalCredentials#withDeviceId(long)}
     * @param lsd         the {@code lsd} token from {@link #fetchLsd()}
     * @return {@code true} when the server reported {@code payload.status == "success"}
     * @throws NullPointerException           if {@code credentials} or {@code lsd} is {@code null}
     * @throws WhatsAppServerRuntimeException if the request fails or the response cannot be parsed
     */
    @WhatsAppWebExport(moduleName = "WAWebCanonicalTokenExchange", exports = "exchangeNonceForToken",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public boolean exchange(WhatsAppWebGraphQlCanonicalCredentials credentials, String lsd) {
        Objects.requireNonNull(credentials, "credentials cannot be null");
        Objects.requireNonNull(lsd, "lsd cannot be null");

        var request = HttpRequest.newBuilder(AUTH_TOKEN_ENDPOINT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", USER_AGENT)
                .header("X-FB-LSD", lsd)
                .header("X-ASBD-ID", X_ASBD_ID)
                .header("Origin", "https://web.whatsapp.com")
                .header("Referer", "https://web.whatsapp.com/")
                .header("Sec-Fetch-Site", "same-origin")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Dest", "empty")
                .POST(HttpRequest.BodyPublishers.ofString(encodeBody(credentials, lsd)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL /auth/token/ exchange failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new WhatsAppServerRuntimeException("WhatsApp Web GraphQL /auth/token/ exchange interrupted", exception);
        }

        var status = response.statusCode();
        if (status < 200 || status >= 300) {
            return false;
        }

        JSONObject json;
        try {
            json = JSON.parseObject(WhatsAppGraphQlHttpSupport.stripXssiPrefix(response.body()));
        } catch (RuntimeException exception) {
            throw new WhatsAppServerRuntimeException("failed to parse WhatsApp Web GraphQL /auth/token/ response", exception);
        }
        if (json == null) {
            return false;
        }

        var payload = json.getJSONObject("payload");
        var result = payload != null ? payload : json;
        return "success".equals(result.getString("status"));
    }

    /**
     * Builds the url-encoded {@code /auth/token/} request body for the given credentials and token.
     *
     * <p>The {@code access_token} and {@code nonce} fields are emitted only when non-{@code null}; an
     * empty string is sent as-is. The remaining fields are always present.
     *
     * @param credentials the canonical credentials
     * @param lsd         the {@code lsd} token
     * @return the {@code application/x-www-form-urlencoded} body string
     */
    private static String encodeBody(WhatsAppWebGraphQlCanonicalCredentials credentials, String lsd) {
        var params = new LinkedHashMap<String, String>();
        if (credentials.accessToken() != null) {
            params.put("access_token", credentials.accessToken());
        }
        if (credentials.nonce() != null) {
            params.put("nonce", credentials.nonce());
        }
        params.put("user_id", Long.toString(credentials.fbid()));
        params.put("device_id", Long.toString(credentials.deviceId()));
        params.put("__a", "1");
        params.put("__user", "0");
        params.put("lsd", lsd);
        params.put("jazoest", WhatsAppGraphQlHttpSupport.jazoest(lsd));

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
     * Renders the captured {@code web.whatsapp.com} cookies into a {@code Cookie} header value.
     *
     * <p>Reads every cookie the jar holds for {@link #BASE} and joins them as
     * {@code name=value; name=value}, the form {@code WhatsAppClient.establishWhatsAppWebGraphQlSession} expects.
     * The session cookie deposited by {@link #exchange(WhatsAppWebGraphQlCanonicalCredentials, String)} is included even
     * though it is HttpOnly, because the Java cookie jar captures it opaquely regardless of the
     * HttpOnly flag.
     *
     * @return the rendered {@code Cookie} header value, possibly empty when the jar holds no cookies
     */
    public String cookieHeader() {
        var joiner = new StringJoiner("; ");
        for (var cookie : cookieManager.getCookieStore().get(BASE)) {
            joiner.add(cookie.getName() + "=" + cookie.getValue());
        }
        return joiner.toString();
    }
}
