package it.auties.whatsapp4j.utils;

import it.auties.whatsapp4j.binary.BinaryArray;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.api.WhatsappConfiguration;
import it.auties.whatsapp4j.model.WhatsappContact;
import it.auties.whatsapp4j.model.WhatsappNode;
import it.auties.whatsapp4j.model.WhatsappProtobuf;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This utility class provides helper functionality to easily extract data out of Whatsapp models or raw protobuf messages
 * The use of accessors in those classes is preferred if they are easily available
 */
@UtilityClass
public class WhatsappUtils {
    /**
     * A singleton instance of {@link WhatsappDataManager}
     */
    private final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * Returns an optional String representing the text stored by this message
     *
     * @param message the target message
     * @return a non empty optional String if {@code message} is a text message
     */
    public @NotNull Optional<String> extractText(@NotNull WhatsappProtobuf.Message message) {
        return message.hasConversation() ? Optional.of(message.getConversation()) : message.hasExtendedTextMessage() ? Optional.of(message.getExtendedTextMessage().getText()) : Optional.empty();
    }

    /**
     * Returns the phone number associated with a jid
     *
     * @param jid the input jid
     * @return a non null String
     */
    public @NotNull String phoneNumberFromJid(@NotNull String jid){
        return jid.split("@", 2)[0];
    }

    /**
     * Parses c.us jids to standard whatsapp jids
     *
     * @param jid the input jid
     * @return a non null String
     */
    public @NotNull String parseJid(@NotNull String jid){
        return jid.replaceAll("@c.us", "@s.whatsapp.net");
    }

    /**
     * Returns a random message id
     *
     * @return a non null ten character String
     */
    public @NotNull String randomId(){
        return BinaryArray.random(10).toHex();
    }

    /**
     * Returns a request tag built using {@code configuration}
     *
     * @param configuration the configuration to use to build the message
     * @return a non null String
     */
    public @NotNull String buildRequestTag(@NotNull WhatsappConfiguration configuration){
        return "%s.--%s".formatted(configuration.requestTag(), MANAGER.tagAndIncrement());
    }

    /**
     * Returns a ZoneDateTime for {@code time}
     *
     * @param time the time in seconds since {@link Instant#EPOCH}
     * @return a non null empty optional if the {@code time} isn't 0
     */
    public @NotNull Optional<ZonedDateTime> parseWhatsappTime(long time){
        return time == 0 ? Optional.empty() : Optional.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.systemDefault()));
    }

    /**
     * Returns a boolean that determines whether {@code jid} is a group
     *
     * @param jid the input jid
     * @return true if {@code jid} is a group
     */
    public boolean isGroup(@NotNull String jid){
        return jid.contains("-");
    }

    /**
     * Returns an optional context for {@code message}
     *
     * @param message the input raw protobuf message
     * @return a non empty optional if any context is present
     * @apiNote the beauty of composition, r/badcode where u at
     */
    public @NotNull Optional<WhatsappProtobuf.ContextInfo> extractContext(@NotNull WhatsappProtobuf.Message message){
        if(message.hasImageMessage()){
            return Optional.of(message.getImageMessage().getContextInfo());
        } else if(message.hasContactMessage()){
            return Optional.of(message.getContactMessage().getContextInfo());
        } else if(message.hasLocationMessage()) {
            return Optional.of(message.getLocationMessage().getContextInfo());
        } else if(message.hasExtendedTextMessage()){
            return Optional.of(message.getExtendedTextMessage().getContextInfo());
        } else if(message.hasDocumentMessage()){
            return Optional.of(message.getDocumentMessage().getContextInfo());
        } else if(message.hasAudioMessage()) {
            return Optional.of(message.getAudioMessage().getContextInfo());
        } else if(message.hasVideoMessage()) {
            return Optional.of(message.getVideoMessage().getContextInfo());
        } else if(message.hasContactsArrayMessage()){
            return Optional.of(message.getContactsArrayMessage().getContextInfo());
        } else if(message.hasLiveLocationMessage()){
            return Optional.of(message.getLiveLocationMessage().getContextInfo());
        } else if(message.hasStickerMessage()){
            return Optional.of(message.getStickerMessage().getContextInfo());
        } else if(message.hasTemplateMessage()){
            return Optional.of(message.getTemplateMessage().getContextInfo());
        } else if(message.hasTemplateButtonReplyMessage()){
            return Optional.of(message.getTemplateButtonReplyMessage().getContextInfo());
        } else if(message.hasProductMessage()){
            return Optional.of(message.getProductMessage().getContextInfo());
        } else if(message.hasGroupInviteMessage()){
            return Optional.of(message.getGroupInviteMessage().getContextInfo());
        }else {
            return Optional.empty();
        }
    }

    /**
     * Returns a List of WhatsappNodes that represent {@code contacts}
     *
     * @param contacts any number of contacts to convert
     * @throws IllegalArgumentException if {@code contacts} is empty
     * @return a non null List of WhatsappNodes
     */
    public @NotNull List<WhatsappNode> jidsToParticipantNodes(@NotNull WhatsappContact... contacts){
        return jidsToParticipantNodes(Arrays.stream(contacts).map(WhatsappContact::jid).toArray(String[]::new));
    }

    /**
     * Returns a List of WhatsappNodes that represent {@code jids}
     *
     * @param jids any number of jids to convert
     * @throws IllegalArgumentException if {@code jids} is empty
     * @return a non null List of WhatsappNodes
     */
    public @NotNull List<WhatsappNode> jidsToParticipantNodes(@NotNull String... jids){
        Validate.isTrue(jids.length != 0, "WhatsappAPI: Cannot convert an array of jids to a list of jid nodes as the array is empty!");
        return Arrays.stream(jids)
                .map(jid -> new WhatsappNode("participant", Map.of("jid", jid), null))
                .collect(Collectors.toUnmodifiableList());
    }
}
