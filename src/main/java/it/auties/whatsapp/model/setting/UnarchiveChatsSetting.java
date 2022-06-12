package it.auties.whatsapp.model.setting;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.BOOLEAN;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class UnarchiveChatsSetting implements Setting {
    @ProtobufProperty(index = 1, type = BOOLEAN)
    private boolean unarchiveChats;

    @Override
    public String indexName() {
        return "setting_unarchiveChats";
    }
}
