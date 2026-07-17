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
 * Covers {@link ContactActionMutationFactory} against the captured WhatsApp
 * Web encode oracle for {@code handler/contact/encode}. The test drives the
 * eight-arg overload with a pinned {@link Instant} so the oracle's timestamp
 * lines up with Cobalt's re-encoded bytes; the seven-arg overload would use
 * {@link Instant#now()} and break byte parity. The check is gated on
 * {@link SyncFixtures#isOracleAvailable(String)} so it no-ops cleanly until
 * the fixture is present.
 */
@DisplayName("ContactActionMutationFactory")
class ContactActionMutationFactoryTest {
    private static final Jid CONTACT_PN = Jid.of("33330000@s.whatsapp.net");

    private static final Jid CONTACT_LID = Jid.of("70000000000000@lid");

    private ContactActionMutationFactory factory;

    @BeforeEach
    void setUp() {
        factory = new ContactActionMutationFactory();
    }

    @Test
    @DisplayName("captured SyncActionValue bytes match Cobalt's encoded output when the fixture is present")
    void byteEqualityWithOracle() {
        if (!SyncFixtures.isOracleAvailable("handler/contact/encode")) return;
        var oracle = SyncFixtures.loadOracle("handler/contact/encode");
        var expected = SyncFixtures.decodeOracleBytes(oracle, "encoded");

        var pending = factory.getContactSyncMutation(
                CONTACT_PN, "Maria", "Maria Garcia", false, CONTACT_LID, true, "maria",
                Instant.ofEpochSecond(oracle.getLong("timestampSeconds")));
        var actual = SyncActionValueSpec.encode(pending.mutation().value().orElseThrow());

        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }
}
