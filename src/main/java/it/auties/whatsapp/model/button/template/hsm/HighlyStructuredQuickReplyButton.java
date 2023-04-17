package it.auties.whatsapp.model.button.template.hsm;

import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.message.button.HighlyStructuredMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model class that represents a quick reply button
 */
@AllArgsConstructor(staticName = "of")
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class HighlyStructuredQuickReplyButton implements HighlyStructuredButton {
    /**
     * The text of this button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private HighlyStructuredMessage text;

    /**
     * The id of this button
     */
    @ProtobufProperty(index = 2, type = STRING)
    private String id;

    public HighlyStructuredButtonType buttonType() {
        return HighlyStructuredButtonType.QUICK_REPLY;
    }
}
