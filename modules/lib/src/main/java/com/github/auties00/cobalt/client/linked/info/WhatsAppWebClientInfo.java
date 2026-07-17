package com.github.auties00.cobalt.client.linked.info;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@link LinkedWhatsAppClientInfo} variant for the {@code web.whatsapp.com} JavaScript bundle and the macOS desktop client that
 * loads the same bundle through Mac Catalyst.
 *
 * <p>Resolution scrapes {@code web.whatsapp.com} at most once per JVM and produces a {@link ClientAppVersion} of the form
 * {@code 2.3000.X}, where {@code X} is the {@code client_revision} integer inlined into the landing page HTML.
 *
 * @implNote This implementation reads {@code client_revision} by streaming the response byte by byte against a rolling
 *           pattern matcher rather than parsing the HTML, because the marker sits inside an inline JSON blob that the
 *           bundle does not escape. The value is rediscovered on each JVM startup because Cobalt is not shipped as part of
 *           a WhatsApp release.
 * @see LinkedWhatsAppClientInfo
 */
@WhatsAppWebModule(moduleName = "WAWebBuildConstants")
final class WhatsAppWebClientInfo implements LinkedWhatsAppClientInfo {
    /**
     * The logger for {@link WhatsAppWebClientInfo}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppWebClientInfo.class);

    /**
     * Holds the resolved web client identity once it has been scraped.
     *
     * <p>Populated lazily by the first call to {@link #of()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #webInfoLock} for the double checked locking idiom; the
     *           {@code volatile} modifier is what lets the unsynchronised fast path observe a fully constructed instance.
     */
    private static volatile WhatsAppWebClientInfo webInfo;

    /**
     * Serialises initialisation of {@link #webInfo}.
     */
    private static final Object webInfoLock = new Object();

    /**
     * Holds the User-Agent header sent when scraping {@code web.whatsapp.com}.
     *
     * @implNote This implementation uses a realistic desktop Chrome value because a non browser User-Agent makes the
     *           server respond with a mobile redirect page that omits the {@code client_revision} marker.
     */
    private static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36";

    /**
     * Holds the URL of the WhatsApp Web landing page whose HTML embeds {@code client_revision}.
     */
    private static final URI WEB_UPDATE_URL = URI.create("https://web.whatsapp.com");

    /**
     * Holds the character pattern that {@link #queryWebInfo()} scans for inside the streamed landing page HTML.
     *
     * @implNote This implementation stores the pattern as a {@code char[]} so the rolling matcher can compare one byte at a
     *           time without allocating substrings, and matches the literal {@code "client_revision":} JSON key including
     *           the surrounding quote and colon so it cannot collide with prose mentions of the word {@code client_revision}
     *           elsewhere on the page.
     */
    private static final char[] WEB_UPDATE_PATTERN = "\"client_revision\":".toCharArray();

    /**
     * Holds the resolved {@link ClientAppVersion} this instance advertises.
     */
    private final ClientAppVersion version;

    /**
     * Constructs an immutable instance carrying the given resolved version.
     *
     * @param version the version parsed from {@code web.whatsapp.com}
     */
    private WhatsAppWebClientInfo(ClientAppVersion version) {
        this.version = version;
    }

    /**
     * Returns the cached web client identity, performing the landing page scrape on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed scrape is not cached, so a later call retries
     * the scrape.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #webInfo} field publishes the
     *           fully constructed instance to readers on the unsynchronised fast path.
     * @return the resolved web client identity
     * @throws IllegalStateException if the scrape returns a non 200 response or if the {@code client_revision} marker
     *                               cannot be located in the response body
     * @throws RuntimeException      if the underlying HTTP exchange fails with an {@link IOException} or
     *                               {@link InterruptedException}
     */
    public static WhatsAppWebClientInfo of() {
        if (webInfo == null) {
            synchronized (webInfoLock) {
                if(webInfo == null) {
                    webInfo = queryWebInfo();
                }
            }
        }
        return webInfo;
    }

    /**
     * Fetches {@code web.whatsapp.com} and extracts the {@code client_revision} integer into a fresh
     * {@link WhatsAppWebClientInfo}.
     *
     * @implNote This implementation streams the response body byte by byte against {@link #WEB_UPDATE_PATTERN} so the full
     *           HTML never has to be held in memory; on a complete pattern match it consumes the trailing ASCII digits up
     *           to the first non digit and folds them into the tertiary slot of a {@code 2.3000.X} {@link ClientAppVersion}.
     * @return a new {@link WhatsAppWebClientInfo} carrying the discovered version
     * @throws IllegalStateException if the server returns a non 200 status or the marker is missing from the body
     * @throws RuntimeException      if the underlying HTTP exchange fails
     */
    private static WhatsAppWebClientInfo queryWebInfo() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "scraping web landing page for client_revision");
        try(var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(WEB_UPDATE_URL)
                    .GET()
                    .header("User-Agent", WEB_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "none")
                    .header("Sec-Fetch-User", "?1")
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if(response.statusCode() != 200) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "web landing page query returned status {0}", response.statusCode());
                }
                throw new IllegalStateException("Cannot query web version: status code " + response.statusCode());
            }
            try (var inputStream = response.body()) {
                var patternIndex = 0;
                int value;
                while ((value = inputStream.read()) != -1) {
                    if (value == WEB_UPDATE_PATTERN[patternIndex]) {
                        if (++patternIndex == WEB_UPDATE_PATTERN.length) {
                            var clientVersion = 0;
                            while ((value = inputStream.read()) != -1 && Character.isDigit(value)) {
                                clientVersion *= 10;
                                clientVersion += value - '0';
                            }
                            var version = new ClientAppVersion(2, 3000, clientVersion);
                            if (Log.DEBUG) {
                                LOGGER.log(Level.DEBUG, "resolved web client info, version {0}", version);
                            }
                            return new WhatsAppWebClientInfo(version);
                        }
                    } else {
                        patternIndex = 0;
                        if (value == WEB_UPDATE_PATTERN[0]) {
                            patternIndex = 1;
                        }
                    }
                }
                var notFound = new IllegalStateException("Cannot find client_revision in web update response");
                if (Log.ERROR) {
                    LOGGER.log(Level.ERROR, "client_revision marker not found in web response", notFound);
                }
                throw notFound;
            }
        } catch (IOException | InterruptedException exception) {
            if (Log.ERROR) {
                LOGGER.log(Level.ERROR, "failed to query web version", exception);
            }
            throw new RuntimeException("Cannot query web version", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation discards the debug suffix WA Web's {@code VERSION_STR} appends when running outside the
     *           production gatekeeper bucket, because Cobalt always identifies itself as a release build to the server.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBuildConstants", exports = "VERSION_STR", adaptation = WhatsAppAdaptation.ADAPTED)
    public ClientAppVersion version() {
        return version;
    }
}
