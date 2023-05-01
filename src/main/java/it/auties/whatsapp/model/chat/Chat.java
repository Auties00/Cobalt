package it.auties.whatsapp.model.chat;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.listener.Listener;
import it.auties.whatsapp.model.contact.Contact;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.contact.ContactStatus;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.sync.HistorySyncMessage;
import it.auties.whatsapp.util.Clock;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.auties.protobuf.base.ProtobufType.*;
import static java.util.Objects.requireNonNullElse;

/**
 * A model class that represents a Chat. A chat can be of two types: a conversation with a contact
 * or a group. This class is only a model, this means that changing its values will have no real
 * effect on WhatsappWeb's servers. This class also offers a builder, accessible using
 * {@link Chat#builder()}.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("Conversation")
public final class Chat implements ProtobufMessage, ContactJidProvider {
    /**
     * The unique id of this chat
     */
    @NonNull
    @Default
    private final UUID uuid = UUID.randomUUID();

    /**
     * The non-null unique jid used to identify this chat
     */
    @ProtobufProperty(index = 1, type = STRING)
    @NonNull
    private final ContactJid jid;

    /**
     * The nullable new unique jid for this Chat. This field is not null when a contact changes phone
     * number and connects their new phone number with Whatsapp.
     */
    @ProtobufProperty(index = 3, type = STRING)
    private final ContactJid newJid;

    /**
     * The nullable old jid for this Chat. This field is not null when a contact changes phone number
     * and connects their new phone number with Whatsapp.
     */
    @ProtobufProperty(index = 4, type = STRING)
    private final ContactJid oldJid;

    /**
     * The timestamp of this chat
     * Usually refers to the time when it was last modified
     * Doesn't necessarily match {@link Chat#newestMessage()}'s timestamp
     */
    @ProtobufProperty(index = 12, type = UINT64)
    private long timestampSeconds;

    /**
     * A non-null list of messages in this chat sorted chronologically
     */
    @ProtobufProperty(index = 2, type = MESSAGE, implementation = HistorySyncMessage.class, repeated = true)
    @NonNull
    @Default
    private final ConcurrentLinkedDeque<MessageInfo> historySyncMessages = new ConcurrentLinkedDeque<>();

    /**
     * The number of unread messages in this chat. If this field is negative, this chat is marked as
     * unread.
     */
    @ProtobufProperty(index = 6, type = UINT32)
    private int unreadMessagesCount;

    /**
     * Whether this chat is read only
     */
    @ProtobufProperty(index = 7, name = "readOnly", type = BOOL)
    private boolean readOnly;

    /**
     * Whether this chat has been transferred completely
     */
    @ProtobufProperty(index = 8, name = "endOfHistoryTransfer", type = BOOL)
    private boolean endOfHistoryTransfer;

    /**
     * The seconds in seconds before a message is automatically deleted from this chat both locally
     * and from WhatsappWeb's servers. If ephemeral messages aren't enabled, this field has a value of
     * 0
     */
    @ProtobufProperty(index = 9, type = UINT32)
    @Default
    private ChatEphemeralTimer ephemeralMessageDuration = ChatEphemeralTimer.OFF;

    /**
     * The seconds in seconds since {@link java.time.Instant#EPOCH} when ephemeral messages were
     * turned on. If ephemeral messages aren't enabled, this field has a value of 0.
     */
    @ProtobufProperty(index = 10, type = INT64)
    private long ephemeralMessagesToggleTime;

    /**
     * The history sync status
     */
    @ProtobufProperty(index = 11, name = "endOfHistoryTransferType", type = MESSAGE)
    @Default
    private EndOfHistoryTransferType endOfHistoryTransferType = EndOfHistoryTransferType.COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY;

    /**
     * The non-null display name of this chat
     */
    @ProtobufProperty(index = 13, type = STRING)
    private String name;

    /**
     * This field is used to determine whether a chat was marked as being spam or not.
     */
    @ProtobufProperty(index = 15, type = BOOL)
    private boolean notSpam;

    /**
     * This field is used to determine whether a chat is archived or not.
     */
    @ProtobufProperty(index = 16, type = BOOL)
    private boolean archived;

    /**
     * The initiator of disappearing chats
     */
    @ProtobufProperty(index = 17, type = MESSAGE, implementation = ChatDisappear.class)
    private ChatDisappear disappearInitiator;

    /**
     * Whether this chat was manually marked as unread
     */
    @ProtobufProperty(index = 19, name = "markedAsUnread", type = BOOL)
    private boolean markedAsUnread;

    /**
     * The participants of this chat, if it's a group
     */
    @ProtobufProperty(implementation = GroupParticipant.class, index = 20, name = "participant", repeated = true, type = MESSAGE)
    @Default
    private Map<ContactJid, GroupParticipant> participants = new ConcurrentHashMap<>();

    /**
     * The participants that used to be in this chat, if it's a group
     */
    @Default
    private Map<ContactJid, PastParticipant> pastParticipants = new ConcurrentHashMap<>();

    /**
     * The token of this chat
     */
    @ProtobufProperty(index = 21, type = BYTES)
    private byte[] token;

    /**
     * The timestamp of the token of this chat
     */
    @ProtobufProperty(index = 22, type = UINT64)
    private long tokenTimestampSeconds;

    /**
     * The public identity key of this chat
     */
    @ProtobufProperty(index = 23, type = BYTES)
    private byte[] identityKey;

    /**
     * The seconds in seconds since {@link java.time.Instant#EPOCH} when this chat was pinned to the
     * top. If the chat isn't pinned, this field has a value of 0.
     */
    @ProtobufProperty(index = 24, type = UINT32)
    private int pinnedTimestampSeconds;

    /**
     * The mute status of this chat
     */
    @ProtobufProperty(index = 25, type = UINT64)
    @NonNull
    @Default
    private ChatMute mute = ChatMute.notMuted();

    /**
     * The wallpaper of this chat
     */
    @ProtobufProperty(index = 26, type = MESSAGE, implementation = ChatWallpaper.class)
    private ChatWallpaper wallpaper;

    /**
     * The type of this media visibility set for this chat
     */
    @ProtobufProperty(index = 27, type = MESSAGE, implementation = ChatMediaVisibility.class)
    @NonNull
    @Default
    private ChatMediaVisibility mediaVisibility = ChatMediaVisibility.OFF;

    /**
     * The timestamp of the sender of the token of this chat
     */
    @ProtobufProperty(index = 28, type = UINT64)
    private long tokenSenderTimestampSeconds;

    /**
     * Whether this chat was suspended and therefore cannot be accessed anymore
     */
    @ProtobufProperty(index = 29, type = BOOL)
    private boolean suspended;

    /**
     * Whether this chat was terminated
     */
    @ProtobufProperty(index = 30, name = "terminated", type = BOOL)
    private boolean terminated;

    /**
     * The timestamp at which the chat, if a group, was created
     */
    @ProtobufProperty(index = 31, name = "createdAt", type = UINT64)
    private long foundationTimestampSeconds;

    /**
     * The user who created this chat, if a group
     */
    @ProtobufProperty(index = 32, name = "createdBy", type = STRING)
    private ContactJid founder;

    /**
     * The description of this chat, if a group
     */
    @ProtobufProperty(index = 33, name = "description", type = STRING)
    private String description;

    /**
     * Whether this chat is an official support chat from Whatsapp
     */
    @ProtobufProperty(index = 34, name = "support", type = BOOL)
    private boolean support;

    /**
     * Whether this chat is a parent group
     */
    @ProtobufProperty(index = 35, name = "isParentGroup", type = BOOL)
    private boolean parentGroup;

    /**
     * Whether this chat is a default subgroup
     */
    @ProtobufProperty(index = 36, name = "isDefaultSubgroup", type = BOOL)
    private boolean defaultSubGroup;

    /**
     * The parent group's jid in a community
     */
    @ProtobufProperty(index = 37, name = "parentGroupId", type = STRING)
    private ContactJid parentGroupJid;

    /**
     * Experimental
     */
    @ProtobufProperty(index = 38, name = "displayName", type = STRING)
    private String displayName;

    /**
     * Experimental
     */
    @ProtobufProperty(index = 39, name = "pnJid", type = STRING)
    private ContactJid pnJid;

    /**
     * Experimental
     */
    @ProtobufProperty(index = 40, name = "shareOwnPn", type = BOOL)
    private boolean shareOwnPn;

    /**
     * Experimental
     */
    @ProtobufProperty(index = 41, name = "pnhDuplicateLidThread", type = BOOL)
    private boolean pnhDuplicateLidThread;

    /**
     * Experimental
     */
    @ProtobufProperty(index = 42, name = "lidJid", type = STRING)
    private ContactJid lidJid;

    /**
     * A toMap that holds the status of each participant, excluding yourself, for this chat. If the
     * chat is not a group, this toMap's size will range from 0 to 1. Otherwise, it will range from 0
     * to the number of participants - 1. It is important to remember that is not guaranteed that
     * every participant will be present as a key. In this case, if this chat is a group, it can be
     * safely assumed that the user is not available. Otherwise, it's recommended to use
     * {@link Whatsapp#subscribeToPresence(ContactJidProvider)} to force Whatsapp to send updates
     * regarding the status of the other participant. It's also possible to listen for updates to a
     * contact's presence in a group or in a conversation by implementing
     * {@link Listener#onContactPresence}. The presence that this toMap indicates might not line up
     * with {@link Contact#lastKnownPresence()} if the contact is composing, recording or paused. This
     * is because a contact can be online on Whatsapp and composing, recording or paused in a specific
     * chat.
     */
    @Default
    @NonNull
    private ConcurrentHashMap<ContactJid, ContactStatus> presences = new ConcurrentHashMap<>();

    /**
     * A set that hold all the jids of the participants in this chat that have received pre keys. This
     * set is only used if the chat is a group chat. It's not important for anything other than
     * message ciphering.
     */
    @Default
    @NonNull
    private Set<ContactJid> participantsPreKeys = new HashSet<>();

    /**
     * Constructs a chat from a jid
     *
     * @param jid the non-null jid
     * @return a non-null chat
     */
    public static Chat ofJid(@NonNull ContactJid jid) {
        return Chat.builder().jid(jid).build();
    }

    /**
     * Returns the name of this chat
     *
     * @return a non-null string
     */
    public String name() {
        return requireNonNullElse(name, requireNonNullElse(displayName, jid.user()));
    }

    /**
     * Returns whether this chat has a name. If this method returns false, it doesn't imply that
     * {@link Chat#name()} will return null.
     *
     * @return a boolean
     */
    public boolean hasName() {
        return name != null;
    }

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
        return pinnedTimestampSeconds != 0;
    }

    /**
     * Returns a boolean to represent whether ephemeral messages are enabled for this chat
     *
     * @return true if ephemeral messages are enabled for this chat
     */
    public boolean isEphemeral() {
        return ephemeralMessageDuration != ChatEphemeralTimer.OFF && ephemeralMessagesToggleTime != 0;
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
     * Returns a boolean to represent whether this chat has an old jid
     *
     * @return true if this chat has an old jid
     */
    public boolean hasOldJid() {
        return oldJid != null;
    }

    /**
     * Returns all the unread messages in this chat
     *
     * @return a non-null collection
     */
    public Collection<MessageInfo> unreadMessages() {
        if (!hasUnreadMessages()) {
            return List.of();
        }

        var iterator = historySyncMessages.iterator();
        return IntStream.range(0, unreadMessagesCount).mapToObj(i -> iterator.next()).toList();
    }

    /**
     * Returns a boolean to represent whether this chat has unread messages
     *
     * @return true if this chat has unread messages
     */
    public boolean hasUnreadMessages() {
        return unreadMessagesCount > 0;
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
        return Optional.ofNullable(oldJid);
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
     * Returns an optional value containing the wallpaper of this chat, if any is set
     *
     * @return a non-empty optional if this chat has a custom wallpaper
     */
    public Optional<ChatWallpaper> wallpaper() {
        return Optional.ofNullable(wallpaper);
    }

    /**
     * Returns an optional value containing the seconds this chat was pinned
     *
     * @return a non-empty optional if the chat is pinned
     */
    public ZonedDateTime pinnedTimestamp() {
        return Clock.parseSeconds(pinnedTimestampSeconds);
    }

    /**
     * Returns the timestamp for the creation of this chat in seconds since
     * {@link java.time.Instant#EPOCH}
     *
     * @return a non-empty optional if this field is populated
     */
    public ZonedDateTime timestamp() {
        return Clock.parseSeconds(timestampSeconds);
    }

    /**
     * Returns an optional value containing the seconds in seconds since
     * {@link java.time.Instant#EPOCH} when ephemeral messages were turned on
     *
     * @return a non-empty optional if ephemeral messages are enabled for this chat
     */
    public ZonedDateTime ephemeralMessagesToggleTime() {
        return Clock.parseSeconds(ephemeralMessagesToggleTime);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestMessage() {
        return Optional.ofNullable(historySyncMessages.peekLast());
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestStandardMessage() {
        return historySyncMessages.stream()
                .filter(info -> !info.message().hasCategory(MessageCategory.SERVER) && !info.hasStub())
                .reduce((first, second) -> second);

    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestMessageFromMe() {
        return historySyncMessages.stream()
                .filter(info -> !info.message().hasCategory(MessageCategory.SERVER) && !info.hasStub())
                .filter(MessageInfo::fromMe)
                .reduce((first, second) -> second);
    }

    /**
     * Returns an optional value containing the latest message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<MessageInfo> newestServerMessage() {
        return historySyncMessages.stream()
                .filter(info -> info.message().hasCategory(MessageCategory.SERVER) || info.hasStub())
                .reduce((first, second) -> second);
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestMessage() {

        return Optional.ofNullable(historySyncMessages.peekFirst());

    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * sent from you
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestMessageFromMe() {

        return historySyncMessages.stream()
                .filter(info -> !info.message().hasCategory(MessageCategory.SERVER) && !info.hasStub())
                .filter(MessageInfo::fromMe)
                .findFirst();
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type that isn't server
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestStandardMessage() {

        return historySyncMessages.stream()
                .filter(info -> !info.message().hasCategory(MessageCategory.SERVER) && !info.hasStub())
                .findFirst();
    }

    /**
     * Returns an optional value containing the first message in chronological terms for this chat
     * with type server
     *
     * @return an optional
     */
    public Optional<MessageInfo> oldestServerMessage() {

        return historySyncMessages.stream()
                .filter(info -> info.message().hasCategory(MessageCategory.SERVER) || info.hasStub())
                .findFirst();
    }

    /**
     * Returns all the starred messages in this chat
     *
     * @return a non-null list of messages
     */
    public Collection<MessageInfo> starredMessages() {
        return historySyncMessages.stream()
                .filter(MessageInfo::starred)
                .toList();
    }

    /**
     * Returns the token for this chat
     *
     * @return a non-null optional value
     */
    public Optional<byte[]> token() {
        return Optional.ofNullable(token);
    }

    /**
     * Returns the timestamp for the creation of this chat's token
     *
     * @return a non-null optional value
     */
    public ZonedDateTime tokenTimestamp() {
        return Clock.parseSeconds(tokenTimestampSeconds);
    }

    /**
     * Returns the identity token for this chat
     *
     * @return a non-null optional value
     */
    public Optional<byte[]> identityKey() {
        return Optional.ofNullable(identityKey);
    }

    /**
     * Returns the timestamp for the token sender creation of this chat
     *
     * @return a non-null optional value
     */
    public ZonedDateTime tokenSenderTimestamp() {
        return Clock.parseSeconds(tokenSenderTimestampSeconds);
    }

    /**
     * Returns the timestamp for the creation of this chat if it's a group
     *
     * @return a non-null optional value
     */
    public ZonedDateTime foundationTimestamp() {
        return Clock.parseSeconds(foundationTimestampSeconds);
    }

    /**
     * Returns the contact who created this chat if it's a group
     * This method only works if {@link Whatsapp#queryGroupMetadata(ContactJidProvider)} has been called before on this chat.
     * By default, all groups that have been used in the last two weeks wil be synced automatically
     *
     * @return a non-null optional value
     */
    public Optional<ContactJid> founder() {
        return Optional.ofNullable(founder);
    }

    /**
     * Returns the description of this chat if it's a group
     * This method only works if {@link Whatsapp#queryGroupMetadata(ContactJidProvider)} has been called before on this chat.
     * By default, all groups that have been used in the last two weeks wil be synced automatically
     *
     * @return a non-null optional value
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the pn jid
     * Experimental
     *
     * @return a non-null optional value
     */
    public Optional<ContactJid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Returns the lid jid
     * Experimental
     *
     * @return a non-null optional value
     */
    public Optional<ContactJid> lidJid() {
        return Optional.ofNullable(lidJid);
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param newMessages the non-null messages to add
     */
    public void addMessages(@NonNull Collection<MessageInfo> newMessages) {
        historySyncMessages.addAll(newMessages);
    }

    /**
     * Adds a new unspecified amount of messages to this chat and sorts them accordingly
     *
     * @param oldMessages the non-null messages to add
     */
    public void addOldMessages(@NonNull Collection<MessageInfo> oldMessages) {
        oldMessages.forEach(historySyncMessages::addFirst);
    }

    /**
     * Adds a message to the chat in the most recent slot available
     *
     * @param info the message to add to the chat
     * @return whether the message was added
     */
    public boolean addNewMessage(@NonNull MessageInfo info) {
        if (historySyncMessages.contains(info)) {
            return false;
        }
        historySyncMessages.add(info);
        updateChatTimestamp(info);
        return true;
    }

    /**
     * Adds a message to the chat in the oldest slot available
     *
     * @param info the message to add to the chat
     * @return whether the message was added
     */
    public boolean addOldMessage(@NonNull MessageInfo info) {
        historySyncMessages.addFirst(info);
        return true;
    }

    /**
     * Remove a message from the chat
     *
     * @param info the message to remove
     * @return whether the message was removed
     */
    public boolean removeMessage(@NonNull MessageInfo info) {

        var result = historySyncMessages.remove(info);
        refreshChatTimestamp();
        return result;
    }

    /**
     * Remove a message from the chat
     *
     * @param predicate the predicate that determines if a message should be removed
     * @return whether the message was removed
     */
    public boolean removeMessage(@NonNull Predicate<? super MessageInfo> predicate) {

        var result = historySyncMessages.removeIf(predicate);
        refreshChatTimestamp();
        return result;
    }

    private void refreshChatTimestamp() {
        var message = newestMessage();
        if(message.isEmpty()){
            return;
        }

        updateChatTimestamp(message.get());
    }

    private void updateChatTimestamp(MessageInfo info) {
        var oldTimeStamp = newestMessage().map(MessageInfo::timestampSeconds).orElse(0L);
        if(oldTimeStamp > info.timestampSeconds()){
            return;
        }
        timestampSeconds(info.timestampSeconds());
    }

    /**
     * Removes all messages from the chat
     */
    public void removeMessages() {
        historySyncMessages.clear();
    }

    /**
     * Returns an immutable list of all the messages in this chat
     *
     * @return a non-null collection
     */
    @JsonGetter
    public Collection<MessageInfo> messages() {
        return Collections.unmodifiableCollection(historySyncMessages);
    }

    /**
     * Returns an immutable list of messages wrapped in history syncs
     * This is useful for the proto
     *
     * @return a non-null collection
     */
    public Collection<HistorySyncMessage> historySyncMessages(){
        var iterator = historySyncMessages.iterator();
        var syncs = new ArrayList<HistorySyncMessage>(historySyncMessages.size());
        var index = 0;
        while(iterator.hasNext()) {
            syncs.add(new HistorySyncMessage(iterator.next(), index++));
        }

        return Collections.unmodifiableList(syncs);
    }

    /**
     * Returns an immutable list representing the participants of this chat if it's a group
     *
     * @return a non-null collection
     */
    public Collection<GroupParticipant> participants() {
        return participants.values();
    }

    /**
     * Adds a participant to this chat
     *
     * @param jid the non-null jid of the participant
     * @param role the role of the participant
     * @return the old value associated with the provided jid
     */
    public Optional<GroupParticipant> addParticipant(@NonNull ContactJid jid, GroupRole role){
        return Optional.ofNullable(participants.put(jid, new GroupParticipant(jid, role)));
    }

    /**
     * Adds a participant to this chat
     *
     * @param participant the non-null participant
     * @return the old value associated with the provided jid
     */
    public Optional<GroupParticipant> addParticipant(@NonNull GroupParticipant participant){
        return Optional.ofNullable(participants.put(participant.jid(), participant));
    }

    /**
     * Removes a participant from this chat
     *
     * @param jid the non-null jid of the participant
     * @return the old value associated with the provided jid
     */
    public Optional<GroupParticipant> removeParticipant(@NonNull ContactJid jid){
        return Optional.ofNullable(participants.remove(jid));
    }

    /**
     * Finds a participant by jid
     * This method only works if {@link Whatsapp#queryGroupMetadata(ContactJidProvider)} has been called before on this chat.
     * By default, all groups that have been used in the last two weeks wil be synced automatically
     *
     * @param jid the non-null jid of the participant
     * @return the participant, if present
     */
    public Optional<GroupParticipant> findParticipant(@NonNull ContactJid jid){
        return Optional.ofNullable(participants.get(jid));
    }

    /**
     * Returns an immutable list representing the pastParticipants of this chat if it's a group
     *
     * @return a non-null collection
     */
    public Collection<PastParticipant> pastParticipants() {
        return pastParticipants.values();
    }

    /**
     * Adds a past participant
     *
     * @param participant the non-null jid of the past participant
     * @return the old value associated with the provided jid
     */
    public Optional<PastParticipant> addPastParticipant(@NonNull PastParticipant participant){
        return Optional.ofNullable(pastParticipants.put(participant.jid(), participant));
    }

    /**
     * Adds a collection of past participants
     *
     * @param pastParticipants the non-null list of past participants
     */
    public void addPastParticipants(List<PastParticipant> pastParticipants) {
        for(var pastParticipant : pastParticipants){
            this.pastParticipants.put(pastParticipant.jid(), pastParticipant);
        }
    }

    /**
     * Removes a past participant
     *
     * @param jid the non-null jid of the past participant
     * @return the old value associated with the provided jid
     */
    public Optional<PastParticipant> removePastParticipant(@NonNull ContactJid jid){
        return Optional.ofNullable(pastParticipants.remove(jid));
    }

    /**
     * Finds a past participant by jid
     *
     * @param jid the non-null jid of the past participant
     * @return the past participant, if present
     */
    public Optional<PastParticipant> findPastParticipant(@NonNull ContactJid jid){
        return Optional.ofNullable(pastParticipants.get(jid));
    }

    /**
     * Checks if this chat is equal to another chat
     *
     * @param other the chat
     * @return a boolean
     */
    public boolean equals(Object other) {
        return (other instanceof Chat that) && Objects.equals(this.jid(), that.jid());
    }

    /**
     * Returns this object as a jid
     *
     * @return a non-null jid
     */
    @Override
    @NonNull
    public ContactJid toJid() {
        return jid();
    }

    /**
     * Returns the hash code for this chat
     *
     * @return an int
     */
    @Override
    public int hashCode() {
        return Objects.hash(jid());
    }

    /**
     * Returns the hash code for this chat using all fields.
     * This is useful to check if two chats are exactly the same.
     *
     * @return an int
     */
    public int fullHashCode() {
        int result = Objects.hash(jid, newJid, oldJid, timestampSeconds, historySyncMessages, unreadMessagesCount, readOnly, endOfHistoryTransfer, ephemeralMessageDuration, ephemeralMessagesToggleTime, endOfHistoryTransferType, name, notSpam, archived, disappearInitiator, markedAsUnread, participants, pastParticipants, tokenTimestampSeconds, pinnedTimestampSeconds, mute, wallpaper, mediaVisibility, tokenSenderTimestampSeconds, suspended, terminated, foundationTimestampSeconds, founder, description, support, parentGroup, defaultSubGroup, parentGroupJid, displayName, pnJid, shareOwnPn, pnhDuplicateLidThread, lidJid, presences, participantsPreKeys);
        result = 31 * result + Arrays.hashCode(token);
        result = 31 * result + Arrays.hashCode(identityKey);
        result = 31 * result + historySyncMessages.size();
        return result;
    }

    @Override
    public String toString() {
        return "Chat{" + "uuid=" + uuid + ", jid=" + jid + ", newJid=" + newJid + ", oldJid=" + oldJid + ", timestampSeconds=" + timestampSeconds + ", messages=" + historySyncMessages + ", unreadMessagesCount=" + unreadMessagesCount + ", readOnly=" + readOnly + ", endOfHistoryTransfer=" + endOfHistoryTransfer + ", ephemeralMessageDuration=" + ephemeralMessageDuration + ", ephemeralMessagesToggleTime=" + ephemeralMessagesToggleTime + ", endOfHistoryTransferType=" + endOfHistoryTransferType + ", name='" + name + '\'' + ", notSpam=" + notSpam + ", archived=" + archived + ", disappearInitiator=" + disappearInitiator + ", markedAsUnread=" + markedAsUnread + ", participants=" + participants + ", pastParticipants=" + pastParticipants + ", token=" + Arrays.toString(token) + ", tokenTimestamp=" + tokenTimestampSeconds + ", identityKey=" + Arrays.toString(identityKey) + ", pinnedTimestampSeconds=" + pinnedTimestampSeconds + ", mute=" + mute + ", wallpaper=" + wallpaper + ", mediaVisibility=" + mediaVisibility + ", tokenSenderTimestamp=" + tokenSenderTimestampSeconds + ", suspended=" + suspended + ", terminated=" + terminated + ", createdAt=" + foundationTimestampSeconds + ", createdBy=" + founder + ", description='" + description + '\'' + ", support=" + support + ", parentGroup=" + parentGroup + ", defaultSubGroup=" + defaultSubGroup + ", parentGroupJid=" + parentGroupJid + ", displayName='" + displayName + '\'' + ", pnJid=" + pnJid + ", shareOwnPn=" + shareOwnPn + ", pnhDuplicateLidThread=" + pnhDuplicateLidThread + ", lidJid=" + lidJid + ", presences=" + presences + ", participantsPreKeys=" + participantsPreKeys + '}';
    }

    /**
     * The constants of this enumerated type describe the various types of trasnfers that can regard a
     * chat history sync
     */
    @AllArgsConstructor
    @Accessors(fluent = true)
    public enum EndOfHistoryTransferType implements ProtobufMessage {
        /**
         * Complete, but more messages remain on the phone
         */
        COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY(0),

        /**
         * Complete and no more messages remain on the phone
         */
        COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY(1);

        @Getter
        private final int index;
    }

    /**
     * Internal implementation to deserialize messages
     */
    public static class ChatBuilder {
        public ChatBuilder participants(Collection<GroupParticipant> participants){
            this.participants$set = true;
            this.participants$value = participants.stream().collect(Collectors.toConcurrentMap(GroupParticipant::jid, Function.identity()));
            return this;
        }

        public ChatBuilder historySyncMessages(ConcurrentLinkedDeque<HistorySyncMessage> messages) {
            this.historySyncMessages$value = messages.stream()
                    .sorted(Comparator.comparing(HistorySyncMessage::messageOrderId))
                    .map(HistorySyncMessage::message)
                    .collect(Collectors.toCollection(ConcurrentLinkedDeque::new));
            this.historySyncMessages$set = true;
            return this;
        }

        @JsonSetter("historySyncMessages")
        public ChatBuilder messages(@NonNull ConcurrentLinkedDeque<MessageInfo> messages) {
            this.historySyncMessages$value = messages;
            this.historySyncMessages$set = true;
            return this;
        }
    }
}