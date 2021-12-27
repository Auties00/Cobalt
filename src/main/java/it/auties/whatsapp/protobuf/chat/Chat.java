package it.auties.whatsapp.protobuf.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.api.WhatsappListener;
import it.auties.whatsapp.protobuf.contact.Contact;
import it.auties.whatsapp.protobuf.contact.ContactJid;
import it.auties.whatsapp.protobuf.contact.ContactStatus;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.util.SortedMessageList;
import it.auties.whatsapp.util.WhatsappUtils;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A model class that represents a Chat.
 * A chat can be of two types: a conversation with a contact or a group.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link Whatsapp} should be used.
 * This class also offers a builder, accessible using {@link Chat#builder()}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Chat {
    /**
     * The non-null unique jid used to identify this chat
     */
    @JsonProperty(value = "1", required = true)
    @JsonPropertyDescription("string")
    @NonNull
    private ContactJid jid;

    /**
     * A non-null arrayList of messages in this chat sorted chronologically
     */
    @JsonProperty("2")
    @JsonPropertyDescription("HistorySyncMsg")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    @Default
    @NonNull
    private SortedMessageList messages = new SortedMessageList();

    /**
     * The nullable new unique jid for this Chat.
     * This field is not null when a contact changes phone number and connects their new phone number with Whatsapp.
     */
    @JsonProperty("3")
    @JsonPropertyDescription("string")
    private ContactJid newJid;

    /**
     * The nullable old jid for this Chat.
     * This field is not null when a contact changes phone number and connects their new phone number with Whatsapp.
     */
    @JsonProperty("4")
    @JsonPropertyDescription("string")
    private ContactJid oldJid;

    /**
     * The timestamp of the latest message in seconds since {@link java.time.Instant#EPOCH}
     */
    @JsonProperty("5")
    @JsonPropertyDescription("uint64")
    private long lastMessageTimestamp;

    /**
     * The number of unread messages in this chat.
     * If this field is negative, this chat is marked as unread.
     */
    @JsonProperty("6")
    @JsonPropertyDescription("uint32")
    private int unreadMessages;

    /**
     * This field is used to determine whether a chat is read only or not.
     * If true, it means that it's not possible to send messages here.
     * This is the case, for example, for groups where only admins can send messages.
     */
    @JsonProperty("7")
    @JsonPropertyDescription("bool")
    private boolean readOnly;

    /**
     * Marks whether Whatsapp has synced all messages for this chat
     */
    @JsonProperty("8")
    @JsonPropertyDescription("bool")
    private boolean complete;

    /**
     * The time in seconds before a message is automatically deleted from this chat both locally and from WhatsappWeb's servers.
     * If ephemeral messages aren't enabled, this field has a value of 0
     */
    @JsonProperty("9")
    @JsonPropertyDescription("uint32")
    private long ephemeralMessageDuration;

    /**
     * The time in seconds since {@link java.time.Instant#EPOCH} when ephemeral messages were turned on.
     * If ephemeral messages aren't enabled, this field has a value of 0.
     */
    @JsonProperty("10")
    @JsonPropertyDescription("int64")
    private long ephemeralMessagesToggleTime;

    /**
     * The timestamp for the creation of this chat in seconds since {@link java.time.Instant#EPOCH}
     */
    @JsonProperty("12")
    @JsonPropertyDescription("uint64")
    private long timestamp;

    /**
     * The non-null display name of this chat
     */
    @JsonProperty("13")
    @JsonPropertyDescription("string")
    @NonNull
    private String name;

    /**
     * The hash of this chat
     */
    @JsonProperty("14")
    @JsonPropertyDescription("string")
    @NonNull
    private String hash;

    /**
     * This field is used to determine whether a chat was marked as being spam or not.
     */
    @JsonProperty("15")
    @JsonPropertyDescription("bool")
    private boolean notSpam;

    /**
     * This field is used to determine whether a chat is archived or not.
     */
    @JsonProperty("16")
    @JsonPropertyDescription("bool")
    private boolean archived;

    /**
     * The initiator of disappearing chats
     */
    @JsonProperty("17")
    @JsonPropertyDescription("DisappearingMode")
    private ChatDisappear disappearInitiator;

    /**
     * The number of unread messages in this chat that have a mention to the user linked to this session.
     * If this field is negative, this chat is marked as unread.
     */
    @JsonProperty("18")
    @JsonPropertyDescription("uint32")
    private int unreadMentions;

    /**
     * Indicates whether this chat was manually marked as unread
     */
    @JsonProperty("19")
    @JsonPropertyDescription("bool")
    private boolean markedAsUnread;

    /**
     * If this chat is a group, this field is populated with the participants of this group
     */
    @JsonProperty("20")
    @JsonPropertyDescription("GroupParticipant")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<GroupParticipant> participants;

    /**
     * The token of this chat
     */
    @JsonProperty("21")
    @JsonPropertyDescription("bytes")
    @NonNull
    private byte[] token;

    /**
     * The timestamp of the token of this chat
     */
    @JsonProperty(value = "22")
    @JsonPropertyDescription("uint64")
    private long tokenTimestamp;

    /**
     * The public identity key of this
     */
    @JsonProperty("23")
    @JsonPropertyDescription("bytes")
    @NonNull
    private byte[] identityKey;

    /**
     * The time in seconds since {@link java.time.Instant#EPOCH} when this chat was pinned to the top.
     * If the chat isn't pinned, this field has a value of 0.
     */
    @JsonProperty("24")
    @JsonPropertyDescription("uint32")
    private long pinned;

    /**
     * The mute status of this chat
     */
    @JsonProperty("25")
    @JsonPropertyDescription("uint64")
    @NonNull
    private ChatMute mute;

    /**
     * The wallpaper of this chat
     */
    @JsonProperty("26")
    @JsonPropertyDescription("WallpaperSettings")
    private ChatWallpaper wallpaper;

    /**
     * The type of this media visibility set for this chat
     */
    @JsonProperty("27")
    @JsonPropertyDescription("MediaVisibility")
    @Default
    @NonNull
    private ChatMediaVisibility mediaVisibility = ChatMediaVisibility.DEFAULT;

    /**
     * The timestamp of the sender of the token of this chat
     */
    @JsonProperty("28")
    @JsonPropertyDescription("uint64")
    private long tokenSenderTimestamp;

    /**
     * Whether this chat was suspended and therefore cannot be accessed anymore
     */
    @JsonProperty("29")
    @JsonPropertyDescription("bool")
    private boolean suspended;

    /**
     * A map that holds the status of each participant, excluding yourself, for this chat.
     * If the chat is not a group, this map's size will range from 0 to 1.
     * Otherwise, it will range from 0 to the number of participants - 1.
     * It is important to remember that is not guaranteed that every participant will be present as a key.
     * In this case, if this chat is a group, it can be safely assumed that the user is not available.
     * Otherwise, it's recommended to use {@link Whatsapp#subscribeToContactPresence(Contact)} to force Whatsapp to send updates regarding the status of the other participant.
     * It's also possible to listen for updates to a contact's presence in a group or in a conversation by implementing {@link WhatsappListener#onContactPresenceUpdate}.
     * The presence that this map indicates might not line up with {@link Contact#lastKnownPresence()} if the contact is composing, recording or paused.
     * This is because a contact can be online on Whatsapp and composing, recording or paused in a specific chat.
     */
    @Default
    private Map<Contact, ContactStatus> presences = new HashMap<>();

    /**
     * Returns a boolean to represent whether this chat is a group or not
     *
     * @return true if this chat is a group
     */
    public boolean isGroup() {
        return jid.type() == ContactJid.Type.GROUP;
    }

    /**
     * Returns a boolean to represent whether this chat is pinned or not
     *
     * @return true if this chat is pinned
     */
    public boolean isPinned() {
        return pinned != 0;
    }

    /**
     * Returns a boolean to represent whether ephemeral messages are enabled for this chat
     *
     * @return true if ephemeral messages are enabled for this chat
     */
    public boolean isEphemeral() {
        return ephemeralMessageDuration != 0 && ephemeralMessagesToggleTime != 0;
    }

    /**
     * Returns a boolean to represent whether this chat has a new jid
     *
     * @return true if this chat has a new jid
     */
    public boolean hasNewJid() {
        return newJid != null;
    }

    /**
     * Returns a boolean to represent whether this chat has unread messages
     *
     * @return true if this chat has unread messages
     */
    public boolean hasUnreadMessages() {
        return unreadMessages == 0 && unreadMentions == 0;
    }

    /**
     * Returns an optional value containing the new jid of this chat
     *
     * @return a non-empty optional if the new jid is not null
     */
    public Optional<ContactJid> newJid() {
        return Optional.ofNullable(newJid);
    }

    /**
     * Returns an optional value containing the old jid of this chat
     *
     * @return a non-empty optional if the old jid is not null
     */
    public Optional<ContactJid> oldJid() {
        return Optional.ofNullable(newJid);
    }

    /**
     * Returns an optional value containing the disappearing status of this chat
     *
     * @return a non-empty optional if the disappearing status of this chat is not null
     */
    public Optional<ChatDisappear> disappearInitiator() {
        return Optional.ofNullable(disappearInitiator);
    }

    /**
     * Returns an optional value containing the participants of this chat, if it is a group
     *
     * @return a non-empty optional if this chat is a group
     */
    public Optional<List<GroupParticipant>> participants() {
        return Optional.ofNullable(participants);
    }

    /**
     * Returns an optional value containing the wallpaper of this chat, if any is set
     *
     * @return a non-empty optional if this chat has a custom wallpaper
     */
    public Optional<ChatWallpaper> wallpaper() {
        return Optional.ofNullable(wallpaper);
    }

    /**
     * Returns an optional value containing the time this chat was pinned
     *
     * @return a non-empty optional if the chat is pinned
     */
    public Optional<ZonedDateTime> pinned() {
        return WhatsappUtils.parseWhatsappTime(pinned);
    }

    /**
     * Returns an optional value containing the time in seconds before a message is automatically deleted from this chat both locally and from WhatsappWeb's servers
     *
     * @return a non-empty optional if ephemeral messages are enabled for this chat
     */
    public Optional<ZonedDateTime> ephemeralMessageDuration() {
        return WhatsappUtils.parseWhatsappTime(ephemeralMessageDuration);
    }

    /**
     * Returns an optional value containing the time in seconds since {@link java.time.Instant#EPOCH} when ephemeral messages were turned on
     *
     * @return a non-empty optional if ephemeral messages are enabled for this chat
     */
    public Optional<ZonedDateTime> ephemeralMessagesToggleTime() {
        return WhatsappUtils.parseWhatsappTime(ephemeralMessagesToggleTime);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     *
     * @return a non-empty optional if {@link Chat#messages} isn't empty
     */
    public Optional<MessageInfo> lastMessage() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     *
     * @return a non-empty optional if {@link Chat#messages} isn't empty
     */
    public Optional<MessageInfo> firstMessage() {
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    // Just a linker
    @JsonSetter("17")
    private void unwrapDisappearingMode(Map<String, Object> wrapper) {
        this.disappearInitiator = (ChatDisappear) wrapper.get("1");
    }
}
