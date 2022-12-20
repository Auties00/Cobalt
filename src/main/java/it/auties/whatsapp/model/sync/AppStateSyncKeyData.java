package it.auties.whatsapp.model.sync;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateSyncKeyData
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = BYTES)
    private byte[] keyData;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = AppStateSyncKeyFingerprint.class)
    private AppStateSyncKeyFingerprint fingerprint;

    @ProtobufProperty(index = 3, type = INT64)
    private Long timestamp;
}
