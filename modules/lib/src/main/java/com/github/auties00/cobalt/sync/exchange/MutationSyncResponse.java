package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.media.MediaConnectionService;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatch;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.*;

/**
 * Carries one parsed {@code <collection>} response produced by {@link MutationResponseParser}.
 *
 * <p>Each instance describes a single collection: its {@link SyncPatchType}, the server's
 * reported {@code version} and {@code has_more_patches} flag, the decoded patches (or
 * snapshot blob reference), and any captured collection-level error from a batched parse.
 * Both {@link MutationResponseParser#parseSyncResponse(Stanza)}
 * (single-collection) and
 * {@link MutationResponseParser#parseBatchedSyncResponse(Stanza)}
 * (multi-collection) return instances. Snapshot responses populate {@link #snapshotReference()}
 * and leave {@link #patches()} empty; patch responses do the inverse. Batched responses may
 * additionally surface an exception via {@link #collectionError()} so the caller can apply the
 * surviving collections without losing the failure.
 *
 * @implNote
 * This implementation is a hand-rolled value class rather than a {@link Record} because the
 * {@link #patches()} accessor wraps the field in
 * {@link Collections#unmodifiableSequencedCollection(SequencedCollection)} on each call, which
 * a record component accessor cannot do.
 */
public final class MutationSyncResponse {
    /**
     * Holds the collection type this response applies to.
     */
    private final SyncPatchType collectionName;

    /**
     * Holds the collection version reported by the server, or {@code 0} when the response was
     * a captured collection-level error.
     */
    private final long version;

    /**
     * Holds whether the server signalled that more patches are available beyond this response.
     */
    private final boolean hasMore;

    /**
     * Holds the decoded patches in document order, or {@code null} when the response was a
     * snapshot or carried a collection-level error.
     */
    private final SequencedCollection<SyncdPatch> patches;

    /**
     * Holds the decoded {@link ExternalBlobReference} for a snapshot response, or {@code null}
     * when the response carried patches.
     */
    private final ExternalBlobReference snapshotReference;

    /**
     * Holds the collection-level error captured during a batched parse, or {@code null} when
     * the collection succeeded.
     */
    private final WhatsAppWebAppStateSyncException collectionError;

    /**
     * Constructs a successful sync response with no collection-level error.
     *
     * <p>Delegates to
     * {@link #MutationSyncResponse(SyncPatchType, long, boolean, SequencedCollection, ExternalBlobReference, WhatsAppWebAppStateSyncException)}
     * with a {@code null} error, used by
     * {@link MutationResponseParser#parseSyncResponse(Stanza)}
     * and the success path of
     * {@link MutationResponseParser#parseBatchedSyncResponse(Stanza)}.
     *
     * @param collectionName the {@link SyncPatchType}; never {@code null}
     * @param version the collection version reported by the server
     * @param hasMore whether more patches are available
     * @param patches the decoded patches; may be {@code null} (treated as empty)
     * @param snapshotReference the snapshot reference; {@code null} for patch responses
     */
    public MutationSyncResponse(
            SyncPatchType collectionName,
            long version,
            boolean hasMore,
            SequencedCollection<SyncdPatch> patches,
            ExternalBlobReference snapshotReference
    ) {
        this(collectionName, version, hasMore, patches, snapshotReference, null);
    }

    /**
     * Constructs a sync response, optionally with a captured collection-level error.
     *
     * <p>The failure-capture path of
     * {@link MutationResponseParser#parseBatchedSyncResponse(Stanza)}
     * uses the {@code collectionError} parameter so a single failed collection in a batch does
     * not poison the surviving ones.
     *
     * @param collectionName the {@link SyncPatchType}; never {@code null}
     * @param version the collection version reported by the server
     * @param hasMore whether more patches are available
     * @param patches the decoded patches; may be {@code null} (treated as empty)
     * @param snapshotReference the snapshot reference; {@code null} for patch responses
     * @param collectionError the captured collection-level error, or {@code null} on success
     */
    public MutationSyncResponse(
            SyncPatchType collectionName,
            long version,
            boolean hasMore,
            SequencedCollection<SyncdPatch> patches,
            ExternalBlobReference snapshotReference,
            WhatsAppWebAppStateSyncException collectionError
    ) {
        this.collectionName = Objects.requireNonNull(collectionName);
        this.version = version;
        this.hasMore = hasMore;
        this.patches = patches;
        this.snapshotReference = snapshotReference;
        this.collectionError = collectionError;
    }

