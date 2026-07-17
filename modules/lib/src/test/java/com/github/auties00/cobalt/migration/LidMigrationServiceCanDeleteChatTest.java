package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.Chat;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingMode;
import com.github.auties00.cobalt.wire.linked.chat.ChatDisappearingModeBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfo;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.linked.chat.ChatMute;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the deletability heuristic {@link LidMigrationService#canDeleteChat(Chat)}
 * applies to a 1:1 thread that resolves to no LID mapping anywhere, one branch
 * of the cascade per test against an isolated in-memory chat.
 */
@DisplayName("LidMigrationService.canDeleteChat")
class LidMigrationServiceCanDeleteChatTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid PEER_PN = Jid.of("393495089819@s.whatsapp.net");

    private record Harness(TestWhatsAppClient client, LiveLidMigrationService service) {}

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, service);
    }

    private static Chat newChat(Harness h) {
        return h.client.store().chatStore().addNewChat(PEER_PN);
    }

    private static ChatMessageInfo stubMessage(ChatMessageInfo.StubType stubType, Instant ts) {
        var key = new MessageKeyBuilder()
                .id("stub-" + stubType.name())
                .fromMe(false)
                .parentJid(PEER_PN)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.empty())
                .stubType(stubType)
                .timestamp(ts)
                .build();
    }

    private static ChatMessageInfo textMessage(Instant ts) {
        var key = new MessageKeyBuilder()
                .id("text")
                .fromMe(false)
                .parentJid(PEER_PN)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("hello"))
                .timestamp(ts)
                .build();
    }

    private static ChatMessageInfo broadcastMessage(Instant ts) {
        var key = new MessageKeyBuilder()
                .id("broadcast")
                .fromMe(false)
                .parentJid(PEER_PN)
                .build();
        return new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.empty())
                .broadcast(true)
                .timestamp(ts)
                .build();
    }

    @Test
    @DisplayName("empty chat is deletable (vacuous all-safe-stubs)")
    void emptyChat() {
        var h = build();
        var chat = newChat(h);
        assertTrue(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("E2E_ENCRYPTED stubs only -> deletable")
    void allE2eEncryptedStubs() {
        var h = build();
        var chat = newChat(h);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED_NOW, Instant.now()));
        assertTrue(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("DISAPPEARING_MODE stubs only -> deletable")
    void allDisappearingModeStubs() {
        var h = build();
        var chat = newChat(h);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.DISAPPEARING_MODE, Instant.now()));
        assertTrue(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("safe stubs + one call-log entry -> deletable (call-log path)")
    void safeStubsPlusCallLog() {
        var h = build();
        var chat = newChat(h);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.CALL_MISSED_VOICE, Instant.now()));
        assertTrue(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("safe stubs + one regular text message -> not deletable")
    void safeStubsPlusText() {
        var h = build();
        var chat = newChat(h);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        chat.addMessage(textMessage(Instant.now()));
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("safe stubs + broadcast, pairing-ts <= oldest msg -> deletable (broadcast-exempt)")
    void broadcastExemptPairingBeforeOldest() {
        var h = build();
        var pairing = Instant.parse("2026-01-01T00:00:00Z");
        var oldest = Instant.parse("2026-01-02T00:00:00Z");
        h.client.store().accountStore().setPairingTimestamp(pairing);

        var chat = newChat(h);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, oldest.plusSeconds(60)));
        chat.addMessage(broadcastMessage(oldest));

        assertTrue(h.service.canDeleteChat(chat),
                "pairing precedes the oldest message, so broadcast-only history is safe to drop");
    }

    @Test
    @DisplayName("safe stubs + broadcast, pairing-ts > oldest msg -> not deletable")
    void broadcastNoExemptPairingAfterOldest() {
        var h = build();
        var oldest = Instant.parse("2026-01-01T00:00:00Z");
        var pairing = Instant.parse("2026-01-02T00:00:00Z");
        h.client.store().accountStore().setPairingTimestamp(pairing);

        var chat = newChat(h);
        chat.addMessage(broadcastMessage(oldest));

        assertFalse(h.service.canDeleteChat(chat),
                "pairing happened after the broadcast was received, so the broadcast belongs to user history");
    }

    @Test
    @DisplayName("safe stubs + broadcast, no pairing-ts -> not deletable")
    void broadcastNoExemptNoPairing() {
        var h = build();
        var chat = newChat(h);
        chat.addMessage(broadcastMessage(Instant.now()));
        // No pairing timestamp set.
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("ephemeralExpiration set -> not deletable")
    void ephemeralExpirationBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setEphemeralExpiration(ChatEphemeralTimer.ONE_WEEK);
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("ephemeralSettingTimestamp set -> not deletable")
    void ephemeralSettingTimestampBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setEphemeralSettingTimestamp(Instant.now());
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("ephemeral + ACCOUNT_SETTING trigger + DISAPPEARING_MODE stub -> ephemeral-exempt -> deletable")
    void ephemeralAccountSettingExempt() {
        var h = build();
        var chat = newChat(h);
        chat.setEphemeralExpiration(ChatEphemeralTimer.ONE_DAY);
        chat.setDisappearingMode(new ChatDisappearingModeBuilder()
                .trigger(ChatDisappearingMode.Trigger.ACCOUNT_SETTING)
                .build());
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.DISAPPEARING_MODE, Instant.now()));
        assertTrue(h.service.canDeleteChat(chat),
                "ACCOUNT_SETTING trigger + DISAPPEARING_MODE stub bypasses the ephemeral block");
    }

    @Test
    @DisplayName("ephemeral + ACCOUNT_SETTING trigger but no DISAPPEARING_MODE stub -> not deletable")
    void ephemeralAccountSettingNoStubBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setEphemeralExpiration(ChatEphemeralTimer.ONE_DAY);
        chat.setDisappearingMode(new ChatDisappearingModeBuilder()
                .trigger(ChatDisappearingMode.Trigger.ACCOUNT_SETTING)
                .build());
        chat.addMessage(stubMessage(ChatMessageInfo.StubType.E2E_ENCRYPTED, Instant.now()));
        assertFalse(h.service.canDeleteChat(chat),
                "exempt only fires when DISAPPEARING_MODE stub is present");
    }

    @Test
    @DisplayName("locked=true -> not deletable")
    void lockedBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setLocked(true);
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("archived=true -> not deletable")
    void archivedBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setArchived(true);
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("muted -> not deletable")
    void mutedBlocks() {
        var h = build();
        var chat = newChat(h);
        chat.setMute(ChatMute.muted());
        assertFalse(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("notMuted -> still deletable for empty chat")
    void notMutedDoesNotBlock() {
        var h = build();
        var chat = newChat(h);
        chat.setMute(ChatMute.notMuted());
        assertTrue(h.service.canDeleteChat(chat));
    }

    @Test
    @DisplayName("regression: stub type set but message body non-empty -> not a migration-safe stub -> not deletable")
    void stubTypeWithNonEmptyMessageNotSafe() {
        var h = build();
        var chat = newChat(h);
        var key = new MessageKeyBuilder()
                .id("real-msg-with-stub-type")
                .fromMe(false)
                .parentJid(PEER_PN)
                .build();
        var msg = new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("not empty"))
                .stubType(ChatMessageInfo.StubType.E2E_ENCRYPTED)
                .timestamp(Instant.now())
                .build();
        chat.addMessage(msg);
        assertFalse(h.service.canDeleteChat(chat),
                "isMigrationSafeStub guards on !message.isEmpty() before checking stubType");
    }
}
