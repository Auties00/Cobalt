package it.auties.whatsapp.model.signal.auth;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class WebPayload implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BOOLEAN)
    private boolean usesParticipantInKey;

    @ProtobufProperty(index = 2, type = BOOLEAN)
    private boolean supportsStarredMessages;

    @ProtobufProperty(index = 3, type = BOOLEAN)
    private boolean supportsDocumentMessages;

    @ProtobufProperty(index = 4, type = BOOLEAN)
    private boolean supportsUrlMessages;

    @ProtobufProperty(index = 5, type = BOOLEAN)
    private boolean supportsMediaRetry;

    @ProtobufProperty(index = 6, type = BOOLEAN)
    private boolean supportsE2EImage;

    @ProtobufProperty(index = 7, type = BOOLEAN)
    private boolean supportsE2EVideo;

    @ProtobufProperty(index = 8, type = BOOLEAN)
    private boolean supportsE2EAudio;

    @ProtobufProperty(index = 9, type = BOOLEAN)
    private boolean supportsE2EDocument;

    @ProtobufProperty(index = 10, type = STRING)
    private String documentTypes;

    @ProtobufProperty(index = 11, type = BYTES)
    private byte[] features;
}
