package com.github.auties00.cobalt.migration;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the public surface of the sealed {@link LidMigrationResolution}
 * hierarchy so callers can pattern-match the {@code Migrate}, {@code Keep},
 * and {@code Delete} variants exhaustively, and pins the membership of the
 * {@code KeepReason} and {@code DeleteReason} enums.
 */
@DisplayName("LidMigrationResolution")
class LidMigrationResolutionTest {

    private static final Jid PN = Jid.of("393495089819@s.whatsapp.net");
    private static final Jid LID = Jid.of("258252122116273@lid");

    @Test
    @DisplayName("Migrate carries originalJid and targetLid")
    void migrateAccessors() {
        var resolution = new LidMigrationResolution.Migrate(PN, LID);
        assertEquals(PN, resolution.originalJid());
        assertEquals(LID, resolution.targetLid());
    }

    @Test
    @DisplayName("Keep carries originalJid and reason")
    void keepAccessors() {
        var resolution = new LidMigrationResolution.Keep(LID, LidMigrationResolution.KeepReason.ALREADY_LID);
        assertEquals(LID, resolution.originalJid());
        assertEquals(LidMigrationResolution.KeepReason.ALREADY_LID, resolution.reason());
    }

    @Test
    @DisplayName("Delete carries originalJid and reason")
    void deleteAccessors() {
        var resolution = new LidMigrationResolution.Delete(PN, LidMigrationResolution.DeleteReason.NO_LID_MAPPING);
        assertEquals(PN, resolution.originalJid());
        assertEquals(LidMigrationResolution.DeleteReason.NO_LID_MAPPING, resolution.reason());
    }

    @Test
    @DisplayName("KeepReason exposes every documented variant")
    void keepReasonVariants() {
        var reasons = LidMigrationResolution.KeepReason.values();
        assertEquals(7, reasons.length);
        assertTrue(Arrays.asList(reasons)
                .containsAll(List.of(
                        LidMigrationResolution.KeepReason.ALREADY_LID,
                        LidMigrationResolution.KeepReason.GROUP_OR_COMMUNITY,
                        LidMigrationResolution.KeepReason.NEWSLETTER,
                        LidMigrationResolution.KeepReason.BROADCAST,
                        LidMigrationResolution.KeepReason.STATUS_BROADCAST,
                        LidMigrationResolution.KeepReason.BOT,
                        LidMigrationResolution.KeepReason.DUPLICATE_WILL_MERGE
                )));
    }

    @Test
    @DisplayName("DeleteReason exposes every documented variant")
    void deleteReasonVariants() {
        var reasons = LidMigrationResolution.DeleteReason.values();
        assertEquals(3, reasons.length);
        assertTrue(Arrays.asList(reasons)
                .containsAll(List.of(
                        LidMigrationResolution.DeleteReason.NO_LID_MAPPING,
                        LidMigrationResolution.DeleteReason.CONTACT_NOT_MIGRATED,
                        LidMigrationResolution.DeleteReason.SPLIT_THREAD_MISMATCH
                )));
    }

    @Test
    @DisplayName("records implement equals/hashCode by value")
    void recordEquality() {
        var a = new LidMigrationResolution.Migrate(PN, LID);
        var b = new LidMigrationResolution.Migrate(PN, LID);
        var c = new LidMigrationResolution.Migrate(LID, PN);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);

        var keepA = new LidMigrationResolution.Keep(PN, LidMigrationResolution.KeepReason.ALREADY_LID);
        var keepB = new LidMigrationResolution.Keep(PN, LidMigrationResolution.KeepReason.ALREADY_LID);
        assertEquals(keepA, keepB);

        var delA = new LidMigrationResolution.Delete(PN, LidMigrationResolution.DeleteReason.NO_LID_MAPPING);
        var delB = new LidMigrationResolution.Delete(PN, LidMigrationResolution.DeleteReason.NO_LID_MAPPING);
        assertEquals(delA, delB);

        assertNotEquals(keepA, delA);
    }

    @Test
    @DisplayName("sealed permits exactly Migrate/Keep/Delete")
    void sealedPermits() {
        // Adding a fourth variant fails here, forcing a conscious update to the pattern-match call sites.
        var permitted = LidMigrationResolution.class.getPermittedSubclasses();
        assertEquals(3, permitted.length);
        var permittedSet = Set.of(permitted);
        assertTrue(permittedSet.contains(LidMigrationResolution.Migrate.class));
        assertTrue(permittedSet.contains(LidMigrationResolution.Keep.class));
        assertTrue(permittedSet.contains(LidMigrationResolution.Delete.class));
    }
}
