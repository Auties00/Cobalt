package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor(staticName = "of")
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("Option")
public class PollOptionName
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, name = "optionName", type = ProtobufType.STRING)
    private String name;
}
