package it.auties.whatsapp.model.info;

import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.sync.DeviceListMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.INT32;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class MessageContextInfo implements Info {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = DeviceListMetadata.class)
    private DeviceListMetadata deviceListMetadata;

    @ProtobufProperty(index = 2, type = INT32)
    private int deviceListMetadataVersion;
}
