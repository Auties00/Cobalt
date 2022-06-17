package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PublicServiceAnnouncementStatus implements ProtobufMessage {
    @ProtobufProperty(index = 44, type = STRING)
    @NonNull
    private String campaignId;

    @ProtobufProperty(index = 45, type = UINT64)
    private long campaignExpirationTimestamp;
}
