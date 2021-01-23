package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import lombok.ToString;
import org.glassfish.grizzly.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@ToString(exclude = "info")
public record WhatsappMessage(@NotNull ProtoBuf.WebMessageInfo info) {
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    public @NotNull Optional<String> text(@NotNull ProtoBuf.Message message) {
        if (message.hasConversation()) {
            return Optional.of(message.getConversation());
        }

        if (message.hasExtendedTextMessage()) {
            return Optional.of(message.getExtendedTextMessage().getText());
        }

        return Optional.empty();
    }

    public @NotNull Optional<String> text() {
        return text(info.getMessage());
    }

    public @NotNull Optional<String> sender(@NotNull WhatsappChat chat) {
        if (chat.isGroup()){
            return info.getKey().getFromMe() ? Optional.empty() : WhatsappDataManager.singletonInstance().findContactByJid(info.getParticipant()).map(WhatsappContact::bestName);
        }

        return info.getKey().getFromMe() ? Optional.empty() : Optional.ofNullable(chat.name() == null ? chat.jid() : chat.name());
    }

    public @NotNull Optional<WhatsappQuotedMessage> quotedMessage(){
        var message = info.getMessage();
        if(!message.hasExtendedTextMessage()){
            return Optional.empty();
        }

        var extendedMessage = message.getExtendedTextMessage();
        if(!extendedMessage.hasContextInfo()){
            return Optional.empty();
        }

        var context = extendedMessage.getContextInfo();
        var text = text(context.getQuotedMessage());
        if(text.isEmpty()){
            return Optional.empty();
        }

        if(!context.hasParticipant()){
            return Optional.empty();
        }

        var participant = context.getParticipant();
        var sender = MANAGER.findContactByJid(participant).map(WhatsappContact::bestName).orElse(participant);
        return Optional.of(new WhatsappQuotedMessage(text.get(), sender));
    }

    public @NotNull String chatName(){
        var jid = info.hasMessage() && info.getMessage().hasExtendedTextMessage() && info.getMessage().getExtendedTextMessage().hasContextInfo() && info.getMessage().getExtendedTextMessage().getContextInfo().hasParticipant() ? info.getMessage().getExtendedTextMessage().getContextInfo().getParticipant() : info.hasParticipant() ? info.getParticipant() : info.getKey().getRemoteJid();
        return MANAGER.findContactByJid(jid).map(WhatsappContact::bestName).orElse(jid);
    }
}
