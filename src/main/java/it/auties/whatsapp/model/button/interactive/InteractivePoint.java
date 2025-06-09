package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * This model class describes a Point in space
 */
@ProtobufMessage(name = "Point")
public final class InteractivePoint {
    @ProtobufProperty(index = 1, type = ProtobufType.INT32)
    final Integer xDeprecated;

    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    final Integer yDeprecated;

    @ProtobufProperty(index = 3, type = ProtobufType.DOUBLE)
    final Double x;

    @ProtobufProperty(index = 4, type = ProtobufType.DOUBLE)
    final Double y;

    InteractivePoint(Integer xDeprecated, Integer yDeprecated, Double x, Double y) {
        this.xDeprecated = xDeprecated;
        this.yDeprecated = yDeprecated;
        this.x = x;
        this.y = y;
    }

    public int xDeprecated() {
        return xDeprecated;
    }

    public int yDeprecated() {
        return yDeprecated;
    }

    public double x() {
        if (x != null) {
            return x;
        } else if (xDeprecated != null) {
            return xDeprecated;
        } else {
            return 0;
        }
    }

    public double y() {
        if (y != null) {
            return y;
        } else if (yDeprecated != null) {
            return yDeprecated;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractivePoint that
                && Objects.equals(xDeprecated, that.xDeprecated)
                && Objects.equals(yDeprecated, that.yDeprecated)
                && Objects.equals(x, that.x)
                && Objects.equals(y, that.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xDeprecated, yDeprecated, x, y);
    }

    @Override
    public String toString() {
        return "InteractivePoint[" +
                "xDeprecated=" + xDeprecated + ", " +
                "yDeprecated=" + yDeprecated + ", " +
                "x=" + x + ", " +
                "y=" + y + ']';
    }
}