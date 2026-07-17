package com.github.auties00.cobalt.device;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests for {@link DeviceFixtures}, covering the entry points downstream device tests rely
 * on: oracle loading round-trips the JSON-stringified {@code result.value} payload,
 * {@link DeviceFixtures#isAvailable} discriminates committed-corpus topics from uncaptured ones,
 * and {@link DeviceFixtures#temporaryStore} sets the caller-supplied self JID and LID. Uses the
 * committed {@code self-identity} and {@code phash-samples} fixtures as canonical test data.
 */
class DeviceFixturesTest {

    @Test
    void selfIdentityOracleParses() {
        var oracle = DeviceFixtures.loadOracle("self-identity");
        assertEquals("19254863482@c.us", oracle.getString("mePn"));
        assertEquals("83116928594056@lid", oracle.getString("meLid"));
        assertEquals("19254863482:1@c.us", oracle.getString("meDevicePn"));
        assertEquals("83116928594056:1@lid", oracle.getString("meDeviceLid"));
        assertEquals(1, oracle.getIntValue("meDeviceId"));
    }

    // Pins the four-sample shape downstream KAT tests depend on; fails if the corpus is regenerated with a different sample count
    @Test
    void phashOracleHasFourSamples() {
        var oracle = DeviceFixtures.loadOracle("phash-samples");
        assertEquals(4, oracle.size());
        for (var key : oracle.keySet()) {
            var sample = oracle.getJSONObject(key);
            assertNotNull(sample.getJSONArray("devices"));
            assertTrue(sample.getString("phashV1").startsWith("1:"));
            assertTrue(sample.getString("phashV2").startsWith("2:"));
        }
    }

    @Test
    void isAvailableDiscriminates() {
        // self-identity commits only the .expected.json oracle, no JSONL dump, so it is not "available"
        assertFalse(DeviceFixtures.isAvailable("self-identity"));
        assertFalse(DeviceFixtures.isAvailable("does-not-exist"));
    }

    @Test
    void temporaryStoreReflectsCallerJids() {
        var pn = Jid.of("19254863482@s.whatsapp.net");
        var lid = Jid.of("83116928594056@lid");
        var store = DeviceFixtures.temporaryStore(pn, lid);
        assertEquals(pn, store.accountStore().jid().orElseThrow());
        assertEquals(lid, store.accountStore().lid().orElseThrow());
    }
}
