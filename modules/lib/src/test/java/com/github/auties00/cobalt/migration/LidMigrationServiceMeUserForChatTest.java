package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.client.linked.TestWhatsAppClient;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadataBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import com.github.auties00.cobalt.wam.LiveWamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers {@link LidMigrationService#getMeUserLidOrJidForChat(com.github.auties00.cobalt.wire.linked.chat.Chat,
 * LidMigrationService.TranslateMsgKeyType)}: the helper that picks which form (LID or PN) of the
 * current user's JID appears as the participant of an outgoing
 * {@link com.github.auties00.cobalt.wire.core.message.MessageKey}. The cases walk the five-input
 * decision table (chat on LID server, chat is a group, chat is a Community Announcement Group,
 * group metadata reports {@code isLidAddressingMode}, and the chosen
 * {@link LidMigrationService.TranslateMsgKeyType}) branch by branch, and drive the missing-self-LID
 * and missing-self-PN failure paths by mutating the store directly.
 */
@DisplayName("LidMigrationService.getMeUserLidOrJidForChat")
class LidMigrationServiceMeUserForChatTest {

    private static final Jid SELF_PN = Jid.of("19254863482@s.whatsapp.net");
    private static final Jid SELF_LID = Jid.of("83116928594056@lid");
    private static final Jid SELF_PN_USER = SELF_PN.toUserJid();
    private static final Jid SELF_LID_USER = SELF_LID.toUserJid();
    private static final Jid PEER_PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid PEER_LID = Jid.of("258252122116273@lid");
    private static final Jid GROUP = Jid.of("120363012345678901@g.us");

    private record Harness(TestWhatsAppClient client, LiveLidMigrationService service) {}

    private static Harness build(Jid selfPn, Jid selfLid) {
        var props = TestABPropsService.builder().build();
        var store = MigrationFixtures.temporaryStore(selfPn, selfLid);
        var client = TestWhatsAppClient.create().withStore(store);
        var wamService = new LiveWamService(client, props);
        var service = new LiveLidMigrationService(client, props, wamService);
        return new Harness(client, service);
    }

    private static Harness build() {
        return build(SELF_PN, SELF_LID);
    }

    @Test
    @DisplayName("LID-server 1:1 chat -> me-LID for all translate types")
    void lidChatAllTypes() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_LID);

        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE));
    }

    @Test
    @DisplayName("1:1 PN chat (no metadata) -> me-PN for all translate types")
    void pnChatAllTypes() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(PEER_PN);

        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON));
        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE));
    }

    @Test
    @DisplayName("non-CAG group, isLidAddressingMode=true -> me-LID for all types")
    void groupLidModeAllTypes() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .defaultSubgroup(false)
                .build());

        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE));
    }

    @Test
    @DisplayName("non-CAG group, isLidAddressingMode=false -> me-PN for all types")
    void groupPnModeAllTypes() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .defaultSubgroup(false)
                .build());

        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON));
        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE));
    }

    @Test
    @DisplayName("CAG, isLidAddressingMode=true -> me-LID for all types")
    void cagLidModeAllTypes() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(true)
                .defaultSubgroup(true)
                .build());

        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE));
    }

    @Test
    @DisplayName("CAG, isLidAddressingMode=false: ADDON -> me-LID (isCAG short-circuit)")
    void cagPnModeAddonUsesLid() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .defaultSubgroup(true)
                .build());

        assertEquals(SELF_LID_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.ADDON),
                "isCAG triggers LID for ADDON even when isLidAddressingMode=false");
    }

    @Test
    @DisplayName("CAG, isLidAddressingMode=false: MESSAGE and EDIT_MESSAGE -> me-PN")
    void cagPnModeMessageAndEditUsePn() {
        var h = build();
        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .defaultSubgroup(true)
                .build());

        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE),
                "CAG without LID addressing routes MESSAGE through PN");
        assertEquals(SELF_PN_USER,
                h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.EDIT_MESSAGE),
                "CAG without LID addressing routes EDIT_MESSAGE through PN");
    }

    @Test
    @DisplayName("missing self-LID throws IllegalStateException on LID-route")
    void missingSelfLidThrows() {
        // No self-LID configured.
        var h = build(SELF_PN, null);
        var chat = h.client.store().chatStore().addNewChat(PEER_LID);

        assertThrows(IllegalStateException.class,
                () -> h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
    }

    @Test
    @DisplayName("missing self-PN throws IllegalStateException on PN-route (CAG MESSAGE without LID addressing)")
    void missingSelfPnThrows() {
        // Self-PN set initially, then cleared so the PN-route fails.
        var h = build();
        h.client.store().accountStore().setJid(null);

        var chat = h.client.store().chatStore().addNewChat(GROUP);
        h.client.store().chatStore().addChatMetadata(new GroupMetadataBuilder()
                .jid(GROUP)
                .subject("Test Group")
                .isLidAddressingMode(false)
                .defaultSubgroup(true) // CAG
                .build());

        // CAG + !isLidAddressingMode + MESSAGE -> me-PN path -> throws because store.accountStore().jid() is null.
        assertThrows(IllegalStateException.class,
                () -> h.service.getMeUserLidOrJidForChat(chat, LidMigrationService.TranslateMsgKeyType.MESSAGE));
    }
}
