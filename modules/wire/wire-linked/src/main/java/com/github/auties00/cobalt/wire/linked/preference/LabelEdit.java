package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Input model for {@code editLabel}. Carries the identifier of the label
 * being edited together with the new display name and palette colour index
 * that replace the label's current rendering.
 *
 * <p>The label identifier and the new display name are required; the colour
 * index selects one of the fixed entries in WhatsApp's label colour palette.
 */
@ProtobufMessage
public final class LabelEdit {
    /**
     * Identifier of the label being edited.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String labelId;

    /**
     * New display name shown for the label.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Index into WhatsApp's fixed label colour palette that selects the
     * colour the label is rendered with.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    final int colorIndex;

    /**
     * Constructs a new {@code LabelEdit}.
     *
     * @param labelId    the label identifier; required
     * @param name       the new label display name; required
     * @param colorIndex the palette colour index
     * @throws NullPointerException if {@code labelId} or {@code name} is {@code null}
     */
    LabelEdit(String labelId, String name, int colorIndex) {
        this.labelId = Objects.requireNonNull(labelId, "labelId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.colorIndex = colorIndex;
    }

    /**
     * Returns the identifier of the label being edited.
     *
     * @return the label identifier, never {@code null}
     */
    public String labelId() {
        return labelId;
    }

    /**
     * Returns the new label display name.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the palette colour index the label is rendered with.
     *
     * @return the colour index
     */
    public int colorIndex() {
        return colorIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LabelEdit) obj;
        return Objects.equals(labelId, that.labelId) &&
                Objects.equals(name, that.name) &&
                colorIndex == that.colorIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelId, name, colorIndex);
    }

    @Override
    public String toString() {
        return "LabelEdit[" +
                "labelId=" + labelId + ", " +
                "name=" + name + ", " +
                "colorIndex=" + colorIndex + ']';
    }
}
