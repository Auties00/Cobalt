package com.github.auties00.cobalt.message.send.token;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import javax.crypto.KDF;
import javax.crypto.Mac;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Generates the franking-style reporting token (RT) attached to outgoing
 * messages.
 *
 * <p>The token is computed once per outgoing reportable message and wrapped into
 * the {@code <reporting>} child element of the outbound {@code <message>}. It
 * lets the server verify a later abuse report without ever seeing the plaintext:
 * the report submission replays the same HMAC over the recovered plaintext and
 * compares it against the token. The default send pipeline attaches the token
 * before the stanza leaves the client; embedders that build their own stanzas
 * call {@link #generate} directly.
 */
@WhatsAppWebModule(moduleName = "WAWebReportingTokenUtils")
public final class ReportingToken {
    /**
     * The logger for {@link ReportingToken}.
     */
    private static final System.Logger LOGGER = Log.get(ReportingToken.class);

    /**
     * The HKDF-derived reporting-token key length, in bytes.
     *
     * <p>The HKDF output of this length drives the {@link Mac} key used to
     * compute the HMAC.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "REPORTING_TOKEN_KEY_SIZE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int KEY_LENGTH = 32;

    /**
     * The number of leading HMAC bytes kept as the reporting token, in bytes.
     *
     * <p>This is the size of the bytes carried by
     * {@link ReportingTokenResult#token()} and the wire length of every
     * {@code <reporting_token>} value.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "REPORTING_TOKEN_SIZE",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final int TOKEN_LENGTH = 16;

    /**
     * The storage size, in bytes, of a valid reporting-token entry.
     *
     * <p>The receive-side validation cache uses this size for each entry it
     * persists for later report submission.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "REPORTING_TOKEN_STORAGE_SIZE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int REPORTING_TOKEN_STORAGE_SIZE = 6;

    /**
     * The storage size, in bytes, used to flag an invalid reporting-token entry.
     *
     * <p>The receive-side validator persists this sentinel size when a token
     * fails validation so re-validation on a subsequent decrypt does not
     * silently turn a known-bad entry into a known-good one.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "REPORTING_TOKEN_INVALID_STORAGE_SIZE",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final int REPORTING_TOKEN_INVALID_STORAGE_SIZE = 7;

    /**
     * The HKDF algorithm used for key derivation.
     */
    private static final String HKDF_ALGORITHM = "HKDF-SHA256";

    /**
     * The HMAC algorithm used for token computation.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * The use-case secret modification type appended to the HKDF info parameter
     * when deriving the reporting-token key.
     */
    private static final String USE_CASE_TYPE = "Report Token";

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private ReportingToken() {
        throw new AssertionError();
    }

    /**
     * Generates the reporting token for an outgoing message.
     *
     * <p>Derives the HMAC key from {@code messageSecret} via {@link #deriveKey}
     * and HMACs the supplied franking content (a sparse copy of the serialised
     * protobuf, produced by {@link ReportingTokenContent#compute(byte[], int)}).
     * When {@code reportingTokenContent} is {@code null} or empty the message
     * type is not reporting-token-compatible and {@link Optional#empty()} is
     * returned so the caller knows to omit the {@code <reporting>} element
     * entirely.
     *
     * @implNote
     * This implementation receives the franking content already computed by the
     * caller; WA Web computes it inline. Splitting the two lets Cobalt cache the
     * content across multi-recipient fanouts where the token key changes per
     * recipient but the content does not.
     *
     * @param messageSecret         the 32-byte message secret
     * @param stanzaId              the message's stanza id
     * @param senderJid             the sender's user {@link Jid}
     * @param remoteJid             the remote {@link Jid} (recipient for 1:1,
     *                              self JID for groups and broadcasts)
     * @param reportingTokenContent the deterministic franking content extracted
     *                              from the serialised protobuf, or {@code null}
     *                              for incompatible message types
     * @param version               the reporting-token version
     * @return the reporting token result, or {@link Optional#empty()} when
     *         {@code reportingTokenContent} is {@code null} or empty
     * @throws NullPointerException     if any required argument is {@code null}
     * @throws GeneralSecurityException if a cryptographic primitive is
     *                                  unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "genReportingToken",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Optional<ReportingTokenResult> generate(
            byte[] messageSecret,
            String stanzaId,
            Jid senderJid,
            Jid remoteJid,
            byte[] reportingTokenContent,
            int version
    ) throws GeneralSecurityException {
        Objects.requireNonNull(messageSecret, "messageSecret");
        Objects.requireNonNull(stanzaId, "stanzaId");
        Objects.requireNonNull(senderJid, "senderJid");
        Objects.requireNonNull(remoteJid, "remoteJid");

        if (reportingTokenContent == null || reportingTokenContent.length == 0) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reporting token generation skipped: no franking content");
            return Optional.empty();
        }

        var key = deriveKey(messageSecret, stanzaId, senderJid, remoteJid);

        var token = hmacTruncated(key, reportingTokenContent);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "reporting token generated for message {0} version={1}", stanzaId, version);
        return Optional.of(new ReportingTokenResult(version, token));
    }

    /**
     * Derives the {@value #KEY_LENGTH}-byte reporting-token key from the message
     * secret via HKDF-SHA-256 extract-and-expand.
     *
     * <p>The {@code messageSecret} is the input keying material, the extract
     * salt is implicit-zero (the JCA default for a {@code null} salt), and the
     * expand info parameter is the UTF-8 encoding of
     * {@code stanzaId || senderJid || remoteJid || "Report Token"}. The
     * resulting key is the HMAC key in {@link #generate}, so any divergence in
     * this info layout corrupts every token produced.
     *
     * @param messageSecret the 32-byte message secret used as input keying
     *                      material
     * @param stanzaId      the message's stanza id
     * @param senderJid     the sender's user {@link Jid}
     * @param remoteJid     the remote {@link Jid}
     * @return a {@value #KEY_LENGTH}-byte key
     * @throws GeneralSecurityException if HKDF-SHA-256 is unavailable
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "genReportingTokenKeyFromMessageSecret",
            adaptation = WhatsAppAdaptation.DIRECT)
    static byte[] deriveKey(
            byte[] messageSecret,
            String stanzaId,
            Jid senderJid,
            Jid remoteJid
    ) throws GeneralSecurityException {
        var info = (stanzaId + senderJid + remoteJid + USE_CASE_TYPE)
                .getBytes(StandardCharsets.UTF_8);

        var kdf = KDF.getInstance(HKDF_ALGORITHM);
        var params = HKDFParameterSpec.ofExtract()
                .addIKM(messageSecret)
                .thenExpand(info, KEY_LENGTH);
        return kdf.deriveData(params);
    }

    /**
     * Computes HMAC-SHA-256 of {@code data} keyed by {@code key}, truncated to
     * the first {@value #TOKEN_LENGTH} bytes.
     *
     * @param key  the HMAC key
     * @param data the data to authenticate
     * @return the first {@value #TOKEN_LENGTH} bytes of the HMAC
     * @throws GeneralSecurityException if HMAC computation fails
     */
    private static byte[] hmacTruncated(byte[] key, byte[] data) throws GeneralSecurityException {
        var mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        var full = mac.doFinal(data);
        return Arrays.copyOf(full, TOKEN_LENGTH);
    }
}
