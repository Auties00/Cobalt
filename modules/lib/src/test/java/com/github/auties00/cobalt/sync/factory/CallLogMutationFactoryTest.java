package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers {@link CallLogMutationFactory} against the captured WhatsApp Web
 * encode oracle for {@code handler/call-log/encode}. The test passes a
 * {@code null} {@code CallLog} record so the oracle pins the minimal action
 * shape. The check is gated on {@link SyncFixtures#isOracleAvailable(String)}
 * so it no-ops cleanly until the fixture is present.
 */
@DisplayName("CallLogMutationFactory")
class CallLogMutationFactoryTest {
    private static final Jid PEER = Jid.of("1234567890@s.whatsapp.net");

    private static final String CALL_ID = "CALL_ID_42";

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/call-log/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/call-log/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = new CallLogMutationFactory().getCallLogMutation(
                Instant.ofEpochSecond(1_700_000_000L), PEER, CALL_ID, true, null);
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
