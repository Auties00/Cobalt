package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.business.profile.WebBizProfileInput;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that edits a WhatsApp Business profile.
 *
 * <p>The mutation takes two GraphQL variables: a {@code lid} {@link Jid} naming the business account to
 * edit and an {@code input} {@link WebBizProfileInput} carrying the changed fields. WhatsApp Web's
 * {@code WAWebBizRefreshedProfileDrawer} builds the input for {@code editBizProfile(lid, input)} with
 * the fields the merchant actually changed: the {@code description}, {@code email}, {@code address},
 * the coarse {@code price_tier}, the {@code latitude} and {@code longitude} of the business location,
 * the {@code websites} list, and the {@code service_areas} the business delivers to. The Meta graph
 * endpoint returns the edit outcome under the scalar {@code edit_wa_web_biz_profile}; the reply is
 * consumed through {@link EditBizProfileFacebookGraphQlResponse}.
 *
 * @implNote This implementation maps the typed {@link WebBizProfileInput} to its snake_case JSON shape,
 * writing each scalar only when present, the {@code websites} array only when non-empty, and the
 * {@code service_areas} array only when non-empty; {@code price_tier} is rendered as its
 * {@link com.github.auties00.cobalt.wire.linked.business.profile.BusinessProfilePriceTier#wireValue() wire
 * literal}.
 *
 * @see EditBizProfileFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebEditBizProfileMutation")
public final class EditBizProfileFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebEditBizProfileMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26652989367627867";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebEditBizProfileMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebEditBizProfileMutation";

    /**
     * The {@code lid} GraphQL variable naming the business account to edit, or {@code null} to omit
     * it.
     */
    private final Jid lid;

    /**
     * The {@code input} GraphQL variable carrying the changed profile fields, or {@code null} to omit
     * it.
     */
    private final WebBizProfileInput input;

    /**
     * Constructs an edit-business-profile mutation request.
     *
     * <p>The {@code lid} names the business account to edit and {@code input} carries the changed
     * profile fields. Each value that is {@code null} omits its variable from the serialized object.
     *
     * @param lid   the business account {@link Jid} to edit, or {@code null} to omit the variable
     * @param input the changed profile fields, or {@code null} to omit the variable
     */
    public EditBizProfileFacebookGraphQlRequest(Jid lid, WebBizProfileInput input) {
        this.lid = lid;
        this.input = input;
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
     * @implNote This implementation emits {@code {"lid": <lid>, "input": {"description": ..., "email":
     * ..., "address": ..., "price_tier": ..., "latitude": ..., "longitude": ..., "websites": [...],
     * "service_areas": [{"radius_meters": ..., "center_latitude": ..., "center_longitude": ...,
     * "description": ...}]}}}, writing {@code lid} and each populated {@code input} field only when
     * present (and the {@code websites} and {@code service_areas} arrays only when non-empty), and
     * emitting {@code "{}"} when both variables are absent.
     */
    @WhatsAppWebExport(moduleName = "WAWebEditBizProfileMutation", exports = "editBizProfile",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (lid != null) {
                writer.writeName("lid");
                writer.writeColon();
                writer.writeString(lid.toString());
            }

            if (input != null) {
                writer.writeName("input");
                writer.writeColon();
                writer.startObject();
                input.description().ifPresent(value -> {
                    writer.writeName("description");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.email().ifPresent(value -> {
                    writer.writeName("email");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.address().ifPresent(value -> {
                    writer.writeName("address");
                    writer.writeColon();
                    writer.writeString(value);
                });
                input.priceTier().ifPresent(tier -> {
                    writer.writeName("price_tier");
                    writer.writeColon();
                    writer.writeString(tier.wireValue());
                });
                input.latitude().ifPresent(value -> {
                    writer.writeName("latitude");
                    writer.writeColon();
                    writer.writeDouble(value);
                });
                input.longitude().ifPresent(value -> {
                    writer.writeName("longitude");
                    writer.writeColon();
                    writer.writeDouble(value);
                });
                var websites = input.websites();
                if (!websites.isEmpty()) {
                    writer.writeName("websites");
                    writer.writeColon();
                    writer.startArray();
                    for (var i = 0; i < websites.size(); i++) {
                        if (i > 0) {
                            writer.writeComma();
                        }
                        writer.writeString(websites.get(i));
                    }
                    writer.endArray();
                }
                var serviceAreas = input.serviceAreas();
                if (!serviceAreas.isEmpty()) {
                    writer.writeName("service_areas");
                    writer.writeColon();
                    writer.startArray();
                    for (var i = 0; i < serviceAreas.size(); i++) {
                        if (i > 0) {
                            writer.writeComma();
                        }
                        var area = serviceAreas.get(i);
                        writer.startObject();
                        area.radiusMeters().ifPresent(value -> {
                            writer.writeName("radius_meters");
                            writer.writeColon();
                            writer.writeInt32(value);
                        });
                        area.centerLatitude().ifPresent(value -> {
                            writer.writeName("center_latitude");
                            writer.writeColon();
                            writer.writeDouble(value);
                        });
                        area.centerLongitude().ifPresent(value -> {
                            writer.writeName("center_longitude");
                            writer.writeColon();
                            writer.writeDouble(value);
                        });
                        area.description().ifPresent(value -> {
                            writer.writeName("description");
                            writer.writeColon();
                            writer.writeString(value);
                        });
                        writer.endObject();
                    }
                    writer.endArray();
                }
                writer.endObject();
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
}
