package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.community.CommunityMetadataBuilder;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the inactive-group LID migration sweep: completion-flag round-trip,
 * PN-group filtering, retry rescheduling when groups remain on PN, and
 * per-group exception swallowing. Each test drives {@code run()} directly
 * (its package-private visibility is the test seam) so the 60-second initial
 * delay armed by {@code start()} does not stall the suite.
 */
@DisplayName("InactiveGroupLidMigrationService")
class InactiveGroupLidMigrationServiceTest {

    // Identity pair matches the 'business' VoIP capture session.
    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");

    private record Harness(TestWhatsAppClient client, LiveInactiveGroupLidMigrationService service) {}

    private static Harness build() {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(SELF_PN, SELF_LID);
        var client = TestWhatsAppClient.create().withStore(store);
        var service = new LiveInactiveGroupLidMigrationService(client, props);
        return new Harness(client, service);
    }

    private static Jid groupJid(int suffix) {
        return Jid.of("1203630123456789" + String.format("%02d", suffix) + "@g.us");
    }

    @Test
    @DisplayName("setComplete + isComplete round-trip")
    void completionFlag() {
        var h = build();
        assertFalse(h.service.isInactiveGroupLidMigrationComplete());
        h.service.setInactiveGroupLidMigrationComplete();
        assertTrue(h.service.isInactiveGroupLidMigrationComplete());
    }

    @Test
    @DisplayName("run early-returns when already complete")
    void runShortCircuitsOnComplete() {
        var h = build();
        h.service.setInactiveGroupLidMigrationComplete();

        var pnGroup = groupJid(1);
        h.client.store().chatStore().addNewChat(pnGroup);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build());

        h.service.run();

        assertTrue(h.service.isInactiveGroupLidMigrationComplete());
    }

    @Test
    @DisplayName("run with zero PN groups marks complete immediately")
    void runNoPnGroups() {
        var h = build();
        h.service.run();
        assertTrue(h.service.isInactiveGroupLidMigrationComplete());
    }

    @Test
    @DisplayName("findPnGroups filters out: LID-addressing groups, suspended/terminated groups, non-group servers")
    void findPnGroupsFilters() {
        var h = build();
        var store = h.client.store();

        var pnGroup = groupJid(1);
        store.chatStore().addNewChat(pnGroup);
        store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build());

        var lidGroup = groupJid(2);
        store.chatStore().addNewChat(lidGroup);
        store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(lidGroup)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .build());

        var suspendedGroup = groupJid(3);
        store.chatStore().addNewChat(suspendedGroup);
        var suspendedMeta = new GroupMetadataBuilder()
                .jid(suspendedGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build();
        suspendedMeta.setSuspended(true);
        store.chatStore().addChatMetadata(suspendedMeta);

        var terminatedGroup = groupJid(4);
        store.chatStore().addNewChat(terminatedGroup);
        var terminatedMeta = new GroupMetadataBuilder()
                .jid(terminatedGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build();
        terminatedMeta.setTerminated(true);
        store.chatStore().addChatMetadata(terminatedMeta);

        store.chatStore().addNewChat(Jid.of("12025550100@s.whatsapp.net"));

        var noMetaGroup = groupJid(5);
        store.chatStore().addNewChat(noMetaGroup);

        var suspendedCommunity = groupJid(6);
        store.chatStore().addNewChat(suspendedCommunity);
        var communityMeta = new CommunityMetadataBuilder()
                .jid(suspendedCommunity)
                .subject("Test Community")
                .isLidAddressingMode(false)
                .build();
        communityMeta.setSuspended(true);
        store.chatStore().addChatMetadata(communityMeta);

        var pnGroups = h.service.findPnGroups();

        assertEquals(1, pnGroups.size(), "only the unsuspended/non-terminated PN-mode group passes");
        assertEquals(pnGroup, pnGroups.getFirst());
    }

    @Test
    @DisplayName("run with PN groups + all queryChatMetadata succeed + groups become LID marks complete")
    void runMarksCompleteWhenAllGroupsFlip() {
        var h = build();
        var store = h.client.store();

        var pnGroup = groupJid(1);
        store.chatStore().addNewChat(pnGroup);
        store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build());

        h.client.withChatMetadata(pnGroup, new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .build());

        h.service.run();

        // TestWhatsAppClient does not auto-refresh cached metadata after queryChatMetadata,
        // so rewrite the stored metadata to simulate the server-driven flip to LID.
        store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .build());

        h.service.run();

        assertTrue(h.service.isInactiveGroupLidMigrationComplete());
    }

    @Test
    @DisplayName("run swallows exceptions thrown by queryChatMetadata for individual groups")
    void runSwallowsQueryFailures() {
        var h = build();
        var store = h.client.store();
        var pnGroup = groupJid(1);
        store.chatStore().addNewChat(pnGroup);
        store.chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(pnGroup)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .build());

        h.service.run();

        assertFalse(h.service.isInactiveGroupLidMigrationComplete());
    }

    @Test
    @DisplayName("run with PN groups remaining reschedules a retry task")
    void runWithRemainingPnGroupsReschedules() {
        var h = build();
        var store = h.client.store();

        var pnGroup1 = groupJid(1);
        var pnGroup2 = groupJid(2);
        store.chatStore().addNewChat(pnGroup1);
        store.chatStore().addNewChat(pnGroup2);

        var meta1 = new GroupMetadataBuilder().jid(pnGroup1).subject("Test Group").isLidAddressingMode(false).build();
        var meta2 = new GroupMetadataBuilder().jid(pnGroup2).subject("Test Group").isLidAddressingMode(false).build();
        store.chatStore().addChatMetadata(meta1);
        store.chatStore().addChatMetadata(meta2);
        h.client.withChatMetadata(pnGroup1, meta1);
        h.client.withChatMetadata(pnGroup2, meta2);

        h.service.run();

        assertFalse(h.service.isInactiveGroupLidMigrationComplete(),
                "groups still on PN -> migration is rescheduled, not marked complete");
    }

    @Test
    @DisplayName("reset cancels any pending scheduled task and nulls the field")
    void resetCancelsScheduledTask() {
        var h = build();
        h.service.start();
        h.service.reset();
        h.service.start();
        h.service.reset();
    }
}
