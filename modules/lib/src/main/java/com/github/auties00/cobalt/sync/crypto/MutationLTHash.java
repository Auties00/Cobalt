package com.github.auties00.cobalt.sync.crypto;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.store.linked.LinkedWhatsAppStore;

import javax.crypto.KDF;
import javax.crypto.spec.HKDFParameterSpec;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * The 128-byte lattice-hash accumulator that WhatsApp uses as the anti-tampering
 * digest for every app-state collection.
 *
 * <p>LT-Hash is a homomorphic group accumulator: each value MAC is expanded
 * via HKDF-SHA256 to 128 bytes and treated as 64 little-endian unsigned 16-bit
 * lanes that pointwise add into the running hash, with subtraction as the
 * inverse operation. The accumulator is commutative, associative, and
 * group-invertible, which is what lets the relay incrementally apply a patch
 * to a snapshot without re-reading every record.
 *
 * <p>Driven by both the incoming-patch verifier
 * ({@link MutationIntegrityVerifier}) and the outgoing-patch builder, which
 * both compute a new LT-Hash from the mutations they are about to commit. The
 * {@link #checkLtHash} and {@link #reportCollectionInconsistency} helpers are
 * used by periodic consistency tasks rather than by the main sync path.
 */
@WhatsAppWebModule(moduleName = "WACryptoLtHash")
@WhatsAppWebModule(moduleName = "WAWebSyncdAntiTamperingLtHash")
public final class MutationLTHash {
    /**
     * The logger for {@link MutationLTHash}.
     */
    private static final System.Logger LOGGER = Log.get(MutationLTHash.class);

    /**
     * The fixed byte length of every LT-Hash buffer.
     *
     * @implNote
     * This implementation renames the WA Web {@code KEY_LENGTH_BYTES} constant
     * to {@code HASH_LENGTH} to reflect its actual role: it is the hash output
     * length, not a key length.
     */
    @WhatsAppWebExport(moduleName = "WACryptoLtHash", exports = "KEY_LENGTH_BYTES", adaptation = WhatsAppAdaptation.DIRECT)
    public static final int HASH_LENGTH = 128;

    /**
     * The HKDF info bytes used when expanding each value MAC into a lane buffer.
     *
     * @implNote
     * The WA Web source names the matching field {@code salt} on
     * {@code LtHash16}; the value is passed to {@code WACryptoHkdf.extractAndExpand}
     * as the {@code info} parameter, not as the HKDF salt (the salt defaults
     * to the 32-byte zero string). The Cobalt name reflects the actual HKDF
     * argument position.
     */
    private static final byte[] HKDF_INFO = "WhatsApp Patch Integrity".getBytes();

    /**
     * The all-zero hash state.
     *
     * <p>The starting accumulator for a collection that has never seen a
     * mutation. Every {@link #add(byte[], List)} call moves the hash off zero;
     * every matching {@link #subtract(byte[], List)} call moves it back.
     */
    @WhatsAppWebExport(moduleName = "WACryptoLtHash", exports = "EMPTY_LT_HASH", adaptation = WhatsAppAdaptation.DIRECT)
    public static final byte[] EMPTY_HASH = new byte[HASH_LENGTH];

    /**
     * Prevents instantiation.
     */
    private MutationLTHash() {

    }

    /**
     * Expands a value MAC to {@value #HASH_LENGTH} bytes via HKDF-SHA256.
     *
     * <p>The per-element expansion step that turns each 32-byte value MAC into
     * a 128-byte vector ready for pointwise addition. The HKDF salt is null
     * (RFC 5869 default of 32 zero bytes); the info parameter is
     * {@link #HKDF_INFO}.
     *
     * @param valueMac the value MAC to expand
     * @return the expanded 128-byte lane buffer
     */
    private static byte[] expand(byte[] valueMac) {
        try {
            var kdf = KDF.getInstance("HKDF-SHA256");
            var params = HKDFParameterSpec.ofExtract()
                    .addIKM(valueMac)
                    .thenExpand(HKDF_INFO, HASH_LENGTH);
            return kdf.deriveData(params);
        } catch (GeneralSecurityException e) {
            throw new InternalError("Failed to expand value MAC via HKDF", e);
        }
    }

    /**
     * Performs pointwise wrapping arithmetic on two 128-byte hash buffers
     * treated as 64 little-endian unsigned 16-bit lanes.
     *
     * @param hash     the current hash state
     * @param expanded the HKDF-expanded value MAC
     * @param addition {@code true} for addition (modular sum), {@code false} for subtraction (modular difference)
     * @return a freshly allocated buffer holding the result
     */
    private static byte[] performPointwiseWithOverflow(byte[] hash, byte[] expanded, boolean addition) {
        var hashBuf = ByteBuffer.wrap(hash).order(ByteOrder.LITTLE_ENDIAN);
        var expandedBuf = ByteBuffer.wrap(expanded).order(ByteOrder.LITTLE_ENDIAN);
        var result = ByteBuffer.allocate(hashBuf.capacity()).order(ByteOrder.LITTLE_ENDIAN);
        for (var i = 0; i < hashBuf.capacity(); i += 2) {
            var a = Short.toUnsignedInt(hashBuf.getShort());
            var b = Short.toUnsignedInt(expandedBuf.getShort());
            result.putShort((short) ((addition ? a + b : a - b) & 0xFFFF));
        }
        return result.array();
    }

    /**
     * Adds a single value MAC into the running hash.
     *
     * @param currentHash the current hash state (must be {@value #HASH_LENGTH} bytes)
     * @param valueMac    the value MAC to add
     * @return the new hash state
     */
    private static byte[] addSingle(byte[] currentHash, byte[] valueMac) {
        var expanded = expand(valueMac);
        return performPointwiseWithOverflow(currentHash, expanded, true);
    }

    /**
     * Subtracts a single value MAC from the running hash.
     *
     * @param currentHash the current hash state (must be {@value #HASH_LENGTH} bytes)
     * @param valueMac    the value MAC to subtract
     * @return the new hash state
     */
    private static byte[] subtractSingle(byte[] currentHash, byte[] valueMac) {
        var expanded = expand(valueMac);
        return performPointwiseWithOverflow(currentHash, expanded, false);
    }

    /**
     * Folds a sequence of value MACs into the running hash by repeated addition.
     *
     * <p>Drives the simple-add path used when building a fresh snapshot's
     * LT-Hash from its decrypted mutations. Ordering of the input does not
     * change the result; the operation is commutative and associative.
     *
     * @param currentHash the current hash state (must be {@value #HASH_LENGTH} bytes)
     * @param valueMacs   the value MACs to add, in any order
     * @return the new hash state
     */
    @WhatsAppWebExport(moduleName = "WACryptoLtHash", exports = "LT_HASH_ANTI_TAMPERING", adaptation = WhatsAppAdaptation.ADAPTED)
    public static byte[] add(byte[] currentHash, List<byte[]> valueMacs) {
        var result = currentHash;
        for (var valueMac : valueMacs) {
            result = addSingle(result, valueMac);
        }
        return result;
    }

    /**
     * Folds a sequence of value MACs out of the running hash by repeated subtraction.
     *
     * <p>Inverse of {@link #add(byte[], List)}: subtracting every element of a
     * previously added sequence restores the original hash. Used by the
     * outgoing-patch builder when a mutation is replaced or removed.
     *
     * @param currentHash the current hash state (must be {@value #HASH_LENGTH} bytes)
     * @param valueMacs   the value MACs to subtract, in any order
     * @return the new hash state
     */
    @WhatsAppWebExport(moduleName = "WACryptoLtHash", exports = "LT_HASH_ANTI_TAMPERING", adaptation = WhatsAppAdaptation.ADAPTED)
    public static byte[] subtract(byte[] currentHash, List<byte[]> valueMacs) {
        var result = currentHash;
        for (var valueMac : valueMacs) {
            result = subtractSingle(result, valueMac);
        }
        return result;
    }

    /**
     * The paired output of {@link #subtractThenAdd}.
     *
     * <p>The intermediate {@code subtractResult} is preserved so that the
     * verbose diagnostic logging block that fires on a snapshot MAC mismatch
     * can report both phases without re-running the subtract.
     *
     * @param ltHash         the final hash after both phases
     * @param subtractResult the intermediate hash after the subtract phase
     */
    public record SubtractThenAddResult(
            byte[] ltHash,
            byte[] subtractResult
    ) {

    }

    /**
     * Subtracts a removal set out of the running hash and then adds an addition set.
     *
     * <p>The combined operation that the patch-application path uses to move
     * from a collection's old LT-Hash to its new one in a single sweep.
     * Returning the intermediate {@code subtractResult} lets the caller log
     * both phases without re-running the subtract.
     *
     * @param currentHash the current hash state (must be {@value #HASH_LENGTH} bytes)
     * @param toAdd       the value MACs to add (may be empty)
     * @param toRemove    the value MACs to subtract (may be empty)
     * @return the paired final and intermediate hashes
     */
    @WhatsAppWebExport(moduleName = "WACryptoLtHash", exports = "LT_HASH_ANTI_TAMPERING", adaptation = WhatsAppAdaptation.ADAPTED)
    public static SubtractThenAddResult subtractThenAdd(
        byte[] currentHash,
        List<byte[]> toAdd,
        List<byte[]> toRemove
    ) {
        var subtractResult = subtract(currentHash, toRemove);
        var ltHash = add(subtractResult, toAdd);
        return new SubtractThenAddResult(ltHash, subtractResult);
    }

    /**
     * Returns a defensive clone of a hash buffer, or {@code null} for a {@code null} input.
     *
     * <p>Used wherever a hash buffer escapes from a {@link MutationLTHash}
     * routine into mutable storage; the {@code null} tolerance matches the
     * nullable accessors on the collection-version store.
     *
     * @param hash the hash to clone
     * @return a defensive copy, or {@code null}
     */
    public static byte[] copy(byte[] hash) {
        return hash == null
                ? null
                : hash.clone();
    }

    /**
     * The outcome of an LT-Hash consistency check across one or more collections.
     *
     * <p>All three fields are {@code null} when the mutation-count threshold
     * triggered a skip; otherwise {@code isLtHashConsistent} carries the
     * boolean verdict and the two hash fields carry the first collection's
     * recomputed and stored hashes (used in the diagnostic log line).
     *
     * @param isLtHashConsistent {@code true} if every checked collection matched,
     *                           {@code false} if any mismatched,
     *                           {@code null} if the check was skipped
     * @param scratchLtHash      the first checked collection's recomputed hash, or {@code null}
     * @param cachedLtHash       the first checked collection's stored hash, or {@code null}
     */
    public record LtHashCheckResult(
            Boolean isLtHashConsistent,
            byte[] scratchLtHash,
            byte[] cachedLtHash
    ) {
    }

    /**
     * Recomputes the LT-Hash of one or every collection from the
     * {@link SyncActionEntry} table and compares it against the stored hash.
     *
     * <p>Driven by a periodic consistency task. Skipped (returns the unknown
     * sentinel) when the total mutation count across the checked collections
     * exceeds {@code maxMutations}; passing {@code null} disables the threshold.
     *
     * @implNote
     * This implementation always uses the caller-supplied threshold value as
     * is. WA Web overrides the threshold to {@code 900} for employee
     * accounts; Cobalt has no notion of employee status, so the override
     * does not apply.
     *
     * @param store        the store providing the {@code SyncActionEntry} table and the cached hashes
     * @param collection   a single collection to check, or {@code null} to check every collection
     * @param maxMutations the mutation-count threshold, or {@code null} to skip the threshold
     * @param context      a free-form context string folded into the inconsistency log line
     * @return the verdict and the diagnostic hash pair
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTamperingLtHash", exports = "checkLtHash", adaptation = WhatsAppAdaptation.ADAPTED)
    public static LtHashCheckResult checkLtHash(LinkedWhatsAppStore store, SyncPatchType collection, Integer maxMutations, String context) {
        var collectionsData = new ArrayList<CollectionCheckData>();
        if (collection == null) {
            for (var patchType : SyncPatchType.values()) {
                var metadata = store.syncStore().findWebAppState(patchType);
                var entries = store.syncStore().getSyncActionEntries(patchType);
                collectionsData.add(new CollectionCheckData(patchType, metadata.ltHash(), entries));
            }
        } else {
            var metadata = store.syncStore().findWebAppState(collection);
            var entries = store.syncStore().getSyncActionEntries(collection);
            collectionsData.add(new CollectionCheckData(collection, metadata.ltHash(), entries));
        }

        var totalMutations = 0;
        for (var data : collectionsData) {
            totalMutations += data.entries().size();
        }

        if (maxMutations != null && totalMutations > maxMutations) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "ltHash check skipped: {0} mutations exceeds threshold {1}", totalMutations, maxMutations);
            return new LtHashCheckResult(null, null, null);
        }

        byte[] scratchLtHash = null;
        byte[] cachedLtHash = null;
        var consistent = true;
        var inconsistentCollections = new ArrayList<SyncPatchType>();

        for (var data : collectionsData) {
            var computed = computeFromEntries(data.entries());
            if (scratchLtHash == null) {
                scratchLtHash = computed;
            }
            if (cachedLtHash == null) {
                cachedLtHash = data.storedLtHash();
            }
            if (!Arrays.equals(data.storedLtHash(), computed) && totalMutations > 0) {
                consistent = false;
                inconsistentCollections.add(data.collection());
            }
        }

        if (!inconsistentCollections.isEmpty() && Log.WARNING) {
            LOGGER.log(Level.WARNING, "ltHash check failed for {0} collections in context {1}: {2}",
                    inconsistentCollections.size(), context,
                    inconsistentCollections.subList(0, Math.min(3, inconsistentCollections.size())));
        }

        return new LtHashCheckResult(consistent, scratchLtHash, cachedLtHash);
    }

    /**
     * Runs {@link #checkLtHash} on a single collection and emits a diagnostic
     * log line describing the result.
     *
     * <p>Driven by the collection handler at patch-pre-processing time and on
     * any path that wants a side-effecting consistency probe. Defaults the
     * mutation-count threshold to {@code 400} when {@code maxMutations} is
     * {@code null}.
     *
     * @implNote
     * This implementation does not call into the WA Web telemetry helpers
     * (no {@code sendLogs}, no {@code printSyncdLog}). The log line carries
     * the truncated hex suffixes of both the recomputed and stored hashes,
     * which is the same surface that the matching WA Web log lines expose.
     *
     * @param store             the store providing the {@link SyncActionEntry} table and the cached hashes
     * @param collection        the collection to check
     * @param diagnosticContext a free-form context describing the call site
     * @param checkContext      the context string passed through to {@link #checkLtHash}
     * @param maxMutations      the mutation-count threshold, defaulting to {@code 400} when {@code null}
     * @return {@code true} on detected inconsistency, {@code false} on a confirmed match,
     *         {@code null} when the check was skipped
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdAntiTamperingLtHash", exports = "reportCollectionInconsistency", adaptation = WhatsAppAdaptation.ADAPTED)
    public static Boolean reportCollectionInconsistency(
            LinkedWhatsAppStore store,
            SyncPatchType collection,
            String diagnosticContext,
            String checkContext,
            Integer maxMutations
    ) {
        var threshold = maxMutations != null ? maxMutations : 400;
        var result = checkLtHash(store, collection, threshold, checkContext);

        var scratchSuffix = result.scratchLtHash() == null
                ? ""
                : hexPaddedSuffix(result.scratchLtHash(), 16);
        var cachedSuffix = result.cachedLtHash() == null
                ? ""
                : hexPaddedSuffix(result.cachedLtHash(), 16);

        if (result.isLtHashConsistent() != null && !result.isLtHashConsistent()) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "lthash inconsistent in context {0}: scratch={1}, cached={2}, diagnostic={3}",
                        checkContext, scratchSuffix, cachedSuffix, diagnosticContext);
            }
            return true;
        } else if (result.isLtHashConsistent() != null && result.isLtHashConsistent()) {
            if (Log.DEBUG) {
                LOGGER.log(Level.DEBUG, "lthash consistent in context {0}: scratch={1}, cached={2}, diagnostic={3}",
                        checkContext, scratchSuffix, cachedSuffix, diagnosticContext);
            }
            return false;
        }

        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "lthash consistency unknown in context {0}: scratch={1}, cached={2}, diagnostic={3}",
                    checkContext, scratchSuffix, cachedSuffix, diagnosticContext);
        }
        return null;
    }

    /**
     * Recomputes the LT-Hash for a collection from its persisted entries.
     *
     * <p>Helper for {@link #checkLtHash}. Entries are deduplicated by hex
     * {@code indexMac} with last-write-wins semantics before they are folded
     * into the hash.
     *
     * @param entries the persisted sync action entries for the collection
     * @return the recomputed LT-Hash
     */
    private static byte[] computeFromEntries(Collection<SyncActionEntry> entries) {
        var deduplicated = new LinkedHashMap<String, byte[]>();
        for (var entry : entries) {
            if (entry.indexMac() != null && entry.valueMac() != null) {
                deduplicated.put(HexFormat.of().formatHex(entry.indexMac()), entry.valueMac());
            }
        }
        return add(EMPTY_HASH, List.copyOf(deduplicated.values()));
    }

    /**
     * Returns the trailing {@code suffixLength} hex characters of {@code data}.
     *
     * @param data         the bytes to encode
     * @param suffixLength the number of trailing hex characters to keep
     * @return the suffix, or the full encoding when shorter than {@code suffixLength}
     */
    private static String hexPaddedSuffix(byte[] data, int suffixLength) {
        var hex = HexFormat.of().formatHex(data);
        return hex.length() <= suffixLength
                ? hex
                : hex.substring(hex.length() - suffixLength);
    }

    /**
     * A bundle of one collection's identity, stored hash, and entries.
     *
     * @param collection    the collection type
     * @param storedLtHash  the stored hash from {@code WebAppState.ltHash()}
     * @param entries       the {@code SyncActionEntry} rows for the collection
     */
    private record CollectionCheckData(
            SyncPatchType collection,
            byte[] storedLtHash,
            Collection<SyncActionEntry> entries
    ) {
    }
}
