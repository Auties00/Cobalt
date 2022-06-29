package it.auties.whatsapp.model.message.button;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.protobuf.api.model.ProtobufProperty;
import it.auties.whatsapp.model.button.ButtonSection;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.ProductListInfo;
import it.auties.whatsapp.model.message.model.ButtonMessage;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.MESSAGE;
import static it.auties.protobuf.api.model.ProtobufProperty.Type.STRING;

/**
 * A model class that represents a message that contains a list of buttons or a list of products
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@Accessors(fluent = true)
public final class ButtonListMessage extends ContextualMessage implements ButtonMessage {
    /**
     * The title of this message
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String title;

    /**
     * The description of this message
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String description;

    /**
     * The text of the button of this message
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String buttonText;

    /**
     * The type of this message
     */
    @ProtobufProperty(index = 4, type = MESSAGE, concreteType = Type.class)
    private Type type;

    /**
     * The button sections of this message
     */
    @ProtobufProperty(index = 5, type = MESSAGE, concreteType = ButtonSection.class, repeated = true)
    private List<ButtonSection> sections;

    /**
     * The product info of this message
     */
    @ProtobufProperty(index = 6, type = MESSAGE, concreteType = ProductListInfo.class)
    private ProductListInfo productListInfo;

    /**
     * The footer text of this message
     */
    @ProtobufProperty(index = 7, type = STRING)
    private String footerText;

    /**
     * The context info of this message
     */
    @ProtobufProperty(index = 8, type = MESSAGE, concreteType = ContextInfo.class)
    @Builder.Default
    private ContextInfo contextInfo = new ContextInfo();  // Overrides ContextualMessage's context info

    /**
     * The constants of this enumerated type describe the various types of {@link ButtonListMessage}
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum Type {
        /**
         * Unknown
         */
        UNKNOWN(0),

        /**
         * Only one option can be selected
         */
        SINGLE_SELECT(1),

        /**
         * A list of products
         */
        PRODUCT_LIST(2);

        @Getter
        private final int index;

        @JsonCreator
        public static Type forIndex(int index) {
            return Arrays.stream(values())
                    .filter(entry -> entry.index() == index)
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class ButtonListMessageBuilder {
        public ButtonListMessageBuilder sections(List<ButtonSection> sections) {
            if (this.sections == null)
                this.sections = new ArrayList<>();
            this.sections.addAll(sections);
            return this;
        }
    }
}
