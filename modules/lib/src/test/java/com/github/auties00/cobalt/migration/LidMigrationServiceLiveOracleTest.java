package com.github.auties00.cobalt.migration;

import com.alibaba.fastjson2.JSON;
import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMapping;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingBuilder;
import com.github.auties00.cobalt.wire.linked.jid.migration.LIDMigrationMappingSyncPayloadBuilder;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live-oracle parity suite for {@link LidMigrationService}. Each case loads a
 * {@code .expected.json} oracle captured from WA Web's live runtime and asserts that Cobalt's
 * equivalent helper returns the same value for the same input, pinning
 * {@link LidMigrationService#isRegularUser(Jid)},
 * {@link LidMigrationService#shouldHaveAccountLid(Jid)},
 * {@link LidMigrationService#toCommonAddressingMode(Jid, Jid)},
 * {@link LidMigrationService#chatIsLid}, {@link LidMigrationService#getAlternateMsgKey}, and the
 * mapping-sync payload shape against the captured output.
 *
 * <p>Fixtures live under {@code src/test/resources/fixtures/migration/}, captured from a logged-in
 * WA Web session via the MCP {@code web_live_debug_eval_to_file} tool. Captured JID strings carry
 * WA Web's {@code @c.us} user server, which is normalised to Cobalt's {@code @s.whatsapp.net}
 * before comparison. The {@code toCommonAddressingMode} and {@code getAlternateMsgKey} cases build
 * their service with the capture session's self identity (PN {@code 393495089819}, LID
 * {@code 258252122116273}) so the me-fast-path matches the oracle.
 */
@DisplayName("LiveLidMigrationService live-oracle parity")
class LidMigrationServiceLiveOracleTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");

    private static LiveLidMigrationService buildService() {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        return new LiveLidMigrationService(client, props, wamService);
    }

    @Test
    @DisplayName("WAWebWid.isRegularUser parity: Cobalt matches WA Web for every server family")
    void isRegularUserParity() {
        // The captured oracle's inner result.value is a JSON array, not an object, so loadOracle
        // (which assumes object) cannot be used here; parse the array directly.
        var raw = MigrationFixtures.loadExpected("is-regular-user")
                .getJSONObject("result")
                .getString("value");
        var entries = JSON.parseArray(raw);

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.getJSONObject(i);
            // Skip entries whose JID could not be constructed in WA Web.
            if (entry.containsKey("error")) {
                continue;
            }
            var label = entry.getString("label");
            var jidStr = entry.getString("jid");
            var expected = entry.getBooleanValue("isRegularUser");

            // WA Web uses @c.us for the user server; Cobalt uses @s.whatsapp.net. Normalise.
            var jid = Jid.of(jidStr.replace("@c.us", "@s.whatsapp.net"));
            var actual = LidMigrationService.isRegularUser(jid);
            assertEquals(expected, actual,
                    label + " (" + jidStr + "): WA Web returned " + expected + ", Cobalt returned " + actual);
        }
    }

    @Test
    @DisplayName("LidThreadMigrationStatus enum: WA Web indices 1-5 match Cobalt LidMigrationState.index")
    void migrationStatusEnumParity() {
        var oracle = MigrationFixtures.loadExpected("migration-status-enum");
        var values = oracle.getJSONObject("values");

        assertEquals(values.getIntValue("WAITING_PROP"), LidMigrationState.WAITING_PROP.index);
        assertEquals(values.getIntValue("WAITING_MAPPINGS"), LidMigrationState.WAITING_MAPPINGS.index);
        assertEquals(values.getIntValue("READY"), LidMigrationState.READY.index);
        assertEquals(values.getIntValue("IN_PROGRESS"), LidMigrationState.IN_PROGRESS.index);
        assertEquals(values.getIntValue("COMPLETE"), LidMigrationState.COMPLETE.index);
    }

    @Test
    @DisplayName("shouldHaveAccountLid parity for every server family (only the migrated-true branch)")
    void shouldHaveAccountLidParity() {
        var oracle = MigrationFixtures.loadOracle("should-have-account-lid");
        var liveMigrated = oracle.getBooleanValue("isLidMigrated");

        // Cobalt's shouldHaveAccountLid gates on isLidMigrated() (state == COMPLETE).
        // The captured oracle pinned isLidMigrated=true on the WA Web side; mirror that on Cobalt.
        var service = buildService();
        if (liveMigrated) {
            advanceToComplete(service);
        }

        var cases = oracle.getJSONArray("cases");
        for (var i = 0; i < cases.size(); i++) {
            var entry = cases.getJSONObject(i);
            if (entry.containsKey("error")) {
                continue;
            }
            var label = entry.getString("label");
            var jidStr = entry.getString("jid");
            var expected = entry.getBooleanValue("shouldHaveAccountLid");

            var jid = Jid.of(jidStr.replace("@c.us", "@s.whatsapp.net"));
            var actual = service.shouldHaveAccountLid(jid);
            assertEquals(expected, actual,
                    label + " (" + jidStr + "): WA Web returned " + expected + ", Cobalt returned " + actual);
        }
    }

    @Test
    @DisplayName("toCommonAddressingMode parity: WA Web's pair-conversion rules match Cobalt for me-fast-path and same-server")
    void toCommonAddressingModeParity() {
        // Capture session is "personal" whose self is 393495089819@c.us / 258252122116273@lid.
        // To replicate WA Web's me-fast-path against the captured oracle we build a service whose
        // self identity matches the capture session.
        var props = TestABPropsService.builder().build();
        var personalSelfPn = Jid.of("393495089819@s.whatsapp.net");
        var personalSelfLid = Jid.of("258252122116273@lid");
        var store = MigrationFixtures.temporaryStore(personalSelfPn, personalSelfLid);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        var oracle = MigrationFixtures.loadOracle("to-common-addressing-mode");
        var pairs = oracle.getJSONArray("pairs");
        for (var i = 0; i < pairs.size(); i++) {
            var entry = pairs.getJSONObject(i);
            if (entry.containsKey("error")) continue;

            var label = entry.getString("label");
            var inA = Jid.of(entry.getString("inA").replace("@c.us", "@s.whatsapp.net"));
            var inB = Jid.of(entry.getString("inB").replace("@c.us", "@s.whatsapp.net"));
            var expectedA = Jid.of(entry.getString("outA").replace("@c.us", "@s.whatsapp.net"));
            var expectedB = Jid.of(entry.getString("outB").replace("@c.us", "@s.whatsapp.net"));

            var result = service.toCommonAddressingMode(inA, inB);
            assertEquals(expectedA, result[0], label + " outA");
            assertEquals(expectedB, result[1], label + " outB");
        }
    }

    @Test
    @DisplayName("chatIsLid parity: WA Web's id+groupMetadata rule matches Cobalt's chat-driven implementation")
    void chatIsLidParity() {
        var raw = MigrationFixtures.loadExpected("chat-is-lid")
                .getJSONObject("result")
                .getString("value");
        var entries = JSON.parseArray(raw);

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.getJSONObject(i);
            var label = entry.getString("label");
            var jidStr = entry.getString("jid");
            var expected = entry.getBooleanValue("chatIsLid");

            // Fresh service + store per case so metadata from one case does not pollute the next.
            var props = TestABPropsService.builder().build();
            var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
            var client = TestWhatsAppClient.create().withStore(store);
            var wamService = new LiveWamService(client, props);
            var service = new LiveLidMigrationService(client, props, wamService);

            var jid = Jid.of(jidStr.replace("@c.us", "@s.whatsapp.net"));
            var chat = store.chatStore().addNewChat(jid);

            // Install the same groupMetadata WA Web's oracle was given (where applicable).
            if (label.equals("group_lid_mode")) {
                store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                        .jid(jid).subject("Test Group").isLidAddressingMode(true).build());
            } else if (label.equals("group_pn_mode")) {
                store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                        .jid(jid).subject("Test Group").isLidAddressingMode(false).build());
            }

            var actual = service.chatIsLid(chat);
            assertEquals(expected, actual, label + " (" + jidStr + ")");
        }
    }

    @Test
    @DisplayName("getAlternateMsgKey parity (1:1): swaps remote PN<->LID via WA Web's getAlternateUserWid")
    void getAlternateMsgKey1on1Parity() {
        // Same self identity as the capture session ('personal').
        var props = TestABPropsService.builder().build();
        var personalSelfPn = Jid.of("393495089819@s.whatsapp.net");
        var personalSelfLid = Jid.of("258252122116273@lid");
        var store = MigrationFixtures.temporaryStore(personalSelfPn, personalSelfLid);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        var raw = MigrationFixtures.loadExpected("get-alternate-msg-key-1on1")
                .getJSONObject("result")
                .getString("value");
        var entries = JSON.parseArray(raw);

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.getJSONObject(i);
            var label = entry.getString("label");
            var inRemote = Jid.of(entry.getString("inRemote").replace("@c.us", "@s.whatsapp.net"));
            var expectedRemote = entry.getString("outRemote");
            var expectedId = entry.getString("outId");

            var key = new MessageKeyBuilder()
                    .fromMe(entry.getBooleanValue("outFromMe"))
                    .id("ABC".equals(expectedId) || "XYZ".equals(expectedId) || "P1".equals(expectedId)
                            ? expectedId : "TEST")
                    .parentJid(inRemote)
                    .build();
            var alt = service.getAlternateMsgKey(key);

            if (expectedRemote == null) {
                assertNull(alt, label + ": WA Web returned null alternate; Cobalt must too");
            } else {
                assertTrue(alt != null, label + ": WA Web returned a non-null alternate");
                var expectedRemoteJid = Jid.of(expectedRemote.replace("@c.us", "@s.whatsapp.net"));
                assertEquals(expectedRemoteJid, alt.parentJid().orElseThrow(), label + " remote");
            }
        }
    }

    @Test
    @DisplayName("getAlternateMsgKey parity (group): swaps participant PN<->LID via WA Web's getAlternateUserWid")
    void getAlternateMsgKeyGroupParity() {
        // Use the personal-session self identity that the oracle was captured with.
        var props = TestABPropsService.builder().build();
        var personalSelfPn = Jid.of("393495089819@s.whatsapp.net");
        var personalSelfLid = Jid.of("258252122116273@lid");
        var store = MigrationFixtures.temporaryStore(personalSelfPn, personalSelfLid);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        var raw = MigrationFixtures.loadExpected("get-alternate-msg-key-group")
                .getJSONObject("result")
                .getString("value");
        var entries = JSON.parseArray(raw);
        var groupRemote = Jid.of("120363012345678901@g.us");

        for (var i = 0; i < entries.size(); i++) {
            var entry = entries.getJSONObject(i);
            var label = entry.getString("label");
            var inParticipantStr = entry.getString("inParticipant");
            var expectedParticipantStr = entry.getString("outParticipant");

            var keyBuilder = new MessageKeyBuilder()
                    .fromMe(true)
                    .id("G")
                    .parentJid(groupRemote);
            if (inParticipantStr != null) {
                keyBuilder.senderJid(Jid.of(inParticipantStr.replace("@c.us", "@s.whatsapp.net")));
            }
            var alt = service.getAlternateMsgKey(keyBuilder.build());

            if (expectedParticipantStr == null) {
                assertNull(alt, label + ": WA Web returned null; Cobalt must too");
            } else {
                assertTrue(alt != null, label + ": WA Web returned a non-null alternate");
                var expectedParticipant = Jid.of(expectedParticipantStr.replace("@c.us", "@s.whatsapp.net"));
                assertEquals(expectedParticipant, alt.senderJid().orElseThrow(), label + " participant");
                assertEquals(groupRemote, alt.parentJid().orElseThrow(), label + " remote (unchanged for group)");
            }
        }
    }

    @Test
    @DisplayName("mapping-sync-empty parity: Cobalt's empty-payload path produces no cache entries (mirrors WA Web's empty mappings list)")
    void mappingSyncEmptyParity() {
        var oracle = MigrationFixtures.loadOracle("mapping-sync-empty");
        var mappings = oracle.getJSONArray("mappings");
        var primaryTs = oracle.get("primaryMigrationTsSec");

        assertEquals(0, mappings.size(),
                "WA Web's parser returns empty mappings when the payload has none");
        assertNull(primaryTs,
                "WA Web's parser returns null primaryMigrationTsSec on empty mappings");

        // Cobalt mirror: processProtocolMessage with empty payload leaves the lookupLid cache empty
        // and advances state through to COMPLETE.
        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        service.initialize();
        service.enableMigration();
        service.processProtocolMessage(
                new LIDMigrationMappingSyncPayloadBuilder()
                        .pnToLidMappings(List.of())
                        .build());

        assertEquals(LidMigrationState.COMPLETE, service.state());
        // No cache entries -> any random JID lookup returns empty.
        assertTrue(service.lookupLid(Jid.of("393495089819@s.whatsapp.net")).isEmpty());
    }

    @Test
    @DisplayName("mapping-sync-typical parity: Cobalt's caches end up with the same pn->assignedLid map WA Web's parser produces")
    void mappingSyncTypicalParity() {
        var oracle = MigrationFixtures.loadOracle("mapping-sync-typical");
        var mappings = oracle.getJSONArray("mappings");
        var primaryTs = oracle.getLongValue("primaryMigrationTsSec");

        var props = TestABPropsService.builder()
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_PEER_SYNC_TIMEOUT_IN_SECONDS, 0L)
                .with(ABProp.LID_ONE_ON_ONE_MIGRATION_COMPATIBLE, true)
                .build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);

        // Build a Cobalt LIDMigrationMappingSyncPayload from the oracle's input numbers (the same
        // input the parser was given inline in the eval script).
        var cobaltMappings = new ArrayList<LIDMigrationMapping>();
        cobaltMappings.add(new LIDMigrationMappingBuilder()
                .pn(393495089819L)
                .assignedLid(258252122116273L)
                .latestLid(999999999999999L)
                .build());
        cobaltMappings.add(new LIDMigrationMappingBuilder()
                .pn(12025550100L)
                .assignedLid(555555555555555L)
                .build());

        service.initialize();
        service.enableMigration();
        service.processProtocolMessage(
                new LIDMigrationMappingSyncPayloadBuilder()
                        .pnToLidMappings(cobaltMappings)
                        .chatDbMigrationTimestamp(primaryTs)
                        .build());

        // Each oracle mapping's pnUser -> assignedLid must resolve via Cobalt's lookupLid.
        for (var i = 0; i < mappings.size(); i++) {
            var m = mappings.getJSONObject(i);
            var pnUser = Jid.of(m.getString("pnUser").replace("@c.us", "@s.whatsapp.net"));
            var expectedAssigned = Jid.of(m.getString("assignedLid"));
            assertEquals(expectedAssigned, service.lookupLid(pnUser).orElseThrow(),
                    "Cobalt lookupLid(" + pnUser + ") must match WA Web parser's assignedLid");
        }
    }

    private static void advanceToComplete(LiveLidMigrationService service) {
        service.initialize();
        service.enableMigration();
        var payload = new LIDMigrationMappingSyncPayloadBuilder()
                .pnToLidMappings(List.of())
                .build();
        service.processProtocolMessage(payload);
    }
}
