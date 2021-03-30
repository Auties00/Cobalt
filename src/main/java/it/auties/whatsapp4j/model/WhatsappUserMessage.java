package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@Accessors(fluent = true)
@ToString
public abstract sealed class WhatsappUserMessage extends WhatsappMessage permits WhatsappContactMessage, WhatsappGenericMessage, WhatsappGroupInviteMessage, WhatsappLocationMessage, WhatsappMediaMessage, WhatsappTextMessage {
    /**
     * A map that holds the read status of this message for each participant.
     * If the chat associated with this chat is not a group, this map's size will always be 1.
     * In this case it is guaranteed that the value stored in this map for the contact associated with this chat equals {@link WhatsappUserMessage#globalStatus()}.
     * Otherwise, it is guaranteed to be participants - 1.
     * In this case it is guaranteed that every value stored in this map for each participant of this chat is equal or higher hierarchically then {@link WhatsappUserMessage#globalStatus()}.
     * It is important to remember that it is guaranteed that every participant will be present as a key.
     */
    private @NotNull @Getter final Map<WhatsappContact, WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS> individualReadStatus;

    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object if it's a message and the condition is met
     *
     * @param info the raw protobuf to wrap
     * @param condition the condition to meet
     */
    public WhatsappUserMessage(@NotNull WhatsappProtobuf.WebMessageInfo info, boolean condition) {
        super(info, info.hasMessage() && condition);
        this.individualReadStatus = new HashMap<>();
    }

    protected WhatsappUserMessage() {
        super(WhatsappProtobuf.WebMessageInfo.getDefaultInstance(), true);
        this.individualReadStatus = new HashMap<>();
    }

    /**
     * Returns whether this message was sent by yourself or not
     *
     * @return true if this message was sent by yourself
     */
    public boolean sentByMe() {
        return info.getKey().getFromMe();
    }

    /**
     * Returns an optional {@link WhatsappContact} representing the sender of this message
     *
     * @return a non empty optional {@link WhatsappContact} if this message wasn't sent by yourself
     */
    public @NotNull Optional<WhatsappContact> sender() {
        var jid = senderJid();
        return jid.isEmpty() ? Optional.empty() : MANAGER.findContactByJid(jid.get());
    }

    /**
     * Returns an optional String representing the {@link WhatsappContact#jid()} of the sender of this message
     *
     * @return a non empty optional String if this message wasn't sent by yourself
     */
    public @NotNull Optional<String> senderJid() {
        if (sentByMe()) {
            return Optional.empty();
        }

        if (info.hasParticipant()) {
            return Optional.of(info.getParticipant());
        }

        return Optional.of(info.getKey().hasParticipant() ? info.getKey().getParticipant() : info.getKey().getRemoteJid());
    }

    /**
     * Returns an optional {@link WhatsappMessage} representing the message quoted by this message if said message is in memory
     *
     * @return a non empty optional {@link WhatsappMessage} if this message quotes a message
     */
    public @NotNull Optional<WhatsappUserMessage> quotedMessage() {
        return !info.hasMessage() ? Optional.empty() : contextInfo().flatMap(context -> MANAGER.findChatByMessage(this).flatMap(chat -> MANAGER.findQuotedMessageInChatByContext(chat, context)));
    }

    /**
     * Returns whether this message is marked as important or not
     *
     * @return true if this message is marked as important
     */
    public boolean starred() {
        return info.getStarred();
    }

    /**
     * Sets whether this message is marked as important or not
     *
     * @param starred the new value to assign to the starred field
     */
    public void starred(boolean starred) {
        this.info = info.toBuilder().setStarred(starred).build();
    }

    /**
     * Returns the non null global status of this message.
     * If the chat associated with this message is a group it is guaranteed that this field is equal or lower hierarchically then every value stored by {@link WhatsappUserMessage#individualReadStatus()}.
     * Otherwise, this field is guaranteed to be equal to the single value stored by {@link WhatsappUserMessage#individualReadStatus()} for the contact associated with the chat associated with this message.
     *
     * @return the non null global status of this message
     */
    public @NotNull WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS globalStatus() {
        return info.getStatus();
    }

    /**
     * Sets the global read status of this message
     *
     * @param status the new status to assign to the globalStatus field
     */
    public void globalStatus(@NotNull WhatsappProtobuf.WebMessageInfo.WEB_MESSAGE_INFO_STATUS status) {
        this.info = info.toBuilder().setStatus(status).build();
    }
}
