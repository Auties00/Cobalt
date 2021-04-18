package it.auties.whatsapp4j.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * An immutable model class that holds a pair of coordinates
 *
 */
@Getter
@Setter
@Accessors(chain = true,fluent = true)
@EqualsAndHashCode
@ToString
public final class WhatsappCoordinates {
    private final double latitude;
    private final double longitude;
    private final int degreesClockwiseFromMagneticNorth;

    /**
     * @param latitude the latitude in degrees
     * @param longitude the longitude in degrees
     */
    public WhatsappCoordinates(double latitude, double longitude, int degreesClockwiseFromMagneticNorth) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.degreesClockwiseFromMagneticNorth = degreesClockwiseFromMagneticNorth;
    }

    public int degreesClockwiseFromMagneticNorth() {
        return degreesClockwiseFromMagneticNorth;
    }
}
