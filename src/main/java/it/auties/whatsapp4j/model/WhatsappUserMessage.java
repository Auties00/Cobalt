package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage sent by a contact.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(fluent = true)
@ToString
@Data
public abstract sealed class WhatsappUserMessage extends WhatsappMessage permits WhatsappContactMessage, WhatsappGroupInviteMessage, WhatsappLocationMessage, WhatsappMediaMessage, WhatsappTextMessage, WhatsappGenericMessage {
    /**
     * A nullable {@link WhatsappMessage} representing the message quoted by this message if in memory
     */
    private final @Nullable WhatsappUserMessage quotedMessage;

    /**
     * A map that holds the read status of this message for each participant.
     * If the chat associated with this chat is not a group, this map's size will always be 1.
     * In this case it is guaranteed that the value stored in this map for the contact associated with this chat equals {@link WhatsappUserMessage#globalStatus()}.
     * Otherwise, it is guaranteed to be participants - 1.
     * In this case it is guaranteed that every value stored in this map for each participant of this chat is equal or higher hierarchically then {@link WhatsappUserMessage#globalStatus()}.
     * It is important to remember that it is guaranteed that every participant will be present as a key.
     */
    private final @NotNull Map<WhatsappContact, WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus> individualReadStatus;

    /**
     * Whether this message was forwarded or not
     */
    protected final boolean isForwarded;

    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object if it's a message and the condition is met
     *
     * @param info the raw protobuf to wrap
     * @param condition the condition to meet
     */
    public WhatsappUserMessage(@NotNull WhatsappProtobuf.WebMessageInfo info, boolean condition) {
        super(info, info.hasMessage() && condition);
        this.individualReadStatus = new HashMap<>();
        this.quotedMessage = contextInfo().flatMap(context -> MANAGER.findChatByMessage(this).flatMap(chat -> MANAGER.findQuotedMessageInChatByContext(chat, context))).orElse(null);
        this.isForwarded = contextInfo().map(WhatsappProtobuf.ContextInfo::getIsForwarded).orElse(false);
    }

    /**
     * A constructor used for lombok
     */
    protected WhatsappUserMessage() {
        super(WhatsappProtobuf.WebMessageInfo.getDefaultInstance(), true);
        this.individualReadStatus = new HashMap<>();
        this.quotedMessage = null;
        this.isForwarded = false;
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
        return jid.isEmpty() ? Optional.empty() : MANAGER.findContactByJid(jid);
    }

    /**
     * Returns an optional String representing the {@link WhatsappContact#jid()} of the sender of this message
     *
     * @return a non empty optional String
     */
    public @NotNull String senderJid() {
        return info.hasParticipant() ? info.getParticipant() : info.getKey().hasParticipant() ? info.getKey().getParticipant() : info.getKey().getRemoteJid();
    }

    /**
     * Returns an optional {@link WhatsappMessage} representing the message quoted by this message if said message is in memory
     *
     * @return a non empty optional {@link WhatsappMessage} if this message quotes a message
     */
    public @NotNull Optional<WhatsappUserMessage> quotedMessage() {
        return Optional.ofNullable(quotedMessage);
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
    public @NotNull WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus globalStatus() {
        return info.getStatus();
    }

    /**
     * Sets the global read status of this message
     *
     * @param status the new status to assign to the globalStatus field
     */
    public void globalStatus(@NotNull WhatsappProtobuf.WebMessageInfo.WebMessageInfoStatus status) {
        this.info = info.toBuilder().setStatus(status).build();
    }

    /**
     * Returns a list of {@link WhatsappContact} mentioned in this message
     *
     * @return a non null List
     */
    public @NotNull List<WhatsappContact> mentions(){
        return contextInfo().map(context -> context.getMentionedJidList().stream().map(MANAGER::findContactByJid).filter(Optional::isPresent).map(Optional::get).toList()).orElse(List.of());
    }

    /**
     * Returns the number of {@link WhatsappContact} mentioned in this message
     *
     * @return an unsigned int
     */
    public int mentionsCount(){
        return contextInfo().map(WhatsappProtobuf.ContextInfo::getMentionedJidCount).orElse(0);
    }
}
