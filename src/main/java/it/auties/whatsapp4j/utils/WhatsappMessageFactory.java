package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.model.*;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;

/**
 * A utility class used to convert raw protobuf objects in WhatsappMessage objects
 * This class cannot be initialized
 */
@UtilityClass
public class WhatsappMessageFactory {
    /**
     * Returns a WhatsappMessage that wraps the input raw protobuf message
     *
     * @param info the message to wrap
     * @return a non null WhatsappMessage
     */
    public @NotNull WhatsappMessage buildMessageFromProtobuf(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        return info.hasMessage() ? buildUserMessageFromProtobuf(info) : new WhatsappServerMessage(info);
    }

    /**
     * Returns a WhatsappUserMessage that wraps the input raw protobuf message
     *
     * @param info the message to wrap
     * @return a non null WhatsappMessage
     */
    public @NotNull WhatsappUserMessage buildUserMessageFromProtobuf(@NotNull WhatsappProtobuf.WebMessageInfo info) {
        var message = info.getMessage();
        if (message.hasContactsArrayMessage() || message.hasContactMessage()) {
            return new WhatsappContactMessage(info);
        } else if (message.hasGroupInviteMessage()) {
            return new WhatsappGroupInviteMessage(info);
        } else if (message.hasLiveLocationMessage() || message.hasLocationMessage()) {
            return new WhatsappLocationMessage(info);
        } else if (message.hasImageMessage()) {
            return new WhatsappImageMessage(info);
        }else if(message.hasDocumentMessage()) {
            return new WhatsappDocumentMessage(info);
        }else if(message.hasAudioMessage()) {
            return new WhatsappAudioMessage(info);
        }else if(message.hasVideoMessage()) {
            return info.getMessage().getVideoMessage().hasGifPlayback() ? new WhatsappGifMessage(info) : new WhatsappVideoMessage(info);
        }else if(message.hasStickerMessage()){
            return new WhatsappStickerMessage(info);
        } else if (message.hasConversation() || message.hasExtendedTextMessage()) {
            return new WhatsappTextMessage(info);
        } else {
            return new WhatsappGenericMessage(info);
        }
    }
}
