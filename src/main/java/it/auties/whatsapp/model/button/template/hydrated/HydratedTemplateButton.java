package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a hydrated template for a button
 */
@ProtobufMessage(name = "HydratedTemplateButton")
public record HydratedTemplateButton(
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        HydratedQuickReplyButton quickReplyButton,
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        HydratedURLButton urlButton,
        @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
        HydratedCallButton callButton,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int index
) {
    /**
     * Constructs a new template button
     *
     * @param button the non-null button
     * @return a non-null button template
     */
    public static HydratedTemplateButton of(HydratedButton button) {
        return of(-1, button);
    }

    /**
     * Constructs a new template button
     *
     * @param index  the index
     * @param button the non-null button
     * @return a non-null button template
     */
    public static HydratedTemplateButton of(int index, HydratedButton button) {
        var builder = new HydratedTemplateButtonBuilder()
                .index(index);
        switch (button) {
            case HydratedQuickReplyButton hydratedQuickReplyButton ->
                    builder.quickReplyButton(hydratedQuickReplyButton);
            case HydratedURLButton hydratedURLButton -> builder.urlButton(hydratedURLButton);
            case HydratedCallButton hydratedCallButton -> builder.callButton(hydratedCallButton);
            case null -> {
            }
        }
        return builder.build();
    }

    /**
     * Returns this button
     *
     * @return a non-null optional
     */
    public Optional<HydratedButton> button() {
        if (quickReplyButton != null) {
            return Optional.of(quickReplyButton);
        }

        if (urlButton != null) {
            return Optional.of(urlButton);
        }

        if (callButton != null) {
            return Optional.of(callButton);
        }

        return Optional.empty();
    }

    /**
     * Returns the type of button that this message wraps
     *
     * @return a non-null button type
     */
    public HydratedButton.Type buttonType() {
        return button().map(HydratedButton::buttonType)
                .orElse(HydratedButton.Type.NONE);
    }
}
