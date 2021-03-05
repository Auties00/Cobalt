package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.model.WhatsappProtobuf.WebMessageInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappMessage {
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();
    private @NotNull WebMessageInfo info;
    private @NotNull Map<WhatsappContact, WebMessageInfo.WEB_MESSAGE_INFO_STATUS> readStatus;
    public WhatsappMessage(@NotNull WebMessageInfo info){
        this(info, new HashMap<>());
    }

    public @NotNull Optional<String> text() {
        return WhatsappUtils.extractText(info.getMessage());
    }

    public @NotNull String id(){
        return info.getKey().getId();
    }

    public @NotNull Optional<WhatsappContact> sender() {
        var jid = senderJid();
        return jid.isEmpty() ? Optional.empty() : MANAGER.findContactByJid(jid.get());
    }

    public @NotNull Optional<String> senderJid() {
        return sentByMe() ? Optional.empty() : info.hasParticipant() ? Optional.of(info.getParticipant()) : info.getKey().hasParticipant() ? Optional.of(info.getKey().getParticipant()) : Optional.of(info.getKey().getRemoteJid());
    }

    @SneakyThrows
    public @NotNull Optional<WhatsappMessage> quotedMessage(){
        if (info.hasMessage()) {
            System.out.println("Msg");
            return WhatsappUtils.extractContext(info.getMessage()).flatMap(context -> MANAGER.findChatByMessage(this).flatMap(chat -> MANAGER.findQuotedMessageInChatByContext(chat, context)));
        }

        System.out.println("no message");
        return Optional.empty();
    }

    public boolean starred(){
        return info.getStarred();
    }

    public void starred(boolean starred){
        this.info = info.toBuilder().setStarred(starred).build();
    }

    public @NotNull WebMessageInfo.WEB_MESSAGE_INFO_STATUS status(){
        return info.getStatus();
    }

    public void status(@NotNull WebMessageInfo.WEB_MESSAGE_INFO_STATUS status){
        this.info = info.toBuilder().setStatus(status).build();
    }

    public boolean sentByMe(){
        return info.getKey().getFromMe();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappMessage that && that.info.getKey().getId().equals(this.info.getKey().getId());
    }
}
