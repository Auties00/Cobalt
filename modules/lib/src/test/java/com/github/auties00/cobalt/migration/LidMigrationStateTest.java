package com.github.auties00.cobalt.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link LidMigrationState}: the {@link LidMigrationState#isTerminal()} truth table over every
 * value and the protobuf wire indices, pinned explicitly rather than via {@link Enum#ordinal()} so the
 * persisted form cannot drift. Indices 1 to 5 round-trip with WA Web's {@code LidThreadMigrationStatus}
 * enum from {@code WAWebLid1X1ThreadAccountMigrations.flow}; indices 0, 6, 7 are the Cobalt-only
 * {@code NOT_STARTED} / {@code FAILED} / {@code DISABLED} sinks.
 */
@DisplayName("LidMigrationState")
class LidMigrationStateTest {

    @Test
    @DisplayName("isTerminal truth table")
    void isTerminalTruthTable() {
        assertFalse(LidMigrationState.NOT_STARTED.isTerminal());
        assertFalse(LidMigrationState.WAITING_PROP.isTerminal());
        assertFalse(LidMigrationState.WAITING_MAPPINGS.isTerminal());
        assertFalse(LidMigrationState.READY.isTerminal());
        assertFalse(LidMigrationState.IN_PROGRESS.isTerminal());
        assertTrue(LidMigrationState.COMPLETE.isTerminal());
        assertTrue(LidMigrationState.FAILED.isTerminal());
        assertTrue(LidMigrationState.DISABLED.isTerminal());
    }

    @Test
    @DisplayName("protobuf indices are stable and contiguous 0..7")
    void protobufIndicesAreStable() {
        assertEquals(0, LidMigrationState.NOT_STARTED.index);
        assertEquals(1, LidMigrationState.WAITING_PROP.index);
        assertEquals(2, LidMigrationState.WAITING_MAPPINGS.index);
        assertEquals(3, LidMigrationState.READY.index);
        assertEquals(4, LidMigrationState.IN_PROGRESS.index);
        assertEquals(5, LidMigrationState.COMPLETE.index);
        assertEquals(6, LidMigrationState.FAILED.index);
        assertEquals(7, LidMigrationState.DISABLED.index);
    }

    @Test
    @DisplayName("ordinal matches index for every value")
    void ordinalMatchesIndex() {
        // Catches a future reorder that leaves ordinals stable but silently drifts index vs ordinal.
        for (var state : LidMigrationState.values()) {
            assertEquals(state.ordinal(), state.index,
                    "ordinal must equal index for " + state);
        }
    }

    @Test
    @DisplayName("exactly 8 values declared")
    void valueCount() {
        assertEquals(8, LidMigrationState.values().length);
    }
}
