package it.auties.whatsapp4j.model;

/**
 * An immutable model class that holds a pair of coordinates
 * @param latitude  the latitude in degrees
 * @param longitude the longitude in degrees
 */
public record WhatsappCoordinates(double latitude, double longitude, int degreesClockwiseFromMagneticNorth) {
}
