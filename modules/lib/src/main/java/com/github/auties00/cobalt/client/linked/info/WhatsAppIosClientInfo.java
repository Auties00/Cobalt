package com.github.auties00.cobalt.client.linked.info;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * {@link WhatsAppMobileClientInfo} variant for the consumer ({@code net.whatsapp.WhatsApp}) and business
 * ({@code net.whatsapp.WhatsAppSMB}) iOS WhatsApp bundles.
 *
 * <p>Resolution requires a single call to the public Apple App Store {@code itunes.apple.com/lookup} endpoint to read the
 * published version string; no signed binary is ever downloaded because the static secrets the iOS registration scheme
 * needs are embedded in this class.
 *
 * @implNote This implementation has no WA Web counterpart; the iOS registration token scheme is reverse engineered from the
 *           iOS WhatsApp IPA. The token algorithm is simpler than the Android counterpart in
 *           {@link WhatsAppAndroidClientInfo}: a single MD5 over a static secret plus the build hash plus the phone number,
 *           with no per request signing material.
 * @see WhatsAppMobileClientInfo
 */
final class WhatsAppIosClientInfo implements WhatsAppMobileClientInfo {
    /**
     * The logger for {@link WhatsAppIosClientInfo}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppIosClientInfo.class);

    /**
     * Holds the App Store lookup URL that returns JSON metadata for the consumer WhatsApp bundle.
     */
    private static final URI MOBILE_PERSONAL_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsApp");

    /**
     * Holds the App Store lookup URL that returns JSON metadata for the business WhatsApp bundle.
     */
    private static final URI MOBILE_BUSINESS_IOS_URL = URI.create("https://itunes.apple.com/lookup?bundleId=net.whatsapp.WhatsAppSMB");

    /**
     * Holds the User-Agent header sent when calling the App Store lookup API.
     *
     * @implNote This implementation mimics a recent mobile Safari on an iPhone because some catalog responses vary by
     *           User-Agent, so the value steers the endpoint to return metadata identical to what a real device sees.
     */
    private static final String MOBILE_IOS_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1";

    /**
     * Holds the resolved consumer iOS identity once it has been looked up.
     *
     * <p>Populated lazily by the first call to {@link #ofPersonal()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #personalIpaInfoLock} for the double checked locking idiom;
     *           the {@code volatile} modifier publishes a fully constructed instance to readers on the unsynchronised fast
     *           path.
     */
    private static volatile WhatsAppIosClientInfo personalIpaInfo;

    /**
     * Serialises initialisation of {@link #personalIpaInfo}.
     */
    private static final Object personalIpaInfoLock = new Object();

    /**
     * Holds the resolved business iOS identity once it has been looked up.
     *
     * <p>Populated lazily by the first call to {@link #ofBusiness()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #businessIpaInfoLock} for the double checked locking idiom;
     *           the {@code volatile} modifier publishes a fully constructed instance to readers on the unsynchronised fast
     *           path.
     */
    private static volatile WhatsAppIosClientInfo businessIpaInfo;

    /**
     * Serialises initialisation of {@link #businessIpaInfo}.
     */
    private static final Object businessIpaInfoLock = new Object();

    /**
     * Holds the static 40 character secret prefix used by the consumer iOS registration token algorithm.
     *
     * @implNote This implementation embeds the secret directly, reverse engineered from the consumer iOS WhatsApp IPA;
     *           rotation requires a binary release on Apple's side.
     */
    private static final String MOBILE_IOS_STATIC = "0a1mLfGUIBVrMKF1RdvLI5lkRBvof6vn0fD2QRSM";

    /**
     * Holds the static 40 character secret prefix used by the business iOS registration token algorithm.
     *
     * @implNote This implementation embeds the secret directly, reverse engineered from the business iOS WhatsApp IPA; it
     *           differs from {@link #MOBILE_IOS_STATIC} so consumer and business builds cannot impersonate each other.
     */
    private static final String MOBILE_BUSINESS_IOS_STATIC = "USUDuDYDeQhY4RF2fCSp5m3F6kJ1M2J8wS7bbNA2";

    /**
     * Holds the resolved {@link ClientAppVersion} returned by the App Store lookup, normalised to the {@code 2.X.Y} form.
     */
    private final ClientAppVersion version;

    /**
     * Holds whether this instance represents the WhatsApp Business IPA rather than the consumer IPA.
     */
    private final boolean business;

    /**
     * Constructs an immutable instance from the App Store lookup result.
     *
     * @param version  the parsed application version
     * @param business whether this represents the business variant
     */
    private WhatsAppIosClientInfo(ClientAppVersion version, boolean business) {
        this.version = version;
        this.business = business;
    }

