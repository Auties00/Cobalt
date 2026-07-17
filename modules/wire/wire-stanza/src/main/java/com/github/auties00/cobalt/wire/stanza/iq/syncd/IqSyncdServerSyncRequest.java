package com.github.auties00.cobalt.wire.stanza.iq.syncd;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;

/**
 * Outbound {@code <iq xmlns="w:sync:app:state" type="set">} stanza that drives the
 * syncd app-state two-way sync loop for a typed list of collection entries.
 *
 * <p>Each iteration of the sync loop uploads any pending local mutations as
 * encrypted patches and asks the relay for any patches or snapshot it has buffered
 * for the bound collections. A fresh request is built per iteration; the relay's
 * per-collection {@link IqSyncdServerSyncCollectionState state} determines whether
 * a follow-up iteration is needed. The reply is parsed by
 * {@link IqSyncdServerSyncResponse}.
 *
 * @implNote
 * The outbound payload is a single {@code <sync>} child carrying one
 * {@code <collection name return_snapshot version>} per entry, with an optional
 * {@code <patch>} grandchild when the entry ships encrypted local mutations. The
 * {@code return_snapshot} attribute is {@code "true"} when the entry has no local
 * version (initial bootstrap) and {@code "false"} otherwise; the relay uses it to
 * decide whether to ship a snapshot blob or only patches.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
@WhatsAppWebModule(moduleName = "WAWebSyncdRequestBuilderBuild")
public final class IqSyncdServerSyncRequest implements IqStanza.Request {
    /**
     * Holds the default collection version emitted in the {@code version} attribute
     * when the caller has never synced a collection.
     *
     * <p>The relay interprets a zero version paired with
     * {@code return_snapshot="true"} as an initial-bootstrap request.
     */
    @WhatsAppWebExport(moduleName = "WASyncdConst",
            exports = "DEFAULT_COLLECTION_VERSION", adaptation = WhatsAppAdaptation.DIRECT)
    public static final long DEFAULT_COLLECTION_VERSION = 0L;

    /**
     * Holds the {@link IqSyncdServerSyncRequestCollection} entries to sync in this
     * iteration.
     */
    private final List<IqSyncdServerSyncRequestCollection> collections;

    /**
     * Constructs a new server-sync request bound to the given collection entries.
     *
     * <p>An empty list produces a degenerate {@code <sync/>} child with no
     * collection grandchildren, which the relay treats as a no-op iteration that
     * returns an empty reply; callers normally pass at least one entry.
     *
     * @param collections the collection entries; never {@code null}, may be empty
     * @throws NullPointerException if {@code collections} is {@code null}
     */
    public IqSyncdServerSyncRequest(List<IqSyncdServerSyncRequestCollection> collections) {
        Objects.requireNonNull(collections, "collections cannot be null");
        this.collections = collections;
    }

    /**
     * Returns the bound list of collection entries.
     *
     * @return an unmodifiable view of the entries; never {@code null}, possibly
     *         empty
     */
    public SequencedCollection<IqSyncdServerSyncRequestCollection> collections() {
        return Collections.unmodifiableSequencedCollection(collections);
    }

    /**
     * Builds the outbound {@code <iq>} stanza from the typed collection list.
     *
     * <p>The returned {@link StanzaBuilder} is wire-ready except for the IQ
     * {@code id} attribute, which the dispatch layer assigns. The encrypted patch
     * bytes carried in {@link IqSyncdServerSyncRequestCollection#patch()} are routed
     * verbatim into the optional {@code <patch>} grandchild.
     *
     * @implNote
     * This implementation sets {@code return_snapshot="true"} when the local
     * version is empty and {@code "false"} otherwise, falling back to
     * {@link #DEFAULT_COLLECTION_VERSION} for the {@code version} attribute when no
     * local version is present.
     *
     * @return a {@link StanzaBuilder} carrying the IQ envelope and the typed
     *         {@code <sync>}/{@code <collection>} payload
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebSyncdRequestBuilderBuild",
            exports = "buildSyncIqNode", adaptation = WhatsAppAdaptation.ADAPTED)
    public StanzaBuilder toStanza() {
        var collectionNodes = new ArrayList<Stanza>(collections.size());
        for (var collection : collections) {
            var collectionBuilder = new StanzaBuilder()
                    .description("collection")
                    .attribute("name", collection.name())
                    .attribute("return_snapshot", collection.version().isEmpty() ? "true" : "false")
                    .attribute("version", collection.version().orElse(DEFAULT_COLLECTION_VERSION));
            collection.patch().ifPresent(patch -> {
                var patchNode = new StanzaBuilder()
                        .description("patch")
                        .content(patch)
                        .build();
                collectionBuilder.content(patchNode);
            });
            collectionNodes.add(collectionBuilder.build());
        }
        var syncNode = new StanzaBuilder()
                .description("sync")
                .content(collectionNodes)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:sync:app:state")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(syncNode);
    }

    /**
     * Compares this request to another for equality across the bound collection
     * entries.
     *
     * @param obj the object to compare against, or {@code null}
     * @return {@code true} when {@code obj} is an {@link IqSyncdServerSyncRequest}
     *         with an equal collection list
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSyncdServerSyncRequest) obj;
        return Objects.equals(this.collections, that.collections);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)} over the bound
     * collection entries.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(collections);
    }

    /**
     * Returns a debugging representation listing the bound collection entries.
     *
     * @return a string of the form
     *         {@code IqSyncdServerSyncRequest[collections=...]}
     */
    @Override
    public String toString() {
        return "IqSyncdServerSyncRequest[collections=" + collections + ']';
    }
}
