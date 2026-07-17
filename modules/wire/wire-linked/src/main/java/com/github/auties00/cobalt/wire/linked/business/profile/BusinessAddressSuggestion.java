package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * One place suggestion returned while a user types an address for a WhatsApp
 * Business profile.
 *
 * <p>When a business sets its physical location, an app can offer
 * address autocomplete: as the user types, WhatsApp returns ranked place
 * matches. Each match is modelled here with a stable {@link #placeId() place
 * identifier} used to commit the selection, a one-line {@link #title() display
 * title} to show in the suggestion list, the {@link #latitude() latitude} and
 * {@link #longitude() longitude} of the place, and a structured breakdown of
 * the postal address ({@link #streetAddress() street}, {@link #city() city},
 * {@link #stateOrProvince() state or province}, {@link #postalCode() postal
 * code}, {@link #country() country}).
 *
 * <p>The coordinates are carried as decimal strings exactly as WhatsApp emits
 * them; every field is independently optional, since a given suggestion may
 * resolve only a partial address.
 */
@ProtobufMessage
public final class BusinessAddressSuggestion {
    /**
     * Stable place identifier used to commit this suggestion as the chosen
     * address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String placeId;

    /**
     * One-line display title shown for this suggestion in the picker. Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String title;

    /**
     * Latitude of the place as a decimal string. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String latitude;

    /**
     * Longitude of the place as a decimal string. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String longitude;

    /**
     * Street line of the structured postal address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String streetAddress;

    /**
     * City of the structured postal address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String city;

    /**
     * State or province of the structured postal address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String stateOrProvince;

    /**
     * Postal code of the structured postal address. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.STRING)
    final String postalCode;

    /**
     * Country of the structured postal address. Empty when the server omitted
     * it.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String country;

    /**
     * Constructs a new {@code BusinessAddressSuggestion}. Every argument is
     * optional and may be {@code null} when the server omitted the field.
     *
     * @param placeId         the stable place identifier, or {@code null}
     * @param title           the one-line display title, or {@code null}
     * @param latitude        the latitude as a decimal string, or {@code null}
     * @param longitude       the longitude as a decimal string, or {@code null}
     * @param streetAddress   the street line, or {@code null}
     * @param city            the city, or {@code null}
     * @param stateOrProvince the state or province, or {@code null}
     * @param postalCode      the postal code, or {@code null}
     * @param country         the country, or {@code null}
     */
    BusinessAddressSuggestion(String placeId,
                              String title,
                              String latitude,
                              String longitude,
                              String streetAddress,
                              String city,
                              String stateOrProvince,
                              String postalCode,
                              String country) {
        this.placeId = placeId;
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.streetAddress = streetAddress;
        this.city = city;
        this.stateOrProvince = stateOrProvince;
        this.postalCode = postalCode;
        this.country = country;
    }

    /**
     * Returns the stable place identifier used to commit this suggestion.
     *
     * @return an {@code Optional} containing the place id, or empty when the
     *         server omitted it
     */
    public Optional<String> placeId() {
        return Optional.ofNullable(placeId);
    }

    /**
     * Returns the one-line display title shown for this suggestion.
     *
     * @return an {@code Optional} containing the title, or empty when the server
     *         omitted it
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the latitude of the place as a decimal string.
     *
     * @return an {@code Optional} containing the latitude, or empty when the
     *         server omitted it
     */
    public Optional<String> latitude() {
        return Optional.ofNullable(latitude);
    }

    /**
     * Returns the longitude of the place as a decimal string.
     *
     * @return an {@code Optional} containing the longitude, or empty when the
     *         server omitted it
     */
    public Optional<String> longitude() {
        return Optional.ofNullable(longitude);
    }

    /**
     * Returns the street line of the structured postal address.
     *
     * @return an {@code Optional} containing the street line, or empty when the
     *         server omitted it
     */
    public Optional<String> streetAddress() {
        return Optional.ofNullable(streetAddress);
    }

    /**
     * Returns the city of the structured postal address.
     *
     * @return an {@code Optional} containing the city, or empty when the server
     *         omitted it
     */
    public Optional<String> city() {
        return Optional.ofNullable(city);
    }

    /**
     * Returns the state or province of the structured postal address.
     *
     * @return an {@code Optional} containing the state or province, or empty
     *         when the server omitted it
     */
    public Optional<String> stateOrProvince() {
        return Optional.ofNullable(stateOrProvince);
    }

    /**
     * Returns the postal code of the structured postal address.
     *
     * @return an {@code Optional} containing the postal code, or empty when the
     *         server omitted it
     */
    public Optional<String> postalCode() {
        return Optional.ofNullable(postalCode);
    }

    /**
     * Returns the country of the structured postal address.
     *
     * @return an {@code Optional} containing the country, or empty when the
     *         server omitted it
     */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessAddressSuggestion) obj;
        return Objects.equals(this.placeId, that.placeId) &&
               Objects.equals(this.title, that.title) &&
               Objects.equals(this.latitude, that.latitude) &&
               Objects.equals(this.longitude, that.longitude) &&
               Objects.equals(this.streetAddress, that.streetAddress) &&
               Objects.equals(this.city, that.city) &&
               Objects.equals(this.stateOrProvince, that.stateOrProvince) &&
               Objects.equals(this.postalCode, that.postalCode) &&
               Objects.equals(this.country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(placeId, title, latitude, longitude, streetAddress,
                city, stateOrProvince, postalCode, country);
    }

    @Override
    public String toString() {
        return "BusinessAddressSuggestion[" +
               "placeId=" + placeId + ", " +
               "title=" + title + ", " +
               "city=" + city + ", " +
               "country=" + country + ']';
    }
}
