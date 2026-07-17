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
 * Input model for creating one collection inside a WhatsApp Business catalog.
 *
 * <p>A collection group products under a merchant-chosen display name and an
 * optional seed list of products. The catalog session id is an opaque
 * correlator the WhatsApp Business app uses to group consecutive catalog
 * edits made within the same editor session; leaving it unset starts a
 * fresh, uncorrelated edit.
 */
@ProtobufMessage(name = "BusinessCatalogCollectionCreate")
public final class BusinessCatalogCollectionCreate {
    /**
     * Business account that owns the catalog the collection is added to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid businessJid;

    /**
     * Display name of the new collection.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String name;

    /**
     * Identifiers of the products to seed the collection with. Defaults to
     * {@link List#of()} when unset.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final List<String> productIds;

    /**
     * Opaque session correlator grouping consecutive catalog edits. Unset
     * starts a fresh, uncorrelated edit.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String catalogSessionId;

    /**
     * Constructs a new {@code BusinessCatalogCollectionCreate}.
     *
     * @param businessJid      the business account that owns the catalog; required
     * @param name             the collection display name; required
     * @param productIds       the seed product ids; never {@code null}, defaults to {@link List#of()}
     * @param catalogSessionId the optional session correlator, or {@code null}
     * @throws NullPointerException if {@code businessJid}, {@code name}, or
     *                              {@code productIds} is {@code null}
     */
    public BusinessCatalogCollectionCreate(Jid businessJid, String name, List<String> productIds,
                                           String catalogSessionId) {
        this.businessJid = Objects.requireNonNull(businessJid, "businessJid cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.productIds = Objects.requireNonNull(productIds, "productIds cannot be null");
        this.catalogSessionId = catalogSessionId;
    }

    /**
     * Convenience constructor that accepts any {@link JidProvider} and
     * resolves it to a {@link Jid}.
     *
     * @param businessJid      the business account that owns the catalog; required
     * @param name             the collection display name; required
     * @param productIds       the seed product ids; never {@code null}
     * @param catalogSessionId the optional session correlator, or {@code null}
     * @throws NullPointerException if {@code businessJid}, {@code name}, or
     *                              {@code productIds} is {@code null}
     */
    public BusinessCatalogCollectionCreate(JidProvider businessJid, String name,
                                           List<String> productIds, String catalogSessionId) {
        this(Objects.requireNonNull(businessJid, "businessJid cannot be null").toJid(),
                name, productIds, catalogSessionId);
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
     * Returns the new collection's display name.
     *
     * @return the collection name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the seed product ids.
     *
     * @return the product ids, never {@code null}
     */
    public List<String> productIds() {
        return productIds;
    }

    /**
     * Returns the opaque session correlator.
     *
     * @return an {@link Optional} carrying the session id, or empty when unset
     */
    public Optional<String> catalogSessionId() {
        return Optional.ofNullable(catalogSessionId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessCatalogCollectionCreate) obj;
        return Objects.equals(businessJid, that.businessJid)
                && Objects.equals(name, that.name)
                && Objects.equals(productIds, that.productIds)
                && Objects.equals(catalogSessionId, that.catalogSessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(businessJid, name, productIds, catalogSessionId);
    }

    @Override
    public String toString() {
        return "BusinessCatalogCollectionCreate[" +
                "businessJid=" + businessJid + ", " +
                "name=" + name + ", " +
                "productIds=" + productIds + ", " +
                "catalogSessionId=" + catalogSessionId + ']';
    }
}
