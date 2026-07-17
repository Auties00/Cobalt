package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants produced by the relay in response to an {@link IqGetMerchantComplianceRequest}.
 *
 * <p>The three documented outcomes of a merchant-compliance fetch are {@link Success}, which
 * carries the decoded compliance bundles per merchant, {@link ClientError}, which surfaces a relay
 * validation rejection, and {@link ServerError}, which reports a transport or backend failure. The
 * dispatcher invokes {@link #of(Stanza, Stanza)} to project the raw {@link Stanza} into the right variant.
 */
@WhatsAppWebModule(moduleName = "WAWebMerchantComplianceJob")
public sealed interface IqGetMerchantComplianceResponse extends IqStanza.Response
        permits IqGetMerchantComplianceResponse.Success, IqGetMerchantComplianceResponse.ClientError, IqGetMerchantComplianceResponse.ServerError {

    /**
     * Tries each {@link IqGetMerchantComplianceResponse} variant in priority order.
     *
     * <p>The success path is tried first, then the client-error envelope, then the server-error
     * envelope. The result is empty only when none of the three documented shapes apply.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqGetMerchantComplianceResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries the typed merchant-compliance bundles for a successful fetch.
     *
     * <p>The India e-commerce compliance surfaces consume the {@link MerchantInfo} entries to
     * render the entity-name row, the entity-type row, the registered flag, the customer-care
     * contact triple and the grievance-officer block.
     */
    final class Success implements IqGetMerchantComplianceResponse {
        /**
         * Models one merchant's compliance bundle decoded from a {@code <merchant_info/>} child of the inbound IQ.
         *
         * <p>The entity-name and entity-type fields drive the headline rows, the registered flag
         * drives the registered-merchant badge, and the customer-care and grievance-officer blocks
         * drive the contact tables.
         */
        public static final class MerchantInfo {
            /**
             * Holds the legal entity name lifted from {@code <merchant_info><entity_name/></merchant_info>}.
             *
             * <p>Empty when the relay returned no entity name.
             */
            private final String entityName;

            /**
             * Holds the entity-type identifier (for example {@code "LIMITED_LIABILITY_PARTNERSHIP"} or {@code "PRIVATE_COMPANY"}).
             *
             * <p>Empty when the relay returned no entity type.
             */
            private final String entityType;

            /**
             * Holds the registered-merchant flag lifted from the {@code is_registered} attribute.
             */
            private final boolean registered;

            /**
             * Holds the optional custom entity-type free-text label rendered when {@link #entityType} is {@code "OTHER"}.
             */
            private final String entityTypeCustom;

            /**
             * Holds the customer-care contact triple decoded from {@code <merchant_info><customer_care_details/></merchant_info>}.
             */
            private final BusinessContactDetails customerCareDetails;

            /**
             * Holds the grievance-officer block decoded from {@code <merchant_info><grievance_officer_details/></merchant_info>}.
             */
            private final BusinessGrievanceOfficerDetails grievanceOfficerDetails;

            /**
             * Constructs a typed merchant-info entry from a decoded {@code <merchant_info/>} child.
             *
             * <p>A {@code null} {@code entityTypeCustom} marks a wire shape that omitted the
             * custom-type label.
             *
             * @param entityName              the legal entity name; never {@code null}
             * @param entityType              the entity-type identifier; never {@code null}
             * @param registered              the registered flag
             * @param entityTypeCustom        the custom-type label; may be {@code null}
             * @param customerCareDetails     the customer-care contact triple; never {@code null}
             * @param grievanceOfficerDetails the grievance-officer block; never {@code null}
             * @throws NullPointerException if any non-optional argument is {@code null}
             */
            public MerchantInfo(String entityName,
                                String entityType,
                                boolean registered,
                                String entityTypeCustom,
                                BusinessContactDetails customerCareDetails,
                                BusinessGrievanceOfficerDetails grievanceOfficerDetails) {
                this.entityName = Objects.requireNonNull(entityName, "entityName cannot be null");
                this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
                this.registered = registered;
                this.entityTypeCustom = entityTypeCustom;
                this.customerCareDetails = Objects.requireNonNull(
                        customerCareDetails, "customerCareDetails cannot be null");
                this.grievanceOfficerDetails = Objects.requireNonNull(
                        grievanceOfficerDetails, "grievanceOfficerDetails cannot be null");
            }

            /**
             * Returns the legal entity name that drives the headline entity-name row of the compliance surface.
             *
             * @return the name; never {@code null}
             */
            public String entityName() {
                return entityName;
            }

            /**
             * Returns the entity-type identifier.
             *
             * <p>Consumers map the value through a localised lookup to render the entity-type row.
             *
             * @return the identifier; never {@code null}
             */
            public String entityType() {
                return entityType;
            }

            /**
             * Returns the registered-merchant flag that drives the registered-merchant badge.
             *
             * @return {@code true} when the merchant is registered
             */
            public boolean registered() {
                return registered;
            }

            /**
             * Returns the merchant-supplied custom entity-type label.
             *
             * <p>Rendered when {@link #entityType()} is the catch-all {@code "OTHER"} bucket; empty
             * otherwise.
             *
             * @return an {@link Optional} carrying the label
             */
            public Optional<String> entityTypeCustom() {
                return Optional.ofNullable(entityTypeCustom);
            }

            /**
             * Returns the customer-care contact triple that drives the customer-care row of the compliance surface.
             *
             * @return the contact triple; never {@code null}
             */
            public BusinessContactDetails customerCareDetails() {
                return customerCareDetails;
            }

            /**
             * Returns the grievance-officer block that drives the grievance-officer row of the compliance surface.
             *
             * @return the block; never {@code null}
             */
            public BusinessGrievanceOfficerDetails grievanceOfficerDetails() {
                return grievanceOfficerDetails;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (MerchantInfo) obj;
                return this.registered == that.registered
                        && Objects.equals(this.entityName, that.entityName)
                        && Objects.equals(this.entityType, that.entityType)
                        && Objects.equals(this.entityTypeCustom, that.entityTypeCustom)
                        && Objects.equals(this.customerCareDetails, that.customerCareDetails)
                        && Objects.equals(this.grievanceOfficerDetails, that.grievanceOfficerDetails);
            }

            @Override
            public int hashCode() {
                return Objects.hash(entityName, entityType, registered, entityTypeCustom,
                        customerCareDetails, grievanceOfficerDetails);
            }

            @Override
            public String toString() {
                return "IqGetMerchantComplianceResponse.Success.MerchantInfo[entityName="
                        + entityName + ", entityType=" + entityType
                        + ", registered=" + registered + ']';
            }
        }

        /**
         * Models the customer-care contact triple decoded from the {@code <customer_care_details/>} block.
         *
         * <p>Each field defaults to the empty string when the wire shape omitted the corresponding
         * child, matching the WA Web parser's default.
         */
        public static final class BusinessContactDetails {
            /**
             * Holds the contact email lifted from {@code <customer_care_details><email/></customer_care_details>}.
             */
            private final String email;

            /**
             * Holds the landline phone number lifted from {@code <customer_care_details><landline_number/></customer_care_details>}.
             */
            private final String landlineNumber;

            /**
             * Holds the mobile phone number lifted from {@code <customer_care_details><mobile_number/></customer_care_details>}.
             */
            private final String mobileNumber;

            /**
             * Constructs a typed contact triple from a decoded customer-care or grievance-officer contact block.
             *
             * <p>Any field that the wire shape omitted is passed as an empty string to keep the WA
             * Web parser's defaults.
             *
             * @param email          the contact email; never {@code null}
             * @param landlineNumber the landline number; never {@code null}
             * @param mobileNumber   the mobile number; never {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public BusinessContactDetails(String email, String landlineNumber, String mobileNumber) {
                this.email = Objects.requireNonNull(email, "email cannot be null");
                this.landlineNumber = Objects.requireNonNull(landlineNumber, "landlineNumber cannot be null");
                this.mobileNumber = Objects.requireNonNull(mobileNumber, "mobileNumber cannot be null");
            }

            /**
             * Returns the contact email that drives the contact-email cell of the customer-care row.
             *
             * @return the email; never {@code null}
             */
            public String email() {
                return email;
            }

            /**
             * Returns the landline phone number that drives the landline-number cell of the customer-care row.
             *
             * @return the number; never {@code null}
             */
            public String landlineNumber() {
                return landlineNumber;
            }

            /**
             * Returns the mobile phone number that drives the mobile-number cell of the customer-care row.
             *
             * @return the number; never {@code null}
             */
            public String mobileNumber() {
                return mobileNumber;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BusinessContactDetails) obj;
                return Objects.equals(this.email, that.email)
                        && Objects.equals(this.landlineNumber, that.landlineNumber)
                        && Objects.equals(this.mobileNumber, that.mobileNumber);
            }

            @Override
            public int hashCode() {
                return Objects.hash(email, landlineNumber, mobileNumber);
            }

            @Override
            public String toString() {
                return "IqGetMerchantComplianceResponse.Success.BusinessContactDetails[email="
                        + email + ", landlineNumber=" + landlineNumber
                        + ", mobileNumber=" + mobileNumber + ']';
            }
        }

        /**
         * Models the grievance-officer block decoded from the {@code <grievance_officer_details/>} child.
         *
         * <p>Extends the customer-care triple with an officer name. Each field defaults to the
         * empty string when the wire shape omitted the corresponding child, matching the WA Web
         * parser's default.
         */
        public static final class BusinessGrievanceOfficerDetails {
            /**
             * Holds the officer's full name lifted from {@code <grievance_officer_details><name/></grievance_officer_details>}.
             */
            private final String name;

            /**
             * Holds the officer's email lifted from {@code <grievance_officer_details><email/></grievance_officer_details>}.
             */
            private final String email;

            /**
             * Holds the officer's landline phone number lifted from {@code <grievance_officer_details><landline_number/></grievance_officer_details>}.
             */
            private final String landlineNumber;

            /**
             * Holds the officer's mobile phone number lifted from {@code <grievance_officer_details><mobile_number/></grievance_officer_details>}.
             */
            private final String mobileNumber;

            /**
             * Constructs a typed grievance-officer block from a decoded {@code <grievance_officer_details/>} child.
             *
             * <p>Any field that the wire shape omitted is passed as an empty string to keep the WA
             * Web parser's defaults.
             *
             * @param name           the officer's full name; never {@code null}
             * @param email          the officer's email; never {@code null}
             * @param landlineNumber the officer's landline number; never {@code null}
             * @param mobileNumber   the officer's mobile number; never {@code null}
             * @throws NullPointerException if any argument is {@code null}
             */
            public BusinessGrievanceOfficerDetails(String name, String email,
                                           String landlineNumber, String mobileNumber) {
                this.name = Objects.requireNonNull(name, "name cannot be null");
                this.email = Objects.requireNonNull(email, "email cannot be null");
                this.landlineNumber = Objects.requireNonNull(landlineNumber, "landlineNumber cannot be null");
                this.mobileNumber = Objects.requireNonNull(mobileNumber, "mobileNumber cannot be null");
            }

            /**
             * Returns the officer's full name that drives the name cell of the grievance-officer row.
             *
             * @return the name; never {@code null}
             */
            public String name() {
                return name;
            }

            /**
             * Returns the officer's email that drives the email cell of the grievance-officer row.
             *
             * @return the email; never {@code null}
             */
            public String email() {
                return email;
            }

            /**
             * Returns the officer's landline phone number that drives the landline-number cell of the grievance-officer row.
             *
             * @return the number; never {@code null}
             */
            public String landlineNumber() {
                return landlineNumber;
            }

            /**
             * Returns the officer's mobile phone number that drives the mobile-number cell of the grievance-officer row.
             *
             * @return the number; never {@code null}
             */
            public String mobileNumber() {
                return mobileNumber;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (BusinessGrievanceOfficerDetails) obj;
                return Objects.equals(this.name, that.name)
                        && Objects.equals(this.email, that.email)
                        && Objects.equals(this.landlineNumber, that.landlineNumber)
                        && Objects.equals(this.mobileNumber, that.mobileNumber);
            }

            @Override
            public int hashCode() {
                return Objects.hash(name, email, landlineNumber, mobileNumber);
            }

            @Override
            public String toString() {
                return "IqGetMerchantComplianceResponse.Success.BusinessGrievanceOfficerDetails[name="
                        + name + ", email=" + email + ']';
            }
        }

        /**
         * Holds the decoded merchant-compliance entries in wire order.
         */
        private final List<MerchantInfo> entries;

        /**
         * Constructs a typed success reply from the decoded {@code result} envelope.
         *
         * <p>An empty list marks a relay reply that carried no {@code <merchant_info/>} children.
         *
         * @param entries the compliance entries; never {@code null}
         * @throws NullPointerException if {@code entries} is {@code null}
         */
        public Success(List<MerchantInfo> entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            this.entries = List.copyOf(entries);
        }

        /**
         * Returns the merchant-compliance entries.
         *
         * <p>The list preserves the wire order, which mirrors the order of the merchant JIDs
         * supplied to the request.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<MerchantInfo> entries() {
            return entries;
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a {@code result} envelope matching
         * the original request.
         *
         * @implNote
         * This implementation mirrors the deprecated WAP parser inside {@code WAWebMerchantComplianceJob.merchantComplianceResponse}:
         * each {@code <merchant_info/>} child contributes a {@link MerchantInfo} where the
         * entity-name, entity-type and entity-type-custom fields default to the empty string when
         * omitted, the registered flag is decoded from the {@code is_registered="true"} attribute
         * string, and the customer-care and grievance-officer blocks default to all-empty-string
         * when the carrier child is absent.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebMerchantComplianceJob",
                exports = "merchantComplianceResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var entries = new ArrayList<MerchantInfo>();
            for (var miNode : stanza.getChildren("merchant_info")) {
                var entityName = miNode.getChild("entity_name")
                        .flatMap(Stanza::toContentString).orElse("");
                var entityType = miNode.getChild("entity_type")
                        .flatMap(Stanza::toContentString).orElse("");
                var entityTypeCustom = miNode.getChild("entity_type_custom")
                        .flatMap(Stanza::toContentString).orElse(null);
                var registered = miNode.getAttributeAsString("is_registered")
                        .map("true"::equals).orElse(false);
                var ccdNode = miNode.getChild("customer_care_details").orElse(null);
                var ccd = parseContactDetails(ccdNode);
                var godNode = miNode.getChild("grievance_officer_details").orElse(null);
                var godName = godNode == null
                        ? ""
                        : godNode.getChild("name").flatMap(Stanza::toContentString).orElse("");
                var godEmail = godNode == null
                        ? ""
                        : godNode.getChild("email").flatMap(Stanza::toContentString).orElse("");
                var godLandline = godNode == null
                        ? ""
                        : godNode.getChild("landline_number").flatMap(Stanza::toContentString).orElse("");
                var godMobile = godNode == null
                        ? ""
                        : godNode.getChild("mobile_number").flatMap(Stanza::toContentString).orElse("");
                var god = new BusinessGrievanceOfficerDetails(godName, godEmail, godLandline, godMobile);
                entries.add(new MerchantInfo(entityName, entityType, registered,
                        entityTypeCustom, ccd, god));
            }
            if (entries.isEmpty()) {
                return Optional.of(new Success(Collections.emptyList()));
            }
            return Optional.of(new Success(entries));
        }

        /**
         * Decodes a contact-details carrier stanza into a {@link BusinessContactDetails} triple.
         *
         * <p>Folds a {@code <customer_care_details/>} or any other carrier of {@code <email>},
         * {@code <landline_number>} and {@code <mobile_number>} children into the typed model.
         *
         * @implNote
         * This implementation defaults each missing field to the empty string per the WA Web
         * parser; a {@code null} carrier produces an all-empty-string triple rather than a
         * {@code null} result, so the merchant-info row always has a non-{@code null} contact block.
         *
         * @param contactStanza the carrier stanza; may be {@code null}
         * @return the typed triple; never {@code null}
         */
        private static BusinessContactDetails parseContactDetails(Stanza contactStanza) {
            var email = contactStanza == null
                    ? ""
                    : contactStanza.getChild("email").flatMap(Stanza::toContentString).orElse("");
            var landline = contactStanza == null
                    ? ""
                    : contactStanza.getChild("landline_number").flatMap(Stanza::toContentString).orElse("");
            var mobile = contactStanza == null
                    ? ""
                    : contactStanza.getChild("mobile_number").flatMap(Stanza::toContentString).orElse("");
            return new BusinessContactDetails(email, landline, mobile);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.entries, that.entries);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entries);
        }

        @Override
        public String toString() {
            return "IqGetMerchantComplianceResponse.Success[entries=" + entries + ']';
        }
    }

    /**
     * Surfaces a client-side rejection of a merchant-compliance fetch.
     *
     * <p>Typical examples include the relay returning a code-451 sanctions block or a validation
     * rejection on a malformed merchant JID.
     */
    final class ClientError implements IqGetMerchantComplianceResponse {
        /**
         * Holds the numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed client-error reply from a decoded client-error envelope.
         *
         * <p>A {@code null} {@code errorText} marks a wire shape that omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation, when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a client-error envelope matching
         * the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqGetMerchantComplianceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Surfaces a server-side failure that did not produce typed compliance bundles.
     *
     * <p>WA Web's {@code WAWebMerchantComplianceJob.getMerchantCompliance} surfaces this as a
     * {@code ServerStatusCodeError} carrying the relay-supplied status.
     */
    final class ServerError implements IqGetMerchantComplianceResponse {
        /**
         * Holds the numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed server-error reply from a decoded server-error envelope.
         *
         * <p>A {@code null} {@code errorText} marks a wire shape that omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation, when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a server-error envelope matching
         * the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ServerError) obj;
            return this.errorCode == that.errorCode && Objects.equals(this.errorText, that.errorText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqGetMerchantComplianceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
