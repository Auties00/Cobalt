package com.github.auties00.cobalt.sync.exchange;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionValue;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEntry;
import com.github.auties00.cobalt.wire.linked.sync.data.SyncdOperation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.List;

/**
 * Carries the output of {@link MutationRequestBuilder#buildSyncRequest(SyncPatchType, java.util.SequencedCollection)}.
 *
 * <p>The {@link #node} is the IQ {@link StanzaBuilder} ready to be id-tagged and sent, and is
 * left mutable so the send path can attach the auto-generated stanza id before serialising.
 * The {@link #uploadInfo} is the metadata that the post-response success path consumes once
 * the server ACKs the push; it is {@code null} when no mutations were emitted (an
 * empty-patches build), in which case there is nothing for the success path to apply.
 *
 * @param node the IQ {@link StanzaBuilder} carrying {@code <iq type="set" xmlns="w:sync:app:state">}
 * @param uploadInfo the post-response success metadata, or {@code null} when no mutations were emitted
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
public record SyncRequest(
        StanzaBuilder node,
        UploadedPatchInfo uploadInfo
) {
    /**
     * Captures the per-collection upload metadata produced during patch encoding.
     *
     * <p>After the server ACKs an outgoing patch the success path persists the new sync
     * action entries built from {@link #mutations}, advances the collection version to
     * {@link #newVersion} and the LT-Hash to {@link #newLtHash}, and clears every pending
     * mutation id listed in {@link #uploadedPendingMutationIds}.
     *
     * @implNote
     * This implementation captures {@link #uploadedPendingMutationIds} before compaction so
     * the success path clears every original pending mutation, not just the deduplicated
     * subset that actually rode the wire.
     *
     * @param patchType the collection type
     * @param newLtHash the LT-Hash computed after applying the outgoing mutations
     * @param newVersion the expected new collection version (local version plus one)
     * @param uploadedPendingMutationIds the ids of every pending mutation (pre-compaction) included in the upload
     * @param mutations the per-mutation metadata used for sync action entry persistence
     */
    public record UploadedPatchInfo(
            SyncPatchType patchType,
            byte[] newLtHash,
            long newVersion,
            List<String> uploadedPendingMutationIds,
            List<UploadedMutationInfo> mutations
    ) {
    }

    /**
     * Captures the per-mutation metadata produced during patch encoding.
     *
     * <p>Pairs the encrypted output ({@link #indexMac}, {@link #valueMac}, {@link #keyId},
     * {@link #operation}) with the plaintext source data ({@link #actionIndex},
     * {@link #actionValue}, {@link #actionVersion}) so the success path can persist a matching
     * {@link SyncActionEntry} without re-decrypting the
     * patch.
     *
     * @param indexMac the HMAC of the mutation index, also used as the entry's primary key
     * @param valueMac the HMAC of the encrypted value, used as the LT-Hash add/remove input
     * @param keyId the encrypting sync key id (latest active for SET, original for REMOVE)
     * @param operation the {@link SyncdOperation}
     * @param actionIndex the plaintext index string
     * @param actionValue the decoded {@link SyncActionValue}
     * @param actionVersion the action version number
     */
    public record UploadedMutationInfo(
            byte[] indexMac,
            byte[] valueMac,
            byte[] keyId,
            SyncdOperation operation,
            String actionIndex,
            SyncActionValue actionValue,
            int actionVersion
    ) {
    }
}
