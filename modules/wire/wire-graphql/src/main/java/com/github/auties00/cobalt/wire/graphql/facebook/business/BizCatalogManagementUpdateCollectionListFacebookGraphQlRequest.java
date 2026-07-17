package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the Facebook GraphQL mutation that reorders a WhatsApp Business catalog's collection list.
 *
 * <p>The single {@code input} GraphQL variable carries the owning {@code biz_jid} and a {@code move}
 * array describing the new ordering. WhatsApp Web's
 * {@code WAWebProductCollectionsJob.reorderCollectionGraphQL} fills it with
 * {@code {biz_jid, move: [{collection_id, from_index, to_index}]}}, one {@link Move} entry per
 * collection that changes position, each naming the collection's identifier and the source and
 * destination indices. The Meta graph endpoint returns the reorder outcome under
 * {@code xfb_whatsapp_catalog_update_collection_list}; the reply is consumed through
 * {@link BizCatalogManagementUpdateCollectionListFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementUpdateCollectionListFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateCollectionListMutation")
public final class BizCatalogManagementUpdateCollectionListFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCollectionListMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9930298893688430";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCollectionListMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementUpdateCollectionListMutation";

    /**
     * The {@code input.biz_jid} field naming the business account that owns the collection list, or
     * {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code input.move} entries describing the new collection ordering, or {@code null} to omit
     * the {@code move} array.
     */
    private final List<Move> moves;

    /**
     * Constructs an update-collection-list mutation request carrying the owning business account and
     * the list of reorder moves.
     *
     * <p>Both values populate the {@code input} GraphQL object; a {@code null} {@code bizJid} omits the
     * {@code biz_jid} field, a {@code null} {@code moves} omits the {@code move} array, and an empty
     * {@code moves} serializes as an empty array.
     *
     * @param bizJid the business account {@link Jid} that owns the collection list, or {@code null} to
     *               omit the field
     * @param moves  the reorder moves to apply, or {@code null} to omit the {@code move} array
     */
    public BizCatalogManagementUpdateCollectionListFacebookGraphQlRequest(Jid bizJid, List<Move> moves) {
        this.bizJid = bizJid;
        this.moves = moves;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {"biz_jid": <bizJid>, "move": [{"collection_id":
     * ..., "from_index": ..., "to_index": ...}, ...]}}}, writing the {@code biz_jid} field only when it
     * is non-null and the {@code move} array only when {@code moves} is non-null; an empty {@code input}
     * object is emitted when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebProductCollectionsJob", exports = "reorderCollectionGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            if (moves != null) {
                writer.writeName("move");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < moves.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    moves.get(i).write(writer);
                }
                writer.endArray();
            }
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Describes a single reposition within the {@code input.move} array.
     *
     * <p>Each move names the {@code collection_id} being repositioned and the {@code from_index} and
     * {@code to_index} bracketing the slide; WhatsApp Web derives all three from the source ordering
     * tuple supplied to {@code reorderCollectionGraphQL}.
     */
    public static final class Move {
        /**
         * The {@code collection_id} of the collection being repositioned.
         *
         * <p>This is an opaque catalog collection identifier rather than a WhatsApp address, so it is
         * kept as a plain {@link String}.
         */
        private final String collectionId;

        /**
         * The {@code from_index} the collection currently occupies.
         */
        private final int fromIndex;

        /**
         * The {@code to_index} the collection moves to.
         */
        private final int toIndex;

        /**
         * Constructs a move entry from the collection identifier and the source and destination
         * indices.
         *
         * @param collectionId the collection identifier being repositioned
         * @param fromIndex    the current index of the collection
         * @param toIndex      the target index of the collection
         */
        public Move(String collectionId, int fromIndex, int toIndex) {
            this.collectionId = collectionId;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        /**
         * Returns the collection identifier being repositioned.
         *
         * @return the collection identifier, may be {@code null}
         */
        public String collectionId() {
            return collectionId;
        }

        /**
         * Returns the current index of the collection.
         *
         * @return the source index
         */
        public int fromIndex() {
            return fromIndex;
        }

        /**
         * Returns the target index of the collection.
         *
         * @return the destination index
         */
        public int toIndex() {
            return toIndex;
        }

        /**
         * Writes this move as a {@code {"collection_id": ..., "from_index": ..., "to_index": ...}}
         * object onto the given JSON writer.
         *
         * <p>The {@code collection_id} field is written only when it is non-null; the two indices are
         * always written.
         *
         * @param writer the JSON writer to append to
         */
        private void write(JSONWriter writer) {
            writer.startObject();
            if (collectionId != null) {
                writer.writeName("collection_id");
                writer.writeColon();
                writer.writeString(collectionId);
            }

            writer.writeName("from_index");
            writer.writeColon();
            writer.writeInt32(fromIndex);
            writer.writeName("to_index");
            writer.writeColon();
            writer.writeInt32(toIndex);
            writer.endObject();
        }
    }
}
