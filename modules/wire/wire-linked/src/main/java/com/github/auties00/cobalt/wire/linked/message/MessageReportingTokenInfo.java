package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata that lets the WhatsApp server identify a message if it is
 * later reported for abuse.
 *
 * <p>When a user reports a suspicious or abusive message, the client
 * forwards the original message together with a server-issued
 * reporting tag. The tag is cryptographically bound to the message and
 * its sender and allows the server to verify that the reported
 * content actually belongs to the party being accused, without
 * revealing the message content to anyone other than the server.
 *
 * <p>This metadata travels alongside regular messages as part of the
 * message envelope and is normally invisible to the end user.
 */
@ProtobufMessage(name = "ReportingTokenInfo")
public final class MessageReportingTokenInfo {
    /**
     * Opaque byte sequence issued by the server that identifies this
     * message for reporting purposes.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] reportingTag;


    /**
     * Constructs a new {@code MessageReportingTokenInfo}.
     *
     * <p>The constructor is package-private; use
     * {@code MessageReportingTokenInfoBuilder} to instantiate new values.
     *
     * @param reportingTag the opaque reporting tag, or {@code null}
     */
    MessageReportingTokenInfo(byte[] reportingTag) {
        this.reportingTag = reportingTag;
    }

    /**
     * Returns the opaque reporting tag associated with the message,
     * if any.
     *
     * @return an {@link Optional} holding the reporting tag bytes, or
     *         empty if none was set
     */
    public Optional<byte[]> reportingTag() {
        return Optional.ofNullable(reportingTag);
    }

    /**
     * Updates the reporting tag.
     *
     * @param reportingTag the new reporting tag bytes, or {@code null}
     *                     to clear
     */
    public void setReportingTag(byte[] reportingTag) {
        this.reportingTag = reportingTag;
    }
}
