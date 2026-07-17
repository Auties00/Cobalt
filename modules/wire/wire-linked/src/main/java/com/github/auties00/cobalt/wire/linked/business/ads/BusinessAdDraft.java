package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A work-in-progress WhatsApp Business advertisement that has not yet gone
 * live.
 *
 * <p>Before a merchant publishes a "Click-to-WhatsApp" ad (a paid promotion
 * that opens a chat with the business when tapped), they assemble it as a
 * draft: the ad's text, audience, budget, and duration are collected and saved
 * server-side without spending money. The draft can be edited repeatedly and
 * is published only once the merchant confirms it.
 *
 * <p>This model is the saved draft as the server reports it after a create or
 * edit. The server returns only the draft's identifier, which the merchant
 * uses to edit, publish, or discard the draft; {@link #id()} carries that
 * identifier.
 */
@ProtobufMessage(name = "BusinessAdDraft")
public final class BusinessAdDraft {
    /**
     * Server-issued identifier of this draft. This is the handle used to edit,
     * publish, or discard the draft; it is a numeric advertising identifier,
     * not a WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code BusinessAdDraft}. The {@code id} may be
     * {@code null} when the server omitted it.
     *
     * @param id the draft identifier, or {@code null}
     */
    BusinessAdDraft(String id) {
        this.id = id;
    }

    /**
     * Returns the server-issued identifier of this draft.
     *
     * @return the draft id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
