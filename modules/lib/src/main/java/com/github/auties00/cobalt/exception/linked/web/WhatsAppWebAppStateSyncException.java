package com.github.auties00.cobalt.exception.linked.web;

import com.github.auties00.cobalt.client.linked.WhatsAppLinkedClientErrorResult;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.error.DisconnectReason;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Sealed root for failures during Web App State (also known as syncd)
 * synchronization.
 *
 * <p>Web App State is the encrypted key-value protocol WhatsApp uses to
 * keep contacts, chat metadata, settings, starred messages, the blocklist,
 * and similar shared state in sync across the primary phone and every
 * linked companion device. State is published as named collections;
 * updates are sent as patches the receiving device applies on top of the
 * last known snapshot. Each patch carries mutations whose values, indices,
 * and ordering are protected by HMACs and chained against the previous
 * patch.
 *
 * <p>The nested subtypes split into three buckets:<ul>
 *   <li>fatal integrity failures at the snapshot, patch, value, or index
 *       level, unrecoverable key losses, decryption failures, structural
 *       violations of the patch stream, terminal patches, and the
 *       catch-all {@link UnexpectedError};
 *   <li>retryable failures: {@link Conflict}, {@link RetryableServerError},
 *       {@link ExternalDownloadFailed}, {@link ExternalDecodeFailed};
 *   <li>recoverable missing-key failures: {@link MissingKey}.
 * </ul>
 *
 * @apiNote
 * Raised during state synchronization; {@link #isFatal()} reports the
 * per-subtype classification the app-state pipeline uses to decide whether
 * to wipe and resync the affected collection, schedule a retry, or wait for
 * an out-of-band key delivery. The session-level recovery is carried by
 * {@link #toErrorResult()}, which returns
 * {@link WhatsAppLinkedClientErrorResult#DISCARD} for every subtype except
 * {@link MissingKeyOnAllDevices}, which returns
 * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}.
 *
 * @implNote
 * This implementation classifies each subtype individually rather than
 * collapsing them onto the three coarse fatal/retryable/missing-key
 * classes the wire protocol uses, so embedders can distinguish, for
 * example, a {@link Conflict} (must refetch) from a
 * {@link RetryableServerError} (must honour
 * {@link RetryableServerError#serverBackoffMs}).
 *
 * @see SnapshotMacMismatch
 * @see PatchMacMismatch
 * @see ValueMacMismatch
 * @see IndexMacMismatch
 * @see MissingKey
 * @see MissingKeyOnAllDevices
 * @see TimeoutWhileWaitingForMissingKey
 * @see MissingPatches
 * @see TerminalPatch
 * @see Conflict
 * @see RetryableServerError
 * @see DecryptionFailed
 * @see ExternalDownloadFailed
 * @see ExternalDecodeFailed
 * @see MacComputationFailed
 * @see MissingActionTimestamp
 * @see DuplicateIndexInPatch
 * @see DuplicatePatchVersion
 * @see UnexpectedError
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdError")
public sealed abstract class WhatsAppWebAppStateSyncException extends WhatsAppWebException
        permits WhatsAppWebAppStateSyncException.SnapshotMacMismatch,
                WhatsAppWebAppStateSyncException.PatchMacMismatch,
                WhatsAppWebAppStateSyncException.ValueMacMismatch,
                WhatsAppWebAppStateSyncException.IndexMacMismatch,
                WhatsAppWebAppStateSyncException.MissingKey,
                WhatsAppWebAppStateSyncException.MissingKeyOnAllDevices,
                WhatsAppWebAppStateSyncException.TimeoutWhileWaitingForMissingKey,
                WhatsAppWebAppStateSyncException.MissingPatches,
                WhatsAppWebAppStateSyncException.TerminalPatch,
                WhatsAppWebAppStateSyncException.Conflict,
                WhatsAppWebAppStateSyncException.RetryableServerError,
                WhatsAppWebAppStateSyncException.DecryptionFailed,
                WhatsAppWebAppStateSyncException.ExternalDownloadFailed,
                WhatsAppWebAppStateSyncException.ExternalDecodeFailed,
                WhatsAppWebAppStateSyncException.MacComputationFailed,
                WhatsAppWebAppStateSyncException.MissingActionTimestamp,
                WhatsAppWebAppStateSyncException.DuplicateIndexInPatch,
                WhatsAppWebAppStateSyncException.DuplicatePatchVersion,
                WhatsAppWebAppStateSyncException.UnexpectedError {

    /**
     * Constructs a new web app state sync exception with the specified detail message.
     *
     * @param message the detail message describing the sync failure
     */
    protected WhatsAppWebAppStateSyncException(String message) {
        super(message);
    }

    /**
     * Constructs a new web app state sync exception with a detail message and cause.
     *
     * @param message the detail message describing the sync failure
     * @param cause   the underlying cause of the sync failure
     */
    protected WhatsAppWebAppStateSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns whether this sync failure is fatal to the affected
     * collection.
     *
     * <p>A fatal failure means the collection cannot be reconciled in place
     * and must be wiped and resynced from a fresh snapshot; a non-fatal
     * failure (a key that may still arrive, a retryable server error, a
     * conflict) can be recovered without discarding the collection. The
     * app-state sync pipeline consults this classification when deciding
     * whether to attempt recovery and whether to emit a fatal-error metric;
     * it is distinct from {@link #toErrorResult()}, which carries the
     * session-level recovery action.
     *
     * @implSpec
     * Every concrete subtype returns a constant: integrity failures,
     * unrecoverable key losses, decryption failures, structural
     * patch-stream violations, and terminal patches return {@code true};
     * conflicts, retryable server errors, external download and decode
     * failures, and a transiently missing key return {@code false}.
     *
     * @return {@code true} if the collection must be wiped and resynced,
     *         {@code false} if the failure is recoverable in place
     */
    public abstract boolean isFatal();

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation returns {@link WhatsAppLinkedClientErrorResult#DISCARD}:
     * a sync failure resyncs or retries the affected collection without
     * tearing the session down. {@link MissingKeyOnAllDevices} overrides
     * this with {@link WhatsAppLinkedClientErrorResult#LOG_OUT} because an
     * unrecoverable key with no holder forces a re-pair.
     */
    @Override
    public WhatsAppLinkedClientErrorResult toErrorResult() {
        return WhatsAppLinkedClientErrorResult.DISCARD;
    }

    /**
     * Thrown when the HMAC stamped over a state snapshot does not match
     * the value Cobalt computes locally.
     *
     * <p>Snapshots are full state dumps used during initial sync and after
     * a fatal resync. A mismatch means the snapshot bytes are corrupt,
     * were tampered with, or were validated against the wrong key; the
     * collection has to be wiped and refetched.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class SnapshotMacMismatch extends WhatsAppWebAppStateSyncException {
        /**
         * The collection whose snapshot failed validation.
         */
        private final SyncPatchType collectionName;

        /**
         * The snapshot version that failed validation.
         */
        private final long version;

        /**
         * Constructs a new snapshot MAC mismatch exception.
         *
         * @param collectionName the collection whose snapshot failed validation
         * @param version        the snapshot version
         * @throws NullPointerException if {@code collectionName} is {@code null}
         */
        public SnapshotMacMismatch(SyncPatchType collectionName, long version) {
            super("Snapshot MAC mismatch for collection " + collectionName + " at version " + version);
            this.collectionName = Objects.requireNonNull(collectionName);
            this.version = version;
        }

        /**
         * Returns the collection whose snapshot failed validation.
         *
         * @return the collection identifier, never {@code null}
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the snapshot version that failed validation.
         *
         * @return the snapshot version
         */
        public long version() {
            return version;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: snapshot
         * integrity failures require a wipe-and-resync.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when the HMAC stamped over a state patch does not match the
     * value Cobalt computes locally.
     *
     * <p>Patches are incremental updates chained on top of the latest
     * snapshot. A MAC mismatch means the patch bytes are corrupt, were
     * tampered with, or that the chain has been broken by a missing
     * predecessor; the affected collection has to be wiped and resynced.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class PatchMacMismatch extends WhatsAppWebAppStateSyncException {
        /**
         * The collection whose patch failed validation.
         */
        private final SyncPatchType collectionName;

        /**
         * The patch version that failed validation.
         */
        private final long version;

        /**
         * Constructs a new patch MAC mismatch exception.
         *
         * @param collectionName the collection whose patch failed validation
         * @param version        the patch version
         * @throws NullPointerException if {@code collectionName} is {@code null}
         */
        public PatchMacMismatch(SyncPatchType collectionName, long version) {
            super("Patch MAC mismatch for collection " + collectionName + " at version " + version);
            this.collectionName = Objects.requireNonNull(collectionName);
            this.version = version;
        }

        /**
         * Returns the collection whose patch failed validation.
         *
         * @return the collection identifier, never {@code null}
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the patch version that failed validation.
         *
         * @return the patch version
         */
        public long version() {
            return version;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a broken
         * patch chain requires a wipe-and-resync.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when the HMAC over the encrypted value of a single mutation
     * does not match the expected value after decryption.
     *
     * <p>The value MAC ties an encrypted mutation to its plaintext. A
     * mismatch means either the ciphertext was corrupted or the decryption
     * key is wrong; in either case the collection cannot be trusted and
     * has to be resynced.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class ValueMacMismatch extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new value MAC mismatch exception.
         */
        public ValueMacMismatch() {
            super("Value MAC mismatch: mutation value integrity check failed");
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: value
         * integrity failures require a wipe-and-resync.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when the HMAC over the encrypted index of a single mutation
     * does not match the expected value after decryption.
     *
     * <p>The index MAC binds a mutation to its key in the key-value store
     * and prevents an attacker from substituting one mutation for another.
     * A mismatch is treated as data corruption and the collection is
     * resynced.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class IndexMacMismatch extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new index MAC mismatch exception.
         */
        public IndexMacMismatch() {
            super("Index MAC mismatch: mutation index integrity check failed");
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: index
         * integrity failures require a wipe-and-resync.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when a sync mutation references an encryption key that is not
     * yet present locally.
     *
     * <p>App-state keys are pushed from the primary phone and rotated
     * periodically.
     *
     * @apiNote
     * Raised on a transient key gap; requesting the key from a companion
     * that has it and waiting for the reply is normally enough to make
     * progress, which is why {@link #isFatal()} reports {@code false}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdMissingKeyError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class MissingKey extends WhatsAppWebAppStateSyncException {
        /**
         * The identifiers of every key referenced by the snapshot or patch that
         * is absent from the local store, in first-seen order.
         */
        private final List<byte[]> keyIds;

        /**
         * Constructs a new missing key exception for a single key.
         *
         * <p>Equivalent to a single-element {@link #MissingKey(List)}; retained
         * for call sites such as recovery-snapshot ingest that fail on the first
         * absent key rather than scanning a whole batch.
         *
         * @param keyId the identifier of the missing key
         * @throws NullPointerException if {@code keyId} is {@code null}
         */
        public MissingKey(byte[] keyId) {
            this(List.of(Objects.requireNonNull(keyId, "keyId cannot be null")));
        }

        /**
         * Constructs a new missing key exception for every absent key found in a
         * snapshot or patch.
         *
         * <p>The batch mirrors WhatsApp Web's snapshot and patch missing-key
         * scans, which request every absent key in one round so the affected
         * collection unblocks after a single key-share rather than one per round.
         *
         * @param keyIds the identifiers of the missing keys, in first-seen order
         * @throws NullPointerException     if {@code keyIds} is {@code null}
         * @throws IllegalArgumentException if {@code keyIds} is empty
         */
        public MissingKey(List<byte[]> keyIds) {
            super("Missing sync key(s) with id " + HexFormat.of().formatHex(
                    requireNonEmptyFirst(keyIds)));
            this.keyIds = List.copyOf(keyIds);
        }

        /**
         * Validates the supplied key-id batch and returns its first element for
         * the detail message.
         *
         * @param keyIds the candidate batch
         * @return the first key id
         * @throws NullPointerException     if {@code keyIds} is {@code null}
         * @throws IllegalArgumentException if {@code keyIds} is empty
         */
        private static byte[] requireNonEmptyFirst(List<byte[]> keyIds) {
            Objects.requireNonNull(keyIds, "keyIds cannot be null");
            if (keyIds.isEmpty()) {
                throw new IllegalArgumentException("keyIds cannot be empty");
            }
            return keyIds.getFirst();
        }

        /**
         * Returns the identifier of the first missing key.
         *
         * <p>A convenience for logging and for the single-key call sites; the
         * full batch is available via {@link #keyIds()}.
         *
         * @return the first missing key id, never {@code null}
         */
        public byte[] keyId() {
            return keyIds.getFirst();
        }

        /**
         * Returns the identifiers of every missing key the snapshot or patch
         * referenced.
         *
         * @return an unmodifiable list of the missing key ids, never empty
         */
        public List<byte[]> keyIds() {
            return keyIds;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code false}: the key may
         * arrive from a companion and let progress resume.
         */
        @Override
        public boolean isFatal() {
            return false;
        }
    }

    /**
     * Thrown when no companion device has the encryption key that a
     * mutation needs.
     *
     * <p>Cobalt asks every other device about a missing key before
     * concluding the key is unrecoverable; this exception is raised once
     * every device has answered that it does not hold the key. The
     * affected collection cannot be decrypted on this device without
     * re-pairing.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdStoreMissingKeys")
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class MissingKeyOnAllDevices extends WhatsAppWebAppStateSyncException {
        /**
         * The identifier of the key that no companion device holds.
         */
        private final byte[] keyId;

        /**
         * Constructs a new missing-key-on-all-devices exception.
         *
         * @param keyId the identifier of the missing key
         * @throws NullPointerException if {@code keyId} is {@code null}
         */
        public MissingKeyOnAllDevices(byte[] keyId) {
            super("Missing sync key with id " + HexFormat.of().formatHex(
                    Objects.requireNonNull(keyId, "keyId cannot be null")) + " on all companion devices");
            this.keyId = keyId;
        }

        /**
         * Returns the identifier of the missing key.
         *
         * @return the key id, never {@code null}
         */
        public byte[] keyId() {
            return keyId;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: with no
         * device holding the key, only re-pairing recovers the
         * collection.
         */
        @Override
        public boolean isFatal() {
            return true;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation returns
         * {@link WhatsAppLinkedClientErrorResult#LOG_OUT}: with no companion
         * device holding the key the collection can only be recovered by
         * re-pairing, matching WhatsApp Web's
         * {@code SyncdFatalErrorType.MISSING_KEY_ON_ALL_CLIENTS} terminal
         * state.
         */
        @Override
        public WhatsAppLinkedClientErrorResult toErrorResult() {
            return WhatsAppLinkedClientErrorResult.LOG_OUT;
        }
    }

    /**
     * Thrown when the wait for a missing app-state key expires before any
     * companion device has answered with the key.
     *
     * <p>The wait timeout is longer than the per-device round-trip, so
     * timing out means the key is effectively unobtainable and the
     * collection has to be resynced from scratch.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdStoreMissingKeys")
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class TimeoutWhileWaitingForMissingKey extends WhatsAppWebAppStateSyncException {
        /**
         * The identifier of the key whose wait timed out.
         */
        private final byte[] keyId;

        /**
         * Constructs a new timeout-while-waiting-for-missing-key exception.
         *
         * @param keyId the identifier of the missing key whose wait timed out
         * @throws NullPointerException if {@code keyId} is {@code null}
         */
        public TimeoutWhileWaitingForMissingKey(byte[] keyId) {
            super("Timeout waiting for missing sync key with id " + HexFormat.of().formatHex(
                    Objects.requireNonNull(keyId, "keyId cannot be null")));
            this.keyId = keyId;
        }

        /**
         * Returns the identifier of the missing key whose wait timed out.
         *
         * @return the key id, never {@code null}
         */
        public byte[] keyId() {
            return keyId;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: an unbounded
         * wait failure means the key is unobtainable.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when AES-GCM decryption of a mutation fails after the correct
     * key has been located.
     *
     * <p>Typically indicates corrupted ciphertext or an issue with key
     * derivation; the collection is wiped and resynced.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class DecryptionFailed extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new decryption failed exception wrapping a cause.
         *
         * @param cause the underlying cryptographic exception
         */
        public DecryptionFailed(Throwable cause) {
            super("Failed to decrypt mutation", cause);
        }

        /**
         * Constructs a new decryption failed exception with a message and cause.
         *
         * @param message extra detail about the decryption failure
         * @param cause   the underlying cryptographic exception
         */
        public DecryptionFailed(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a decryption
         * failure after the right key is in hand cannot be retried.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when downloading an external mutation blob fails.
     *
     * <p>Mutations larger than the inline limit are stored on the media
     * servers; the patch only carries a URL and the encryption key. The
     * download can be retried.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdNetCallbacksApi")
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdRetryableError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class ExternalDownloadFailed extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new external download failed exception.
         *
         * @param cause the underlying I/O or network exception
         */
        public ExternalDownloadFailed(Throwable cause) {
            super("Failed to download external mutations", cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code false}: the
         * external blob may become available on a retry.
         */
        @Override
        public boolean isFatal() {
            return false;
        }
    }

    /**
     * Thrown when an external mutation blob downloads successfully but its
     * decoded payload is malformed.
     *
     * <p>A common cause is a stale recovery snapshot that no longer
     * matches the current schema; re-fetching a fresh blob can decode
     * successfully.
     */
    @WhatsAppWebModule(moduleName = "WAWebNonMessageDataRequestHandler")
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdRetryableError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class ExternalDecodeFailed extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new external decode failed exception.
         *
         * @param cause the underlying parsing or decompression exception
         */
        public ExternalDecodeFailed(Throwable cause) {
            super("Failed to decode external mutations", cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code false}: re-fetching
         * a fresh blob may decode successfully.
         */
        @Override
        public boolean isFatal() {
            return false;
        }
    }

    /**
     * Thrown when computing an HMAC for a sync operation fails.
     *
     * <p>Indicates a problem with the JCE provider, the key material, or
     * the cryptographic state of the process.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class MacComputationFailed extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new MAC computation failed exception.
         *
         * @param cause the underlying cryptographic exception
         */
        public MacComputationFailed(Throwable cause) {
            super("Failed to compute MAC", cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: HMAC
         * computation is required for every outgoing patch.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when a SET mutation arrives without the timestamp every
     * SyncActionValue must carry.
     *
     * <p>The validation only applies to SET mutations; REMOVE mutations
     * have no SyncActionValue and are unaffected.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class MissingActionTimestamp extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new missing action timestamp exception.
         */
        public MissingActionTimestamp() {
            super("Missing action timestamp in sync mutation");
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a SET
         * mutation without a timestamp cannot be ordered against peers.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when a single patch contains two mutations of the same kind
     * (two SETs or two REMOVEs) targeting the same index.
     *
     * <p>Such a patch is malformed by construction and processing cannot
     * continue safely.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class DuplicateIndexInPatch extends WhatsAppWebAppStateSyncException {
        /**
         * The collection that contained the duplicate index.
         */
        private final SyncPatchType collectionName;

        /**
         * Constructs a new duplicate index in patch exception.
         *
         * @param collectionName the affected collection
         */
        public DuplicateIndexInPatch(SyncPatchType collectionName) {
            super("Same index for multiple mutations in patch for collection " + collectionName);
            this.collectionName = collectionName;
        }

        /**
         * Returns the collection that contained the duplicate index.
         *
         * @return the collection identifier
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a malformed
         * patch cannot be applied partially.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when two patches in the same collection share the same
     * version number.
     *
     * <p>Patch versions are required to be unique; a duplicate makes it
     * impossible to determine ordering and the response is rejected.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class DuplicatePatchVersion extends WhatsAppWebAppStateSyncException {
        /**
         * The collection that contained the duplicate version.
         */
        private final SyncPatchType collectionName;

        /**
         * The version that appeared on more than one patch.
         */
        private final long version;

        /**
         * Constructs a new duplicate patch version exception.
         *
         * @param collectionName the affected collection
         * @param version        the duplicated version number
         */
        public DuplicatePatchVersion(SyncPatchType collectionName, long version) {
            super("Duplicate patch version " + version + " in collection " + collectionName);
            this.collectionName = Objects.requireNonNull(collectionName);
            this.version = version;
        }

        /**
         * Returns the collection that contained the duplicate version.
         *
         * @return the collection identifier, never {@code null}
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the duplicated version number.
         *
         * @return the version number
         */
        public long version() {
            return version;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a duplicate
         * version makes patch ordering ambiguous.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown for sync failures that do not match any of the more specific
     * categories.
     *
     * <p>Treated as fatal because the cause is unknown and the safe
     * recovery is to wipe the affected collection and resync.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class UnexpectedError extends WhatsAppWebAppStateSyncException {
        /**
         * Constructs a new unexpected error exception wrapping a cause.
         *
         * @param cause the underlying unexpected exception
         */
        public UnexpectedError(Throwable cause) {
            super("Unexpected sync error", cause);
        }

        /**
         * Constructs a new unexpected error exception with a message and cause.
         *
         * @param message extra detail about the unexpected error
         * @param cause   the underlying unexpected exception
         */
        public UnexpectedError(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: when the
         * cause is unknown, only a wipe-and-resync is safe.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when there is a gap in the patch sequence the server
     * delivered: the lowest version in the response is greater than the
     * local version plus one.
     *
     * <p>The chain integrity is broken and the only safe recovery is to
     * wipe the collection and resync from a fresh snapshot.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class MissingPatches extends WhatsAppWebAppStateSyncException {
        /**
         * The collection that has missing patches.
         */
        private final SyncPatchType collectionName;

        /**
         * The current local version of the collection.
         */
        private final long localVersion;

        /**
         * The lowest version among the patches that did arrive.
         */
        private final long minPatchVersion;

        /**
         * Constructs a new missing patches exception.
         *
         * @param collectionName  the affected collection
         * @param localVersion    the current local version
         * @param minPatchVersion the lowest version among received patches
         * @throws NullPointerException if {@code collectionName} is {@code null}
         */
        public MissingPatches(SyncPatchType collectionName, long localVersion, long minPatchVersion) {
            super("Missing patches for collection " + collectionName
                    + ": local version " + localVersion + ", min patch version " + minPatchVersion);
            this.collectionName = Objects.requireNonNull(collectionName);
            this.localVersion = localVersion;
            this.minPatchVersion = minPatchVersion;
        }

        /**
         * Returns the collection that has missing patches.
         *
         * @return the collection identifier, never {@code null}
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the current local version of the collection.
         *
         * @return the local version
         */
        public long localVersion() {
            return localVersion;
        }

        /**
         * Returns the lowest version among the patches that did arrive.
         *
         * @return the minimum patch version
         */
        public long minPatchVersion() {
            return minPatchVersion;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a broken
         * patch chain cannot be back-filled.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when the server marks a patch as terminal, signaling that the
     * collection data is unrecoverable.
     *
     * <p>The {@link #exitCode()} carries the server's reason; the
     * collection is wiped and resynchronized from scratch.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdFatalError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class TerminalPatch extends WhatsAppWebAppStateSyncException {
        /**
         * The collection that received the terminal patch.
         */
        private final SyncPatchType collectionName;

        /**
         * The exit code declaring why the data is unrecoverable.
         */
        private final DisconnectReason exitCode;

        /**
         * Constructs a new terminal patch exception.
         *
         * @param collectionName the affected collection
         * @param exitCode       the exit code carried by the patch
         * @throws NullPointerException if {@code collectionName} or {@code exitCode} is {@code null}
         */
        public TerminalPatch(SyncPatchType collectionName, DisconnectReason exitCode) {
            super("Terminal patch for collection " + collectionName + " with exit code: " + exitCode);
            this.collectionName = Objects.requireNonNull(collectionName);
            this.exitCode = Objects.requireNonNull(exitCode);
        }

        /**
         * Returns the collection that received the terminal patch.
         *
         * @return the collection identifier, never {@code null}
         */
        public SyncPatchType collectionName() {
            return collectionName;
        }

        /**
         * Returns the exit code carried by the terminal patch.
         *
         * @return the exit code, never {@code null}
         */
        public DisconnectReason exitCode() {
            return exitCode;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code true}: a terminal
         * patch is the server explicitly declaring the data
         * unrecoverable.
         */
        @Override
        public boolean isFatal() {
            return true;
        }
    }

    /**
     * Thrown when the server returns a {@code 409 Conflict} response to a
     * patch publication, meaning a newer version exists on the server.
     *
     * @apiNote
     * Raised on a stale patch publication; the caller refetches the
     * collection and reapplies the patch on top of the new version.
     * {@link #hasMorePatches()} reports whether further patches are pending
     * after the conflict.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdRetryableError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class Conflict extends WhatsAppWebAppStateSyncException {
        /**
         * Whether the server signaled that more patches are available
         * after the conflict.
         */
        private final boolean hasMorePatches;

        /**
         * Constructs a new conflict exception.
         *
         * @param hasMorePatches whether the server signaled more patches are available
         */
        public Conflict(boolean hasMorePatches) {
            super("Server returned 409 conflict");
            this.hasMorePatches = hasMorePatches;
        }

        /**
         * Returns whether the server signaled that more patches are
         * available after the conflict.
         *
         * @return {@code true} if more patches are pending
         */
        public boolean hasMorePatches() {
            return hasMorePatches;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code false}: a conflict
         * is resolved by refetch-and-reapply.
         */
        @Override
        public boolean isFatal() {
            return false;
        }
    }

    /**
     * Thrown when the server returns a retryable error code other than
     * {@code 409 Conflict} or one of the fatal codes.
     *
     * @apiNote
     * Raised on a server-classified retryable failure; the server may
     * attach a backoff hint via {@link #serverBackoffMs()} that should be
     * honored before the next attempt.
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdError", exports = "SyncdRetryableError",
                       adaptation = WhatsAppAdaptation.ADAPTED)
    public static final class RetryableServerError extends WhatsAppWebAppStateSyncException {
        /**
         * The error code the server returned.
         */
        private final String errorCode;

        /**
         * The server-suggested backoff in milliseconds, or {@code null}
         * when none was provided.
         */
        private final Long serverBackoffMs;

        /**
         * Constructs a new retryable server error exception.
         *
         * @param errorCode the server error code
         */
        public RetryableServerError(String errorCode) {
            this(errorCode, null);
        }

        /**
         * Constructs a new retryable server error exception with a server-suggested backoff.
         *
         * @param errorCode       the server error code
         * @param serverBackoffMs the server-suggested backoff in milliseconds, or {@code null}
         */
        public RetryableServerError(String errorCode, Long serverBackoffMs) {
            super("Server returned retryable error code: " + errorCode);
            this.errorCode = errorCode;
            this.serverBackoffMs = serverBackoffMs;
        }

        /**
         * Returns the error code the server returned.
         *
         * @return the error code
         */
        public String errorCode() {
            return errorCode;
        }

        /**
         * Returns the server-suggested backoff in milliseconds, when one was provided.
         *
         * @return the backoff in milliseconds, or {@code null} when none was provided
         */
        public Long serverBackoffMs() {
            return serverBackoffMs;
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation always returns {@code false}: the server
         * has explicitly classified the failure as retryable.
         */
        @Override
        public boolean isFatal() {
            return false;
        }
    }
}
