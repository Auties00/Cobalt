package com.github.auties00.cobalt.sync.factory;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueSpec;
import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers the outgoing-mutation wire shape of {@link NoteEditMutationFactory} by
 * re-encoding a {@link SyncActionValueSpec} and comparing it byte-for-byte against
 * the captured WhatsApp Web oracle. The deterministic-timestamp overload
 * {@link NoteEditMutationFactory#getNoteEditMutation(String, Jid, String, boolean, Instant)}
 * is used so the oracle's pinned {@code timestampSeconds} reproduces across builds;
 * the overload that stamps {@link Instant#now()} cannot be pinned without a clock
 * seam and is not exercised. Each test returns early when its oracle fixture is absent.
 */
@DisplayName("NoteEditMutationFactory")
class NoteEditMutationFactoryTest {
    private static final Jid CHAT_JID = Jid.of("12345@s.whatsapp.net");

    private NoteEditMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new NoteEditMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/note-edit/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/note-edit/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getNoteEditMutation(
                "note-oracle", CHAT_JID, "body", false,
                Instant.ofEpochSecond(oracle.getLong("timestampSeconds")));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
