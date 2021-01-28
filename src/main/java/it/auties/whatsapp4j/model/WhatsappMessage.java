package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.utils.WhatsappIdUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Optional;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappMessage {
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();
    private @NotNull ProtoBuf.WebMessageInfo info;

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

    public @NotNull Optional<String> sender() {
        var jid = senderJid();
        if(jid.isEmpty()){
            return Optional.empty();
        }

        var contact = MANAGER.findContactByJid(jid.get());
        return contact.isEmpty() ? Optional.empty() : Optional.ofNullable(contact.get().bestName());
    }

    public @NotNull Optional<String> senderJid() {
        return info.getKey().getFromMe() ? Optional.empty() : info.hasParticipant() ? Optional.of(info.getParticipant()) : info.getKey().hasParticipant() ? Optional.of(info.getKey().getParticipant()) : Optional.of(info.getKey().getRemoteJid());
    }

    public @NotNull Optional<WhatsappMessage> quotedMessage(){
        var message = info.getMessage();
        if(!message.hasExtendedTextMessage()){
            return Optional.empty();
        }

        var extendedMessage = message.getExtendedTextMessage();
        if(!extendedMessage.hasContextInfo()){
            return Optional.empty();
        }

        return MANAGER.findMessage(MANAGER.findChatByMessage(this).orElseThrow(), extendedMessage.getContextInfo().getQuotedMessage());
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappMessage that && that.info.getKey().getId().equals(this.info.getKey().getId());
    }
}
