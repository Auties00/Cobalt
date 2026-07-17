package com.github.auties00.cobalt.store.linked;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.call.CallLog;
import com.github.auties00.cobalt.wire.linked.call.IncomingCall;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMetadata;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataEdit;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterPin;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The conversation state of a WhatsApp client session.
 *
 * <p>This is the sub-store that owns chats (one-to-one, groups, communities, broadcast lists),
 * newsletters (Channels), the status feed, their messages, the in-flight call table, call history,
 * per-chat metadata, the favourite-chats list, mention-everyone mute expirations, newsletter pins,
 * the peer-message buffer and the UTM read-tracking set.
 *
 * <p>The chat, newsletter, status and message accessors are backed differently by each persistence
 * strategy (MVStore-backed metadata for the persistent store, fully in-memory for the temporary store),
 * so they are implemented per-variant; the remaining state is strategy-independent.
 *
 * @apiNote
 * Embedders reach this through {@link LinkedWhatsAppStore#chatStore()}.
 *
 * @see LinkedWhatsAppStore
 */
@WhatsAppWebModule(moduleName = "WAWebModelStorageInitialize")
@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface LinkedWhatsAppChatStore {
    /**
     * Returns all chats known to this session.
     *
     * @return an unmodifiable copy of the chats
     */
    Collection<Chat> chats();

    /**
     * Looks up a chat by JID, bridging the phone-number/LID boundary on a direct miss.
     *
     * @param jid the chat identifier, or {@code null}
     * @return the matching chat, or empty if none is known
     */
    Optional<Chat> findChatByJid(JidProvider jid);

    /**
     * Creates and stores a new chat for the given JID.
     *
     * @param chatJid the chat JID, never {@code null}
     * @return the newly created chat
     */
    Chat addNewChat(Jid chatJid);

    /**
     * Removes a chat by JID, bridging the phone-number/LID boundary, and drops its message bodies.
     *
     * @param chatJid the chat identifier, or {@code null}
     * @return the removed chat, or empty if none matched
     */
    Optional<Chat> removeChat(JidProvider chatJid);

    /**
     * Returns all newsletters (Channels) known to this session.
     *
     * @return an unmodifiable copy of the newsletters
     */
    Collection<Newsletter> newsletters();

    /**
     * Looks up a newsletter by JID.
     *
     * @param jid the newsletter identifier, or {@code null}
     * @return the matching newsletter, or empty if none is known
     */
    Optional<Newsletter> findNewsletterByJid(JidProvider jid);

    /**
     * Creates and stores a new newsletter for the given JID.
     *
     * @param newsletterJid the newsletter JID, never {@code null}
     * @return the newly created newsletter
     */
    Newsletter addNewNewsletter(Jid newsletterJid);

    /**
     * Removes a newsletter by JID and drops its message bodies.
     *
     * @param newsletterJid the newsletter identifier, or {@code null}
     * @return the removed newsletter, or empty if none matched
     */
    Optional<Newsletter> removeNewsletter(JidProvider newsletterJid);

    /**
     * Returns the status feed messages.
     *
     * @return an unmodifiable copy of the status feed
     */
    Collection<ChatMessageInfo> status();

    /**
     * Stores a status feed message.
     *
     * @param messageInfo the status message, never {@code null}
     * @return the stored message
     */
    ChatMessageInfo addStatus(ChatMessageInfo messageInfo);

    /**
     * Removes a status feed message by id.
     *
     * @param id the message id, or {@code null}
     * @return the removed message, or empty if not present
     */
    Optional<ChatMessageInfo> removeStatus(String id);

    /**
     * Looks up a status feed message by id.
     *
     * @param id the message id, or {@code null}
     * @return the message, or empty if not present
     */
    Optional<ChatMessageInfo> findStatusById(String id);

    /**
     * Looks up a message by provider and id, routing to chats, newsletters or the status feed.
     *
     * @param provider the owning chat, newsletter, contact or JID, or {@code null}
     * @param id       the message id, or {@code null}
     * @return the matching message, or empty if not present
     */
    Optional<? extends LinkedMessageInfo> findMessageById(JidProvider provider, String id);

    /**
     * Looks up a newsletter message by newsletter and id (server id or message-key id).
     *
     * @param newsletter the newsletter, or {@code null}
     * @param id         the server id or message-key id, or {@code null}
     * @return the matching message, or empty if not present
     */
    Optional<NewsletterMessageInfo> findMessageById(Newsletter newsletter, String id);

    /**
     * Looks up a chat message by chat and id.
     *
     * @param chat the chat, or {@code null}
     * @param id   the message id, or {@code null}
     * @return the matching message, or empty if not present
     */
    Optional<ChatMessageInfo> findMessageById(Chat chat, String id);

    /**
     * Looks up a chat message by its message key.
     *
     * @param key the message key
     * @return the matching message, or empty if not present
     */
    Optional<? extends LinkedMessageInfo> findMessageByKey(MessageKey key);

    /**
     * Resolves the message quoted by the given message, if any.
     *
     * @param info the message whose quoted reference is resolved
     * @return the quoted message, or empty if there is none or it is not stored
     */
    Optional<? extends LinkedMessageInfo> findQuotedMessage(LinkedMessageInfo info);

    /**
     * Returns the currently tracked incoming calls.
     *
     * @return an unmodifiable copy of the incoming calls
     */
    Collection<IncomingCall> calls();

    /**
     * Looks up a tracked incoming call by id.
     *
     * @param callId the call id, or {@code null}
     * @return the call, or empty if not tracked
     */
    Optional<IncomingCall> findCallById(String callId);

    /**
     * Stores a tracked incoming call.
     *
     * @param call the call, never {@code null}
     * @return the stored call
     */
    IncomingCall addCall(IncomingCall call);

    /**
     * Removes a tracked incoming call by id.
     *
     * @param id the call id, or {@code null}
     * @return the removed call, or empty if not tracked
     */
    Optional<IncomingCall> removeCall(String id);

    /**
     * Looks up per-chat metadata by chat/group JID.
     *
     * @param groupJid the chat JID, never {@code null}
     * @return the metadata, or empty if none is cached
     */
    Optional<ChatMetadata> findChatMetadata(Jid groupJid);

    /**
     * Stores per-chat metadata.
     *
     * @param metadata the metadata, never {@code null}
     */
    void addChatMetadata(ChatMetadata metadata);

    /**
     * Removes per-chat metadata by chat/group JID.
     *
     * @param groupJid the chat JID, never {@code null}
     */
    void removeChatMetadata(Jid groupJid);

    /**
     * Applies the local-only fields of a group-metadata edit to the cached group metadata.
     *
     * @param groupJid the group JID, never {@code null}
     * @param edit     the edit, never {@code null}
     * @return the updated group metadata, or empty if no group is cached
     */
    Optional<GroupMetadata> applyGroupMetadataEdit(Jid groupJid, GroupMetadataEdit edit);

    /**
     * Returns the ordered list of pinned (favourite) chat JIDs.
     *
     * @return an unmodifiable copy of the favourite chat JIDs
     */
    List<Jid> favoriteChats();

    /**
     * Replaces the favourite-chats list.
     *
     * @param favoriteChats the favourite chat JIDs, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppChatStore setFavoriteChats(List<Jid> favoriteChats);

    /**
     * Returns the mention-everyone mute expiration for a group.
     *
     * @param chatJid the group JID, never {@code null}
     * @return the mute expiration, or empty if none is set
     */
    Optional<ChatMute> mentionEveryoneMuteExpiration(Jid chatJid);

    /**
     * Sets the mention-everyone mute expiration for a group.
     *
     * @param chatJid the group JID, never {@code null}
     * @param mute    the mute expiration, never {@code null}
     */
    void setMentionEveryoneMuteExpiration(Jid chatJid, ChatMute mute);

    /**
     * Returns the newsletter pins.
     *
     * @return an unmodifiable copy of the newsletter pins
     */
    Collection<NewsletterPin> newsletterPinStates();

    /**
     * Looks up a newsletter pin by newsletter JID.
     *
     * @param newsletterJid the newsletter JID, or {@code null}
     * @return the pin, or empty if not present
     */
    Optional<NewsletterPin> findNewsletterPin(Jid newsletterJid);

    /**
     * Stores a newsletter pin.
     *
     * @param pin the pin, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppChatStore putNewsletterPin(NewsletterPin pin);

    /**
     * Removes a newsletter pin by newsletter JID.
     *
     * @param newsletterJid the newsletter JID, or {@code null}
     * @return the removed pin, or empty if not present
     */
    Optional<NewsletterPin> removeNewsletterPin(Jid newsletterJid);

    /**
     * Clears all newsletter pins.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppChatStore clearNewsletterPins();

    /**
     * Returns the call history entries.
     *
     * @return an unmodifiable copy of the call logs
     */
    Collection<CallLog> callLogStates();

    /**
     * Looks up a call-history entry by call id.
     *
     * @param callId the call id, or {@code null}
     * @return the call log, or empty if not present
     */
    Optional<CallLog> findCallLog(String callId);

    /**
     * Stores a call-history entry.
     *
     * @param callLog the call log, never {@code null}
     * @return this store instance for method chaining
     */
    LinkedWhatsAppChatStore addCallLog(CallLog callLog);

    /**
     * Removes a call-history entry by call id.
     *
     * @param callId the call id, or {@code null}
     * @return the removed call log, or empty if not present
     */
    Optional<CallLog> removeCallLog(String callId);

    /**
     * Clears all call-history entries.
     *
     * @return this store instance for method chaining
     */
    LinkedWhatsAppChatStore clearCallLogs();

    /**
     * Stores a peer message in the internal peer-message buffer.
     *
     * @param id      the message id
     * @param message the message
     */
    void addPeerMessage(String id, ChatMessageInfo message);

    /**
     * Removes a peer message from the internal buffer by id.
     *
     * @param id the message id
     */
    void removePeerMessage(String id);

    /**
     * Returns the recipient device JIDs recorded for a sent message.
     *
     * @param messageId the message id
     * @return an unmodifiable copy of the recipient set
     */
    Set<Jid> findReceiptRecords(String messageId);

    /**
     * Creates or merges receipt records for a sent message.
     *
     * @param messageId     the message id
     * @param recipientJids the recipient device JIDs
     */
    void createOrMergeReceiptRecords(String messageId, Collection<Jid> recipientJids);

    /**
     * Removes receipt records for a message.
     *
     * @param messageId the message id
     */
    void removeReceiptRecords(String messageId);

    /**
     * Marks a chat's UTM tracking message as read.
     *
     * @param chatJid the chat JID, or {@code null} to ignore
     */
    void markUtmReadForChat(Jid chatJid);

    /**
     * Returns whether a chat's UTM tracking message has been read.
     *
     * @param chatJid the chat JID, or {@code null}
     * @return {@code true} if the chat's UTM message has been read
     */
    boolean hasReadUtmForChat(Jid chatJid);

    /**
     * Clears the UTM-read marker for a chat.
     *
     * @param chatJid the chat JID, or {@code null} to ignore
     */
    void deleteUtmReadChatId(Jid chatJid);

    /**
     * Clears all UTM-read markers.
     */
    void clearUtmReadChatIds();
}
