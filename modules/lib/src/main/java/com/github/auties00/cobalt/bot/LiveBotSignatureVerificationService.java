package com.github.auties00.cobalt.bot;

import com.github.auties00.cobalt.exception.linked.WhatsAppBotSignatureException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotSignatureVerificationMetadata;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotSignatureVerificationUseCaseProof;
import com.github.auties00.cobalt.wire.linked.bot.feedback.BotSignatureVerificationUseCaseProof.BotSignatureUseCase;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.CertificateValidationEventEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.CertVerificationResultType;

import java.io.ByteArrayInputStream;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Production {@link BotSignatureVerificationService} that validates the bot-feature certificate chain
 * with the JDK PKI primitives and checks the Ed25519 message signature.
 *
 * @implNote
 * This implementation uses the JDK's {@link CertificateFactory} and {@link Signature} for X.509
 * parsing, per-certificate signature verification (both the Ed25519 {@code 1.3.101.112} and the
 * ECDSA {@code 1.2.840.10045.4.3.2} chains WA Web handles are covered by the platform providers)
 * and the final Ed25519 check, rather than the WASM PKI.js path WA Web falls back to.
 */
@WhatsAppWebModule(moduleName = "WAWebBotSignatureVerificationUtils")
@WhatsAppWebModule(moduleName = "WAWebBotSignatureCertificateManager")
@WhatsAppWebModule(moduleName = "WAWebBotSignatureVerificationGating")
public final class LiveBotSignatureVerificationService implements BotSignatureVerificationService {
    /**
     * The logger for {@link LiveBotSignatureVerificationService}.
     */
    private static final System.Logger LOGGER = Log.get(LiveBotSignatureVerificationService.class);

    /**
     * The supported signature version; WA Web rejects any proof whose version is not {@code 1}.
     */
    private static final String SIGNATURE_VERSION = "1";

    /**
     * The embedded {@code Meta WA Feature Root CA} that anchors every bot-feature certificate chain.
     */
    private static final String ROOT_CERTIFICATE_PEM = """
            -----BEGIN CERTIFICATE-----
            MIIC7TCCApOgAwIBAgIUAtu5QuxmVhfGT8LPkjfm40mSl0AwCgYIKoZIzj0EAwIw
            dzEgMB4GA1UEAwwXTWV0YSBXQSBGZWF0dXJlIFJvb3QgQ0ExCzAJBgNVBAYTAlVT
            MRMwEQYDVQQIDApDYWxpZm9ybmlhMRMwEQYDVQQHDApNZW5sbyBQYXJrMRwwGgYD
            VQQKDBNNZXRhIFBsYXRmb3JtcyBJbmMuMCAXDTI1MDkwNDE3MzEyNFoYDzIwNjUw
            OTA0MTczMTI0WjB3MSAwHgYDVQQDDBdNZXRhIFdBIEZlYXR1cmUgUm9vdCBDQTEL
            MAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExEzARBgNVBAcMCk1lbmxv
            IFBhcmsxHDAaBgNVBAoME01ldGEgUGxhdGZvcm1zIEluYy4wWTATBgcqhkjOPQIB
            BggqhkjOPQMBBwNCAAT60blw90ebreMkw8+Wpcs0ETAkr1VQjoZoyi7PSSQbsoiP
            qYRnzfRrR+xiahaXbYU83qXiTHjVUiOU9wDxI83qo4H6MIH3MA8GA1UdEwEB/wQF
            MAMBAf8wHQYDVR0OBBYEFNO7KMTVSYUxkL6VS3LyWJw7m76zMIG0BgNVHSMEgaww
            gamAFNO7KMTVSYUxkL6VS3LyWJw7m76zoXukeTB3MSAwHgYDVQQDDBdNZXRhIFdB
            IEZlYXR1cmUgUm9vdCBDQTELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3Ju
            aWExEzARBgNVBAcMCk1lbmxvIFBhcmsxHDAaBgNVBAoME01ldGEgUGxhdGZvcm1z
            IEluYy6CFALbuULsZlYXxk/Cz5I35uNJkpdAMA4GA1UdDwEB/wQEAwIBhjAKBggq
            hkjOPQQDAgNIADBFAiAINhjk9DbP416vx/WjqdUfexgic08aQsxnpDDsNE5M0gIh
            ANorq7KwCQVMtS2or5uKJAQsx1FxCHyDafq2GCk9t0AN
            -----END CERTIFICATE-----""";

