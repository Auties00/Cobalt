package it.auties.whatsapp.model._generated;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Jacksonized
@Builder
@ProtobufName("SenderKeyRecordStructure")
public class SenderKeyRecordStructure implements ProtobufMessage {
    @ProtobufProperty(implementation = SenderKeyStateStructure.class, index = 1, name = "senderKeyStates", repeated = true, type = MESSAGE)
    private List<SenderKeyStateStructure> senderKeyStates;
}