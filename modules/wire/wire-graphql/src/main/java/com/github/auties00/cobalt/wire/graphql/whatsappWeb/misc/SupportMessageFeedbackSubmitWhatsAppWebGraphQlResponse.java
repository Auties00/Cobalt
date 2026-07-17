package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.support.SupportMessageFeedbackSubmission;
import com.github.auties00.cobalt.wire.linked.business.support.SupportMessageFeedbackSubmissionBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the submit-support-message-feedback mutation built by
 * {@link SupportMessageFeedbackSubmitWhatsAppWebGraphQlRequest} into a {@link SupportMessageFeedbackSubmission}.
 *
 * <p>Reads the linked root {@code xwa_wa_support_message_feedback_submit} and projects its
 * {@code success} verdict and the optional {@code error_code} and {@code error_message} fields onto
 * the Cobalt domain model.
 *
 * @see SupportMessageFeedbackSubmitWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebSupportMessageFeedbackSubmitMutation")
public final class SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed submission.
     */
    private final SupportMessageFeedbackSubmission submission;

    /**
     * Constructs a response wrapping the parsed submission.
     *
     * <p>Reserved for the static parser.
     *
     * @param submission the parsed submission, or {@code null} when the relay omitted the field
     */
    private SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse(SupportMessageFeedbackSubmission submission) {
        this.submission = submission;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_wa_support_message_feedback_submit} and projects it onto
     * a {@link SupportMessageFeedbackSubmission}; the returned {@link Optional} is empty when
     * {@code data} or the submission object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the submission object is missing
     */
    public static Optional<SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_wa_support_message_feedback_submit");
        if (node == null) {
            return Optional.empty();
        }

        var success = node.getBoolean("success");
        var submission = new SupportMessageFeedbackSubmissionBuilder()
                .success(success != null && success)
                .errorCode(node.getString("error_code"))
                .errorMessage(node.getString("error_message"))
                .build();
        return Optional.of(new SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse(submission));
    }

    /**
     * Returns the parsed submission.
     *
     * @return the parsed {@link SupportMessageFeedbackSubmission}, never {@code null}
     */
    public SupportMessageFeedbackSubmission submission() {
        return submission;
    }
}
