package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappProtobuf.WebMessageInfo;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers
 * Instead, methods inside {@link WhatsappAPI} should be used
 */
@AllArgsConstructor
@Data
@Accessors(fluent = true)
@ToString
public class WhatsappMessage {
    /**
     * A singleton instance of {@link WhatsappDataManager}
     */
    private static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * The raw Protobuf object associated with this message
     */
    private @NotNull WebMessageInfo info;

    /**
     * A map that holds the read status of this message for each participant
     * If the chat associated with this chat is not a group, this map's size will always be 1
     * In this case it is guaranteed that the value stored in this map for the contact associated with this chat equals {@link WhatsappMessage#globalStatus()}
     * Otherwise, it is guaranteed to be participants - 1
     * In this case it is guaranteed that every value stored in this map for each participant of this chat is equal or higher hierarchically then {@link WhatsappMessage#globalStatus()}
     * It is important to remember that it is guaranteed that every participant will be present as a key
     */
    private @NotNull Map<WhatsappContact, WebMessageInfo.WEB_MESSAGE_INFO_STATUS> individualReadStatus;


    /**
     * Constructs a new WhatsappMessage from a raw Protobuf object
     * @param info the raw protobuf to wrap
     */
    public WhatsappMessage(@NotNull WebMessageInfo info){
        this(info, new HashMap<>());
    }

    /**
     * Returns an optional String representing the text stored by this message
     * @return a non empty optional String if this is a text message
     */
    public @NotNull Optional<String> text() {
        return WhatsappUtils.extractText(info.getMessage());
    }

    /**
     * Returns a non null unique identifier for this object
     * @return a non null String
     */
    public @NotNull String id(){
        return info.getKey().getId();
    }

    /**
     * Returns an optional {@link WhatsappContact} representing the sender of this message
     * @return a non empty optional {@link WhatsappContact} if this message isn't a service message and wasn't sent by yourself
     */
    public @NotNull Optional<WhatsappContact> sender() {
        var jid = senderJid();
        return jid.isEmpty() ? Optional.empty() : MANAGER.findContactByJid(jid.get());
    }

    /**
     * Returns an optional String representing the {@link WhatsappContact#jid()} of the sender of this message
     * @return a non empty optional String if this message isn't a service message and wasn't sent by yourself
     */
    public @NotNull Optional<String> senderJid() {
        return sentByMe() ? Optional.empty() : info.hasParticipant() ? Optional.of(info.getParticipant()) : info.getKey().hasParticipant() ? Optional.of(info.getKey().getParticipant()) : Optional.of(info.getKey().getRemoteJid());
    }

    /**
     * Returns an optional {@link WhatsappMessage} representing the message quoted by this message
     * @return a non empty optional {@link WhatsappMessage} if this message quotes a message
     */
    public @NotNull Optional<WhatsappMessage> quotedMessage(){
        return info.hasMessage() ? WhatsappUtils.extractContext(info.getMessage()).flatMap(context -> MANAGER.findChatByMessage(this).flatMap(chat -> MANAGER.findQuotedMessageInChatByContext(chat, context))) : Optional.empty();
    }

    /**
     * Returns whether this message is marked as important or not
     * @return true if this message is marked as important
     */
    public boolean starred(){
        return info.getStarred();
    }

    /**
     * Sets whether this message is marked as important or not
     * @param starred the new value to assign to the starred field
     */
    public void starred(boolean starred){
        this.info = info.toBuilder().setStarred(starred).build();
    }

    /**
     * Returns the non null global status of this message
     * If the chat associated with this message is a group it is guaranteed that this field is equal or lower hierarchically then every value stored by {@link WhatsappMessage#individualReadStatus()}
     * Otherwise, this field is guaranteed to be equal to the single value stored by {@link WhatsappMessage#individualReadStatus()} for the contact associated with the chat associated with this message
     * @return the non null global status of this message
     */
    public @NotNull WebMessageInfo.WEB_MESSAGE_INFO_STATUS globalStatus(){
        return info.getStatus();
    }

    /**
     * Sets the global read status of this message
     * @param status the new status to assign to the globalStatus field
     */
    public void globalStatus(@NotNull WebMessageInfo.WEB_MESSAGE_INFO_STATUS status){
        this.info = info.toBuilder().setStatus(status).build();
    }

    /**
     * Returns whether this message was sent by yourself or not
     * @return true if this message was sent by yourself
     */
    public boolean sentByMe(){
        return info.getKey().getFromMe();
    }

    /**
     * Checks if this object and {@param o} are equal
     *
     * @return true if {@param o} is an instance of {@link WhatsappMessage} and if their unique ids({@link WhatsappMessage#id()}) are equal
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof WhatsappMessage that && that.info.getKey().getId().equals(this.info.getKey().getId());
    }
}
