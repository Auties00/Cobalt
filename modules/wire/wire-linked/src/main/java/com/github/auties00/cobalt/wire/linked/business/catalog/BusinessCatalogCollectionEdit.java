package com.github.auties00.cobalt.wire.linked.business.catalog;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidProvider;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Input model for editing one existing collection inside a WhatsApp Business
 * catalog.
 *
 * <p>An edit identifies the target collection, optionally renames it, and
 * adds or removes products. Each list defaults to {@link List#of()} when
 * unset, so a caller that only renames the collection leaves both add and
 * remove lists empty; a caller that only adjusts membership leaves
 * {@link #name()} unset. The catalog session id is an opaque correlator
 * the WhatsApp Business app uses to group consecutive catalog edits.
 */
@ProtobufMessage(name = "BusinessCatalogCollectionEdit")
public final class BusinessCatalogCollectionEdit {
    /**
     * Server-assigned identifier of the collection being edited.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String collectionId;

    /**
     * Business account that owns the catalog the collection lives in.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid businessJid;

    /**
     * Opaque session correlator grouping consecutive catalog edits. Unset
     * starts a fresh, uncorrelated edit.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String catalogSessionId;

    /**
     * New display name. Unset leaves the collection's existing name
     * unchanged.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String name;

    /**
     * Identifiers of products to add to the collection. Defaults to
     * {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final List<String> productsToAdd;

    /**
     * Identifiers of products to remove from the collection. Defaults to
     * {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final List<String> productsToRemove;

    /**
     * Constructs a new {@code BusinessCatalogCollectionEdit}.
     *
     * @param collectionId     the identifier of the collection to edit; required
     * @param businessJid      the business account that owns the catalog; required
     * @param catalogSessionId the optional session correlator, or {@code null}
     * @param name             the new display name, or {@code null} to leave unchanged
     * @param productsToAdd    the products to add; never {@code null}, defaults to {@link List#of()}
     * @param productsToRemove the products to remove; never {@code null}, defaults to {@link List#of()}
     * @throws NullPointerException if {@code collectionId}, {@code businessJid},
     *                              {@code productsToAdd}, or
     *                              {@code productsToRemove} is {@code null}
     */
    public BusinessCatalogCollectionEdit(String collectionId, Jid businessJid, String catalogSessionId,
                                         String name, List<String> productsToAdd,
                                         List<String> productsToRemove) {
        this.collectionId = Objects.requireNonNull(collectionId, "collectionId cannot be null");
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.catalogSessionId = catalogSessionId;
        this.name = name;
        this.productsToAdd = Objects.requireNonNull(productsToAdd, "productsToAdd cannot be null");
        this.productsToRemove = Objects.requireNonNull(productsToRemove, "productsToRemove cannot be null");
    }

    /**
     * Convenience constructor that accepts any {@link JidProvider} and
     * resolves it to a {@link Jid}.
     *
     * @param collectionId     the identifier of the collection to edit; required
     * @param businessJid      the business account that owns the catalog; required
     * @param catalogSessionId the optional session correlator, or {@code null}
     * @param name             the new display name, or {@code null}
     * @param productsToAdd    the products to add; never {@code null}
     * @param productsToRemove the products to remove; never {@code null}
     * @throws NullPointerException if {@code collectionId}, {@code businessJid},
     *                              {@code productsToAdd}, or
     *                              {@code productsToRemove} is {@code null}
     */
    public BusinessCatalogCollectionEdit(String collectionId, JidProvider businessJid,
                                         String catalogSessionId, String name,
                                         List<String> productsToAdd,
                                         List<String> productsToRemove) {
        this(collectionId,
                Objects.requireNonNull(businessJid, "businessJid cannot be null").toJid(),
                catalogSessionId, name, productsToAdd, productsToRemove);
    }

    /**
     * Returns the identifier of the collection to edit.
     *
     * @return the collection id, never {@code null}
     */
    public String collectionId() {
        return collectionId;
    }

    /**
     * Returns the business account that owns the catalog.
     *
     * @return the business JID, never {@code null}
     */
    public Jid businessJid() {
        return businessJid;
    }

    /**
     * Returns the opaque session correlator.
     *
     * @return an {@link Optional} carrying the session id, or empty when unset
     */
    public Optional<String> catalogSessionId() {
        return Optional.ofNullable(catalogSessionId);
    }

    /**
     * Returns the new display name.
     *
     * @return an {@link Optional} carrying the new name, or empty when unset
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the product ids to add.
     *
     * @return the products to add, never {@code null}
     */
    public List<String> productsToAdd() {
        return productsToAdd;
    }

    /**
     * Returns the product ids to remove.
     *
     * @return the products to remove, never {@code null}
     */
    public List<String> productsToRemove() {
        return productsToRemove;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogCollectionEdit) obj;
        return Objects.equals(collectionId, that.collectionId)
                && Objects.equals(businessJid, that.businessJid)
                && Objects.equals(catalogSessionId, that.catalogSessionId)
                && Objects.equals(name, that.name)
                && Objects.equals(productsToAdd, that.productsToAdd)
                && Objects.equals(productsToRemove, that.productsToRemove);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collectionId, businessJid, catalogSessionId, name,
                productsToAdd, productsToRemove);
    }

    @Override
    public String toString() {
        return "BusinessCatalogCollectionEdit[" +
                "collectionId=" + collectionId + ", " +
                "businessJid=" + businessJid + ", " +
                "catalogSessionId=" + catalogSessionId + ", " +
                "name=" + name + ", " +
                "productsToAdd=" + productsToAdd + ", " +
                "productsToRemove=" + productsToRemove + ']';
    }
}
