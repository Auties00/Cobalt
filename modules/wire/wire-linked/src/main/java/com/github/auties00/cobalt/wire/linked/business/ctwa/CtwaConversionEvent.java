package com.github.auties00.cobalt.wire.linked.business.ctwa;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A Click-to-WhatsApp conversion event mapped from one of a merchant's custom
 * chat labels.
 *
 * <p>When a merchant tags a chat with a custom label (for example "new order"
 * or "shipped"), WhatsApp can report that action back to the advertising
 * platform as a conversion against the ad that started the chat. The server
 * resolves each custom label into the conversion the ad platform records for
 * it: a coarse {@linkplain #conversionType() conversion type} (such as an
 * order being created or a lead being captured), a finer
 * {@linkplain #conversionSubtype() conversion subtype} (such as "shipped" or
 * "paid"), and an opaque {@linkplain #conversionMetadata() metadata} blob the
 * ad platform attaches.
 *
 * <p>This model is one such resolved mapping. The conversion type and subtype
 * are exposed as raw strings because the client lowercases them before
 * matching against an open-ended set, so the server casing is not a closed
 * value set.
 */
@ProtobufMessage(name = "CtwaConversionEvent")
public final class CtwaConversionEvent {
    /**
     * The custom chat label this conversion was mapped from. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String customLabel;

    /**
     * Coarse conversion type the ad platform records for the label (for
     * example an order being created or a lead being captured), as a raw
     * server marker. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String conversionType;

    /**
     * Finer conversion subtype the ad platform records for the label (for
     * example "shipped" or "paid"), as a raw server marker. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String conversionSubtype;

    /**
     * Opaque conversion-metadata blob the ad platform attaches to the event.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String conversionMetadata;

    /**
     * Constructs a new {@code CtwaConversionEvent}. Any reference argument may
     * be {@code null} when the server omitted the corresponding field.
     *
     * @param customLabel        the source custom label, or {@code null}
     * @param conversionType     the conversion type marker, or {@code null}
     * @param conversionSubtype  the conversion subtype marker, or {@code null}
     * @param conversionMetadata the opaque conversion-metadata blob, or {@code null}
     */
    CtwaConversionEvent(String customLabel, String conversionType, String conversionSubtype,
                        String conversionMetadata) {
        this.customLabel = customLabel;
        this.conversionType = conversionType;
        this.conversionSubtype = conversionSubtype;
        this.conversionMetadata = conversionMetadata;
    }

    /**
     * Returns the custom chat label this conversion was mapped from.
     *
     * @return the custom label, or empty when the server omitted it
     */
    public Optional<String> customLabel() {
        return Optional.ofNullable(customLabel);
    }

    /**
     * Returns the coarse conversion type the ad platform records for the
     * label.
     *
     * @return the conversion type marker, or empty when the server omitted it
     */
    public Optional<String> conversionType() {
        return Optional.ofNullable(conversionType);
    }

    /**
     * Returns the finer conversion subtype the ad platform records for the
     * label.
     *
     * @return the conversion subtype marker, or empty when the server omitted
     *         it
     */
    public Optional<String> conversionSubtype() {
        return Optional.ofNullable(conversionSubtype);
    }

    /**
     * Returns the opaque conversion-metadata blob the ad platform attaches.
     *
     * @return the conversion metadata, or empty when the server omitted it
     */
    public Optional<String> conversionMetadata() {
        return Optional.ofNullable(conversionMetadata);
    }
}
