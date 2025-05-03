package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;

/**
 * A model class that represents a section of buttons
 */
@ProtobufMessage(name = "Message.ListMessage.Section")
public final class ButtonSection {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final List<ButtonRow> rows;

    ButtonSection(String title, List<ButtonRow> rows) {
        this.title = Objects.requireNonNull(title, "title cannot be null");
        this.rows = Objects.requireNonNullElse(rows, List.of());
    }

    public String title() {
        return title;
    }

    public List<ButtonRow> rows() {
        return rows;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonSection that
                && Objects.equals(title, that.title)
                && Objects.equals(rows, that.rows);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, rows);
    }

    @Override
    public String toString() {
        return "ButtonSection[" +
                "title=" + title + ", " +
                "rows=" + rows + ']';
    }
}