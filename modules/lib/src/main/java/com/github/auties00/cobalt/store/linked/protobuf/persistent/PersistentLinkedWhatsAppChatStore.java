package com.github.auties00.cobalt.store.linked.protobuf.persistent;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageInfo;
import com.github.auties00.cobalt.wire.core.message.MessageKey;
import com.github.auties00.cobalt.wire.linked.newsletter.Newsletter;
import com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo;
import com.github.auties00.cobalt.store.linked.protobuf.ProtobufLinkedWhatsAppChatStore;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNullElseGet;

/**
 * The {@link ProtobufLinkedWhatsAppChatStore} variant that persists chat and newsletter metadata to {@code store.proto}
 * and offloads every message body to an embedded {@link PersistentMessageStore MVStore}.
 *
 * <p>Chat and newsletter entries carry metadata only (jid, name, unread counters, ephemeral
 * settings); message bodies live in {@link PersistentMessageStore}, wired in by
 * {@link #setMessageStore(PersistentMessageStore)} after construction or deserialisation.
 *
 * @implNote
 * The {@code chats} and {@code newsletters} maps are package-private so the generated
 * {@code PersistentChatStoreSpec} (in this package) can serialise them by direct field access; the
 * inherited {@code mentionEveryoneMuteExpirationsMap} map property is re-exposed here for the same
 * reason.
 */
