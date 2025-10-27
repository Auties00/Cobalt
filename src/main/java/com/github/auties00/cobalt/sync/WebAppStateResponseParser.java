package com.github.auties00.cobalt.sync;

import com.github.auties00.cobalt.model.core.node.Node;
import com.github.auties00.cobalt.model.core.sync.SyncResponse;
import com.github.auties00.cobalt.model.proto.sync.*;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

public final class WebAppStateResponseParser {
    /**
     * Parses a sync response node.
     *
     * <p>Expected structure:
     * <pre>{@code
     * <iq type="result">
     *   <sync>
     *     <collection name="regular" version="45" has_more_patches="false">
     *       <patches>
     *         <patch version="43">...</patch>
     *       </patches>
     *       OR
     *       <snapshot version="45">...</snapshot>
     *     </collection>
     *   </sync>
     * </iq>
     * }</pre>
     *
     * @param responseNode the response node from the server
     * @return the parsed sync response
     * @throws IllegalArgumentException if the response structure is invalid
     */
    public SyncResponse parseSyncResponse(Node responseNode) {
        // Navigate to sync node
        var syncNode = responseNode.getChild("sync")
                .orElseThrow(() -> new IllegalArgumentException("Response missing 'sync' node"));

        // Navigate to collection node
        var collectionNode = syncNode.getChild("collection")
                .orElseThrow(() -> new IllegalArgumentException("Response missing 'collection' node"));

        // Extract collection metadata
        var collectionName = collectionNode.getAttributeAsString("name")
                .orElseThrow(() -> new IllegalArgumentException("Collection missing 'name' attribute"));
        var patchType = PatchType.of(collectionName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid collection name: " + collectionName));

        var version = collectionNode.getAttributeAsLong("version")
                .orElse(0L);

        var hasMore = collectionNode.getAttributeAsBool("has_more_patches", false);

        // Check if response contains snapshot or patches
        var snapshotNode = collectionNode.getChild("snapshot");
        var patchesNode = collectionNode.getChild("patches");

        if (snapshotNode.isPresent()) {
            // Parse snapshot
            var snapshot = parseSnapshot(snapshotNode.get());
            return new SyncResponse(patchType, version, hasMore, List.of(), snapshot);
        } else if (patchesNode.isPresent()) {
            // Parse patches
            var patches = parsePatches(patchesNode.get());
            return new SyncResponse(patchType, version, hasMore, patches, null);
        } else {
            // No updates available
            return new SyncResponse(patchType, version, false, List.of(), null);
        }
    }

    /**
     * Parses a snapshot node.
     *
     * @param snapshotNode the snapshot node
     * @return the parsed snapshot
     */
    private SnapshotSync parseSnapshot(Node snapshotNode) {
        // Get snapshot as bytes and decode
        var snapshotBytes = snapshotNode.toContentBytes()
                .orElseThrow(() -> new IllegalArgumentException("Snapshot node has no content"));

        try {
            return SnapshotSyncSpec.decode(snapshotBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode snapshot", e);
        }
    }

    /**
     * Parses patches from a patches container node.
     *
     * @param patchesNode the patches container node
     * @return list of parsed patches
     */
    private SequencedCollection<PatchSync> parsePatches(Node patchesNode) {
        var patches = new ArrayList<PatchSync>();

        // Find all patch child nodes
        var patchNodes = patchesNode.getChildren("patch");

        for (var patchNode : patchNodes) {
            var patchBytes = patchNode.toContentBytes()
                    .orElseThrow(() -> new IllegalArgumentException("Patch node has no content"));

            try {
                var patch = PatchSyncSpec.decode(patchBytes);
                patches.add(patch);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decode patch", e);
            }
        }

        return patches;
    }
}
