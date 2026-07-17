package com.github.auties00.cobalt.wire.linked.business.catalog;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A single reposition within a WhatsApp Business catalog's collection list.
 *
 * <p>When the merchant drags a collection from one slot to another in the
 * catalog editor, the resulting reorder operation is described as a list of
 * these moves: each entry pairs the affected collection's identifier with
 * the source index it currently occupies and the destination index it slides
 * to. The catalog editor batches every changed collection into a single
 * reorder request, one {@code BusinessCatalogCollectionMove} per
 * collection that actually shifted position.
 */
@ProtobufMessage(name = "BusinessCatalogCollectionMove")
public final class BusinessCatalogCollectionMove {
    /**
     * The server-issued identifier of the collection being repositioned.
     * Always populated.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String collectionId;

    /**
     * The index the collection currently occupies in the catalog's
     * collection list.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.INT32)
    int fromIndex;

    /**
     * The index the collection moves to in the catalog's collection list.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.INT32)
    int toIndex;

    /**
     * Constructs a new {@code BusinessCatalogCollectionMove} from the
     * collection identifier and the source and destination indices.
     *
     * @param collectionId the catalog collection identifier; never {@code null}
     * @param fromIndex    the current index of the collection
     * @param toIndex      the target index of the collection
     * @throws NullPointerException if {@code collectionId} is {@code null}
     */
    BusinessCatalogCollectionMove(String collectionId, int fromIndex, int toIndex) {
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId cannot be null");
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * Returns the server-issued identifier of the collection being
     * repositioned.
     *
     * @return the collection identifier; never {@code null}
     */
    public String collectionId() {
        return collectionId;
    }

    /**
     * Returns the index the collection currently occupies.
     *
     * @return the source index
     */
    public int fromIndex() {
        return fromIndex;
    }

    /**
     * Returns the index the collection moves to.
     *
     * @return the destination index
     */
    public int toIndex() {
        return toIndex;
    }

    /**
     * Sets the server-issued identifier of the collection being repositioned.
     *
     * @param collectionId the collection identifier to set
     * @throws NullPointerException if {@code collectionId} is {@code null}
     */
    public void setCollectionId(String collectionId) {
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId cannot be null");
    }

    /**
     * Sets the current index of the collection.
     *
     * @param fromIndex the source index to set
     */
    public void setFromIndex(int fromIndex) {
        this.fromIndex = fromIndex;
    }

    /**
     * Sets the target index of the collection.
     *
     * @param toIndex the destination index to set
     */
    public void setToIndex(int toIndex) {
        this.toIndex = toIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogCollectionMove) obj;
        return Objects.equals(this.collectionId, that.collectionId)
                && this.fromIndex == that.fromIndex
                && this.toIndex == that.toIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionId, fromIndex, toIndex);
    }

    @Override
    public String toString() {
        return "BusinessCatalogCollectionMove[" +
                "collectionId=" + collectionId + ", " +
                "fromIndex=" + fromIndex + ", " +
                "toIndex=" + toIndex + ']';
    }
}
