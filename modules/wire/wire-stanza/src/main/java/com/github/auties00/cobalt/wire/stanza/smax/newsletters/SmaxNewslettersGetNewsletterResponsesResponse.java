package com.github.auties00.cobalt.wire.stanza.smax.newsletters;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the inbound reply variants for a {@link SmaxNewslettersGetNewsletterResponsesRequest}.
 *
 * <p>The reply is one of {@link Success}, {@link ClientError}, or {@link ServerError}. A non-admin
 * caller is rejected through the {@link ClientError} variant.</p>
 */
public sealed interface SmaxNewslettersGetNewsletterResponsesResponse extends SmaxStanza.Response
        permits SmaxNewslettersGetNewsletterResponsesResponse.Success, SmaxNewslettersGetNewsletterResponsesResponse.ClientError, SmaxNewslettersGetNewsletterResponsesResponse.ServerError {

    /**
     * Dispatches the inbound IQ stanza to the first matching variant parser.
     *
     * <p>Tries {@link Success}, then {@link ClientError}, then {@link ServerError} in order; returns
     * {@link Optional#empty()} when none match.</p>
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on no-match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxNewslettersGetNewsletterResponsesRPC",
            exports = "sendGetNewsletterResponsesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxNewslettersGetNewsletterResponsesResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the requested response slice returned by the relay.
     *
     * <p>Each {@link #questionResponses()} entry's underlying {@link Stanza} exposes the
     * variable-shape content-type and payload children for downstream rendering.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseSuccess")
    final class Success implements SmaxNewslettersGetNewsletterResponsesResponse {
        /**
         * The newsletter {@link Jid} echoed by the relay on the {@code <iq from>} attribute.
         */
        private final Jid from;

        /**
         * The echoed server-id of the question whose responses are carried in this slice.
         */
        private final long questionResponsesServerId;

        /**
         * The list of question-response entries returned by the relay.
         */
        private final List<QuestionResponse> questionResponses;

        /**
         * Constructs a new successful reply.
         *
         * <p>Both {@code from} and {@code questionResponses} are required; an absent {@code from} on
         * the wire breaks the parser upstream.</p>
         *
         * @param from                      the echoed newsletter {@link Jid}; never {@code null}
         * @param questionResponsesServerId the echoed question server-id
         * @param questionResponses         the response entries; never {@code null}
         * @throws NullPointerException if {@code from} or {@code questionResponses} is {@code null}
         */
        public Success(Jid from, long questionResponsesServerId,
                       List<QuestionResponse> questionResponses) {
            this.from = Objects.requireNonNull(from, "from cannot be null");
            this.questionResponsesServerId = questionResponsesServerId;
            this.questionResponses = List.copyOf(Objects.requireNonNull(questionResponses,
                    "questionResponses cannot be null"));
        }

        /**
         * Returns the echoed newsletter {@link Jid}.
         *
         * @return the newsletter {@link Jid}; never {@code null}
         */
        public Jid from() {
            return from;
        }

        /**
         * Returns the echoed question server-id.
         *
         * @return the question server-id
         */
        public long questionResponsesServerId() {
            return questionResponsesServerId;
        }

        /**
         * Returns the response entries.
         *
         * @return an unmodifiable {@link List} of entries; never {@code null}
         */
        public List<QuestionResponse> questionResponses() {
            return questionResponses;
        }

        /**
         * Tries to parse a {@link Success} from the inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the IQ envelope fails
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, the {@code from} attribute is
         * missing, the {@code <question_responses>} envelope is missing, the {@code server_id} is
         * outside {@code [99, 2147476647]}, or any {@code <question_response>} child fails its own
         * {@link QuestionResponse#of(Stanza)} parse.</p>
         *
         * @implNote The {@code [99, 2147476647]} bounds match the server-side validators for a
         * newsletter server-id.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty on no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseSuccess",
                exports = "parseGetNewsletterResponsesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var from = stanza.getAttributeAsJid("from").orElse(null);
            if (from == null) {
                return Optional.empty();
            }
            var questionResponsesNode = stanza.getChild("question_responses").orElse(null);
            if (questionResponsesNode == null) {
                return Optional.empty();
            }
            var serverIdOpt = questionResponsesNode.getAttributeAsLong("server_id");
            if (serverIdOpt.isEmpty()) {
                return Optional.empty();
            }
            var serverId = serverIdOpt.getAsLong();
            if (serverId < 99 || serverId > 2147476647L) {
                return Optional.empty();
            }
            var entries = new ArrayList<QuestionResponse>();
            for (var qr : questionResponsesNode.getChildren("question_response")) {
                var entry = QuestionResponse.of(qr).orElse(null);
                if (entry == null) {
                    return Optional.empty();
                }
                entries.add(entry);
            }
            return Optional.of(new Success(from, serverId, entries));
        }

        /**
         * Compares two replies for value equality on every field.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link Success} carrying equal field values
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return this.questionResponsesServerId == that.questionResponsesServerId
                    && Objects.equals(this.from, that.from)
                    && Objects.equals(this.questionResponses, that.questionResponses);
        }

        /**
         * Returns the hash code derived from every field.
         *
         * @return the combined hash of every field
         */
        @Override
        public int hashCode() {
            return Objects.hash(from, questionResponsesServerId, questionResponses);
        }

        /**
         * Returns a debug representation including every field.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesResponse.Success[from=" + from
                    + ", questionResponsesServerId=" + questionResponsesServerId
                    + ", questionResponses=" + questionResponses + ']';
        }
    }

    /**
     * Projects one {@code <question_response>} entry.
     *
     * <p>Each entry represents a subscriber's free-form reply against a newsletter question post.
     * The underlying {@link Stanza} exposes the variable-shape content-type and payload children for
     * downstream rendering.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersQuestionResponseMessageMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersQuestionResponseSenderMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersQuestionResponseFlagsMixin")
    final class QuestionResponse {
        /**
         * The message stanza id.
         */
        private final String messageId;

        /**
         * The message unix-second timestamp.
         */
        private final long messageTimestamp;

        /**
         * Whether the message was authored by the connected client.
         */
        private final boolean fromSelf;

        /**
         * The optional anonymised sender {@link Jid} in LID form.
         */
        private final Jid senderLid;

        /**
         * The optional sender notify-name.
         */
        private final String senderNotifyName;

        /**
         * The sender's profile-picture direct-path.
         */
        private final String senderPictureDirectPath;

        /**
         * Whether the question owner has explicitly replied to this response.
         */
        private final boolean hasRepliedFlag;

        /**
         * The underlying {@link Stanza} exposing the content and payload children.
         */
        private final Stanza raw;

        /**
         * Constructs a new question-response projection.
         *
         * <p>{@code senderLid} and {@code senderNotifyName} are optional because the relay anonymises
         * subscribers it does not have full identity for; {@code senderPictureDirectPath} is always
         * present per the wire schema.</p>
         *
         * @param messageId               the message stanza id; never {@code null}
         * @param messageTimestamp        the unix-second timestamp
         * @param fromSelf                whether the message was authored by self
         * @param senderLid               the optional sender LID; may be {@code null}
         * @param senderNotifyName        the optional notify-name; may be {@code null}
         * @param senderPictureDirectPath the picture direct-path; never {@code null}
         * @param hasRepliedFlag          whether the {@code <replied/>} marker was present
         * @param raw                     the underlying {@link Stanza}; never {@code null}
         * @throws NullPointerException if {@code messageId}, {@code senderPictureDirectPath}, or
         *                              {@code raw} is {@code null}
         */
        public QuestionResponse(String messageId, long messageTimestamp, boolean fromSelf,
                                Jid senderLid, String senderNotifyName,
                                String senderPictureDirectPath, boolean hasRepliedFlag, Stanza raw) {
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            this.messageTimestamp = messageTimestamp;
            this.fromSelf = fromSelf;
            this.senderLid = senderLid;
            this.senderNotifyName = senderNotifyName;
            this.senderPictureDirectPath = Objects.requireNonNull(senderPictureDirectPath,
                    "senderPictureDirectPath cannot be null");
            this.hasRepliedFlag = hasRepliedFlag;
            this.raw = Objects.requireNonNull(raw, "raw cannot be null");
        }

        /**
         * Returns the message stanza id.
         *
         * @return the stanza id; never {@code null}
         */
        public String messageId() {
            return messageId;
        }

        /**
         * Returns the message unix-second timestamp.
         *
         * @return the timestamp
         */
        public long messageTimestamp() {
            return messageTimestamp;
        }

        /**
         * Returns whether the message was authored by the connected client.
         *
         * @return {@code true} when {@code is_sender="true"} was present on the wire
         */
        public boolean fromSelf() {
            return fromSelf;
        }

        /**
         * Returns the optional anonymised sender LID.
         *
         * @return an {@link Optional} carrying the LID, or empty when the relay omitted it
         */
        public Optional<Jid> senderLid() {
            return Optional.ofNullable(senderLid);
        }

        /**
         * Returns the optional sender notify-name.
         *
         * @return an {@link Optional} carrying the notify-name, or empty when the relay omitted it
         */
        public Optional<String> senderNotifyName() {
            return Optional.ofNullable(senderNotifyName);
        }

        /**
         * Returns the sender's profile-picture direct-path.
         *
         * @return the direct-path; never {@code null}
         */
        public String senderPictureDirectPath() {
            return senderPictureDirectPath;
        }

        /**
         * Returns whether the question owner has explicitly replied to this response.
         *
         * @return {@code true} when the {@code <replied/>} flag was present
         */
        public boolean hasRepliedFlag() {
            return hasRepliedFlag;
        }

        /**
         * Returns the underlying {@link Stanza}.
         *
         * @return the raw {@link Stanza} exposing the content children; never {@code null}
         */
        public Stanza raw() {
            return raw;
        }

        /**
         * Tries to parse a {@link QuestionResponse} from a {@code <question_response>} {@link Stanza}.
         *
         * <p>Returns {@link Optional#empty()} when the description is not {@code question_response},
         * the nested {@code <message>} envelope is missing or carries an out-of-range {@code t}
         * (outside {@code [1577865600, 4102473600]}), or any required child of {@code <sender>}
         * (including {@code <picture direct_path>}) is missing.</p>
         *
         * @implNote The {@code [1577865600, 4102473600]} bounds match the server-side validators for
         * a message timestamp, spanning roughly 2020-01-01 to 2100-01-01 in unix seconds.
         *
         * @param stanza the source {@link Stanza}; never {@code null}
         * @return an {@link Optional} carrying the parsed entry, or empty when the stanza fails validation
         * @throws NullPointerException if {@code stanza} is {@code null}
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseSuccess",
                exports = "parseGetNewsletterResponsesResponseSuccessQuestionResponsesQuestionResponse",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<QuestionResponse> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("question_response")) {
                return Optional.empty();
            }
            var messageNode = stanza.getChild("message").orElse(null);
            if (messageNode == null) {
                return Optional.empty();
            }
            var messageId = messageNode.getAttributeAsString("id").orElse(null);
            if (messageId == null) {
                return Optional.empty();
            }
            var tOpt = messageNode.getAttributeAsLong("t");
            if (tOpt.isEmpty()) {
                return Optional.empty();
            }
            var messageT = tOpt.getAsLong();
            if (messageT < 1577865600L || messageT > 4102473600L) {
                return Optional.empty();
            }
            var fromSelf = messageNode.hasAttribute("is_sender", "true");
            var senderNode = stanza.getChild("sender").orElse(null);
            if (senderNode == null) {
                return Optional.empty();
            }
            var pictureNode = senderNode.getChild("picture").orElse(null);
            if (pictureNode == null) {
                return Optional.empty();
            }
            var directPath = pictureNode.getAttributeAsString("direct_path").orElse(null);
            if (directPath == null) {
                return Optional.empty();
            }
            var senderLid = senderNode.getAttributeAsJid("lid").orElse(null);
            var notifyName = senderNode.getAttributeAsString("notify_name").orElse(null);
            var flagsNode = stanza.getChild("flags").orElse(null);
            var hasReplied = flagsNode != null && flagsNode.getChild("replied").isPresent();
            return Optional.of(new QuestionResponse(messageId, messageT, fromSelf,
                    senderLid, notifyName, directPath, hasReplied, stanza));
        }

        /**
         * Compares two entries for value equality on every field.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link QuestionResponse} with equal field values
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (QuestionResponse) obj;
            return this.messageTimestamp == that.messageTimestamp
                    && this.fromSelf == that.fromSelf
                    && this.hasRepliedFlag == that.hasRepliedFlag
                    && Objects.equals(this.messageId, that.messageId)
                    && Objects.equals(this.senderLid, that.senderLid)
                    && Objects.equals(this.senderNotifyName, that.senderNotifyName)
                    && Objects.equals(this.senderPictureDirectPath, that.senderPictureDirectPath)
                    && Objects.equals(this.raw, that.raw);
        }

        /**
         * Returns the hash code derived from every field.
         *
         * @return the combined hash of every field
         */
        @Override
        public int hashCode() {
            return Objects.hash(messageId, messageTimestamp, fromSelf, senderLid, senderNotifyName,
                    senderPictureDirectPath, hasRepliedFlag, raw);
        }

        /**
         * Returns a debug representation including the typed fields.
         *
         * @return a record-like rendering of this entry, excluding the underlying {@link Stanza} for brevity
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesResponse.QuestionResponse[messageId="
                    + messageId
                    + ", messageTimestamp=" + messageTimestamp
                    + ", fromSelf=" + fromSelf
                    + ", senderLid=" + senderLid
                    + ", senderNotifyName=" + senderNotifyName
                    + ", senderPictureDirectPath=" + senderPictureDirectPath
                    + ", hasRepliedFlag=" + hasRepliedFlag + ']';
        }
    }

    /**
     * Carries a relay-side client-rejection of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for a malformed,
     * unauthorized, rate-limited, suspended, or otherwise rejected request, including the
     * non-admin-caller rejection.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseClientError")
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterResponsesClientErrors")
    final class ClientError implements SmaxNewslettersGetNewsletterResponsesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new client-error reply.
         *
         * <p>The text is optional because not every sub-error carries a human-readable message.</p>
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseClientError",
                exports = "parseGetNewsletterResponsesResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ClientError} with equal
         *         {@link #errorCode()} and {@link #errorText()}
         */
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

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient relay-side failure of the request.
     *
     * <p>Holds the numeric error code and optional text the relay returned for an internal-server
     * error; the variant itself performs no retry.</p>
     */
    @WhatsAppWebModule(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseServerError")
    final class ServerError implements SmaxNewslettersGetNewsletterResponsesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text from the relay.
         */
        private final String errorText;

        /**
         * Constructs a new server-error reply.
         *
         * <p>The text is optional because not every relay-side error carries a human-readable message.</p>
         *
         * @param errorCode the numeric error code echoed by the relay
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code echoed by the relay
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} from the inbound stanza.
         *
         * <p>Delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.</p>
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound stanza
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WASmaxInNewslettersGetNewsletterResponsesResponseServerError",
                exports = "parseGetNewsletterResponsesResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares two replies for value equality on both fields.
         *
         * @param obj the reference object to compare against
         * @return {@code true} when {@code obj} is a {@link ServerError} with equal
         *         {@link #errorCode()} and {@link #errorText()}
         */
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

        /**
         * Returns the hash code derived from both fields.
         *
         * @return the combined hash of {@link #errorCode()} and {@link #errorText()}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation including both fields.
         *
         * @return a record-like rendering of this reply
         */
        @Override
        public String toString() {
            return "SmaxNewslettersGetNewsletterResponsesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
