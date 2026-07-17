package com.github.auties00.cobalt.registration.push.fcm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.push.fcm.checkin.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Drives the three-step Android FCM registration handshake against Google's HTTP endpoints, mutating an
 * {@link FcmSession} in place with the credentials it acquires.
 *
 * <p>The handshake is the standard sequence a real Android client performs the first time it boots a Firebase-backed
 * app:
 * <ol>
 *   <li>{@code POST android.clients.google.com/checkin} sends a gzipped AndroidCheckin protobuf and returns
 *       {@code (androidId, securityToken)} used as the MCS login credentials.</li>
 *   <li>{@code POST firebaseinstallations.googleapis.com/.../installations} runs only when {@link FcmConfig#useFis()}
 *       is {@code true} and returns a {@code fid} plus FIS auth token used as headers on the next call.</li>
 *   <li>{@code POST android.clients.google.com/c2dm/register3} returns the FCM push token surfaced via
 *       {@link FcmClient#getPushToken()}.</li>
 * </ol>
 *
 * <p>Each step is skipped when the corresponding session field is already populated, so
 * {@link #ensureCredentials(FcmSession)} is effectively a one-shot bootstrap on a fresh session and a no-op (or a
 * FIS-only refresh) on a session restored from {@link FcmClient#loadSession(FcmSession)}.
 */
final class FcmRegistration {
    /** The logger for {@link FcmRegistration}. */
    private static final System.Logger LOGGER = Log.get(FcmRegistration.class);

    /**
     * Endpoint for the first registration step.
     *
     * <p>Returns the server-assigned {@code androidId} and {@code securityToken} pair used as the MCS login
     * credentials.
     */
    private static final String CHECKIN_URL = "https://android.clients.google.com/checkin";

    /**
     * Endpoint for the third registration step.
     *
     * <p>Returns the FCM token the WhatsApp registration server pushes verification codes to.
     */
    private static final String REGISTER_URL = "https://android.clients.google.com/c2dm/register3";

    /**
     * Template for the Firebase Installations endpoint.
     *
     * <p>The single {@code %s} placeholder is the Firebase project id from {@link FcmConfig#projectId()}.
     */
    private static final String FIS_URL_TEMPLATE = "https://firebaseinstallations.googleapis.com/v1/projects/%s/installations";

    /**
     * Connect and request timeout applied to every HTTP call.
     */
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Number of random bytes used to seed a Firebase Installation Id.
     *
     * @implNote
     * This implementation uses 17 bytes because the FIS spec mandates that width so the URL-safe base64 encoding fits
     * in 22 characters after the leading nibble is fixed to {@code 0b0111}.
     */
    private static final int FID_BYTES = 17;

    /**
     * Length of the truncated, URL-safe base64 FID surfaced to the Firebase backend.
     */
    private static final int FID_TRUNCATED_LENGTH = 22;

    /**
     * Underlying HTTP client.
     *
     * <p>Built once with the configured proxy and reused across every step; all three handshake calls share connection
     * pools and TLS sessions through this instance.
     */
    private final HttpClient http;

    /**
     * Source of randomness for the per-checkin {@code logging_id} field and the FID material.
     *
     * @implNote
     * This implementation uses {@link SecureRandom} to avoid leaking machine-identifying patterns the server could
     * fingerprint as a non-real device.
     */
    private final SecureRandom random;

    /**
     * Constructs a registration helper bound to the given proxy.
     *
     * <p>Instances are owned by {@link FcmClient} and built in its constructor.
     *
     * @param proxy proxy URI ({@code http(s)://...}, {@code socks://...}), or {@code null} to dial Google directly
     */
    FcmRegistration(URI proxy) {
        this.http = newHttpClient(proxy);
        this.random = new SecureRandom();
    }

    /**
     * Runs only the registration steps that have not already produced the values they would produce, mutating
     * {@code session} in place.
     *
     * <p>Idempotent: a second call on a fully-populated session is a no-op, though FIS may still refresh near expiry.
     * Drives all three steps on a fresh session built by {@link FcmSession#newSession(FcmConfig)}; on a session
     * restored via {@link FcmClient#loadSession(FcmSession)} only the FIS refresh may run.
     *
     * @param session the session whose credentials are filled in
     * @throws IOException on any HTTP or protocol failure
     */
    void ensureCredentials(FcmSession session) throws IOException {
        if (session.androidId() == 0L || session.securityToken() == 0L) {
            if (Log.INFO) LOGGER.log(Level.INFO, "fcm registration step 1/3: checkin -> {0}", CHECKIN_URL);
            performCheckin(session);
        }
        if (session.config().useFis()) {
            var nowSeconds = System.currentTimeMillis() / 1000L;
            if (session.fisAuthToken().isEmpty() || session.fisExpiresAt() < nowSeconds + 60L) {
                if (Log.INFO) LOGGER.log(Level.INFO, "fcm registration step 2/3: fis install -> {0}", fisUrl(session));
                firebaseInstall(session);
            }
        } else {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fcm registration step 2/3: fis install skipped (legacy project)");
        }
        if (session.fcmToken().isEmpty()) {
            if (Log.INFO) LOGGER.log(Level.INFO, "fcm registration step 3/3: register3 -> {0}", REGISTER_URL);
            gcmRegister(session);
        }
    }

    /**
     * Sends the gzipped AndroidCheckin protobuf and stores the returned {@code androidId} and {@code securityToken} on
     * {@code session}.
     *
     * @implNote
     * This implementation impersonates a Nexus 7 ({@code "google/razor/flo:5.0.1/..."}) running SDK 30; the synthetic
     * device profile is deliberately stable so the server fingerprint stays consistent across embedders.
     *
     * @param session the session to mutate
     * @throws IOException if the HTTP call fails or the response omits the credentials
     */
    private void performCheckin(FcmSession session) throws IOException {
        var build = new FcmCheckinRequestBuildBuilder()
                .fingerprint("google/razor/flo:5.0.1/LRX22C/1602158:user/release-keys")
                .hardware("flo")
                .brand("google")
                .clientId("android-google")
                .timeMs(System.currentTimeMillis() / 1000L)
                .sdkVersion(30)
                .model("Nexus 7")
                .manufacturer("asus")
                .product("razor")
                .otaInstalled(false)
                .build();
        var event = new FcmCheckinRequestEventBuilder()
                .tag("event_log_start")
                .timeMs(System.currentTimeMillis())
                .build();
        var checkin = new FcmCheckinRequestCheckinBuilder()
                .build(build)
                .lastCheckinMs(0L)
                .event(event)
                .userNumber(0L)
                .build();
        var request = new FcmCheckinRequestBuilder()
                .id(0L)
                .checkin(checkin)
                .locale("en_US")
                .loggingId(random.nextLong() & 0x7FFFFFFFFFFFFFFFL)
                .timeZone("UTC")
                .version(3)
                .fragment(0L)
                .userSerialNumber(0L)
                .build();
        var body = FcmCheckinRequestSpec.encode(request);

        var http = HttpRequest.newBuilder()
                .uri(URI.create(CHECKIN_URL))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-protobuffer")
                .header("Content-Encoding", "gzip")
                .header("Accept-Encoding", "gzip")
                .header("User-Agent", "Android-Checkin/2.0 (generic JLS36G); gzip")
                .POST(HttpRequest.BodyPublishers.ofByteArray(gzip(body)))
                .build();
        var response = sendBytes(http, "checkin");
        var decoded = decodeMaybeGzipped(response);

        var parsed = FcmCheckinResponseSpec.decode(decoded);
        if (parsed.androidId() == 0L || parsed.securityToken() == 0L) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "checkin failed, no android id in response, size={0} bytes", decoded.length);
            throw new IOException("checkin returned no android id (response=" + decoded.length + " B)");
        }
        session.setAndroidId(parsed.androidId());
        session.setSecurityToken(parsed.securityToken());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "checkin succeeded, android id and security token acquired");
    }

    /**
     * Calls the Firebase Installations endpoint to obtain (or refresh) a {@code fid} plus FIS auth token, storing both
     * plus the refresh token and the absolute expiry on {@code session}.
     *
     * <p>Generates a fresh FID via {@link #generateFid()} only when the session does not already carry one; FIS allows
     * the server to confirm or override the candidate id, so whichever value the response carries is stored back via
     * {@link FcmSession#setFid(String)}.
     *
     * @param session the session to mutate
     * @throws IOException if the HTTP call fails or the JSON cannot be parsed
     */
    private void firebaseInstall(FcmSession session) throws IOException {
        var fid = session.fid().isEmpty() ? generateFid() : session.fid();
        var payload = new JSONObject()
                .fluentPut("fid", fid)
                .fluentPut("appId", session.config().appId())
                .fluentPut("authVersion", "FIS_v2")
                .fluentPut("sdkVersion", "a:17.0.0");
        var bodyBytes = JSON.toJSONBytes(payload);
        var clientHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"heartbeats\":[],\"version\":2}".getBytes(StandardCharsets.UTF_8));
        var cert = session.config().certSha1().replace(":", "").toUpperCase(Locale.ROOT);

        var http = HttpRequest.newBuilder()
                .uri(URI.create(fisUrl(session)))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 11; Pixel 2 Build/RP1A.201005.004) FirebaseInstallations/17.0.0")
                .header("X-Firebase-Client", clientHeader)
                .header("x-goog-api-key", session.config().apiKey())
                .header("X-Android-Package", session.config().packageName())
                .header("X-Android-Cert", cert)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                .build();
        var raw = sendBytes(http, "FIS install");
        var parsed = JSON.parseObject(raw);
        var authToken = parsed.getJSONObject("authToken");
        var token = authToken == null ? "" : authToken.getString("token");
        var expiresIn = authToken == null ? "0s" : authToken.getString("expiresIn");
        var seconds = parseDurationSeconds(expiresIn);

        session.setFid(parsed.getString("fid") != null ? parsed.getString("fid") : fid);
        session.setFisRefreshToken(parsed.getString("refreshToken") != null
                ? parsed.getString("refreshToken") : "");
        session.setFisAuthToken(token == null ? "" : token);
        session.setFisExpiresAt(System.currentTimeMillis() / 1000L + seconds);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "fis install succeeded, token expires in {0}s", seconds);
    }

    /**
     * Calls {@code c2dm/register3} and parses the {@code token=...} line out of the {@code key=value\n} form-encoded
     * response, storing the FCM token on {@code session}.
     *
     * <p>Authenticates via the {@code AidLogin} scheme using the {@code androidId:securityToken} pair from the previous
     * checkin step; without those credentials register3 returns {@code Error=AUTHENTICATION_FAILED}.
     *
     * @param session the session to mutate
     * @throws IOException if the HTTP call fails or the response does not contain a {@code token} entry
     */
    private void gcmRegister(FcmSession session) throws IOException {
        var cfg = session.config();
        var cert = cfg.certSha1().replace(":", "").toLowerCase(Locale.ROOT);
        var form = new LinkedHashMap<String, String>();
        form.put("app", cfg.packageName());
        form.put("sender", cfg.senderId());
        form.put("device", Long.toString(session.androidId()));
        form.put("cert", cert);
        form.put("app_ver", "1");
        form.put("target_ver", "30");
        form.put("info", "");
        form.put("X-subtype", cfg.senderId());
        form.put("X-subscription", cfg.senderId());
        form.put("X-app_ver", "1");
        form.put("X-osv", "30");
        form.put("X-cliv", "fiid-21.1.0");
        form.put("X-gmsv", "240913000");
        if (cfg.useFis()) {
            form.put("X-scope", "*");
            form.put("X-appid", session.fid());
            form.put("X-gmp_app_id", cfg.appId());
            form.put("X-Goog-Firebase-Installations-Auth", session.fisAuthToken());
            form.put("X-firebase-app-name-hash", "");
        } else {
            form.put("X-scope", "GCM");
        }
        var body = encodeForm(form).getBytes(StandardCharsets.UTF_8);
        var auth = "AidLogin " + session.androidId() + ":" + session.securityToken();

        var http = HttpRequest.newBuilder()
                .uri(URI.create(REGISTER_URL))
                .timeout(HTTP_TIMEOUT)
                .header("Authorization", auth)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Android-GCM/1.5 (generic LRX22C)")
                .header("app", cfg.packageName())
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
        var raw = new String(sendBytes(http, "register3"), StandardCharsets.UTF_8);
        String token = null;
        for (var line : raw.strip().split("\n")) {
            var idx = line.indexOf('=');
            if (idx > 0 && "token".equals(line.substring(0, idx))) {
                token = line.substring(idx + 1);
                break;
            }
        }
        if (token == null || token.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "register3 failed, no token in response, size={0} bytes", raw.length());
            throw new IOException("register3 returned no token: " + raw);
        }
        session.setFcmToken(token);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "register3 succeeded, token acquired, length={0}", token.length());
    }

    /**
     * Sends {@code request} synchronously and returns the raw response body bytes on a {@code 2xx} status.
     *
     * <p>Non-2xx responses and interruptions are rewritten into {@link IOException}s tagged with the step name for log
     * readability; the interrupt flag is restored before the {@link IOException} is thrown.
     *
     * @param request  the prepared HTTP request
     * @param stepName a short label folded into error messages
     * @return the raw response body bytes
     * @throws IOException on any non-2xx status, transport failure or interruption during the call
     */
    private byte[] sendBytes(HttpRequest request, String stepName) throws IOException {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "{0} http request failed, status={1}", stepName, response.statusCode());
                throw new IOException(stepName + " HTTP " + response.statusCode() + ": "
                        + new String(response.body(), StandardCharsets.UTF_8));
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "{0} http request succeeded, status={1}, size={2} bytes", stepName, response.statusCode(), response.body().length);
            return response.body();
        } catch (InterruptedException ie) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "{0} http request interrupted", stepName);
            Thread.currentThread().interrupt();
            throw new IOException(stepName + " interrupted", ie);
        }
    }

    /**
     * Builds the FIS endpoint URL by interpolating the project id from {@code session.config()} into
     * {@link #FIS_URL_TEMPLATE}.
     *
     * @param session the session whose config carries the project id
     * @return the fully-qualified FIS install URL
     */
    private String fisUrl(FcmSession session) {
        return FIS_URL_TEMPLATE.formatted(session.config().projectId());
    }

    /**
     * Generates a fresh Firebase Installation Id.
     *
     * <p>Each call produces a fresh candidate id; the FIS server may confirm or replace it.
     *
     * @implNote
     * This implementation produces {@link #FID_BYTES} random bytes with the leading nibble forced to {@code 0b0111}
     * (per the FIS spec) then URL-safe base64 encodes them and truncates to {@link #FID_TRUNCATED_LENGTH} characters.
     *
     * @return a new candidate FID
     */
    private String generateFid() {
        var raw = new byte[FID_BYTES];
        random.nextBytes(raw);
        raw[0] = (byte) (0b01110000 | (raw[0] & 0b00001111));
        return Base64.getUrlEncoder().encodeToString(raw).substring(0, FID_TRUNCATED_LENGTH);
    }

    /**
     * Parses a Google duration string like {@code "604800s"} into the underlying integer seconds.
     *
     * @implNote
     * This implementation returns {@code 0} for missing or malformed input rather than throwing, because the caller
     * folds the value straight into a clock comparison and zero is a safe expired sentinel that triggers a refresh on
     * the next {@link #ensureCredentials(FcmSession)} call.
     *
     * @param raw the duration string, e.g. {@code "604800s"}
     * @return the parsed seconds, or {@code 0} if unparseable
     */
    private static long parseDurationSeconds(String raw) {
        if (raw == null || raw.isEmpty()) return 0L;
        var trimmed = raw.endsWith("s") ? raw.substring(0, raw.length() - 1) : raw;
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException _) {
            return 0L;
        }
    }

    /**
     * URL-encodes a key/value map into the {@code application/x-www-form-urlencoded} wire format expected by
     * {@code register3}, preserving insertion order.
     *
     * @implNote
     * Insertion order is load-bearing because the native client emits the same fields in the same sequence; the caller
     * supplies a {@link LinkedHashMap} so the wire bytes stay stable and predictable.
     *
     * @param form the key/value entries to encode
     * @return the encoded form body
     */
    private static String encodeForm(Map<String, String> form) {
        var out = new StringBuilder();
        for (var e : form.entrySet()) {
            if (!out.isEmpty()) out.append('&');
            out.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            out.append('=');
            out.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return out.toString();
    }

    /**
     * Gzip-compresses the given bytes for use as the {@code Content-Encoding: gzip} body of the checkin POST.
     *
     * @param data the raw protobuf bytes
     * @return the gzipped bytes
     * @throws IOException if the {@link GZIPOutputStream} writer fails, which in practice is impossible for an
     *                     in-memory byte sink
     */
    private static byte[] gzip(byte[] data) throws IOException {
        var out = new ByteArrayOutputStream(data.length);
        try (var gz = new GZIPOutputStream(out)) {
            gz.write(data);
        }
        return out.toByteArray();
    }

    /**
     * Tries to gunzip {@code data}, falling back to the input verbatim when the bytes are not a valid gzip stream.
     *
     * <p>The checkin server may reply with either form depending on the {@code Accept-Encoding} negotiation; this
     * fallback keeps the decoder agnostic to the negotiated encoding.
     *
     * @param data the raw response bytes
     * @return the decompressed bytes, or the input unchanged if it was not gzipped
     */
    private static byte[] decodeMaybeGzipped(byte[] data) {
        try (var gz = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return gz.readAllBytes();
        } catch (IOException _) {
            return data;
        }
    }

    /**
     * Builds an {@link HttpClient} configured with {@link #HTTP_TIMEOUT} and the optional caller-supplied proxy.
     *
     * @implNote
     * This implementation falls back to proxy port {@code 8080} when {@code proxy.getPort()} returns {@code -1}, and
     * follows redirects in {@link HttpClient.Redirect#NORMAL} mode so cross-scheme HTTPS-to-HTTPS redirects work but
     * downgrades do not.
     *
     * @param proxy proxy URI, or {@code null} for direct
     * @return a configured HTTP client
     */
    private static HttpClient newHttpClient(URI proxy) {
        var builder = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (proxy != null && proxy.getHost() != null) {
            var port = proxy.getPort() == -1 ? 8080 : proxy.getPort();
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), port)));
        }
        return builder.build();
    }
}
