package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * This model class describes a Location
 */
@ProtobufMessage(name = "Location")
public final class InteractiveLocation {
    @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
    final double latitude;

    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    final double longitude;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String name;

    InteractiveLocation(double latitude, double longitude, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InteractiveLocation that
                && Double.compare(latitude, that.latitude) == 0
                && Double.compare(longitude, that.longitude) == 0
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, name);
    }

    @Override
    public String toString() {
        return "InteractiveLocation[" +
                "latitude=" + latitude + ", " +
                "longitude=" + longitude + ", " +
                "name=" + name + ']';
    }
}