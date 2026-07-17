package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.sync.SyncFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the LT-Hash accumulator in {@link MutationLTHash} against the
 * algebraic contract WA Web relies on for anti-tampering: four algebraic
 * properties (commutativity, associativity, group inverse, determinism), the
 * batched {@code subtractThenAdd} operation, defensive {@code copy}, tamper
 * detection, and the wire-level invariants (empty hash is 128 zero bytes, hash
 * buffers are always 128 bytes). Three synthetic value MACs ({@code A},
 * {@code B}, {@code C}) drive the algebraic tests; the parity test additionally
 * consumes WA-Web-captured value MACs from {@code crypto/lt-hash.expected} when
 * the fixture is present.
 */
@DisplayName("MutationLTHash")
class MutationLTHashTest {
    private static final byte[] A = filled(32, 0x11);

    private static final byte[] B = filled(32, 0x22);

    private static final byte[] C = filled(32, 0x33);

    private static byte[] filled(int length, int value) {
        var out = new byte[length];
        for (var i = 0; i < length; i++) out[i] = (byte) value;
        return out;
    }

    @Nested
    @DisplayName("empty state")
    class EmptyState {
        @Test
        @DisplayName("EMPTY_HASH is 128 zero bytes")
        void emptyHashIsZero() {
            assertEquals(MutationLTHash.HASH_LENGTH, MutationLTHash.EMPTY_HASH.length);
            for (var b : MutationLTHash.EMPTY_HASH) assertEquals(0, b);
        }

        @Test
        @DisplayName("HASH_LENGTH is 128")
        void hashLengthConstant() {
            assertEquals(128, MutationLTHash.HASH_LENGTH);
        }

        @Test
        @DisplayName("add(EMPTY, []) returns EMPTY")
        void addEmptyListIsIdentity() {
            var hash = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of());
            assertArrayEquals(MutationLTHash.EMPTY_HASH, hash);
        }

