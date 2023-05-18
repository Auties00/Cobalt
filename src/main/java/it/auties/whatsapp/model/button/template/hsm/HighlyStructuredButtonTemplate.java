package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.MESSAGE;
import static it.auties.protobuf.base.ProtobufType.UINT32;

/**
 * A model class that represents a template for a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("TemplateButton")
public class HighlyStructuredButtonTemplate implements ProtobufMessage {
    @ProtobufProperty(index = 4, type = UINT32)
    private int index;

    @ProtobufProperty(index = 1, type = MESSAGE)
    private HighlyStructuredQuickReplyButton highlyStructuredQuickReplyButton;

    @ProtobufProperty(index = 2, type = MESSAGE)
    private HighlyStructuredURLButton highlyStructuredUrlButton;

    @ProtobufProperty(index = 3, type = MESSAGE)
    private HighlyStructuredCallButton highlyStructuredCallButton;

    /**
     * Constructs a new template
     *
     * @param highlyStructuredButton the button
     * @return a non-null button template
     */
    public static HighlyStructuredButtonTemplate of(HighlyStructuredButton highlyStructuredButton) {
        var builder = HighlyStructuredButtonTemplate.builder();
        if (highlyStructuredButton instanceof HighlyStructuredCallButton structuredCallButton) {
            builder.highlyStructuredCallButton(structuredCallButton);
        } else if (highlyStructuredButton instanceof HighlyStructuredQuickReplyButton structuredQuickReplyButton) {
            builder.highlyStructuredQuickReplyButton(structuredQuickReplyButton);
        } else if (highlyStructuredButton instanceof HighlyStructuredURLButton highlyStructuredURLButton) {
            builder.highlyStructuredUrlButton(highlyStructuredURLButton);
        }
        return builder.build();
    }

    /**
     * Returns this button
     *
     * @return a non-null optional
     */
    public Optional<HighlyStructuredButton> button(){
        if(highlyStructuredQuickReplyButton != null){
            return Optional.of(highlyStructuredQuickReplyButton);
        }

        if(highlyStructuredUrlButton != null){
            return Optional.of(highlyStructuredUrlButton);
        }

        if(highlyStructuredCallButton != null){
            return Optional.of(highlyStructuredCallButton);
        }

        return Optional.empty();
    }

    /**
     * Returns the type of button that this message wraps
     *
     * @return a non-null button type
     */
    public HighlyStructuredButtonType buttonType() {
        return button().map(HighlyStructuredButton::buttonType)
                .orElse(HighlyStructuredButtonType.NONE);
    }
}