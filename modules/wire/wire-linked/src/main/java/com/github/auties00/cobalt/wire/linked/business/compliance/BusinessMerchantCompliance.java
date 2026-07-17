package com.github.auties00.cobalt.wire.linked.business.compliance;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Legal-entity disclosure published by a WhatsApp Business account to
 * comply with merchant-compliance regulations.
 *
 * <p>Several jurisdictions (most notably India under the Consumer
 * Protection Rules) require online merchants to publish the legal entity
 * behind the account, the entity's category (sole proprietorship, LLP,
 * private company, and so on), an indication of whether the business is
 * formally registered with the relevant authority, and verifiable contact
 * details for both customer-care and a designated grievance officer.
 * WhatsApp surfaces this disclosure under the {@code w:biz:merchant_info}
 * IQ namespace and lets business owners read it (GET) or update it (SET).
 *
 * <p>The {@link #entityName()} and {@link #entityType()} fields are required
 * to make the disclosure valid; {@link #entityTypeCustom()} is only used
 * when the business cannot be classified by one of WhatsApp's canonical
 * entity types. The {@link #customerCareDetails()} and
 * {@link #grievanceOfficerDetails()} blocks must always be present, but
 * each individual contact channel inside them is optional.
 */
@ProtobufMessage
public final class BusinessMerchantCompliance {
    /**
     * Registered legal-entity name of the business as recorded with the
     * relevant authority. Required for the disclosure to be valid.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String entityName;

    /**
     * Entity type identifier classifying the legal form of the business.
     * Values are drawn from WhatsApp's canonical set, for example
     * {@code "Limited liability partnership"}, {@code "Sole proprietorship"},
     * {@code "Partnership"}, {@code "Private Company"}, or
     * {@code "Public Company"}.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String entityType;

    /**
     * Free-form custom entity-type description supplied by the business
     * when none of the canonical entity types fits. Populated only when
     * {@link #entityType} resolves to {@code "Other"}.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String entityTypeCustom;

    /**
     * Whether the business is formally registered with the relevant
     * authority. {@code true} indicates the business has completed
     * registration, {@code false} indicates an unregistered merchant
     * operating in good faith.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean registered;

    /**
     * Customer-care contact block disclosing the channels through which
     * customers can reach the business directly for support enquiries.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    final BusinessContactDetails customerCareDetails;

    /**
     * Grievance-officer contact block disclosing the identity and channels
     * of the individual designated to handle escalated consumer complaints.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    final BusinessGrievanceOfficerDetails grievanceOfficerDetails;

    /**
     * Constructs a new {@code BusinessMerchantCompliance} disclosure. The
     * legal-entity name, entity type, customer-care block, and
     * grievance-officer block are required; {@code entityTypeCustom} may
     * be {@code null} when the canonical entity type is sufficient.
     *
     * @param entityName              the registered legal-entity name; must not be {@code null}
     * @param entityType              the canonical entity-type identifier; must not be {@code null}
     * @param entityTypeCustom        the optional custom entity-type description, or {@code null}
     * @param registered              {@code true} if the business is formally registered
     * @param customerCareDetails     the customer-care contact block; must not be {@code null}
     * @param grievanceOfficerDetails the grievance-officer contact block; must not be {@code null}
     * @throws NullPointerException if any required argument is {@code null}
     */
    BusinessMerchantCompliance(String entityName,
                               String entityType,
                               String entityTypeCustom,
                               boolean registered,
                               BusinessContactDetails customerCareDetails,
                               BusinessGrievanceOfficerDetails grievanceOfficerDetails) {
        this.entityName = Objects.requireNonNull(entityName, "entityName cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        this.entityTypeCustom = entityTypeCustom;
        this.registered = registered;
        this.customerCareDetails = Objects.requireNonNull(customerCareDetails, "customerCareDetails cannot be null");
        this.grievanceOfficerDetails = Objects.requireNonNull(grievanceOfficerDetails, "grievanceOfficerDetails cannot be null");
    }

    /**
     * Returns the registered legal-entity name of the business.
     *
     * @return the entity name, never {@code null}
     */
    public String entityName() {
        return entityName;
    }

    /**
     * Returns the canonical entity-type identifier classifying the legal
     * form of the business.
     *
     * @return the entity type, never {@code null}
     */
    public String entityType() {
        return entityType;
    }

    /**
     * Returns the free-form custom entity-type description supplied when
     * none of the canonical entity types fits.
     *
     * @return an {@code Optional} containing the custom entity-type
     *         description, or empty when the canonical
     *         {@link #entityType()} is sufficient
     */
    public Optional<String> entityTypeCustom() {
        return Optional.ofNullable(entityTypeCustom);
    }

    /**
     * Returns whether the business is formally registered with the
     * relevant authority.
     *
     * @return {@code true} when the business is registered, {@code false}
     *         otherwise
     */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * Returns the customer-care contact block disclosing how customers
     * can reach the business directly for support enquiries.
     *
     * @return the customer-care contact block, never {@code null}
     */
    public BusinessContactDetails customerCareDetails() {
        return customerCareDetails;
    }

    /**
     * Returns the grievance-officer contact block disclosing how
     * customers can reach the designated grievance officer.
     *
     * @return the grievance-officer contact block, never {@code null}
     */
    public BusinessGrievanceOfficerDetails grievanceOfficerDetails() {
        return grievanceOfficerDetails;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (BusinessMerchantCompliance) obj;
        return this.registered == that.registered &&
               Objects.equals(this.entityName, that.entityName) &&
               Objects.equals(this.entityType, that.entityType) &&
               Objects.equals(this.entityTypeCustom, that.entityTypeCustom) &&
               Objects.equals(this.customerCareDetails, that.customerCareDetails) &&
               Objects.equals(this.grievanceOfficerDetails, that.grievanceOfficerDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityName, entityType, entityTypeCustom, registered, customerCareDetails, grievanceOfficerDetails);
    }

    @Override
    public String toString() {
        return "BusinessMerchantCompliance[" +
               "entityName=" + entityName + ", " +
               "entityType=" + entityType + ", " +
               "entityTypeCustom=" + entityTypeCustom + ", " +
               "registered=" + registered + ", " +
               "customerCareDetails=" + customerCareDetails + ", " +
               "grievanceOfficerDetails=" + grievanceOfficerDetails + ']';
    }
}
