package it.auties.whatsapp.model.action;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * A model clas that represents an edit to a label
 */
@ProtobufMessage(name = "SyncActionValue.LabelEditAction")
public final class LabelEditAction implements Action {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int color;

    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int id;

    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean deleted;

    LabelEditAction(String name, int color, int id, boolean deleted) {
        this.name = name;
        this.color = color;
        this.id = id;
        this.deleted = deleted;
    }

    @Override
    public String indexName() {
        return "label_edit";
    }

    @Override
    public int actionVersion() {
        return 3;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public int color() {
        return color;
    }

    public int id() {
        return id;
    }

    public boolean deleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LabelEditAction that
                && color == that.color
                && id == that.id
                && deleted == that.deleted
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color, id, deleted);
    }

    @Override
    public String toString() {
        return "LabelEditAction[" +
                "name=" + name + ", " +
                "color=" + color + ", " +
                "id=" + id + ", " +
                "deleted=" + deleted + ']';
    }
}
