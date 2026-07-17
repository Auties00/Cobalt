package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiCapturedLead;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiCapturedLeadBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenField;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenFieldBuilder;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenForm;
import com.github.auties00.cobalt.wire.linked.business.ai.BusinessAiLeadGenFormBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the Facebook GraphQL response of the list-lead-gen-flows query built by
 * {@link BizAiLeadGenFormsFacebookGraphQlRequest} into a list of {@link BusinessAiLeadGenForm}.
 *
 * <p>Projects the plural root {@code xfb_maiba_gen_lead_gen_flow} onto one {@link BusinessAiLeadGenForm}
 * per flow, flattening each flow's configured capture fields, its Relay-style captured-lead connection
 * (with the leads' consumer phone-number and LID addresses), and its unseen-lead summary onto the
 * domain shape.
 *
 * @see BizAiLeadGenFormsFacebookGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebBizAiLeadGenFormsQuery")
public final class BizAiLeadGenFormsFacebookGraphQlResponse implements FacebookGraphQlOperation.Response {
    /**
     * Holds the projected lead-capture flows.
     */
    private final List<BusinessAiLeadGenForm> forms;

    /**
     * Constructs a response wrapping the projected lead-capture flows.
     *
     * <p>Reserved for the static parser.
     *
     * @param forms the projected lead-capture flows
     */
    private BizAiLeadGenFormsFacebookGraphQlResponse(List<BusinessAiLeadGenForm> forms) {
        this.forms = forms;
    }

    /**
     * Parses the Facebook GraphQL response from the unwrapped GraphQL {@code data} object and projects each flow
     * onto a {@link BusinessAiLeadGenForm}.
     *
     * <p>Reads the plural root {@code xfb_maiba_gen_lead_gen_flow}; the returned {@link Optional} is
     * empty when {@code data} is {@code null}.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code FacebookGraphQlClient#send(FacebookGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} is {@code null}
     */
    public static Optional<BizAiLeadGenFormsFacebookGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var forms = parseForms(data.getJSONArray("xfb_maiba_gen_lead_gen_flow"));
        return Optional.of(new BizAiLeadGenFormsFacebookGraphQlResponse(forms));
    }

    /**
     * Projects the {@code xfb_maiba_gen_lead_gen_flow} array onto a list of {@link BusinessAiLeadGenForm}.
     *
     * @param arr the {@code xfb_maiba_gen_lead_gen_flow} array, possibly {@code null}
     * @return the projected flows, never {@code null}
     */
    private static List<BusinessAiLeadGenForm> parseForms(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiLeadGenForm>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            var numOfLeadData = obj.getLong("num_of_lead_data");
            result.add(new BusinessAiLeadGenFormBuilder()
                    .id(obj.getString("id"))
                    .moment(obj.getString("custom_moment"))
                    .momentType(obj.getString("moment_type"))
                    .fields(parseFields(obj.getJSONArray("fields")))
                    .capturedLeads(parseLeads(obj.getJSONObject("lead_data")))
                    .hasUnseenLeads(Boolean.TRUE.equals(obj.getBoolean("has_unseen_lead_data")))
                    .totalLeadCount(numOfLeadData != null ? numOfLeadData : 0L)
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code fields} array onto a list of {@link BusinessAiLeadGenField}.
     *
     * @param arr the {@code fields} array, possibly {@code null}
     * @return the projected capture fields, never {@code null}
     */
    private static List<BusinessAiLeadGenField> parseFields(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiLeadGenField>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            result.add(new BusinessAiLeadGenFieldBuilder()
                    .label(obj.getString("label"))
                    .enabled(Boolean.TRUE.equals(obj.getBoolean("is_enabled")))
                    .build());
        }
        return result;
    }

    /**
     * Flattens the Relay-style {@code lead_data} connection onto a list of {@link BusinessAiCapturedLead},
     * resolving each lead's consumer phone-number and LID addresses.
     *
     * @param connection the {@code lead_data} connection object, possibly {@code null}
     * @return the projected captured leads, never {@code null}
     */
    private static List<BusinessAiCapturedLead> parseLeads(JSONObject connection) {
        if (connection == null) {
            return List.of();
        }

        var edges = connection.getJSONArray("edges");
        if (edges == null) {
            return List.of();
        }

        var result = new ArrayList<BusinessAiCapturedLead>(edges.size());
        for (var i = 0; i < edges.size(); i++) {
            var edge = edges.getJSONObject(i);
            if (edge == null) {
                continue;
            }
            var node = edge.getJSONObject("node");
            if (node == null) {
                continue;
            }
            var consumerUid = node.getJSONObject("consumer_uid");
            var phoneNumber = parseJid(consumerUid, "pn");
            var lid = parseJid(consumerUid, "lid");
            var creationTime = node.getLong("creation_time");
            result.add(new BusinessAiCapturedLeadBuilder()
                    .id(node.getString("id"))
                    .seen(Boolean.TRUE.equals(node.getBoolean("has_seen")))
                    .customerInfo(node.getString("customer_info"))
                    .phoneNumber(phoneNumber)
                    .lid(lid)
                    .capturedAt(creationTime != null ? Instant.ofEpochSecond(creationTime) : null)
                    .build());
        }
        return result;
    }

    /**
     * Resolves a WhatsApp address from the named field of the {@code consumer_uid} sub-object.
     *
     * @param consumerUid the {@code consumer_uid} sub-object, possibly {@code null}
     * @param key         the address field name to read
     * @return the resolved {@link Jid}, or {@code null} when the field is absent
     */
    private static Jid parseJid(JSONObject consumerUid, String key) {
        if (consumerUid == null) {
            return null;
        }
        var value = consumerUid.getString(key);
        return value == null ? null : Jid.of(value);
    }

    /**
     * Returns the projected lead-capture flows.
     *
     * @return the projected flows, empty when the Meta graph endpoint returned none
     */
    public List<BusinessAiLeadGenForm> forms() {
        return forms;
    }
}
