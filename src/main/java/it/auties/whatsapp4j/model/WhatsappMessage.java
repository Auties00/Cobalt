package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.ProtoBuf;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappMessage {
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();
    private @NotNull ProtoBuf.WebMessageInfo info;

    public @NotNull Optional<String> text() {
        return WhatsappUtils.extractText(info.getMessage());
    }

    public @NotNull Optional<WhatsappContact> sender() {
        var jid = senderJid();
        if(jid.isEmpty()){
            return Optional.empty();
        }

        return MANAGER.findContactByJid(jid.get());
    }

    public @NotNull Optional<String> senderJid() {
        return info.getKey().getFromMe() ? Optional.empty() : info.hasParticipant() ? Optional.of(info.getParticipant()) : info.getKey().hasParticipant() ? Optional.of(info.getKey().getParticipant()) : Optional.of(info.getKey().getRemoteJid());
    }

    @SneakyThrows
    public @NotNull Optional<WhatsappMessage> quotedMessage(){
        var message = info.getMessage();
        if(!message.hasExtendedTextMessage()){
            return Optional.empty();
        }

        var extendedMessage = message.getExtendedTextMessage();
        if(!extendedMessage.hasContextInfo()){
            return Optional.empty();
        }

        var context = extendedMessage.getContextInfo();
        if(!context.hasQuotedMessage()){
            return Optional.empty();
        }

        var chat = MANAGER.findChatByMessage(this);
        if(chat.isEmpty()){
            return Optional.empty();
        }

        var textOpt = WhatsappUtils.extractText(context.getQuotedMessage());
        if(textOpt.isEmpty()){
            return Optional.empty();
        }

        var textToSearch = textOpt.get();
        return chat.get().messages().stream().filter(e -> e.text().map(text -> text.equals(textToSearch) && context.getStanzaId().equals(e.info().getKey().getId())).orElse(false)).findAny();
    }

    public boolean sentByMe(){
        return info.getKey().getFromMe();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappMessage that && that.info.getKey().getId().equals(this.info.getKey().getId());
    }
}
