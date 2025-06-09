package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Bytes;

import java.util.HexFormat;
import java.util.Objects;

/**
 * A model class that represents a row of buttons
 */
@ProtobufMessage(name = "Message.ListMessage.Row")
public final class ButtonRow {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    ButtonRow(String title, String description, String id) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public static ButtonRow of(String title, String description) {
        return new ButtonRow(title, description, HexFormat.of().formatHex(Bytes.random(5)));
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonRow that
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, id);
    }

    @Override
    public String toString() {
        return "ButtonRow[" +
                "title=" + title + ", " +
                "description=" + description + ", " +
                "id=" + id + ']';
    }
}