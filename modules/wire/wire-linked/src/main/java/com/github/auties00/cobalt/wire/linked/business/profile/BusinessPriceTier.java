package com.github.auties00.cobalt.wire.linked.business.profile;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * One selectable pricing band a WhatsApp Business account can pick when
 * configuring a paid feature.
 *
 * <p>Some paid WhatsApp Business features are sold in fixed bands rather than
 * at a freely chosen amount; WhatsApp returns the bands available in a given
 * market so an app can present them in a picker. Each tier carries a stable
 * {@link #id() identifier} an app stores when the user chooses it, a localised
 * {@link #description() description} naming the band, and the
 * {@link #currencySymbol() currency symbol} used to render the amounts in the
 * picker.
 *
 * <p>The description and currency symbol are localised to the request locale
 * supplied when the tiers were fetched; the identifier is locale-independent.
 */
@ProtobufMessage
public final class BusinessPriceTier {
    /**
     * Locale-independent stable identifier of this pricing band, recorded when
     * the user selects the tier. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Localised, human-readable description naming this pricing band in the
     * request locale. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Currency symbol used to render the amounts of this band in the request
     * locale, for example {@code "$"} or {@code "INR"}. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String currencySymbol;

    /**
     * Constructs a new {@code BusinessPriceTier}. Every argument is optional
     * and may be {@code null} when the server omitted the field.
     *
     * @param id             the locale-independent tier identifier, or {@code null}
     * @param description    the localised tier description, or {@code null}
     * @param currencySymbol the localised currency symbol, or {@code null}
     */
    BusinessPriceTier(String id, String description, String currencySymbol) {
        this.id = id;
        this.description = description;
        this.currencySymbol = currencySymbol;
    }

    /**
     * Returns the locale-independent stable identifier of this pricing band.
     *
     * @return an {@code Optional} containing the identifier, or empty when the
     *         server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the localised description naming this pricing band.
     *
     * @return an {@code Optional} containing the description, or empty when the
     *         server omitted it
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the currency symbol used to render the amounts of this band.
     *
     * @return an {@code Optional} containing the currency symbol, or empty when
     *         the server omitted it
     */
    public Optional<String> currencySymbol() {
        return Optional.ofNullable(currencySymbol);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessPriceTier) obj;
        return Objects.equals(this.id, that.id) &&
               Objects.equals(this.description, that.description) &&
               Objects.equals(this.currencySymbol, that.currencySymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, currencySymbol);
    }

    @Override
    public String toString() {
        return "BusinessPriceTier[" +
               "id=" + id + ", " +
               "description=" + description + ", " +
               "currencySymbol=" + currencySymbol + ']';
    }
}
