package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the {@code <iq xmlns="w:biz:merchant_info" type="set">} stanza that updates the
 * regulatory-compliance bundle of the current merchant.
 *
 * <p>The stanza is sent from the merchant-compliance edit surface to set or update the legal-entity
 * registration flag, entity name, entity type, customer-care triple and grievance-officer block in a
 * single round-trip. The relay accepts a partial mutation, so any field left {@code null} is dropped
 * from the wire stanza rather than zeroed out.
 *
 * @implNote
 * This implementation models the legacy WAP-IQ path only; WA Web routes the same call through the
 * Relay GraphQL endpoint when the {@code graphQLForSetComplianceInfo} gating flag is on, falling
 * back to the WAP-IQ payload on graphql-error and not-enabled paths, but Cobalt keeps the WAP-IQ
 * payload as the single transport.
 */
@WhatsAppWebModule(moduleName = "WAWebMerchantComplianceJob")
public final class IqSetMerchantComplianceRequest implements IqStanza.Request {
    /**
     * Holds the registered flag stamped into the {@code is_registered} attribute of the
     * {@code <merchant_info/>} child.
     */
    private final boolean registered;

    /**
     * Holds the optional legal entity name stamped into the {@code <entity_name/>} grandchild.
     */
    private final String entityName;

    /**
     * Holds the optional entity-type marker stamped into the {@code <entity_type/>} grandchild.
     */
    private final String entityType;

    /**
     * Holds the optional custom entity-type label stamped into the {@code <entity_type_custom/>}
     * grandchild when the entity type is {@code "OTHER"}.
     */
    private final String entityTypeCustom;

    /**
     * Holds the optional customer-care contact triple flattened into the
     * {@code <customer_care_details/>} grandchild.
     */
    private final IqSetMerchantComplianceContactDetails customerCareDetails;

    /**
     * Holds the optional grievance-officer block flattened into the
     * {@code <grievance_officer_details/>} grandchild.
     */
    private final IqSetMerchantComplianceGrievanceOfficerDetails grievanceOfficerDetails;

    /**
     * Constructs a request from the full field set.
     *
     * <p>Any field left {@code null} is dropped from the wire stanza rather than zeroed out. Prefer
     * {@link #builder()} when only a subset of fields needs to be set; this all-args constructor is
     * exposed for callers that keep their own pre-validated bundle.
     *
     * @param registered              the registered flag
     * @param entityName              the entity name; may be {@code null}
     * @param entityType              the entity type; may be {@code null}
     * @param entityTypeCustom        the custom-type label; may be {@code null}
     * @param customerCareDetails     the customer-care triple; may be {@code null}
     * @param grievanceOfficerDetails the grievance-officer block; may be {@code null}
     */
    public IqSetMerchantComplianceRequest(boolean registered,
                   String entityName,
                   String entityType,
                   String entityTypeCustom,
                   IqSetMerchantComplianceContactDetails customerCareDetails,
                   IqSetMerchantComplianceGrievanceOfficerDetails grievanceOfficerDetails) {
        this.registered = registered;
        this.entityName = entityName;
        this.entityType = entityType;
        this.entityTypeCustom = entityTypeCustom;
        this.customerCareDetails = customerCareDetails;
        this.grievanceOfficerDetails = grievanceOfficerDetails;
    }

    /**
     * Returns the registered flag the mutation stamps.
     *
     * <p>The value is routed verbatim into the {@code is_registered} attribute of the
     * {@code <merchant_info/>} child.
     *
     * @return the flag
     */
    public boolean registered() {
        return registered;
    }

    /**
     * Returns the entity name the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the name
     */
    public Optional<String> entityName() {
        return Optional.ofNullable(entityName);
    }

    /**
     * Returns the entity-type marker the mutation stamps.
     *
     * <p>An empty optional means the field is dropped from the wire stanza.
     *
     * @return an {@link Optional} carrying the type
     */
    public Optional<String> entityType() {
        return Optional.ofNullable(entityType);
    }

    /**
     * Returns the custom entity-type label the mutation stamps.
     *
     * <p>The label is set only when {@link #entityType()} resolves to {@code "OTHER"}.
     *
     * @return an {@link Optional} carrying the label
     */
    public Optional<String> entityTypeCustom() {
        return Optional.ofNullable(entityTypeCustom);
    }

    /**
     * Returns the customer-care contact triple the mutation stamps.
     *
     * <p>An empty optional means the {@code <customer_care_details/>} grandchild is dropped from the
     * wire stanza.
     *
     * @return an {@link Optional} carrying the triple
     */
    public Optional<IqSetMerchantComplianceContactDetails> customerCareDetails() {
        return Optional.ofNullable(customerCareDetails);
    }

