package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.model.sync.PatchSyncSpec;
import com.github.auties00.cobalt.model.sync.SnapshotSyncSpec;
import com.github.auties00.cobalt.model.sync.PatchSync;
import com.github.auties00.cobalt.model.sync.PatchType;
import com.github.auties00.cobalt.model.sync.SnapshotSync;
import com.github.auties00.cobalt.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.SequencedCollection;

public final class MutationResponseParser {
    public MutationSyncResponse parseSyncResponse(Node responseNode) {
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
            return new MutationSyncResponse(patchType, version, hasMore, List.of(), snapshot);
        } else if (patchesNode.isPresent()) {
            // Parse patches
            var patches = parsePatches(patchesNode.get());
            return new MutationSyncResponse(patchType, version, hasMore, patches, null);
        } else {
            // No updates available
            return new MutationSyncResponse(patchType, version, false, List.of(), null);
        }
    }

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
