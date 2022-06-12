package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateSyncKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = AppStateSyncKeyId.class)
    private AppStateSyncKeyId keyId;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = AppStateSyncKeyData.class)
    private AppStateSyncKeyData keyData;
}
