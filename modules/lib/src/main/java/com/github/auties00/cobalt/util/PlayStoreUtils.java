package com.github.auties00.cobalt.util;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.exception.ProtobufDeserializationException;
import it.auties.protobuf.model.ProtobufType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Anonymously downloads an APK by Google Play package name.
 *
 * <p>The flow mirrors the Python {@code gplaydl} tool:
 *
 * <ol>
 *   <li>Request an anonymous {@code authToken} from the Aurora OSS token
 *       dispenser by posting a synthetic Android device profile. The dispenser
 *       rotates through community-contributed anonymous Google accounts, so no
 *       user Google account is ever involved.</li>
 *   <li>Query {@code /fdfe/details} to resolve the latest {@code versionCode}
 *       for the requested package.</li>
 *   <li>Acquire the app through {@code /fdfe/purchase} (the free-app equivalent
 *       of pressing "Install"). Best-effort, since already-owned apps return
 *       non-{@code 2xx} which is silently ignored.</li>
 *   <li>Call {@code /fdfe/delivery} and decode the returned
 *       {@code AppDeliveryData} protobuf to extract the signed CDN URLs for the
 *       base APK and every split, plus the auth cookies that gate the
 *       downloads.</li>
 *   <li>Open one HTTP stream per APK (base plus splits) with the cookies
 *       attached and return them to the caller.</li>
 * </ol>
 *
 * <p>The Cobalt mobile-companion tooling uses this to fetch the latest WhatsApp
 * / WhatsApp Business APK without going through a Google account. Consumers
 * typically combine {@link #latestVersion(String)} for cache-key resolution with
 * {@link #downloadApk(String, int)} for the actual download.
 */
public final class PlayStoreUtils {
    /**
     * The logger for {@link PlayStoreUtils}.
     */
    private static final System.Logger LOGGER = Log.get(PlayStoreUtils.class);

    /**
     * Holds the URL of the Aurora OSS anonymous token dispenser.
     */
    private static final String AURORA_DISPENSER_URL = "https://auroraoss.com/api/auth";

    /**
     * Holds the User-Agent string expected by the Aurora dispenser.
     *
     * <p>Sent on the request to {@link #AURORA_DISPENSER_URL}. The dispenser
     * rejects requests with arbitrary user-agents.
     */
    private static final String AURORA_USER_AGENT = "com.aurora.store-4.6.1-70";

    /**
     * Holds the base URL of the Google Play FDFE ("Finsky Device Front End")
     * API.
     */
    private static final String FDFE_BASE_URL = "https://android.clients.google.com/fdfe";

    /**
     * Holds the endpoint that returns the protobuf-encoded details document for
     * a given application package.
     */
    private static final String DETAILS_URL = FDFE_BASE_URL + "/details";

    /**
     * Holds the endpoint used to acquire a free application.
     */
    private static final String PURCHASE_URL = FDFE_BASE_URL + "/purchase";

    /**
     * Holds the endpoint that returns the signed CDN download URLs and auth
     * cookies.
     */
    private static final String DELIVERY_URL = FDFE_BASE_URL + "/delivery";

    /**
     * Holds the base-64 bitmask advertising which Play protocol features the
     * client understands.
     *
     * <p>The same blob is shipped by {@code gplaydl} and the Aurora Store;
     * required on every FDFE request via the {@code X-DFE-Encoded-Targets}
     * header.
     */
    private static final String X_DFE_ENCODED_TARGETS =
            "CAESN/qigQYC2AMBFfUbyA7SM5Ij/CvfBoIDgxXrBPsDlQUdMfOLAfoFrwEHgAcBrQYhoA0cGt4MKK0Y2gI";

    /**
     * Holds the fallback Finsky user-agent used when the dispenser response does
     * not include a device-specific user-agent under
     * {@code deviceInfoProvider.userAgentString}.
     */
    private static final String FALLBACK_FINSKY_USER_AGENT =
            "Android-Finsky/41.2.29-23 [0] [PR] 639844241 " +
            "(api=3,versionCode=84122900,sdk=34,device=lynx," +
            "hardware=lynx,product=lynx,platformVersionRelease=14," +
            "model=Pixel%207a,buildId=UQ1A.231205.015," +
            "isWideScreen=0,supportedAbis=arm64-v8a;armeabi-v7a;armeabi)";

    /**
     * Holds the locale reported in {@code Accept-Language} and
     * {@code X-DFE-UserLanguages}.
     */
    private static final String LOCALE = "en_US";

    /**
     * Holds the request-level timeout applied to every FDFE call and to each APK
     * download stream.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);

    /**
     * Holds the connect timeout applied to the {@link HttpClient} built for each
     * {@link #downloadApk(String)} invocation.
     */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Holds the tag byte that identifies a length-delimited value at protobuf
     * field number {@code 1} (the {@code name} field of {@link HttpCookie}).
     *
     * <p>Distinguishes cookie entries from OBB entries inside
     * {@link AppDeliveryData#field4Entries()}, whose tag-{@code 4} slot is
     * overloaded by the upstream schema.
     */
    private static final byte COOKIE_TAG_LENGTH_DELIMITED = 0x0A;

    /**
     * Holds the classpath path of the Aurora device profile posted to the
     * dispenser.
     *
     * <p>The JSON document's keys match the Aurora property names and the values
     * are their stringified forms, dumped from a Google Play system image on the
     * Android Studio emulator (x86_64).
     */
    private static final String DEVICE_PROFILE_RESOURCE = "/com/github/auties00/cobalt/util/sdk-gphone64-x86_64.json";

    /**
     * Holds the emulator-derived x86_64 device profile loaded from
     * {@link #DEVICE_PROFILE_RESOURCE}.
     */
    private static final Map<String, Object> DEVICE_PROFILE = loadDeviceProfile();

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private PlayStoreUtils() {
        throw new AssertionError();
    }

    /**
     * Downloads the latest build of the given Play Store package and returns the
     * open HTTP response streams grouped into a single {@link DownloadedApk}
     * descriptor.
     *
     * <p>Resolves the freshest APK at the cost of the {@code /fdfe/details}
     * round-trip. The base APK stream is always populated; configuration splits
     * returned by the delivery response appear in {@link DownloadedApk#splits()}
     * keyed by their split identifier (for example {@code config.arm64_v8a}) in
     * the order returned by the server. Every stream is connected; the caller
     * must drain and close each one (including the base and every map value).
     *
     * @implNote
     * This implementation forwards to
     * {@link #openDownloadedApk(HttpClient, String, int, Map)} after resolving
     * the version code via {@link #fetchVersion(HttpClient, String, Map)}. On
     * failure opening any split stream after the base stream was opened, the
     * already-opened streams are closed before the exception propagates.
     *
     * @param packageName the fully-qualified Android package identifier, for
     *                    example {@code com.whatsapp}
     * @return a populated {@link DownloadedApk} whose {@code packageName} equals
     *         the argument, {@code baseApk} is the base APK response stream, and
     *         {@code splits} is a live {@link SequencedMap} from split identifier
     *         to split APK response stream
     * @throws IOException if the dispenser refuses, an FDFE call fails, the app
     *                     is unavailable for the synthetic device profile, or
     *                     opening any of the CDN streams fails
     * @throws IllegalArgumentException if {@code packageName} is blank
     */
    public static DownloadedApk downloadApk(String packageName) throws IOException {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "downloading latest apk for package {0}", packageName);
        var client = newClient();
        var headers = buildFdfeHeaders(fetchAnonymousAuth(client));
        var version = fetchVersion(client, packageName, headers);
        return openDownloadedApk(client, packageName, version.code(), headers);
    }

    /**
     * Downloads a known {@code versionCode} of the given Play Store package.
     *
     * <p>Used when the caller already has the target {@code versionCode} (for
     * example, from a prior {@link #latestVersion(String)} call used to gate a
     * local cache). Skips the {@code /fdfe/details} round-trip entirely.
     *
     * @param packageName the fully-qualified Android package identifier
     * @param versionCode the exact {@code versionCode} to request from
     *                    {@code /fdfe/purchase} and {@code /fdfe/delivery}
     * @return a populated {@link DownloadedApk}
     * @throws IOException if the dispenser refuses, an FDFE call fails, the app
     *                     is unavailable for the synthetic device profile, or
     *                     opening any of the CDN streams fails
     * @throws IllegalArgumentException if {@code packageName} is blank
     */
    public static DownloadedApk downloadApk(String packageName, int versionCode) throws IOException {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "downloading apk for package {0} version {1}", packageName, versionCode);
        var client = newClient();
        var headers = buildFdfeHeaders(fetchAnonymousAuth(client));
        return openDownloadedApk(client, packageName, versionCode, headers);
    }

    /**
     * Builds the shared {@link HttpClient} used by {@link #downloadApk(String)}
     * and {@link #downloadApk(String, int)}.
     *
     * <p>Configured with redirect following and the connect timeout from
     * {@link #CONNECT_TIMEOUT}.
     *
     * @return a freshly built {@link HttpClient}
     */
    private static HttpClient newClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Performs the {@code /fdfe/purchase} acquisition and {@code /fdfe/delivery}
     * fetch for {@code versionCode}, then opens one streaming response per APK
     * URL and packages them into a {@link DownloadedApk}.
     *
     * <p>This is the shared body of the two {@code downloadApk} overloads.
     *
     * @param client the HTTP client
     * @param packageName the package identifier
     * @param versionCode the version code to acquire and deliver
     * @param headers the FDFE header set
     * @return a populated {@link DownloadedApk}
     * @throws IOException if any FDFE call or CDN open fails
     */
    private static DownloadedApk openDownloadedApk(HttpClient client, String packageName, int versionCode, Map<String, String> headers) throws IOException {
        acquire(client, packageName, versionCode, headers);
        var delivery = fetchDelivery(client, packageName, versionCode, headers);

        var baseStream = openDownloadStream(client, delivery.baseUrl(), delivery.cookies());
        var splits = new LinkedHashMap<String, InputStream>(delivery.splits().size());
        try {
            for (var split : delivery.splits()) {
                splits.put(split.name(), openDownloadStream(client, split.url(), delivery.cookies()));
            }
        } catch (IOException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to open split stream for " + packageName, e);
            closeQuietly(baseStream);
            splits.values().forEach(PlayStoreUtils::closeQuietly);
            throw e;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "opened apk download for {0} version {1}, {2} splits", packageName, versionCode, splits.size());
        return new DownloadedApk(packageName, baseStream, splits);
    }

    /**
     * Obtains an anonymous Play Store auth token from the Aurora dispenser.
     *
     * <p>Every FDFE call path uses the result to populate the
     * {@code Authorization: Bearer} header and the device metadata headers.
     *
     * @param client the HTTP client to use for the dispenser request
     * @return parsed credentials ready to be turned into FDFE headers
     * @throws IOException if the dispenser is unreachable, returns a
     *                     non-{@code 200} status, or returns a payload without an
     *                     {@code authToken}
     */
    private static AuthContext fetchAnonymousAuth(HttpClient client) throws IOException {
        var body = JSON.toJSONString(DEVICE_PROFILE);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(AURORA_DISPENSER_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("User-Agent", AURORA_USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while contacting token dispenser", e);
        }
        if (response.statusCode() != 200) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "token dispenser returned http {0}", response.statusCode());
            throw new IOException("Token dispenser returned HTTP " + response.statusCode());
        }
        var json = JSON.parseObject(response.body());
        if (json == null) {
            throw new IOException("Token dispenser returned an empty body");
        }
        var authToken = json.getString("authToken");
        if (authToken == null || authToken.isBlank()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "token dispenser response missing authToken");
            throw new IOException("Token dispenser returned no authToken");
        }
        var rawGsfId = json.getString("gsfId");
        var gsfId = rawGsfId == null ? "" : rawGsfId;
        var rawDfeCookie = json.getString("dfeCookie");
        var dfeCookie = rawDfeCookie == null ? "" : rawDfeCookie;
        var deviceCheckinConsistencyToken = json.getString("deviceCheckInConsistencyToken");
        var deviceConfigToken = json.getString("deviceConfigToken");
        var deviceInfoProvider = json.getJSONObject("deviceInfoProvider");
        var rawUserAgent = deviceInfoProvider != null
                ? deviceInfoProvider.getString("userAgentString")
                : null;
        var userAgent = rawUserAgent != null && !rawUserAgent.isBlank()
                ? rawUserAgent
                : FALLBACK_FINSKY_USER_AGENT;
        var mccMnc = deviceInfoProvider != null
                ? deviceInfoProvider.getString("mccMnc")
                : null;
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "obtained anonymous play store auth token");
        return new AuthContext(
                authToken,
                gsfId,
                dfeCookie,
                deviceCheckinConsistencyToken,
                deviceConfigToken,
                userAgent,
                mccMnc
        );
    }

    /**
     * Builds the shared header set expected by every FDFE endpoint.
     *
     * <p>{@link #downloadApk(String)} and {@link #latestVersion(String)} reuse
     * the result so each FDFE call shares the same auth, device-metadata, and
     * locale headers.
     *
     * @param auth the dispenser credentials
     * @return a mutable ordered map of HTTP headers (callers may add
     *         request-specific headers such as {@code Content-Type})
     */
    private static Map<String, String> buildFdfeHeaders(AuthContext auth) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + auth.authToken());
        headers.put("User-Agent", auth.userAgent());
        headers.put("X-DFE-Device-Id", auth.gsfId());
        headers.put("Accept-Language", "en-US");
        headers.put("X-DFE-Encoded-Targets", X_DFE_ENCODED_TARGETS);
        headers.put("X-DFE-Client-Id", "am-android-google");
        headers.put("X-DFE-Network-Type", "4");
        headers.put("X-DFE-Content-Filters", "");
        headers.put("X-Limit-Ad-Tracking-Enabled", "false");
        headers.put("X-Ad-Id", "");
        headers.put("X-DFE-UserLanguages", LOCALE);
        headers.put("X-DFE-Request-Params", "timeoutMs=4000");
        headers.put("X-DFE-Cookie", auth.dfeCookie());
        headers.put("X-DFE-No-Prefetch", "true");
        if (auth.deviceCheckinConsistencyToken() != null && !auth.deviceCheckinConsistencyToken().isBlank()) {
            headers.put("X-DFE-Device-Checkin-Consistency-Token", auth.deviceCheckinConsistencyToken());
        }
        if (auth.deviceConfigToken() != null && !auth.deviceConfigToken().isBlank()) {
            headers.put("X-DFE-Device-Config-Token", auth.deviceConfigToken());
        }
        if (auth.mccMnc() != null && !auth.mccMnc().isBlank()) {
            headers.put("X-DFE-MCCMNC", auth.mccMnc());
        }
        return headers;
    }

    /**
     * Fetches the latest published version of the given Play Store package (both
     * the numeric {@code versionCode} and the human-readable
     * {@code versionName}) without downloading any APK bytes.
     *
     * <p>Gates a local APK cache: calling this first, comparing the returned
     * {@code versionCode} against the cached one, and skipping
     * {@link #downloadApk(String, int)} when they match avoids a redundant
     * download. The internal {@link HttpClient} is closed before return, so the
     * call is safe to issue in a try-with-resources over it.
     *
     * @param packageName the fully-qualified Android package identifier
     * @return the latest version reported by the Play catalogue
     * @throws IOException if the dispenser refuses, the call fails, or the
     *                     response does not contain a version code
     * @throws IllegalArgumentException if {@code packageName} is blank
     */
    public static AppVersion latestVersion(String packageName) throws IOException {
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("packageName must not be blank");
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "querying latest version for package {0}", packageName);
        try (var client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(CONNECT_TIMEOUT)
                .build()) {
            var auth = fetchAnonymousAuth(client);
            var headers = buildFdfeHeaders(auth);
            return fetchVersion(client, packageName, headers);
        }
    }

    /**
     * Fetches the {@code /fdfe/details} document for {@code packageName} and
     * returns the latest version reported by the catalogue.
     *
     * <p>Shared by {@link #downloadApk(String)} and
     * {@link #latestVersion(String)}.
     *
     * @param client the HTTP client to use
     * @param packageName the package identifier
     * @param headers the FDFE header set produced by
     *                {@link #buildFdfeHeaders(AuthContext)}
     * @return the latest version advertised by the Play catalogue
     * @throws IOException if the call fails or the response does not contain a
     *                     version code (typically because the app is unavailable
     *                     for the synthetic device profile)
     */
    private static AppVersion fetchVersion(HttpClient client, String packageName, Map<String, String> headers) throws IOException {
        var raw = sendProtoGet(
                client,
                DETAILS_URL + "?doc=" + URLEncoder.encode(packageName, StandardCharsets.UTF_8),
                headers
        );
        var wrapper = decodeWrapper(raw, "details", packageName);
        var details = Optional.ofNullable(wrapper.payload())
                .map(Payload::detailsResponse)
                .map(DetailsResponse::docV2)
                .map(DocV2::docDetails)
                .map(DocDetails::appDetails)
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "details response missing AppDetails for {0}", packageName);
                    return new IOException("Details response missing AppDetails for '" + packageName + "'");
                });
        var code = details.versionCode();
        if (code == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "details response missing versionCode for {0}", packageName);
            throw new IOException("Details response missing versionCode for '" + packageName + "'");
        }
        var name = details.versionName();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved latest version for {0}: code={1}", packageName, code);
        return new AppVersion(code, name == null ? "" : name);
    }

    /**
     * Acquires a free application (the "Install" button equivalent).
     *
     * <p>Silently tolerates non-success responses because the app may already be
     * associated with the dispenser's rotated account from a previous request.
     *
     * @param client the HTTP client to use
     * @param packageName the package identifier
     * @param versionCode the version code
     * @param headers the FDFE header set
     */
    private static void acquire(HttpClient client, String packageName, int versionCode, Map<String, String> headers) {
        var body = "doc=" + URLEncoder.encode(packageName, StandardCharsets.UTF_8)
                + "&ot=1&vc=" + versionCode;
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(PURCHASE_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        headers.forEach((k, v) -> {
            if (v != null) {
                builder.header(k, v);
            }
        });
        try {
            client.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "acquired package {0} version {1}", packageName, versionCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // Best-effort: already-purchased apps return 4xx; downloads still work.
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "acquire request failed for {0} (already owned or unavailable)", packageName);
        }
    }

    /**
     * Fetches {@code /fdfe/delivery} and extracts the base APK URL, split APK
     * URLs, and the auth cookies that the CDN expects on the follow-up download
     * requests.
     *
     * <p>Used by {@link #openDownloadedApk(HttpClient, String, int, Map)}.
     *
     * @implNote
     * This implementation positions the cookies / OBB split at tag {@code 4}:
     * the upstream schema overloads that tag, so the body filters on the leading
     * {@link #COOKIE_TAG_LENGTH_DELIMITED} byte to keep only the cookie entries.
     *
     * @param client the HTTP client to use
     * @param packageName the package identifier
     * @param versionCode the version code
     * @param headers the FDFE header set
     * @return the combined delivery descriptor
     * @throws IOException if the response contains no download URL, which usually
     *                     indicates the app requires payment or is unavailable
     *                     for the synthetic device profile
     */
    private static Delivery fetchDelivery(HttpClient client, String packageName, int versionCode, Map<String, String> headers) throws IOException {
        var url = DELIVERY_URL
                + "?doc=" + URLEncoder.encode(packageName, StandardCharsets.UTF_8)
                + "&ot=1&vc=" + versionCode;
        var raw = sendProtoGet(client, url, headers);
        var wrapper = decodeWrapper(raw, "delivery", packageName);
        var data = Optional.ofNullable(wrapper.payload())
                .map(Payload::deliveryResponse)
                .map(DeliveryResponse::appDeliveryData)
                .orElseThrow(() -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "delivery response missing AppDeliveryData for {0}", packageName);
                    return new IOException("Delivery response missing AppDeliveryData for '" + packageName + "'");
                });
        var baseUrl = data.downloadUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "no download url for {0}, may require purchase", packageName);
            throw new IOException(
                    "No download URL for '" + packageName + "'; app may require purchase or be unavailable");
        }
        var splits = new ArrayList<SplitInfo>(data.splits().size());
        for (var split : data.splits()) {
            var splitUrl = split.downloadUrl();
            if (splitUrl == null || splitUrl.isBlank()) {
                continue;
            }
            var splitName = split.name();
            if (splitName == null || splitName.isBlank()) {
                splitName = "split-" + splits.size();
            }
            splits.add(new SplitInfo(splitName, splitUrl));
        }
        var cookies = data.field4Entries().stream()
                .filter(e -> e != null && e.length > 0 && e[0] == COOKIE_TAG_LENGTH_DELIMITED)
                .map(PlayStoreUtils::decodeCookie)
                .filter(c -> c != null && c.name() != null && !c.name().isEmpty() && c.value() != null)
                .collect(Collectors.toMap(HttpCookie::name, HttpCookie::value, (a, b) -> a, LinkedHashMap::new));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved delivery for {0}: {1} splits", packageName, splits.size());
        return new Delivery(baseUrl, splits, cookies);
    }

    /**
     * Decodes a tag-{@code 4} entry as a {@link HttpCookie}.
     *
     * <p>Returns {@code null} when the blob turns out to be an OBB entry or is
     * otherwise malformed; the caller filters those out.
     *
     * @param entry raw bytes of a tag-{@code 4} sub-message
     * @return the decoded cookie, or {@code null} if the entry cannot be decoded
     */
    private static HttpCookie decodeCookie(byte[] entry) {
        try {
            return PlayStoreUtilsHttpCookieSpec.decode(entry);
        } catch (ProtobufDeserializationException e) {
            return null;
        }
    }

    /**
     * Decodes an FDFE protobuf response stream into a {@link ResponseWrapper},
     * attributing any parse failure to {@code endpoint} and {@code packageName}
     * in the error message.
     *
     * <p>Used by {@link #fetchVersion(HttpClient, String, Map)} and
     * {@link #fetchDelivery(HttpClient, String, int, Map)}; the stream is always
     * closed on exit.
     *
     * @param raw the response body stream
     * @param endpoint a short label used in error messages ({@code "details"},
     *                 {@code "delivery"}, ...)
     * @param packageName the package identifier the call was issued for, used in
     *                    error messages
     * @return the decoded wrapper
     * @throws IOException if the body cannot be decoded
     */
    private static ResponseWrapper decodeWrapper(InputStream raw, String endpoint, String packageName) throws IOException {
        try (raw) {
            return PlayStoreUtilsResponseWrapperSpec.decode(new BufferedProtobufInputStream(raw));
        } catch (ProtobufDeserializationException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to decode " + endpoint + " response for " + packageName, e);
            throw new IOException("Failed to decode " + endpoint + " response for '" + packageName + "'", e);
        }
    }

    /**
     * Opens a streaming GET to {@code downloadUrl}, attaching {@code cookies} in
     * a single {@code Cookie} header.
     *
     * <p>Used by {@link #openDownloadedApk(HttpClient, String, int, Map)} for
     * both the base APK and every split.
     *
     * @param client the HTTP client to use
     * @param downloadUrl the signed CDN URL returned by
     *                    {@link #fetchDelivery(HttpClient, String, int, Map)}
     * @param cookies cookie pairs to attach to the request
     * @return the response body stream; the caller must close it
     * @throws IOException if the CDN returns a non-{@code 2xx} status or the
     *                     connection cannot be opened
     */
    private static InputStream openDownloadStream(HttpClient client, String downloadUrl, Map<String, String> cookies) throws IOException {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET();
        if (!cookies.isEmpty()) {
            var cookieHeader = cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; "));
            builder.header("Cookie", cookieHeader);
        }
        HttpResponse<InputStream> response;
        try {
            response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while opening APK download stream", e);
        }
        if (response.statusCode() / 100 != 2) {
            closeQuietly(response.body());
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apk download failed with http {0}", response.statusCode());
            throw new IOException("APK download failed with HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Sends an FDFE {@code GET} request that expects a protobuf response body.
     *
     * <p>Used by {@link #fetchVersion(HttpClient, String, Map)} and
     * {@link #fetchDelivery(HttpClient, String, int, Map)}.
     *
     * @param client the HTTP client to use
     * @param url the full request URL including query string
     * @param headers the FDFE header set
     * @return the response body stream; the caller must close it
     * @throws IOException if the call fails or the server returns a
     *                     non-{@code 200} status
     */
    private static InputStream sendProtoGet(HttpClient client, String url, Map<String, String> headers) throws IOException {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .header("Content-Type", "application/x-protobuf")
                .header("Accept", "application/x-protobuf");
        headers.forEach((k, v) -> {
            if (v != null) {
                builder.header(k, v);
            }
        });
        HttpResponse<InputStream> response;
        try {
            response = client.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted during GET " + url, e);
        }
        if (response.statusCode() != 200) {
            closeQuietly(response.body());
            if (Log.WARNING) LOGGER.log(Level.WARNING, "get {0} returned http {1}", url, response.statusCode());
            throw new IOException("GET " + url + " returned HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * Closes {@code stream} while swallowing any {@link IOException}.
     *
     * <p>The cleanup paths in
     * {@link #openDownloadedApk(HttpClient, String, int, Map)} and
     * {@link #openDownloadStream(HttpClient, String, Map)} use it; the swallowed
     * exception would only obscure the original failure.
     *
     * @param stream the stream to close
     */
    private static void closeQuietly(InputStream stream) {
        try {
            stream.close();
        } catch (IOException _) {
        }
    }

    /**
     * Loads the Aurora device profile from {@link #DEVICE_PROFILE_RESOURCE} and
     * returns it as an immutable map.
     *
     * <p>Runs once at class init to populate {@link #DEVICE_PROFILE}.
     *
     * @return an immutable map of Aurora-style property keys to values
     * @throws UncheckedIOException if the resource is missing or cannot be read
     */
    private static Map<String, Object> loadDeviceProfile() {
        try (var in = PlayStoreUtils.class.getResourceAsStream(DEVICE_PROFILE_RESOURCE)) {
            Objects.requireNonNull(in, "Missing classpath resource: " + DEVICE_PROFILE_RESOURCE);
            var json = JSON.parseObject(in);
            if (json == null) {
                throw new IOException("Device profile JSON is empty");
            }
            return json;
        } catch (IOException e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to load play store device profile", e);
            throw new UncheckedIOException("Failed to load device profile from " + DEVICE_PROFILE_RESOURCE, e);
        }
    }

    /**
     * Parsed Aurora dispenser credentials and device metadata.
     *
     * <p>Produced by {@link #fetchAnonymousAuth(HttpClient)} and consumed by
     * {@link #buildFdfeHeaders(AuthContext)}.
     *
     * @param authToken the bearer token accepted by the FDFE API
     * @param gsfId the GSF identifier reported via {@code X-DFE-Device-Id}
     * @param dfeCookie the opaque cookie reported via {@code X-DFE-Cookie}
     * @param deviceCheckinConsistencyToken optional token reported via
     *     {@code X-DFE-Device-Checkin-Consistency-Token}, or {@code null} when
     *     absent
     * @param deviceConfigToken optional token reported via
     *     {@code X-DFE-Device-Config-Token}, or {@code null}
     * @param userAgent Finsky user-agent associated with the profile
     * @param mccMnc optional MCC+MNC string reported via {@code X-DFE-MCCMNC}, or
     *     {@code null}
     */
    private record AuthContext(
            String authToken,
            String gsfId,
            String dfeCookie,
            String deviceCheckinConsistencyToken,
            String deviceConfigToken,
            String userAgent,
            String mccMnc
    ) {}

    /**
     * Latest version of a Play Store application as reported by the
     * {@code /fdfe/details} endpoint.
     *
     * <p>Returned from {@link #latestVersion(String)} and from the
     * version-resolution branch inside {@link #downloadApk(String)}.
     *
     * @param code the monotonic {@code versionCode} (for example
     *     {@code 252015}), suitable as a cache-invalidation key
     * @param name the human-readable {@code versionName} (for example
     *     {@code "2.25.20.15"}); never {@code null} but may be empty when the
     *     server omitted it
     */
    public record AppVersion(int code, String name) {}

    /**
     * Result of a {@link #downloadApk(String)} call, grouping the open HTTP
     * response streams that make up an App Bundle: the base APK plus zero or more
     * configuration splits keyed by their identifier.
     *
     * <p>The caller owns every stream in this record. Closing the record through
     * {@link #close()}, typically via a try-with-resources, closes
     * {@link #baseApk()} and every value in {@link #splits()}, aborting any
     * pending HTTP transfers.
     *
     * @implNote
     * {@link #close()} does not short-circuit on the first failure: it walks
     * every stream and rethrows the first {@link IOException} with subsequent
     * failures attached via {@link Throwable#addSuppressed(Throwable)}.
     *
     * @param packageName the fully-qualified Android package identifier the
     *     download was issued for, for example {@code com.whatsapp}
     * @param baseApk the response body stream serving the base APK
     * @param splits split APK response body streams keyed by their
     *     server-reported split identifier (for example {@code config.arm64_v8a}),
     *     preserving the order returned by the server; empty for apps that ship
     *     as a single APK
     */
    public record DownloadedApk(
            String packageName,
            InputStream baseApk,
            SequencedMap<String, InputStream> splits
    ) implements AutoCloseable {
        /**
         * Closes the base APK stream and every split stream, aborting their
         * pending HTTP transfers.
         *
         * <p>Called exactly once per record (or once via try-with-resources).
         * Subsequent {@link InputStream#read()} calls on the contained streams
         * fail with the underlying transport's closed-stream behaviour.
         *
         * @throws IOException if any stream fails to close; if several streams
         *     fail, the first failure is thrown with the others attached via
         *     {@link Throwable#addSuppressed(Throwable)}
         */
        @Override
        public void close() throws IOException {
            IOException error = null;
            try {
                baseApk.close();
            } catch (IOException e) {
                error = e;
            }
            for (var stream : splits.values()) {
                try {
                    stream.close();
                } catch (IOException e) {
                    if (error == null) {
                        error = e;
                    } else {
                        error.addSuppressed(e);
                    }
                }
            }
            if (error != null) {
                throw error;
            }
        }
    }

    /**
     * Combined delivery descriptor carrying everything needed to open the APK
     * download streams.
     *
     * <p>Produced by {@link #fetchDelivery(HttpClient, String, int, Map)} and
     * consumed by {@link #openDownloadedApk(HttpClient, String, int, Map)}.
     *
     * @param baseUrl CDN URL of the base APK
     * @param splits split APK descriptors in server-returned order, each already
     *     filtered down to entries with a usable download URL; empty for apps
     *     that ship as a single APK
     * @param cookies ordered cookie pairs shared by every download
     */
    private record Delivery(
            String baseUrl,
            List<SplitInfo> splits,
            Map<String, String> cookies
    ) {}

    /**
     * Name / URL pair extracted from a {@link SplitDeliveryData} entry.
     *
     * <p>A positional fallback name is applied by
     * {@link #fetchDelivery(HttpClient, String, int, Map)} when the server
     * omitted the split identifier.
     *
     * @param name split identifier, never {@code null} or blank
     * @param url signed CDN URL serving the split, never {@code null} or blank
     */
    private record SplitInfo(String name, String url) {}

    /**
     * Outer envelope of every response returned by the Google Play FDFE API.
     *
     * <p>The wire format nests the actual response under a single
     * {@link Payload} field.
     *
     * @param payload the payload carrying the request-specific response, or
     *     {@code null} if the response was empty
     */
    @ProtobufMessage
    record ResponseWrapper(
            @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
            Payload payload
    ) {}

    /**
     * Request-specific payload carried inside a {@link ResponseWrapper}.
     *
     * <p>Only the two variants used for APK downloads are declared here:
     * {@link #detailsResponse} at tag {@code 2} for {@code /fdfe/details} and
     * {@link #deliveryResponse} at tag {@code 21} for {@code /fdfe/delivery}.
     *
     * @param detailsResponse the details-response variant, or {@code null} for
     *     non-details responses
     * @param deliveryResponse the delivery-response variant, or {@code null} for
     *     non-delivery responses
     */
    @ProtobufMessage
    record Payload(
            @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
            DetailsResponse detailsResponse,
            @ProtobufProperty(index = 21, type = ProtobufType.MESSAGE)
            DeliveryResponse deliveryResponse
    ) {}

    /**
     * Response returned by the FDFE {@code /details} endpoint.
     *
     * @param docV2 the catalogue document for the requested application, or
     *     {@code null} if the server returned no result
     */
    @ProtobufMessage
    record DetailsResponse(
            @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
            DocV2 docV2
    ) {}

    /**
     * Google Play catalogue document (a.k.a. {@code DocV2}) returned inside a
     * {@link DetailsResponse}.
     *
     * <p>Only the nested {@link #docDetails} is declared; every other catalogue
     * field the server returns is skipped by the decoder.
     *
     * @param docDetails the document-type-specific detail block, or {@code null}
     *     if the server omitted it
     */
    @ProtobufMessage
    record DocV2(
            @ProtobufProperty(index = 13, type = ProtobufType.MESSAGE)
            DocDetails docDetails
    ) {}

    /**
     * Polymorphic wrapper around the document-type-specific detail payload.
     *
     * <p>Only the application variant is declared; other catalogue variants are
     * ignored.
     *
     * @param appDetails the application-specific details, or {@code null} if the
     *     document represents a non-app catalogue entry
     */
    @ProtobufMessage
    record DocDetails(
            @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
            AppDetails appDetails
    ) {}

    /**
     * Application-specific portion of a Google Play {@link DocV2}.
     *
     * @param versionCode the monotonic version code for the application, or
     *     {@code null} if the server omitted it
     * @param versionName the human-readable version string (for example
     *     {@code 2.25.20.15}), or {@code null} if the server omitted it
     */
    @ProtobufMessage
    record AppDetails(
            @ProtobufProperty(index = 3, type = ProtobufType.INT32)
            Integer versionCode,
            @ProtobufProperty(index = 4, type = ProtobufType.STRING)
            String versionName
    ) {}

    /**
     * Response returned by the FDFE {@code /delivery} endpoint.
     *
     * @param appDeliveryData the nested delivery data, or {@code null} if the
     *     server could not resolve a download
     */
    @ProtobufMessage
    record DeliveryResponse(
            @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
            AppDeliveryData appDeliveryData
    ) {}

    /**
     * Core payload returned by {@code /fdfe/delivery}: signed CDN URL for the
     * base APK, URLs for any split APKs, and the auth cookies that the CDN
     * validates on each download request.
     *
     * <p>The tag-{@code 4} slot is overloaded by the upstream schema: cookie
     * sub-messages start with the {@link #COOKIE_TAG_LENGTH_DELIMITED} byte and
     * are decoded by {@link #decodeCookie(byte[])}; other entries are OBB /
     * asset-pack metadata and are filtered out by
     * {@link #fetchDelivery(HttpClient, String, int, Map)}.
     *
     * @param downloadUrl signed CDN URL serving the base APK, or {@code null} if
     *     the server refused to issue one
     * @param field4Entries raw bytes of each sub-message at tag {@code 4}
     * @param splits split APK descriptors, empty for apps that ship as a single
     *     base APK
     */
    @ProtobufMessage
    record AppDeliveryData(
            @ProtobufProperty(index = 3, type = ProtobufType.STRING)
            String downloadUrl,
            @ProtobufProperty(index = 4, type = ProtobufType.BYTES)
            List<byte[]> field4Entries,
            @ProtobufProperty(index = 15, type = ProtobufType.MESSAGE)
            List<SplitDeliveryData> splits
    ) {}

    /**
     * Descriptor for a single split APK.
     *
     * <p>Each split has its own signed CDN URL but shares the auth cookies of its
     * parent {@link AppDeliveryData}.
     *
     * @param name split identifier as reported by the server, for example
     *     {@code config.arm64_v8a} or {@code config.en}; {@code null} when the
     *     server omitted it
     * @param downloadUrl signed CDN URL serving this split, or {@code null} if
     *     the server omitted it
     */
    @ProtobufMessage
    record SplitDeliveryData(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String name,
            @ProtobufProperty(index = 5, type = ProtobufType.STRING)
            String downloadUrl
    ) {}

    /**
     * Name / value pair authenticating a base or split APK download request
     * against the Google Play CDN.
     *
     * <p>Decoded by {@link #decodeCookie(byte[])} from a tag-{@code 4}
     * sub-message inside an {@link AppDeliveryData}.
     *
     * @param name cookie name as it appears in the request {@code Cookie} header
     * @param value cookie value as it appears in the request {@code Cookie}
     *     header
     */
    @ProtobufMessage
    record HttpCookie(
            @ProtobufProperty(index = 1, type = ProtobufType.STRING)
            String name,
            @ProtobufProperty(index = 2, type = ProtobufType.STRING)
            String value
    ) {}
}
