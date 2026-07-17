package com.github.auties00.cobalt.client.linked.info;

import com.github.auties00.cobalt.wire.linked.device.pairing.ClientAppVersion;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;

/**
 * Sealed root of the client identity that Cobalt advertises to WhatsApp servers during the Noise handshake and registration.
 *
 * <p>The selected variant determines which {@link ClientAppVersion} is placed into the handshake client payload, which
 * browser or device platform string the server observes, and (on the mobile variants) which registration token algorithm
 * the mobile registration endpoints expect. Each variant resolves the latest published version at runtime rather than
 * embedding a fixed one, and caches the result for the lifetime of the JVM. The web variant scrapes
 * {@code web.whatsapp.com}, the Windows variant additionally folds in the Microsoft Store package build, and the mobile
 * variants either download the current Play Store APK or query the Apple App Store lookup API.
 *
 * @apiNote Embedders do not construct these directly; they obtain one through {@link #of(ClientPlatformType)} or through a
 *          variant specific factory and pass the result on when building a client. The variant must match the platform the
 *          session impersonates: servers reject sessions whose advertised build is too old or whose platform fingerprint
 *          does not match the credentials.
 * @implNote This implementation has no single WA Web counterpart because WA Web embeds its constants at compile time inside
 *           {@code WAWebBuildConstants}. Cobalt is not shipped as part of a WhatsApp release and must therefore discover
 *           those values at runtime from public distribution channels, which is why this hierarchy lives outside the WA
 *           module mapping.
 * @see ClientAppVersion
 * @see ClientPlatformType
 */
public sealed interface LinkedWhatsAppClientInfo
        permits WhatsAppWebClientInfo, WhatsAppWindowsClientInfo, WhatsAppMobileClientInfo {
    /**
     * Returns the {@link LinkedWhatsAppClientInfo} variant matching the requested {@link ClientPlatformType}.
     *
     * <p>{@link ClientPlatformType#WEB} and {@link ClientPlatformType#MACOS} both resolve to {@link WhatsAppWebClientInfo}
     * because the macOS desktop client is a Mac Catalyst port that loads the same bundle as {@code web.whatsapp.com}.
     * {@link ClientPlatformType#WINDOWS} resolves to {@link WhatsAppWindowsClientInfo}. The four mobile platforms resolve
     * to a consumer or business {@link WhatsAppAndroidClientInfo} or {@link WhatsAppIosClientInfo}. The chosen variant is
     * resolved lazily on the first call and the cached instance is returned thereafter.
     *
     * @apiNote Use this when the platform is known dynamically; for a mobile only entry point that rejects web and desktop
     *          platforms see {@link WhatsAppMobileClientInfo#of(ClientPlatformType)}.
     * @param platform the target client platform
     * @return the cached {@link LinkedWhatsAppClientInfo} for {@code platform}
     * @throws IllegalStateException if {@code platform} is not one of the platforms this dispatcher recognises
     */
    static LinkedWhatsAppClientInfo of(ClientPlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientInfo.ofPersonal();
            case IOS -> WhatsAppIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientInfo.ofBusiness();
            case WINDOWS -> WhatsAppWindowsClientInfo.of();
            case MACOS, WEB -> WhatsAppWebClientInfo.of();
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
    }

    /**
     * Returns the application version this variant advertises to WhatsApp servers.
     *
     * <p>The returned value is folded into the handshake client payload; the mobile variants additionally feed it into the
     * build hash that {@link WhatsAppMobileClientInfo#computeRegistrationToken(long)} consumes.
     *
     * @apiNote The version is resolved from a public distribution channel, so a fresh process may observe a newer build
     *          than a long lived one started earlier.
     * @return the advertised {@link ClientAppVersion}
     */
    ClientAppVersion version();
}
