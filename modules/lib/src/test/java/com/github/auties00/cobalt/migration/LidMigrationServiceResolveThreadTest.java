package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.exception.linked.WhatsAppLidMigrationException;
import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayloadBuilder;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link LidMigrationService#resolveThread(com.github.auties00.cobalt.wire.linked.chat.Chat)} and
 * its two-argument overload: every path of the classifier cascade that decides whether to migrate,
 * keep, or delete a chat. The cases walk the already-LID branches (including ctwa-origin
 * promotion), the server-keyed {@code Keep} branches (group, newsletter, broadcast, bot,
 * duplicate-merge), the migrate branches (primary cache hit, local LID fallback, original-LID cache
 * fallback) with their mismatch, split-thread, and obsolete-mappings throws, and the deletability
 * branches. Each case feeds the primary caches via
 * {@link LidMigrationService#changeLid(Jid, Jid, Jid)} on an isolated store so branches stay
 * independent.
 */
@DisplayName("LidMigrationService.resolveThread")
class LidMigrationServiceResolveThreadTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid PEER_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid PEER_LID = Jid.of("258252122116273@lid");
    private static final Jid OTHER_LID = Jid.of("999999999999999@lid");
    private static final Jid GROUP = Jid.of("120363012345678901@g.us");
    private static final Jid NEWSLETTER = Jid.of("120363023456789012@newsletter");
    private static final Jid BROADCAST = Jid.of("19254863482@broadcast");
    private static final Jid STATUS_BROADCAST = Jid.of("status@broadcast");
    private static final Jid BOT = Jid.of("867051314767696@bot");

    private record Harness(TestWhatsAppClient client, TestABPropsService props, LiveLidMigrationService service) {}

    private static Harness build(TestABPropsService props) {
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, props, service);
    }

    private static Harness build() {
        return build(TestABPropsService.builder().build());
    }

    @Test
    @DisplayName("LID chat, ctwa origin, primary latest cache matches -> ALREADY_LID, origin promoted to general")
    void lidCtwaMatchesPrimary() {
        var h = build();
        // Populate primaryPnToLatestLidCache with PEER_PN -> PEER_LID.
        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_LID);
        chat.setLidOriginType("ctwa");

        var resolution = h.service.resolveThread(chat);

        assertInstanceOf(LidMigrationResolution.Keep.class, resolution);
        assertEquals(LidMigrationResolution.KeepReason.ALREADY_LID,
                ((LidMigrationResolution.Keep) resolution).reason());
        assertEquals("general", chat.lidOriginType().orElseThrow(),
                "ctwa origin is promoted to general after primary-match");
    }

    @Test
    @DisplayName("LID chat, ctwa origin, primary latest cache empty -> ALREADY_LID, origin unchanged")
    void lidCtwaNoMatch() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_LID);
        chat.setLidOriginType("ctwa");

        var resolution = h.service.resolveThread(chat);

        assertInstanceOf(LidMigrationResolution.Keep.class, resolution);
        assertEquals("ctwa", chat.lidOriginType().orElseThrow(),
                "origin stays ctwa when no primary cache entry matches");
    }

    @Test
    @DisplayName("LID chat, non-ctwa origin -> ALREADY_LID, origin unchanged")
    void lidNonCtwa() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_LID);
        chat.setLidOriginType("general");

        var resolution = h.service.resolveThread(chat);

        assertInstanceOf(LidMigrationResolution.Keep.class, resolution);
        assertEquals("general", chat.lidOriginType().orElseThrow());
    }

    @Test
    @DisplayName("group server -> GROUP_OR_COMMUNITY")
    void groupKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.GROUP_OR_COMMUNITY,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("newsletter server -> NEWSLETTER")
    void newsletterKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(NEWSLETTER);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.NEWSLETTER,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("regular broadcast -> BROADCAST")
    void broadcastKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(BROADCAST);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.BROADCAST,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("status broadcast account -> STATUS_BROADCAST")
    void statusBroadcastKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(STATUS_BROADCAST);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.STATUS_BROADCAST,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("bot server -> BOT")
    void botKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(BOT);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.BOT,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("PN chat with phoneNumberhDuplicateLidThread=true -> DUPLICATE_WILL_MERGE")
    void duplicateMergeKept() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setPhoneNumberDuplicateLidThread(true);
        var resolution = h.service.resolveThread(chat);
        assertEquals(LidMigrationResolution.KeepReason.DUPLICATE_WILL_MERGE,
                ((LidMigrationResolution.Keep) resolution).reason());
    }

    @Test
    @DisplayName("PN chat, primary hit, localLid null -> Migrate(primaryLid)")
    void migrateOnPrimaryNoLocal() {
        var h = build();
        h.service.changeLid(PEER_PN, PEER_LID, null);
        // Chat is created without setLid, so chat.lid() is empty: the localLid-null branch.
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);

        var resolution = h.service.resolveThread(chat);

        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID, ((LidMigrationResolution.Migrate) resolution).targetLid());
    }

    @Test
    @DisplayName("PN chat, primary hit, localLid matches primary -> Migrate(primaryLid)")
    void migrateOnPrimaryLocalMatches() {
        var h = build();
        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(PEER_LID); // Local matches primary.

        var resolution = h.service.resolveThread(chat);

        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID, ((LidMigrationResolution.Migrate) resolution).targetLid());
    }

    @Test
    @DisplayName("PN chat, primary hit, localLid differs, log_out_on_mismatch=true, chat newer than sync -> PrimaryMappingsObsolete")
    void primaryMappingsObsoleteThrows() {
        // Default log_out_on_mismatch=true.
        var h = build();
        // Prime primary cache via changeLid.
        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID); // Local differs from primary.
        chat.setConversationTimestamp(Instant.now()); // Chat is "fresh" relative to no sync timestamp (EPOCH).

        assertThrows(WhatsAppLidMigrationException.PrimaryMappingsObsolete.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("PN chat, primary hit, localLid differs, log_out_on_mismatch=true, chat older than sync -> Migrate(primaryLid)")
    void migrateOnPrimaryChatOlderThanSync() {
        // Default log_out_on_mismatch=true. The mismatch path inspects the chat's conversation timestamp:
        // a timestamp BEFORE the effective sync timestamp means the primary is fresher and we
        // proceed with the migration; only chat timestamps at-or-after the sync trigger the throw.
        var h = build();
        h.service.changeLid(PEER_PN, PEER_LID, null);

        // Advance the effective sync timestamp to "now" via observeChatDbMigrationTimestamp.
        var syncNow = Instant.now();
        h.service.observeChatDbMigrationTimestamp(syncNow);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID); // Local differs from primary.
        chat.setConversationTimestamp(syncNow.minusSeconds(3600)); // Older than the effective sync.

        var resolution = h.service.resolveThread(chat);
        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID, ((LidMigrationResolution.Migrate) resolution).targetLid(),
                "older chat is not 'fresher than the sync', so we accept the primary's mapping");
    }

    @Test
    @DisplayName("getEffectiveSyncTimestamp: chatDb ts present -> wins over receive ts and EPOCH")
    void effectiveSyncTimestampPrefersChatDb() {
        var h = build();
        h.service.changeLid(PEER_PN, PEER_LID, null);
        // Set chatDb ts to a known recent value.
        var chatDbTs = Instant.parse("2026-03-01T00:00:00Z");
        h.service.observeChatDbMigrationTimestamp(chatDbTs);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID);
        // Chat ts equals chatDb ts -> "at or after" the sync timestamp -> throws.
        chat.setConversationTimestamp(chatDbTs);

        assertThrows(WhatsAppLidMigrationException.PrimaryMappingsObsolete.class,
                () -> h.service.resolveThread(chat),
                "chatDb ts is the effective sync; chat ts == chatDb ts triggers the throw");
    }

    @Test
    @DisplayName("getEffectiveSyncTimestamp: only receive ts (no chatDb) -> receive ts is effective")
    void effectiveSyncTimestampFallsBackToReceive() throws InterruptedException {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var h = build(props);

        // Send an empty payload -> chatDbMigrationTimestamp stays null, receiveTimestamp = now.
        h.service.initialize();
        h.service.enableMigration();
        h.service.processProtocolMessage(
                new LIDMigrationMappingSyncPayloadBuilder()
                        .pnToLidMappings(List.of())
                        .build());
        // Now state is COMPLETE; resolveThread is still callable.

        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID);
        chat.setConversationTimestamp(Instant.now().plusSeconds(60));
        // Chat ts is "fresh" relative to the receive ts -> triggers throw.
        assertThrows(WhatsAppLidMigrationException.PrimaryMappingsObsolete.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("getEffectiveSyncTimestamp: neither chatDb nor receive set -> EPOCH; any non-empty chat ts triggers throw")
    void effectiveSyncTimestampDefaultsToEpoch() {
        var h = build();
        // No observeChatDbMigrationTimestamp call; no processProtocolMessage call. Effective = EPOCH.
        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID);
        // Any real timestamp is after EPOCH -> throws.
        chat.setConversationTimestamp(Instant.parse("2026-01-01T00:00:00Z"));
        assertThrows(WhatsAppLidMigrationException.PrimaryMappingsObsolete.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("PN chat, primary hit, localLid differs, log_out_on_mismatch=false -> Migrate(primaryLid)")
    void migrateOnPrimaryLogOutDisabled() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_LOG_OUT_ON_MISMATCH, false)
                .build();
        var h = build(props);
        h.service.changeLid(PEER_PN, PEER_LID, null);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(OTHER_LID);
        chat.setConversationTimestamp(Instant.now());

        var resolution = h.service.resolveThread(chat);
        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID, ((LidMigrationResolution.Migrate) resolution).targetLid());
    }

    @Test
    @DisplayName("PN chat, no primary, localLid present, no collision -> Migrate(localLid)")
    void migrateOnLocalNoCollision() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(PEER_LID);

        var resolution = h.service.resolveThread(chat);
        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID, ((LidMigrationResolution.Migrate) resolution).targetLid());
    }

    @Test
    @DisplayName("PN chat, no primary, localLid collides with existing LID thread -> SplitThreadMismatch")
    void splitThreadMismatchThrows() {
        var h = build();
        // Pre-existing LID-keyed chat with the same user (collision).
        h.client.store().chatStore().addNewChat(PEER_LID);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(PEER_LID);

        assertThrows(WhatsAppLidMigrationException.SplitThreadMismatch.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("PN chat, no primary, no localLid, originalLidCache hit -> Migrate(cachedOriginalLid)")
    void migrateOnOriginalLidCache() {
        var h = build();
        h.service.registerOriginalLid(PEER_PN, PEER_LID);

        var chat = h.client.store().chatStore().addNewChat(PEER_PN);

        var resolution = h.service.resolveThread(chat);
        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
        assertEquals(PEER_LID.toUserJid(),
                ((LidMigrationResolution.Migrate) resolution).targetLid());
    }

    @Test
    @DisplayName("PN chat, no primary, no local, no cache, deletable (empty chat) -> Delete NO_LID_MAPPING")
    void deleteDeletable() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        // Empty chat -> deletable.

        var resolution = h.service.resolveThread(chat);
        assertInstanceOf(LidMigrationResolution.Delete.class, resolution);
        assertEquals(LidMigrationResolution.DeleteReason.NO_LID_MAPPING,
                ((LidMigrationResolution.Delete) resolution).reason());
    }

    @Test
    @DisplayName("PN chat, no primary, no local, no cache, non-deletable (locked) -> NoLidAvailable")
    void noLidAvailableThrows() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLocked(true);

        assertThrows(WhatsAppLidMigrationException.NoLidAvailable.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("PN chat with real message + no LID -> NoLidAvailable (data-bearing chat must not be deleted)")
    void noLidAvailableForDataBearingChat() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        var key = new MessageKeyBuilder()
                .id("real")
                .fromMe(false)
                .parentJid(PEER_PN)
                .build();
        chat.addMessage(new ChatMessageInfoBuilder()
                .key(key)
                .message(LinkedMessageContainer.of("hi"))
                .timestamp(Instant.now())
                .build());

        assertThrows(WhatsAppLidMigrationException.NoLidAvailable.class,
                () -> h.service.resolveThread(chat));
    }

    @Test
    @DisplayName("2-arg resolveThread: empty existingLidThreads + localLid -> Migrate (no collision check fires)")
    void twoArgNoCollision() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(PEER_LID);

        var resolution = h.service.resolveThread(chat, Set.of());
        assertInstanceOf(LidMigrationResolution.Migrate.class, resolution);
    }

    @Test
    @DisplayName("2-arg resolveThread: caller-supplied existingLidThreads triggers SplitThreadMismatch")
    void twoArgCollision() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        chat.setLid(PEER_LID);
        var existingLidThreads = Set.of(PEER_LID.toUserJid());

        assertThrows(WhatsAppLidMigrationException.SplitThreadMismatch.class,
                () -> h.service.resolveThread(chat, existingLidThreads));
    }

}
