package com.github.auties00.cobalt.store;

import com.github.auties00.cobalt.device.DeviceFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppContactStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the self-send PN/LID round-trip through {@link LinkedWhatsAppContactStore#findLidByPhone(Jid)}
 * and {@link LinkedWhatsAppContactStore#findPhoneByLid(Jid)}: a message addressed to the local user's
 * own phone-number JID must resolve to a LID and that LID must map back to the same PN.
 *
 * <p>The expectations come from a captured live oracle
 * ({@code fixtures/device/self-chat-routing.expected.json}) recorded from WA Web's
 * {@code findOrCreateLatestChat(<own PN>)}, which keys the self-chat under a LID.
 */
class SelfPnLidRoutingTest {

    @Test
    void selfMappingRoundTripMatchesLiveOracle() {
        var oracle = DeviceFixtures.loadOracle("self-chat-routing");
        var pnInput = Jid.of(oracle.getString("pnInput").replace("@c.us", "@s.whatsapp.net"));
        var expectedLidBare = Jid.of(oracle.getString("chatId"));
        var expectedAccountLid = Jid.of(oracle.getString("accountLid"));

        var store = DeviceFixtures.temporaryStore(pnInput, expectedLidBare);

        var lookedUpLid = store.contactStore().findLidByPhone(pnInput).orElseThrow();
        assertEquals(expectedLidBare.toUserJid(), lookedUpLid.toUserJid(),
                "findLidByPhone(<own PN>) must return the LID WA Web resolves on the same input");

        var lookedUpPn = store.contactStore().findPhoneByLid(expectedLidBare).orElseThrow();
        assertEquals(pnInput.toUserJid(), lookedUpPn.toUserJid(),
                "findPhoneByLid(<own LID>) must round-trip back to the same PN; "
                        + "before the symmetric self-special-case fix, this returned empty");

        assertEquals(expectedLidBare, expectedAccountLid,
                "captured oracle invariant: chat.id == chat.accountLid for self-chat");
    }
}
