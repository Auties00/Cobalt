package com.github.auties00.cobalt.registration;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDeviceAttestor;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevicePushClient;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Mobile registration driver impersonating the native Android
 * WhatsApp application.
 *
 * <p>Adds Android-specific behaviour on top of
 * {@link MobileClientRegistration}:
 * <ul>
 *   <li>{@link #createRequest(String, String, String)} attaches the
 *       Android User-Agent plus the {@code WaMsysRequest} and
 *       {@code request_token} headers the native client emits.</li>
 *   <li>{@link #getRequestVerificationCodeParameters(String)}
 *       populates the long list of Android-only form fields the
 *       {@code /code} endpoint expects (SIM MCC/MNC, advertising id,
 *       backup token, cellular strength, client metrics, etc.).</li>
 *   <li>{@link #attestBody(byte[])} HMAC-signs the body via the
 *       configured {@link LinkedWhatsAppClientDeviceAttestor.Android} and
 *       packages the result into the {@code H=} suffix plus the
 *       {@code Authorization} certificate-chain header.</li>
 *   <li>{@link #attestationFields()} ships the Play Integrity
 *       sextuple and the FCM {@code push_token} on every attested
 *       endpoint.</li>
 *   <li>{@link #generateFdid()} formats the device family UUID in
 *       lowercase, matching the native client.</li>
 * </ul>
 *
 * <p>Package-private because instances are obtained through
 * {@link MobileClientRegistration#newRegistration(LinkedWhatsAppStore,
 * LinkedWhatsAppClientVerificationHandler.Mobile, LinkedWhatsAppClientDeviceAttestor,
 * LinkedWhatsAppClientDevicePushClient)} rather than constructed directly.
 *
 * @implNote
 * This implementation reproduces the exact wire shape captured from a
 * live native Android client via Frida-instrumented
 * {@code mbedtls_gcm_crypt_and_tag} (used inside
 * {@code libwhatsapp.so}) on a
 * {@code method=voice} run. Every form field name and default value
 * here matches what the native client emits for a fresh device, so
 * the registration server cannot fingerprint Cobalt by absent or
 * surplus fields.
 *
 * @see MobileClientRegistration
 */
final class AndroidClientRegistration extends MobileClientRegistration {
    /**
     * The logger for {@link AndroidClientRegistration}.
     */
    private static final System.Logger LOGGER = Log.get(AndroidClientRegistration.class);

    /**
     * The Android device attestor consulted before each outgoing
     * request.
     *
     * <p>Never {@code null}: the constructor substitutes
     * {@link LinkedWhatsAppClientDeviceAttestor.Android#NONE} when the caller
     * supplies {@code null}. The {@code NONE} fallback produces empty
     * Play Integrity and signature output, which the server tolerates
     * as a low-trust signal.
     */
    private final LinkedWhatsAppClientDeviceAttestor.Android attestor;

    /**
     * The push client consulted for the FCM {@code push_token} and
     * silent-push {@code push_code} form fields.
     *
     * <p>Never {@code null}: the constructor substitutes
     * {@link LinkedWhatsAppClientDevicePushClient#noop()} when the caller supplies
     * {@code null}. The {@code noop} fallback returns empty strings,
     * which the server tolerates.
     */
    private final LinkedWhatsAppClientDevicePushClient pushClient;

    /**
     * Constructs an Android registration bound to the given
     * collaborators.
     *
     * <p>Invoked only from
     * {@link MobileClientRegistration#newRegistration(LinkedWhatsAppStore,
     * LinkedWhatsAppClientVerificationHandler.Mobile,
     * LinkedWhatsAppClientDeviceAttestor, LinkedWhatsAppClientDevicePushClient)}. A
     * {@code null} attestor or push client is replaced by the
     * respective {@code NONE} / {@code noop} fallback.
     *
     * @param store        the store carrying identity keys and the
     *                     phone number
     * @param verification the verification handler supplying the
     *                     method and the user-entered code
     * @param attestor     the Android device attestor, or {@code null}
     *                     to fall back to
     *                     {@link LinkedWhatsAppClientDeviceAttestor.Android#NONE}
     * @param pushClient   the push client, or {@code null} to fall back
     *                     to {@link LinkedWhatsAppClientDevicePushClient#noop()}
     */
    AndroidClientRegistration(
            LinkedWhatsAppStore store,
            LinkedWhatsAppClientVerificationHandler.Mobile verification,
            LinkedWhatsAppClientDeviceAttestor.Android attestor,
            LinkedWhatsAppClientDevicePushClient pushClient) {
        super(store, verification);
        this.attestor = Objects.requireNonNullElse(attestor, LinkedWhatsAppClientDeviceAttestor.Android.NONE);
        this.pushClient = Objects.requireNonNullElse(pushClient, LinkedWhatsAppClientDevicePushClient.noop());
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation sets the Android User-Agent derived from
     * the configured device description, fixes
     * {@code Content-Type} to
     * {@code application/x-www-form-urlencoded}, fixes
     * {@code Accept} to {@code text/json}, advertises
     * {@code WaMsysRequest: 1} (the marker the native Android
     * registration stack adds), and attaches a fresh random
     * {@code request_token} UUID on every call. Each of these
     * headers was observed verbatim on a live native Android
     * registration capture.
     */
    @Override
    protected HttpRequest createRequest(String path, String body, String authorizationHeader) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "android registration request path={0} attested={1}",
                    path, authorizationHeader != null);
        }
        var builder = HttpRequest.newBuilder()
                .uri(URI.create("%s%s".formatted(MOBILE_REGISTRATION_ENDPOINT, path)))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("User-Agent", store.accountStore().device().toUserAgent(store.accountStore().clientVersion()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/json")
                .header("WaMsysRequest", "1")
                .header("request_token", UUID.randomUUID().toString());
        if (authorizationHeader != null) {
            builder.header("Authorization", authorizationHeader);
        }
        return builder.build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Android, hex-encodes the signature returned by
     * {@link LinkedWhatsAppClientDeviceAttestor.Android#sign(LinkedWhatsAppStore, byte[])}
     * (lowercase, no separator, the format the server expects in the
     * {@code &H=} suffix) and URL-safe-base64-encodes the certificate
     * chain without padding (the format the server expects in the
     * {@code Authorization} header).
     *
     * @implNote
     * This implementation returns {@link BodyAttestation#EMPTY} when
     * the signature is empty, which signals that the
     * {@link LinkedWhatsAppClientDeviceAttestor.Android#NONE} fallback is in
     * effect; the base class then skips both wire slots. A non-empty
     * signature with an empty chain yields a populated suffix and a
     * {@code null} {@code Authorization} header, matching the native
     * client's behaviour for builds that produce a Keystore signature
     * but withhold the chain.
     */
    @Override
    protected BodyAttestation attestBody(byte[] encBodyBytes) {
        var signed = attestor.sign(store, encBodyBytes);
        var signature = signed.signature();
        var chain = signed.certificateChain();
        if (signature.length == 0) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "android body attestation empty, falling back to unsigned request");
            }
            return BodyAttestation.EMPTY;
        }
        var hex = HexFormat.of().formatHex(signature);
        var auth = chain.length == 0
                ? null
                : Base64.getUrlEncoder().withoutPadding().encodeToString(chain);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "android body attestation produced signature={0} chain={1}", signature, chain);
        }
        return new BodyAttestation(hex, auth);
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Android, the returned fields include
     * {@code sim_mcc}/{@code sim_mnc}, {@code reason},
     * {@code mcc}/{@code mnc}, {@code feo2_query_status} (always
     * {@code "error_security_exception"} so the server's anti-abuse
     * pipeline interprets Cobalt as a device whose Play Integrity
     * minting failed), {@code db}, {@code sim_type}, {@code recaptcha}
     * (the always-{@code "ABPROP_DISABLED"} stage),
     * {@code network_radio_type}, {@code prefer_sms_over_flash},
     * {@code simnum}, {@code airplane_mode_type}, {@code client_metrics}
     * (a URL-encoded JSON object built by {@link #buildClientMetrics}),
     * {@code mistyped}, {@code advertising_id}, {@code hasinrc},
     * {@code roaming_type}, {@code device_ram},
     * {@code education_screen_displayed}, {@code pid},
     * {@code cellular_strength}, {@code backup_token},
     * {@code tos_version}, {@code call_log_permission},
     * {@code manage_call_permission}, {@code clicked_education_link},
     * {@code aid}, and {@code push_code}.
     *
     * @implNote
     * This implementation does not include the Play Integrity sextuple
     * here because it ships on every attested endpoint via
     * {@link #attestationFields()}.
     */
    @Override
    protected String[] getRequestVerificationCodeParameters(String method) {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "android verification code request method={0}", method);
        }
        return new String[]{
                "method", method,
                "sim_mcc", "000",
                "sim_mnc", "000",
                "reason", "",
                "mcc", "000",
                "mnc", "000",
                "feo2_query_status", "error_security_exception",
                "db", "1",
                "sim_type", "1",
                "recaptcha", "%7B%22stage%22%3A%22ABPROP_DISABLED%22%7D",
                "network_radio_type", "1",
                "prefer_sms_over_flash", "true",
                "simnum", "0",
                "airplane_mode_type", "0",
                "client_metrics", buildClientMetrics(),
                "mistyped", "7",
                "advertising_id", store.signalStore().advertisingId().toString(),
                "hasinrc", "1",
                "roaming_type", "0",
                "device_ram", "3.57",
                "education_screen_displayed", "true",
                "pid", String.valueOf(ProcessHandle.current().pid()),
                "cellular_strength", "5",
                "backup_token", toUrlHex(store.signalStore().backupToken()),
                "tos_version", "5",
                "call_log_permission", "false",
                "manage_call_permission", "false",
                "clicked_education_link", "false",
                "aid", "",
                "push_code", pushClient.getPushCode()
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Android, returns the Play Integrity sextuple
     * ({@code gpia}, {@code _gg}, {@code _gi}, {@code _gp},
     * {@code _ge}, {@code _ga}) produced by the configured
     * {@link LinkedWhatsAppClientDeviceAttestor.Android} plus the FCM
     * {@code push_token} produced by the configured push client.
     *
     * @implNote
     * This implementation pairs the attestation and the push token
     * together because they share the same per-endpoint lifecycle:
     * both ship on {@code /v2/exist} (matching the native-client
     * capture for that path) and on every other attested endpoint, and
     * both are suppressed on the funnel endpoints by the base class.
     * The {@link LinkedWhatsAppClientDeviceAttestor.Android#NONE} attestor returns
     * six empty strings and the {@link LinkedWhatsAppClientDevicePushClient#noop()}
     * push client returns an empty {@code push_token}, all of which the
     * registration server accepts as low-trust signals.
     */
    @Override
    protected String[] attestationFields() {
        var attestation = attestor.attest(store);
        var pushToken = pushClient.getPushToken();
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "android attestation fields gpia_present={0} push_token_present={1}",
                    !attestation.gpia().isEmpty(), !pushToken.isEmpty());
        }
        return new String[]{
                "gpia", attestation.gpia(),
                "_gg", attestation.gg(),
                "_gi", attestation.gi(),
                "_gp", attestation.gp(),
                "_ge", attestation.ge(),
                "_ga", attestation.ga(),
                "push_token", pushToken
        };
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Android, the UUID is formatted in lowercase with hyphens,
     * matching the native client's {@code fdid} scheme.
     */
    @Override
    protected String generateFdid() {
        return store.signalStore().fdid().toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Builds the percent-encoded {@code client_metrics} JSON payload
     * tracking the current retry attempt.
     *
     * <p>The field is consulted by WhatsApp's anti-abuse pipeline to
     * detect uninstall-reinstall loops and persistent
     * registration-spam patterns.
     *
     * @implNote
     * This implementation reproduces the shape captured from a live
     * native Android client via Frida-instrumented
     * {@code mbedtls_gcm_crypt_and_tag} on a {@code method=voice} run:
     * {@code attempts} integer, {@code app_campaign_download_source}
     * string, {@code is_sim_absent} boolean. Field order matches the
     * native client's emission order, which the server's strict JSON
     * parser is known to enforce.
     *
     * @return the percent-encoded JSON string
     */
    private String buildClientMetrics() {
        var json = "{\"attempts\":" + attempt
                + ",\"app_campaign_download_source\":\"" + attestor.downloadSource().wireValue() + "\""
                + ",\"is_sim_absent\":false}";
        if (Log.TRACE) LOGGER.log(Level.TRACE, "android client metrics attempt={0}", attempt);
        return URLEncoder.encode(json, StandardCharsets.UTF_8);
    }
}
