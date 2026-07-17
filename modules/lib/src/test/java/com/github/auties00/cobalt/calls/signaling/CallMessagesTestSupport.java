package com.github.auties00.cobalt.calls.signaling;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

/**
 * Test-only helper that stamps the universal {@code call-id}/{@code call-creator} header onto a
 * {@link StanzaBuilder}, mirroring the package-private production {@code CallMessages.stampHeader} so a
 * test can hand-build a raw action stanza without the typed record.
 */
final class CallMessagesTestSupport {
    private CallMessagesTestSupport() {
    }

    static StanzaBuilder stamp(String description, String callId, Jid callCreator) {
        return new StanzaBuilder()
                .description(description)
                .attribute("call-id", callId)
                .attribute("call-creator", callCreator);
    }
}
