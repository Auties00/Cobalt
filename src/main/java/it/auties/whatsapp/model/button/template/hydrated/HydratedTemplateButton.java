package it.auties.whatsapp.model.button.template.hydrated;

import it.auties.protobuf.base.ProtobufMessage;
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
 * A model class that represents a hydrated template for a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class HydratedTemplateButton implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = HydratedQuickReplyButton.class)
    private HydratedQuickReplyButton quickReplyButton;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HydratedURLButton.class)
    private HydratedURLButton urlButton;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = HydratedCallButton.class)
    private HydratedCallButton callButton;

    @ProtobufProperty(index = 4, type = UINT32)
    private int index;

    /**
     * Constructs a new template button
     *
     * @param button the non-null button
     * @return a non-null button template
     */
    public static HydratedTemplateButton of(HydratedButton button) {
        var builder = HydratedTemplateButton.builder();
        switch (button){
            case HydratedQuickReplyButton hydratedQuickReplyButton -> builder.quickReplyButton(hydratedQuickReplyButton);
            case HydratedURLButton hydratedURLButton -> builder.urlButton(hydratedURLButton);
            case HydratedCallButton hydratedCallButton -> builder.callButton(hydratedCallButton);
            case null -> {}
        }
        return builder.build();
    }

    /**
     * Returns this button
     *
     * @return a non-null optional
     */
    public Optional<HydratedButton> button(){
        if(quickReplyButton != null){
            return Optional.of(quickReplyButton);
        }

        if(urlButton != null){
            return Optional.of(urlButton);
        }

        if(callButton != null){
            return Optional.of(callButton);
        }

        return Optional.empty();
    }

    /**
     * Returns the type of button that this message wraps
     *
     * @return a non-null button type
     */
    public HydratedButtonType buttonType() {
        return button().map(HydratedButton::buttonType)
                .orElse(HydratedButtonType.NONE);
    }
}
