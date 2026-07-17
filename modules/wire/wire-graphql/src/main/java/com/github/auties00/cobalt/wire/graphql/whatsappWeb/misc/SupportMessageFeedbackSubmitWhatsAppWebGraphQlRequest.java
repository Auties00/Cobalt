package com.github.auties00.cobalt.wire.graphql.whatsappWeb.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * Builds the relay mutation that submits feedback on a WhatsApp support (Saga) assistant message.
 *
 * <p>The single {@code input} GraphQL variable is the feedback payload. WhatsApp Web's
 * {@code WAWebSendSupportBotFeedbackActions} fills it with the stanza id of the message being rated
 * ({@code message_id}) and the list of feedback kinds ({@code feedback_types}), each mapped through
 * {@code WAWebSupportMessageFeedbackSubmitMutation.feedbackKindForGraphQL} to a closed wire token; the
 * relay returns the submission outcome under {@code xwa_wa_support_message_feedback_submit}. The reply
 * is consumed through {@link SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse}.
 *
 * @see SupportMessageFeedbackSubmitWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebSupportMessageFeedbackSubmitMutation")
public final class SupportMessageFeedbackSubmitWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportMessageFeedbackSubmitMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "25772720305756789";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportMessageFeedbackSubmitMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebSupportMessageFeedbackSubmitMutation";

    /**
     * The closed set of feedback kinds the relay accepts under the {@code feedback_types} field of the
     * {@code input} object.
     *
     * <p>The constants and their wire tokens mirror the
     * {@code WAWebSupportMessageFeedbackSubmitMutation.feedbackKindForGraphQL} map of snapshot
     * {@code 1040120866}: {@code positive -> POSITIVE}, {@code negative_harmful -> NEGATIVE_HARMFUL},
     * {@code negative_inaccurate -> NEGATIVE_INACCURATE}, {@code negative_irrelevant ->
     * NEGATIVE_IRRELEVANT}, {@code negative_other -> NEGATIVE_OTHER}, {@code negative_repetitive ->
     * NEGATIVE_REPETITIVE}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSupportMessageFeedbackSubmitMutation", exports = "feedbackKindForGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public enum FeedbackType {
        /**
         * Positive feedback on the assistant message.
         */
        POSITIVE("POSITIVE"),

        /**
         * Negative feedback flagging the assistant message as harmful.
         */
        NEGATIVE_HARMFUL("NEGATIVE_HARMFUL"),

        /**
         * Negative feedback flagging the assistant message as inaccurate.
         */
        NEGATIVE_INACCURATE("NEGATIVE_INACCURATE"),

        /**
         * Negative feedback flagging the assistant message as irrelevant.
         */
        NEGATIVE_IRRELEVANT("NEGATIVE_IRRELEVANT"),

        /**
         * Negative feedback that does not fit the other categories.
         */
        NEGATIVE_OTHER("NEGATIVE_OTHER"),

        /**
         * Negative feedback flagging the assistant message as repetitive.
         */
        NEGATIVE_REPETITIVE("NEGATIVE_REPETITIVE");

        /**
         * The wire token emitted for this feedback kind.
         */
        private final String token;

        /**
         * Constructs a feedback kind bound to its wire token.
         *
         * @param token the wire token emitted for this kind
         */
        FeedbackType(String token) {
            this.token = token;
        }

        /**
         * Returns the wire token emitted for this feedback kind.
         *
         * @return the wire token, never {@code null}
         */
        public String token() {
            return token;
        }

        /**
         * Resolves a feedback kind from its wire token.
         *
         * <p>Performs a lenient lookup that maps an unrecognised or {@code null} token to
         * {@link Optional#empty()} rather than throwing, so an unknown server token does not fail the
         * caller.
         *
         * @param token the wire token to resolve, may be {@code null}
         * @return the matching {@link FeedbackType}, or empty when {@code token} is {@code null} or
         *         unrecognised
         */
        public static Optional<FeedbackType> of(String token) {
            if (token == null) {
                return Optional.empty();
            }

            for (var value : values()) {
                if (value.token.equals(token)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * The {@code message_id} field of the {@code input} object naming the assistant message being
     * rated, or {@code null} to omit it.
     *
     * <p>A WhatsApp stanza (message) id rather than a WhatsApp address, so it is carried as a plain
     * {@link String}.
     */
    private final String messageId;

    /**
     * The {@code feedback_types} field of the {@code input} object listing the feedback kinds, or
     * {@code null} to omit it.
     */
    private final List<FeedbackType> feedbackTypes;

    /**
     * Constructs a submit-support-message-feedback mutation request.
     *
     * <p>The {@code messageId} names the assistant message being rated and {@code feedbackTypes} lists
     * the feedback kinds; each value that is {@code null} omits its field from the serialized
     * {@code input} object.
     *
     * @param messageId     the stanza id of the message being rated, or {@code null} to omit the field
     * @param feedbackTypes the feedback kinds to report, or {@code null} to omit the field
     */
    public SupportMessageFeedbackSubmitWhatsAppWebGraphQlRequest(String messageId, List<FeedbackType> feedbackTypes) {
        this.messageId = messageId;
        this.feedbackTypes = feedbackTypes;
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
     * @implNote This implementation emits {@code {"input": {"message_id": <messageId>,
     * "feedback_types": [<token>, ...]}}}, writing each field only when its value is non-null,
     * rendering each {@link FeedbackType} as its wire {@link FeedbackType#token()}, and emitting
     * {@code {"input": {}}} when both are {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebSendSupportBotFeedbackActions", exports = "default",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (messageId != null) {
                writer.writeName("message_id");
                writer.writeColon();
                writer.writeString(messageId);
            }

            if (feedbackTypes != null) {
                writer.writeName("feedback_types");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < feedbackTypes.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(feedbackTypes.get(i).token());
                }
                writer.endArray();
            }
            writer.endObject();
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
