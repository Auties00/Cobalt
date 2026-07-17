package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.wire.linked.sync.mutation.MutationConflictResolutionState;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValueBuilder;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.sync.crypto.DecryptedMutation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers the factory and record-equality contract of {@link ConflictResolution}: the
 * {@link ConflictResolution#of(MutationConflictResolutionState)} and
 * {@link ConflictResolution#merged(DecryptedMutation.Trusted)} factories plus the auto-generated
 * record equality and {@code toString}. The merged-mutation fixture is built directly through the
 * model builder with deterministic timestamps; only the equality identity matters here, not the
 * wire shape.
 */
@DisplayName("ConflictResolution")
class ConflictResolutionTest {

    private static DecryptedMutation.Trusted sampleTrusted() {
        var value = new SyncActionValueBuilder()
                .timestamp(Instant.ofEpochSecond(1700000000L))
                .build();
        return new DecryptedMutation.Trusted("idx", value, SyncdOperation.SET,
                Instant.ofEpochSecond(1700000000L), 3);
    }

    @Nested
    @DisplayName("factory of(state) -- non-merged resolutions")
    class StateOnly {
        @Test
        @DisplayName("of(state) carries the state and a null merged mutation")
        void stateOnly() {
            var resolution = ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL);
            assertEquals(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL, resolution.state());
            assertNull(resolution.mergedMutation(),
                    "of(state) never carries a merged mutation");
        }

        @Test
        @DisplayName("every MutationConflictResolutionState variant is constructible via of(state)")
        void everyVariantConstructible() {
            for (var variant : MutationConflictResolutionState.values()) {
                var r = ConflictResolution.of(variant);
                assertEquals(variant, r.state(), "variant=" + variant);
                assertNull(r.mergedMutation(), "variant=" + variant);
            }
        }
    }

    @Nested
    @DisplayName("factory merged(mutation) -- message-range handler merge")
    class MergedMutation {
        @Test
        @DisplayName("merged() forces SKIP_REMOTE_DROP_LOCAL state")
        void mergedForcesState() {
            var merged = sampleTrusted();
            var resolution = ConflictResolution.merged(merged);
            assertEquals(MutationConflictResolutionState.SKIP_REMOTE_DROP_LOCAL, resolution.state(),
                    "merged() always sets the SKIP_REMOTE_DROP_LOCAL state per WAWebSyncActionStore.doConflictResolution");
        }

        @Test
        @DisplayName("merged() preserves the supplied mutation by reference")
        void mergedPreservesReference() {
            var merged = sampleTrusted();
            var resolution = ConflictResolution.merged(merged);
            assertSame(merged, resolution.mergedMutation(),
                    "the merged mutation must be stored as-is; the handler is the source of truth");
        }
    }

    @Nested
    @DisplayName("record equality and toString")
    class RecordContract {
        @Test
        @DisplayName("two resolutions with the same state and null merged are equal")
        void equalsStateOnly() {
            assertEquals(
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE),
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE));
            assertEquals(
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE).hashCode(),
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE).hashCode());
        }

        @Test
        @DisplayName("resolutions with different state are not equal")
        void differentStateUnequal() {
            assertNotEquals(
                    ConflictResolution.of(MutationConflictResolutionState.SKIP_REMOTE),
                    ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL));
        }

        @Test
        @DisplayName("two merged resolutions with the same Trusted are equal")
        void equalsMerged() {
            var merged = sampleTrusted();
            assertEquals(ConflictResolution.merged(merged), ConflictResolution.merged(merged));
        }

        @Test
        @DisplayName("toString includes the state name")
        void toStringIncludesState() {
            var s = ConflictResolution.of(MutationConflictResolutionState.APPLY_REMOTE_DROP_LOCAL).toString();
            assertNotNull(s);
            Assertions.assertTrue(s.contains("APPLY_REMOTE_DROP_LOCAL"),
                    "toString should surface the state name for diagnostic logging");
        }
    }
}
