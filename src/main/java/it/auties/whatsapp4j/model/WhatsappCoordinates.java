package it.auties.whatsapp4j.model;

/**
 * An immutable model class that holds a pair of coordinates
 */
public record WhatsappCoordinates(double latitude, double longitude, int degreesClockwiseFromMagneticNorth) {
    /**
     * @param latitude  the latitude in degrees
     * @param longitude the longitude in degrees
     */
    public WhatsappCoordinates {
    }
}
