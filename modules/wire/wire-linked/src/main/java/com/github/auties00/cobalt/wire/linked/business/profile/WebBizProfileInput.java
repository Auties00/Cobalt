package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Edit payload for a WhatsApp Business profile submitted through the web profile editor.
 *
 * <p>When a merchant edits their Business profile in the web drawer, the editor sends only the fields
 * the merchant actually changed. This model carries that edit set: the free-text
 * {@link #description() description}, {@link #email() email} and {@link #address() address}, the coarse
 * {@link #priceTier() price band}, the map {@link #latitude() latitude} and {@link #longitude()
 * longitude} of the business location, the {@link #websites() websites}, and the
 * {@link #serviceAreas() service areas} the business delivers to. Every field is optional; an unset
 * field leaves the corresponding profile value unchanged.
 */
@ProtobufMessage(name = "WebBizProfileInput")
public final class WebBizProfileInput {
    /**
     * Free-form business description. Unset leaves the existing description unchanged.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String description;

    /**
     * Contact email address. Unset leaves the existing email unchanged.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String email;

    /**
     * Street address of the business. Unset leaves the existing address unchanged.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String address;

    /**
     * Coarse price band advertised by the business. Unset leaves the existing band unchanged.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    final BusinessProfilePriceTier priceTier;

    /**
     * Latitude of the business location. Unset leaves the existing latitude unchanged.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
    final Double latitude;

    /**
     * Longitude of the business location. Unset leaves the existing longitude unchanged.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.DOUBLE)
    final Double longitude;

    /**
     * Website URLs advertised on the profile, in the order they are sent. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final List<String> websites;

    /**
     * Geographic areas the business serves, in the order they are sent. Never {@code null}, possibly
     * empty.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final List<BusinessProfileServiceArea> serviceAreas;

    /**
     * Constructs a new {@code WebBizProfileInput}. A {@code null} list argument is coerced to an empty
     * list; every other argument may be {@code null} to leave the corresponding field unset.
     *
     * @param description  the business description, or {@code null}
     * @param email        the contact email, or {@code null}
     * @param address      the street address, or {@code null}
     * @param priceTier    the price band, or {@code null}
     * @param latitude     the location latitude, or {@code null}
     * @param longitude    the location longitude, or {@code null}
     * @param websites     the website URLs; {@code null} treated as empty
     * @param serviceAreas the served areas; {@code null} treated as empty
     */
    WebBizProfileInput(String description, String email, String address, BusinessProfilePriceTier priceTier,
                       Double latitude, Double longitude, List<String> websites,
                       List<BusinessProfileServiceArea> serviceAreas) {
        this.description = description;
        this.email = email;
        this.address = address;
        this.priceTier = priceTier;
        this.latitude = latitude;
        this.longitude = longitude;
        this.websites = websites == null ? List.of() : List.copyOf(websites);
        this.serviceAreas = serviceAreas == null ? List.of() : List.copyOf(serviceAreas);
    }

    /**
     * Returns the free-form business description.
     *
     * @return an {@link Optional} carrying the description, or empty when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the contact email address.
     *
     * @return an {@link Optional} carrying the email, or empty when unset
     */
    public Optional<String> email() {
        return Optional.ofNullable(email);
    }

    /**
     * Returns the street address of the business.
     *
     * @return an {@link Optional} carrying the address, or empty when unset
     */
    public Optional<String> address() {
        return Optional.ofNullable(address);
    }

    /**
     * Returns the coarse price band advertised by the business.
     *
     * @return an {@link Optional} carrying the price band, or empty when unset
     */
    public Optional<BusinessProfilePriceTier> priceTier() {
        return Optional.ofNullable(priceTier);
    }

    /**
     * Returns the latitude of the business location.
     *
     * @return an {@link OptionalDouble} carrying the latitude, or empty when unset
     */
    public OptionalDouble latitude() {
        return latitude == null ? OptionalDouble.empty() : OptionalDouble.of(latitude);
    }

    /**
     * Returns the longitude of the business location.
     *
     * @return an {@link OptionalDouble} carrying the longitude, or empty when unset
     */
    public OptionalDouble longitude() {
        return longitude == null ? OptionalDouble.empty() : OptionalDouble.of(longitude);
    }

    /**
     * Returns the website URLs advertised on the profile.
     *
     * @return an unmodifiable view of the website URLs; never {@code null}, possibly empty
     */
    public List<String> websites() {
        return websites;
    }

    /**
     * Returns the geographic areas the business serves.
     *
     * @return an unmodifiable view of the served areas; never {@code null}, possibly empty
     */
    public List<BusinessProfileServiceArea> serviceAreas() {
        return serviceAreas;
    }
}
