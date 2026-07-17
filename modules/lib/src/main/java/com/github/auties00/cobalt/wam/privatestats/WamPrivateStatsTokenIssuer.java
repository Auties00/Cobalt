package com.github.auties00.cobalt.wam.privatestats;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppPrivateStatsTokenIssuerException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wam.WamService;
import com.github.auties00.cobalt.wire.wam.event.SignCredentialEvent;
import com.github.auties00.cobalt.wire.wam.event.SignCredentialEventBuilder;
import com.github.auties00.cobalt.wire.wam.type.ApplicationState;
import com.github.auties00.cobalt.wire.wam.type.SignCredentialResult;

import java.lang.System.Logger.Level;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Issues a fresh {@link WamPrivateStatsToken} per call by performing one {@code sign_credential} IQ round-trip
 * against {@code s.whatsapp.net}.
 *
 * <p>This collapses three WA Web concerns into a single class:
 *
 * <ul>
 *   <li>the {@code getBlindedToken} export, which generates the random token, the random blinding factor, and
 *       computes {@code blindedToken = blind(token, blindingFactor)};</li>
 *   <li>the {@code getToken} export, which orchestrates the IQ exchange behind a semaphore and a WAM-event
 *       emitter, and further delegates to {@code WAWebRedeemACSToken.redeemACSToken} for caching, retry, and
 *       project-aware token reuse;</li>
 *   <li>the {@code getSharedSecret} export, which derives {@code SHA-512(token || unblindedSignedToken)} as the
 *       upload authentication key.</li>
 * </ul>
 *
 * <p>One issuer is used per {@link LinkedWhatsAppClient}; each {@link #issue()} call performs a fresh round-trip and
 * returns a single-use token. The token must not be reused across uploads; VOPRF unlinkability rests on each
 * scalar being used at most once.
 *
 * @implNote
 * This implementation diverges from WA Web in three ways. (1) It does not cache tokens; WA Web batches a
 * project-keyed pool of tokens via {@code WAWebCRUDOperationsACSTokens}, redeeming them lazily. (2) It uses the
 * older {@code version="1"} request shape without the {@code project_name} child; the current
 * {@code WASmaxOutPrivatestatsSignCredentialRequest} module emits {@code version="2"} with a
 * {@code project_name} element. (3) The {@code dleq_proof} block carried in the response is not verified; a
 * fully trust-minimised implementation would Chaum-Pedersen-check that the server used the same secret key for
 * {@code signedCredential = sk * blindedToken} and for {@code acsPublicKey = sk * B}.
 */
@WhatsAppWebModule(moduleName = "WAWebIssuePrivateStatsToken")
@WhatsAppWebModule(moduleName = "WAACSTokenUtils")
@WhatsAppWebModule(moduleName = "WAWebSignCredentialWamEvent")
public final class WamPrivateStatsTokenIssuer {
    /**
     * The logger for {@link WamPrivateStatsTokenIssuer}.
     */
    private static final System.Logger LOGGER = Log.get(WamPrivateStatsTokenIssuer.class);

    /**
     * The XMPP namespace under which the private-stats issuance IQ is routed.
     *
     * @implNote
     * This implementation matches the {@code xmlns="privatestats"} attribute emitted by the
     * {@code WASmaxOutPrivatestatsSignCredentialRequest} module.
     */
    private static final String XMLNS = "privatestats";

    /**
     * The server JID accepting the {@code sign_credential} IQ.
     *
     * @implNote
     * This implementation matches the {@code WAWap.S_WHATSAPP_NET} target used by the
     * {@code WASmaxOutPrivatestatsSignCredentialRequest} module.
     */
    private static final String SERVER = "s.whatsapp.net";

    /**
     * The WhatsApp client used to dispatch the IQ.
     */
    private final LinkedWhatsAppClient client;

    /**
     * The WAM service that receives the {@link SignCredentialEvent} committed on every {@link #issue()}
     * attempt, or {@code null} when the issuer was built without a telemetry sink.
     *
     * <p>Injected via the two-argument production constructor; the older single-argument and the test
     * constructors leave it {@code null}, in which case {@link #emitSignCredential} is a no-op.
     */
    private final WamService wamService;

    /**
     * The cryptographic random source used to generate the token nonce and the blinding factor.
     *
     * <p>Supplied via the package-private constructor so behavioural tests can pin issuance to the captured
     * live-bundle vectors.
     */
    private final SecureRandom random;

    /**
     * Constructs a new issuer bound to the given client and a default-provider {@link SecureRandom}, with no
     * WAM telemetry sink.
     *
     * <p>Retained for backward compatibility; issuance performed through an issuer built this way does not
     * commit the {@link SignCredentialEvent} telemetry. Production code should use
     * {@link #WamPrivateStatsTokenIssuer(LinkedWhatsAppClient, WamService)} so each attempt is reported.
     *
     * @param client the {@link LinkedWhatsAppClient} used to dispatch the IQ
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public WamPrivateStatsTokenIssuer(LinkedWhatsAppClient client) {
        this(client, null, new SecureRandom());
    }

    /**
     * Constructs a new issuer bound to the given client and WAM service, with a default-provider
     * {@link SecureRandom}.
     *
     * <p>This is the production constructor: each {@link #issue()} attempt commits one
     * {@link SignCredentialEvent} through the supplied {@link WamService}.
     *
     * @param client     the {@link LinkedWhatsAppClient} used to dispatch the IQ
     * @param wamService the WAM service that receives the per-attempt {@link SignCredentialEvent}, or
     *                   {@code null} to disable telemetry
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public WamPrivateStatsTokenIssuer(LinkedWhatsAppClient client, WamService wamService) {
        this(client, wamService, new SecureRandom());
    }

    /**
     * Constructs a new issuer bound to the given client and an explicit {@link SecureRandom} source, with no
     * WAM telemetry sink.
     *
     * <p>Intended for behavioural tests that script the random source so the produced blinded credential and
     * unblinded token can be checked against captured live-bundle known-answer vectors.
     *
     * @param client the {@link LinkedWhatsAppClient} used to dispatch the IQ
     * @param random the random source for the token and the blinding factor
     * @throws NullPointerException if {@code client} or {@code random} is {@code null}
     */
    WamPrivateStatsTokenIssuer(LinkedWhatsAppClient client, SecureRandom random) {
        this(client, null, random);
    }

    /**
     * Constructs a new issuer bound to the given client, WAM service, and explicit {@link SecureRandom} source.
     *
     * <p>This is the canonical constructor all other constructors delegate to.
     *
     * @param client     the {@link LinkedWhatsAppClient} used to dispatch the IQ
     * @param wamService the WAM service that receives the per-attempt {@link SignCredentialEvent}, or
     *                   {@code null} to disable telemetry
     * @param random     the random source for the token and the blinding factor
     * @throws NullPointerException if {@code client} or {@code random} is {@code null}
     */
    WamPrivateStatsTokenIssuer(LinkedWhatsAppClient client, WamService wamService, SecureRandom random) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.wamService = wamService;
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * Performs one {@code sign_credential} round-trip and returns the resulting {@link WamPrivateStatsToken}.
     *
     * <p>Returns a single-use token; the caller is responsible for pairing it with exactly one
     * {@link WamPrivateStatsUploader#upload(byte[])} invocation. Reusing the same token across uploads breaks
     * the VOPRF unlinkability that motivates the protocol. The IQ shape is:
     * {@snippet :
     *     <iq xmlns="privatestats" type="get" to="s.whatsapp.net">
     *       <sign_credential version="1">
     *         <blinded_credential>BLINDED_BYTES</blinded_credential>
     *       </sign_credential>
     *     </iq>
     * }
     *
     * <p>Every attempt commits one {@link SignCredentialEvent} (WAM id {@code 2242}) on the
     * {@link com.github.auties00.cobalt.wam.model.WamChannel#REGULAR} channel through the injected
     * {@link WamService}, reporting the mapped {@link SignCredentialResult}, the overall and per-attempt
     * timers, a retry count of {@code 0} (this issuer performs a single attempt with no retry loop), and
     * whether the client reached chatd. The telemetry is skipped when no {@link WamService} was injected.
     *
     * @implNote
     * This implementation inlines the three WA Web responsibilities ({@code getBlindedToken}, {@code getToken},
     * {@code getSharedSecret}) into one call:
     * <ul>
     *   <li>draws two 32-byte sequences from {@link #random},</li>
     *   <li>blinds via {@link WamPrivateStatsTokenBlinder#blind(byte[], byte[])},</li>
     *   <li>dispatches the IQ via {@link LinkedWhatsAppClient#sendNode(StanzaBuilder)},</li>
     *   <li>parses {@code signed_credential} and {@code acs_public_key} under the {@code sign_credential}
     *       reply,</li>
     *   <li>unblinds via {@link WamPrivateStatsTokenBlinder#unblind(byte[], byte[], byte[])},</li>
     *   <li>computes {@code SHA-512(token || unblindedSignedToken)}.</li>
     * </ul>
     *
     * @return the freshly issued token
     * @throws WhatsAppPrivateStatsTokenIssuerException if the server returns a non-result IQ, if the response is
     *         missing either the signed credential or the ACS public key, if either has the wrong byte length,
     *         or if the unblinding fails to decode the signed credential
     * @see WamPrivateStatsTokenBlinder#blind(byte[], byte[])
     * @see WamPrivateStatsTokenBlinder#unblind(byte[], byte[], byte[])
     */
    @WhatsAppWebExport(
            moduleName = "WAWebIssuePrivateStatsToken",
            exports = "getToken",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    @WhatsAppWebExport(
            moduleName = "WAACSTokenUtils",
            exports = {"getBlindedToken", "getSharedSecret"},
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    public WamPrivateStatsToken issue() {
        var telemetry = new SignCredentialEventBuilder()
                .startOverallT()
                .startSignCredentialT();

        var token = new byte[WamPrivateStatsToken.TOKEN_BYTES];
        random.nextBytes(token);
        var blindingFactor = new byte[WamPrivateStatsToken.TOKEN_BYTES];
        random.nextBytes(blindingFactor);

        var blindedCredential = WamPrivateStatsTokenBlinder.blind(token, blindingFactor);

        var iq = new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", XMLNS)
                .attribute("type", "get")
                .attribute("to", SERVER)
                .content(new StanzaBuilder()
                        .description("sign_credential")
                        .attribute("version", "1")
                        .content(new StanzaBuilder()
                                .description("blinded_credential")
                                .content(blindedCredential)
                                .build())
                        .build());

        Stanza response;
        try {
            response = client.sendNode(iq);
        } catch (RuntimeException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sign_credential iq failed", e);
            emitSignCredential(telemetry, SignCredentialResult.ERROR_CLIENT_NETWORK, false);
            throw e;
        }

        if (!"result".equals(response.getAttributeAsString("type", ""))) {
            var errorResult = classifyErrorResult(response);
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sign_credential iq rejected result={0}", errorResult);
            emitSignCredential(telemetry, errorResult, true);
            throw new WhatsAppPrivateStatsTokenIssuerException(
                    "sign_credential IQ failed: " + response);
        }

        try {
            var signCredential = response.getRequiredChild("sign_credential");
            var signedCredential = signCredential.getRequiredChild("signed_credential")
                    .toContentBytes()
                    .orElseThrow(() -> new WhatsAppPrivateStatsTokenIssuerException(
                            "missing signed_credential bytes in response"));
            var acsPublicKey = signCredential.getRequiredChild("acs_public_key")
                    .toContentBytes()
                    .orElseThrow(() -> new WhatsAppPrivateStatsTokenIssuerException(
                            "missing acs_public_key bytes in response"));

            if (signedCredential.length != WamPrivateStatsToken.TOKEN_BYTES) {
                throw new WhatsAppPrivateStatsTokenIssuerException(
                        "signed_credential length " + signedCredential.length
                        + " != " + WamPrivateStatsToken.TOKEN_BYTES);
            }
            if (acsPublicKey.length != WamPrivateStatsToken.TOKEN_BYTES) {
                throw new WhatsAppPrivateStatsTokenIssuerException(
                        "acs_public_key length " + acsPublicKey.length
                        + " != " + WamPrivateStatsToken.TOKEN_BYTES);
            }

            byte[] unblindedSignedToken;
            try {
                unblindedSignedToken =
                        WamPrivateStatsTokenBlinder.unblind(signedCredential, blindingFactor, acsPublicKey);
            } catch (IllegalArgumentException e) {
                throw new WhatsAppPrivateStatsTokenIssuerException(
                        "failed to unblind signed credential", e);
            }

            var sharedSecret = deriveSharedSecret(token, unblindedSignedToken);
            emitSignCredential(telemetry, SignCredentialResult.SUCCESS, true);
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "sign_credential token issued");
            return new WamPrivateStatsToken(token, sharedSecret);
        } catch (WhatsAppPrivateStatsTokenIssuerException e) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "sign_credential token issuance failed", e);
            emitSignCredential(telemetry, SignCredentialResult.ERROR_OTHER, true);
            throw e;
        }
    }

    /**
     * Commits one {@link SignCredentialEvent} describing the outcome of an {@link #issue()} attempt.
     *
     * <p>Stops the overall and per-attempt timers carried on the supplied builder, then sets the mapped
     * result, a fixed retry count of {@code 0} (this issuer performs a single attempt with no retry loop), the
     * connectivity flag, and a {@link ApplicationState#FOREGROUND} application state, and hands the built event
     * to {@link WamService#commit(com.github.auties00.cobalt.wam.model.WamEventSpec)}. It is a no-op when no
     * {@link WamService} was injected (the single-argument and the test constructors).
     *
     * @implNote
     * This implementation mirrors the internal emitter of WA Web's {@code WAWebIssuePrivateStatsToken}, which
     * builds a {@code SignCredentialWamEvent} carrying {@code overallT}, {@code signCredentialT},
     * {@code retryCount}, {@code waConnectedToChatd}, and {@code applicationState} on every {@code getToken}
     * attempt. Cobalt reports {@link ApplicationState#FOREGROUND} unconditionally because a headless client has
     * no page-visibility state to consult and is by definition active while acquiring a token, and it leaves
     * {@code isFromWameta} and {@code projectCode} unset because the WA emitter does not populate them at this
     * call site.
     *
     * @param telemetry          the event builder whose timers were started at the top of {@link #issue()}
     * @param result             the mapped sign-credential outcome
     * @param waConnectedToChatd whether the client reached chatd during the attempt
     */
    @WhatsAppWebExport(
            moduleName = "WAWebSignCredentialWamEvent",
            exports = "SignCredentialWamEvent",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    private void emitSignCredential(SignCredentialEventBuilder telemetry,
                                    SignCredentialResult result,
                                    boolean waConnectedToChatd) {
        if (wamService == null) {
            return;
        }
        wamService.commit(telemetry
                .stopOverallT()
                .stopSignCredentialT()
                .signCredentialResult(result)
                .retryCount(0)
                .waConnectedToChatd(waConnectedToChatd)
                .applicationState(ApplicationState.FOREGROUND)
                .build());
    }

    /**
     * Maps a non-result {@code sign_credential} IQ reply to a {@link SignCredentialResult}.
     *
     * <p>Reads the {@code code} attribute of the reply's {@code error} child and translates the standard
     * status codes into the WAM result enum: {@code 400} to {@link SignCredentialResult#ERROR_BAD_REQUEST},
     * {@code 500} to {@link SignCredentialResult#ERROR_SERVER}, and any other or absent code to
     * {@link SignCredentialResult#ERROR_OTHER}.
     *
     * @implNote
     * This implementation mirrors the {@code u} classifier in WA Web's {@code WAWebIssuePrivateStatsToken},
     * which folds {@code bad-request} to {@code ERROR_BAD_REQUEST}, {@code internal-server-error} to
     * {@code ERROR_SERVER}, and {@code feature-not-implemented}, {@code service-unavailable},
     * {@code decryption-error}, and {@code unknown} to {@code ERROR_OTHER}.
     *
     * @param response the non-result IQ reply
     * @return the mapped result
     */
    private static SignCredentialResult classifyErrorResult(Stanza response) {
        var code = response.getChild("error")
                .flatMap(error -> error.getAttributeAsString("code"))
                .orElse("");
        return switch (code) {
            case "400" -> SignCredentialResult.ERROR_BAD_REQUEST;
            case "500" -> SignCredentialResult.ERROR_SERVER;
            default -> SignCredentialResult.ERROR_OTHER;
        };
    }

    /**
     * Derives the upload authentication key as {@code SHA-512(token || unblindedSignedToken)}.
     *
     * @implNote
     * This implementation isolates the SHA-512 digest so the {@link NoSuchAlgorithmException} branch lives with
     * its only call site; an unavailable SHA-512 provider is fatal per JCE conformance and surfaces as an
     * {@link AssertionError}.
     *
     * @param token                the random token nonce
     * @param unblindedSignedToken the unblinded signed credential returned by the server
     * @return the derived shared secret
     */
    @WhatsAppWebExport(
            moduleName = "WAACSTokenUtils",
            exports = "getSharedSecret",
            adaptation = WhatsAppAdaptation.ADAPTED
    )
    private static byte[] deriveSharedSecret(byte[] token, byte[] unblindedSignedToken) {
        try {
            var md = MessageDigest.getInstance("SHA-512");
            md.update(token);
            md.update(unblindedSignedToken);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-512 must be available on every JVM", e);
        }
    }
}
