package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.exception.linked.web.WhatsAppWebAppStateSyncException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReference;
import com.github.auties00.cobalt.wire.linked.media.ExternalBlobReferenceSpec;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatch;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdPatchSpec;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.SequencedCollection;

/**
 * Parses incoming {@code <iq xmlns="w:sync:app:state">} response stanzas into
 * {@link MutationSyncResponse} records.
 *
 * <p>Two parse modes share the bulk of the body. The single-collection mode
 * ({@link #parseSyncResponse(Stanza)}) raises on a fatal or retryable collection-level error so a
 * failed push can roll back, but returns a 409 conflict on the response (with its catch-up patches)
 * so the push can apply them and retry; the batched mode ({@link #parseBatchedSyncResponse(Stanza)})
 * captures every collection-level error on its response record so the caller can process the
 * surviving collections. Both
 * share the same IQ-level error router ({@link #handleIqLevelError(int, String, Stanza)}) and
 * the same protobuf decoders for {@code <patch>} and {@code <snapshot>} children. The parser
 * is driven directly with the raw {@code <iq>} {@link Stanza} delivered by the WhatsApp socket;
 * no embedder integration is required.
 *
 * @implNote
 * This implementation skips the WAM/telemetry side effects that WA Web fires from
 * {@code reportSyncdFatalError} per Cobalt's pluggable error model: the routed
 * {@link WhatsAppWebAppStateSyncException} subtypes carry the same classification, and the
 * caller decides whether to log or beacon them via the configured error handler.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdResponseParser")
@WhatsAppWebModule(moduleName = "WAWebSyncdDecode")
@WhatsAppWebModule(moduleName = "WAWebSyncdValidateServerSyncProtobuf")
public final class MutationResponseParser {
    /**
     * The logger for {@link MutationResponseParser}.
     */
    private static final System.Logger LOGGER = Log.get(MutationResponseParser.class);

    /**
     * Parses a single-collection sync response.
     *
     * <p>Used on the push response path where exactly one collection is expected. A fatal or
     * retryable collection-level error propagates as a thrown exception so the push can roll back;
     * a 409 conflict instead returns normally with the error surfaced on
     * {@link MutationSyncResponse#collectionError()} and the catch-up patches populated, so the
     * caller can apply them and retry rather than discard them. IQ-level errors are routed first
     * via {@link #handleIqLevelError(int, String, Stanza)}.
     * Use {@link #parseBatchedSyncResponse(Stanza)} for pull responses that legitimately carry
     * multiple collections.
     *
     * @implNote
     * This implementation parses the collection name before the error gate so the
     * unknown-name path raises the same {@link WhatsAppWebAppStateSyncException.UnexpectedError}
     * regardless of whether the response would otherwise have been an error.
     *
     * @param responseStanza the raw IQ response stanza from the server
     * @return the parsed {@link MutationSyncResponse}; a 409 conflict carries its
     *         {@link WhatsAppWebAppStateSyncException.Conflict} on
     *         {@link MutationSyncResponse#collectionError()} alongside the catch-up patches
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when the server returns a fatal IQ or collection error
     * @throws WhatsAppWebAppStateSyncException.RetryableServerError when the server returns a retryable error
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdResponseParser", exports = "syncResponseParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public MutationSyncResponse parseSyncResponse(Stanza responseStanza) {
        var iqType = responseStanza.getAttributeAsString("type");
        if (iqType.isPresent() && iqType.get().equals("error")) {
            var iqErrorNode = responseStanza.getChild("error").orElse(null);
            var errorCode = iqErrorNode != null
                    ? iqErrorNode.getAttributeAsString("code").map(Integer::parseInt).orElse(0)
                    : 0;
            var errorText = iqErrorNode != null
                    ? iqErrorNode.getAttributeAsString("text").orElse("unknown")
                    : "unknown";
            handleIqLevelError(errorCode, errorText, iqErrorNode);
        }

        var syncNode = responseStanza.getChild("sync")
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Response missing 'sync' stanza",
                        null
                ));

        var collectionNode = syncNode.getChild("collection")
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Response missing 'collection' stanza",
                        null
                ));

        var collectionName = collectionNode.getAttributeAsString("name")
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Collection missing 'name' attribute",
                        null
                ));
        var patchType = SyncPatchType.of(collectionName)
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Invalid collection name: " + collectionName,
                        null
                ));

        WhatsAppWebAppStateSyncException collectionError = null;
        var type = collectionNode.getAttributeAsString("type");
        if (type.isPresent() && type.get().equals("error")) {
            collectionError = buildCollectionError(collectionNode);
            if (!(collectionError instanceof WhatsAppWebAppStateSyncException.Conflict)) {
                throw collectionError;
            }
        }

        var version = collectionNode.getAttributeAsLong("version")
                .orElse(0L);

        var hasMore = collectionNode.hasAttribute("has_more_patches");

        var snapshotNode = collectionNode.getChild("snapshot");
        var patchesNode = collectionNode.getChild("patches");

        var snapshotRef = snapshotNode.map(this::parseSnapshotReference).orElse(null);
        var patches = patchesNode.map(this::parsePatches).orElse(List.of());
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed sync response for {0}: version={1}, hasMore={2}, patches={3}, snapshot={4}",
                    patchType, version, hasMore, patches.size(), snapshotRef != null);
        }
        return new MutationSyncResponse(patchType, version, hasMore, patches, snapshotRef, collectionError);
    }

    /**
     * Parses a multi-collection sync response.
     *
     * <p>Used on the pull response path where one IQ may carry several {@code <collection>}
     * children, one per dirty collection. Per-collection errors are captured on the response
     * object via {@link #parseCollectionNode(Stanza)} so the caller can apply the successful
     * collections and retry only the failed ones; IQ-level errors still throw.
     *
     * @implNote
     * This implementation iterates the {@code <collection>} children in document order, and the
     * resulting list preserves that order so callers can correlate it positionally with their
     * push input.
     *
     * @param responseStanza the raw IQ response stanza from the server
     * @return the per-collection {@link MutationSyncResponse} list
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when an IQ-level fatal error fires
     * @throws WhatsAppWebAppStateSyncException.RetryableServerError when an IQ-level retryable error fires
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdResponseParser", exports = "syncResponseParser", adaptation = WhatsAppAdaptation.DIRECT)
    public List<MutationSyncResponse> parseBatchedSyncResponse(Stanza responseStanza) {
        var iqType = responseStanza.getAttributeAsString("type");
        if (iqType.isPresent() && iqType.get().equals("error")) {
            var iqErrorNode = responseStanza.getChild("error").orElse(null);
            var errorCode = iqErrorNode != null
                    ? iqErrorNode.getAttributeAsString("code").map(Integer::parseInt).orElse(0)
                    : 0;
            var errorText = iqErrorNode != null
                    ? iqErrorNode.getAttributeAsString("text").orElse("unknown")
                    : "unknown";
            handleIqLevelError(errorCode, errorText, iqErrorNode);
        }

        var syncNode = responseStanza.getChild("sync")
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Response missing 'sync' stanza",
                        null
                ));

        var collectionNodes = syncNode.getChildren("collection");
        var results = new ArrayList<MutationSyncResponse>(collectionNodes.size());
        for (var collectionNode : collectionNodes) {
            results.add(parseCollectionNode(collectionNode));
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsed {0} collections from batched sync response", results.size());
        return results;
    }

    /**
     * Parses a single {@code <collection>} child into a {@link MutationSyncResponse}, capturing
     * any collection-level error on the response rather than throwing.
     *
     * <p>Used by {@link #parseBatchedSyncResponse(Stanza)} so the failure of one collection in a
     * batch does not poison the rest. A collection in error state yields a response carrying the
     * exception via {@link MutationSyncResponse#collectionError()}; an otherwise valid collection
     * yields its decoded patches or snapshot reference.
     *
     * @implNote
     * This implementation reuses {@link #buildCollectionError(Stanza)} so the collection-level
     * routing (409 to {@link WhatsAppWebAppStateSyncException.Conflict}, 400/404 to
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError}, anything else to
     * {@link WhatsAppWebAppStateSyncException.RetryableServerError}) stays consistent between the
     * throwing and capturing call paths.
     *
     * @param collectionStanza the collection stanza to parse
     * @return the parsed {@link MutationSyncResponse}; collection-level errors are surfaced via
     *         {@link MutationSyncResponse#collectionError()}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdResponseParser", exports = "syncResponseParser", adaptation = WhatsAppAdaptation.DIRECT)
    private MutationSyncResponse parseCollectionNode(Stanza collectionStanza) {
        var collectionName = collectionStanza.getAttributeAsString("name")
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Collection missing 'name' attribute",
                        null
                ));
        var patchType = SyncPatchType.of(collectionName)
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Invalid collection name: " + collectionName,
                        null
                ));

        var version = collectionStanza.getAttributeAsLong("version")
                .orElse(0L);
        var hasMore = collectionStanza.hasAttribute("has_more_patches");

        var snapshotNode = collectionStanza.getChild("snapshot");
        var patchesNode = collectionStanza.getChild("patches");

        var snapshotRef = snapshotNode.map(this::parseSnapshotReference).orElse(null);
        var patches = patchesNode.map(this::parsePatches).orElse(List.of());

        var type = collectionStanza.getAttributeAsString("type");
        WhatsAppWebAppStateSyncException collectionError = null;
        if (type.isPresent() && type.get().equals("error")) {
            collectionError = buildCollectionError(collectionStanza);
        }
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed collection {0}: version={1}, hasMore={2}, patches={3}, snapshot={4}, error={5}",
                    patchType, version, hasMore, patches.size(), snapshotRef != null, collectionError != null);
        }
        return new MutationSyncResponse(patchType, version, hasMore, patches, snapshotRef, collectionError);
    }

    /**
     * Builds the {@link WhatsAppWebAppStateSyncException} subtype that corresponds to a given
     * collection-level error code.
     *
     * <p>The routing table is fixed: 409 maps to
     * {@link WhatsAppWebAppStateSyncException.Conflict}, 400 and 404 to
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError}, and any other code (including a
     * missing code attribute) to {@link WhatsAppWebAppStateSyncException.RetryableServerError}.
     *
     * @param collectionStanza the collection stanza carrying the error
     * @return the exception that should be reported for this collection
     */
    private WhatsAppWebAppStateSyncException buildCollectionError(Stanza collectionStanza) {
        var errorNode = collectionStanza.getChild("error");
        var errorCode = errorNode
                .flatMap(e -> e.getAttributeAsString("code"))
                .orElse("unknown");

        if (Log.WARNING) LOGGER.log(Level.WARNING, "collection-level sync error code {0}", errorCode);

        return switch (errorCode) {
            case "409" -> new WhatsAppWebAppStateSyncException.Conflict(
                    collectionStanza.hasAttribute("has_more_patches")
            );
            case "400", "404" -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "Server returned fatal error code: " + errorCode, null
            );
            default -> new WhatsAppWebAppStateSyncException.RetryableServerError(errorCode);
        };
    }


    /**
     * Routes an IQ-level error code to the appropriate {@link WhatsAppWebAppStateSyncException}
     * subtype and throws it.
     *
     * <p>IQ-level errors affect every collection in the request and short-circuit the parse:
     * {@code 400}, {@code 404}, {@code 405} and {@code 406} are fatal; everything else is
     * retryable, with the optional {@code backoff} attribute preserved on the
     * {@link WhatsAppWebAppStateSyncException.RetryableServerError}.
     *
     * @param errorCode the IQ error code
     * @param errorText the IQ error text
     * @param errorStanza the {@code <error>} stanza, possibly {@code null}
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError for fatal IQ codes
     * @throws WhatsAppWebAppStateSyncException.RetryableServerError for any other IQ code
     */
    private void handleIqLevelError(int errorCode, String errorText, Stanza errorStanza) {
        switch (errorCode) {
            case 400, 404, 405, 406 -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "iq-level fatal sync error {0}: {1}", errorCode, errorText);
                throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "IQ-level fatal error " + errorCode + ": " + errorText, null);
            }
            default -> {
                var serverBackoffMs = errorStanza != null
                        ? errorStanza.getAttributeAsLong("backoff", null)
                        : null;
                if (Log.WARNING) LOGGER.log(Level.WARNING, "iq-level retryable sync error {0}: {1}, backoff={2}", errorCode, errorText, serverBackoffMs);
                throw new WhatsAppWebAppStateSyncException.RetryableServerError(
                        String.valueOf(errorCode), serverBackoffMs);
            }
        }
    }

    /**
     * Decodes the {@code <snapshot>} stanza content into an {@link ExternalBlobReference} the
     * caller can later download from MMS.
     *
     * <p>Snapshots are too large to ship inline; the server returns an external blob reference
     * and the caller streams the actual {@code SyncdSnapshot} bytes from MMS via the media
     * connection. A bare snapshot child without content, or content that fails to deserialize,
     * is treated as a fatal error.
     *
     * @implNote
     * This implementation collapses WA Web's separate
     * {@code reportSyncdFatalError(EXTERNAL_BLOB_REFERENCE_PROTOBUF_DESERIALIZATION_FAILED)} WAM
     * beacon plus {@code WALogger.ERROR} into a single thrown
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError}, in line with Cobalt's pluggable
     * error model.
     *
     * @param snapshotStanza the {@code <snapshot>} stanza
     * @return the parsed {@link ExternalBlobReference}
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when the stanza has no content or the protobuf fails to deserialize
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecode", exports = "decodeExternalBlobReference", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdValidateServerSyncProtobuf", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private ExternalBlobReference parseSnapshotReference(Stanza snapshotStanza) {
        var snapshotBytes = snapshotStanza.toContentBytes()
                .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "Snapshot stanza has no content",
                        null
                ));

        try {
            return ExternalBlobReferenceSpec.decode(snapshotBytes);
        } catch (Exception e) {
            if (Log.ERROR) LOGGER.log(Level.ERROR, "external blob reference protobuf deserialization failed", e);
            throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                    "syncd: external blob reference protobuf deserialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decodes every {@code <patch>} child of the supplied {@code <patches>} stanza into a
     * {@link SyncdPatch}, logging each patch's debug data at {@link Level#DEBUG DEBUG}.
     *
     * <p>Used by both parse modes. Returns an empty collection when the {@code <patches>} parent
     * has no children; a missing patch content or undecodable patch protobuf is fatal.
     *
     * @implNote
     * This implementation collapses WA Web's separate
     * {@code reportSyncdFatalError(PATCH_PROTOBUF_DESERIALIZATION_FAILED)} WAM beacon plus
     * {@code WALogger.ERROR} into a single thrown
     * {@link WhatsAppWebAppStateSyncException.UnexpectedError}; the per-patch
     * {@link #logClientDebugData(SyncdPatch)} call surfaces the same diagnostic data WA Web's
     * {@code _applyPatch} prints.
     *
     * @param patchesStanza the parent {@code <patches>} stanza
     * @return the parsed patches in document order
     * @throws WhatsAppWebAppStateSyncException.UnexpectedError when a patch has no content or fails to deserialize
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdDecode", exports = "decodeSyncdPatch", adaptation = WhatsAppAdaptation.ADAPTED)
    @WhatsAppWebExport(moduleName = "WAWebSyncdValidateServerSyncProtobuf", exports = "default", adaptation = WhatsAppAdaptation.ADAPTED)
    private SequencedCollection<SyncdPatch> parsePatches(Stanza patchesStanza) {
        var patches = new ArrayList<SyncdPatch>();

        var patchNodes = patchesStanza.getChildren("patch");

        for (var patchNode : patchNodes) {
            var patchBytes = patchNode.toContentBytes()
                    .orElseThrow(() -> new WhatsAppWebAppStateSyncException.UnexpectedError(
                            "Patch stanza has no content",
                            null
                    ));

            try {
                var patch = SyncdPatchSpec.decode(patchBytes);
                logClientDebugData(patch);
                patches.add(patch);
            } catch (Exception e) {
                if (Log.ERROR) LOGGER.log(Level.ERROR, "patch protobuf deserialization failed", e);
                throw new WhatsAppWebAppStateSyncException.UnexpectedError(
                        "syncd: patch protobuf deserialization failed: " + e.getMessage(), e);
            }
        }

        return patches;
    }

    /**
     * Logs the decoded {@code currentLthash} and {@code newLthash} of the supplied patch at
     * {@link Level#DEBUG DEBUG} for diagnostic cross-checking.
     *
     * <p>Lets a developer correlate the server's reported LT-hash transition against Cobalt's
     * locally computed {@link com.github.auties00.cobalt.sync.crypto.MutationLTHash} state when
     * chasing a desync.
     *
     * @implNote
     * This implementation is a no-op when {@link Log#DEBUG} is disabled. Decoding the
     * debug data itself is best-effort and does not throw.
     *
     * @param patch the patch whose debug data should be logged; never {@code null}
     */
    private void logClientDebugData(SyncdPatch patch) {
        if (!Log.DEBUG) {
            return;
        }
        patch.decodedClientDebugData().ifPresent(debug -> {
            var hex = HexFormat.of();
            var current = debug.currentLthash().map(hex::formatHex).orElse("<none>");
            var next = debug.newLthash().map(hex::formatHex).orElse("<none>");
            LOGGER.log(Level.DEBUG, "patch debug: currentLthash={0}, newLthash={1}", current, next);
        });
    }
}
