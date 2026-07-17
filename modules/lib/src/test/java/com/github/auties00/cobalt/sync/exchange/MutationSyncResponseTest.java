package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReferenceBuilder;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the structural invariants of {@link MutationSyncResponse}: required vs nullable
 * fields, defensive defaults (a {@code null} patches list resolves to an empty list at the
 * accessor), the unmodifiable patches accessor, equals/hashCode field coverage, and the
 * toString format.
 *
 * <p>The tests build {@link MutationSyncResponse} instances directly via the public
 * constructors, so {@link MutationResponseParser} is not in scope here.
 */
@DisplayName("MutationSyncResponse")
class MutationSyncResponseTest {

    @Nested
    @DisplayName("required vs nullable fields")
    class Construction {
        @Test
        @DisplayName("collection name must not be null")
        void collectionNameRequired() {
            assertThrows(NullPointerException.class,
                    () -> new MutationSyncResponse(null, 0L, false, List.of(), null));
        }

        @Test
        @DisplayName("snapshot reference defaults to empty Optional")
        void snapshotReferenceOptional() {
            var response = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null);
            assertTrue(response.snapshotReference().isEmpty());
            assertFalse(response.isSnapshot());
        }

        @Test
        @DisplayName("collectionError defaults to empty Optional (single-arg constructor)")
        void collectionErrorOptional() {
            var response = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null);
            assertTrue(response.collectionError().isEmpty());
        }

        @Test
        @DisplayName("explicit collectionError surfaces through Optional accessor")
        void explicitCollectionError() {
            var error = new WhatsAppWebAppStateSyncException.Conflict(false);
            var response = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null, error);
            assertTrue(response.collectionError().isPresent());
            assertEquals(error, response.collectionError().orElseThrow());
        }

        @Test
        @DisplayName("patches list defaults to empty when null is passed")
        void nullPatchesYieldsEmpty() {
            var response = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, null, null);
            assertTrue(response.patches().isEmpty(),
                    "constructor null patches -> accessor returns empty list");
        }

        @Test
        @DisplayName("isSnapshot returns true iff snapshotReference was supplied")
        void isSnapshotReflectsReference() {
            var blob = new ExternalBlobReferenceBuilder().build();
            var snapshot = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), blob);
            var patches  = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null);
            assertTrue(snapshot.isSnapshot());
            assertFalse(patches.isSnapshot());
        }
    }

    @Nested
    @DisplayName("patches accessor - unmodifiable")
    class PatchesAccessor {
        @Test
        @DisplayName("patches() returns an unmodifiable sequenced collection")
        void patchesIsUnmodifiable() {
            var response = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null);
            assertThrows(UnsupportedOperationException.class,
                    () -> response.patches().add(null));
        }
    }

    @Nested
    @DisplayName("equality and hashCode")
    class Equality {
        @Test
        @DisplayName("two responses with the same fields are equal")
        void equalsByField() {
            var a = new MutationSyncResponse(SyncPatchType.REGULAR, 7L, true, List.of(), null);
            var b = new MutationSyncResponse(SyncPatchType.REGULAR, 7L, true, List.of(), null);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }

        @Test
        @DisplayName("differing version makes responses unequal")
        void inequalityByVersion() {
            var a = new MutationSyncResponse(SyncPatchType.REGULAR, 7L, true, List.of(), null);
            var b = new MutationSyncResponse(SyncPatchType.REGULAR, 8L, true, List.of(), null);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("differing collection name makes responses unequal")
        void inequalityByCollection() {
            var a = new MutationSyncResponse(SyncPatchType.REGULAR, 7L, true, List.of(), null);
            var b = new MutationSyncResponse(SyncPatchType.REGULAR_LOW, 7L, true, List.of(), null);
            assertNotEquals(a, b);
        }

        @Test
        @DisplayName("differing hasMore makes responses unequal")
        void inequalityByHasMore() {
            var a = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, false, List.of(), null);
            var b = new MutationSyncResponse(SyncPatchType.REGULAR, 0L, true, List.of(), null);
            assertNotEquals(a, b);
        }
    }

    @Nested
    @DisplayName("toString - diagnostic format")
    class ToStringFormat {
        @Test
        @DisplayName("toString lists every field with its label")
        void toStringListsEveryField() {
            var response = new MutationSyncResponse(SyncPatchType.REGULAR_LOW, 99L, true, List.of(), null);
            var s = response.toString();
            assertTrue(s.contains("collectionName=" + SyncPatchType.REGULAR_LOW), "must include name");
            assertTrue(s.contains("version=99"), "must include version");
            assertTrue(s.contains("hasMore=true"), "must include hasMore");
        }
    }
}
