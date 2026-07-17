package com.github.auties00.cobalt.wire.graphql.whatsappWeb.user;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.support.SupportContactFormSubmission;
import com.github.auties00.cobalt.wire.linked.business.support.SupportContactFormSubmissionBuilder;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the support-contact-form-submit mutation built by
 * {@link SupportContactFormSubmitWhatsAppWebGraphQlRequest} into a {@link SupportContactFormSubmission}.
 *
 * <p>Reads the linked {@code xwa_wa_support_contact_form_submit} root and projects its
 * {@code success}, {@code error_code}, {@code error_message}, {@code ticket_id}, and
 * {@code support_phone_number_jid} fields onto a {@link SupportContactFormSubmission}.
 *
 * @see SupportContactFormSubmitWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebSupportContactFormSubmitMutation")
public final class SupportContactFormSubmitWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed submission outcome.
     */
    private final SupportContactFormSubmission submission;

    /**
     * Constructs a response wrapping the parsed submission outcome.
     *
     * <p>Reserved for the static parser.
     *
     * @param submission the parsed submission outcome
     */
    private SupportContactFormSubmitWhatsAppWebGraphQlResponse(SupportContactFormSubmission submission) {
        this.submission = submission;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked {@code xwa_wa_support_contact_form_submit} root and projects it onto a
     * {@link SupportContactFormSubmission}; the returned {@link Optional} is empty when
     * {@code data} or the root is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the root is missing
     */
    public static Optional<SupportContactFormSubmitWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var root = data.getJSONObject("xwa_wa_support_contact_form_submit");
        if (root == null) {
            return Optional.empty();
        }

        var success = root.getBoolean("success");
        var jidString = root.getString("support_phone_number_jid");
        var submission = new SupportContactFormSubmissionBuilder()
                .success(success != null && success)
                .errorCode(root.getInteger("error_code"))
                .errorMessage(root.getString("error_message"))
                .ticketId(root.getString("ticket_id"))
                .supportPhoneNumber(jidString == null ? null : Jid.of(jidString))
                .build();
        return Optional.of(new SupportContactFormSubmitWhatsAppWebGraphQlResponse(submission));
    }

    /**
     * Returns the parsed submission outcome.
     *
     * @return the parsed {@link SupportContactFormSubmission}, never {@code null}
     */
    public SupportContactFormSubmission submission() {
        return submission;
    }
}
