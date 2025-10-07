package com.github.auties00.cobalt.model.message.standard;

import com.github.auties00.cobalt.model.info.ContextInfo;
import com.github.auties00.cobalt.model.message.model.ContextualMessage;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A model class that represents a message holding a request for a phone value inside
 * Still needs to be implemented by Whatsapp
 */
@ProtobufMessage(name = "Message.RequestPhoneNumberMessage")
public final class RequestPhoneNumberMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    RequestPhoneNumberMessage(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    @Override
    public Type type() {
        return Type.REQUEST_PHONE_NUMBER;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    @Override
    public String toString() {
        return "RequestPhoneNumberMessage[" +
                "contextInfo=" + contextInfo + ']';
    }

}
