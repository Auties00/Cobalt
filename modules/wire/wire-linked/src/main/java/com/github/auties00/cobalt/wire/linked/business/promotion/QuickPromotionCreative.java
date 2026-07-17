package com.github.auties00.cobalt.wire.linked.business.promotion;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Rendered content of one WhatsApp quick-promotion banner variant.
 *
 * <p>Each quick-promotion banner is shipped as one or more creatives: a
 * title and body text, the primary {@link QuickPromotionAction
 * call-to-action}, the light- and dark-mode image variants, an
 * accessibility description for the image, and a dismissible flag the
 * client honours to decide whether to render a close affordance.
 *
 * <p>This model is one such creative.
 */
@ProtobufMessage(name = "QuickPromotionCreative")
public final class QuickPromotionCreative {
    /**
     * Rendered title text, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String title;

    /**
     * Rendered body text, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String body;

    /**
     * Primary call-to-action carried by this creative, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final QuickPromotionAction primaryAction;

    /**
     * Light-mode image variant, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final QuickPromotionMediaVariant lightModeMedia;

    /**
     * Dark-mode image variant, or {@code null} when the server omitted
     * it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final QuickPromotionMediaVariant darkModeMedia;

    /**
     * Accessibility description of the banner image, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String imageAccessibilityText;

    /**
     * Whether the banner exposes a dismiss affordance. {@code false}
     * both when the server explicitly reported the banner as not
     * dismissible and when it omitted the flag entirely.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean dismissible;

    /**
     * Constructs a new {@code QuickPromotionCreative}. Any reference
     * argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param title                  the rendered title text, or
     *                               {@code null}
     * @param body                   the rendered body text, or
     *                               {@code null}
     * @param primaryAction          the primary call-to-action, or
     *                               {@code null}
     * @param lightModeMedia         the light-mode image variant, or
     *                               {@code null}
     * @param darkModeMedia          the dark-mode image variant, or
     *                               {@code null}
     * @param imageAccessibilityText the image accessibility description,
     *                               or {@code null}
     * @param dismissible            whether the banner exposes a dismiss
     *                               affordance
     */
    QuickPromotionCreative(String title, String body, QuickPromotionAction primaryAction,
                           QuickPromotionMediaVariant lightModeMedia, QuickPromotionMediaVariant darkModeMedia,
                           String imageAccessibilityText, boolean dismissible) {
        this.title = title;
        this.body = body;
        this.primaryAction = primaryAction;
        this.lightModeMedia = lightModeMedia;
        this.darkModeMedia = darkModeMedia;
        this.imageAccessibilityText = imageAccessibilityText;
        this.dismissible = dismissible;
    }

    /**
     * Returns the rendered title text.
     *
     * @return an {@code Optional} carrying the title, or empty when the
     *         server omitted it
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the rendered body text.
     *
     * @return an {@code Optional} carrying the body text, or empty when
     *         the server omitted it
     */
    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    /**
     * Returns the primary call-to-action carried by this creative.
     *
     * @return an {@code Optional} carrying the {@link QuickPromotionAction},
     *         or empty when the server omitted it
     */
    public Optional<QuickPromotionAction> primaryAction() {
        return Optional.ofNullable(primaryAction);
    }

    /**
     * Returns the light-mode image variant.
     *
     * @return an {@code Optional} carrying the
     *         {@link QuickPromotionMediaVariant}, or empty when the
     *         server omitted it
     */
    public Optional<QuickPromotionMediaVariant> lightModeMedia() {
        return Optional.ofNullable(lightModeMedia);
    }

    /**
     * Returns the dark-mode image variant.
     *
     * @return an {@code Optional} carrying the
     *         {@link QuickPromotionMediaVariant}, or empty when the
     *         server omitted it
     */
    public Optional<QuickPromotionMediaVariant> darkModeMedia() {
        return Optional.ofNullable(darkModeMedia);
    }

    /**
     * Returns the accessibility description of the banner image.
     *
     * @return an {@code Optional} carrying the description, or empty
     *         when the server omitted it
     */
    public Optional<String> imageAccessibilityText() {
        return Optional.ofNullable(imageAccessibilityText);
    }

    /**
     * Returns whether the banner exposes a dismiss affordance.
     *
     * @return {@code true} when the server flagged the banner as
     *         dismissible
     */
    public boolean dismissible() {
        return dismissible;
    }
}
