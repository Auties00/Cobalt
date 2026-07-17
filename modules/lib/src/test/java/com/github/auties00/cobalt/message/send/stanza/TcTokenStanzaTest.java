package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.message.MessageFixtures;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.privacy.LiveTrustedContactTokenService;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the gating predicates on {@link TcTokenStanza}: the constructor null guards and the two cheap negative paths
 * (the {@link ABProp#PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES} gate off, and the chat absent from the store). The
 * fixtures use an empty {@link MessageFixtures#temporaryStore(Jid, Jid)} so the recipient chat is always missing; the
 * positive emission path (seeded {@code tcToken} with a non-expired timestamp) is covered by the upstream send-pipeline
 * tests.
 */
@DisplayName("TcTokenStanza")
class TcTokenStanzaTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid CHAT = Jid.of("19254863482@s.whatsapp.net");

    @Test
    @DisplayName("constructor: null store / null abPropsService / null trustedContactTokenService all throw NullPointerException")
    void nullArgsThrow() {
        var ab = TestABPropsService.builder().build();
        var store = MessageFixtures.temporaryStore(SELF, null);
        var tokenService = new LiveTrustedContactTokenService(ab);
        assertThrows(NullPointerException.class, () -> new TcTokenStanza(null, ab, tokenService));
        assertThrows(NullPointerException.class, () -> new TcTokenStanza(store, null, tokenService));
        assertThrows(NullPointerException.class, () -> new TcTokenStanza(store, ab, null));
    }

    @Test
    @DisplayName("build: AB-prop disabled -> returns null (early gate)")
    void abPropDisabledReturnsNull() {
        var store = MessageFixtures.temporaryStore(SELF, null);
        var ab = TestABPropsService.builder()
                .with(ABProp.PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES, false)
                .build();
        var stanza = new TcTokenStanza(store, ab, new LiveTrustedContactTokenService(ab));
        assertNull(stanza.build(CHAT),
                "AB-prop off must suppress <tctoken> emission unconditionally");
    }

    @Test
    @DisplayName("build: AB-prop enabled but chat missing -> returns null")
    void chatMissingReturnsNull() {
        var store = MessageFixtures.temporaryStore(SELF, null);
        var ab = TestABPropsService.builder()
                .with(ABProp.PRIVACY_TOKEN_SENDING_ON_ALL_1_ON_1_MESSAGES, true)
                .build();
        var stanza = new TcTokenStanza(store, ab, new LiveTrustedContactTokenService(ab));
        assertNull(stanza.build(CHAT),
                "missing chat must produce no <tctoken>");
    }
}
