package com.github.auties00.cobalt.wire.linked.location;

import com.github.auties00.cobalt.wire.linked.message.interactive.InteractiveAnnotation;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * A two-dimensional coordinate used as a vertex of the tappable polygon
 * attached to an image or video message.
 *
 * <p>Interactive annotations overlay a tappable region on top of a media
 * message. The boundary of that region is described by an ordered list of
 * {@code Point} instances stored in
 * {@link InteractiveAnnotation#polygonVertices() InteractiveAnnotation.polygonVertices()}.
 * Consecutive vertices are connected in order and the final vertex is
 * implicitly joined back to the first to form a closed polygon. Tapping
 * inside the polygon triggers the annotation's associated action, for
 * example, opening a {@link Location} on a map.
 *
 * <p>Coordinates are expressed as normalised values relative to the media
 * content: {@code x} measures the horizontal position from the left edge
 * and {@code y} measures the vertical position from the top edge, both
 * typically in the range {@code [0.0, 1.0]}.
 *
 * <p>Earlier versions of the protocol encoded vertices as integers
 * exposed through {@link #xDeprecated()} and {@link #yDeprecated()}. The
 * protocol has since moved to double-precision floating-point coordinates
 * accessible through {@link #x()} and {@link #y()}. New code should use
 * the double-precision accessors exclusively; the integer accessors are
 * retained only for backward compatibility with older peers.
 *
 * @see InteractiveAnnotation
 * @see Location
 */
@ProtobufMessage(name = "Point")
public final class Point {
    /**
     * The deprecated integer x-coordinate of this vertex.
     *
     * <p>This field has been superseded by the double-precision {@link #x}
     * field and is retained only for backward compatibility with older
     * protocol versions.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    Integer xDeprecated;

    /**
     * The deprecated integer y-coordinate of this vertex.
     *
     * <p>This field has been superseded by the double-precision {@link #y}
     * field and is retained only for backward compatibility with older
     * protocol versions.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    Integer yDeprecated;

    /**
     * The double-precision x-coordinate of this vertex within the
     * annotation polygon.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    Double x;

    /**
     * The double-precision y-coordinate of this vertex within the
     * annotation polygon.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
    Double y;

    /**
     * Constructs a new {@code Point} with the specified deprecated integer
     * coordinates and double-precision coordinates.
     *
     * @param xDeprecated the deprecated integer x-coordinate, or
     *                    {@code null} if not available
     * @param yDeprecated the deprecated integer y-coordinate, or
     *                    {@code null} if not available
     * @param x           the double-precision x-coordinate, or
     *                    {@code null} if not available
     * @param y           the double-precision y-coordinate, or
     *                    {@code null} if not available
     */
    Point(Integer xDeprecated, Integer yDeprecated, Double x, Double y) {
        this.xDeprecated = xDeprecated;
        this.yDeprecated = yDeprecated;
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the deprecated integer x-coordinate of this vertex, if
     * present. New code should use {@link #x()} instead.
     *
     * @return an {@code OptionalInt} containing the deprecated x-coordinate,
     *         or empty if the value was not set
     * @deprecated Use {@link #x()} for the double-precision coordinate.
     */
    @Deprecated
    public OptionalInt xDeprecated() {
        return xDeprecated == null ? OptionalInt.empty() : OptionalInt.of(xDeprecated);
    }

    /**
     * Returns the deprecated integer y-coordinate of this vertex, if
     * present. New code should use {@link #y()} instead.
     *
     * @return an {@code OptionalInt} containing the deprecated y-coordinate,
     *         or empty if the value was not set
     * @deprecated Use {@link #y()} for the double-precision coordinate.
     */
    @Deprecated
    public OptionalInt yDeprecated() {
        return yDeprecated == null ? OptionalInt.empty() : OptionalInt.of(yDeprecated);
    }

    /**
     * Returns the double-precision x-coordinate of this vertex within the
     * annotation polygon, if present.
     *
     * @return an {@code OptionalDouble} containing the x-coordinate, or
     *         empty if the value was not set
     */
    public OptionalDouble x() {
        return x == null ? OptionalDouble.empty() : OptionalDouble.of(x);
    }

    /**
     * Returns the double-precision y-coordinate of this vertex within the
     * annotation polygon, if present.
     *
     * @return an {@code OptionalDouble} containing the y-coordinate, or
     *         empty if the value was not set
     */
    public OptionalDouble y() {
        return y == null ? OptionalDouble.empty() : OptionalDouble.of(y);
    }

    /**
     * Sets the deprecated integer x-coordinate of this vertex.
     *
     * @param xDeprecated the deprecated x-coordinate, or {@code null} to
     *                    clear the value
     * @deprecated Use {@link #setX(Double)} instead.
     */
    @Deprecated
    public void setXDeprecated(Integer xDeprecated) {
        this.xDeprecated = xDeprecated;
    }

    /**
     * Sets the deprecated integer y-coordinate of this vertex.
     *
     * @param yDeprecated the deprecated y-coordinate, or {@code null} to
     *                    clear the value
     * @deprecated Use {@link #setY(Double)} instead.
     */
    @Deprecated
    public void setYDeprecated(Integer yDeprecated) {
        this.yDeprecated = yDeprecated;
    }

    /**
     * Sets the double-precision x-coordinate of this vertex.
     *
     * @param x the x-coordinate, or {@code null} to clear the value
     */
    public void setX(Double x) {
        this.x = x;
    }

    /**
     * Sets the double-precision y-coordinate of this vertex.
     *
     * @param y the y-coordinate, or {@code null} to clear the value
     */
    public void setY(Double y) {
        this.y = y;
    }
}
