package com.github.auties00.cobalt.wire.linked.business.ai;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One website a WhatsApp Business AI agent reads as a knowledge source.
 *
 * <p>The merchant's auto-reply assistant can ingest the contents of a
 * website (a storefront page, a published document, and similar) so it can
 * answer customer questions from that material. This model identifies one
 * such website by its address and its kind.
 */
@ProtobufMessage(name = "BusinessAiWebsite")
public final class BusinessAiWebsite {
    /**
     * Server-defined marker classifying the kind of website (for example a
     * generic web page versus a published spreadsheet). The full value set
     * is not recoverable from the WhatsApp client, so the raw marker is
     * exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String type;

    /**
     * Address of the website the assistant ingests. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String url;

    /**
     * Constructs a new {@code BusinessAiWebsite}. Either argument may be
     * {@code null} when the server omitted the corresponding field.
     *
     * @param type the website-kind marker, or {@code null}
     * @param url  the website address, or {@code null}
     */
    BusinessAiWebsite(String type, String url) {
        this.type = type;
        this.url = url;
    }

    /**
     * Returns the marker classifying the kind of website.
     *
     * @return the website-kind marker, or empty when the server omitted it
     */
    public Optional<String> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the address of the website the assistant ingests.
     *
     * @return the website address, or empty when the server omitted it
     */
    public Optional<String> url() {
        return Optional.ofNullable(url);
    }
}
