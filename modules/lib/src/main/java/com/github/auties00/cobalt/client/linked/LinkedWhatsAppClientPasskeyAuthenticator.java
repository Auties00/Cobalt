package com.github.auties00.cobalt.client.linked;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.passkey.SystemPasskeyAuthenticator;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.github.auties00.qr.QrTerminal;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Bridge between Cobalt and a WebAuthn authenticator able to assert a passkey registered for
 * WhatsApp's relying party.
 *
 * <p>Both of WhatsApp's passkey ceremonies, companion linking
 * ({@link LinkedWhatsAppClientVerificationHandler.Web.Passkey}) and the server-pushed integrity
 * checkpoint, require the client to produce a WebAuthn assertion over a server-issued challenge. A
 * browser cannot do this on a third-party origin: it binds the relying-party identifier to its own
 * origin and refuses to assert {@code whatsapp.com} from anywhere outside {@code *.whatsapp.com}.
 * The assertion's origin, however, is only a string the authenticator signs over and the server
 * validates against an allow list, so no browser is needed. This interface is the seam through
 * which a holder of the credential, wherever it lives, produces the assertion.
 *
 * <p>Two strategies satisfy the contract:
 * <ul>
 *   <li><b>Relay</b> - {@link #toWebAuthnJson(Function)} wraps a handler that ships
 *       {@link Request#toWebAuthnGetJson()} to a real authenticator the embedder controls (a browser
 *       served from {@code web.whatsapp.com}, the user's phone, a hardware security key), blocks
 *       until the ceremony returns, and rebuilds the result with
 *       {@link Assertion#fromWebAuthnGetJson(String)}; implementing this interface directly does the
 *       same by hand. This is the path for a headless or REST deployment whose credential lives on
 *       another device.</li>
 *   <li><b>Hybrid QR</b> - {@link #toQr(Consumer)} returns an opt-in desktop bridge that drives
 *       Warden's entitlement-free cross-device hybrid transport, handing each ceremony's
 *       {@code FIDO:/...} QR payload to a consumer to render; the user scans it with the phone that
 *       holds the {@code whatsapp.com} passkey, which then produces the assertion over an encrypted
 *       tunnel. {@link #toTerminal()} renders it as a terminal QR for you.</li>
 * </ul>
 *
 * @apiNote Implementations block the calling virtual thread until the ceremony completes; a remote
 *          implementation that prompts an off-machine authenticator must therefore return within the
 *          ceremony's timeout (120 seconds for companion linking, 10 minutes for the integrity
 *          checkpoint) or the operation is abandoned. Implementations are not required to be
 *          thread-safe: the library invokes {@link #assertCredential(Request)} sequentially.
 */
@FunctionalInterface
public interface LinkedWhatsAppClientPasskeyAuthenticator {
    /**
     * Produces a WebAuthn assertion satisfying the given request.
     *
     * <p>The returned assertion must sign the standard WebAuthn signed data
     * ({@code authenticatorData || SHA-256(clientDataJSON)}) with the private key of a credential the
     * relying party {@link Request#relyingPartyId()} registered, and its {@code clientDataJSON} must
     * carry an {@link Request#origin() origin} the relying party allows.
     *
     * @param request the challenge to assert against
     * @return the produced assertion
     * @throws RuntimeException if no usable credential is available or the ceremony fails or is
     *                          declined; the caller converts a throw into aborting the ceremony or
     *                          logging the session out
     */
    Assertion assertCredential(Request request);

    /**
     * Returns a relay authenticator that hands each ceremony off to an external WebAuthn
     * authenticator as JSON.
     *
     * <p>The returned authenticator serialises every request with {@link Request#toWebAuthnGetJson()},
     * passes the {@code navigator.credentials.get} options JSON to {@code ceremony}, and rebuilds the
     * result from the {@code PublicKeyCredential} JSON the handler returns with
     * {@link Assertion#fromWebAuthnGetJson(String)}. The handler owns the transport: forwarding the
     * options to a browser served from {@code *.whatsapp.com}, the user's phone, a hardware key, or a
     * REST endpoint, and blocking until that authenticator answers. This packages the relay strategy
     * so the embedder never touches {@link Request} or {@link Assertion} directly.
     *
     * @apiNote This is the path for a headless or REST deployment whose credential lives on another
     *          device; {@code ceremony} runs on the calling virtual thread and must return the result
     *          JSON within the request's {@link Request#timeout() timeout} or the ceremony is
     *          abandoned.
     *
     * @param ceremony maps the {@code navigator.credentials.get} options JSON to the resulting
     *                 {@code PublicKeyCredential} JSON; never {@code null}
     * @return a relay authenticator over the given handler
     * @throws NullPointerException if {@code ceremony} is {@code null}
     */
    static LinkedWhatsAppClientPasskeyAuthenticator toWebAuthnJson(Function<String, String> ceremony) {
        Objects.requireNonNull(ceremony, "ceremony must not be null");
        return request -> Assertion.fromWebAuthnGetJson(ceremony.apply(request.toWebAuthnGetJson()));
    }

    /**
     * Returns a hybrid-transport authenticator that renders each ceremony's QR code as a scannable
     * terminal QR on standard output.
     *
     * <p>Drives Warden's entitlement-free cross-device hybrid transport, exactly like
     * {@link #toQr(Consumer)}, and prints each {@code FIDO:/...} QR payload as ASCII art for the user to
     * scan with the phone that holds the {@code whatsapp.com} passkey. Use {@link #toQr(Consumer)} to
     * render the QR on a surface of your own instead.
     *
     * @return a hybrid-transport authenticator that prints its QR code to standard output
     * @throws UnsupportedOperationException if the host platform has no supported native passkey service
     */
    static LinkedWhatsAppClientPasskeyAuthenticator toTerminal() {
        return toQr(LinkedWhatsAppClientPasskeyAuthenticator::printQrToTerminal);
    }

    /**
     * Returns an opt-in desktop authenticator that drives Warden's cross-device hybrid transport,
     * handing each ceremony's {@code FIDO:/...} QR payload to the given consumer to render.
     *
     * <p>The user scans the rendered QR with the phone that holds the {@code whatsapp.com} passkey, and
     * the phone produces the assertion over an encrypted tunnel. The transport is entitlement-free on
     * every platform, so it suits desktop applications that cannot carry a native WebAuthn entitlement.
     * Render the QR as an image, a web asset, or any surface of your own; {@link #toTerminal()}
     * renders it as a terminal QR on standard output.
     *
     * @param onQrCode the consumer the {@code FIDO:/...} QR payload is handed to for rendering; never
     *                 {@code null}
     * @return a hybrid-transport authenticator over the given QR consumer
     * @throws NullPointerException          if {@code onQrCode} is {@code null}
     * @throws UnsupportedOperationException if the host platform has no supported native passkey service
     */
    static LinkedWhatsAppClientPasskeyAuthenticator toQr(Consumer<String> onQrCode) {
        return SystemPasskeyAuthenticator.create(onQrCode);
    }

    /**
     * Renders a {@code FIDO:/...} hybrid-transport URL as a scannable QR code on standard output.
     *
     * <p>Encodes the URL as a level-{@code L} QR matrix and prints it as ASCII art, prefixed with a
     * short instruction so the user knows to scan it with the phone that holds the passkey.
     *
     * @param fidoUrl the {@code FIDO:/...} URL to encode
     * @throws UnsupportedOperationException if the URL cannot be encoded as a QR code
     */
    private static void printQrToTerminal(String fidoUrl) {
        try {
            var matrix = new MultiFormatWriter().encode(fidoUrl, BarcodeFormat.QR_CODE, 10, 10,
                    Map.of(EncodeHintType.MARGIN, 0, EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L));
            System.out.println("Scan this QR code with the phone that holds your WhatsApp passkey:");
            QrTerminal.print(matrix, true);
        } catch (WriterException exception) {
            throw new UnsupportedOperationException("Cannot render the FIDO QR code", exception);
        }
    }

    /**
     * The user-verification preference a request expresses to the authenticator.
     *
     * <p>Mirrors the WebAuthn {@code UserVerificationRequirement} enumeration: whether the
     * authenticator must, may, or should not perform local user verification (PIN, biometric) during
     * the ceremony.
     */
    enum UserVerification {
        /**
         * User verification is required; the authenticator must verify the user or fail.
         */
        REQUIRED("required"),
        /**
         * User verification is preferred but not mandatory.
         */
        PREFERRED("preferred"),
        /**
         * User verification should not be performed, to streamline the ceremony.
         */
        DISCOURAGED("discouraged");

        /**
         * The lowercase token WebAuthn uses for this preference on the wire.
         */
        private final String wireValue;

        /**
         * Constructs a preference bound to its WebAuthn wire token.
         *
         * @param wireValue the lowercase WebAuthn token
         */
        UserVerification(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * Returns the lowercase WebAuthn token for this preference.
         *
         * @return the wire token, for example {@code "preferred"}
         */
        public String wireValue() {
            return wireValue;
        }
    }

    /**
     * The challenge the library hands to an authenticator to assert against.
     *
     * <p>Carries everything a WebAuthn {@code navigator.credentials.get} ceremony needs: the relying
     * party, the server challenge, the optional allow list of acceptable credential ids (empty for a
     * discoverable-credential request), the user-verification preference, the ceremony timeout, the
     * optional PRF evaluation input, and the {@code origin} the assertion's {@code clientDataJSON}
     * must declare. {@link #origin()} defaults to {@code https://web.whatsapp.com}, the origin the
     * genuine web client signs.
     *
     * @param relyingPartyId      the relying-party identifier, for example {@code whatsapp.com}
     * @param challenge           the raw server challenge bytes
     * @param allowedCredentialIds the acceptable credential ids, or an empty array for a discoverable
     *                            (allow-list-less) request
     * @param userVerification    the user-verification preference
     * @param timeout             the ceremony timeout
     * @param prfEvalFirst        the first PRF (hmac-secret) evaluation input, or {@code null} when
     *                            no PRF is requested
     * @param origin              the origin the assertion's {@code clientDataJSON} must declare
     */
    record Request(String relyingPartyId, byte[] challenge, byte[][] allowedCredentialIds,
                   UserVerification userVerification, Duration timeout, byte[] prfEvalFirst,
                   String origin) {
        /**
         * The default origin a Cobalt-issued request declares, matching the genuine web client.
         */
        public static final String WEB_ORIGIN = "https://web.whatsapp.com";

        /**
         * The relying-party identifier WhatsApp passkeys are scoped to.
         */
        public static final String WHATSAPP_RP_ID = "whatsapp.com";

        /**
         * Validates required components and defensively copies {@code allowedCredentialIds}.
         *
         * @throws NullPointerException if any required component is {@code null}
         */
        public Request {
            Objects.requireNonNull(relyingPartyId, "relyingPartyId must not be null");
            Objects.requireNonNull(challenge, "challenge must not be null");
            Objects.requireNonNull(allowedCredentialIds, "allowedCredentialIds must not be null");
            Objects.requireNonNull(userVerification, "userVerification must not be null");
            Objects.requireNonNull(timeout, "timeout must not be null");
            Objects.requireNonNull(origin, "origin must not be null");
            allowedCredentialIds = deepCopy(allowedCredentialIds);
        }

        /**
         * Returns a deep copy of the acceptable credential ids.
         *
         * @return the allow list, empty for a discoverable request; never {@code null}
         */
        @Override
        public byte[][] allowedCredentialIds() {
            return deepCopy(allowedCredentialIds);
        }

        /**
         * Deep-copies an array of credential ids so the request owns immutable state.
         *
         * @param ids the ids to copy
         * @return an independent copy
         */
        private static byte[][] deepCopy(byte[][] ids) {
            var copy = new byte[ids.length][];
            for (var index = 0; index < ids.length; index++) {
                copy[index] = ids[index].clone();
            }
            return copy;
        }

        /**
         * Decodes the opaque {@code passkey_request_options} bytes WhatsApp's Shortcake linking flow
         * returns into a request.
         *
         * <p>The server returns the options as a UTF-8 JSON object whose {@code challenge} and
         * {@code allowCredentials[].id} are URL-safe base64 strings, plus optional {@code rpId},
         * {@code userVerification}, {@code timeout}, and a {@code prf} extension. Components the JSON
         * omits fall back to {@link #WHATSAPP_RP_ID}, {@link UserVerification#PREFERRED}, and a
         * two-minute timeout.
         *
         * @param optionsJson the raw UTF-8 JSON options bytes from the server
         * @param origin      the origin the resulting assertion must declare
         * @return the decoded request
         * @throws NullPointerException if {@code optionsJson} or {@code origin} is {@code null}
         */
        public static Request ofShortcakeOptions(byte[] optionsJson, String origin) {
            Objects.requireNonNull(optionsJson, "optionsJson must not be null");
            Objects.requireNonNull(origin, "origin must not be null");
            var json = JSON.parseObject(new String(optionsJson, StandardCharsets.UTF_8));
            var rpId = json.getString("rpId");
            var challenge = decodeBase64Url(json.getString("challenge"));
            var allowList = new ArrayList<byte[]>();
            var allowCredentials = json.getJSONArray("allowCredentials");
            if (allowCredentials != null) {
                for (var i = 0; i < allowCredentials.size(); i++) {
                    var entry = allowCredentials.getJSONObject(i);
                    var id = entry == null ? null : entry.getString("id");
                    if (id != null) {
                        allowList.add(decodeBase64Url(id));
                    }
                }
            }
            var userVerification = parseUserVerification(json.getString("userVerification"));
            var timeoutMillis = json.getLongValue("timeout", 120_000L);
            byte[] prfEvalFirst = null;
            var extensions = json.getJSONObject("extensions");
            if (extensions != null) {
                var prf = extensions.getJSONObject("prf");
                var eval = prf == null ? null : prf.getJSONObject("eval");
                var first = eval == null ? null : eval.getString("first");
                if (first != null) {
                    prfEvalFirst = decodeBase64Url(first);
                }
            }
            return new Request(rpId != null ? rpId : WHATSAPP_RP_ID, challenge,
                    allowList.toArray(new byte[0][]),
                    userVerification, Duration.ofMillis(timeoutMillis), prfEvalFirst, origin);
        }

        /**
         * Serialises this request into the {@code {publicKey: ...}} JSON a browser's
         * {@code navigator.credentials.get} accepts, with every binary field URL-safe base64 encoded.
         *
         * <p>A relay implementation forwards this to an authenticator it controls. The browser form is
         * accepted only from a {@code *.whatsapp.com} page: a browser refuses to assert this
         * relying party from any other origin.
         *
         * @return the {@code navigator.credentials.get} options as a JSON string
         */
        public String toWebAuthnGetJson() {
            var publicKey = new JSONObject();
            publicKey.put("challenge", encodeBase64Url(challenge));
            publicKey.put("rpId", relyingPartyId);
            publicKey.put("userVerification", userVerification.wireValue());
            publicKey.put("timeout", timeout.toMillis());
            if (allowedCredentialIds.length > 0) {
                var allowCredentials = new JSONArray();
                for (var id : allowedCredentialIds) {
                    var entry = new JSONObject();
                    entry.put("type", "public-key");
                    entry.put("id", encodeBase64Url(id));
                    allowCredentials.add(entry);
                }
                publicKey.put("allowCredentials", allowCredentials);
            }
            if (prfEvalFirst != null) {
                var eval = new JSONObject();
                eval.put("first", encodeBase64Url(prfEvalFirst));
                var prf = new JSONObject();
                prf.put("eval", eval);
                var extensions = new JSONObject();
                extensions.put("prf", prf);
                publicKey.put("extensions", extensions);
            }
            var root = new JSONObject();
            root.put("publicKey", publicKey);
            return root.toString();
        }

        /**
         * Maps a WebAuthn user-verification token onto the enum, defaulting to
         * {@link UserVerification#PREFERRED}.
         *
         * @param token the WebAuthn token, or {@code null}
         * @return the matching preference
         */
        private static UserVerification parseUserVerification(String token) {
            if (token == null) {
                return UserVerification.PREFERRED;
            }
            for (var value : UserVerification.values()) {
                if (value.wireValue().equals(token)) {
                    return value;
                }
            }
            return UserVerification.PREFERRED;
        }
    }

    /**
     * The assertion an authenticator produced.
     *
     * <p>Mirrors a WebAuthn {@code PublicKeyCredential} assertion result: the credential id, the
     * signed {@code authenticatorData} and {@code clientDataJSON}, the signature over their
     * concatenation, the optional user handle for a discoverable credential, and the optional PRF
     * (hmac-secret) output.
     *
     * @param credentialId      the id of the credential that signed
     * @param authenticatorData the raw authenticator data
     * @param clientDataJson    the raw {@code clientDataJSON}
     * @param signature         the signature over {@code authenticatorData || SHA-256(clientDataJSON)}
     * @param userHandle        the user handle, or {@code null} when none was returned
     * @param prfOutput         the first PRF evaluation result, or {@code null} when PRF was not
     *                          evaluated
     */
    record Assertion(byte[] credentialId, byte[] authenticatorData, byte[] clientDataJson,
                     byte[] signature, byte[] userHandle, byte[] prfOutput) {
        /**
         * Validates required components.
         *
         * @throws NullPointerException if any required component is {@code null}
         */
        public Assertion {
            Objects.requireNonNull(credentialId, "credentialId must not be null");
            Objects.requireNonNull(authenticatorData, "authenticatorData must not be null");
            Objects.requireNonNull(clientDataJson, "clientDataJson must not be null");
            Objects.requireNonNull(signature, "signature must not be null");
        }

        /**
         * Parses a browser {@code PublicKeyCredential} JSON result into an assertion.
         *
         * <p>Accepts the shape {@code navigator.credentials.get} produces when its binary fields are
         * URL-safe base64 encoded (as {@code PublicKeyCredential.toJSON()} emits): a top-level
         * {@code rawId}, a {@code response} object with {@code authenticatorData},
         * {@code clientDataJSON}, {@code signature}, and an optional {@code userHandle}, and an
         * optional {@code clientExtensionResults.prf.results.first}.
         *
         * @param credentialJson the browser credential result as a JSON string
         * @return the parsed assertion
         * @throws NullPointerException if {@code credentialJson} is {@code null}
         */
        public static Assertion fromWebAuthnGetJson(String credentialJson) {
            Objects.requireNonNull(credentialJson, "credentialJson must not be null");
            var json = JSON.parseObject(credentialJson);
            var rawId = json.getString("rawId");
            var credentialId = rawId != null ? decodeBase64Url(rawId) : decodeBase64Url(json.getString("id"));
            var response = json.getJSONObject("response");
            var authenticatorData = decodeBase64Url(response.getString("authenticatorData"));
            var clientDataJson = decodeBase64Url(response.getString("clientDataJSON"));
            var signature = decodeBase64Url(response.getString("signature"));
            var userHandleValue = response.getString("userHandle");
            var userHandle = userHandleValue != null && !userHandleValue.isEmpty()
                    ? decodeBase64Url(userHandleValue) : null;
            byte[] prfOutput = null;
            var extensionResults = json.getJSONObject("clientExtensionResults");
            if (extensionResults != null) {
                var prf = extensionResults.getJSONObject("prf");
                var results = prf == null ? null : prf.getJSONObject("results");
                var first = results == null ? null : results.getString("first");
                if (first != null) {
                    prfOutput = decodeBase64Url(first);
                }
            }
            return new Assertion(credentialId, authenticatorData, clientDataJson, signature,
                    userHandle, prfOutput);
        }
    }

    /**
     * Encodes bytes as URL-safe base64 without padding, the encoding WebAuthn JSON uses.
     *
     * @param value the bytes to encode
     * @return the URL-safe base64 string
     */
    private static String encodeBase64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    /**
     * Decodes a URL-safe base64 string, tolerating absent padding.
     *
     * @param value the URL-safe base64 string
     * @return the decoded bytes
     */
    private static byte[] decodeBase64Url(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
