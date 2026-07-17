package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Smoke tests for {@link MigrationFixtures} that exercise only the helpers needing no fixture corpus on
 * disk. The migration-fixture directory may be empty in a fresh checkout (the live-oracle captures are
 * committed independently), so these tests pin {@link MigrationFixtures#isAvailable(String)} discrimination
 * and {@link MigrationFixtures#temporaryStore(Jid, Jid)} round-tripping, the corpus-less subset of the helper.
 */
class MigrationFixturesTest {

    @Test
    void isAvailableReturnsFalseForMissingTopic() {
        assertFalse(MigrationFixtures.isAvailable("does-not-exist"));
    }

    @Test
    void temporaryStoreReflectsCallerJids() {
        var pn = Jid.of("19254863482@s.whatsapp.net");
        var lid = Jid.of("83116928594056@lid");
        var store = MigrationFixtures.temporaryStore(pn, lid);
        assertEquals(pn, store.accountStore().jid().orElseThrow());
        assertEquals(lid, store.accountStore().lid().orElseThrow());
    }

    @Test
    void temporaryStoreAllowsNullLid() {
        var pn = Jid.of("19254863482@s.whatsapp.net");
        var store = MigrationFixtures.temporaryStore(pn, null);
        assertEquals(pn, store.accountStore().jid().orElseThrow());
        assertFalse(store.accountStore().lid().isPresent());
    }
}