    /**
     * Returns the grievance-officer block the mutation stamps.
     *
     * <p>An empty optional means the {@code <grievance_officer_details/>} grandchild is dropped from
     * the wire stanza.
     *
     * @return an {@link Optional} carrying the block
     */
    public Optional<IqSetMerchantComplianceGrievanceOfficerDetails> grievanceOfficerDetails() {
        return Optional.ofNullable(grievanceOfficerDetails);
    }

    /**
     * Returns a fresh builder.
     *
     * <p>The returned builder defaults every field to {@code null} and the registered flag to
     * {@code false}; use it to assemble a request when only a subset of fields needs to be set.
     *
     * @return a new {@link Builder}; never {@code null}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation materialises a {@code <merchant_info is_registered/>} child carrying one
     * grandchild per non-{@code null} field, wrapped in the {@code w:biz:merchant_info set} IQ frame
     * routed to the WhatsApp service.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebMerchantComplianceJob",
            exports = "setMerchantCompliance", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var children = new ArrayList<Stanza>();
        if (entityName != null) {
            children.add(new StanzaBuilder().description("entity_name").content(entityName).build());
        }
        if (entityType != null) {
            children.add(new StanzaBuilder().description("entity_type").content(entityType).build());
        }
        if (entityTypeCustom != null) {
            children.add(new StanzaBuilder().description("entity_type_custom")
                    .content(entityTypeCustom).build());
        }
        if (customerCareDetails != null) {
            children.add(buildContactNode("customer_care_details", customerCareDetails));
        }
        if (grievanceOfficerDetails != null) {
            children.add(buildGrievanceNode(grievanceOfficerDetails));
        }
        var merchantInfoNode = new StanzaBuilder()
                .description("merchant_info")
                .attribute("is_registered", registered ? "true" : "false")
                .content(children)
                .build();
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:biz:merchant_info")
                .attribute("to", JidServer.user())
                .attribute("type", "set")
                .content(merchantInfoNode);
    }

    /**
     * Builds a contact-triple child carrying the supplied tag for {@link #toStanza()}.
     *
     * <p>The {@code email}, {@code landline_number} and {@code mobile_number} fields of the supplied
     * triple are flattened into grandchildren of the requested element; an absent field is dropped.
     *
     * @param description the wrapper element tag (e.g. {@code "customer_care_details"})
     * @param contact     the contact triple
     * @return the built stanza; never {@code null}
     */
    private static Stanza buildContactNode(String description, IqSetMerchantComplianceContactDetails contact) {
        var contactChildren = new ArrayList<Stanza>();
        if (contact.email().isPresent()) {
            contactChildren.add(new StanzaBuilder()
                    .description("email")
                    .content(contact.email().get())
                    .build());
        }
        if (contact.landlineNumber().isPresent()) {
            contactChildren.add(new StanzaBuilder()
                    .description("landline_number")
                    .content(contact.landlineNumber().get())
                    .build());
        }
        if (contact.mobileNumber().isPresent()) {
            contactChildren.add(new StanzaBuilder()
                    .description("mobile_number")
                    .content(contact.mobileNumber().get())
                    .build());
        }
        return new StanzaBuilder()
                .description(description)
                .content(contactChildren)
                .build();
    }

    /**
     * Builds the {@code <grievance_officer_details/>} child for {@link #toStanza()}.
     *
     * <p>The officer name and contact triple are flattened into grandchildren of the wrapper
     * element; an absent field is dropped.
     *
     * @param block the block; never {@code null}
     * @return the built stanza; never {@code null}
     */
    private static Stanza buildGrievanceNode(IqSetMerchantComplianceGrievanceOfficerDetails block) {
        var grievanceChildren = new ArrayList<Stanza>();
        if (block.name().isPresent()) {
            grievanceChildren.add(new StanzaBuilder()
                    .description("name")
                    .content(block.name().get())
                    .build());
        }
        var contact = block.contact();
        if (contact.email().isPresent()) {
            grievanceChildren.add(new StanzaBuilder()
                    .description("email")
                    .content(contact.email().get())
                    .build());
        }
        if (contact.landlineNumber().isPresent()) {
            grievanceChildren.add(new StanzaBuilder()
                    .description("landline_number")
                    .content(contact.landlineNumber().get())
                    .build());
        }
        if (contact.mobileNumber().isPresent()) {
            grievanceChildren.add(new StanzaBuilder()
                    .description("mobile_number")
                    .content(contact.mobileNumber().get())
                    .build());
        }
        return new StanzaBuilder()
                .description("grievance_officer_details")
                .content(grievanceChildren)
                .build();
    }

