package it.auties.whatsapp.model.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;

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
public class HydratedButtonTemplate
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = MESSAGE, implementation = HydratedQuickReplyButton.class)
    private HydratedQuickReplyButton quickReplyButton;

    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HydratedURLButton.class)
    private HydratedURLButton urlButton;

    @ProtobufProperty(index = 3, type = MESSAGE, implementation = HydratedCallButton.class)
    private HydratedCallButton callButton;

    @ProtobufProperty(index = 4, type = UINT32)
    private int index;

    /**
     * Constructs a new template from a quick reply
     *
     * @param index            the index of this button
     * @param quickReplyButton the non-null quick reply
     * @return a non-null button template
     */
    public static HydratedButtonTemplate of(int index, @NonNull HydratedQuickReplyButton quickReplyButton) {
        return HydratedButtonTemplate.builder()
                .index(index)
                .quickReplyButton(quickReplyButton)
                .build();
    }

    /**
     * Constructs a new template from an url button
     *
     * @param index     the index of this button
     * @param urlButton the non-null url button
     * @return a non-null button template
     */
    public static HydratedButtonTemplate of(int index, @NonNull HydratedURLButton urlButton) {
        return HydratedButtonTemplate.builder()
                .index(index)
                .urlButton(urlButton)
                .build();
    }

    /**
     * Constructs a new template from a call button
     *
     * @param index      the index of this button
     * @param callButton the non-null call button
     * @return a non-null button template
     */
    public static HydratedButtonTemplate of(int index, @NonNull HydratedCallButton callButton) {
        return HydratedButtonTemplate.builder()
                .index(index)
                .callButton(callButton)
                .build();
    }

    /**
     * Returns the type of button that this message wraps
     *
     * @return a non-null button type
     */
    public ButtonType buttonType() {
        if (quickReplyButton != null)
            return ButtonType.QUICK_REPLY_BUTTON;
        if (urlButton != null)
            return ButtonType.URL_BUTTON;
        if (callButton != null)
            return ButtonType.CALL_BUTTON;
        return ButtonType.NONE;
    }

    /**
     * The constants of this enumerated type describe the various types of buttons that a template can wrap
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum ButtonType
            implements ProtobufMessage {
        /**
         * No button
         */
        NONE(0),

        /**
         * Quick reply button
         */
        QUICK_REPLY_BUTTON(1),

        /**
         * Url button
         */
        URL_BUTTON(2),

        /**
         * Call button
         */
        CALL_BUTTON(3);

        @Getter
        private final int index;

        @JsonCreator
        public static ButtonType of(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(ButtonType.NONE);
        }
    }
}
