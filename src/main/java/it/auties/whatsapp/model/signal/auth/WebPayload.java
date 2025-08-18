package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;

import static it.auties.protobuf.model.ProtobufType.*;


@ProtobufMessage(name = "ClientPayload.WebInfo.WebdPayload")
public record WebPayload(@ProtobufProperty(index = 1, type = BOOL) boolean usesParticipantInKey,
                         @ProtobufProperty(index = 2, type = BOOL) boolean supportsStarredMessages,
                         @ProtobufProperty(index = 3, type = BOOL) boolean supportsDocumentMessages,
                         @ProtobufProperty(index = 4, type = BOOL) boolean supportsUrlMessages,
                         @ProtobufProperty(index = 5, type = BOOL) boolean supportsMediaRetry,
                         @ProtobufProperty(index = 6, type = BOOL) boolean supportsE2EImage,
                         @ProtobufProperty(index = 7, type = BOOL) boolean supportsE2EVideo,
                         @ProtobufProperty(index = 8, type = BOOL) boolean supportsE2EAudio,
                         @ProtobufProperty(index = 9, type = BOOL) boolean supportsE2EDocument,
                         @ProtobufProperty(index = 10, type = STRING) String documentTypes,
                         @ProtobufProperty(index = 11, type = BYTES) byte[] features) {
}
