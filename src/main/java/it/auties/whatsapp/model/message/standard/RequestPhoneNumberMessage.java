package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jackson.Jacksonized;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = true)
@Jacksonized
@Builder
@ProtobufName("RequestPhoneNumberMessage")
public final class RequestPhoneNumberMessage extends ContextualMessage {
    @ProtobufProperty(index = 1, name = "contextInfo", type = ProtobufType.MESSAGE)
    @Builder.Default
    private ContextInfo contextInfo = new ContextInfo();

    @Override
    public MessageType type() {
        return MessageType.REQUEST_PHONE_NUMBER;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }
}
