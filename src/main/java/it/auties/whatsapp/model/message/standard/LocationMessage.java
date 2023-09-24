package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.button.template.hsm.HighlyStructuredFourRowTemplateTitle;
import it.auties.whatsapp.model.button.template.hydrated.HydratedFourRowTemplateTitle;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.button.ButtonsMessageHeader;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;

import java.util.Optional;


/**
 * A model class that represents a message holding a location inside
 */
@ProtobufMessageName("Message.LocationMessage")
public record LocationMessage(
        @ProtobufProperty(index = 1, type = ProtobufType.DOUBLE)
        double latitude,
        @ProtobufProperty(index = 2, type = ProtobufType.DOUBLE)
        double longitude,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Optional<String> name,
        @ProtobufProperty(index = 4, type = ProtobufType.STRING)
        Optional<String> address,
        @ProtobufProperty(index = 5, type = ProtobufType.STRING)
        Optional<String> url,
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        boolean live,
        @ProtobufProperty(index = 7, type = ProtobufType.UINT32)
        int accuracy,
        @ProtobufProperty(index = 8, type = ProtobufType.FLOAT)
        float speed,
        @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
        int magneticNorthOffset,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        Optional<String> caption,
        @ProtobufProperty(index = 16, type = ProtobufType.BYTES)
        Optional<byte[]> thumbnail,
        @ProtobufProperty(index = 17, type = ProtobufType.OBJECT)
        Optional<ContextInfo> contextInfo
) implements ContextualMessage, ButtonsMessageHeader, HighlyStructuredFourRowTemplateTitle, HydratedFourRowTemplateTitle {

    @Override
    public MessageType type() {
        return MessageType.LOCATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    @Override
    public HighlyStructuredFourRowTemplateTitle.Type titleType() {
        return HighlyStructuredFourRowTemplateTitle.Type.LOCATION;
    }

    @Override
    public HydratedFourRowTemplateTitle.Type hydratedTitleType() {
        return HydratedFourRowTemplateTitle.Type.LOCATION;
    }

    @Override
    public ButtonsMessageHeader.Type buttonHeaderType() {
        return ButtonsMessageHeader.Type.LOCATION;
    }
}