package com.github.auties00.cobalt.wire.graphql.whatsapp.business;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the merchant-compliance query built by
 * {@link BizGetMerchantComplianceWhatsAppGraphQlRequest}.
 *
 * <p>Exposes the linked {@code xfb_whatsapp_biz_merchant_compliance_info} root, whose single linked
 * {@link MerchantInfo} field carries the legal-entity identity and the customer-care and
 * grievance-officer contact blocks WhatsApp surfaces on a business profile's compliance page.
 *
 * @see BizGetMerchantComplianceWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizGetMerchantComplianceQuery")
public final class BizGetMerchantComplianceWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed {@code merchant_info} sub-object.
     */
    private final MerchantInfo merchantInfo;

    /**
     * Constructs a response wrapping the parsed merchant info.
     *
     * <p>Reserved for the static parser.
     *
     * @param merchantInfo the parsed merchant info, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private BizGetMerchantComplianceWhatsAppGraphQlResponse(MerchantInfo merchantInfo) {
        this.merchantInfo = merchantInfo;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xfb_whatsapp_biz_merchant_compliance_info} and projects its
     * {@code merchant_info} child; the returned {@link Optional} is empty when {@code data} is
     * {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizGetMerchantComplianceWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xfb_whatsapp_biz_merchant_compliance_info");
        var merchantInfo = root == null ? null : MerchantInfo.of(root.getJSONObject("merchant_info")).orElse(null);
        return Optional.of(new BizGetMerchantComplianceWhatsAppGraphQlResponse(merchantInfo));
    }

    /**
     * Returns the parsed merchant info.
     *
     * @return the parsed {@link MerchantInfo}, or empty when the graph.whatsapp.com endpoint omitted the field
     */
    public Optional<MerchantInfo> merchantInfo() {
        return Optional.ofNullable(merchantInfo);
    }

    /**
     * Wraps the {@code merchant_info} sub-object of type {@code XFBWhatsAppBizMerchantInfo}.
     *
     * <p>Carries the legal-entity identity (name, type, registration flag, and a free-text custom
     * type) plus the linked {@link CustomerCareDetails} and {@link GrievanceOfficerDetails} contact
     * blocks.
     */
    public static final class MerchantInfo {
        /**
         * Holds the legal entity name.
         */
        private final String entityName;

        /**
         * Holds the legal entity type.
         *
         * <p>Kept as a {@link String} because the closed value set is not confirmable from the JS
         * bundle of snapshot {@code 1040120866}; WhatsApp Web maps it through
         * {@code WAWebBizSetMerchantCompliance.mapEntityTypeToBusinessTypeOption} at the call site.
         */
        private final String entityType;

        /**
         * Holds whether the merchant is a registered legal entity.
         */
        private final Boolean isRegistered;

        /**
         * Holds the free-text custom entity type used when {@link #entityType} does not cover the
         * merchant's legal form.
         */
        private final String entityTypeCustom;

        /**
         * Holds the parsed customer-care contact block.
         */
        private final CustomerCareDetails customerCareDetails;

        /**
         * Holds the parsed grievance-officer contact block.
         */
        private final GrievanceOfficerDetails grievanceOfficerDetails;

        /**
         * Constructs a merchant-info wrapper from the parsed sub-fields.
         *
         * <p>Reserved for the static parser.
         *
         * @param entityName              the legal entity name
         * @param entityType              the legal entity type
         * @param isRegistered            whether the merchant is a registered legal entity
         * @param entityTypeCustom        the free-text custom entity type
         * @param customerCareDetails     the customer-care contact block
         * @param grievanceOfficerDetails the grievance-officer contact block
         */
        private MerchantInfo(String entityName, String entityType, Boolean isRegistered, String entityTypeCustom, CustomerCareDetails customerCareDetails, GrievanceOfficerDetails grievanceOfficerDetails) {
            this.entityName = entityName;
            this.entityType = entityType;
            this.isRegistered = isRegistered;
            this.entityTypeCustom = entityTypeCustom;
            this.customerCareDetails = customerCareDetails;
            this.grievanceOfficerDetails = grievanceOfficerDetails;
        }

        /**
         * Returns the legal entity name.
         *
         * @return the entity name, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> entityName() {
            return Optional.ofNullable(entityName);
        }

        /**
         * Returns the legal entity type.
         *
         * @return the entity type, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> entityType() {
            return Optional.ofNullable(entityType);
        }

        /**
         * Returns whether the merchant is a registered legal entity.
         *
         * @return {@code true} when the graph.whatsapp.com endpoint reported the merchant as registered, {@code false} when
         *         it did not or omitted the field
         */
        public boolean isRegistered() {
            return isRegistered != null && isRegistered;
        }

        /**
         * Returns the free-text custom entity type.
         *
         * @return the custom entity type, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<String> entityTypeCustom() {
            return Optional.ofNullable(entityTypeCustom);
        }

        /**
         * Returns the customer-care contact block.
         *
         * @return the parsed {@link CustomerCareDetails}, or empty when the graph.whatsapp.com endpoint omitted the field
         */
        public Optional<CustomerCareDetails> customerCareDetails() {
            return Optional.ofNullable(customerCareDetails);
        }

        /**
         * Returns the grievance-officer contact block.
         *
         * @return the parsed {@link GrievanceOfficerDetails}, or empty when the graph.whatsapp.com endpoint omitted the
         *         field
         */
        public Optional<GrievanceOfficerDetails> grievanceOfficerDetails() {
            return Optional.ofNullable(grievanceOfficerDetails);
        }

        /**
         * Parses a {@link MerchantInfo} from the given JSON object.
         *
         * <p>Used by {@link BizGetMerchantComplianceWhatsAppGraphQlResponse#of(JSONObject)} to hydrate the
         * nested {@code merchant_info} entry.
         *
         * @param obj the JSON object to parse
         * @return the parsed {@link MerchantInfo}, or empty when {@code obj} is {@code null}
         */
        static Optional<MerchantInfo> of(JSONObject obj) {
            if (obj == null) {
                return Optional.empty();
            }

            var entityName = obj.getString("entity_name");
            var entityType = obj.getString("entity_type");
            var isRegistered = obj.getBoolean("is_registered");
            var entityTypeCustom = obj.getString("entity_type_custom");
            var customerCareDetails = CustomerCareDetails.of(obj.getJSONObject("customer_care_details")).orElse(null);
            var grievanceOfficerDetails = GrievanceOfficerDetails.of(obj.getJSONObject("grievance_officer_details")).orElse(null);
            return Optional.of(new MerchantInfo(entityName, entityType, isRegistered, entityTypeCustom, customerCareDetails, grievanceOfficerDetails));
        }

        /**
         * Wraps the {@code customer_care_details} sub-object of type
         * {@code XFBWhatsAppBizMerchantInfoCustomerCareDetails}.
         *
         * <p>Carries the contact channels a customer uses to reach the merchant's care desk: an email
         * address, a landline number, and a mobile number.
         */
        public static final class CustomerCareDetails {
            /**
             * Holds the customer-care email address.
             */
            private final String email;

            /**
             * Holds the customer-care landline number.
             */
            private final String landlineNumber;

            /**
             * Holds the customer-care mobile number.
             */
            private final String mobileNumber;

            /**
             * Constructs a customer-care-details wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param email          the customer-care email address
             * @param landlineNumber the customer-care landline number
             * @param mobileNumber   the customer-care mobile number
             */
            private CustomerCareDetails(String email, String landlineNumber, String mobileNumber) {
                this.email = email;
                this.landlineNumber = landlineNumber;
                this.mobileNumber = mobileNumber;
            }

            /**
             * Returns the customer-care email address.
             *
             * @return the email address, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> email() {
                return Optional.ofNullable(email);
            }

            /**
             * Returns the customer-care landline number.
             *
             * @return the landline number, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> landlineNumber() {
                return Optional.ofNullable(landlineNumber);
            }

            /**
             * Returns the customer-care mobile number.
             *
             * @return the mobile number, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> mobileNumber() {
                return Optional.ofNullable(mobileNumber);
            }

            /**
             * Parses a {@link CustomerCareDetails} from the given JSON object.
             *
             * <p>Used by {@link MerchantInfo#of(JSONObject)} to hydrate the nested
             * {@code customer_care_details} entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link CustomerCareDetails}, or empty when {@code obj} is
             *         {@code null}
             */
            static Optional<CustomerCareDetails> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var email = obj.getString("email");
                var landlineNumber = obj.getString("landline_number");
                var mobileNumber = obj.getString("mobile_number");
                return Optional.of(new CustomerCareDetails(email, landlineNumber, mobileNumber));
            }
        }

        /**
         * Wraps the {@code grievance_officer_details} sub-object of type
         * {@code XFBWhatsAppBizMerchantInfoGrievanceOfficerDetails}.
         *
         * <p>Carries the named grievance officer's identity and reachable contact channels: a name, an
         * email address, a landline number, and a mobile number.
         */
        public static final class GrievanceOfficerDetails {
            /**
             * Holds the grievance officer's name.
             */
            private final String name;

            /**
             * Holds the grievance officer's email address.
             */
            private final String email;

            /**
             * Holds the grievance officer's landline number.
             */
            private final String landlineNumber;

            /**
             * Holds the grievance officer's mobile number.
             */
            private final String mobileNumber;

            /**
             * Constructs a grievance-officer-details wrapper from the parsed sub-fields.
             *
             * <p>Reserved for the static parser.
             *
             * @param name           the grievance officer's name
             * @param email          the grievance officer's email address
             * @param landlineNumber the grievance officer's landline number
             * @param mobileNumber   the grievance officer's mobile number
             */
            private GrievanceOfficerDetails(String name, String email, String landlineNumber, String mobileNumber) {
                this.name = name;
                this.email = email;
                this.landlineNumber = landlineNumber;
                this.mobileNumber = mobileNumber;
            }

            /**
             * Returns the grievance officer's name.
             *
             * @return the name, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> name() {
                return Optional.ofNullable(name);
            }

            /**
             * Returns the grievance officer's email address.
             *
             * @return the email address, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> email() {
                return Optional.ofNullable(email);
            }

            /**
             * Returns the grievance officer's landline number.
             *
             * @return the landline number, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> landlineNumber() {
                return Optional.ofNullable(landlineNumber);
            }

            /**
             * Returns the grievance officer's mobile number.
             *
             * @return the mobile number, or empty when the graph.whatsapp.com endpoint omitted the field
             */
            public Optional<String> mobileNumber() {
                return Optional.ofNullable(mobileNumber);
            }

            /**
             * Parses a {@link GrievanceOfficerDetails} from the given JSON object.
             *
             * <p>Used by {@link MerchantInfo#of(JSONObject)} to hydrate the nested
             * {@code grievance_officer_details} entry.
             *
             * @param obj the JSON object to parse
             * @return the parsed {@link GrievanceOfficerDetails}, or empty when {@code obj} is
             *         {@code null}
             */
            static Optional<GrievanceOfficerDetails> of(JSONObject obj) {
                if (obj == null) {
                    return Optional.empty();
                }

                var name = obj.getString("name");
                var email = obj.getString("email");
                var landlineNumber = obj.getString("landline_number");
                var mobileNumber = obj.getString("mobile_number");
                return Optional.of(new GrievanceOfficerDetails(name, email, landlineNumber, mobileNumber));
            }
        }
    }
}
