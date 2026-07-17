package com.github.auties00.cobalt.wire.linked.preference;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Input model for {@code createLabel}. Carries the display name of the
 * label being created together with the palette colour index that controls
 * how the label is rendered.
 *
 * <p>The display name is required; the colour index selects one of the
 * fixed entries in WhatsApp's label colour palette.
 */
@ProtobufMessage
public final class LabelCreate {
    /**
     * Display name shown for the label.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Index into WhatsApp's fixed label colour palette that selects the
     * colour the label is rendered with.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final int colorIndex;

    /**
     * Constructs a new {@code LabelCreate}.
     *
     * @param name       the label display name; required
     * @param colorIndex the palette colour index
     * @throws NullPointerException if {@code name} is {@code null}
     */
    LabelCreate(String name, int colorIndex) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.colorIndex = colorIndex;
    }

    /**
     * Returns the label display name.
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
        var that = (LabelCreate) obj;
        return Objects.equals(name, that.name) &&
                colorIndex == that.colorIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, colorIndex);
    }

    @Override
    public String toString() {
        return "LabelCreate[" +
                "name=" + name + ", " +
                "colorIndex=" + colorIndex + ']';
    }
}
