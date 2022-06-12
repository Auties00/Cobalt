package it.auties.whatsapp.model.button;

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
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ButtonSection implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    @ProtobufProperty(index = 2, type = MESSAGE, concreteType = ButtonRow.class, repeated = true)
    private List<ButtonRow> rows;

    public static class ButtonSectionBuilder {
        public ButtonSectionBuilder hydratedButtons(List<ButtonRow> rows) {
            if (this.rows == null)
                this.rows = new ArrayList<>();
            this.rows.addAll(rows);
            return this;
        }
    }
}
