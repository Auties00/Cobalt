package it.auties.whatsapp.model.sync;

import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;

import static it.auties.protobuf.model.ProtobufType.INT32;

public record KeyExpiration(@ProtobufProperty(index = 1, type = INT32) Integer expiredKeyEpoch) implements ProtobufMessage {
}
