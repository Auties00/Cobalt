package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.BYTES;


/**
 * A model class that represents the cypher data to decode a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage}
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollEncValue")
public class PollUpdateEncryptedMetadata implements ProtobufMessage {
    /**
     * The bytes of the payload, decoded internally by the message handler
     */
    @ProtobufProperty(index = 1, name = "encPayload", type = BYTES)
    private byte[] payload;

    /**
     * The bytes of the iv, used to decode the payload internally in the message handler
     */
    @ProtobufProperty(index = 2, name = "encIv", type = BYTES)
    private byte[] iv;
}
