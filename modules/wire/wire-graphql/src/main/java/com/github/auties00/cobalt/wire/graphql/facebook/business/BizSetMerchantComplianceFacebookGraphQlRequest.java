package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessContactDetails;
import com.github.auties00.cobalt.wire.linked.business.compliance.BusinessGrievanceOfficerDetails;
import com.github.auties00.cobalt.wire.linked.business.compliance.MerchantComplianceEdit;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that sets a WhatsApp Business merchant's compliance information.
 *
 * <p>The single {@code input} GraphQL variable is the merchant compliance information object. Its
 * field set mirrors the {@link MerchantComplianceEdit} model: an entity name and entity-type code
 * (with an optional custom-type description), a registered-business flag, and customer-care and
 * grievance-officer contact blocks. The Meta graph endpoint returns the persisted compliance projection under
 * {@code xfb_whatsapp_biz_merchant_set_compliance_info}; the reply is consumed through
 * {@link BizSetMerchantComplianceFacebookGraphQlResponse}.
 *
 * @implNote This implementation builds the {@code input} object from the typed
 * {@link MerchantComplianceEdit} model, emitting only the fields the caller populated. The wire-level
 * field names ({@code entity_name}, {@code entity_type}, {@code is_registered},
 * {@code entity_type_custom}, {@code customer_care_details}, {@code grievance_officer_details}) match
 * the selections of the {@code WAWebBizSetMerchantComplianceMutation.graphql} document.
 *
 * @see BizSetMerchantComplianceFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizSetMerchantComplianceMutation")
public final class BizSetMerchantComplianceFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizSetMerchantComplianceMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25188352884120072";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizSetMerchantComplianceMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizSetMerchantComplianceMutation";

    /**
     * The typed merchant-compliance edit carrying the fields to persist, or {@code null} to omit the
     * {@code input} variable entirely.
     */
    private final MerchantComplianceEdit edit;

    /**
     * Constructs a set-merchant-compliance mutation request.
     *
     * <p>The {@code edit} carries the typed merchant-compliance fields to persist. Each
     * {@link MerchantComplianceEdit} field that is unset is omitted from the serialized
     * {@code input} object so the server treats the persisted disclosure's existing value as
     * untouched. A {@code null} {@code edit} omits the {@code input} variable from the serialized
     * variables object entirely.
     *
     * @param edit the typed merchant-compliance edit, or {@code null} to omit the variable
     */
    public BizSetMerchantComplianceFacebookGraphQlRequest(MerchantComplianceEdit edit) {
        this.edit = edit;
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
     * @implNote This implementation emits {@code {"input": {...}}}, writing the {@code input}
     * variable only when {@code edit} is non-null and emitting {@code "{}"} otherwise. Inside the
     * {@code input} object every {@link MerchantComplianceEdit} field that is unset is omitted from
     * the serialized payload; the {@code is_registered} flag is always emitted because it is a
     * primitive {@code boolean} on the typed model.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizSetMerchantCompliance", exports = "setMerchantCompliance",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (edit != null) {
                writer.writeName("input");
                writer.writeColon();
                writeInput(writer, edit);
            }
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
     * Emits the merchant-compliance {@code input} object on the given JSON writer.
     *
     * @param writer the JSON writer the object is emitted on
     * @param edit   the typed merchant-compliance edit being serialized
     */
    private static void writeInput(JSONWriter writer, MerchantComplianceEdit edit) {
        writer.startObject();
        edit.entityName().ifPresent(value -> {
            writer.writeName("entity_name");
            writer.writeColon();
            writer.writeString(value);
        });
        edit.entityType().ifPresent(value -> {
            writer.writeName("entity_type");
            writer.writeColon();
            writer.writeString(value.data());
        });
        writer.writeName("is_registered");
        writer.writeColon();
        writer.writeBool(edit.registered());
        edit.entityTypeCustom().ifPresent(value -> {
            writer.writeName("entity_type_custom");
            writer.writeColon();
            writer.writeString(value);
        });
        edit.customerCareDetails().ifPresent(value -> {
            writer.writeName("customer_care_details");
            writer.writeColon();
            writeCustomerCare(writer, value);
        });
        edit.grievanceOfficerDetails().ifPresent(value -> {
            writer.writeName("grievance_officer_details");
            writer.writeColon();
            writeGrievanceOfficer(writer, value);
        });
        writer.endObject();
    }

    /**
     * Emits the customer-care contact block on the given JSON writer.
     *
     * @param writer  the JSON writer the object is emitted on
     * @param details the typed customer-care block being serialized
     */
    private static void writeCustomerCare(JSONWriter writer, BusinessContactDetails details) {
        writer.startObject();
        details.email().ifPresent(value -> {
            writer.writeName("email");
            writer.writeColon();
            writer.writeString(value);
        });
        details.landlineNumber().ifPresent(value -> {
            writer.writeName("landline_number");
            writer.writeColon();
            writer.writeString(value);
        });
        details.mobileNumber().ifPresent(value -> {
            writer.writeName("mobile_number");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }

    /**
     * Emits the grievance-officer contact block on the given JSON writer.
     *
     * @param writer  the JSON writer the object is emitted on
     * @param details the typed grievance-officer block being serialized
     */
    private static void writeGrievanceOfficer(JSONWriter writer, BusinessGrievanceOfficerDetails details) {
        writer.startObject();
        details.name().ifPresent(value -> {
            writer.writeName("name");
            writer.writeColon();
            writer.writeString(value);
        });
        details.email().ifPresent(value -> {
            writer.writeName("email");
            writer.writeColon();
            writer.writeString(value);
        });
        details.landlineNumber().ifPresent(value -> {
            writer.writeName("landline_number");
            writer.writeColon();
            writer.writeString(value);
        });
        details.mobileNumber().ifPresent(value -> {
            writer.writeName("mobile_number");
            writer.writeColon();
            writer.writeString(value);
        });
        writer.endObject();
    }
}
