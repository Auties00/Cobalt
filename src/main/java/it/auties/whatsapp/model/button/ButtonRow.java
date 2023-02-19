package it.auties.whatsapp.model.button;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a row of buttons
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class ButtonRow implements ProtobufMessage {
    /**
     * The title of the row
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The description of the row
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String description;

    /**
     * The id of the row
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String id;

    /**
     * Constructs a new row with a random id
     *
     * @param title       the title
     * @param description the description
     * @return a non-null row
     */
    public static ButtonRow of(String title, String description) {
        return of(title, description, Bytes.ofRandom(5).toHex());
    }
}