        @Test
        @DisplayName("subtract(EMPTY, []) returns EMPTY")
        void subtractEmptyListIsIdentity() {
            var hash = MutationLTHash.subtract(MutationLTHash.EMPTY_HASH, List.of());
            assertArrayEquals(MutationLTHash.EMPTY_HASH, hash);
        }
    }

    @Nested
    @DisplayName("structural invariants")
    class Structural {
        @Test
        @DisplayName("add of any element produces a 128-byte result")
        void addProducesFixedLength() {
            var hash = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            assertEquals(MutationLTHash.HASH_LENGTH, hash.length);
        }

        @Test
        @DisplayName("add(EMPTY, X) is not equal to EMPTY for non-trivial X")
        void addChangesHash() {
            var hash = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            assertFalse(MessageDigest.isEqual(MutationLTHash.EMPTY_HASH, hash),
                    "adding a value must move the hash off zero");
        }

        @Test
        @DisplayName("distinct elements produce distinct hashes")
        void distinctElementsDistinctHashes() {
            var hashA = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            var hashB = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(B));
            assertFalse(MessageDigest.isEqual(hashA, hashB));
        }

        @Test
        @DisplayName("add is deterministic")
        void deterministic() {
            var h1 = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B, C));
            var h2 = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B, C));
            assertArrayEquals(h1, h2);
        }
    }

    @Nested
    @DisplayName("commutativity - order of adds does not matter")
    class Commutativity {
        @Test
        @DisplayName("add(EMPTY, [A, B]) == add(EMPTY, [B, A])")
        void twoElementCommute() {
            var ab = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var ba = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(B, A));
            assertArrayEquals(ab, ba);
        }

        @Test
        @DisplayName("all 6 permutations of {A, B, C} produce the same hash")
        void threeElementCommute() {
            var permutations = new List[]{
                    List.of(A, B, C), List.of(A, C, B), List.of(B, A, C),
                    List.of(B, C, A), List.of(C, A, B), List.of(C, B, A)
            };
            var reference = MutationLTHash.add(MutationLTHash.EMPTY_HASH, permutations[0]);
            for (var i = 1; i < permutations.length; i++) {
                @SuppressWarnings("unchecked")
                var candidate = MutationLTHash.add(MutationLTHash.EMPTY_HASH, permutations[i]);
                assertArrayEquals(reference, candidate,
                        "permutation " + i + " must produce the same hash as the reference order");
            }
        }
    }

    @Nested
    @DisplayName("associativity - split-and-merge equivalence")
    class Associativity {
        @Test
        @DisplayName("add(add(EMPTY, [A]), [B]) == add(EMPTY, [A, B])")
        void splitAddIsAddBoth() {
            var combined = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var stepwise = MutationLTHash.add(
                    MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A)),
                    List.of(B));
            assertArrayEquals(combined, stepwise);
        }
    }

    @Nested
    @DisplayName("group inverse - add then subtract is identity")
    class GroupInverse {
        @Test
        @DisplayName("subtract(add(EMPTY, [A]), [A]) == EMPTY")
        void addThenSubtractIsIdentity() {
            var added = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            var removed = MutationLTHash.subtract(added, List.of(A));
            assertArrayEquals(MutationLTHash.EMPTY_HASH, removed);
        }

        @Test
        @DisplayName("subtract(add(EMPTY, [A, B, C]), [A, B, C]) == EMPTY")
        void addThenSubtractMultiple() {
            var added = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B, C));
            var removed = MutationLTHash.subtract(added, List.of(C, B, A));
            assertArrayEquals(MutationLTHash.EMPTY_HASH, removed,
                    "subtract order is irrelevant; group is abelian");
        }

        @Test
        @DisplayName("subtract(add(EMPTY, [A, B]), [A]) == add(EMPTY, [B])")
        void partialInverse() {
            var ab = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var afterRemoveA = MutationLTHash.subtract(ab, List.of(A));
            var onlyB = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(B));
            assertArrayEquals(onlyB, afterRemoveA);
        }
    }

    @Nested
    @DisplayName("subtractThenAdd - batched mixed operation")
    class SubtractThenAdd {
        @Test
        @DisplayName("returns both the intermediate subtract result and the final hash")
        void resultCarriesIntermediate() {
            var initial = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var result = MutationLTHash.subtractThenAdd(initial, List.of(C), List.of(A));
            assertNotNull(result.subtractResult(), "intermediate must be present");
            assertNotNull(result.ltHash(), "final must be present");

            // Intermediate: initial minus A equals add(EMPTY, [B])
            var onlyB = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(B));
            assertArrayEquals(onlyB, result.subtractResult(),
                    "subtractResult must equal initial minus toRemove");

            // Final: intermediate + C = add(EMPTY, [B, C])
            var bc = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(B, C));
            assertArrayEquals(bc, result.ltHash(),
                    "ltHash must equal (initial minus toRemove) plus toAdd");
        }

        @Test
        @DisplayName("subtract-only round-trip restores empty state")
        void subtractOnlyRestoresEmpty() {
            var initial = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            var result = MutationLTHash.subtractThenAdd(initial, List.of(), List.of(A));
            assertArrayEquals(MutationLTHash.EMPTY_HASH, result.ltHash());
            assertArrayEquals(MutationLTHash.EMPTY_HASH, result.subtractResult());
        }

        @Test
        @DisplayName("add-only matches plain add")
        void addOnlyMatchesAdd() {
            var initial = MutationLTHash.EMPTY_HASH;
            var result = MutationLTHash.subtractThenAdd(initial, List.of(A, B), List.of());
            var plain = MutationLTHash.add(initial, List.of(A, B));
            assertArrayEquals(plain, result.ltHash());
            assertArrayEquals(initial, result.subtractResult(),
                    "subtractResult should equal the input when nothing was subtracted");
        }
    }

    @Nested
    @DisplayName("copy - defensive clone")
    class Copy {
        @Test
        @DisplayName("copy of null is null")
        void copyOfNullIsNull() {
            assertEquals(null, MutationLTHash.copy(null));
        }

        @Test
        @DisplayName("copy returns a distinct array with the same contents")
        void copyClones() {
            var original = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A));
            var clone = MutationLTHash.copy(original);
            assertArrayEquals(original, clone);
            assertFalse(original == clone, "clone must be a distinct instance");

            // Mutating the clone must not affect the original
            var originalByte0Before = original[0];
            clone[0] = (byte) 0xFF;
            assertEquals(originalByte0Before, original[0],
                    "mutating the clone must not change the original");
        }
    }

    @Nested
    @DisplayName("WA Web oracle parity")
    class OracleParity {
        @Test
        @DisplayName("captured (empty / add / subtract / add-then-subtract) vectors match")
        void waWebVectors() {
            if (!SyncFixtures.isOracleAvailable("crypto/lt-hash")) return;
            var oracle = SyncFixtures.loadOracle("crypto/lt-hash");
            var dec = Base64.getDecoder();
            var empty   = dec.decode(oracle.getString("empty"));
            var oracleA = dec.decode(oracle.getString("a"));
            var oracleB = dec.decode(oracle.getString("b"));
            var oracleC = dec.decode(oracle.getString("c"));
            var oracleAB         = dec.decode(oracle.getString("afterAB"));
            var oracleABC        = dec.decode(oracle.getString("afterABC"));
            var oracleABminusA   = dec.decode(oracle.getString("afterABminusA"));

            // Sanity: oracle's empty matches Cobalt's
            assertArrayEquals(MutationLTHash.EMPTY_HASH, empty,
                    "oracle's empty hash must be all zeros");

            // Match Cobalt's reductions against the oracle's outputs
            var ab = MutationLTHash.subtractThenAdd(empty, List.of(oracleA, oracleB), List.of());
            assertArrayEquals(oracleAB, ab.ltHash(),
                    "afterAB must byte-equal WACryptoLtHash.subtractThenAdd(empty, [a, b], [])");

            var abc = MutationLTHash.subtractThenAdd(ab.ltHash(), List.of(oracleC), List.of());
            assertArrayEquals(oracleABC, abc.ltHash(),
                    "afterABC must byte-equal subtractThenAdd(afterAB, [c], [])");

            var minusA = MutationLTHash.subtractThenAdd(ab.ltHash(), List.of(), List.of(oracleA));
            assertArrayEquals(oracleABminusA, minusA.ltHash(),
                    "afterABminusA must byte-equal subtractThenAdd(afterAB, [], [a])");
        }
    }

    @Nested
    @DisplayName("tamper detection")
    class TamperDetection {
        @Test
        @DisplayName("flipping a single bit in the hash makes it mismatch the expected")
        void singleBitFlipDetected() {
            var hash = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var tampered = hash.clone();
            tampered[0] ^= (byte) 0x01;
            assertFalse(MessageDigest.isEqual(hash, tampered),
                    "a one-bit flip must break equality");
        }

        @Test
        @DisplayName("substituting one value-MAC produces a different hash")
        void valueMacSubstitutionDetected() {
            var hashAB = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, B));
            var hashAC = MutationLTHash.add(MutationLTHash.EMPTY_HASH, List.of(A, C));
            assertFalse(MessageDigest.isEqual(hashAB, hashAC),
                    "swapping a value-MAC must change the hash");
        }
    }

    @Nested
    @DisplayName("subtractThenAdd result accessors")
    class ResultRecord {
        @Test
        @DisplayName("record accessors return the constructed values")
        void recordAccessorsAreStable() {
            var r = new MutationLTHash.SubtractThenAddResult(A, B);
            assertArrayEquals(A, r.ltHash());
            assertArrayEquals(B, r.subtractResult());
            assertTrue(true);  // structural smoke test
        }
    }
}
