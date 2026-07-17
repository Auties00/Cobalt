package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the gating predicates on {@link CsTokenStanza}: the constructor
 * null guards and the empty-store default-disabled branch, where with no NCT
 * salt and the AB prop off {@link CsTokenStanza#build} must not emit a
 * {@code <cstoken>} child. The fixtures use an empty
 * {@link MessageFixtures#temporaryStore(Jid, Jid)} carrying neither a salt
 * nor a chat record for the recipient, so both negative paths collapse the
 * build to {@code null}; the positive HMAC-derivation path needs a seeded
 * salt and an account LID and is covered by the upstream send-pipeline tests.
 */
@DisplayName("CsTokenStanza")
class CsTokenStanzaTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid CHAT = Jid.of("19254863482@s.whatsapp.net");

    @Test
    @DisplayName("constructor: null store / null abPropsService both throw NullPointerException")
    void nullArgsThrow() {
        var ab = TestABPropsService.builder().build();
        var store = MessageFixtures.temporaryStore(SELF, null);
        assertThrows(NullPointerException.class, () -> new CsTokenStanza(null, ab));
        assertThrows(NullPointerException.class, () -> new CsTokenStanza(store, null));
    }

    @Test
    @DisplayName("build: chat with no consent state -> returns null")
    void noConsentStateReturnsNull() {
        var store = MessageFixtures.temporaryStore(SELF, null);
        var ab = TestABPropsService.builder().build();
        var stanza = new CsTokenStanza(store, ab);
        assertNull(stanza.build(CHAT),
                "chat with no consent state must not emit <cstoken>");
    }
}