@ProtobufMessage
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class PersistentLinkedWhatsAppChatStore extends ProtobufLinkedWhatsAppChatStore {
    /**
     * The logger for {@link PersistentLinkedWhatsAppChatStore}.
     */
    private static final System.Logger LOGGER = Log.get(PersistentLinkedWhatsAppChatStore.class);

    /**
     * The map of chat JIDs to their metadata-only {@link PersistentChat} entries.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<Jid, PersistentChat> chats;

    /**
     * The map of newsletter JIDs to their metadata-only {@link PersistentNewsletter} entries.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final ConcurrentHashMap<Jid, PersistentNewsletter> newsletters;

    /**
     * The MVStore facade backing every message-bearing accessor; wired by
     * {@link #setMessageStore(PersistentMessageStore)}, not persisted.
     */
    private volatile PersistentMessageStore messageStore;

    /**
     * Constructs a persistent chat sub-store with the given protobuf-decoded maps.
     *
     * @param favoriteChats                     the favourite chat JIDs, or {@code null} for an empty list
     * @param mentionEveryoneMuteExpirationsMap the mention-everyone mute map, or {@code null} for an empty map
     * @param chats                             the persistent chats map, or {@code null} for an empty map
     * @param newsletters                       the persistent newsletters map, or {@code null} for an empty map
     */
    PersistentLinkedWhatsAppChatStore(List<Jid> favoriteChats, ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap, ConcurrentHashMap<Jid, PersistentChat> chats, ConcurrentHashMap<Jid, PersistentNewsletter> newsletters) {
        super(favoriteChats, mentionEveryoneMuteExpirationsMap);
        this.chats = requireNonNullElseGet(chats, ConcurrentHashMap::new);
        this.newsletters = requireNonNullElseGet(newsletters, ConcurrentHashMap::new);
    }

    @Override
    protected ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap() {
        return super.mentionEveryoneMuteExpirationsMap();
    }

    /**
     * Sets the MVStore facade on this store and on every existing chat and newsletter entry.
     *
     * <p>The facade opens only after this store is built or deserialised, so it cannot be a constructor
     * argument; the persistent store sets it once, before the store is handed back.
     *
     * @param messageStore the freshly opened MVStore facade
     */
    void setMessageStore(PersistentMessageStore messageStore) {
        this.messageStore = messageStore;
        for (var chat : chats.values()) {
            chat.setMessageStore(messageStore);
        }
        for (var newsletter : newsletters.values()) {
            newsletter.setMessageStore(messageStore);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "message store set: {0} chats, {1} newsletters", chats.size(), newsletters.size());
        }
    }

    /**
     * Returns the MVStore facade owned by this store.
     *
     * @return the message store, or {@code null} if not yet attached
     */
    PersistentMessageStore messageStore() {
        return messageStore;
    }

    @Override
    public Optional<Chat> findChatByJid(JidProvider jid) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "resolving chat for {0}", jid);
        return switch (jid) {
            case null -> Optional.empty();
            case Chat chat -> Optional.of(chat);
            case JidProvider provider -> {
                var targetJid = provider.toJid();
                if (targetJid.hasUserServer()) {
                    var jidChat = chats.get(targetJid);
                    if (jidChat != null) {
                        yield Optional.of(jidChat);
                    }
                    yield contacts.findLidByPhone(targetJid).map(chats::get);
                } else if (targetJid.hasLidServer()) {
                    var lidChat = chats.get(targetJid);
                    if (lidChat != null) {
                        yield Optional.of(lidChat);
                    }
                    var phone = contacts.findPhoneByLid(targetJid);
                    if (phone.isEmpty()) {
                        yield Optional.empty();
                    }
                    var phoneChat = chats.get(phone.get());
                    if (phoneChat != null) {
                        yield Optional.of(phoneChat);
                    }
                    yield contacts.findLidByPhone(phone.get()).map(chats::get);
                } else {
                    yield Optional.ofNullable(chats.get(targetJid));
                }
            }
        };
    }

    @Override
    public Optional<? extends LinkedMessageInfo> findMessageById(JidProvider provider, String id) {
        if (Log.TRACE) LOGGER.log(Level.TRACE, "looking up message {0} for {1}", id, provider);
        return provider == null || id == null ? Optional.empty() : switch (provider) {
            case Chat chat -> findMessageById(chat, id);
            case Newsletter newsletter -> findMessageById(newsletter, id);
            case Jid contactJid -> {
                if (contactJid.server().type() == JidServer.Type.NEWSLETTER) {
                    yield findNewsletterByJid(contactJid).flatMap(newsletter -> findMessageById(newsletter, id));
                } else if (Jid.statusBroadcastAccount().equals(contactJid)) {
                    yield messageStore.getStatusMessage(id);
                } else {
                    yield findChatByJid(contactJid).flatMap(chat -> findMessageById(chat, id));
                }
            }
            case JidProvider other -> findChatByJid(other.toJid()).flatMap(chat -> findMessageById(chat, id));
        };
    }

    @Override
    public Optional<NewsletterMessageInfo> findMessageById(Newsletter newsletter, String id) {
        if (newsletter == null || id == null) {
            return Optional.empty();
        }
        try {
            var serverId = Integer.parseInt(id);
            var byServerId = messageStore.getNewsletterMessageByServerId(newsletter.jid(), serverId);
            if (byServerId.isPresent()) {
                return byServerId;
            }
        } catch (NumberFormatException _) {
            // Fall through to the scan below.
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "falling back to full newsletter scan for message {0}", id);
        try (var stream = newsletter.messages()) {
            return stream
                    .filter(entry -> Objects.equals(id, entry.key().id().orElse(null)))
                    .findFirst();
        }
    }

    @Override
    public Optional<ChatMessageInfo> findMessageById(Chat chat, String id) {
        if (chat == null || id == null) {
            return Optional.empty();
        }
        return chat.getMessageById(id);
    }

    @Override
    public Collection<Chat> chats() {
        return List.copyOf(chats.values());
    }

    @Override
    public Chat addNewChat(Jid chatJid) {
        Objects.requireNonNull(chatJid, "chatJid cannot be null");
        var chat = new PersistentChatBuilder()
                .jid(chatJid)
                .build();
        chat.setMessageStore(messageStore);
        chats.put(chatJid, chat);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat created: {0}", chatJid);
        return chat;
    }

    @Override
    public Optional<Chat> removeChat(JidProvider chatJid) {
        if (chatJid == null) {
            return Optional.empty();
        }
        var targetJid = chatJid.toJid();
        Optional<Chat> removed;
        if (targetJid.hasUserServer()) {
            var jidChat = chats.remove(targetJid);
            removed = jidChat != null
                    ? Optional.of(jidChat)
                    : contacts.findLidByPhone(targetJid).map(chats::remove);
        } else if (targetJid.hasLidServer()) {
            var lidChat = chats.remove(targetJid);
            removed = lidChat != null
                    ? Optional.of(lidChat)
                    : contacts.findPhoneByLid(targetJid).map(chats::remove);
        } else {
            removed = Optional.ofNullable(chats.remove(targetJid));
        }
        removed.ifPresent(chat -> messageStore.removeChatMessages(chat.jid()));
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "chat removed: {0}, found={1}", targetJid, removed.isPresent());
        return removed;
    }

    @Override
    public ChatMessageInfo addStatus(ChatMessageInfo messageInfo) {
        Objects.requireNonNull(messageInfo, "messageInfo cannot be null");
        messageStore.putStatusMessage(messageInfo);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "status message added");
        return messageInfo;
    }

    @Override
    public Optional<ChatMessageInfo> removeStatus(String id) {
        if (id == null) {
            return Optional.empty();
        }
        var removed = messageStore.removeStatusMessage(id);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "status message {0} removed: {1}", id, removed.isPresent());
        return removed;
    }

    @Override
    public Optional<ChatMessageInfo> findStatusById(String id) {
        return id == null ? Optional.empty() : messageStore.getStatusMessage(id);
    }

    @Override
    public Optional<Newsletter> findNewsletterByJid(JidProvider jid) {
        if (jid == null) {
            return Optional.empty();
        }
        Newsletter newsletter = newsletters.get(jid.toJid());
        return Optional.ofNullable(newsletter);
    }

    @Override
    public Collection<Newsletter> newsletters() {
        return List.copyOf(newsletters.values());
    }

    @Override
    public Newsletter addNewNewsletter(Jid newsletterJid) {
        Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        var newsletter = new PersistentNewsletterBuilder()
                .jid(newsletterJid)
                .build();
        newsletter.setMessageStore(messageStore);
        newsletters.put(newsletter.jid(), newsletter);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "newsletter created: {0}", newsletterJid);
        return newsletter;
    }

    @Override
    public Optional<Newsletter> removeNewsletter(JidProvider newsletterJid) {
        if (newsletterJid == null) {
            return Optional.empty();
        }
        Newsletter removed = newsletters.remove(newsletterJid.toJid());
        if (removed != null) {
            messageStore.removeNewsletterMessages(removed.jid());
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "newsletter removed: {0}, found={1}", newsletterJid, removed != null);
        return Optional.ofNullable(removed);
    }

    @Override
    public Optional<? extends LinkedMessageInfo> findMessageByKey(MessageKey key) {
        var id = key.id();
        if (id.isEmpty()) {
            return Optional.empty();
        }
        var parentJid = key.parentJid();
        if (parentJid.isEmpty()) {
            return Optional.empty();
        }
        return findChatByJid(parentJid.get())
                .flatMap(chat -> chat.getMessageById(id.get()));
    }

    @Override
    public Collection<ChatMessageInfo> status() {
        try (var stream = messageStore.streamStatusMessages()) {
            return stream.toList();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentLinkedWhatsAppChatStore that)) {
            return false;
        }
        return Objects.equals(favoriteChats(), that.favoriteChats())
               && Objects.equals(mentionEveryoneMuteExpirationsMap(), that.mentionEveryoneMuteExpirationsMap())
               && Objects.equals(chats, that.chats)
               && Objects.equals(newsletters, that.newsletters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(favoriteChats(), mentionEveryoneMuteExpirationsMap(), chats, newsletters);
    }
}