    /**
     * Returns whether this response carries a snapshot blob reference rather than patches.
     *
     * <p>This is a convenience predicate that decides between the snapshot-apply and
     * patch-apply branches without unwrapping the {@link #snapshotReference()} {@link Optional}.
     *
     * @return {@code true} when this response carries a snapshot reference
     */
    public boolean isSnapshot() {
        return snapshotReference != null;
    }

    /**
     * Returns the {@link SyncPatchType} this response applies to.
     *
     * @return the collection type
     */
    public SyncPatchType collectionName() {
        return collectionName;
    }

    /**
     * Returns the collection version reported by the server.
     *
     * <p>For a successful patch response this is the version after applying the contained
     * patches; for a snapshot response it is the version of the snapshot; for a captured
     * collection-level error it is {@code 0}.
     *
     * @return the version number
     */
    public long version() {
        return version;
    }

    /**
     * Returns whether the server signalled that more patches are available.
     *
     * <p>Maps directly from the {@code has_more_patches} attribute on the {@code <collection>}
     * stanza; the caller uses it to decide whether to issue a follow-up pull immediately.
     *
     * @return {@code true} when more patches remain
     */
    public boolean hasMore() {
        return hasMore;
    }

    /**
     * Returns the decoded patches in document order, wrapped to be unmodifiable.
     *
     * <p>Returns an empty collection when the response was a snapshot or a captured error.
     *
     * @implNote
     * This implementation re-wraps on every call rather than caching the unmodifiable wrapper
     * because the field may be {@code null}; the {@code null} branch returns {@link List#of()},
     * which is itself already unmodifiable.
     *
     * @return the patches; never {@code null}
     */
    public SequencedCollection<SyncdPatch> patches() {
        return patches == null ? List.of() : Collections.unmodifiableSequencedCollection(patches);
    }

    /**
     * Returns the decoded snapshot blob reference, if any.
     *
     * <p>Present only for snapshot responses; the caller streams the actual
     * {@code SyncdSnapshot} bytes from MMS through the
     * {@link MediaConnectionService}.
     *
     * @return the snapshot reference wrapped in an {@link Optional}
     */
    public Optional<ExternalBlobReference> snapshotReference() {
        return Optional.ofNullable(snapshotReference);
    }

    /**
     * Returns the collection-level error captured during parsing, if any.
     *
     * <p>The batched parser
     * ({@link MutationResponseParser#parseBatchedSyncResponse(Stanza)})
     * captures every collection-level error here; the single-collection parser
     * ({@link MutationResponseParser#parseSyncResponse(Stanza)})
     * throws fatal and retryable errors but surfaces a 409
     * {@link WhatsAppWebAppStateSyncException.Conflict} here alongside the catch-up
     * {@link #patches()}, so the caller can apply them and retry.
     *
     * @return the captured error wrapped in an {@link Optional}
     */
    public Optional<WhatsAppWebAppStateSyncException> collectionError() {
        return Optional.ofNullable(collectionError);
    }

    /**
     * Returns whether this response is field-equal to the supplied object.
     *
     * <p>Equality covers every field including the {@link #patches()} list and the captured
     * {@link #collectionError()} reference.
     *
     * @param o the object to compare against
     * @return {@code true} when {@code o} is a {@code MutationSyncResponse} with identical fields
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof MutationSyncResponse that
               && version == that.version
               && hasMore == that.hasMore
               && collectionName == that.collectionName
               && Objects.equals(patches, that.patches)
               && Objects.equals(snapshotReference, that.snapshotReference)
               && Objects.equals(collectionError, that.collectionError);
    }

    /**
     * Returns the field-combined hash that pairs with {@link #equals(Object)}.
     *
     * @return the {@link Objects#hash(Object...)} of every field
     */
    @Override
    public int hashCode() {
        return Objects.hash(collectionName, version, hasMore, patches, snapshotReference, collectionError);
    }

    /**
     * Returns a single-line bracketed dump of every field for diagnostic logs.
     *
     * <p>The format is unstable and not part of any contract; it is intended for log lines
     * only.
     *
     * @return the diagnostic representation
     */
    @Override
    public String toString() {
        return "MutationSyncResponse[" +
               "collectionName=" + collectionName + ", " +
               "version=" + version + ", " +
               "hasMore=" + hasMore + ", " +
               "patches=" + patches + ", " +
               "snapshotReference=" + snapshotReference + ", " +
               "collectionError=" + collectionError + ']';
    }
}
