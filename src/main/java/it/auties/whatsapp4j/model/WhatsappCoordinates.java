package it.auties.whatsapp4j.model;

import java.util.Objects;

/**
 * An immutable model class that holds a pair of coordinates
 *
 */
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

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    public int degreesClockwiseFromMagneticNorth() {
        return degreesClockwiseFromMagneticNorth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WhatsappCoordinates) obj;
        return Double.doubleToLongBits(this.latitude) == Double.doubleToLongBits(that.latitude) &&
                Double.doubleToLongBits(this.longitude) == Double.doubleToLongBits(that.longitude) &&
                this.degreesClockwiseFromMagneticNorth == that.degreesClockwiseFromMagneticNorth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, degreesClockwiseFromMagneticNorth);
    }

    @Override
    public String toString() {
        return "WhatsappCoordinates[" +
                "latitude=" + latitude + ", " +
                "longitude=" + longitude + ", " +
                "degreesClockwiseFromMagneticNorth=" + degreesClockwiseFromMagneticNorth + ']';
    }

}
