package com.github.auties00.cobalt.registration.push.apns;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.registration.push.apns.activation.ApnsActivationCrypto;
import com.github.auties00.cobalt.registration.push.apns.activation.ApnsActivationInfo;
import com.github.auties00.cobalt.registration.push.apns.plist.Plist;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDictionaryValue;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Drives the FairPlay-signed device activation handshake against {@code albert.apple.com} so the
 * APNS client can present a real Apple-signed device certificate to the courier connection.
 *
 * <p>The handshake mirrors the flow a freshly-flashed iOS device performs on first boot:
 * <ol>
 *   <li>generate a 2048-bit RSA keypair via {@link ApnsActivationCrypto#newRsaKeyPair()};</li>
 *   <li>encode a PKCS#10 CSR for that keypair and wrap it in an {@code ActivationInfo} plist;</li>
 *   <li>sign the plist bytes with the leaked FairPlay private key;</li>
 *   <li>POST the signed bundle to {@code albert.apple.com/.../deviceActivation};</li>
 *   <li>extract the {@code DeviceCertificate} from the {@code <Protocol>} block of the
 *       response.</li>
 * </ol>
 *
 * <p>The resulting certificate is valid for roughly three years; on a subsequent run the saved
 * certificate plus keypair are restored via {@link ApnsClient#loadSession(ApnsSession)} and the
 * round-trip is skipped entirely.
 */
final class ApnsActivation {
    /**
     * The logger for {@link ApnsActivation}.
     */
    private static final System.Logger LOGGER = Log.get(ApnsActivation.class);

    /**
     * Holds the URL that signs the device CSR.
     *
     * <p>Hit over HTTPS exactly once per fresh activation; the response body is a plist whose
     * {@code <Protocol>} element holds the Apple-signed device certificate.
     */
    private static final String ACTIVATION_URL = "https://albert.apple.com/WebObjects/ALUnbrick.woa/wa/deviceActivation";

    /**
     * Holds the regex that pulls the inner {@code <Protocol>} plist out of the activation response.
     *
     * @implNote This implementation relies on Apple wrapping the inner blob between literal
     *           {@code <Protocol>} markers without surrounding whitespace; the
     *           {@link Pattern#MULTILINE} flag keeps the dot-matching behaviour portable across
     *           response shapes.
     */
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile(
            "<Protocol>([^*]+)</Protocol>", Pattern.MULTILINE);

    /**
     * Holds the connect and request timeout applied to the activation HTTP call.
     */
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Holds the hard-coded FairPlay certificate chain leaked from a real iOS device.
     *
     * <p>{@code albert.apple.com} validates the activation request against this chain plus the
     * {@code FairPlaySignature} produced by {@link ApnsActivationCrypto#signActivationInfo(byte[])}.
     *
     * @implNote This implementation ships the chain verbatim so the impersonation is
     *           indistinguishable from a real device boot at the HTTP layer.
     */
    private static final byte[] FAIRPLAY_CERT_CHAIN = Base64.getDecoder().decode(
            "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");

    /**
     * Holds the HTTP client used for the activation POST.
     *
     * <p>Built once with the configured proxy and reused across {@link #activate(ApnsSession)}
     * retries.
     */
    private final HttpClient http;

    /**
     * Constructs an activation helper bound to a proxy.
     *
     * <p>The proxy is honoured by the internal {@link HttpClient} for the single activation POST.
     *
     * @param proxy proxy URI ({@code http(s)://...}, {@code socks://...}), or {@code null} to dial
     *              {@code albert.apple.com} directly
     */
    ApnsActivation(URI proxy) {
        this.http = newHttpClient(proxy);
    }

    /**
     * Runs the activation handshake.
     *
     * <p>Returns early when the session is already fully populated (all three credential fields are
     * non-empty), making this safe to call on a freshly loaded session for which the credentials were
     * captured on a previous run. On success the session carries a fresh RSA keypair and the
     * Apple-signed device certificate.
     *
     * @implNote This implementation generates the keypair and CSR locally via
     *           {@link ApnsActivationCrypto}, signs the inner activation plist with the leaked
     *           FairPlay key, wraps it in the outer URL-encoded form body, POSTs it to
     *           {@link #ACTIVATION_URL}, and parses the response with the {@link #PROTOCOL_PATTERN}
     *           regex. The {@link InterruptedException} thrown by {@link HttpClient#send} is surfaced
     *           as {@link IOException} after restoring the interrupt flag so callers can treat it as
     *           a transient transport failure.
     *
     * @param session the session to mutate
     * @throws IOException on any HTTP failure or if the response is missing the {@code <Protocol>}
     *                     block
     */
    void activate(ApnsSession session) throws IOException {
        if (session.deviceCertificate().length > 0
                && session.privateKeyDer().length > 0
                && session.publicKeyDer().length > 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "apns activation skipped, session already populated");
            return;
        }
        if (Log.INFO) LOGGER.log(Level.INFO, "apns activation starting");
        var keyPair = ApnsActivationCrypto.newRsaKeyPair();
        var csr = ApnsActivationCrypto.generateCsr(keyPair);
        var activationXml = buildActivationInfoXml(csr);
        var signature = ApnsActivationCrypto.signActivationInfo(activationXml);
        var body = buildOuterActivationBody(activationXml, signature);

        var request = HttpRequest.newBuilder()
                .uri(URI.create(ACTIVATION_URL))
                .timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "device=Windows&activation-info="
                                + URLEncoder.encode(body, StandardCharsets.UTF_8)))
                .build();
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "apns activation http failed, status={0}", response.statusCode());
                throw new IOException("APNS activation HTTP " + response.statusCode() + ": " + response.body());
            }
            var matcher = PROTOCOL_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "apns activation response missing protocol block");
                throw new IOException("activation response missing <Protocol> block: " + response.body());
            }
            var protocol = matcher.group(1).getBytes(StandardCharsets.UTF_8);
            var info = ApnsActivationInfo.ofPlist(protocol);
            session.setPrivateKeyDer(keyPair.getPrivate().getEncoded());
            session.setPublicKeyDer(keyPair.getPublic().getEncoded());
            session.setDeviceCertificate(info.deviceCertificate());
            if (Log.INFO) LOGGER.log(Level.INFO, "apns activation complete");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (Log.WARNING) LOGGER.log(Level.WARNING, "apns activation interrupted", ie);
            throw new IOException("APNS activation interrupted", ie);
        }
    }

    /**
     * Builds the inner activation plist as UTF-8 bytes.
     *
     * <p>Returns bytes rather than a {@link String} because Apple computes the FairPlay signature
     * over the exact byte sequence Cobalt later embeds verbatim in the outer body; routing the value
     * through {@link String} would risk encoding normalisation.
     *
     * @implNote This implementation advertises a fixed Windows-1,1 device profile because that is the
     *           device class {@code albert.apple.com} signs CSRs for from a Windows host; the
     *           per-call {@code ActivationRandomness} and {@code UniqueDeviceID} {@link UUID}s keep
     *           two consecutive activations distinguishable in Apple's logs.
     *
     * @param csr the PEM-encoded CSR returned by {@link ApnsActivationCrypto#generateCsr}
     * @return the UTF-8 plist bytes
     */
    private static byte[] buildActivationInfoXml(byte[] csr) {
        var dict = PlistDictionaryValue.builder()
                .put("ActivationRandomness", UUID.randomUUID().toString())
                .put("ActivationState", "Unactivated")
                .put("BuildVersion", "10.6.4")
                .put("DeviceCertRequest", csr)
                .put("DeviceClass", "Windows")
                .put("ProductType", "windows1,1")
                .put("ProductVersion", "10.6.4")
                .put("SerialNumber", "WindowSerial")
                .put("UniqueDeviceID", UUID.randomUUID().toString())
                .build();
        return Plist.writeXml(dict);
    }

    /**
     * Wraps the inner activation plist plus its FairPlay signature in the outer plist
     * {@code albert.apple.com} expects as the request body.
     *
     * <p>The four keys are exactly the ones Apple's activation endpoint indexes:
     * {@code ActivationInfoComplete}, {@code ActivationInfoXML}, {@code FairPlayCertChain}, and
     * {@code FairPlaySignature}.
     *
     * @param activationXml the inner plist bytes
     * @param signature     the SHA-1-with-RSA FairPlay signature over {@code activationXml}
     * @return the XML plist string for the request body
     */
    private static String buildOuterActivationBody(byte[] activationXml, byte[] signature) {
        var dict = PlistDictionaryValue.builder()
                .put("ActivationInfoComplete", true)
                .put("ActivationInfoXML", activationXml)
                .put("FairPlayCertChain", FAIRPLAY_CERT_CHAIN)
                .put("FairPlaySignature", signature)
                .build();
        return new String(Plist.writeXml(dict), StandardCharsets.UTF_8);
    }

    /**
     * Builds an {@link HttpClient} configured with the timeout and the optional proxy.
     *
     * <p>When the proxy URI omits an explicit port the default {@code 8080} is used, matching the
     * convention most HTTP proxies advertise.
     *
     * @param proxy proxy URI, or {@code null} for direct
     * @return a configured HTTP client
     */
    private static HttpClient newHttpClient(URI proxy) {
        var builder = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (proxy != null && proxy.getHost() != null) {
            var port = proxy.getPort() == -1 ? 8080 : proxy.getPort();
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), port)));
        }
        return builder.build();
    }
}
