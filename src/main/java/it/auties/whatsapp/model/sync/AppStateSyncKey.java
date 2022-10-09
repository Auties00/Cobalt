package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateSyncKey implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = AppStateSyncKeyId.class)
    private AppStateSyncKeyId keyId;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = AppStateSyncKeyData.class)
    private AppStateSyncKeyData keyData;
}
