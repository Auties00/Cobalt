package com.github.auties00.cobalt.wire.linked.bot.response;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Raw container for the unified response payload sent by the WhatsApp
 * AI bot.
 *
 * <p>The {@linkplain #data() data} field carries a UTF-8 encoded JSON
 * document that the client decodes and renders as a structured response.
 * This format is used when the server opts to send a single unified blob
 * instead of (or in addition to) a list of typed
 * {@link AIRichResponseSubMessage} fragments. It is carried as field
 * index 3 of the parent {@code AIRichResponseMessage} protobuf message.
 */
@ProtobufMessage(name = "AIRichResponseUnifiedResponse")
public final class AIRichResponseUnifiedResponse {
    /**
     * The raw bytes of the unified response, typically a UTF-8
     * encoded JSON document.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] data;


    /**
     * Constructs a new unified response container.
     *
     * @param data the raw bytes of the unified response, or {@code null}
     */
    AIRichResponseUnifiedResponse(byte[] data) {
        this.data = data;
    }

    /**
     * Returns the raw bytes of the unified response payload.
     *
     * <p>The returned byte array is typically a UTF-8 encoded JSON
     * document that can be decoded with
     * {@code new String(data, StandardCharsets.UTF_8)} and then
     * parsed as JSON.
     *
     * @return an {@link Optional} containing the raw bytes, or empty
     *         if no payload is present
     */
    public Optional<byte[]> data() {
        return Optional.ofNullable(data);
    }

    /**
     * Sets the raw bytes of the unified response payload.
     *
     * @param data the raw bytes to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }
}
