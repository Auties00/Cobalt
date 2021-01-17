package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.model.WhatsappChat;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@UtilityClass
public class WhatsappChatUtils {
    public Optional<String> getText(@NotNull ProtoBuf.WebMessageInfo webMessageInfo) {
        var message = webMessageInfo.getMessage();
        if (message.hasConversation()) {
            return Optional.of(message.getConversation());
        }

        if (message.hasExtendedTextMessage()) {
            return Optional.of(message.getExtendedTextMessage().getText());
        }

        return Optional.empty();
    }

    public Optional<String> getSender(@NotNull WhatsappChat chat, @NotNull ProtoBuf.WebMessageInfo webMessageInfo) {
        if (chat.isGroup()){
            return webMessageInfo.getKey().getFromMe() ? Optional.empty() : Optional.of(webMessageInfo.getParticipant());
        }

        return webMessageInfo.getKey().getFromMe() ? Optional.empty() : Optional.ofNullable(chat.name() == null ? chat.jid() : chat.name());
    }
}
