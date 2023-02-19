package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;
import static it.auties.protobuf.base.ProtobufType.UINT64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("StatusPSA")
public class PublicServiceAnnouncementStatus implements ProtobufMessage {
    @ProtobufProperty(index = 44, type = STRING)
    @NonNull
    private String campaignId;

    @ProtobufProperty(index = 45, type = UINT64)
    private long campaignExpirationTimestamp;
}