package com.github.auties00.cobalt.message.receive.crypto;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link SenderKeyNameFactory#create(Jid, Jid)}: the resulting
 * {@code (groupId, senderAddress)} key tracks the group JID and the sender's user and
 * device portions, and distinct groups or distinct sender devices yield distinct keys.
 * The cases are pure-function checks on synthetic JIDs, so no fixtures or live state are
 * needed.
 */
@DisplayName("SenderKeyNameFactory")
class SenderKeyNameFactoryTest {

    private static final Jid GROUP = Jid.of("120363023250764418@g.us");
    private static final Jid SENDER_PRIMARY = Jid.of("12025550100@s.whatsapp.net");
    private static final Jid SENDER_COMPANION = Jid.of("12025550100:73@s.whatsapp.net");

    @Test
    @DisplayName("create(group, sender): groupId is the group JID string form")
    void groupIdIsGroupJidString() {
        var name = SenderKeyNameFactory.create(GROUP, SENDER_PRIMARY);
        assertNotNull(name);
        assertEquals(GROUP.toString(), name.groupId());
    }

    @Test
    @DisplayName("create(group, sender): sender address has user + device 0 for primary device JID")
    void primaryDeviceAddress() {
        var name = SenderKeyNameFactory.create(GROUP, SENDER_PRIMARY);
        assertEquals(SENDER_PRIMARY.user(), name.sender().name());
        assertEquals(SENDER_PRIMARY.device(), name.sender().id(),
                "primary device JIDs encode device id 0");
    }

    @Test
    @DisplayName("create(group, sender): companion device id propagates")
    void companionDeviceAddress() {
        var name = SenderKeyNameFactory.create(GROUP, SENDER_COMPANION);
        assertEquals("12025550100", name.sender().name());
        assertEquals(73, name.sender().id(),
                "companion device id (73) must propagate into the sender address");
    }

    @Test
    @DisplayName("different group then different sender-key name")
    void groupDistinguishes() {
        var first = SenderKeyNameFactory.create(GROUP, SENDER_PRIMARY);
        var second = SenderKeyNameFactory.create(Jid.of("120363099999999999@g.us"), SENDER_PRIMARY);
        Assertions.assertNotEquals(first.groupId(), second.groupId(),
                "different group JID then different groupId on the SenderKeyName");
    }

    @Test
    @DisplayName("different sender device then different sender-key name")
    void senderDeviceDistinguishes() {
        var primary = SenderKeyNameFactory.create(GROUP, SENDER_PRIMARY);
        var companion = SenderKeyNameFactory.create(GROUP, SENDER_COMPANION);
        Assertions.assertNotEquals(
                primary.sender().id(), companion.sender().id(),
                "device id must distinguish sender addresses for the same user");
    }
}
