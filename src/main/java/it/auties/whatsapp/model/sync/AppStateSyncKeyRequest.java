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
public class AppStateSyncKeyRequest implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, concreteType = AppStateSyncKeyId.class, repeated = true)
    private List<AppStateSyncKeyId> keyIds;

    public static class AppStateSyncKeyRequestBuilder {
        public AppStateSyncKeyRequestBuilder keyIds(List<AppStateSyncKeyId> keyIds) {
            if (this.keyIds == null)
                this.keyIds = new ArrayList<>();
            this.keyIds.addAll(keyIds);
            return this;
        }
    }
}