    /**
     * Returns the cached consumer iOS identity, performing the App Store lookup on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed lookup is not cached, so a later call retries
     * the lookup.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #personalIpaInfo} field
     *           publishes the fully constructed instance to readers on the unsynchronised fast path.
     * @return the consumer iOS client identity
     * @throws RuntimeException if the App Store lookup fails
     */
    public static WhatsAppIosClientInfo ofPersonal() {
        if (personalIpaInfo == null) {
            synchronized (personalIpaInfoLock) {
                if(personalIpaInfo == null) {
                    personalIpaInfo = queryIpaInfo(false);
                }
            }
        }
        return personalIpaInfo;
    }

    /**
     * Returns the cached business iOS identity, performing the App Store lookup on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed lookup is not cached, so a later call retries
     * the lookup.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #businessIpaInfo} field
     *           publishes the fully constructed instance to readers on the unsynchronised fast path.
     * @return the business iOS client identity
     * @throws RuntimeException if the App Store lookup fails
     */
    public static WhatsAppIosClientInfo ofBusiness() {
        if (businessIpaInfo == null) {
            synchronized (businessIpaInfoLock) {
                if(businessIpaInfo == null) {
                    businessIpaInfo = queryIpaInfo(true);
                }
            }
        }
        return businessIpaInfo;
    }

    /**
     * Calls the App Store lookup API for the requested bundle and parses the returned version string into a
     * {@link ClientAppVersion}.
     *
     * <p>Returns {@code null} when the response is empty or missing the version field, so the calling accessor leaves the
     * singleton unpopulated and the next call retries.
     *
     * @implNote This implementation prepends {@code "2."} to App Store versions that lack the leading {@code "2."} prefix
     *           because the iOS marketing version is sometimes published in a year based form while WhatsApp's wire scheme
     *           expects the canonical {@code "2.X.Y"} form.
     * @param business {@code true} for the business variant, {@code false} for the consumer variant
     * @return a populated {@link WhatsAppIosClientInfo}, or {@code null} if the lookup returned no usable data
     * @throws RuntimeException if the HTTP exchange fails
     */
    private static WhatsAppIosClientInfo queryIpaInfo(boolean business) {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "querying app store for ios client info, business {0}", business);
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(business ? MOBILE_BUSINESS_IOS_URL : MOBILE_PERSONAL_IOS_URL)
                    .header("User-Agent", MOBILE_IOS_USER_AGENT)
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException("HTTP request failed with status code: " + response.statusCode());
            }

            var jsonObject = JSON.parseObject(response.body());
            var results = jsonObject.getJSONArray("results");
            if (results == null || results.isEmpty()) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "app store lookup returned no results for ios client info, business {0}", business);
                }
                return null;
            }

            var result = results.getJSONObject(0);
            var version = result.getString("version");
            if (version == null) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "app store lookup missing version field for ios client info, business {0}", business);
                }
                return null;
            }

            if (!version.startsWith("2.")) {
                version = "2." + version;
            }

            var parsedVersion = ClientAppVersion.of(version);
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "resolved ios client info, business {0}, version {1}", business, parsedVersion);
            }
            return new WhatsAppIosClientInfo(parsedVersion, business);
        } catch (IOException | InterruptedException e) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "failed to query ios version, business " + business, e);
            }
            throw new RuntimeException("Cannot query iOS version", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation returns the version reported by the Apple App Store lookup endpoint, normalised to the
     *           canonical {@code 2.X.Y} form WhatsApp servers expect.
     */
    @Override
    public ClientAppVersion version() {
        return version;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation reports the variant determined by which App Store bundle the lookup queried
     *           ({@link #MOBILE_PERSONAL_IOS_URL} versus {@link #MOBILE_BUSINESS_IOS_URL}).
     */
    @Override
    public boolean business() {
        return business;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation MD5s the lower case hex concatenation of the variant specific static secret
     *           ({@link #MOBILE_IOS_STATIC} or {@link #MOBILE_BUSINESS_IOS_STATIC}), the hex encoded build hash from
     *           {@link ClientAppVersion#toHash()}, and the decimal national phone number; no signed binary key material is
     *           involved on iOS, which is why no IPA download is needed.
     * @throws UnsupportedOperationException if MD5 is not available on the running JDK
     */
    @Override
    public String computeRegistrationToken(long nationalPhoneNumber) {
        try {
            var staticToken = business ? MOBILE_BUSINESS_IOS_STATIC : MOBILE_IOS_STATIC;
            var token = staticToken + HexFormat.of().formatHex(version.toHash()) + nationalPhoneNumber;
            var digest = MessageDigest.getInstance("MD5");
            digest.update(token.getBytes());
            var result = digest.digest();
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException exception) {
            throw new UnsupportedOperationException("Missing md5 implementation", exception);
        }
    }
}
