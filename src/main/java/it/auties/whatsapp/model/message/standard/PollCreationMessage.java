package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.poll.PollOptionName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@Jacksonized
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ProtobufName("PollCreationMessage")
public final class PollCreationMessage extends ContextualMessage {
    @ProtobufProperty(index = 1, name = "encKey", type = ProtobufType.BYTES)
    private byte[] encKey;

    @ProtobufProperty(index = 2, name = "name", type = ProtobufType.STRING)
    private String name;

    @ProtobufProperty(implementation = PollOptionName.class, index = 3, name = "options", repeated = true, type = ProtobufType.MESSAGE)
    private List<PollOptionName> options;

    @ProtobufProperty(index = 4, name = "selectableOptionsCount", type = ProtobufType.UINT32)
    private Integer selectableOptionsCount;

    @ProtobufProperty(index = 5, name = "contextInfo", type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;

    @Override
    public MessageType type() {
        return MessageType.POLL_CREATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public static abstract class PollCreationMessageBuilder<C extends PollCreationMessage, B extends PollCreationMessageBuilder<C, B>>
            extends ContextualMessageBuilder<C, B> {
        public PollCreationMessageBuilder<C, B> options(List<PollOptionName> options) {
            if (this.options == null)
                this.options = new ArrayList<>();

            this.options.addAll(options);
            return this;
        }
    }
}
