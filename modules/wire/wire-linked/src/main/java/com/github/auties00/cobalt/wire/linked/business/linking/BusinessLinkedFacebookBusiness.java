package com.github.auties00.cobalt.wire.linked.business.linking;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Linked Facebook Business projection within a
 * {@link BusinessLinkedAccounts} bundle.
 *
 * <p>The Facebook Business identity represents the Business Manager
 * record that owns the page, the catalog and the ad account. The relay
 * exposes the business identifier and display name plus an optional
 * catalog projection.
 */
@ProtobufMessage(name = "BusinessLinkedFacebookBusiness")
public final class BusinessLinkedFacebookBusiness {
    /**
     * The Facebook Business Manager identifier.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * The business display name (the user-facing brand name shown in
     * the Linked Accounts panel).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String displayName;

    /**
     * Optional catalog identifier linked to this business.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String catalogId;

    /**
     * Optional catalog-import disabled toggle. {@code true} when the
     * user has disabled importing the Facebook catalog into the
     * WhatsApp catalog, absent (empty {@link Optional}) when the relay
     * omitted the toggle.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    Boolean catalogImportDisabled;

    /**
     * Full protobuf constructor invoked by the generated builder and the
     * deserializer.
     *
     * @param id                    the business identifier
     * @param displayName           the business display name
     * @param catalogId             the optional catalog identifier
     * @param catalogImportDisabled the optional catalog-import disabled
     *                              toggle
     */
    BusinessLinkedFacebookBusiness(String id, String displayName,
                                   String catalogId, Boolean catalogImportDisabled) {
        this.id = id;
        this.displayName = displayName;
        this.catalogId = catalogId;
        this.catalogImportDisabled = catalogImportDisabled;
    }

    /**
     * Returns the business identifier.
     *
     * @return the identifier; never {@code null} for a parsed
     *         projection
     */
    public String id() {
        return id;
    }

    /**
     * Returns the business display name.
     *
     * @return the name; never {@code null} for a parsed projection
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns the optional catalog identifier.
     *
     * @return an {@link Optional} carrying the identifier, or empty
     *         when no catalog is linked
     */
    public Optional<String> catalogId() {
        return Optional.ofNullable(catalogId);
    }

    /**
     * Returns the optional catalog-import disabled toggle.
     *
     * @return an {@link Optional} carrying the toggle, or empty when
     *         the relay omitted it
     */
    public Optional<Boolean> catalogImportDisabled() {
        return Optional.ofNullable(catalogImportDisabled);
    }
}
