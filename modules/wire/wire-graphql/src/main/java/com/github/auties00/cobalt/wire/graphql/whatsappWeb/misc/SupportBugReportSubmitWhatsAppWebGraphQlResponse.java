package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.support.SupportBugReportSubmission;
import com.github.auties00.cobalt.wire.linked.business.support.SupportBugReportSubmissionBuilder;

import java.util.Optional;

/**
 * Parses the WhatsApp Web GraphQL response of the submit-bug-report mutation built by
 * {@link SupportBugReportSubmitWhatsAppWebGraphQlRequest} into a {@link SupportBugReportSubmission}.
 *
 * <p>Reads the linked root {@code xwa_wa_support_bug_report_submit} and projects its
 * {@code success} verdict, the optional {@code error_code} and {@code error_message} fields, and
 * the assigned {@code bug_report_id} and {@code task_id} onto the Cobalt domain model.
 *
 * @see SupportBugReportSubmitWhatsAppWebGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebSupportBugReportSubmitMutation")
public final class SupportBugReportSubmitWhatsAppWebGraphQlResponse implements WhatsAppWebGraphQlOperation.Response {
    /**
     * Holds the parsed submission.
     */
    private final SupportBugReportSubmission submission;

    /**
     * Constructs a response wrapping the parsed submission.
     *
     * <p>Reserved for the static parser.
     *
     * @param submission the parsed submission, or {@code null} when the relay omitted the field
     */
    private SupportBugReportSubmitWhatsAppWebGraphQlResponse(SupportBugReportSubmission submission) {
        this.submission = submission;
    }

    /**
     * Parses the WhatsApp Web GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code xwa_wa_support_bug_report_submit} and projects it onto a
     * {@link SupportBugReportSubmission}; the returned {@link Optional} is empty when {@code data}
     * or the submission object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppWebGraphQlClient#send(WhatsAppWebGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the submission object is missing
     */
    public static Optional<SupportBugReportSubmitWhatsAppWebGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("xwa_wa_support_bug_report_submit");
        if (node == null) {
            return Optional.empty();
        }

        var success = node.getBoolean("success");
        var submission = new SupportBugReportSubmissionBuilder()
                .success(success != null && success)
                .errorCode(node.getLong("error_code"))
                .errorMessage(node.getString("error_message"))
                .bugReportId(node.getString("bug_report_id"))
                .taskId(node.getString("task_id"))
                .build();
        return Optional.of(new SupportBugReportSubmitWhatsAppWebGraphQlResponse(submission));
    }

    /**
     * Returns the parsed submission.
     *
     * @return the parsed {@link SupportBugReportSubmission}, never {@code null}
     */
    public SupportBugReportSubmission submission() {
        return submission;
    }
}
