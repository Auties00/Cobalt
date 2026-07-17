package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayloadBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@link LidMigrationService} cross-addressing-mode helpers ({@code toPn},
 * {@code toLid}, {@code toUserLid}, {@code toUserLidOrThrow}, {@code toPnOrThrow},
 * {@code lookupLid}, {@code toAddressingModeFactory}, {@code toCommonAddressingMode},
 * {@code getPnAndLidToUpdate}, {@code chatIsLid}, {@code shouldUseLidAddressing},
 * {@code shouldHaveAccountLid}, and {@code isRegularUser}), pinning both the cache-before-store
 * lookup ordering and the me-fast-path that resolves the local user's own JID without a registered
 * mapping. Each case runs against an isolated store so registered mappings cannot bleed across
 * cases.
 */
@DisplayName("LiveLidMigrationService JID conversion helpers")
class LidMigrationServiceJidConversionTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid PEER_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid PEER_LID = Jid.of("258252122116273@lid");
    private static final Jid PEER_DEVICE = Jid.of("393495089819:5@s.whatsapp.net");
    private static final Jid GROUP = Jid.of("120363012345678901@g.us");
    private static final Jid NEWSLETTER = Jid.of("120363023456789012@newsletter");
    private static final Jid BROADCAST = Jid.of("19254863482@broadcast");
    private static final Jid BOT = Jid.of("867051314767696@bot");
    private static final Jid HOSTED = Jid.of("18005550199@hosted");

    private record Harness(TestWhatsAppClient client, LiveLidMigrationService service) {}

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, service);
    }

    @Test
    @DisplayName("toPn returns null for null input")
    void toPnNull() {
        assertNull(build().service.toPn(null));
    }

    @Test
    @DisplayName("toPn returns the input unchanged when it is already a phone-number JID")
    void toPnPassthroughPhoneNumber() {
        var h = build();
        assertEquals(PEER_PN, h.service.toPn(PEER_PN));
    }

    @Test
    @DisplayName("toPn returns null for a LID with no mapping")
    void toPnLidUnmapped() {
        var h = build();
        assertNull(h.service.toPn(PEER_LID));
    }

    @Test
    @DisplayName("toPn looks up the LID through the store")
    void toPnLidMapped() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertEquals(PEER_PN, h.service.toPn(PEER_LID));
    }

    @Test
    @DisplayName("toLid returns null for null input")
    void toLidNull() {
        assertNull(build().service.toLid(null));
    }

    @Test
    @DisplayName("toLid returns the input unchanged when it is already a LID")
    void toLidPassthroughLid() {
        var h = build();
        assertEquals(PEER_LID, h.service.toLid(PEER_LID));
    }

    @Test
    @DisplayName("toLid returns null for a phone-number JID with no mapping")
    void toLidPhoneUnmapped() {
        var h = build();
        assertNull(h.service.toLid(PEER_PN));
    }

    @Test
    @DisplayName("toLid strips device suffix before looking up")
    void toLidStripsDevice() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertEquals(PEER_LID, h.service.toLid(PEER_DEVICE));
    }

    @Test
    @DisplayName("toUserLid returns null for null input")
    void toUserLidNull() {
        assertNull(build().service.toUserLid(null));
    }

    @Test
    @DisplayName("toUserLid strips device suffix before checking")
    void toUserLidStripsDevice() {
        var h = build();
        var lidWithDevice = Jid.of("258252122116273:3@lid");
        // Already-LID branch: strip device, return user-level LID.
        assertEquals(PEER_LID, h.service.toUserLid(lidWithDevice));
    }

    @Test
    @DisplayName("toUserLidOrThrow throws when no mapping exists")
    void toUserLidOrThrow() {
        var h = build();
        assertThrows(IllegalStateException.class, () -> h.service.toUserLidOrThrow(PEER_PN));
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertEquals(PEER_LID, h.service.toUserLidOrThrow(PEER_PN));
    }

    @Test
    @DisplayName("toPnOrThrow throws when no mapping exists")
    void toPnOrThrow() {
        var h = build();
        assertThrows(IllegalStateException.class, () -> h.service.toPnOrThrow(PEER_LID));
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertEquals(PEER_PN, h.service.toPnOrThrow(PEER_LID));
    }

    @Test
    @DisplayName("lookupLid returns empty for null")
    void lookupLidNull() {
        var h = build();
        assertFalse(h.service.lookupLid(null).isPresent());
    }

    @Test
    @DisplayName("lookupLid falls back to store when primary cache is empty")
    void lookupLidStoreFallback() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertEquals(PEER_LID, h.service.lookupLid(PEER_PN).orElseThrow());
    }

    @Test
    @DisplayName("lookupLid: primary cache hit short-circuits store lookup")
    void lookupLidPrimaryCacheShortCircuits() {
        var h = build();
        // Populate the PRIMARY cache via changeLid (which also writes the store, so we
        // overwrite the store's entry afterwards to prove the cache wins).
        h.service.changeLid(PEER_PN, PEER_LID, null);
        var differentLid = Jid.of("12345678901234@lid");
        h.client.store().contactStore().registerLidMapping(PEER_PN, differentLid);

        assertEquals(PEER_LID, h.service.lookupLid(PEER_PN).orElseThrow(),
                "primary cache (PEER_LID) wins over store (differentLid)");
    }

    @Test
    @DisplayName("getAlternateUserWid (via toCommonAddressingMode): me LID -> me PN via store.accountStore().jid()/store.accountStore().lid() fast path")
    void getAlternateUserWidMeFastPathLidToPn() {
        var h = build();
        // No registered mapping for self; only store.accountStore().jid() and store.accountStore().lid() are set.
        // Pair (selfLid, peerPn) is "mixed addressing" so toCommonAddressingMode converts.
        var result = h.service.toCommonAddressingMode(SELF_LID, PEER_PN);
        // selfLid's alternate (via me fast path) is the self PN at user level.
        assertEquals(SELF_PN.toUserJid(), result[0],
                "me LID -> me PN through the fast path that consults store.accountStore().jid()");
        assertEquals(PEER_PN, result[1]);
    }

    @Test
    @DisplayName("getAlternateUserWid (via toCommonAddressingMode): me PN -> me LID via store.accountStore().lid() fast path")
    void getAlternateUserWidMeFastPathPnToLid() {
        var h = build();
        var result = h.service.toCommonAddressingMode(SELF_PN, PEER_LID);
        assertEquals(SELF_LID.toUserJid(), result[0],
                "me PN -> me LID through the fast path that consults store.accountStore().lid()");
        assertEquals(PEER_LID, result[1]);
    }

    @Test
    @DisplayName("shouldUseLidAddressing: state=COMPLETE + 1:1 PN recipient with mapping -> true")
    void shouldUseLidAddressingPositiveCase() {
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        // Register a mapping so lookupLid finds something.
        store.contactStore().registerLidMapping(PEER_PN, PEER_LID);

        service.initialize();
        service.enableMigration();
        service.processProtocolMessage(
                new LIDMigrationMappingSyncPayloadBuilder()
                        .pnToLidMappings(List.of())
                        .build());

        assertEquals(LidMigrationState.COMPLETE, service.state());
        assertTrue(service.shouldUseLidAddressing(PEER_PN),
                "COMPLETE + mapped 1:1 PN recipient -> LID addressing");
    }

    @Test
    @DisplayName("isRegularUser is true for the standard user/lid/hosted/hostedLid servers")
    void isRegularUserTrue() {
        assertTrue(LidMigrationService.isRegularUser(PEER_PN));
        assertTrue(LidMigrationService.isRegularUser(PEER_LID));
        assertTrue(LidMigrationService.isRegularUser(HOSTED));
    }

    @Test
    @DisplayName("isRegularUser is false for group/newsletter/broadcast")
    void isRegularUserNonUserServers() {
        assertFalse(LidMigrationService.isRegularUser(GROUP));
        assertFalse(LidMigrationService.isRegularUser(NEWSLETTER));
        assertFalse(LidMigrationService.isRegularUser(BROADCAST));
    }

    @Test
    @DisplayName("isRegularUser is false for bot and for the announcements account")
    void isRegularUserExcludesBotAndAnnouncements() {
        assertFalse(LidMigrationService.isRegularUser(BOT));
        assertFalse(LidMigrationService.isRegularUser(Jid.announcementsAccount()));
    }

    @Test
    @DisplayName("shouldHaveAccountLid is false when migration is not COMPLETE")
    void shouldHaveAccountLidPreMigration() {
        var h = build();
        assertFalse(h.service.shouldHaveAccountLid(PEER_PN));
    }

    @Test
    @DisplayName("shouldHaveAccountLid is false for null JID")
    void shouldHaveAccountLidNull() {
        var h = build();
        assertFalse(h.service.shouldHaveAccountLid(null));
    }

    @Test
    @DisplayName("shouldUseLidAddressing is false for null")
    void shouldUseLidAddressingNull() {
        assertFalse(build().service.shouldUseLidAddressing(null));
    }

    @Test
    @DisplayName("shouldUseLidAddressing is true when the recipient is already a LID")
    void shouldUseLidAddressingAlreadyLid() {
        assertTrue(build().service.shouldUseLidAddressing(PEER_LID));
    }

    @Test
    @DisplayName("shouldUseLidAddressing is false for group, newsletter, and broadcast servers")
    void shouldUseLidAddressingNonUserServers() {
        var h = build();
        assertFalse(h.service.shouldUseLidAddressing(GROUP));
        assertFalse(h.service.shouldUseLidAddressing(NEWSLETTER));
        assertFalse(h.service.shouldUseLidAddressing(BROADCAST));
    }

    @Test
    @DisplayName("shouldUseLidAddressing is false for a 1:1 PN recipient when migration has not advanced")
    void shouldUseLidAddressingNotMigrated() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        assertFalse(h.service.shouldUseLidAddressing(PEER_PN),
                "state machine is NOT_STARTED; LID addressing is gated on COMPLETE/IN_PROGRESS");
    }

    @Test
    @DisplayName("toAddressingModeFactory(true) returns toLid as a function reference")
    void toAddressingModeFactoryTrue() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var fn = h.service.toAddressingModeFactory(true);
        assertEquals(PEER_LID, fn.apply(PEER_PN));
    }

    @Test
    @DisplayName("toAddressingModeFactory(false) returns toPn as a function reference")
    void toAddressingModeFactoryFalse() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var fn = h.service.toAddressingModeFactory(false);
        assertEquals(PEER_PN, fn.apply(PEER_LID));
    }

    @Test
    @DisplayName("toCommonAddressingMode leaves a same-server pair unchanged")
    void toCommonAddressingModeSameServer() {
        var h = build();
        var pn2 = Jid.of("12025550100@s.whatsapp.net");
        var result = h.service.toCommonAddressingMode(PEER_PN, pn2);
        assertArrayEquals(new Jid[]{PEER_PN, pn2}, result);
    }

    @Test
    @DisplayName("toCommonAddressingMode converts the first side when its alternate is known")
    void toCommonAddressingModeConvertsFirst() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var other = Jid.of("999999999999999@lid");
        // first=PN (mapping known), second=LID (no mapping); first is converted to LID.
        var result = h.service.toCommonAddressingMode(PEER_PN, other);
        assertEquals(PEER_LID, result[0]);
        assertEquals(other, result[1]);
    }

    @Test
    @DisplayName("toCommonAddressingMode converts the second side when only its alternate is known")
    void toCommonAddressingModeConvertsSecond() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var unknownPn = Jid.of("12025550100@s.whatsapp.net");
        // first=unknownPn (no LID), second=PEER_LID (PN known); second is converted to PN.
        var result = h.service.toCommonAddressingMode(unknownPn, PEER_LID);
        assertEquals(unknownPn, result[0]);
        assertEquals(PEER_PN, result[1]);
    }

    @Test
    @DisplayName("toCommonAddressingMode passes through nulls")
    void toCommonAddressingModeNulls() {
        var h = build();
        var result = h.service.toCommonAddressingMode(null, PEER_PN);
        assertNull(result[0]);
        assertEquals(PEER_PN, result[1]);
    }

    @Test
    @DisplayName("getPnAndLidToUpdate returns empty list for null")
    void getPnAndLidToUpdateNull() {
        assertTrue(build().service.getPnAndLidToUpdate(null).isEmpty());
    }

    @Test
    @DisplayName("getPnAndLidToUpdate returns [LID, PN] when the LID has a mapping")
    void getPnAndLidToUpdateFromLid() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var pair = h.service.getPnAndLidToUpdate(PEER_LID);
        assertEquals(2, pair.size());
        assertEquals(PEER_LID, pair.get(0));
        assertEquals(PEER_PN, pair.get(1));
    }

    @Test
    @DisplayName("getPnAndLidToUpdate returns [PN, LID] when the PN has a mapping")
    void getPnAndLidToUpdateFromPn() {
        var h = build();
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        var pair = h.service.getPnAndLidToUpdate(PEER_PN);
        assertEquals(2, pair.size());
        assertEquals(PEER_PN, pair.get(0));
        assertEquals(PEER_LID, pair.get(1));
    }

    @Test
    @DisplayName("getPnAndLidToUpdate returns single-element list when no alternate is known")
    void getPnAndLidToUpdateNoAlternate() {
        var h = build();
        var pair = h.service.getPnAndLidToUpdate(PEER_PN);
        assertEquals(1, pair.size());
        assertEquals(PEER_PN, pair.getFirst());
    }

    @Test
    @DisplayName("chatIsLid is false for null chat")
    void chatIsLidNull() {
        assertFalse(build().service.chatIsLid(null));
    }

    @Test
    @DisplayName("chatIsLid is true for a chat on the LID server")
    void chatIsLidLidServer() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_LID);
        assertTrue(h.service.chatIsLid(chat));
    }

    @Test
    @DisplayName("chatIsLid is true for a group whose metadata reports isLidAddressingMode=true")
    void chatIsLidGroupOnLidMode() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        var metadata = new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .build();
        h.client.store().chatStore().addChatMetadata(metadata);
        assertTrue(h.service.chatIsLid(chat));
    }

    @Test
    @DisplayName("chatIsLid is false for a group whose metadata reports isLidAddressingMode=false")
    void chatIsLidGroupNotOnLidMode() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        var metadata = new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build();
        h.client.store().chatStore().addChatMetadata(metadata);
        assertFalse(h.service.chatIsLid(chat));
    }

    @Test
    @DisplayName("chatIsLid is false for a 1:1 PN chat regardless of mapping")
    void chatIsLidPnChat() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);
        h.client.store().contactStore().registerLidMapping(PEER_PN, PEER_LID);
        // The chat JID is PN; chatIsLid only flips for LID-server chats or LID-addressing groups.
        assertFalse(h.service.chatIsLid(chat));
    }
}
