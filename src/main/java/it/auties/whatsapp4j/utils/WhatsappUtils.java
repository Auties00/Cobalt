package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappConfiguration;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.glassfish.tyrus.core.Beta;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@UtilityClass
public class WhatsappUtils {
    private final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    public @NotNull Optional<String> extractText(@NotNull WhatsappProtobuf.Message message) {
        return message.hasConversation() ? Optional.of(message.getConversation()) : message.hasExtendedTextMessage() ? Optional.of(message.getExtendedTextMessage().getText()) : Optional.empty();
    }

    public @NotNull String phoneNumberFromJid(@NotNull String jid){
        return jid.split("@", 2)[0];
    }

    public @NotNull String parseJid(@NotNull String jid){
        return jid.replaceAll("@c.us", "@s.whatsapp.net");
    }

    public @NotNull String randomId(){
        return BinaryArray.random(10).toHex();
    }

    public @NotNull String buildRequestTag(@NotNull WhatsappConfiguration configuration){
        return "%s.--%s".formatted(configuration.requestTag(), MANAGER.tagAndIncrement());
    }

    public @NotNull Optional<ZonedDateTime> parseWhatsappTime(long time){
        return time == 0 ? Optional.empty() : Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault()));
    }

    public boolean isGroup(@NotNull String jid){
        return jid.contains("-");
    }

    // The beauty of composition, r/badcode where u at
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> extractContext(@NotNull WhatsappProtobuf.Message message){
        System.out.println("Extracting context");
        if(message.hasImageMessage()){
            System.out.println("Image");
            return Optional.of(message.getImageMessage().getContextInfo());
        } else if(message.hasContactMessage()){
            System.out.println("Contact");
            return Optional.of(message.getContactMessage().getContextInfo());
        } else if(message.hasLocationMessage()) {
            System.out.println("Loc");
            return Optional.of(message.getLocationMessage().getContextInfo());
        } else if(message.hasExtendedTextMessage()){
            System.out.println("Etm");
            return Optional.of(message.getExtendedTextMessage().getContextInfo());
        } else if(message.hasDocumentMessage()){
            System.out.println("Doc");
            return Optional.of(message.getDocumentMessage().getContextInfo());
        } else if(message.hasAudioMessage()) {
            System.out.println("Audio");
            return Optional.of(message.getAudioMessage().getContextInfo());
        } else if(message.hasVideoMessage()) {
            System.out.println("Video");
            return Optional.of(message.getVideoMessage().getContextInfo());
        } else if(message.hasContactsArrayMessage()){
            System.out.println("Contacts");
            return Optional.of(message.getContactsArrayMessage().getContextInfo());
        } else if(message.hasLiveLocationMessage()){
            System.out.println("llm");
            return Optional.of(message.getLiveLocationMessage().getContextInfo());
        } else if(message.hasStickerMessage()){
            System.out.println("sticker");
            return Optional.of(message.getStickerMessage().getContextInfo());
        } else if(message.hasTemplateMessage()){
            System.out.println("template");
            return Optional.of(message.getTemplateMessage().getContextInfo());
        } else if(message.hasTemplateButtonReplyMessage()){
            System.out.println("t2");
            return Optional.of(message.getTemplateButtonReplyMessage().getContextInfo());
        } else if(message.hasProductMessage()){
            System.out.println("pro");
            return Optional.of(message.getProductMessage().getContextInfo());
        } else if(message.hasGroupInviteMessage()){
            System.out.println("invite");
            return Optional.of(message.getGroupInviteMessage().getContextInfo());
        }else {
            return Optional.empty();
        }
    }

    public @NotNull List<WhatsappNode> jidsToParticipantNodes(@NotNull WhatsappContact... contacts){
        return jidsToParticipantNodes(Arrays.stream(contacts).map(WhatsappContact::jid).toArray(String[]::new));
    }

    public @NotNull List<WhatsappNode> jidsToParticipantNodes(@NotNull String... jids){
        Validate.isTrue(jids.length != 0, "WhatsappAPI: Cannot convert an array of jids to a list of jid nodes as the array is empty!");
        return Arrays.stream(jids)
                .map(jid -> new WhatsappNode("participant", Map.of("jid", jid), null))
                .collect(Collectors.toUnmodifiableList());
    }
}
