package com.github.auties00.cobalt.wire.linked.bot.rendering;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Configuration metadata that controls how the AI bot response is rendered on the client.
 *
 * <p>This metadata is sent from the server as part of
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata BotMetadata} to specify the
 * version of Meta's Bloks UI rendering framework that should be used, along with the
 * device's pixel density for resolution-appropriate rendering.
 *
 * <p>This type is referenced from {@code BotMetadata} as the
 * {@code botRenderingConfigMetadata} field (protobuf index 36).
 */
@ProtobufMessage(name = "BotRenderingConfigMetadata")
public final class BotRenderingConfigMetadata {
    /**
     * The version identifier for Meta's Bloks UI rendering framework, for
     * example {@code "bloks_v10"}.
     *
     * <p>Bloks is Meta's cross-platform UI framework used to render dynamic
     * bot UI components. This identifier determines which version of the
     * framework should interpret the response.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String bloksVersioningId;

    /**
     * The device's pixel density (DPI scale factor) used to deliver
     * resolution-appropriate rendering, for example {@code 2.0} for a Retina
     * display or {@code 3.0} for an xxhdpi Android device.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
    Double pixelDensity;


    /**
     * Constructs a new {@code BotRenderingConfigMetadata} with the specified values.
     *
     * @param bloksVersioningId the Bloks framework version identifier, or {@code null}
     * @param pixelDensity      the device pixel density, or {@code null}
     */
    BotRenderingConfigMetadata(String bloksVersioningId, Double pixelDensity) {
        this.bloksVersioningId = bloksVersioningId;
        this.pixelDensity = pixelDensity;
    }

    /**
     * Returns the Bloks UI framework version identifier.
     *
     * @return an {@code Optional} describing the version identifier, or an
     *         empty {@code Optional} if not set
     */
    public Optional<String> bloksVersioningId() {
        return Optional.ofNullable(bloksVersioningId);
    }

    /**
     * Returns the device's pixel density.
     *
     * @return an {@code OptionalDouble} describing the pixel density, or an
     *         empty {@code OptionalDouble} if not set
     */
    public OptionalDouble pixelDensity() {
        return pixelDensity == null ? OptionalDouble.empty() : OptionalDouble.of(pixelDensity);
    }

    /**
     * Sets the Bloks UI framework version identifier.
     *
     * @param bloksVersioningId the new version identifier, or {@code null}
     */
    public void setBloksVersioningId(String bloksVersioningId) {
        this.bloksVersioningId = bloksVersioningId;
    }

    /**
     * Sets the device's pixel density.
     *
     * @param pixelDensity the new pixel density, or {@code null}
     */
    public void setPixelDensity(Double pixelDensity) {
        this.pixelDensity = pixelDensity;
    }
}
