package com.github.auties00.cobalt.client.linked.info;

import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;

/**
 * Sealed sub root of {@link LinkedWhatsAppClientInfo} for the native mobile clients (Android consumer, Android business, iOS
 * consumer, iOS business) that participate in the mobile registration protocol.
 *
 * <p>Every request in the mobile registration flow carries a {@link #computeRegistrationToken(long) registration token}
 * derived from the app's signing material and the national phone number. Web and desktop clients pair by QR code or device
 * link and never exercise this protocol, so they are not part of this sub hierarchy.
 *
 * @apiNote Pick a variant through this interface, or directly through {@link WhatsAppAndroidClientInfo} or
 *          {@link WhatsAppIosClientInfo}, when driving the mobile registration flow.
 * @implNote This implementation has no WA Web counterpart because the mobile registration protocol lives inside the native
 *           Android and iOS WhatsApp binaries rather than the JS bundle. Each token algorithm is reverse engineered from
 *           the respective binary.
 * @see WhatsAppAndroidClientInfo
 * @see WhatsAppIosClientInfo
 */
public sealed interface WhatsAppMobileClientInfo
        extends LinkedWhatsAppClientInfo
        permits WhatsAppAndroidClientInfo, WhatsAppIosClientInfo {
    /**
     * Returns the {@link WhatsAppMobileClientInfo} variant matching the requested mobile {@link ClientPlatformType}.
     *
     * <p>Only the four mobile platforms are accepted; web and desktop platforms are rejected because they do not
     * participate in the native registration protocol. The chosen variant is resolved lazily on the first call and the
     * cached instance is returned thereafter.
     *
     * @apiNote For a dispatcher that accepts every platform see {@link LinkedWhatsAppClientInfo#of(ClientPlatformType)}.
     * @param platform the target mobile platform
     * @return the cached {@link WhatsAppMobileClientInfo} for {@code platform}
     * @throws IllegalStateException if {@code platform} is not one of the four supported mobile platforms
     */
    static WhatsAppMobileClientInfo of(ClientPlatformType platform) {
        return switch (platform) {
            case ANDROID -> WhatsAppAndroidClientInfo.ofPersonal();
            case IOS -> WhatsAppIosClientInfo.ofPersonal();
            case ANDROID_BUSINESS -> WhatsAppAndroidClientInfo.ofBusiness();
            case IOS_BUSINESS -> WhatsAppIosClientInfo.ofBusiness();
            default -> throw new IllegalStateException("Unexpected value: " + platform);
        };
    }

    /**
     * Returns whether this variant represents the WhatsApp Business build rather than the consumer WhatsApp build.
     *
     * <p>Business and consumer builds use different package or bundle identifiers, different signing material, and (on iOS)
     * different static secrets in the registration token derivation.
     *
     * @apiNote Higher level pairing and registration layers branch on this flag for business specific fields such as the
     *          verified name certificate.
     * @return {@code true} for a business variant, {@code false} for the consumer variant
     */
    boolean business();

    /**
     * Computes the URL encoded, per phone number registration token expected by the mobile registration endpoints.
     *
     * <p>The token authenticates the caller as a real signed app instance to the server. The returned string is already
     * URL encoded, so it can be inlined into a form encoded request body without further escaping.
     *
     * @apiNote Pass the returned value as the registration token field of the registration request.
     * @implSpec Android implementations HMAC-SHA1 the concatenation of the APK signing certificates, the MD5 of
     *           {@code classes.dex}, and the decimal national phone number, keyed by a PBKDF2-HMAC-SHA1 derived key seeded
     *           from the package name plus the {@code about_logo.png} asset. iOS implementations MD5 the concatenation of a
     *           variant specific static secret, the hex encoded build hash, and the decimal national phone number.
     *           Implementations must URL encode the digest before returning it.
     * @param nationalPhoneNumber the phone number in its national form, without the country code
     * @return the URL encoded registration token
     */
    String computeRegistrationToken(long nationalPhoneNumber);
}
