package com.github.auties00.cobalt.client.linked.info;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.meta.model.WhatsAppWebPlatform;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersionBuilder;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@link LinkedWhatsAppClientInfo} variant for the Microsoft Store WhatsApp Desktop hybrid shell on Windows.
 *
 * <p>Resolution reuses the {@link WhatsAppWebClientInfo} bundle version for the first three slots and folds the Microsoft
 * Store package build into the {@code quaternary} slot of the resulting {@link ClientAppVersion}. The server reads that
 * quaternary slot to identify a connection as coming from the hybrid shell rather than a plain browser tab, and (when the
 * build is six digits long) derives further handshake fields from its halves, so a plausible value is required for the
 * handshake to be accepted.
 *
 * @implNote This implementation approximates the real shell, where the native binary appends a {@code windowsBuild} query
 *           parameter to the bundle URL that {@code WAWebBuildConstants} reads back. Cobalt instead queries the public
 *           Microsoft Store display catalog for the {@code 5319275A.WhatsAppDesktop} package and folds its version into the
 *           same six digit form, falling back to {@link #DEFAULT_WINDOWS_BUILD} when the catalog query fails so pairing
 *           still succeeds offline.
 * @see LinkedWhatsAppClientInfo
 * @see WhatsAppWebClientInfo
 */
@WhatsAppWebModule(moduleName = "WAWebBuildConstants", platform = WhatsAppWebPlatform.WINDOWS)
final class WhatsAppWindowsClientInfo implements LinkedWhatsAppClientInfo {
    /**
     * The logger for {@link WhatsAppWindowsClientInfo}.
     */
    private static final System.Logger LOGGER = Log.get(WhatsAppWindowsClientInfo.class);

    /**
     * Holds the resolved Windows client identity once it has been built.
     *
     * <p>Populated lazily by the first call to {@link #of()} and reused by every subsequent caller in the JVM.
     *
     * @implNote This implementation pairs the field with {@link #windowsInfoLock} for the double checked locking idiom; the
     *           {@code volatile} modifier is what lets the unsynchronised fast path observe a fully constructed instance.
     */
    private static volatile WhatsAppWindowsClientInfo windowsInfo;

    /**
     * Serialises initialisation of {@link #windowsInfo}.
     */
    private static final Object windowsInfoLock = new Object();

    /**
     * Holds the Microsoft Store display catalog endpoint that returns metadata for the {@code 5319275A.WhatsAppDesktop} UWP
     * package.
     *
     * <p>The product id {@code 9NKSQGP7F2NH} is the public Microsoft Store identifier for WhatsApp Desktop. The response is
     * a JSON document whose package metadata carries the shipping build string read by {@link #queryWindowsBuild()}.
     */
    private static final URI WINDOWS_STORE_URL = URI.create(
            "https://displaycatalog.mp.microsoft.com/v7.0/products/9NKSQGP7F2NH?languages=en-us&market=US&fieldsTemplate=Details"
    );

    /**
     * Holds the JSON marker that precedes the dotted version inside the Microsoft Store package full name field.
     *
     * @implNote This implementation anchors on {@code "PackageFullName":"5319275A.WhatsAppDesktop_"} so it skips the
     *           preceding family name entry (the family hash with no version) and the next underscore reliably terminates
     *           the dotted version substring. A typical full name is
     *           {@code "5319275A.WhatsAppDesktop_2.2613.101.0_neutral_~_cv1g1gvanyjgm"}.
     */
    private static final String PACKAGE_MARKER = "\"PackageFullName\":\"5319275A.WhatsAppDesktop_";

    /**
     * Holds the build number used when the Microsoft Store catalog query fails for any reason.
     *
     * @implNote This implementation defaults to {@code 261301} (corresponding to {@code 2.2613.1.0}), a recent shell build
     *           the server still accepts; refresh it if WhatsApp deprecates that build.
     */
    private static final int DEFAULT_WINDOWS_BUILD = 261301;

    /**
     * Holds the resolved {@link ClientAppVersion} this instance advertises.
     */
    private final ClientAppVersion version;

    /**
     * Constructs an immutable instance carrying the given resolved version.
     *
     * @param version the version combining the web base with the Windows store build number in the {@code quaternary} slot
     */
    private WhatsAppWindowsClientInfo(ClientAppVersion version) {
        this.version = version;
    }

    /**
     * Returns the cached Windows client identity, performing the catalog query on the first call.
     *
     * <p>Subsequent calls in the same JVM return the same instance. A failed query is not cached, so a later call retries
     * the query.
     *
     * @implNote This implementation uses double checked locking; the {@code volatile} {@link #windowsInfo} field publishes
     *           the fully constructed instance to readers on the unsynchronised fast path.
     * @return the resolved Windows client identity
     */
    public static WhatsAppWindowsClientInfo of() {
        if (windowsInfo == null) {
            synchronized (windowsInfoLock) {
                if (windowsInfo == null) {
                    windowsInfo = queryWindowsInfo();
                }
            }
        }
        return windowsInfo;
    }

    /**
     * Builds the resolved Windows client identity from the cached web version and the current Microsoft Store build number.
     *
     * @implNote This implementation copies the {@code primary}, {@code secondary} and {@code tertiary} components from
     *           {@link WhatsAppWebClientInfo#of()} (the bundle the shell hosts) and assigns the catalog derived
     *           {@link #queryWindowsBuild()} value to {@code quaternary}.
     * @return a fresh info instance with a version of the form {@code 2.3000.X.WINDOWS_BUILD}
     */
    private static WhatsAppWindowsClientInfo queryWindowsInfo() {
        var webVersion = WhatsAppWebClientInfo.of().version();
        var primary = webVersion.primary();
        var secondary = webVersion.secondary();
        var tertiary = webVersion.tertiary();
        var version = new ClientAppVersionBuilder()
                .primary(primary.isPresent() ? primary.getAsInt() : null)
                .secondary(secondary.isPresent() ? secondary.getAsInt() : null)
                .tertiary(tertiary.isPresent() ? tertiary.getAsInt() : null)
                .quaternary(queryWindowsBuild())
                .build();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved windows client info, version {0}", version);
        return new WhatsAppWindowsClientInfo(version);
    }

    /**
     * Queries the Microsoft Store display catalog for the {@code 5319275A.WhatsAppDesktop} package version and converts it
     * into the six digit build integer the hybrid shell injects.
     *
     * @implNote This implementation folds the catalog version {@code 2.SECONDARY.TERTIARY.0} into
     *           {@code SECONDARY * 100 + (TERTIARY % 100)} because the shell's tertiary component encodes a channel and
     *           build pair (the leading digit is the release channel, {@code 1} for the release channel; the trailing two
     *           digits are the build counter). For {@code 2.2613.101.0} this yields {@code 2613 * 100 + 1 = 261301},
     *           matching the value observed on the wire. Any failure (network error, missing marker, malformed body, out of
     *           range result) returns {@link #DEFAULT_WINDOWS_BUILD}.
     * @return the resolved six digit build number, or {@link #DEFAULT_WINDOWS_BUILD} when discovery fails
     */
    @WhatsAppWebExport(moduleName = "WAWebBuildConstants", exports = "WINDOWS_BUILD", platform = WhatsAppWebPlatform.WINDOWS, adaptation = WhatsAppAdaptation.ADAPTED)
    private static int queryWindowsBuild() {
        try (var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()) {
            var request = HttpRequest.newBuilder()
                    .uri(WINDOWS_STORE_URL)
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "microsoft store catalog query returned status {0}, falling back to default build", response.statusCode());
                }
                return DEFAULT_WINDOWS_BUILD;
            }
            var body = response.body();
            var markerStart = body.indexOf(PACKAGE_MARKER);
            if (markerStart < 0) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "package marker missing from microsoft store catalog response, falling back to default build");
                }
                return DEFAULT_WINDOWS_BUILD;
            }
            var versionStart = markerStart + PACKAGE_MARKER.length();
            var versionEnd = body.indexOf('_', versionStart);
            if (versionEnd < 0) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "malformed package version in microsoft store catalog response, falling back to default build");
                }
                return DEFAULT_WINDOWS_BUILD;
            }
            var versionStr = body.substring(versionStart, versionEnd);
            var parts = versionStr.split("\\.");
            if (parts.length < 3) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "malformed package version parts in microsoft store catalog response, falling back to default build");
                }
                return DEFAULT_WINDOWS_BUILD;
            }
            var secondary = Integer.parseInt(parts[1]);
            var tertiary = Integer.parseInt(parts[2]);
            var build = secondary * 100 + (tertiary % 100);
            if (build < 100000 || build > 999999) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "windows build {0} out of expected range, falling back to default build", build);
                }
                return DEFAULT_WINDOWS_BUILD;
            }
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "resolved windows store build {0}", build);
            return build;
        } catch (IOException | InterruptedException | NumberFormatException exception) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "failed to query microsoft store catalog, falling back to default build", exception);
            }
            return DEFAULT_WINDOWS_BUILD;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation always populates the {@code quaternary} slot, even on the fallback path where the
     *           catalog query failed, because servers reject Windows handshakes whose advertised build is {@code null}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebBuildConstants", exports = "VERSION_BASE_WITH_WINDOWS_BUILD", platform = WhatsAppWebPlatform.WINDOWS, adaptation = WhatsAppAdaptation.ADAPTED)
    public ClientAppVersion version() {
        return version;
    }
}
