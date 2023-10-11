package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a template for a button
 */
@ProtobufMessageName("HydratedTemplateButton")
public record HighlyStructuredButtonTemplate(
        @ProtobufProperty(index = 1, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredQuickReplyButton> highlyStructuredQuickReplyButton,
        @ProtobufProperty(index = 2, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredURLButton> highlyStructuredUrlButton,
        @ProtobufProperty(index = 3, type = ProtobufType.OBJECT)
        Optional<HighlyStructuredCallButton> highlyStructuredCallButton,
        @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
        int index
) implements ProtobufMessage {
    /**
     * Constructs a new template
     *
     * @param highlyStructuredButton the button
     * @return a non-null button template
     */
    public static HighlyStructuredButtonTemplate of(HighlyStructuredButton highlyStructuredButton) {
        return of(-1, highlyStructuredButton);
    }

    /**
     * Constructs a new template
     *
     * @param index                  the index
     * @param highlyStructuredButton the button
     * @return a non-null button template
     */
    public static HighlyStructuredButtonTemplate of(int index, HighlyStructuredButton highlyStructuredButton) {
        var builder = new HighlyStructuredButtonTemplateBuilder()
                .index(index);
        switch (highlyStructuredButton) {
            case HighlyStructuredQuickReplyButton highlyStructuredQuickReplyButton ->
                    builder.highlyStructuredQuickReplyButton(highlyStructuredQuickReplyButton);
            case HighlyStructuredURLButton highlyStructuredURLButton ->
                    builder.highlyStructuredUrlButton(highlyStructuredURLButton);
            case HighlyStructuredCallButton highlyStructuredCallButton ->
                    builder.highlyStructuredCallButton(highlyStructuredCallButton);
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
    public Optional<? extends HighlyStructuredButton> button() {
        if (highlyStructuredQuickReplyButton.isPresent()) {
            return highlyStructuredQuickReplyButton;
        }

        if (highlyStructuredUrlButton.isPresent()) {
            return highlyStructuredUrlButton;
        }

        return highlyStructuredCallButton;
    }

    /**
     * Returns the type of button that this message wraps
     *
     * @return a non-null button type
     */
    public HighlyStructuredButton.Type buttonType() {
        return button().map(HighlyStructuredButton::buttonType)
                .orElse(HighlyStructuredButton.Type.NONE);
    }
}