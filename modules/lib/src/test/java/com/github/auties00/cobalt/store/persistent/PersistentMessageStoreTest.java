package com.github.auties00.cobalt.store.persistent;

import com.github.auties00.cobalt.client.linked.LinkedWhatsAppClientType;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStoreFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end coverage of the persistent store's libmdbx-backed message layer, driven through the
 * public {@link LinkedWhatsAppStoreFactory#persistent(Path)} surface so it exercises the full
 * PersistentStore -&gt; PersistentMessageStore -&gt; libmdbx path against a real env. The native
 * library is loaded for real; there is no availability gating, so a broken native bundle fails the
 * build. Each test closes the env through {@link LinkedWhatsAppStore#delete()} so the temp directory can
 * be removed on Windows, where an open memory-mapped file cannot be unlinked.
 */
class PersistentMessageStoreTest {
    private static final Jid SELF = Jid.of("19999999999@s.whatsapp.net");

    private static LinkedWhatsAppStore store(Path dir) throws Exception {
        return LinkedWhatsAppStoreFactory.persistent(dir).create(LinkedWhatsAppClientType.WEB, UUID.randomUUID());
    }

    private static ChatMessageInfo message(String id, Jid parentJid, String body) {
        var key = new MessageKeyBuilder()
                .id(id)
                .parentJid(parentJid)
                .fromMe(true)
                .senderJid(SELF)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .senderJid(SELF)
                .message(LinkedMessageContainer.of(body))
                .build();
    }

    @Test
    @DisplayName("round-trips chat messages and keeps memcmp cursor order for oldest/newest")
    void chatRoundTrip(@TempDir Path dir) throws Exception {
        var store = store(dir);
        try {
            var chatJid = Jid.of("111111@s.whatsapp.net");
            var chat = store.chatStore().addNewChat(chatJid);
            chat.addMessage(message("MSG0", chatJid, "first"));
            chat.addMessage(message("MSG1", chatJid, "second"));
            chat.addMessage(message("MSG2", chatJid, "third"));

            assertEquals(3, chat.messageCount());
            assertTrue(chat.getMessageById("MSG1").isPresent());
            assertEquals("MSG0", chat.oldestMessage().orElseThrow().key().id().orElseThrow());
            assertEquals("MSG2", chat.newestMessage().orElseThrow().key().id().orElseThrow());

            try (var stream = chat.messages()) {
                assertEquals(List.of("MSG0", "MSG1", "MSG2"),
                        stream.map(m -> m.key().id().orElseThrow()).toList());
            }

            assertTrue(chat.removeMessage("MSG1"));
            assertFalse(chat.removeMessage("MSG1"));
            assertEquals(2, chat.messageCount());
            assertTrue(chat.getMessageById("MSG1").isEmpty());

            chat.removeMessages();
            assertEquals(0, chat.messageCount());
        } finally {
            store.delete();
        }
    }

    @Test
    @DisplayName("stores, finds and removes status-feed messages")
    void statusFeed(@TempDir Path dir) throws Exception {
        var store = store(dir);
        try {
            var statusJid = Jid.statusBroadcastAccount();
            store.chatStore().addStatus(message("ST0", statusJid, "status one"));
            store.chatStore().addStatus(message("ST1", statusJid, "status two"));

            assertEquals(2, store.chatStore().status().size());
            assertTrue(store.chatStore().findStatusById("ST0").isPresent());

            assertEquals("ST1", store.chatStore().removeStatus("ST1").orElseThrow().key().id().orElseThrow());
            assertTrue(store.chatStore().findStatusById("ST1").isEmpty());
            assertEquals(1, store.chatStore().status().size());
        } finally {
            store.delete();
        }
    }

    @Test
    @DisplayName("group-commits concurrent writes from many virtual threads durably")
    void concurrentWrites(@TempDir Path dir) throws Exception {
        var store = store(dir);
        try {
            var chatJid = Jid.of("222222@s.whatsapp.net");
            var chat = store.chatStore().addNewChat(chatJid);
            var count = 500;
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                IntStream.range(0, count).forEach(i ->
                        executor.submit(() -> chat.addMessage(message(String.format("M%04d", i), chatJid, "body" + i))));
            }
            assertEquals(count, chat.messageCount());

            var ids = ConcurrentHashMap.<String>newKeySet();
            try (var stream = chat.messages()) {
                stream.forEach(m -> ids.add(m.key().id().orElseThrow()));
            }
            assertEquals(count, ids.size());
            assertTrue(chat.getMessageById("M0000").isPresent());
            assertTrue(chat.getMessageById(String.format("M%04d", count - 1)).isPresent());
        } finally {
            store.delete();
        }
    }
}