    /**
     * The AB-props service consulted for the forwarding-verification enforcement level.
     */
    private final ABPropsService abPropsService;

    /**
     * The telemetry sink for the {@code CertificateValidationEvent} metrics.
     */
    private final WamService wamService;

    /**
     * The certificate-revocation service consulted for each chain certificate.
     */
    private final BotCertificateRevocationService revocationService;

    /**
     * The parsed {@link #ROOT_CERTIFICATE_PEM root certificate}, or {@code null} when it could not
     * be parsed.
     */
    private final X509Certificate rootCertificate;

    /**
     * Constructs a service bound to the given collaborators.
     *
     * @param abPropsService    the AB-props service, must not be {@code null}
     * @param wamService        the telemetry sink, must not be {@code null}
     * @param revocationService the certificate-revocation service, must not be {@code null}
     * @throws NullPointerException if any argument is {@code null}
     */
    public LiveBotSignatureVerificationService(ABPropsService abPropsService, WamService wamService,
                                               BotCertificateRevocationService revocationService) {
        this.abPropsService = Objects.requireNonNull(abPropsService, "abPropsService cannot be null");
        this.wamService = Objects.requireNonNull(wamService, "wamService cannot be null");
        this.revocationService = Objects.requireNonNull(revocationService, "revocationService cannot be null");
        this.rootCertificate = parseRootCertificate();
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotSignatureVerificationGating", exports = "getForwardVerificationEnforcementLevel", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public BotSignatureEnforcementLevel enforcementLevel() {
        var raw = abPropsService.getString(ABProp.AI_RICH_RESPONSE_FORWARDING_VERIFICATION_ENABLED_V1);
        if (raw == null) {
            return BotSignatureEnforcementLevel.NONE;
        }
        return switch (raw.replace("\"", "").trim()) {
            case "log_only" -> BotSignatureEnforcementLevel.LOG_ONLY;
            case "enforce_blocking" -> BotSignatureEnforcementLevel.ENFORCE_BLOCKING;
            default -> BotSignatureEnforcementLevel.NONE;
        };
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotSignatureVerificationGating", exports = "isForwardVerificationEnabled", adaptation = WhatsAppAdaptation.DIRECT)
    @Override
    public boolean isForwardVerificationEnabled() {
        return enforcementLevel() != BotSignatureEnforcementLevel.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotSignatureVerificationUtils", exports = "verifyBotMessageSignature", adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public boolean verifyBotMessageSignature(String botFbid, BotSignatureVerificationMetadata metadata, byte[] messageDigest) {
        var builder = new CertificateValidationEventEventBuilder().startVerificationLatency();
        var level = enforcementLevel();
        if (level == BotSignatureEnforcementLevel.NONE) {
            if (Log.TRACE) LOGGER.log(Level.TRACE, "bot signature verification skipped, enforcement level is none");
            commit(builder, 0, CertVerificationResultType.SKIPPED_AB_DISABLED, null);
            return true;
        }
        var blocking = level == BotSignatureEnforcementLevel.ENFORCE_BLOCKING;
        try {
            if (metadata == null || messageDigest == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "bot signature verification failed, metadata or digest missing, blocking={0}", blocking);
                commit(builder, 0, CertVerificationResultType.FAILED_SIGNATURE_DATA_MISSING, null);
                return !blocking;
            }
            var proof = metadata.proofs().stream()
                    .filter(candidate -> candidate.useCase().orElse(null) == BotSignatureUseCase.WA_BOT_MSG)
                    .findFirst()
                    .orElse(null);
            if (proof == null) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "bot signature verification failed, no wa_bot_msg proof, blocking={0}", blocking);
                commit(builder, 0, CertVerificationResultType.FAILED_SIGNATURE_DATA_MISSING, null);
                return !blocking;
            }
            var allowed = verifyProof(builder, proof, botFbid, messageDigest) || !blocking;
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bot signature verification result, allowed={0}, level={1}", allowed, level);
            return allowed;
        } catch (RuntimeException exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "bot signature verification threw unexpectedly", exception);
            commit(builder, 0, CertVerificationResultType.FAILED_UNKNOWN_ERROR, null);
            return !blocking;
        }
    }

    /**
     * Validates the certificate chain of a proof and checks the Ed25519 message signature.
     *
     * @param builder       the metric builder timing the verification
     * @param proof         the {@code WA_BOT_MSG} proof
     * @param botFbid       the bot Facebook id bound into the signed payload
     * @param messageDigest the signed unified-response bytes
     * @return {@code true} when the signature is valid, {@code false} otherwise
     */
    private boolean verifyProof(CertificateValidationEventEventBuilder builder, BotSignatureVerificationUseCaseProof proof,
                                String botFbid, byte[] messageDigest) {
        var chain = proof.certificateChain();
        var signature = proof.signature().orElse(null);
        if (signature == null) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot signature proof missing signature bytes, chain_length={0}", chain.size());
            commit(builder, chain.size(), CertVerificationResultType.FAILED_SIGNATURE_DATA_MISSING, null);
            return false;
        }
        if (proof.version().isEmpty() || proof.version().getAsInt() != 1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot signature proof has unsupported version {0}", proof.version().isPresent() ? proof.version().getAsInt() : null);
            commit(builder, chain.size(), CertVerificationResultType.FAILED_SIGNATURE_DATA_MALFORMED, null);
            return false;
        }
        if (chain.isEmpty()) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot signature proof has an empty certificate chain");
            commit(builder, 0, CertVerificationResultType.FAILED_CHAIN_INCOMPLETE, null);
            return false;
        }
        if (rootCertificate == null) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "bot signature verification unavailable, embedded root certificate failed to parse");
            commit(builder, chain.size(), CertVerificationResultType.FAILED_CHAIN_INCOMPLETE, null);
            return false;
        }
        List<X509Certificate> certificates;
        X509Certificate leaf;
        try {
            certificates = parseChain(chain);
            leaf = certificates.getFirst();
            validateChain(certificates);
        } catch (WhatsAppBotSignatureException exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "bot certificate chain validation failed, result={0}", exception.result());
            commit(builder, chain.size(), exception.result(), null);
            return false;
        }
        var valid = verifyEd25519(signature, constructSignaturePayload(botFbid, messageDigest), leaf.getPublicKey());
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "bot signature ed25519 check valid={0}, chain_length={1}", valid, chain.size());
        commit(builder, chain.size(),
                valid ? CertVerificationResultType.SUCCESS : CertVerificationResultType.FAILED_SIGNATURE_INVALID, leaf);
        return valid;
    }

    /**
     * Validates a parsed certificate chain up to the embedded root, throwing a
     * {@link WhatsAppBotSignatureException} carrying the failure result on the first problem.
     *
     * @param certificates the leaf-first parsed chain
     * @throws WhatsAppBotSignatureException when a certificate is expired, has an invalid issuer
     *                                       signature, or is revoked
     */
    private void validateChain(List<X509Certificate> certificates) {
        var now = new Date();
        var leaf = certificates.getFirst();
        if (!isValidAt(leaf, now)) {
            throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_EXPIRED_CERT);
        }
        var fullChain = new ArrayList<>(certificates);
        fullChain.add(rootCertificate);
        for (var position = 0; position < fullChain.size() - 1; position++) {
            var certificate = fullChain.get(position);
            var issuer = fullChain.get(position + 1);
            if (!isValidAt(issuer, now)) {
                throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_EXPIRED_CERT);
            }
            try {
                certificate.verify(issuer.getPublicKey());
            } catch (Exception exception) {
                throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_CHAIN_VALIDATION);
            }
            switch (revocationService.checkRevocationStatus(certificate.getSerialNumber(), now.getTime())) {
                case REVOKED -> throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_CERTIFICATE_REVOKED);
                case CRL_UNAVAILABLE -> throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_CRL_UNAVAILABLE);
                case CRL_STALE -> throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_CRL_EXPIRED);
                case VALID -> {
                }
            }
        }
    }

    /**
     * Returns whether a certificate is within its validity window at the given time.
     *
     * @param certificate the certificate to test
     * @param at          the reference time
     * @return {@code true} when {@code at} is within the certificate's validity period
     */
    private boolean isValidAt(X509Certificate certificate, Date at) {
        try {
            certificate.checkValidity(at);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Constructs the signed payload {@code version || botFbid || messageDigest}.
     *
     * @param botFbid       the bot Facebook id
     * @param messageDigest the unified-response bytes
     * @return the concatenated payload bytes
     */
    @WhatsAppWebExport(moduleName = "WAWebBotSignatureVerificationUtils", exports = "constructSignaturePayload", adaptation = WhatsAppAdaptation.DIRECT)
    private byte[] constructSignaturePayload(String botFbid, byte[] messageDigest) {
        var version = SIGNATURE_VERSION.getBytes(StandardCharsets.UTF_8);
        var fbid = botFbid.getBytes(StandardCharsets.UTF_8);
        var payload = new byte[version.length + fbid.length + messageDigest.length];
        System.arraycopy(version, 0, payload, 0, version.length);
        System.arraycopy(fbid, 0, payload, version.length, fbid.length);
        System.arraycopy(messageDigest, 0, payload, version.length + fbid.length, messageDigest.length);
        return payload;
    }

    /**
     * Verifies an Ed25519 detached signature over the payload with the given public key.
     *
     * @param signature the detached signature
     * @param payload   the signed payload
     * @param publicKey the leaf certificate's Ed25519 public key
     * @return {@code true} when the signature is valid, {@code false} on any failure
     */
    @WhatsAppWebExport(moduleName = "WAWebBotSignatureVerificationUtils", exports = "verifyEddsaSignature", adaptation = WhatsAppAdaptation.ADAPTED)
    private boolean verifyEd25519(byte[] signature, byte[] payload, PublicKey publicKey) {
        try {
            var verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * Parses a list of DER-encoded certificate bytes into a leaf-first chain.
     *
     * @param chain the DER-encoded certificates, leaf first
     * @return the parsed chain
     * @throws WhatsAppBotSignatureException when any certificate cannot be parsed
     */
    private List<X509Certificate> parseChain(List<byte[]> chain) {
        try {
            var factory = CertificateFactory.getInstance("X.509");
            var certificates = new ArrayList<X509Certificate>(chain.size());
            for (var encoded : chain) {
                certificates.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(encoded)));
            }
            return certificates;
        } catch (Exception exception) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "failed to parse bot certificate chain", exception);
            throw new WhatsAppBotSignatureException(CertVerificationResultType.FAILED_INVALID_CERT);
        }
    }

    /**
     * Parses the embedded {@link #ROOT_CERTIFICATE_PEM}.
     *
     * @return the parsed root certificate, or {@code null} when it cannot be parsed
     */
    private X509Certificate parseRootCertificate() {
        try {
            var factory = CertificateFactory.getInstance("X.509");
            var bytes = ROOT_CERTIFICATE_PEM.getBytes(StandardCharsets.UTF_8);
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (Exception exception) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "failed to parse embedded bot feature root certificate", exception);
            return null;
        }
    }

    /**
     * Stamps the chain length, result, latency and leaf-certificate metadata onto the metric and
     * commits it.
     *
     * @param builder         the metric builder timing the verification
     * @param certChainLength the number of certificates supplied in the proof chain
     * @param result          the verification result
     * @param leaf            the validated leaf certificate, or {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebBotCertificateValidationLogger", exports = "logCertificateValidation", adaptation = WhatsAppAdaptation.ADAPTED)
    private void commit(CertificateValidationEventEventBuilder builder, int certChainLength,
                        CertVerificationResultType result, X509Certificate leaf) {
        builder.certChainLength(certChainLength)
                .certVerificationResult(result)
                .signatureVersion(SIGNATURE_VERSION)
                .stopVerificationLatency();
        if (leaf != null) {
            builder.leafCertCommonName(commonName(leaf))
                    .leafCertId(leaf.getSerialNumber().toString(16))
                    .leafCertTtlDays((int) ChronoUnit.DAYS.between(Instant.now(), leaf.getNotAfter().toInstant()));
        }
        wamService.commit(builder.build());
    }

    /**
     * Returns the common name (CN) of a certificate's subject, or {@code null} when absent.
     *
     * @param certificate the certificate
     * @return the subject CN, or {@code null}
     */
    private String commonName(X509Certificate certificate) {
        var name = certificate.getSubjectX500Principal().getName();
        for (var part : name.split(",")) {
            var trimmed = part.trim();
            if (trimmed.startsWith("CN=")) {
                return trimmed.substring(3);
            }
        }
        return null;
    }
}
