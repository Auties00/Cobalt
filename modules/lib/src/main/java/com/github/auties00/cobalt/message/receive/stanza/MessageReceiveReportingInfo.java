package com.github.auties00.cobalt.message.receive.stanza;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * The reporting-token metadata extracted from the {@code <reporting>} child of
 * an incoming {@code <message>} stanza.
 *
 * <p>The reporting token is the cryptographic receipt that lets the WhatsApp
 * abuse-reporting pipeline prove that a reported message was actually received
 * by the reporter, without forcing the reporter to upload the plaintext at
 * report time. Cobalt stores {@link #reportingToken()}, {@link #reportingTag()},
 * {@link #version()}, and the stanza {@link #stanzaTs()} alongside the message
 * so they remain available even if the user reports the message hours or days
 * later. Both byte fields can be missing; a server-side feature gate decides
 * whether the server attaches them at all. Instances are produced by
 * {@link MessageReceiveStanzaParser} and consumed via
 * {@link MessageReceiveStanza#reportingInfo()}.
 */
@WhatsAppWebModule(moduleName = "WAWebHandleMsgParser")
public final class MessageReceiveReportingInfo {
    /**
     * The stanza timestamp captured at the same moment as the reporting token,
     * so a later report can prove the token was bound to this specific message.
     */
    private final Instant stanzaTs;

    /**
     * The raw bytes of the {@code <reporting_token>} child's content region.
     */
    private final byte[] reportingToken;

    /**
     * The {@code v} attribute on the {@code <reporting_token>} child,
     * identifying the token-format version.
     */
    private final int version;

    /**
     * The raw bytes of the {@code <reporting_tag>} child's content region,
     * acting as the integrity tag that authenticates {@link #reportingToken()}.
     */
    private final byte[] reportingTag;

    /**
     * Constructs a populated record from the values extracted by
     * {@link MessageReceiveStanzaParser}.
     *
     * @param stanzaTs       the stanza timestamp
     * @param reportingToken the token bytes, or {@code null} when absent
     * @param version        the token-format version
     * @param reportingTag   the tag bytes, or {@code null} when absent
     * @throws NullPointerException if {@code stanzaTs} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebHandleMsgParser", exports = "incomingMsgParser",
            adaptation = WhatsAppAdaptation.DIRECT)
    public MessageReceiveReportingInfo(
            Instant stanzaTs,
            byte[] reportingToken,
            int version,
            byte[] reportingTag
    ) {
        this.stanzaTs = Objects.requireNonNull(stanzaTs, "stanzaTs cannot be null");
        this.reportingToken = reportingToken;
        this.version = version;
        this.reportingTag = reportingTag;
    }

    /**
     * Returns the stanza timestamp captured with the token.
     *
     * <p>Stored alongside the token so a later report can be matched to this
     * specific delivery rather than to any other instance of the same message
     * id.
     *
     * @return the stanza timestamp
     */
    public Instant stanzaTs() {
        return stanzaTs;
    }

    /**
     * Returns the reporting-token bytes, when present.
     *
     * <p>Forwarded verbatim to the abuse-reporting RPC when the user reports
     * the message; pair with {@link #version()} so the server can pick the
     * matching verification routine.
     *
     * @return an {@link Optional} wrapping the token bytes
     */
    public Optional<byte[]> reportingToken() {
        return Optional.ofNullable(reportingToken);
    }

    /**
     * Returns the token-format version.
     *
     * <p>Parsed from the {@code v} attribute of the {@code <reporting_token>}
     * child; defaults to {@code 0} when the attribute is absent. The server
     * uses this to dispatch the appropriate token-verification routine.
     *
     * @return the version number
     */
    public int version() {
        return version;
    }

    /**
     * Returns the reporting-tag bytes, when present.
     *
     * <p>The integrity tag that authenticates the reporting token; sent
     * alongside the token to the abuse-reporting RPC.
     *
     * @return an {@link Optional} wrapping the tag bytes
     */
    public Optional<byte[]> reportingTag() {
        return Optional.ofNullable(reportingTag);
    }
}
