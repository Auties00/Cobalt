package it.auties.whatsapp.model.sync;

import it.auties.protobuf.api.model.ProtobufMessage;
import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class AppStateSyncKeyShare implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = AppStateSyncKey.class, repeated = true)
    private List<AppStateSyncKey> keys;

    public static class AppStateSyncKeyShareBuilder {
        public AppStateSyncKeyShareBuilder keys(List<AppStateSyncKey> keys) {
            if (this.keys == null)
                this.keys = new ArrayList<>();
            this.keys.addAll(keys);
            return this;
        }
    }
}
