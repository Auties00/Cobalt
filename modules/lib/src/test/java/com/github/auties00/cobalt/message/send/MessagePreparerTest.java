package com.github.auties00.cobalt.message.send;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageStatus;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;
import com.github.auties00.cobalt.wam.TestWamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link MessagePreparer} populating a {@link ChatMessageInfo} (or
 * {@link com.github.auties00.cobalt.wire.linked.newsletter.NewsletterMessageInfo})
 * from a raw {@link LinkedMessageContainer}: parent JID equals the chat JID with
 * {@code fromMe} set, sender matches the local JID, {@link MessageStatus}
 * starts {@code PENDING}, the 32-byte {@code messageSecret} is mirrored onto
 * the inner container's {@code messageContextInfo}, each call mints a fresh
 * wire id, status-broadcast chats get the {@code broadcast} flag, and the
 * failure branches (not-logged-in, newsletter-not-joined, null args) reject.
 * The suite lives in the production package so it can drive the package-private
 * preparer directly instead of going through {@link MessageSendingService}.
 */
@DisplayName("MessagePreparer")
class MessagePreparerTest {

    private static final Jid SELF_PN = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("258252122116273@lid");
    private static final Jid CHAT_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid GROUP_JID = Jid.of("120363023250764418@g.us");

    @Test
    @DisplayName("prepareChat: returns a ChatMessageInfo populated with key, status, secret, and self JID")
    void prepareChatBasic() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var prepared = preparer.prepareChat(CHAT_PN, LinkedMessageContainer.of("hi"));

        assertNotNull(prepared);
        assertEquals(MessageStatus.PENDING, prepared.status().orElseThrow());
        assertEquals(SELF_PN, prepared.senderJid().orElseThrow());

        var key = prepared.key();
        assertEquals(CHAT_PN, key.parentJid().orElseThrow());
        assertTrue(key.fromMe(), "outgoing prepared message must have fromMe=true");
        assertTrue(key.id().isPresent(), "key must carry a generated id");
    }

    @Test
    @DisplayName("prepareChat: messageSecret is exactly 32 bytes and stamped on the container's messageContextInfo")
    void prepareChatStampsSecret() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var prepared = preparer.prepareChat(CHAT_PN, LinkedMessageContainer.of("hi"));

        var secret = prepared.messageSecret().orElseThrow();
        assertEquals(32, secret.length, "messageSecret must be 32 bytes");

        // The same secret must be stamped into the prepared container's
        // messageContextInfo so downstream stanza builders can read it.
        var ctxInfo = prepared.message().messageContextInfo().orElseThrow();
        var stampedSecret = ctxInfo.messageSecret().orElseThrow();
        assertEquals(32, stampedSecret.length);
        Assertions.assertArrayEquals(secret, stampedSecret,
                "the container's messageContextInfo must carry the same secret as the outer info");
    }

    @Test
    @DisplayName("prepareChat: each call generates a distinct id and secret")
    void prepareChatFreshIdAndSecret() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var first = preparer.prepareChat(CHAT_PN, LinkedMessageContainer.of("hi"));
        var second = preparer.prepareChat(CHAT_PN, LinkedMessageContainer.of("hi"));

        Assertions.assertNotEquals(
                first.key().id().orElseThrow(),
                second.key().id().orElseThrow(),
                "id generator must produce a fresh id per call");
        Assertions.assertFalse(
                Arrays.equals(
                        first.messageSecret().orElseThrow(),
                        second.messageSecret().orElseThrow()),
                "messageSecret must be sampled fresh per call");
    }

    @Test
    @DisplayName("prepareChat: groups receive the group JID on the key")
    void prepareChatToGroup() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var prepared = preparer.prepareChat(GROUP_JID, LinkedMessageContainer.of("group"));
        assertEquals(GROUP_JID, prepared.key().parentJid().orElseThrow());
    }

    @Test
    @DisplayName("prepareChat: broadcast flag is set when chatJid is the status broadcast account")
    void prepareChatBroadcastFlag() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var prepared = preparer.prepareChat(Jid.statusBroadcastAccount(), LinkedMessageContainer.of("status"));
        assertTrue(prepared.broadcast(),
                "broadcast flag must be true when targeting the status broadcast account");
    }

    @Test
    @DisplayName("prepareChat: store with no JID throws IllegalStateException (not logged in)")
    void prepareChatNotLoggedIn() {
        var store = MessageFixtures.temporaryStore(SELF_PN, null);
        store.accountStore().setJid(null);
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        assertThrows(IllegalStateException.class,
                () -> preparer.prepareChat(CHAT_PN, LinkedMessageContainer.of("hi")));
    }

    @Test
    @DisplayName("prepareChat: null arguments throw NullPointerException")
    void prepareChatNullArgs() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        assertThrows(NullPointerException.class,
                () -> preparer.prepareChat(null, LinkedMessageContainer.of("hi")));
        assertThrows(NullPointerException.class,
                () -> preparer.prepareChat(CHAT_PN, null));
    }

    @Test
    @DisplayName("prepareNewsletter: throws IllegalArgumentException when the user has not joined the newsletter")
    void prepareNewsletterNotJoined() {
        var store = store();
        var preparer = new MessagePreparer(store, TestWamService.create(TestWhatsAppClient.create().withStore(store)));
        var newsletter = Jid.of("120363402045452944@newsletter");
        assertThrows(IllegalArgumentException.class,
                () -> preparer.prepareNewsletter(newsletter, LinkedMessageContainer.of("hi")));
    }

    @Test
    @DisplayName("constructor: null store throws NullPointerException")
    void constructorNullStore() {
        assertThrows(NullPointerException.class, () -> new MessagePreparer(null, TestWamService.create(TestWhatsAppClient.create())));
    }

    private static LinkedWhatsAppStore store() {
        return MessageFixtures.temporaryStore(SELF_PN, SELF_LID);
    }
}
