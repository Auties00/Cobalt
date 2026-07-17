package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessContactDetailsBuilder;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessGrievanceOfficerDetailsBuilder;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessMerchantCompliance;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessMerchantComplianceBuilder;

import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the set-merchant-compliance mutation built by
 * {@link BizSetMerchantComplianceFacebookGraphQlRequest} into a {@link BusinessMerchantCompliance}.
 *
 * <p>Reads the linked {@code xfb_whatsapp_biz_merchant_set_compliance_info} field and projects its
 * persisted {@code merchant_info} child (entity identity, registration state, and the customer-care and
 * grievance-officer contact blocks) onto the Cobalt domain model.
 *
 * @see BizSetMerchantComplianceFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizSetMerchantComplianceMutation")
public final class BizSetMerchantComplianceFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the parsed persisted merchant-compliance record.
     */
    private final BusinessMerchantCompliance merchantInfo;

    /**
     * Constructs a response wrapping the parsed compliance record.
     *
     * <p>Reserved for the static parser.
     *
     * @param merchantInfo the parsed compliance record, or {@code null} when the Meta graph endpoint omitted the
     *                     field
     */
    private BizSetMerchantComplianceFacebookGraphQlResponse(BusinessMerchantCompliance merchantInfo) {
        this.merchantInfo = merchantInfo;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_biz_merchant_set_compliance_info} and projects its
     * {@code merchant_info} child onto a {@link BusinessMerchantCompliance}; the returned
     * {@link Optional} is empty when {@code data} or the merchant-info projection is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the merchant-info projection is missing
     */
    @WhatsAppWebExport(moduleName = "WAWebBizSetMerchantComplianceMutation", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static Optional<BizSetMerchantComplianceFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }
        var root = data.getJSONObject("xfb_whatsapp_biz_merchant_set_compliance_info");
        if (root == null) {
            return Optional.empty();
        }
        var info = root.getJSONObject("merchant_info");
        if (info == null) {
            return Optional.empty();
        }
        var customerCare = info.getJSONObject("customer_care_details");
        var grievance = info.getJSONObject("grievance_officer_details");
        var isRegistered = info.getBoolean("is_registered");
        var merchant = new BusinessMerchantComplianceBuilder()
                .entityName(orEmpty(info.getString("entity_name")))
                .entityType(orEmpty(info.getString("entity_type")))
                .entityTypeCustom(info.getString("entity_type_custom"))
                .registered(isRegistered != null && isRegistered)
                .customerCareDetails(new BusinessContactDetailsBuilder()
                        .email(customerCare == null ? null : customerCare.getString("email"))
                        .landlineNumber(customerCare == null ? null : customerCare.getString("landline_number"))
                        .mobileNumber(customerCare == null ? null : customerCare.getString("mobile_number"))
                        .build())
                .grievanceOfficerDetails(new BusinessGrievanceOfficerDetailsBuilder()
                        .name(grievance == null ? null : grievance.getString("name"))
                        .email(grievance == null ? null : grievance.getString("email"))
                        .landlineNumber(grievance == null ? null : grievance.getString("landline_number"))
                        .mobileNumber(grievance == null ? null : grievance.getString("mobile_number"))
                        .build())
                .build();
        return Optional.of(new BizSetMerchantComplianceFacebookGraphQlResponse(merchant));
    }

    /**
     * Coalesces a possibly-{@code null} string into the empty string.
     *
     * @param value the value to coalesce, or {@code null}
     * @return {@code value}, or the empty string when {@code value} is {@code null}
     */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns the parsed persisted merchant-compliance record.
     *
     * <p>The returned {@link BusinessMerchantCompliance} carries the legal-entity identity, the
     * registration flag, and the customer-care and grievance-officer contact blocks.
     *
     * @return the parsed compliance record, never {@code null}
     */
    public BusinessMerchantCompliance merchantInfo() {
        return merchantInfo;
    }

    /**
     * Models the closed set of merchant entity types returned under {@code merchant_info.entity_type}.
     *
     * <p>The type set is the one mapped by WhatsApp Web's
     * {@code WAWebBizSetMerchantCompliance.mapEntityTypeToBusinessTypeOption}, whose {@code switch}
     * branches on exactly {@code "SOLE_PROPRIETORSHIP"}, {@code "PARTNERSHIP"},
     * {@code "PRIVATE_COMPANY"}, {@code "PUBLIC_COMPANY"}, {@code "LIMITED_LIABILITY_PARTNERSHIP"},
     * and {@code "OTHER"}, treating every other value (including {@code null}) as {@code OTHER}.
     *
     * @implNote This implementation maps an unrecognized or {@code null} token to
     * {@link Optional#empty()} rather than collapsing it to {@link #OTHER}, so callers can distinguish
     * an explicit {@code "OTHER"} verdict from a missing or out-of-set value; the source
     * {@code mapEntityTypeToBusinessTypeOption} default folds both into the {@code "Other"} display
     * label.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizSetMerchantCompliance", exports = "mapEntityTypeToBusinessTypeOption",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public enum EntityType {
        /**
         * A business owned and run by a single individual.
         */
        SOLE_PROPRIETORSHIP("SOLE_PROPRIETORSHIP"),
        /**
         * A business owned by two or more partners.
         */
        PARTNERSHIP("PARTNERSHIP"),
        /**
         * A privately held company.
         */
        PRIVATE_COMPANY("PRIVATE_COMPANY"),
        /**
         * A publicly traded company.
         */
        PUBLIC_COMPANY("PUBLIC_COMPANY"),
        /**
         * A limited liability partnership.
         */
        LIMITED_LIABILITY_PARTNERSHIP("LIMITED_LIABILITY_PARTNERSHIP"),
        /**
         * Any other entity type.
         */
        OTHER("OTHER");

        /**
         * Holds the wire token carried under {@code merchant_info.entity_type}.
         */
        private final String value;

        /**
         * Constructs an entity-type constant bound to its wire token.
         *
         * @param value the wire token carried under {@code merchant_info.entity_type}
         */
        EntityType(String value) {
            this.value = value;
        }

        /**
         * Returns the wire token this entity type serializes to.
         *
         * @return the wire token, never {@code null}
         */
        public String value() {
            return value;
        }

        /**
         * Maps a raw {@code merchant_info.entity_type} token onto its {@link EntityType} constant.
         *
         * @param value the raw wire token, may be {@code null}
         * @return the matching constant, or empty when {@code value} is {@code null} or outside the
         *         confirmed set
         */
        public static Optional<EntityType> of(String value) {
            if (value == null) {
                return Optional.empty();
            }

            for (var type : values()) {
                if (type.value.equals(value)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }
}
