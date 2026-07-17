package com.github.auties00.cobalt.registration;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientVerificationHandler;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDevicePushClient;
import com.github.auties00.cobalt.client.linked.info.WhatsAppMobileClientInfo;
import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientDeviceAttestor;
import com.github.auties00.cobalt.exception.linked.mobile.WhatsAppRegistrationException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.telemetry.log.LogRedactable;
import com.github.auties00.cobalt.wire.linked.business.*;
import com.github.auties00.cobalt.wire.linked.device.pairing.ClientPlatformType;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppAccountStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppSignalStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.github.auties00.curve25519.Curve25519;
import com.github.auties00.libsignal.key.SignalIdentityKeyPair;
import com.github.auties00.libsignal.key.SignalIdentityPublicKey;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Drives the native-mobile phone-number registration flow against
 * WhatsApp's {@code v.whatsapp.net/v2} endpoint.
 *
 * <p>Claiming a phone number as a native Android or iOS client runs
 * three sequential calls against the legacy mobile registration API:
 * <ol>
 *   <li>{@code /exist} asks the server whether an account already
 *       exists for the exact Signal identity and Noise keys this
 *       instance is about to claim. A {@code "reason": "incorrect"}
 *       response confirms the keys are free and the flow proceeds;
 *       any other response aborts.</li>
 *   <li>{@code /code} asks the server to deliver a verification code
 *       through the channel the user picked (SMS, voice call, or
 *       WhatsApp OTP).</li>
 *   <li>{@code /register} submits the code the user typed back into
 *       the verification handler. On success the store is marked as
 *       registered and its JID is set to the phone-number JID.</li>
 * </ol>
 * Two CAPTCHA / 2FA branches ({@code /challenge}, {@code /security})
 * are taken on demand when the server responds with the matching
 * fields. All request bodies are end-to-end encrypted with AES-GCM
 * under a Curve25519 shared key derived between a freshly minted
 * ephemeral keypair and a hardcoded server registration key, then
 * Base64-URL encoded and wrapped in an {@code ENC=...} form field.
 *
 * <p>An instance is obtained through {@link #newRegistration(LinkedWhatsAppStore,
 * LinkedWhatsAppClientVerificationHandler.Mobile, LinkedWhatsAppClientDeviceAttestor,
 * LinkedWhatsAppClientDevicePushClient)}, which selects
 * {@link AndroidClientRegistration} or {@link IosClientRegistration}
 * based on the configured platform; {@link #register} then runs the
 * whole ceremony once. The class lets Cobalt take the role of a native
 * Android or iOS client, which has no WhatsApp Web counterpart because
 * Web clients always pair against an existing phone.
 *
 * @implNote
 * This implementation tracks the native client's full funnel-event
 * tape (the {@code /v2/pre_pn_client_log} and {@code /v2/client_log}
 * endpoints WhatsApp uses to capture the screen-by-screen registration
 * journey) so the server's anti-abuse heuristics see a connected
 * sequence. Funnel sends are best-effort and intentionally swallow
 * every throwable so a logging failure can never abort a registration.
 *
 * @see AndroidClientRegistration
 * @see IosClientRegistration
 */
public abstract sealed class MobileClientRegistration implements AutoCloseable
        permits AndroidClientRegistration, IosClientRegistration {
    /**
     * The logger for {@link MobileClientRegistration}.
     */
    private static final System.Logger LOGGER = Log.get(MobileClientRegistration.class);

    /**
     * Base URL shared by every endpoint used during registration
     * ({@code /exist}, {@code /code}, {@code /register},
     * {@code /challenge}, {@code /security}, {@code /client_log},
     * {@code /pre_pn_client_log}).
     *
     * <p>The value is not overridable because the cryptographic
     * envelope is bound to WhatsApp's hardcoded server key.
     */
    public static final String MOBILE_REGISTRATION_ENDPOINT = "https://v.whatsapp.net/v2";

    /**
     * Curve25519 public key the WhatsApp registration server
     * advertises, used as the peer for the per-request ECDH that
     * derives the AES-GCM body-encryption key.
     *
     * <p>The value is hex-decoded once at class load and reused for
     * every outbound request. Every request's AES-GCM authentication
     * tag is verified against it server-side, so a wrong key would be
     * rejected.
     */
    private static final byte[] REGISTRATION_PUBLIC_KEY = HexFormat.of().parseHex("8e8c0f74c3ebc5d7a6865c6c3c843856b06121cce8ea774d22fb6f122512302d");

    /**
     * URL-safe base64 encoding of the single byte that identifies
     * Signal identity keys, sent verbatim as the {@code e_keytype}
     * form field.
     */
    private static final String SIGNAL_PUBLIC_KEY_TYPE = Base64.getUrlEncoder().encodeToString(new byte[]{SignalIdentityPublicKey.type()});

    /**
     * HTTP client backing every registration request.
     *
     * <p>Created once at construction with redirect following enabled
     * and closed by {@link #close}. Subclasses build {@link HttpRequest}
     * instances via {@link #createRequest(String, String, String)}
     * rather than reaching into this client directly.
     */
    protected final HttpClient httpClient;

    /**
     * Store carrying the Signal identity keys, Noise keys,
     * registration id, FDID, device id, phone number, and other
     * persistent credentials referenced on every request.
     *
     * <p>Mutated in place when registration succeeds: the
     * {@code registered} flag and the local JID are written via
     * {@link LinkedWhatsAppAccountStore#setRegistered} and
     * {@link LinkedWhatsAppAccountStore#setJid}.
     */
    protected final LinkedWhatsAppStore store;

    /**
     * Callback the registration consults to pick the verification
     * method, retrieve the user-entered code, solve a CAPTCHA, and
     * supply the 2FA PIN when required.
     *
     * <p>Supplied at construction time and never {@code null}.
     */
    protected final LinkedWhatsAppClientVerificationHandler.Mobile verification;

    /**
     * One-based count of {@code /v2/code} attempts driven by the
     * retry loop inside {@link #requestVerificationCode(String)}.
     *
     * <p>Read by Android's {@code client_metrics.attempts} field so
     * each retry advertises its position in the loop, and reset to
     * {@code 1} every time {@link #requestVerificationCode(String)}
     * starts.
     */
    protected int attempt = 1;

    /**
     * Screen-name string reported on the most recent funnel event.
     *
     * <p>Echoed into the {@code previous_screen} field of subsequent
     * funnel events so the server's anti-abuse pipeline sees a
     * connected walk through the registration UI screens.
     *
     * @implNote
     * This implementation initialises the value to
     * {@code "enter_number"} because the very first funnel event
     * Cobalt emits is the {@code exist_check} attempt, which happens
     * after the phone number has already been committed to the store.
     * The native client's pre-phone-number screens (EULA, idle entry
     * screen) have no Cobalt analogue.
     */
    private String previousFunnelScreen = "enter_number";

    /**
     * Last verification method string passed to
     * {@link #requestVerificationCode(String)}.
     *
     * <p>Drives the {@code current_screen} value reported by
     * {@link #sendVerificationCode} and the 2FA handler so funnel
     * events fired after the code exchange carry the same
     * {@code verify_<method>} screen name the native client would have
     * reported. Remains {@code null} until the first call to
     * {@link #requestVerificationCode(String)}.
     */
    private String lastRequestedMethod;

    /**
     * Stable per-registration session identifier sent as the
     * {@code access_session_id} form field on every attested endpoint
     * and every funnel event.
     *
     * <p>Generated once at construction time and reused for the entire
     * registration ceremony, so the server can stitch
     * {@code /v2/exist}, {@code /v2/code}, {@code /v2/register},
     * {@code /v2/client_log}, and every other call together as one
     * session.
     *
     * @implNote
     * This implementation mirrors the 22-character URL-safe base64
     * encoding of a random 16-byte UUID that the native Android client
     * emits.
     */
    private final String accessSessionId = newAccessSessionId();

    /**
     * Mints a fresh {@code access_session_id} matching the
     * 22-character URL-safe base64 shape the native client uses.
     *
     * <p>Backs the field initialiser of {@link #accessSessionId}.
     *
     * @return the generated session identifier
     */
    private static String newAccessSessionId() {
        var u = UUID.randomUUID();
        var bb = ByteBuffer.allocate(16);
        bb.putLong(u.getMostSignificantBits());
        bb.putLong(u.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array());
    }

    /**
     * Constructs a registration for the given store and verification
     * handler.
     *
     * <p>Invoked only from the concrete subclasses, which are
     * themselves created by {@link #newRegistration(LinkedWhatsAppStore,
     * LinkedWhatsAppClientVerificationHandler.Mobile,
     * LinkedWhatsAppClientDeviceAttestor, LinkedWhatsAppClientDevicePushClient)}. Both
     * arguments are validated as non-{@code null} and the shared
     * {@link #httpClient} is built with redirect following enabled.
     *
     * @param store        the store carrying identity keys and the
     *                     phone number; never {@code null}
     * @param verification the verification handler supplying the
     *                     method and the user-entered code; never
     *                     {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    protected MobileClientRegistration(LinkedWhatsAppStore store, LinkedWhatsAppClientVerificationHandler.Mobile verification) {
        Objects.requireNonNull(store, "store cannot be null");
        Objects.requireNonNull(verification, "verification cannot be null");
        this.store = store;
        this.verification = verification;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    /**
     * Returns the concrete registration implementation matching the
     * platform configured on the given store.
     *
     * <p>Dispatches to either {@link AndroidClientRegistration} or
     * {@link IosClientRegistration} based on
     * {@link LinkedWhatsAppAccountStore#device}'s
     * {@link ClientPlatformType platform}. A {@code null} attestor
     * falls back to the platform's
     * {@link LinkedWhatsAppClientDeviceAttestor.Android#NONE} or
     * {@link LinkedWhatsAppClientDeviceAttestor.Ios#NONE} low-trust default, and a
     * {@code null} push client falls back to
     * {@link LinkedWhatsAppClientDevicePushClient#noop()}. A non-{@code null}
     * attestor whose platform does not match the device platform is
     * rejected.
     *
     * @implNote
     * This implementation re-validates that the attestor's platform
     * matches the device's platform so that a misconfigured driver
     * fails with an {@link IllegalArgumentException} rather than a
     * confusing runtime cast error.
     *
     * @param store        the store whose device drives the
     *                     selection
     * @param verification the verification handler passed to the new
     *                     registration
     * @param attestor     the device attestor, or {@code null} to
     *                     fall back to the platform's
     *                     {@code NONE} attestor
     * @param pushClient   the device push client, or {@code null} to
     *                     fall back to
     *                     {@link LinkedWhatsAppClientDevicePushClient#noop()}
     * @return a concrete {@code MobileClientRegistration}
     * @throws IllegalArgumentException if the store's device platform
     *                                  is not a supported mobile
     *                                  platform, or if
     *                                  {@code attestor} does not
     *                                  match that platform
     */
    public static MobileClientRegistration newRegistration(
            LinkedWhatsAppStore store,
            LinkedWhatsAppClientVerificationHandler.Mobile verification,
            LinkedWhatsAppClientDeviceAttestor attestor,
            LinkedWhatsAppClientDevicePushClient pushClient) {
        var platform = store.accountStore().device().platform();
        return switch (platform) {
            case ANDROID, ANDROID_BUSINESS -> {
                var androidAttestor = switch (attestor) {
                    case null -> null;
                    case LinkedWhatsAppClientDeviceAttestor.Android a -> a;
                    case LinkedWhatsAppClientDeviceAttestor.Ios ignored -> throw new IllegalArgumentException(
                            "Android device requires an Android attestor, got: " + attestor.getClass().getName());
                };
                yield new AndroidClientRegistration(store, verification, androidAttestor, pushClient);
            }
            case IOS, IOS_BUSINESS -> {
                var iosAttestor = switch (attestor) {
                    case null -> null;
                    case LinkedWhatsAppClientDeviceAttestor.Ios i -> i;
                    case LinkedWhatsAppClientDeviceAttestor.Android ignored -> throw new IllegalArgumentException(
                            "iOS device requires an iOS attestor, got: " + attestor.getClass().getName());
                };
                yield new IosClientRegistration(store, verification, iosAttestor, pushClient);
            }
            default -> throw new IllegalArgumentException("Unsupported platform: " + platform);
        };
    }

    /**
     * Returns the platform-specific form parameters that accompany a
     * {@code /code} request.
     *
     * <p>Subclasses populate the long Android field list (SIM MCC/MNC,
     * advertising id, backup token, etc.) or the short iOS field list
     * (method, SIM MCC/MNC, jailbroken flag, APNS push code, cellular
     * strength) the native clients emit on a {@code method=<channel>}
     * run.
     *
     * @implSpec
     * Overriders must return an alternating array of name/value
     * strings of even length, in the order the native client emits.
     * The Play Integrity sextuple (Android) and the APNS
     * {@code push_token} (iOS) must not be included here because they
     * ship on every attested endpoint via {@link #attestationFields()}.
     *
     * @param method the verification method chosen by the user (for
     *               example {@code "sms"}, {@code "voice"},
     *               {@code "wa_old"})
     * @return the additional alternating name/value form parameters
     */
    protected abstract String[] getRequestVerificationCodeParameters(String method);

    /**
     * Returns the device family identifier used as the {@code fdid}
     * form field.
     *
     * <p>Subclasses format the same underlying UUID differently:
     * Android emits the lowercase form, iOS emits the uppercase form,
     * matching the respective native apps.
     *
     * @implSpec
     * Overriders must read {@link LinkedWhatsAppSignalStore#fdid} and return a
     * non-{@code null} UUID string in the per-platform casing.
     *
     * @return the formatted device family identifier
     */
    protected abstract String generateFdid();

    /**
     * Returns the platform-specific device-attestation form fields
     * appended to every attested registration endpoint.
     *
     * <p>Android returns the Play Integrity sextuple ({@code gpia},
     * {@code _gg}, {@code _gi}, {@code _gp}, {@code _ge},
     * {@code _ga}) produced by the configured
     * {@link LinkedWhatsAppClientDeviceAttestor.Android} plus the FCM
     * {@code push_token} produced by the configured
     * {@link LinkedWhatsAppClientDevicePushClient}. iOS returns just the APNS
     * {@code push_token}, because its App Attest payloads ride outside
     * the encrypted body (in the {@code H=} suffix and the
     * {@code Authorization} header) rather than inside it. The funnel
     * endpoints {@code /client_log} and {@code /pre_pn_client_log}
     * build their bodies directly without going through
     * {@link #getRegistrationOptions(boolean, String...)} and therefore
     * never reach this method, matching the native client's per-endpoint
     * {@code sendAttestationPayload=false} configuration for them.
     *
     * @implSpec
     * Overriders must return an alternating array of name/value
     * strings of even length. Each invocation may trigger a fresh call
     * into the attestor and the push client, so implementations that
     * talk to a remote minter should cache the per-session payload
     * inside the attestor to avoid one round-trip per registration step.
     *
     * @return the alternating name/value attestation fields
     */
    protected abstract String[] attestationFields();

    /**
     * Builds an {@link HttpRequest} for the given sub-path of the
     * mobile registration endpoint.
     *
     * <p>Invoked by {@link #sendRequest(String, String)} after the
     * body has been assembled in its final
     * {@code ENC=<base64>[&H=<x>]} form. Subclasses set the headers the
     * native client advertises for that platform (User-Agent,
     * Content-Type, plus platform-specific extras) and attach the
     * {@code Authorization} header when one is supplied.
     *
     * @implSpec
     * Overriders must POST {@code body} verbatim, set the
     * {@code User-Agent} matching {@link LinkedWhatsAppAccountStore#device}'s
     * user-agent string, and attach {@code authorizationHeader} as the
     * {@code Authorization} header when it is non-{@code null}. Other
     * headers are platform-specific.
     *
     * @param path                the sub-path, starting with a slash
     *                            (for example {@code "/exist"})
     * @param body                the fully-assembled body to POST
     * @param authorizationHeader the value of the
     *                            {@code Authorization} header, or
     *                            {@code null} to omit it
     * @return a ready-to-send HTTP request
     */
    protected abstract HttpRequest createRequest(String path, String body, String authorizationHeader);

    /**
     * Signs the outer {@code ENC=<base64>} body with the
     * platform-specific attestor.
     *
     * <p>Invoked by {@link #sendRequest(String, String)} once per
     * outgoing request. Implementations backed by a real attestor
     * (Android Keystore HMAC, iOS App Attest) return a populated pair;
     * the {@code NONE} fallbacks and other low-trust attestors return
     * {@link BodyAttestation#EMPTY}, on which the base class skips both
     * the {@code &H=} suffix and the {@code Authorization} header.
     *
     * @implSpec
     * Overriders must derive the {@code H=} suffix value and the
     * {@code Authorization} header value from {@code encBodyBytes}
     * (Android) or from store-derived material (iOS). When no real
     * signature is available, return {@link BodyAttestation#EMPTY}.
     *
     * @param encBodyBytes the UTF-8 bytes of the base64 ENC body, that
     *                     is the value that follows the {@code ENC=}
     *                     prefix on the wire; never {@code null}
     * @return the suffix and header pair, or
     *         {@link BodyAttestation#EMPTY} when none was produced
     */
    protected abstract BodyAttestation attestBody(byte[] encBodyBytes);

    /**
     * Pair carrying the body suffix and the {@code Authorization}
     * header value produced by {@link #attestBody(byte[])}.
     *
     * <p>The semantics of each component are platform-specific:
     * <ul>
     *   <li>Android: {@link #bodyAttestation} is the lowercase hex of
     *       the HMAC-SHA1 over the base64 ENC body produced by the
     *       Keystore-backed key; {@link #authorizationHeader} is the
     *       base64 certificate chain.</li>
     *   <li>iOS: {@link #bodyAttestation} is the JSON envelope
     *       {@code {"assertion":"<base64 CBOR>"}} wrapping the
     *       per-request App Attest assertion;
     *       {@link #authorizationHeader} is
     *       {@code <base64 attestation>|<base64 keyId>}.</li>
     * </ul>
     *
     * @param bodyAttestation     the value appended after {@code &H=},
     *                            or {@code null} when no attestation
     *                            was produced
     * @param authorizationHeader the value of the
     *                            {@code Authorization} header, or
     *                            {@code null} when no header should be
     *                            sent
     */
    protected record BodyAttestation(String bodyAttestation, String authorizationHeader) {
        /**
         * Sentinel returned by {@link #attestBody(byte[])} when no
         * attestation output is available.
         *
         * <p>Returned for the {@code NONE} attestor fallbacks and for
         * any attestor that produces empty output (for example a
         * remote minter that is unreachable). The base class treats
         * this value as a signal to skip both the {@code &H=} suffix
         * and the {@code Authorization} header, and the registration
         * server accepts the resulting request as a low-trust
         * downgrade.
         */
        public static final BodyAttestation EMPTY = new BodyAttestation(null, null);
    }

    /**
     * Executes the three-step registration flow and persists the
     * result on success.
     *
     * <p>On a fresh phone number the flow visits {@code /exist},
     * {@code /code}, and {@code /register} in order, optionally
     * branching into {@code /challenge} (CAPTCHA) or {@code /security}
     * (2FA) if the server requests either. When the server recognises
     * the local keys the flow may short-circuit before requesting a
     * code.
     *
     * @implNote
     * This implementation wraps every {@link IOException} or
     * {@link InterruptedException} raised by the underlying HTTP client
     * in a {@link WhatsAppRegistrationException} so the caller only
     * ever has one exception type to catch.
     *
     * @throws WhatsAppRegistrationException if any server response
     *                                       indicates a non-recoverable
     *                                       failure or if the
     *                                       underlying HTTP exchange
     *                                       fails
     */
    public void register() {
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "mobile registration starting, platform={0}", store.accountStore().device().platform());
        }
        try {
            sendPrePnFunnelLog("session_start", "registration_session_start");

            assertRegistrationKeys();

            requestVerificationCodeIfNecessary();

            sendVerificationCode();
        } catch (IOException | InterruptedException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "mobile registration failed", exception);
            throw new WhatsAppRegistrationException(exception);
        }
    }

    /**
     * Probes {@code /v2/exist} to confirm that the account slot for
     * the local keys is still free.
     *
     * <p>Invoked exactly once at the top of {@link #register}. A
     * {@code "reason": "incorrect"} response means the number is free
     * from the perspective of these keys: the server is reporting that
     * whatever keys it has on file for this number differ from what
     * Cobalt offered, which is the success signal the rest of the flow
     * needs. Any other response aborts the registration.
     *
     * @implNote
     * This implementation retries once to tolerate a transient
     * non-{@code "incorrect"} response, then aborts if the second
     * attempt also fails. The retry mirrors the native client's own
     * one-shot retry inside {@code /v2/exist} processing.
     *
     * @throws IOException                   if the HTTP call fails
     * @throws InterruptedException          if the sending thread is
     *                                       interrupted
     * @throws WhatsAppRegistrationException if neither attempt returns
     *                                       {@code "incorrect"}
     */
    private void assertRegistrationKeys() throws IOException, InterruptedException {
        sendFunnelLog("enter_number", "exist_check", "exist_attempt");

        var attrs = getRegistrationOptions(false);

        var result = sendRequest("/exist", attrs);
        var response = JSON.parseObject(result);
        if (Objects.equals(response.getString("reason"), "incorrect")) {
            sendFunnelLog("enter_number", "exist_check", "exist_success");
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "exist check confirmed free account slot");
            return;
        }

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "exist check retrying, reason={0}", response.getString("reason"));
        result = sendRequest("/exist", attrs);
        response = JSON.parseObject(result);
        if (Objects.equals(response.getString("reason"), "incorrect")) {
            sendFunnelLog("enter_number", "exist_check", "exist_success");
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "exist check confirmed free account slot on retry");
            return;
        }

        sendFunnelLog("enter_number", "exist_check", "exist_failure");

        if (Log.WARNING) LOGGER.log(Level.WARNING, "exist check failed, reason={0}", response.getString("reason"));
        throw new WhatsAppRegistrationException("Cannot get account data", new String(result));
    }


    /**
     * Invokes the verification handler to find out which channel to
     * request a code through and, if any, calls {@code /code}.
     *
     * <p>A handler that returns an empty optional from
     * {@link LinkedWhatsAppClientVerificationHandler.Mobile#requestMethod}
     * is taken to mean that a code has already been requested outside
     * Cobalt (for example through another device), so this step is
     * skipped and the flow proceeds directly to
     * {@link #sendVerificationCode}.
     *
     * @throws IOException          if the HTTP call fails
     * @throws InterruptedException if the sending thread is interrupted
     */
    private void requestVerificationCodeIfNecessary() throws IOException, InterruptedException {
        var codeResult = verification.requestMethod();
        if (codeResult.isEmpty()) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "verification code request skipped, method already requested externally");
            return;
        }

        requestVerificationCode(codeResult.get());
        saveRegistrationStatus(false);
    }

    /**
     * Calls {@code /v2/code} in a retry loop until the server confirms
     * or definitively rejects the request.
     *
     * <p>Server-side rate-limit and route-block reasons are translated
     * into targeted {@link WhatsAppRegistrationException} messages so
     * the failure carries actionable detail.
     *
     * @implNote
     * This implementation aborts when the same error reason appears
     * twice in a row, captures the per-method or maximum {@code _wait}
     * hint from the response and folds it into the rate-limit message,
     * and treats {@code wa_old} specially when {@code no_routes} comes
     * back (suggesting a platform change rather than a method change).
     *
     * @param method the verification method the user asked for
     * @throws IOException                   if the HTTP call fails
     * @throws InterruptedException          if the sending thread is
     *                                       interrupted
     * @throws WhatsAppRegistrationException if the server reports a
     *                                       blocking error or the same
     *                                       error twice
     */
    private void requestVerificationCode(String method) throws IOException, InterruptedException {
        String lastError = null;
        attempt = 1;
        lastRequestedMethod = method;
        var verifyScreen = "verify_" + method;
        sendFunnelLog(verifyScreen, "request_code", "request_code_attempt");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "requesting verification code via {0}", method);
        while (true) {
            var params = getRequestVerificationCodeParameters(method);
            var attrs = getRegistrationOptions(true, params);

            var result = sendRequest("/code", attrs);
            var response = JSON.parseObject(result);
            var status = response.getString("status");
            if (isSuccessful(status)) {
                sendFunnelLog(verifyScreen, "request_code", "request_code_success");
                if (Log.INFO) LOGGER.log(Level.INFO, "verification code requested via {0}", method);
                return;
            }

            var reason = response.getString("reason");
            if(isTooRecent(reason)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "verification code request rate limited, reason={0}", reason);
                throw new WhatsAppRegistrationException(
                        "Please wait before trying to register this phone value again. Don't spam!"
                                + formatWaitSuffix(response, method),
                        new String(result));
            }

            if(isRegistrationBlocked(reason)) {
                var resultJson = new String(result);
                if (Log.WARNING) LOGGER.log(Level.WARNING, "verification code request blocked, method={0}", method);
                if(method.equals("wa_old")) {
                    throw new WhatsAppRegistrationException("The registration attempt was blocked by Whatsapp: you might want to change platform(iOS/Android) or try using a residential proxy (don't spam)", resultJson);
                }else {
                    throw new WhatsAppRegistrationException("The registration attempt was blocked by Whatsapp: please try using a Whatsapp OTP as a verification method", resultJson);
                }
            }

            if (Objects.equals(reason, lastError)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "verification code request failed twice, reason={0}", reason);
                throw new WhatsAppRegistrationException("An error occurred while registering: " + reason, new String(result));
            }

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "verification code request attempt {0} failed, reason={1}", attempt, reason);
            lastError = reason;
            attempt++;
        }
    }

    /**
     * Formats the server's rate-limit {@code _wait} hint as a trailing
     * parenthesised suffix for an error message.
     *
     * <p>Returns an empty string when no usable hint is present so the
     * caller can concatenate unconditionally.
     *
     * @implNote
     * This implementation reads {@code <method>_wait} first (the
     * server's per-channel suggestion for the channel the client asked
     * for) and falls back to {@link #maxWait(JSONObject)} so the
     * message always carries the most restrictive hint the server
     * provided.
     *
     * @param response the parsed {@code /v2/code} JSON response
     * @param method   the method string the client requested
     * @return the formatted suffix, or an empty string if no usable
     *         hint was supplied
     */
    private String formatWaitSuffix(JSONObject response, String method) {
        var preferred = response.getLong(method + "_wait");
        var wait = preferred != null ? preferred : maxWait(response);
        if (wait == null || wait <= 0) {
            return "";
        }
        return " (retry after " + wait + " seconds)";
    }

    /**
     * Returns the largest non-negative {@code *_wait} value carried by
     * the response.
     *
     * <p>The set of keys scanned ({@code sms_wait}, {@code voice_wait},
     * {@code wa_old_wait}, {@code flash_wait}, {@code email_otp_wait},
     * {@code send_sms_wait}, {@code silent_auth_wait}) matches the
     * channels the registration server is known to advertise.
     *
     * @param response the parsed JSON response
     * @return the maximum wait hint in seconds, or {@code null}
     */
    private Long maxWait(JSONObject response) {
        Long max = null;
        for (var key : new String[]{"sms_wait", "voice_wait", "wa_old_wait",
                "flash_wait", "email_otp_wait", "send_sms_wait", "silent_auth_wait"}) {
            var value = response.getLong(key);
            if (value != null && value > 0 && (max == null || value > max)) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Tests whether a server {@code reason} string means the caller is
     * being rate limited.
     *
     * <p>The four reason strings recognised here cover the per-channel
     * rate limit, the cross-channel limit, the per-IP guess limit, and
     * the all-methods limit.
     *
     * @param reason the reason string from a {@code /code} JSON
     *               response
     * @return {@code true} if the reason is a known rate-limit keyword
     */
    private boolean isTooRecent(String reason) {
        return reason.equalsIgnoreCase("too_recent")
               || reason.equalsIgnoreCase("too_many")
               || reason.equalsIgnoreCase("too_many_guesses")
               || reason.equalsIgnoreCase("too_many_all_methods");
    }

    /**
     * Tests whether a server {@code reason} string means WhatsApp
     * refused to deliver the verification code.
     *
     * <p>The {@code no_routes} response means the server cannot route
     * the code through the chosen channel and the client should try a
     * different channel (or a different platform / proxy).
     *
     * @param reason the reason string from a {@code /code} JSON
     *               response
     * @return {@code true} if the reason means routing is unavailable
     */
    private boolean isRegistrationBlocked(String reason) {
        return reason.equalsIgnoreCase("no_routes");
    }


    /**
     * Submits the user-entered verification code to {@code /v2/register}
     * and marks the store as registered on success.
     *
     * <p>This is the final step of the flow. It branches into
     * {@link #handleChallenge} when the server responds with a CAPTCHA
     * and into {@link #handle2FA} when the account is protected by a
     * 2FA PIN.
     *
     * @implNote
     * This implementation strips whitespace and dashes from the code
     * via {@link #normalizeCodeResult(String)} so common user-entered
     * formats (for example {@code "123-456"}) work without further
     * preprocessing.
     *
     * @throws IOException                   if the HTTP call fails
     * @throws InterruptedException          if the sending thread is
     *                                       interrupted
     * @throws WhatsAppRegistrationException if the server refuses the
     *                                       submitted code
     */
    public void sendVerificationCode() throws IOException, InterruptedException {
        var code = verification.verificationCode();

        var verifyScreen = currentVerifyScreen();
        sendFunnelLog(verifyScreen, "submit_code", "submit_code_attempt");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "submitting verification code {0}", new LogRedactable.Code(code));

        var attrs = getRegistrationOptions(true, "code", normalizeCodeResult(code));
        var result = sendRequest("/register", attrs);
        var response = JSON.parseObject(result);
        var status = response.getString("status");
        if (isSuccessful(status)) {
            sendFunnelLog("account_verification_complete", "submit_code", "submit_code_success");
            saveRegistrationStatus(true);
            return;
        }

        if (hasChallenge(response)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "verification code submit requires captcha challenge");
            handleChallenge(response);
            return;
        }

        if (is2FARequired(response)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "verification code submit requires two-factor pin");
            handle2FA();
            return;
        }

        if (Log.WARNING) LOGGER.log(Level.WARNING, "verification code submit rejected, status={0}", status);
        throw new WhatsAppRegistrationException("Cannot confirm registration", new String(result));
    }

    /**
     * Tests whether a server response carries a CAPTCHA challenge blob.
     *
     * <p>Returns {@code true} when either the image or the audio
     * variant is present, so the caller can route the response into
     * the challenge flow.
     *
     * @param response the parsed JSON response
     * @return {@code true} if a challenge is embedded
     */
    private boolean hasChallenge(JSONObject response) {
        var image = response.getString("image_blob");
        var audio = response.getString("audio_blob");
        return (image != null && !image.isEmpty()) || (audio != null && !audio.isEmpty());
    }

    /**
     * Tests whether a server response signals that 2FA is required to
     * finalise the registration.
     *
     * <p>The three reason strings cover the canonical
     * {@code 2fa_required}, the legacy {@code security_code} alias, and
     * the newer {@code two_factor_required} variant.
     *
     * @param response the parsed JSON response
     * @return {@code true} if the response's reason is a known
     *         2FA-required marker
     */
    private boolean is2FARequired(JSONObject response) {
        var reason = response.getString("reason");
        if (reason == null) {
            return false;
        }
        return reason.equalsIgnoreCase("2fa_required")
                || reason.equalsIgnoreCase("security_code")
                || reason.equalsIgnoreCase("two_factor_required");
    }

    /**
     * Decodes the CAPTCHA blobs, asks the verification handler to solve
     * them, and submits the answer to {@code /v2/challenge}.
     *
     * <p>A handler that returns an empty optional from
     * {@link LinkedWhatsAppClientVerificationHandler.Mobile#solveCaptcha}
     * aborts the registration with a
     * {@link WhatsAppRegistrationException}.
     *
     * @implNote
     * This implementation loops because the server may chain multiple
     * CAPTCHAs (a wrong answer is replied to with another challenge);
     * the loop exits on success, on the 2FA branch, or on a
     * non-challenge non-success response.
     *
     * @param initialResponse the response carrying the initial
     *                        challenge
     * @throws IOException                   if the HTTP call fails
     * @throws InterruptedException          if the sending thread is
     *                                       interrupted
     * @throws WhatsAppRegistrationException if the handler refuses to
     *                                       solve the challenge or the
     *                                       server refuses the answer
     */
    private void handleChallenge(JSONObject initialResponse)
            throws IOException, InterruptedException {
        var verifyScreen = currentVerifyScreen();
        sendFunnelLog(verifyScreen, "challenge_shown", "captcha_shown");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "captcha challenge shown");
        var response = initialResponse;
        for (;;) {
            var image = decodeOrNull(response.getString("image_blob"));
            var audio = decodeOrNull(response.getString("audio_blob"));
            var answer = verification.solveCaptcha(image, audio);
            if (answer.isEmpty()) {
                sendFunnelLog(verifyScreen, "challenge_abandoned", "captcha_abandoned");
                if (Log.WARNING) LOGGER.log(Level.WARNING, "captcha challenge abandoned, handler returned no answer");
                throw new WhatsAppRegistrationException(
                        "Server requires a CAPTCHA that the verification handler cannot solve",
                        response.toString());
            }

            sendFunnelLog(verifyScreen, "challenge_submitted", "captcha_submitted");
            var attrs = getRegistrationOptions(true, "code", normalizeCodeResult(answer.get()));
            var result = sendRequest("/challenge", attrs);
            response = JSON.parseObject(result);
            var status = response.getString("status");
            if (isSuccessful(status)) {
                sendFunnelLog("account_verification_complete", "challenge_submitted", "captcha_success");
                saveRegistrationStatus(true);
                return;
            }
            if (hasChallenge(response)) {
                sendFunnelLog(verifyScreen, "challenge_retry", "captcha_retry");
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "captcha challenge retry requested");
                continue;
            }
            if (is2FARequired(response)) {
                handle2FA();
                return;
            }
            if (Log.WARNING) LOGGER.log(Level.WARNING, "captcha challenge submit rejected, status={0}", status);
            throw new WhatsAppRegistrationException("Cannot complete challenge", new String(result));
        }
    }

    /**
     * Asks the verification handler for the 2FA PIN and submits it to
     * {@code /v2/security}.
     *
     * <p>A handler that returns an empty optional from
     * {@link LinkedWhatsAppClientVerificationHandler.Mobile#twoFactorPin}
     * aborts the registration.
     *
     * @throws IOException                   if the HTTP call fails
     * @throws InterruptedException          if the sending thread is
     *                                       interrupted
     * @throws WhatsAppRegistrationException if the handler refuses to
     *                                       supply a PIN or the server
     *                                       refuses the submitted PIN
     */
    private void handle2FA() throws IOException, InterruptedException {
        sendFunnelLog("verify_twofac", "twofac_shown", "twofac_prompt_shown");
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "two-factor pin prompt shown");
        var pin = verification.twoFactorPin();
        if (pin.isEmpty()) {
            sendFunnelLog("verify_twofac", "twofac_abandoned", "twofac_abandoned");
            if (Log.WARNING) LOGGER.log(Level.WARNING, "two-factor pin abandoned, handler returned no pin");
            throw new WhatsAppRegistrationException(
                    "Server requires a 2FA PIN that the verification handler cannot supply", "");
        }

        sendFunnelLog("verify_twofac", "twofac_submitted", "twofac_submitted");
        var attrs = getRegistrationOptions(true, "code", normalizeCodeResult(pin.get()));
        var result = sendRequest("/security", attrs);
        var response = JSON.parseObject(result);
        var status = response.getString("status");
        if (isSuccessful(status)) {
            sendFunnelLog("account_verification_complete", "twofac_submitted", "twofac_success");
            saveRegistrationStatus(true);
            return;
        }
        if (Log.WARNING) LOGGER.log(Level.WARNING, "two-factor pin submit rejected, status={0}", status);
        throw new WhatsAppRegistrationException("Cannot confirm 2FA", new String(result));
    }

    /**
     * Decodes a base64 string defensively, accepting either the
     * standard or URL-safe alphabet.
     *
     * <p>The server has been observed to switch between the two
     * alphabets across releases, so this method tries the standard
     * decoder first and falls back to the URL-safe decoder before
     * giving up.
     *
     * @param base64 the base64-encoded string, or {@code null}
     * @return the decoded bytes, or {@code null} if the input is
     *         absent or undecodable in both alphabets
     */
    private byte[] decodeOrNull(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException _) {
            try {
                return Base64.getUrlDecoder().decode(base64);
            } catch (IllegalArgumentException still) {
                return null;
            }
        }
    }

    /**
     * Persists the registration state and, on success, the derived
     * JID.
     *
     * <p>Writes the {@code registered} flag to the store and, when
     * {@code registered} is {@code true}, derives the local JID from
     * the stored phone number and persists it. The store is mutated in
     * place and then saved.
     *
     * @param registered whether the flow has completed successfully
     * @throws IOException                   if the store save fails
     * @throws WhatsAppRegistrationException if the phone number is
     *                                       missing from the store when
     *                                       registration succeeds
     */
    private void saveRegistrationStatus(boolean registered) throws IOException {
        store.accountStore().setRegistered(registered);
        if (registered) {
            var phoneNumber = store.accountStore().phoneNumber()
                    .orElseThrow(() -> new WhatsAppRegistrationException("Phone number wasn't set"));
            var jid = Jid.of(phoneNumber);
            store.accountStore().setJid(jid);
            if (Log.INFO) LOGGER.log(Level.INFO, "mobile registration complete, jid={0}", jid);
        }
        store.save();
    }

    /**
     * Strips dashes and whitespace from a user-entered verification
     * code.
     *
     * <p>Called from every code-submitting branch so common user
     * formats ({@code "123-456"}, {@code "123 456"}) are accepted
     * without extra preprocessing.
     *
     * @param code the raw code from the verification handler
     * @return the digits-only code
     */
    private String normalizeCodeResult(String code) {
        return code.replaceAll("-", "")
                .trim();
    }

    /**
     * Tests whether a registration-API {@code status} string indicates
     * success.
     *
     * <p>The three accepted values cover the success synonyms observed
     * on different endpoints: {@code ok} for {@code /exist},
     * {@code sent} for {@code /code}, and {@code verified} for
     * {@code /register}, {@code /challenge}, and {@code /security}.
     *
     * @param status the {@code status} field from a JSON response
     * @return {@code true} for {@code ok}, {@code sent}, or
     *         {@code verified}
     */
    private boolean isSuccessful(String status) {
        return status.equalsIgnoreCase("ok")
               || status.equalsIgnoreCase("sent")
               || status.equalsIgnoreCase("verified");
    }

    /**
     * Encrypts the given form body and sends it as an HTTP request to
     * the given sub-path.
     *
     * <p>The server decrypts the payload with its own half of the
     * ECDH, using the hardcoded {@link #REGISTRATION_PUBLIC_KEY} as the
     * other peer.
     *
     * @implNote
     * This implementation mints a fresh ephemeral Curve25519 keypair
     * for every request, runs ECDH against the server key, and
     * encrypts the body with AES-256-GCM under a zero-byte IV (matching
     * the native mobile protocol, which relies on the per-request
     * ephemeral key for freshness). The ENC payload is the
     * concatenation of the ephemeral public key and the ciphertext,
     * URL-base64 encoded. The subclass-supplied attestation suffix and
     * authorization header are attached after the {@code ENC=} envelope
     * is assembled. A non-200 status code surfaces as a runtime
     * exception carrying the failing endpoint and status code.
     *
     * @param path   the API sub-path
     * @param params the unencrypted form body
     * @return the raw response bytes
     * @throws IOException          if the HTTP call fails
     * @throws InterruptedException if the sending thread is interrupted
     * @throws RuntimeException     if encryption fails or the HTTP
     *                              status is not 200
     */
    private byte[] sendRequest(String path, String params) throws IOException, InterruptedException {
        try {
            var keypair = SignalIdentityKeyPair.random();
            var key = Curve25519.sharedKey(keypair.privateKey().toEncodedPoint(), REGISTRATION_PUBLIC_KEY);

            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(128, new byte[12])
            );
            var result = cipher.doFinal(params.getBytes(StandardCharsets.UTF_8));

            var cipheredParameters = Base64.getUrlEncoder().encodeToString(DataUtils.concatByteArrays(keypair.publicKey().toEncodedPoint(), result));

            var attestation = attestBody(cipheredParameters.getBytes(StandardCharsets.UTF_8));

            var body = new StringBuilder("ENC=").append(cipheredParameters);
            if (attestation.bodyAttestation() != null && !attestation.bodyAttestation().isEmpty()) {
                body.append("&H=").append(attestation.bodyAttestation());
            }
            var requestBuilder = createRequest(path, body.toString(), attestation.authorizationHeader());

            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sending mobile registration request to {0}", path);
            var response = httpClient.send(requestBuilder, HttpResponse.BodyHandlers.ofByteArray());
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mobile registration request to {0} -> status {1}", path, response.statusCode());
            if(response.statusCode() != 200) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "mobile registration request to {0} failed, status={1}", path, response.statusCode());
                throw new RuntimeException("Cannot send request to " + path + ": status code" + response.statusCode());
            }
            return response.body();
        } catch (GeneralSecurityException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "mobile registration request encryption failed for " + path, exception);
            throw new RuntimeException("Cannot encrypt request", exception);
        }
    }

    /**
     * Assembles the shared registration form body and appends any
     * caller-supplied extra fields.
     *
     * <p>Used by every attested endpoint ({@code /exist},
     * {@code /code}, {@code /register}, {@code /security},
     * {@code /challenge}). The returned string carries every shared
     * field the server expects: country code and national number,
     * locale, release channel, the Signal identity / pre-key / signed
     * pre-key trio with its signature, the Noise key, the FDID, the
     * per-session {@code access_session_id}, the optional registration
     * token, the business verified-name certificate (only on business
     * platforms), and the platform-specific attestation and push
     * fields returned by {@link #attestationFields()}. The funnel
     * endpoints ({@code /client_log}, {@code /pre_pn_client_log}) build
     * their own body and bypass this method, matching the native
     * client's {@code sendAttestationPayload=false} configuration for
     * them.
     *
     * @implNote
     * This implementation skips {@code null}-valued fields silently so
     * subclasses can pass conditional values (the registration token,
     * the business certificate) without sentinel checks at the call
     * site.
     *
     * @param useToken   whether to compute and include the
     *                   registration token
     * @param attributes optional additional alternating name/value
     *                   pairs appended after the shared fields
     * @return the URL-encoded form body
     */
    private String getRegistrationOptions(boolean useToken, String... attributes) {
        var phoneNumber = getPhoneNumber(store);

        var token = getToken(phoneNumber, useToken);

        var certificate = generateBusinessCertificate();
        var fdid = generateFdid();

        var registrationParams = toFormParams(
                "cc", String.valueOf(phoneNumber.getCountryCode()),
                "in", String.valueOf(phoneNumber.getNationalNumber()),
                "rc", String.valueOf(store.accountStore().releaseChannel().index()),
                "lg", "en",
                "lc", "US",
                "authkey", Base64.getUrlEncoder().encodeToString(store.signalStore().noiseKeyPair().publicKey().toEncodedPoint()),
                "vname", certificate,
                "e_regid", Base64.getUrlEncoder().encodeToString(DataUtils.intToBytes(store.signalStore().registrationId(), 4)),
                "e_keytype", SIGNAL_PUBLIC_KEY_TYPE,
                "e_ident", Base64.getUrlEncoder().encodeToString(store.signalStore().identityKeyPair().publicKey().toEncodedPoint()),
                "e_skey_id", Base64.getUrlEncoder().encodeToString(DataUtils.intToBytes(store.signalStore().signedKeyPair().id(), 3)),
                "e_skey_val", Base64.getUrlEncoder().encodeToString(store.signalStore().signedKeyPair().publicKey().toEncodedPoint()),
                "e_skey_sig", Base64.getUrlEncoder().encodeToString(store.signalStore().signedKeyPair().signature()),
                "fdid", fdid,
                "expid", Base64.getUrlEncoder().encodeToString(store.signalStore().deviceId()),
                "id", toUrlHex(store.signalStore().identityId()),
                "access_session_id", accessSessionId,
                "token", useToken ? token : null
        );

        var attestationParams = toFormParams(attestationFields());

        var additionalParams = toFormParams(attributes);

        return joinNonEmpty(registrationParams, attestationParams, additionalParams);
    }

    /**
     * Concatenates form-parameter fragments with ampersand separators,
     * skipping empty or {@code null} fragments.
     *
     * <p>Lets {@link #getRegistrationOptions(boolean, String...)}
     * compose the body out of three groups (shared, attestation,
     * additional) without checking whether any one of them is empty.
     *
     * @param fragments the fragments to join, each already in
     *                  {@code name=value&name=value} form
     * @return the joined form body, possibly empty
     */
    private static String joinNonEmpty(String... fragments) {
        var out = new StringBuilder();
        for (var fragment : fragments) {
            if (fragment == null || fragment.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(fragment);
        }
        return out.toString();
    }

    /**
     * Computes the registration token unless the caller asks for it to
     * be omitted.
     *
     * <p>The token is derived from the national number via the
     * platform-specific
     * {@link WhatsAppMobileClientInfo#computeRegistrationToken(long)}
     * implementation, which adapts the native client's per-platform
     * obfuscation step.
     *
     * @param phoneNumber the parsed phone number
     * @param useToken    whether a token should be computed
     * @return the token, or {@code null} when {@code useToken} is
     *         {@code false}
     */
    private String getToken(PhoneNumber phoneNumber, boolean useToken) {
        if (!useToken) {
            return null;
        }

        var info = WhatsAppMobileClientInfo.of(store.accountStore().device().platform());
        return info.computeRegistrationToken(phoneNumber.getNationalNumber());
    }

    /**
     * Generates a WhatsApp Business verified-name certificate when the
     * configured platform is a business flavour.
     *
     * <p>Returns {@code null} on consumer platforms so the
     * {@code vname} form field is omitted from the body entirely.
     *
     * @implNote
     * This implementation issues a placeholder certificate with an
     * empty verified name and a random serial number, signed with the
     * local Signal identity private key. WhatsApp's business platforms
     * expect the field to be present even for unverified merchants; the
     * real verified-name flow runs post-registration and replaces this
     * placeholder.
     *
     * @return the base64-URL encoded certificate, or {@code null} on
     *         consumer platforms
     */
    protected String generateBusinessCertificate() {
        var platform = store.accountStore().device().platform();
        if(platform != ClientPlatformType.ANDROID_BUSINESS && platform != ClientPlatformType.IOS_BUSINESS) {
            return null;
        }

        var details = new BusinessVerifiedNameCertificateDetailsBuilder()
                .verifiedName("")
                .issuer(BusinessVerifiedNameCertificate.CertificateIssuer.SMALL_BUSINESS)
                .serial(Math.abs(new SecureRandom().nextLong()))
                .build();
        var encodedDetails = BusinessVerifiedNameCertificateDetailsSpec.encode(details);

        var certificate = new BusinessVerifiedNameCertificateBuilder()
                .details(encodedDetails)
                .signature(Curve25519.sign(store.signalStore().identityKeyPair().privateKey().toEncodedPoint(), encodedDetails))
                .build();
        return Base64.getUrlEncoder().encodeToString(BusinessVerifiedNameCertificateSpec.encode(certificate));
    }

    /**
     * Reads the phone number from the store and parses it into a
     * {@link PhoneNumber}.
     *
     * <p>The parsed form is needed to split the country code and the
     * national number into the {@code cc} and {@code in} form fields.
     *
     * @implNote
     * This implementation feeds the number through
     * {@link PhoneNumberUtil} after prepending {@code "+"} so
     * libphonenumber treats it as E.164. A malformed number surfaces
     * as a {@link WhatsAppRegistrationException}, never as a raw
     * {@link NumberParseException}.
     *
     * @param store the store carrying the registered phone number
     * @return the parsed phone number
     * @throws WhatsAppRegistrationException if the store has no phone
     *                                       number or it cannot be
     *                                       parsed
     */
    protected static PhoneNumber getPhoneNumber(LinkedWhatsAppStore store) {
        var phoneNumber = store.accountStore().phoneNumber()
                .orElseThrow(() -> new WhatsAppRegistrationException("Phone number wasn't set"));
        try {
            return PhoneNumberUtil.getInstance()
                    .parse("+" + phoneNumber, null);
        }catch (NumberParseException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "malformed phone number in store", exception);
            throw new WhatsAppRegistrationException("Malformed phone number: " + phoneNumber);
        }
    }

    /**
     * Percent-encodes every byte of a buffer as {@code %XX} in
     * uppercase.
     *
     * <p>Produces the value of the mobile API's {@code id} form field
     * (over the local identity id) and the Android {@code backup_token}
     * field. The mobile registration server expects opaque byte blobs
     * to be percent-encoded regardless of whether each byte is URL-safe
     * on its own.
     *
     * @param buffer the byte buffer to format
     * @return the percent-encoded uppercase hex string
     */
    protected String toUrlHex(byte[] buffer) {
        var id = new StringBuilder();
        for (var x : buffer) {
            id.append(String.format("%%%02x", x));
        }
        return id.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * Joins alternating name / value pairs into a
     * {@code name1=value1&name2=value2} form body.
     *
     * <p>Pairs whose value is {@code null} are dropped silently so
     * callers can pass conditionals without sentinel checks.
     *
     * @param entries alternating name / value pairs, totalling an even
     *                count
     * @return the joined form body
     * @throws IllegalArgumentException if {@code entries.length} is odd
     */
    private String toFormParams(String... entries) {
        if (entries == null) {
            return "";
        }

        var length = entries.length;
        if ((length & 1) != 0) {
            throw new IllegalArgumentException("Odd form patches");
        }

        var result = new StringJoiner("&");
        for (var i = 0; i < length; i += 2) {
            if (entries[i + 1] == null) {
                continue;
            }
            result.add(entries[i] + "=" + entries[i + 1]);
        }

        return result.toString();
    }

    /**
     * Fires a pre-phone-number funnel event against
     * {@code /v2/pre_pn_client_log}.
     *
     * <p>The native client uses this endpoint for events that
     * originate on screens the user sees before entering their phone
     * number (EULA, idle phone-number entry screen). Cobalt has no such
     * UI stage, so the only event ever sent here is the single
     * {@code registration_session_start} fired at the top of
     * {@link #register}.
     *
     * @implNote
     * This implementation deliberately omits the {@code cc} and
     * {@code in} fields even when the store already has a phone number
     * on file, to match the shape the native client emits. Every
     * throwable is swallowed because funnel telemetry must never abort
     * a registration.
     *
     * @param actionTaken the short action string (for example
     *                    {@code "session_start"})
     * @param eventName   the long event identifier the server uses to
     *                    bucket the event
     */
    private void sendPrePnFunnelLog(String actionTaken, String eventName) {
        try {
            var fdid = generateFdid();
            var body = toFormParams(
                    "lg", "en",
                    "lc", "US",
                    "expid", Base64.getUrlEncoder().encodeToString(store.signalStore().deviceId()),
                    "fdid", fdid,
                    "id", toUrlHex(store.signalStore().identityId()),
                    "current_screen", "enter_number",
                    "previous_screen", "",
                    "action_taken", actionTaken,
                    "event_name", eventName
            );
            sendRequest("/pre_pn_client_log", body);
        } catch (Throwable e) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "pre-pn funnel log failed, action=" + actionTaken, e);
        }
    }

    /**
     * Fires a funnel event against {@code /v2/client_log}.
     *
     * <p>Invoked once per UI transition in the flow
     * ({@code exist_check}, {@code request_code}, {@code submit_code},
     * {@code challenge_shown}, and so on). Each call advances
     * {@link #previousFunnelScreen} so subsequent events report the
     * correct {@code previous_screen} attribute.
     *
     * @implNote
     * This implementation includes the {@code cc}/{@code in} pair
     * because the phone number has been committed by the time this
     * runs, but does not include the Play Integrity attestation triple:
     * the native client's per-endpoint configuration disables
     * attestation for {@code /v2/client_log}. Every throwable is
     * swallowed because funnel telemetry must never abort a
     * registration.
     *
     * @param currentScreen the screen name this event originates from
     *                      (for example {@code "enter_number"},
     *                      {@code "verify_sms"}, {@code "verify_twofac"})
     * @param actionTaken   the short action string describing what the
     *                      user or client just did
     * @param eventName     the long event identifier the server uses to
     *                      bucket the event
     */
    private void sendFunnelLog(String currentScreen, String actionTaken, String eventName) {
        try {
            var phoneNumber = getPhoneNumber(store);
            var fdid = generateFdid();
            var body = toFormParams(
                    "cc", String.valueOf(phoneNumber.getCountryCode()),
                    "in", String.valueOf(phoneNumber.getNationalNumber()),
                    "lg", "en",
                    "lc", "US",
                    "expid", Base64.getUrlEncoder().encodeToString(store.signalStore().deviceId()),
                    "fdid", fdid,
                    "id", toUrlHex(store.signalStore().identityId()),
                    "current_screen", currentScreen,
                    "previous_screen", Objects.requireNonNullElse(previousFunnelScreen, ""),
                    "action_taken", actionTaken,
                    "event_name", eventName
            );
            sendRequest("/client_log", body);
            previousFunnelScreen = currentScreen;
        } catch (Throwable e) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "funnel log failed, screen=" + currentScreen, e);
        }
    }

    /**
     * Returns the funnel screen name that corresponds to the most
     * recently requested verification method.
     *
     * <p>Falls back to {@code "enter_number"} when no method has yet
     * been recorded so the {@code current_screen} attribute stays
     * populated even on degenerate flows where a funnel event fires
     * before the first {@code /v2/code} call.
     *
     * @return the funnel screen name
     */
    private String currentVerifyScreen() {
        if (lastRequestedMethod == null) {
            return "enter_number";
        }
        return "verify_" + lastRequestedMethod;
    }

    /**
     * Closes the shared {@link HttpClient} backing this registration.
     *
     * <p>Suitable for try-with-resources around a single registration
     * ceremony. Once closed, further calls to {@link #register} on the
     * same instance fail at the first HTTP send.
     */
    @Override
    public void close() {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "mobile registration client closing");
        httpClient.close();
    }
}
