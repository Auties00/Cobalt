package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.media.MediaVisibility;
import com.github.auties00.cobalt.wire.linked.message.PrivacySystemMessage;
import com.github.auties00.cobalt.wire.core.mixin.InstantMillisMixin;
import com.github.auties00.cobalt.wire.core.mixin.InstantSecondsMixin;
import com.github.auties00.cobalt.wire.linked.setting.WallpaperSettings;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a single conversation (chat) inside WhatsApp and the entire set
 * of state attached to it.
 *
 * <p>A chat is the central entity of the WhatsApp model: it can describe a
 * one-to-one conversation, a group, a community sub-group, a broadcast list
 * or a newsletter, and is uniquely identified by a {@link Jid}. Each chat
 * carries the metadata that WhatsApp synchronizes across a user's devices
 * via history sync, including the message list, unread counters, mute and
 * pin state, the disappearing-messages timer, the participant list (for
 * groups), wallpaper customization, and privacy controls such as limit
 * sharing and chat lock.
 *
 * <p>This class is abstract because the storage strategy for the message
 * list is left to concrete subclasses: history-sync payloads use a backing
 * map of envelopes, the in-memory store uses a sequenced collection, and
 * other implementations may delegate to a database. The abstract methods
 * {@link #messages()}, {@link #addMessage(ChatMessageInfo)},
 * {@link #removeMessage(String)}, {@link #removeMessages()},
 * {@link #getMessageById(String)}, {@link #newestMessage()} and
 * {@link #oldestMessage()} let each subclass plug its own storage in
 * without forcing a particular collection type onto the protocol layer.
 *
 * @see ChatMessageInfo
 * @see ChatMute
 * @see ChatEphemeralTimer
 */
@ProtobufMessage(name = "Conversation")
public abstract class Chat implements JidProvider {
    /**
     * The JID that uniquely identifies this chat. For one-to-one chats this
     * is the contact's phone-number JID; for groups it is a JID ending in
     * {@code @g.us}; for newsletters it ends in {@code @newsletter}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    private Jid jid;

    // Messages are not deserialized by default because it's up to the implementation class to decide how to do so
    // @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    // private ... messages;

    /**
     * The new JID assigned to this chat after a JID migration (for example
     * when a contact changes phone number). When present, future messages
     * should be associated with this JID.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    private Jid newJid;

    /**
     * The previous JID this chat held before a JID migration. Retained so
     * that messages directed at the old JID can still be correlated with
     * this conversation.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    private Jid oldJid;

    /**
     * The instant of the most recent message in this chat. Used to sort
     * conversations in the chat list.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant lastMsgTimestamp;

    /**
     * The number of unread messages in this chat. {@code 0} means the chat
     * has been fully read.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.UINT32)
    private Integer unreadCount;

    /**
     * Whether this chat is read-only, meaning the current user cannot send
     * messages to it. Typically set on certain system chats and on
     * newsletter channels where the user is only a subscriber.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    private Boolean readOnly;

    /**
     * Whether the history-sync transfer for this chat has completed. When
     * {@code true}, the companion device has already received all available
     * history for this conversation.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.BOOL)
    private Boolean endOfHistoryTransfer;

    /**
     * The disappearing-messages timer for this chat. Determines how long
     * messages persist before being automatically deleted.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.UINT32)
    private ChatEphemeralTimer ephemeralExpiration;

    /**
     * The instant at which the disappearing-messages setting was last
     * changed for this chat.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    private Instant ephemeralSettingTimestamp;

    /**
     * The completion state of the history transfer for this chat: complete
     * with or without more messages remaining on the primary device,
     * incomplete, or omitted from the sync entirely.
     */
    @ProtobufProperty(index = 11, type = ProtobufType.ENUM)
    private EndOfHistoryTransferType endOfHistoryTransferType;

    /**
     * The instant of the most recent activity in this conversation. May
     * differ from {@link #lastMsgTimestamp} as it can include
     * non-message events.
     */
    @ProtobufProperty(index = 12, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant conversationTimestamp;

    /**
     * The server-provided name for this chat. For groups this is the group
     * subject; for contacts it may be the saved contact name or push name.
     */
    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    private String name;

    /**
     * A hash of the chat's participant list, used by the server to detect
     * changes in group membership without comparing the full list.
     */
    @ProtobufProperty(index = 14, type = ProtobufType.STRING)
    private String pHash;

    /**
     * Whether this chat has been explicitly marked as not spam by the user.
     * When {@code true} the chat bypasses spam-detection heuristics.
     */
    @ProtobufProperty(index = 15, type = ProtobufType.BOOL)
    private Boolean notSpam;

    /**
     * Whether this chat is archived. Archived chats are hidden from the
     * main chat list but are not deleted.
     */
    @ProtobufProperty(index = 16, type = ProtobufType.BOOL)
    private Boolean archived;

    /**
     * The disappearing-mode metadata for this chat: who initiated the
     * disappearing-messages setting and what triggered the change.
     */
    @ProtobufProperty(index = 17, type = ProtobufType.MESSAGE)
    private ChatDisappearingMode disappearingMode;

    /**
     * The number of unread messages in this chat that mention the current
     * user, used to render a separate mention badge in the chat list.
     */
    @ProtobufProperty(index = 18, type = ProtobufType.UINT32)
    private Integer unreadMentionCount;

    /**
     * Whether the user has manually marked this chat as unread, regardless
     * of whether unread messages actually exist. Persists until the user
     * opens the chat or marks it as read.
     */
    @ProtobufProperty(index = 19, type = ProtobufType.BOOL)
    private Boolean markedAsUnread;

    /**
     * The participant list for this group chat. Empty for one-to-one chats.
     * Each entry includes the member's JID and their admin status.
     */
    @ProtobufProperty(index = 20, type = ProtobufType.MESSAGE)
    private List<GroupParticipant> participant;

    /**
     * An opaque token used by the server for trust and compliance (T&C)
     * verification of this chat. Must be echoed back in certain protocol
     * exchanges.
     */
    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    private byte[] tcToken;

    /**
     * The instant at which the trust-and-compliance token was issued.
     */
    @ProtobufProperty(index = 22, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant tcTokenTimestamp;

    /**
     * The primary Signal-protocol identity key of the contact in this
     * one-to-one chat, used for safety-number computation and identity
     * verification.
     */
    @ProtobufProperty(index = 23, type = ProtobufType.BYTES)
    private byte[] contactPrimaryIdentityKey;

    /**
     * The instant at which this chat was pinned. When non-{@code null} the
     * chat appears in the pinned section at the top of the chat list;
     * {@code null} means the chat is not pinned.
     */
    @ProtobufProperty(index = 24, type = ProtobufType.UINT32, mixins = InstantSecondsMixin.class)
    private Instant pinnedTimestamp;

    /**
     * The mute state of this chat, controlling whether notifications are
     * silenced. See {@link ChatMute} for the possible states.
     */
    @ProtobufProperty(index = 25, type = ProtobufType.UINT64)
    private ChatMute mute;

    /**
     * The custom wallpaper settings for this chat. Overrides the global
     * wallpaper preference when present.
     */
    @ProtobufProperty(index = 26, type = ProtobufType.MESSAGE)
    private WallpaperSettings wallpaper;

    /**
     * The media-visibility setting for this chat, controlling whether
     * media from this conversation appears in the device gallery or
     * media picker.
     */
    @ProtobufProperty(index = 27, type = ProtobufType.ENUM)
    private MediaVisibility mediaVisibility;

    /**
     * The instant at which the trust-and-compliance token was sent by the
     * message sender.
     */
    @ProtobufProperty(index = 28, type = ProtobufType.UINT64, mixins = InstantMillisMixin.class)
    private Instant tcTokenSenderTimestamp;

    /**
     * Whether this group chat is suspended. Members of a suspended group
     * cannot send or receive messages.
     */
    @ProtobufProperty(index = 29, type = ProtobufType.BOOL)
    private Boolean suspended;

    /**
     * Whether this group chat is terminated. Terminated groups are
     * permanently closed and can no longer be used.
     */
    @ProtobufProperty(index = 30, type = ProtobufType.BOOL)
    private Boolean terminated;

    /**
     * The instant at which this chat was created on the server. For groups
     * this is the group-creation moment; carried as epoch seconds on the
     * wire.
     */
    @ProtobufProperty(index = 31, type = ProtobufType.UINT64, mixins = InstantSecondsMixin.class)
    private Instant createdAt;

    /**
     * The JID or display name of the user who created this group chat.
     */
    @ProtobufProperty(index = 32, type = ProtobufType.STRING)
    private String createdBy;

    /**
     * The description of this group or community chat.
     */
    @ProtobufProperty(index = 33, type = ProtobufType.STRING)
    private String description;

    /**
     * Whether this is a WhatsApp support chat. Support chats receive
     * special handling in the UI and may use different message-processing
     * rules.
     */
    @ProtobufProperty(index = 34, type = ProtobufType.BOOL)
    private Boolean support;

    /**
     * Whether this group is the parent group of a WhatsApp Community.
     * Parent groups serve as the top-level container that links multiple
     * sub-groups.
     */
    @ProtobufProperty(index = 35, type = ProtobufType.BOOL)
    private Boolean isParentGroup;

    /**
     * The identifier of the parent community group that this sub-group
     * belongs to. Only present for sub-groups within a community.
     */
    @ProtobufProperty(index = 37, type = ProtobufType.STRING)
    private String parentGroupId;

    /**
     * Whether this group is the default sub-group of a community (the
     * "General" group that all community members are automatically added
     * to).
     */
    @ProtobufProperty(index = 36, type = ProtobufType.BOOL)
    private Boolean isDefaultSubgroup;

    /**
     * A display name for this chat, typically set by the server. May
     * differ from the contact name or group subject.
     */
    @ProtobufProperty(index = 38, type = ProtobufType.STRING)
    private String displayName;

    /**
     * The phone-number JID associated with this chat. Provides the
     * phone-number identity alongside the LID in LID-based chats.
     */
    @ProtobufProperty(index = 39, type = ProtobufType.STRING)
    private Jid phoneNumberJid;

    /**
     * Whether the current user's phone number should be shared with the
     * other participant in this LID-based chat. Acts as a wire-protocol
     * carrier from the history-sync payload and is typically propagated
     * to the corresponding contact record rather than read directly from
     * the chat.
     */
    @ProtobufProperty(index = 40, type = ProtobufType.BOOL)
    private Boolean shareOwnPhoneNumber;

    /**
     * Whether this chat has a duplicate LID thread associated with the
     * same phone number, indicating that both a phone-number-based and a
     * LID-based conversation exist for the same contact.
     */
    @ProtobufProperty(index = 41, type = ProtobufType.BOOL)
    private Boolean phoneNumberhDuplicateLidThread;

    /**
     * The Linked Identity (LID) JID for this chat. LIDs are an alternative
     * identity system that WhatsApp uses alongside phone numbers in
     * privacy-sensitive contexts.
     */
    @ProtobufProperty(index = 42, type = ProtobufType.STRING)
    private Jid lid;

    /**
     * The username associated with this chat, when the contact or group
     * uses WhatsApp's username feature.
     */
    @ProtobufProperty(index = 43, type = ProtobufType.STRING)
    private String username;

    /**
     * The origin type of the LID for this chat, indicating how the LID was
     * assigned or derived.
     */
    @ProtobufProperty(index = 44, type = ProtobufType.STRING)
    private String lidOriginType;

    /**
     * The number of comments on this chat. Applicable to newsletter posts
     * and other message types that support threaded comments.
     */
    @ProtobufProperty(index = 45, type = ProtobufType.UINT32)
    private Integer commentsCount;

    /**
     * Whether this chat is locked behind WhatsApp's Chat Lock feature.
     * Locked chats require biometric authentication or a device passcode
     * to access.
     */
    @ProtobufProperty(index = 46, type = ProtobufType.BOOL)
    private Boolean locked;

    /**
     * A privacy system message to insert into this chat, used to display
     * informational banners about privacy changes such as encryption
     * transitions or phone-number hiding.
     */
    @ProtobufProperty(index = 47, type = ProtobufType.ENUM)
    private PrivacySystemMessage systemMessageToInsert;

    /**
     * Whether this group was created via the WhatsApp Cloud API (CAPI).
     * CAPI-created groups may have different capabilities and restrictions
     * compared to groups created from standard client apps.
     */
    @ProtobufProperty(index = 48, type = ProtobufType.BOOL)
    private Boolean capiCreatedGroup;

    /**
     * The account-level LID associated with the current user for this
     * chat. Represents the user's own LID identity in the context of this
     * conversation.
     */
    @ProtobufProperty(index = 49, type = ProtobufType.STRING)
    private Jid accountLid;

    /**
     * Whether limit sharing is enabled for this chat. When {@code true},
     * the user's personal information (profile photo, about, etc.) is
     * restricted from the other participant.
     */
    @ProtobufProperty(index = 50, type = ProtobufType.BOOL)
    private Boolean limitSharing;

    /**
     * The instant at which the limit-sharing setting was last changed for
     * this chat.
     */
    @ProtobufProperty(index = 51, type = ProtobufType.INT64, mixins = InstantMillisMixin.class)
    private Instant limitSharingSettingTimestamp;

    /**
     * The trigger that caused the limit-sharing state to be set for this
     * chat.
     */
    @ProtobufProperty(index = 52, type = ProtobufType.ENUM)
    private ChatLimitSharing.TriggerType limitSharingTrigger;

    /**
     * Whether the current user initiated the limit-sharing change for this
     * chat.
     */
    @ProtobufProperty(index = 53, type = ProtobufType.BOOL)
    private Boolean limitSharingInitiatedByMe;

    /**
     * Whether the Meta AI (Maiba) bot thread is enabled for this chat,
     * letting the Meta AI assistant participate in the conversation.
     */
    @ProtobufProperty(index = 54, type = ProtobufType.BOOL)
    private Boolean maibaAiThreadEnabled;

    /**
     * Constructs a new {@code Chat} with all the specified field values.
     *
     * @param jid                            the chat JID (must not be {@code null})
     * @param newJid                         the new JID after migration, or {@code null}
     * @param oldJid                         the old JID before migration, or {@code null}
     * @param lastMsgTimestamp               the last message timestamp, or {@code null}
     * @param unreadCount                    the unread message count, or {@code null}
     * @param readOnly                       whether the chat is read-only, or {@code null}
     * @param endOfHistoryTransfer           whether history transfer completed, or {@code null}
     * @param ephemeralExpiration            the ephemeral timer, or {@code null}
     * @param ephemeralSettingTimestamp       when the ephemeral setting changed, or {@code null}
     * @param endOfHistoryTransferType       the history transfer completion type, or {@code null}
     * @param conversationTimestamp           the last conversation activity timestamp, or {@code null}
     * @param name                           the chat name, or {@code null}
     * @param pHash                          the participant list hash, or {@code null}
     * @param notSpam                        whether marked as not spam, or {@code null}
     * @param archived                       whether archived, or {@code null}
     * @param disappearingMode               the disappearing mode metadata, or {@code null}
     * @param unreadMentionCount             the unread mention count, or {@code null}
     * @param markedAsUnread                 whether manually marked as unread, or {@code null}
     * @param participant                    the group participant list, or {@code null}
     * @param tcToken                        the T&C token, or {@code null}
     * @param tcTokenTimestamp               the T&C token timestamp, or {@code null}
     * @param contactPrimaryIdentityKey      the contact's identity key, or {@code null}
     * @param pinnedTimestamp                the pin timestamp, or {@code null}
     * @param mute                           the mute state, or {@code null}
     * @param wallpaper                      the wallpaper settings, or {@code null}
     * @param mediaVisibility                the media visibility setting, or {@code null}
     * @param tcTokenSenderTimestamp          the T&C token sender timestamp, or {@code null}
     * @param suspended                      whether the group is suspended, or {@code null}
     * @param terminated                     whether the group is terminated, or {@code null}
     * @param createdAt                      the creation timestamp, or {@code null}
     * @param createdBy                      the creator identifier, or {@code null}
     * @param description                    the chat description, or {@code null}
     * @param support                        whether this is a support chat, or {@code null}
     * @param isParentGroup                  whether this is a community parent group, or {@code null}
     * @param parentGroupId                  the parent community identifier, or {@code null}
     * @param isDefaultSubgroup              whether this is the default sub-group, or {@code null}
     * @param displayName                    the display name, or {@code null}
     * @param phoneNumberJid                 the phone number JID, or {@code null}
     * @param shareOwnPhoneNumber            the share own phone number flag, or {@code null}
     * @param phoneNumberhDuplicateLidThread whether a duplicate LID thread exists, or {@code null}
     * @param lid                            the LID JID, or {@code null}
     * @param username                       the username, or {@code null}
     * @param lidOriginType                  the LID origin type, or {@code null}
     * @param commentsCount                  the comments count, or {@code null}
     * @param locked                         whether the chat is locked, or {@code null}
     * @param systemMessageToInsert          the privacy system message, or {@code null}
     * @param capiCreatedGroup               whether the group was CAPI-created, or {@code null}
     * @param accountLid                     the account LID, or {@code null}
     * @param limitSharing                   whether limit sharing is enabled, or {@code null}
     * @param limitSharingSettingTimestamp    the limit sharing timestamp, or {@code null}
     * @param limitSharingTrigger            the limit sharing trigger, or {@code null}
     * @param limitSharingInitiatedByMe      whether limit sharing was self-initiated, or {@code null}
     * @param maibaAiThreadEnabled           whether the AI thread is enabled, or {@code null}
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    protected Chat(Jid jid, Jid newJid, Jid oldJid, Instant lastMsgTimestamp, Integer unreadCount, Boolean readOnly, Boolean endOfHistoryTransfer, ChatEphemeralTimer ephemeralExpiration, Instant ephemeralSettingTimestamp, EndOfHistoryTransferType endOfHistoryTransferType, Instant conversationTimestamp, String name, String pHash, Boolean notSpam, Boolean archived, ChatDisappearingMode disappearingMode, Integer unreadMentionCount, Boolean markedAsUnread, List<GroupParticipant> participant, byte[] tcToken, Instant tcTokenTimestamp, byte[] contactPrimaryIdentityKey, Instant pinnedTimestamp, ChatMute mute, WallpaperSettings wallpaper, MediaVisibility mediaVisibility, Instant tcTokenSenderTimestamp, Boolean suspended, Boolean terminated, Instant createdAt, String createdBy, String description, Boolean support, Boolean isParentGroup, String parentGroupId, Boolean isDefaultSubgroup, String displayName, Jid phoneNumberJid, Boolean shareOwnPhoneNumber, Boolean phoneNumberhDuplicateLidThread, Jid lid, String username, String lidOriginType, Integer commentsCount, Boolean locked, PrivacySystemMessage systemMessageToInsert, Boolean capiCreatedGroup, Jid accountLid, Boolean limitSharing, Instant limitSharingSettingTimestamp, ChatLimitSharing.TriggerType limitSharingTrigger, Boolean limitSharingInitiatedByMe, Boolean maibaAiThreadEnabled) {
        this.jid = Objects.requireNonNull(jid);
        this.newJid = newJid;
        this.oldJid = oldJid;
        this.lastMsgTimestamp = lastMsgTimestamp;
        this.unreadCount = unreadCount;
        this.readOnly = readOnly;
        this.endOfHistoryTransfer = endOfHistoryTransfer;
        this.ephemeralExpiration = ephemeralExpiration;
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
        this.endOfHistoryTransferType = endOfHistoryTransferType;
        this.conversationTimestamp = conversationTimestamp;
        this.name = name;
        this.pHash = pHash;
        this.notSpam = notSpam;
        this.archived = archived;
        this.disappearingMode = disappearingMode;
        this.unreadMentionCount = unreadMentionCount;
        this.markedAsUnread = markedAsUnread;
        this.participant = participant;
        this.tcToken = tcToken;
        this.tcTokenTimestamp = tcTokenTimestamp;
        this.contactPrimaryIdentityKey = contactPrimaryIdentityKey;
        this.pinnedTimestamp = pinnedTimestamp;
        this.mute = mute;
        this.wallpaper = wallpaper;
        this.mediaVisibility = mediaVisibility;
        this.tcTokenSenderTimestamp = tcTokenSenderTimestamp;
        this.suspended = suspended;
        this.terminated = terminated;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.description = description;
        this.support = support;
        this.isParentGroup = isParentGroup;
        this.parentGroupId = parentGroupId;
        this.isDefaultSubgroup = isDefaultSubgroup;
        this.displayName = displayName;
        this.phoneNumberJid = phoneNumberJid;
        this.shareOwnPhoneNumber = shareOwnPhoneNumber;
        this.phoneNumberhDuplicateLidThread = phoneNumberhDuplicateLidThread;
        this.lid = lid;
        this.username = username;
        this.lidOriginType = lidOriginType;
        this.commentsCount = commentsCount;
        this.locked = locked;
        this.systemMessageToInsert = systemMessageToInsert;
        this.capiCreatedGroup = capiCreatedGroup;
        this.accountLid = accountLid;
        this.limitSharing = limitSharing;
        this.limitSharingSettingTimestamp = limitSharingSettingTimestamp;
        this.limitSharingTrigger = limitSharingTrigger;
        this.limitSharingInitiatedByMe = limitSharingInitiatedByMe;
        this.maibaAiThreadEnabled = maibaAiThreadEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Jid toJid() {
        return jid;
    }

    /**
     * Returns the JID that uniquely identifies this chat.
     *
     * @return the chat JID, never {@code null}
     */
    public Jid jid() {
        return jid;
    }

    /**
     * Returns the messages in this chat as a {@link Stream} of
     * {@link ChatMessageInfo}, in insertion order.
     *
     * <p>The returned stream is {@link AutoCloseable}: implementations that
     * back the chat with an external resource (for example a key-value store)
     * keep that resource open for the lifetime of the stream and release it
     * when the stream is closed. Callers MUST consume the stream inside a
     * try-with-resources block, otherwise the underlying resource is leaked.
     *
     * @return a non-null stream of chat messages, in insertion order
     */
    public abstract Stream<ChatMessageInfo> messages();

    /**
     * Returns the number of messages in this chat.
     *
     * <p>This is provided as a dedicated accessor so that callers do not have
     * to consume the entire {@link #messages()} stream just to count. Both the
     * in-memory and the persistent backing implementations answer this in
     * constant time.
     *
     * @return the number of messages currently stored in this chat
     */
    public abstract int messageCount();

    /**
     * Adds a message to this chat.
     *
     * @param info the message to add
     * @throws NullPointerException if {@code info} is {@code null}
     */
    public abstract void addMessage(ChatMessageInfo info);

    /**
     * Removes the message with the specified key ID from this chat.
     *
     * @param id the message key ID to remove
     * @return {@code true} if a message was removed
     */
    public abstract boolean removeMessage(String id);

    /**
     * Removes all messages from this chat.
     */
    public abstract void removeMessages();

    /**
     * Returns the message with the specified key ID, if present.
     *
     * @param id the message key ID to look up
     * @return an {@code Optional} containing the matching message, or empty
     *         if no message with the given ID exists in this chat
     */
    public abstract Optional<ChatMessageInfo> getMessageById(String id);

    /**
     * Returns the newest (most recently added) message in this chat.
     *
     * @return an {@code Optional} containing the newest message, or empty
     *         if this chat has no messages
     */
    public abstract Optional<ChatMessageInfo> newestMessage();

    /**
     * Returns the oldest (earliest added) message in this chat.
     *
     * @return an {@code Optional} containing the oldest message, or empty
     *         if this chat has no messages
     */
    public abstract Optional<ChatMessageInfo> oldestMessage();

    /**
     * Returns the new JID assigned after a JID migration.
     *
     * @return an {@link Optional} containing the new JID, or empty if no
     *         migration has occurred
     */
    public Optional<Jid> newJid() {
        return Optional.ofNullable(newJid);
    }

    /**
     * Returns the previous JID before a JID migration.
     *
     * @return an {@link Optional} containing the old JID, or empty if no
     *         migration has occurred
     */
    public Optional<Jid> oldJid() {
        return Optional.ofNullable(oldJid);
    }

    /**
     * Returns the timestamp of the most recent message in this chat.
     *
     * @return an {@link Optional} containing the timestamp, or empty if no
     *         messages exist
     */
    public Optional<Instant> lastMsgTimestamp() {
        return Optional.ofNullable(lastMsgTimestamp);
    }

    /**
     * Returns the number of unread messages in this chat.
     *
     * @return an {@link OptionalInt} containing the unread count, or empty
     *         if the count is not available
     */
    public OptionalInt unreadCount() {
        return unreadCount == null ? OptionalInt.empty() : OptionalInt.of(unreadCount);
    }

    /**
     * Returns whether this chat is read-only.
     *
     * @return {@code true} if the current user cannot send messages to this
     *         chat, {@code false} otherwise
     */
    public boolean readOnly() {
        return readOnly != null && readOnly;
    }

    /**
     * Returns whether the history sync transfer has completed for this chat.
     *
     * @return {@code true} if all available history has been transferred
     */
    public boolean endOfHistoryTransfer() {
        return endOfHistoryTransfer != null && endOfHistoryTransfer;
    }

    /**
     * Returns the disappearing messages timer for this chat.
     *
     * @return an {@link Optional} containing the ephemeral timer, or empty
     *         if disappearing messages are not configured
     */
    public Optional<ChatEphemeralTimer> ephemeralExpiration() {
        return Optional.ofNullable(ephemeralExpiration);
    }

    /**
     * Returns the timestamp at which the ephemeral setting was last changed.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not
     *         available
     */
    public Optional<Instant> ephemeralSettingTimestamp() {
        return Optional.ofNullable(ephemeralSettingTimestamp);
    }

    /**
     * Returns the type of history transfer completion for this chat.
     *
     * @return an {@link Optional} containing the transfer type, or empty if
     *         not set
     */
    public Optional<EndOfHistoryTransferType> endOfHistoryTransferType() {
        return Optional.ofNullable(endOfHistoryTransferType);
    }

    /**
     * Returns the timestamp of the most recent activity in this conversation.
     *
     * @return an {@link Optional} containing the conversation timestamp, or
     *         empty if not available
     */
    public Optional<Instant> conversationTimestamp() {
        return Optional.ofNullable(conversationTimestamp);
    }

    /**
     * Returns the server-provided name for this chat.
     *
     * @return an {@link Optional} containing the name, or empty if not set
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the participant list hash for this chat.
     *
     * @return an {@link Optional} containing the hash, or empty if not set
     */
    public Optional<String> pHash() {
        return Optional.ofNullable(pHash);
    }

    /**
     * Returns whether this chat has been marked as not spam.
     *
     * @return {@code true} if the chat is explicitly marked as not spam
     */
    public boolean notSpam() {
        return notSpam != null && notSpam;
    }

    /**
     * Returns whether this chat is archived.
     *
     * @return {@code true} if the chat is archived
     */
    public boolean archived() {
        return archived != null && archived;
    }

    /**
     * Returns the disappearing mode metadata for this chat.
     *
     * @return an {@link Optional} containing the disappearing mode, or empty
     *         if disappearing messages are not configured
     */
    public Optional<ChatDisappearingMode> disappearingMode() {
        return Optional.ofNullable(disappearingMode);
    }

    /**
     * Returns the number of unread messages that mention the current user.
     *
     * @return an {@link OptionalInt} containing the mention count, or empty
     *         if not available
     */
    public OptionalInt unreadMentionCount() {
        return unreadMentionCount == null ? OptionalInt.empty() : OptionalInt.of(unreadMentionCount);
    }

    /**
     * Returns whether the user has manually marked this chat as unread.
     *
     * @return {@code true} if the chat is marked as unread
     */
    public boolean markedAsUnread() {
        return markedAsUnread != null && markedAsUnread;
    }

    /**
     * Returns the list of participants in this group chat.
     *
     * @return an unmodifiable list of group participants, or an empty list
     *         for non-group chats; never {@code null}
     */
    public List<GroupParticipant> participant() {
        return participant == null ? List.of() : Collections.unmodifiableList(participant);
    }

    /**
     * Returns the trust and compliance token for this chat.
     *
     * @return an {@link Optional} containing the token bytes, or empty if
     *         not present
     */
    public Optional<byte[]> tcToken() {
        return Optional.ofNullable(tcToken);
    }

    /**
     * Returns the timestamp at which the T&C token was issued.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not
     *         available
     */
    public Optional<Instant> tcTokenTimestamp() {
        return Optional.ofNullable(tcTokenTimestamp);
    }

    /**
     * Returns the contact's primary identity key for Signal Protocol
     * verification.
     *
     * @return an {@link Optional} containing the identity key bytes, or empty
     *         if not available
     */
    public Optional<byte[]> contactPrimaryIdentityKey() {
        return Optional.ofNullable(contactPrimaryIdentityKey);
    }

    /**
     * Returns the timestamp at which this chat was pinned.
     *
     * @return an {@link Optional} containing the pin timestamp, or empty if
     *         the chat is not pinned
     */
    public Optional<Instant> pinnedTimestamp() {
        return Optional.ofNullable(pinnedTimestamp);
    }

    /**
     * Returns the mute state of this chat.
     *
     * @return an {@link Optional} containing the mute state, or empty if
     *         the chat is not muted
     */
    public Optional<ChatMute> mute() {
        return Optional.ofNullable(mute);
    }

    /**
     * Returns the custom wallpaper settings for this chat.
     *
     * @return an {@link Optional} containing the wallpaper settings, or empty
     *         if no custom wallpaper is set
     */
    public Optional<WallpaperSettings> wallpaper() {
        return Optional.ofNullable(wallpaper);
    }

    /**
     * Returns the media visibility setting for this chat.
     *
     * @return an {@link Optional} containing the media visibility, or empty
     *         if using the default setting
     */
    public Optional<MediaVisibility> mediaVisibility() {
        return Optional.ofNullable(mediaVisibility);
    }

    /**
     * Returns the timestamp at which the T&C token was sent by the sender.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not
     *         available
     */
    public Optional<Instant> tcTokenSenderTimestamp() {
        return Optional.ofNullable(tcTokenSenderTimestamp);
    }

    /**
     * Returns whether this group chat is suspended.
     *
     * @return {@code true} if the group is temporarily disabled
     */
    public boolean suspended() {
        return suspended != null && suspended;
    }

    /**
     * Returns whether this group chat is terminated.
     *
     * @return {@code true} if the group has been permanently closed
     */
    public boolean terminated() {
        return terminated != null && terminated;
    }

    /**
     * Returns the creation instant of this chat.
     *
     * @return an {@link Optional} containing the creation timestamp, or empty
     *         if not available
     */
    public Optional<Instant> createdAt() {
        return Optional.ofNullable(createdAt);
    }

    /**
     * Returns the identifier of the user who created this group chat.
     *
     * @return an {@link Optional} containing the creator, or empty if not
     *         available
     */
    public Optional<String> createdBy() {
        return Optional.ofNullable(createdBy);
    }

    /**
     * Returns the description of this group or community chat.
     *
     * @return an {@link Optional} containing the description, or empty if
     *         no description is set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns whether this is a WhatsApp support chat.
     *
     * @return {@code true} if this is a support conversation
     */
    public boolean support() {
        return support != null && support;
    }

    /**
     * Returns whether this group is a community parent group.
     *
     * @return {@code true} if this is a parent group of a community
     */
    public boolean isParentGroup() {
        return isParentGroup != null && isParentGroup;
    }

    /**
     * Returns the identifier of the parent community group.
     *
     * @return an {@link Optional} containing the parent group ID, or empty
     *         if this is not a community sub-group
     */
    public Optional<String> parentGroupId() {
        return Optional.ofNullable(parentGroupId);
    }

    /**
     * Returns whether this is the default sub-group (General group) of a
     * community.
     *
     * @return {@code true} if this is the default sub-group
     */
    public boolean isDefaultSubgroup() {
        return isDefaultSubgroup != null && isDefaultSubgroup;
    }

    /**
     * Returns the display name for this chat.
     *
     * @return an {@link Optional} containing the display name, or empty if
     *         not set
     */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /**
     * Returns the phone number JID associated with this chat.
     *
     * @return an {@link Optional} containing the phone number JID, or empty
     *         if not available
     */
    public Optional<Jid> phoneNumberJid() {
        return Optional.ofNullable(phoneNumberJid);
    }

    /**
     * Returns whether the current user's phone number should be shared in
     * this LID-based chat.
     *
     * @return {@code true} if phone number sharing is enabled
     */
    public boolean shareOwnPhoneNumber() {
        return shareOwnPhoneNumber != null && shareOwnPhoneNumber;
    }

    /**
     * Returns whether a duplicate LID thread exists for the same phone number.
     *
     * @return {@code true} if a duplicate LID thread exists
     */
    public boolean phoneNumberhDuplicateLidThread() {
        return phoneNumberhDuplicateLidThread != null && phoneNumberhDuplicateLidThread;
    }

    /**
     * Returns the Linked Identity (LID) JID for this chat.
     *
     * @return an {@link Optional} containing the LID, or empty if not
     *         available
     */
    public Optional<Jid> lid() {
        return Optional.ofNullable(lid);
    }

    /**
     * Returns the username associated with this chat.
     *
     * @return an {@link Optional} containing the username, or empty if not
     *         set
     */
    public Optional<String> username() {
        return Optional.ofNullable(username);
    }

    /**
     * Returns the origin type of the LID for this chat.
     *
     * @return an {@link Optional} containing the LID origin type, or empty
     *         if not available
     */
    public Optional<String> lidOriginType() {
        return Optional.ofNullable(lidOriginType);
    }

    /**
     * Returns the number of comments on this chat's content.
     *
     * @return an {@link OptionalInt} containing the comments count, or empty
     *         if not available
     */
    public OptionalInt commentsCount() {
        return commentsCount == null ? OptionalInt.empty() : OptionalInt.of(commentsCount);
    }

    /**
     * Returns whether this chat is locked behind biometric authentication.
     *
     * @return {@code true} if the chat is locked
     */
    public boolean locked() {
        return locked != null && locked;
    }

    /**
     * Returns the privacy system message to insert in this chat.
     *
     * @return an {@link Optional} containing the system message type, or
     *         empty if none is pending
     */
    public Optional<PrivacySystemMessage> systemMessageToInsert() {
        return Optional.ofNullable(systemMessageToInsert);
    }

    /**
     * Returns whether this group was created via the Cloud API.
     *
     * @return {@code true} if the group was CAPI-created
     */
    public boolean capiCreatedGroup() {
        return capiCreatedGroup != null && capiCreatedGroup;
    }

    /**
     * Returns the current user's account-level LID for this chat.
     *
     * @return an {@link Optional} containing the account LID, or empty if
     *         not available
     */
    public Optional<Jid> accountLid() {
        return Optional.ofNullable(accountLid);
    }

    /**
     * Returns whether limit sharing is enabled for this chat.
     *
     * @return {@code true} if personal information sharing is restricted
     */
    public boolean limitSharing() {
        return limitSharing != null && limitSharing;
    }

    /**
     * Returns the timestamp at which the limit sharing setting was changed.
     *
     * @return an {@link Optional} containing the timestamp, or empty if not
     *         available
     */
    public Optional<Instant> limitSharingSettingTimestamp() {
        return Optional.ofNullable(limitSharingSettingTimestamp);
    }

    /**
     * Returns the trigger that caused the limit sharing state to be set.
     *
     * @return an {@link Optional} containing the trigger type, or empty if
     *         not set
     */
    public Optional<ChatLimitSharing.TriggerType> limitSharingTrigger() {
        return Optional.ofNullable(limitSharingTrigger);
    }

    /**
     * Returns whether the current user initiated the limit sharing change.
     *
     * @return {@code true} if the current user initiated limit sharing
     */
    public boolean limitSharingInitiatedByMe() {
        return limitSharingInitiatedByMe != null && limitSharingInitiatedByMe;
    }

    /**
     * Returns whether the Meta AI bot thread is enabled for this chat.
     *
     * @return {@code true} if the AI thread is enabled
     */
    public boolean maibaAiThreadEnabled() {
        return maibaAiThreadEnabled != null && maibaAiThreadEnabled;
    }

    /**
     * Sets the JID of this chat.
     *
     * @param jid the new JID
     */
    public void setJid(Jid jid) {
        this.jid = jid;
    }

    /**
     * Transfers all messages from the specified chat into this chat.
     *
     * <p>After this operation the source chat's message collection is empty
     * and this chat owns every message that was previously in the source.
     *
     * @param source the chat whose messages are transferred into this chat
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public void transferMessages(Chat source) {
        Objects.requireNonNull(source);
        try (var stream = source.messages()) {
            stream.forEach(this::addMessage);
        }
        source.removeMessages();
    }

    /**
     * Sets the new JID for this chat after a migration.
     *
     * @param newJid the new JID, or {@code null} to clear
     */
    public void setNewJid(Jid newJid) {
        this.newJid = newJid;
    }

    /**
     * Sets the old JID for this chat before a migration.
     *
     * @param oldJid the old JID, or {@code null} to clear
     */
    public void setOldJid(Jid oldJid) {
        this.oldJid = oldJid;
    }

    /**
     * Sets the timestamp of the most recent message.
     *
     * @param lastMsgTimestamp the timestamp, or {@code null} to clear
     */
    public void setLastMsgTimestamp(Instant lastMsgTimestamp) {
        this.lastMsgTimestamp = lastMsgTimestamp;
    }

    /**
     * Sets the unread message count.
     *
     * @param unreadCount the count, or {@code null} to clear
     */
    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    /**
     * Sets whether this chat is read-only.
     *
     * @param readOnly {@code true} for read-only, or {@code null} to clear
     */
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Sets whether the history sync transfer has completed.
     *
     * @param endOfHistoryTransfer {@code true} if complete, or {@code null}
     *                             to clear
     */
    public void setEndOfHistoryTransfer(Boolean endOfHistoryTransfer) {
        this.endOfHistoryTransfer = endOfHistoryTransfer;
    }

    /**
     * Sets the disappearing messages timer.
     *
     * @param ephemeralExpiration the timer, or {@code null} to disable
     */
    public void setEphemeralExpiration(ChatEphemeralTimer ephemeralExpiration) {
        this.ephemeralExpiration = ephemeralExpiration;
    }

    /**
     * Sets the timestamp at which the ephemeral setting was last changed.
     *
     * @param ephemeralSettingTimestamp the timestamp, or {@code null} to clear
     */
    public void setEphemeralSettingTimestamp(Instant ephemeralSettingTimestamp) {
        this.ephemeralSettingTimestamp = ephemeralSettingTimestamp;
    }

    /**
     * Sets the type of history transfer completion.
     *
     * @param endOfHistoryTransferType the transfer type, or {@code null} to clear
     */
    public void setEndOfHistoryTransferType(EndOfHistoryTransferType endOfHistoryTransferType) {
        this.endOfHistoryTransferType = endOfHistoryTransferType;
    }

    /**
     * Sets the conversation activity timestamp.
     *
     * @param conversationTimestamp the timestamp, or {@code null} to clear
     */
    public void setConversationTimestamp(Instant conversationTimestamp) {
        this.conversationTimestamp = conversationTimestamp;
    }

    /**
     * Sets the chat name.
     *
     * @param name the name, or {@code null} to clear
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the participant list hash.
     *
     * @param pHash the hash, or {@code null} to clear
     */
    public void setPHash(String pHash) {
        this.pHash = pHash;
    }

    /**
     * Sets whether this chat is marked as not spam.
     *
     * @param notSpam {@code true} to mark as not spam, or {@code null} to clear
     */
    public void setNotSpam(Boolean notSpam) {
        this.notSpam = notSpam;
    }

    /**
     * Sets whether this chat is archived.
     *
     * @param archived {@code true} to archive, or {@code null} to clear
     */
    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    /**
     * Sets the disappearing mode metadata.
     *
     * @param disappearingMode the disappearing mode, or {@code null} to clear
     */
    public void setDisappearingMode(ChatDisappearingMode disappearingMode) {
        this.disappearingMode = disappearingMode;
    }

    /**
     * Sets the unread mention count.
     *
     * @param unreadMentionCount the count, or {@code null} to clear
     */
    public void setUnreadMentionCount(Integer unreadMentionCount) {
        this.unreadMentionCount = unreadMentionCount;
    }

    /**
     * Sets whether this chat is manually marked as unread.
     *
     * @param markedAsUnread {@code true} to mark as unread, or {@code null}
     *                       to clear
     */
    public void setMarkedAsUnread(Boolean markedAsUnread) {
        this.markedAsUnread = markedAsUnread;
    }

    /**
     * Sets the group participant list.
     *
     * @param participant the participant list, or {@code null} to clear
     */
    public void setParticipant(List<GroupParticipant> participant) {
        this.participant = participant;
    }

    /**
     * Sets the trust and compliance token.
     *
     * @param tcToken the token bytes, or {@code null} to clear
     */
    public void setTcToken(byte[] tcToken) {
        this.tcToken = tcToken;
    }

    /**
     * Sets the T&C token timestamp.
     *
     * @param tcTokenTimestamp the timestamp, or {@code null} to clear
     */
    public void setTcTokenTimestamp(Instant tcTokenTimestamp) {
        this.tcTokenTimestamp = tcTokenTimestamp;
    }

    /**
     * Sets the contact's primary identity key.
     *
     * @param contactPrimaryIdentityKey the key bytes, or {@code null} to clear
     */
    public void setContactPrimaryIdentityKey(byte[] contactPrimaryIdentityKey) {
        this.contactPrimaryIdentityKey = contactPrimaryIdentityKey;
    }

    /**
     * Sets the pinned timestamp for this chat.
     *
     * @param pinned the pin timestamp, or {@code null} to unpin
     */
    public void setPinnedTimestamp(Instant pinned) {
        this.pinnedTimestamp = pinned;
    }

    /**
     * Sets the mute state of this chat.
     *
     * @param mute the mute state, or {@code null} to unmute
     */
    public void setMute(ChatMute mute) {
        this.mute = mute;
    }

    /**
     * Sets the custom wallpaper settings.
     *
     * @param wallpaper the wallpaper settings, or {@code null} to use the
     *                  default wallpaper
     */
    public void setWallpaper(WallpaperSettings wallpaper) {
        this.wallpaper = wallpaper;
    }

    /**
     * Sets the media visibility setting.
     *
     * @param mediaVisibility the media visibility, or {@code null} to use the
     *                        default
     */
    public void setMediaVisibility(MediaVisibility mediaVisibility) {
        this.mediaVisibility = mediaVisibility;
    }

    /**
     * Sets the T&C token sender timestamp.
     *
     * @param tcTokenSenderTimestamp the timestamp, or {@code null} to clear
     */
    public void setTcTokenSenderTimestamp(Instant tcTokenSenderTimestamp) {
        this.tcTokenSenderTimestamp = tcTokenSenderTimestamp;
    }

    /**
     * Sets whether this group is suspended.
     *
     * @param suspended {@code true} to suspend, or {@code null} to clear
     */
    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Sets whether this group is terminated.
     *
     * @param terminated {@code true} to terminate, or {@code null} to clear
     */
    public void setTerminated(Boolean terminated) {
        this.terminated = terminated;
    }

    /**
     * Sets the creation instant of this chat.
     *
     * @param createdAt the timestamp, or {@code null} to clear
     */
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Sets the identifier of the chat creator.
     *
     * @param createdBy the creator, or {@code null} to clear
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * Sets the description of this chat.
     *
     * @param description the description, or {@code null} to clear
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets whether this is a support chat.
     *
     * @param support {@code true} for a support chat, or {@code null} to clear
     */
    public void setSupport(Boolean support) {
        this.support = support;
    }

    /**
     * Sets whether this group is a community parent group.
     *
     * @param isParentGroup {@code true} for a parent group, or {@code null}
     *                      to clear
     */
    public void setParentGroup(Boolean isParentGroup) {
        this.isParentGroup = isParentGroup;
    }

    /**
     * Sets the parent community group identifier.
     *
     * @param parentGroupId the parent group ID, or {@code null} to clear
     */
    public void setParentGroupId(String parentGroupId) {
        this.parentGroupId = parentGroupId;
    }

    /**
     * Sets whether this is the default sub-group of a community.
     *
     * @param isDefaultSubgroup {@code true} for the default sub-group, or
     *                          {@code null} to clear
     */
    public void setDefaultSubgroup(Boolean isDefaultSubgroup) {
        this.isDefaultSubgroup = isDefaultSubgroup;
    }

    /**
     * Sets the display name for this chat.
     *
     * @param displayName the display name, or {@code null} to clear
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Sets the phone number JID for this chat.
     *
     * @param phoneNumberJid the phone number JID, or {@code null} to clear
     */
    public void setPhoneNumberJid(Jid phoneNumberJid) {
        this.phoneNumberJid = phoneNumberJid;
    }

    /**
     * Sets whether to share the current user's phone number in this chat.
     *
     * @param shareOwnPn {@code true} to share, or {@code null} to clear
     */
    public void setShareOwnPhoneNumber(Boolean shareOwnPn) {
        this.shareOwnPhoneNumber = shareOwnPn;
    }

    /**
     * Sets whether a duplicate LID thread exists for this phone number.
     *
     * @param phoneNumberhDuplicateLidThread {@code true} if duplicate exists,
     *                                       or {@code null} to clear
     */
    public void setPhoneNumberDuplicateLidThread(Boolean phoneNumberhDuplicateLidThread) {
        this.phoneNumberhDuplicateLidThread = phoneNumberhDuplicateLidThread;
    }

    /**
     * Sets the LID JID for this chat.
     *
     * @param lidJid the LID, or {@code null} to clear
     */
    public void setLid(Jid lidJid) {
        this.lid = lidJid;
    }

    /**
     * Sets the username for this chat.
     *
     * @param username the username, or {@code null} to clear
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the LID origin type for this chat.
     *
     * @param lidOriginType the origin type, or {@code null} to clear
     */
    public void setLidOriginType(String lidOriginType) {
        this.lidOriginType = lidOriginType;
    }

    /**
     * Sets the comments count for this chat.
     *
     * @param commentsCount the count, or {@code null} to clear
     */
    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    /**
     * Sets whether this chat is locked behind biometric authentication.
     *
     * @param locked {@code true} to lock, or {@code null} to clear
     */
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    /**
     * Sets the privacy system message to insert in this chat.
     *
     * @param systemMessageToInsert the system message type, or {@code null}
     *                              to clear
     */
    public void setSystemMessageToInsert(PrivacySystemMessage systemMessageToInsert) {
        this.systemMessageToInsert = systemMessageToInsert;
    }

    /**
     * Sets whether this group was created via the Cloud API.
     *
     * @param capiCreatedGroup {@code true} if CAPI-created, or {@code null}
     *                         to clear
     */
    public void setCapiCreatedGroup(Boolean capiCreatedGroup) {
        this.capiCreatedGroup = capiCreatedGroup;
    }

    /**
     * Sets the account-level LID for this chat.
     *
     * @param accountLid the account LID, or {@code null} to clear
     */
    public void setAccountLid(Jid accountLid) {
        this.accountLid = accountLid;
    }

    /**
     * Sets whether limit sharing is enabled for this chat.
     *
     * @param limitSharing {@code true} to enable, or {@code null} to clear
     */
    public void setLimitSharing(Boolean limitSharing) {
        this.limitSharing = limitSharing;
    }

    /**
     * Sets the timestamp at which the limit sharing setting was changed.
     *
     * @param limitSharingSettingTimestamp the timestamp, or {@code null} to clear
     */
    public void setLimitSharingSettingTimestamp(Instant limitSharingSettingTimestamp) {
        this.limitSharingSettingTimestamp = limitSharingSettingTimestamp;
    }

    /**
     * Sets the trigger that caused the limit sharing state.
     *
     * @param limitSharingTrigger the trigger type, or {@code null} to clear
     */
    public void setLimitSharingTrigger(ChatLimitSharing.TriggerType limitSharingTrigger) {
        this.limitSharingTrigger = limitSharingTrigger;
    }

    /**
     * Sets whether the current user initiated the limit sharing change.
     *
     * @param limitSharingInitiatedByMe {@code true} if self-initiated, or
     *                                  {@code null} to clear
     */
    public void setLimitSharingInitiatedByMe(Boolean limitSharingInitiatedByMe) {
        this.limitSharingInitiatedByMe = limitSharingInitiatedByMe;
    }

    /**
     * Sets whether the Meta AI bot thread is enabled for this chat.
     *
     * @param maibaAiThreadEnabled {@code true} to enable, or {@code null}
     *                             to clear
     */
    public void setMaibaAiThreadEnabled(Boolean maibaAiThreadEnabled) {
        this.maibaAiThreadEnabled = maibaAiThreadEnabled;
    }

    /**
     * Describes the state of history-sync transfer completion for a chat.
     *
     * <p>When a companion device joins a WhatsApp account it receives chat
     * history from the primary device. This enum reports whether that
     * transfer has completed for the chat and whether more messages remain
     * available on the primary device.
     */
    @ProtobufEnum(name = "Conversation.EndOfHistoryTransferType")
    public enum EndOfHistoryTransferType {
        /**
         * The transfer is complete, but additional older messages still
         * remain on the primary device and may be fetched on demand.
         */
        COMPLETE_BUT_MORE_MESSAGES_REMAIN_ON_PRIMARY(0),

        /**
         * The transfer is complete and no more messages remain on the
         * primary device. The companion device has all available history.
         */
        COMPLETE_AND_NO_MORE_MESSAGE_REMAIN_ON_PRIMARY(1),

        /**
         * The history-sync transfer did not finish; it is still in progress
         * or was interrupted.
         */
        INCOMPLETE(2),

        /**
         * The chat was not included in the history-sync payload at all.
         */
        NOT_INCLUDED_IN_HIST_SYNC(3),

        /**
         * An on-demand sync chunk has been transferred, but more messages
         * still remain on the primary device.
         */
        COMPLETE_ON_DEMAND_SYNC_BUT_MORE_MSG_REMAIN_ON_PRIMARY(4),

        /**
         * An on-demand sync chunk has been transferred and more messages
         * exist on the primary device, but the companion cannot access them
         * (for example because the primary is on an older app version).
         */
        COMPLETE_ON_DEMAND_SYNC_WITH_MORE_MSG_ON_PRIMARY_BUT_NO_ACCESS(5);

        /**
         * Constructs an {@code EndOfHistoryTransferType} with the specified
         * protobuf index.
         *
         * @param index the protobuf enum index
         */
        EndOfHistoryTransferType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf-assigned index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf index of this transfer type.
         *
         * @return the integer index used for wire serialization
         */
        public int index() {
            return this.index;
        }
    }
}
