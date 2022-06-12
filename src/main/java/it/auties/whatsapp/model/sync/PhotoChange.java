package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BYTES;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.UINT32;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class PhotoChange implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] oldPhoto;

    @ProtobufProperty(index = 2, type = BYTES)
    private byte[] newPhoto;

    @ProtobufProperty(index = 3, type = UINT32)
    private Integer newPhotoId;
}
