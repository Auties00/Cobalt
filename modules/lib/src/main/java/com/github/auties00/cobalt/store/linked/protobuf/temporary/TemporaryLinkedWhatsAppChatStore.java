package com.github.auties00.cobalt.store.linked.protobuf.temporary;

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
import com.github.auties00.collections.ConcurrentLinkedHashMap;

import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The {@link ProtobufLinkedWhatsAppChatStore} variant that holds chats, newsletters and the status feed entirely in
 * RAM and never touches disk.
 *
 * @implNote
 * This implementation keeps the chat and newsletter maps as {@link ConcurrentHashMap} and the status
 * feed as a {@link ConcurrentLinkedHashMap} keyed by message id so insertion order is preserved.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class TemporaryLinkedWhatsAppChatStore extends ProtobufLinkedWhatsAppChatStore {
    /**
     * The logger for {@link TemporaryLinkedWhatsAppChatStore}.
     */
    private static final System.Logger LOGGER = Log.get(TemporaryLinkedWhatsAppChatStore.class);

    /**
     * The map of chat JIDs to their in-memory {@link TemporaryChat} entries.
     */
    private final ConcurrentHashMap<Jid, TemporaryChat> chats;

    /**
     * The map of newsletter JIDs to their in-memory {@link TemporaryNewsletter} entries.
     */
    private final ConcurrentHashMap<Jid, TemporaryNewsletter> newsletters;

    /**
     * The status feed keyed by message id, preserving insertion order.
     */
    private final ConcurrentLinkedHashMap<String, ChatMessageInfo> status;

    /**
     * Constructs an in-memory chat sub-store with empty chat, newsletter and status maps.
     *
     * @param favoriteChats                     the favourite chat JIDs, or {@code null} for an empty list
     * @param mentionEveryoneMuteExpirationsMap the mention-everyone mute map, or {@code null} for an empty map
     */
    TemporaryLinkedWhatsAppChatStore(List<Jid> favoriteChats, ConcurrentMap<Jid, ChatMute> mentionEveryoneMuteExpirationsMap) {
        super(favoriteChats, mentionEveryoneMuteExpirationsMap);
        this.chats = new ConcurrentHashMap<>();
        this.newsletters = new ConcurrentHashMap<>();
        this.status = new ConcurrentLinkedHashMap<>();
    }

    @Override
    public Optional<Chat> findChatByJid(JidProvider jid) {
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
        return provider == null || id == null ? Optional.empty() : switch (provider) {
            case Chat chat -> findMessageById(chat, id);
            case Newsletter newsletter -> findMessageById(newsletter, id);
            case Jid contactJid -> {
                if (contactJid.server().type() == JidServer.Type.NEWSLETTER) {
                    yield findNewsletterByJid(contactJid).flatMap(newsletter -> findMessageById(newsletter, id));
                } else if (Jid.statusBroadcastAccount().equals(contactJid)) {
                    yield Optional.ofNullable(status.get(id));
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
        try (var stream = newsletter.messages()) {
            return stream
                    .filter(entry -> Objects.equals(id, entry.key().id().orElse(null)) || Objects.equals(id, String.valueOf(entry.serverId())))
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
        var chat = new TemporaryChat(chatJid);
        chats.put(chatJid, chat);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "added new chat {0}", chatJid);
        return chat;
    }

    @Override
    public Optional<Chat> removeChat(JidProvider chatJid) {
        if (chatJid == null) {
            return Optional.empty();
        }
        var targetJid = chatJid.toJid();
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "removing chat {0}", targetJid);
        if (targetJid.hasUserServer()) {
            var jidChat = chats.remove(targetJid);
            if (jidChat != null) {
                return Optional.of(jidChat);
            }
            return contacts.findLidByPhone(targetJid).map(chats::remove);
        }
        if (targetJid.hasLidServer()) {
            var lidChat = chats.remove(targetJid);
            if (lidChat != null) {
                return Optional.of(lidChat);
            }
            return contacts.findPhoneByLid(targetJid).map(chats::remove);
        }
        return Optional.ofNullable(chats.remove(targetJid));
    }

    @Override
    public ChatMessageInfo addStatus(ChatMessageInfo messageInfo) {
        Objects.requireNonNull(messageInfo, "messageInfo cannot be null");
        messageInfo.key().id().ifPresent(id -> status.put(id, messageInfo));
        return messageInfo;
    }

    @Override
    public Optional<ChatMessageInfo> removeStatus(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(status.remove(id));
    }

    @Override
    public Optional<ChatMessageInfo> findStatusById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(status.get(id));
    }

    @Override
    public Optional<Newsletter> findNewsletterByJid(JidProvider jid) {
        return jid == null
                ? Optional.empty()
                : Optional.ofNullable(newsletters.get(jid.toJid()));
    }

    @Override
    public Collection<Newsletter> newsletters() {
        return List.copyOf(newsletters.values());
    }

    @Override
    public Newsletter addNewNewsletter(Jid newsletterJid) {
        Objects.requireNonNull(newsletterJid, "newsletterJid cannot be null");
        var newsletter = new TemporaryNewsletter(newsletterJid);
        newsletters.put(newsletterJid, newsletter);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "added new newsletter {0}", newsletterJid);
        return newsletter;
    }

    @Override
    public Optional<Newsletter> removeNewsletter(JidProvider newsletterJid) {
        if (Log.DEBUG && newsletterJid != null) LOGGER.log(Level.DEBUG, "removing newsletter {0}", newsletterJid.toJid());
        return newsletterJid == null
                ? Optional.empty()
                : Optional.ofNullable(newsletters.remove(newsletterJid.toJid()));
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
        return List.copyOf(status.values());
    }
}
