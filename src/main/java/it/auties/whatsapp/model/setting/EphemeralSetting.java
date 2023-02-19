package it.auties.whatsapp.model.setting;

import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.SFIXED32;
import static it.auties.protobuf.base.ProtobufType.SFIXED64;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class EphemeralSetting implements Setting {
    @ProtobufProperty(index = 1, type = SFIXED32)
    private int duration;

    @ProtobufProperty(index = 2, type = SFIXED64)
    private long timestamp;

    @Override
    public String indexName() {
        throw new UnsupportedOperationException("Cannot send setting: no index name");
    }
}
