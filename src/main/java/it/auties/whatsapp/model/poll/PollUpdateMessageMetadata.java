package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

/**
 * A model class that represents additional metadata about a
 * {@link it.auties.whatsapp.model.message.standard.PollUpdateMessage} Currently empty
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollUpdateMessageMetadata")
public class PollUpdateMessageMetadata implements ProtobufMessage {

}
