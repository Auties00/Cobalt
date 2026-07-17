package com.github.auties00.cobalt.message.send.stanza;

import com.github.auties00.cobalt.wire.linked.chat.ChatMessageInfoBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer;
import com.github.auties00.cobalt.wire.core.message.MessageKeyBuilder;
import com.github.auties00.cobalt.props.TestABPropsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the gating predicates on {@link ReportingStanza}: the constructor null guard and the two cheap suppression
 * paths (no {@code messageSecret}, and a key with no id). The default {@link TestABPropsService} reports a sender
 * reporting-token version of zero, so the build call short-circuits before any HMAC derivation; the positive
 * derivation path is covered by the upstream reporting-token integration tests.
 */
@DisplayName("ReportingStanza")
class ReportingStanzaTest {

    private static final Jid SELF = Jid.of("12025550100@s.whatsapp.net");

    private static final Jid REMOTE = Jid.of("19254863482@s.whatsapp.net");

    private static final byte[] SECRET = new byte[32];

    @Test
    @DisplayName("constructor: null abPropsService throws NullPointerException")
    void nullAbPropsThrows() {
        assertThrows(NullPointerException.class, () -> new ReportingStanza(null));
    }

    @Test
    @DisplayName("returns null when message has no messageSecret")
    void nullWhenNoSecret() {
        var ab = TestABPropsService.builder().build();
        var stanza = new ReportingStanza(ab);
        var msg = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder().id("3EB0").parentJid(REMOTE).fromMe(true).build())
                .message(LinkedMessageContainer.of("body"))
                .build();
        assertNull(stanza.build(msg, SELF, REMOTE),
                "missing messageSecret must suppress the <reporting> emission");
    }

    @Test
    @DisplayName("returns null when message has no id")
    void nullWhenNoId() {
        var ab = TestABPropsService.builder().build();
        var stanza = new ReportingStanza(ab);
        var msg = new ChatMessageInfoBuilder()
                .key(new MessageKeyBuilder().parentJid(REMOTE).fromMe(true).build())
                .message(LinkedMessageContainer.of("body"))
                .messageSecret(SECRET)
                .build();
        assertNotNull(stanza);
        assertNull(stanza.build(msg, SELF, REMOTE));
    }
}
