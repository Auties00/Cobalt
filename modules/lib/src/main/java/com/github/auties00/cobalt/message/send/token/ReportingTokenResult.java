package com.github.auties00.cobalt.message.send.token;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.util.Objects;

/**
 * Pairs a reporting-token version with its 16-byte truncated HMAC tag.
 *
 * <p>Returned by {@link ReportingToken#generate} and consumed by the stanza
 * writer that builds the outbound {@code <reporting>} child: the
 * {@link #version()} is written as the {@code v} attribute on the inner
 * {@code <reporting_token>} element and the {@link #token()} bytes are its
 * value.
 *
 * @see ReportingToken
 */
@WhatsAppWebModule(moduleName = "WAWebReportingTokenUtils")
public final class ReportingTokenResult {
    /**
     * The reporting-token version that derived the HMAC key.
     */
    private final int version;

    /**
     * The 16-byte truncated HMAC tag.
     */
    private final byte[] token;

    /**
     * Constructs a reporting-token result.
     *
     * @param version the reporting-token version
     * @param token   the 16-byte token bytes
     * @throws NullPointerException if {@code token} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "genReportingToken",
            adaptation = WhatsAppAdaptation.DIRECT)
    public ReportingTokenResult(int version, byte[] token) {
        this.version = version;
        this.token = Objects.requireNonNull(token, "token");
    }

    /**
     * Returns the reporting-token version.
     *
     * <p>Maps to the {@code v} attribute on the outbound
     * {@code <reporting_token>} element, from which the server selects its
     * validation behaviour.
     *
     * @return the version
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "genReportingToken",
            adaptation = WhatsAppAdaptation.DIRECT)
    public int version() {
        return version;
    }

    /**
     * Returns the 16-byte truncated HMAC tag.
     *
     * <p>The returned array is the verbatim element value the stanza writer
     * places inside the {@code <reporting_token>} element; callers must not
     * mutate it.
     *
     * @return the 16-byte reporting token
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenUtils", exports = "genReportingToken",
            adaptation = WhatsAppAdaptation.DIRECT)
    public byte[] token() {
        return token;
    }

    /**
     * Returns a string representation of this reporting-token result.
     *
     * @implNote
     * This implementation omits the token bytes and surfaces only the version
     * and the tag length so that logging does not leak the franking tag.
     *
     * @return a string containing the version and the token length
     */
    @Override
    public String toString() {
        return "ReportingTokenResult[version=" + version +
                ", tokenLength=" + token.length + ']';
    }
}