    /**
     * Assembles an {@link IqSetMerchantComplianceRequest} field by field.
     *
     * <p>Each setter is chainable and {@link #build()} snapshots the current state into an immutable
     * request; use the builder when only a subset of fields needs to be set.
     */
    public static final class Builder {
        /**
         * Holds the registered flag; defaults to {@code false}.
         */
        private boolean registered;

        /**
         * Holds the optional entity name.
         */
        private String entityName;

        /**
         * Holds the optional entity type.
         */
        private String entityType;

        /**
         * Holds the optional custom-type label.
         */
        private String entityTypeCustom;

        /**
         * Holds the optional customer-care triple.
         */
        private IqSetMerchantComplianceContactDetails customerCareDetails;

        /**
         * Holds the optional grievance-officer block.
         */
        private IqSetMerchantComplianceGrievanceOfficerDetails grievanceOfficerDetails;

        /**
         * Constructs a builder; obtain an instance via {@link IqSetMerchantComplianceRequest#builder()}.
         */
        Builder() {
        }

        /**
         * Sets the registered flag.
         *
         * <p>Pass {@code true} when the legal entity has registered with the local commerce
         * authority; the relay surfaces the flag back inside the next compliance-info fetch.
         *
         * @param registered the flag
         * @return this builder; never {@code null}
         */
        public Builder registered(boolean registered) {
            this.registered = registered;
            return this;
        }

        /**
         * Sets the entity name.
         *
         * <p>Pass {@code null} to leave the field unchanged on the merchant's bundle.
         *
         * @param entityName the name; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder entityName(String entityName) {
            this.entityName = entityName;
            return this;
        }

        /**
         * Sets the entity type.
         *
         * <p>Pass one of the type markers documented by the relay (e.g. {@code "PROPRIETORSHIP"},
         * {@code "PARTNERSHIP"}, {@code "OTHER"}); pass {@code null} to leave the field unchanged on
         * the merchant's bundle.
         *
         * @param entityType the type; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * Sets the custom entity-type label.
         *
         * <p>Pass the custom label when {@link #entityType(String)} is set to {@code "OTHER"}; pass
         * {@code null} otherwise.
         *
         * @param entityTypeCustom the label; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder entityTypeCustom(String entityTypeCustom) {
            this.entityTypeCustom = entityTypeCustom;
            return this;
        }

        /**
         * Sets the customer-care contact triple.
         *
         * <p>Pass {@code null} to leave the {@code <customer_care_details/>} grandchild dropped from
         * the mutation.
         *
         * @param customerCareDetails the triple; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder customerCareDetails(IqSetMerchantComplianceContactDetails customerCareDetails) {
            this.customerCareDetails = customerCareDetails;
            return this;
        }

        /**
         * Sets the grievance-officer block.
         *
         * <p>Pass {@code null} to leave the {@code <grievance_officer_details/>} grandchild dropped
         * from the mutation.
         *
         * @param grievanceOfficerDetails the block; may be {@code null}
         * @return this builder; never {@code null}
         */
        public Builder grievanceOfficerDetails(IqSetMerchantComplianceGrievanceOfficerDetails grievanceOfficerDetails) {
            this.grievanceOfficerDetails = grievanceOfficerDetails;
            return this;
        }

        /**
         * Builds an immutable {@link IqSetMerchantComplianceRequest} snapshotting the current builder
         * state.
         *
         * @return the built request; never {@code null}
         */
        public IqSetMerchantComplianceRequest build() {
            return new IqSetMerchantComplianceRequest(registered, entityName, entityType, entityTypeCustom,
                    customerCareDetails, grievanceOfficerDetails);
        }
    }

    /**
     * Compares this request with another for value equality across every wire-bearing field.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqSetMerchantComplianceRequest) obj;
        return this.registered == that.registered
                && Objects.equals(this.entityName, that.entityName)
                && Objects.equals(this.entityType, that.entityType)
                && Objects.equals(this.entityTypeCustom, that.entityTypeCustom)
                && Objects.equals(this.customerCareDetails, that.customerCareDetails)
                && Objects.equals(this.grievanceOfficerDetails, that.grievanceOfficerDetails);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(registered, entityName, entityType, entityTypeCustom,
                customerCareDetails, grievanceOfficerDetails);
    }

    /**
     * Returns a diagnostic string naming the registered flag, entity name and entity type.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "IqSetMerchantComplianceRequest[registered=" + registered
                + ", entityName=" + entityName + ", entityType=" + entityType + ']';
    }
}
