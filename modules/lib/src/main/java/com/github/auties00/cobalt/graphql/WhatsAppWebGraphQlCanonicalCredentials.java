package com.github.auties00.cobalt.graphql;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

/**
 * Carries the canonical-registration credentials recovered from a {@code <pair-success/>} stanza and
 * exchanged at WhatsApp Web's {@code /auth/token/} endpoint for the HttpOnly session cookie.
 *
 * <p>The fields mirror the {@code s} object WhatsApp Web assembles in
 * {@code WAWebHandleCanonicalRegistration.handleCanonicalRegistration} before it calls
 * {@code WAWebCanonicalTokenExchange.storeCanonicalCredentials} or {@code exchangeNonceForToken}:
 * {@code {userId: fbid, deviceId, nonce, accessToken}}. The {@link #accessToken()} is present only on
 * the {@code storeCanonicalCredentials} branch (the server minted a token at pairing); when it is
 * {@code null} or empty the {@link #nonce()} is traded for the cookie instead. The {@link #deviceId()}
 * is not part of the decrypted payload; it is derived locally from the paired device JID and filled in
 * by {@link #withDeviceId(long)} once the JID is known.
 *
 * @param accessToken the canonical access token recovered from the decrypted payload, or {@code null}
 *                    when the server deferred minting and only a {@link #nonce()} is available
 * @param fbid        the Facebook account id ({@code user_id}) the credentials authenticate
 * @param nonce       the canonical nonce to trade for a token, or {@code null} when an
 *                    {@link #accessToken()} was supplied directly
 * @param deviceId    the local numeric device id extracted from the paired device JID, or {@code 0}
 *                    until {@link #withDeviceId(long)} fills it in
 */
@WhatsAppWebModule(moduleName = "WAWebHandleCanonicalRegistration")
public record WhatsAppWebGraphQlCanonicalCredentials(String accessToken, long fbid, String nonce, long deviceId) {
    /**
     * Returns a copy of these credentials with the device id replaced.
     *
     * <p>The decrypted canonical payload carries {@code access_token}, {@code fbid}, and {@code nonce}
     * but never the device id; WhatsApp Web computes {@code deviceId = WAJids.extractDeviceId(jid)}
     * from the paired device JID and stitches it into the credentials object before the exchange. This
     * helper performs that final stitch without disturbing the other fields.
     *
     * @param deviceId the local numeric device id extracted from the paired device JID
     * @return a new {@link WhatsAppWebGraphQlCanonicalCredentials} identical to this one but carrying {@code deviceId}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleCanonicalRegistration",
            exports = "handleCanonicalRegistration",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public WhatsAppWebGraphQlCanonicalCredentials withDeviceId(long deviceId) {
        return new WhatsAppWebGraphQlCanonicalCredentials(accessToken, fbid, nonce, deviceId);
    }
}
