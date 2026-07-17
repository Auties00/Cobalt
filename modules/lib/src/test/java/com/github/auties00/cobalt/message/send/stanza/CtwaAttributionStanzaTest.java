package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the constructor null guards and the no-campaign default branch on
 * {@link CtwaAttributionStanza}: the builder rejects null services up front,
 * and without a recorded {@link ExternalEntryPoint} the build call collapses
 * to {@code null}. The fixtures use an empty
 * {@link MessageFixtures#temporaryStore(Jid, Jid)} and a default
 * {@link TestABPropsService}, leaving the entry-point cache empty; the
 * positive emission path with a stored entry point is exercised by the
 * upstream send-pipeline tests.
 */
@DisplayName("CtwaAttributionStanza")
class CtwaAttributionStanzaTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid CHAT = Jid.of("19254863482@s.whatsapp.net");

    @Test
    @DisplayName("constructor: null store throws NullPointerException")
    void nullStoreThrows() {
        var ab = TestABPropsService.builder().build();
        assertThrows(NullPointerException.class, () -> new CtwaAttributionStanza(null, ab));
    }

    @Test
    @DisplayName("constructor: null abPropsService throws NullPointerException")
    void nullAbPropsThrows() {
        var store = MessageFixtures.temporaryStore(SELF, null);
        assertThrows(NullPointerException.class, () -> new CtwaAttributionStanza(store, null));
    }

    @Test
    @DisplayName("build: returns null when the chat has no CTWA campaign")
    void noCampaignReturnsNull() {
        var store = MessageFixtures.temporaryStore(SELF, null);
        var ab = TestABPropsService.builder().build();
        var stanza = new CtwaAttributionStanza(store, ab);
        assertNull(stanza.build(CHAT),
                "chat with no campaign info must not emit <ctwa_attribution>");
    }
}
