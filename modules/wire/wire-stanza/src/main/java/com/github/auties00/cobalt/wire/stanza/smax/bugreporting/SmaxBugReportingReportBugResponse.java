package com.github.auties00.cobalt.wire.stanza.smax.bugreporting;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.Objects;
import java.util.Optional;

/**
 * Closes the sealed family of inbound replies to a {@link SmaxBugReportingReportBugRequest}.
 *
 * <p>The reply is a two-arm disjunction: a {@link Success} carrying the backend-assigned task id,
 * or an {@link Error} carrying the relay's rejection code. Callers obtain a variant through
 * {@link #of(Stanza, Stanza)} and pattern-match on it to decide whether to surface a confirmation or
 * a failure.
 */
public sealed interface SmaxBugReportingReportBugResponse extends SmaxStanza.Response
        permits SmaxBugReportingReportBugResponse.Success, SmaxBugReportingReportBugResponse.Error {

    /**
     * Tries each variant in the WhatsApp Web declared order and returns the first that parses.
     *
     * <p>{@link Success} is attempted first, then {@link Error}. The inbound IQ stanza is passed
     * along with the original outbound request so the underlying mixins can cross-check the echoed
     * {@code id} and {@code from} identifiers.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} when neither variant parses cleanly,
     * whereas WhatsApp Web throws a parsing failure that rejects the caller's promise; Cobalt's
     * dispatch layer routes the empty result through the configurable error handler instead of
     * hardcoding the throw.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if {@code stanza} or {@code request} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxBugReportingReportBugRPC",
            exports = "sendReportBugRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxBugReportingReportBugResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return Error.of(stanza, request);
    }

    /**
     * Models the success reply carrying the backend-assigned task id.
     *
     * <p>Surfaced by {@link #of(Stanza, Stanza)} when the relay accepted the report. The
     * {@link #taskIdElementValue} identifies the report in the bug-tracking backend.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingReportBugResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingHackBaseIQResultResponseMixin")
    final class Success implements SmaxBugReportingReportBugResponse {
        /**
         * Holds the {@code to} attribute echoed back by the relay, or {@code null} when the
         * original request stamped no {@code from} for the relay to echo.
         */
        private final Jid replyTo;

        /**
         * Holds the task id the bug-tracking backend assigned to the report.
         */
        private final String taskIdElementValue;

        /**
         * Constructs a success reply from its optional reply-to JID and the assigned task id.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} after the inbound stanza passes the IQ-result
         * envelope validation.
         *
         * @param replyTo            the optional reply-to JID; may be {@code null}
         * @param taskIdElementValue the task id; never {@code null}
         * @throws NullPointerException if {@code taskIdElementValue} is {@code null}
         */
        public Success(Jid replyTo, String taskIdElementValue) {
            this.replyTo = replyTo;
            this.taskIdElementValue = Objects.requireNonNull(taskIdElementValue,
                    "taskIdElementValue cannot be null");
        }

        /**
         * Returns the optional reply-to JID.
         *
         * <p>Empty when the original request did not stamp {@code from}, so the relay had nothing
         * to echo back.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> replyTo() {
            return Optional.ofNullable(replyTo);
        }

        /**
         * Returns the backend-assigned task id.
         *
         * @return the task id; never {@code null}
         */
        public String taskIdElementValue() {
            return taskIdElementValue;
        }

        /**
         * Tries to parse a success reply from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza fails the IQ-result envelope check,
         * when the {@code <task_id>} child is missing, or when its content cannot be decoded as a
         * string. On success the {@code to} attribute, when present, populates {@link #replyTo}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBugReportingReportBugResponseSuccess",
                exports = "parseReportBugResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var taskIdNode = stanza.getChild("task_id").orElse(null);
            if (taskIdNode == null) {
                return Optional.empty();
            }
            var taskId = taskIdNode.toContentString().orElse(null);
            if (taskId == null) {
                return Optional.empty();
            }
            var replyTo = stanza.getAttributeAsJid("to").orElse(null);
            return Optional.of(new Success(replyTo, taskId));
        }

        /**
         * Indicates whether the given object is a success reply equal to this one.
         *
         * <p>Two replies are equal when their reply-to JID and task id both match.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal success reply; {@code false} otherwise
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
            return Objects.equals(this.replyTo, that.replyTo)
                    && Objects.equals(this.taskIdElementValue, that.taskIdElementValue);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(replyTo, taskIdElementValue);
        }

        /**
         * Returns a debug representation listing the reply-to JID and task id.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxBugReportingReportBugResponse.Success[replyTo=" + replyTo
                    + ", taskIdElementValue=" + taskIdElementValue + ']';
        }
    }

    /**
     * Models the error reply carrying the relay's rejection code.
     *
     * <p>Surfaced by {@link #of(Stanza, Stanza)} when the relay rejected the report. The only two
     * documented disjuncts are code {@code 400} with text {@code "bad-request"} (the client should
     * not retry) and code {@code 500} with text {@code "internal-server-error"} (transient,
     * retry-eligible).
     */
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingReportBugResponseError")
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingHackBaseIQErrorResponseMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingReportBugErrors")
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQErrorBadRequestMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInBugReportingIQErrorInternalServerErrorMixin")
    final class Error implements SmaxBugReportingReportBugResponse {
        /**
         * Holds the {@code to} attribute echoed back by the relay, or {@code null} when the
         * original request stamped no {@code from} for the relay to echo.
         */
        private final Jid replyTo;

        /**
         * Holds the numeric error code; one of {@code 400} or {@code 500}.
         */
        private final int errorCode;

        /**
         * Holds the error text paired with {@link #errorCode}; one of {@code "bad-request"} or
         * {@code "internal-server-error"}.
         */
        private final String errorText;

        /**
         * Constructs an error reply from its optional reply-to JID and the classified code/text
         * pair.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the {@link SmaxBaseServerErrorMixin} envelope
         * has been classified against the documented {@code (400, "bad-request")} and
         * {@code (500, "internal-server-error")} pairs.
         *
         * @param replyTo   the optional reply-to JID; may be {@code null}
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public Error(Jid replyTo, int errorCode, String errorText) {
            this.replyTo = replyTo;
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the optional reply-to JID.
         *
         * <p>Empty when the original request did not stamp {@code from}, so the relay had nothing
         * to echo back.
         *
         * @return an {@link Optional} carrying the JID
         */
        public Optional<Jid> replyTo() {
            return Optional.ofNullable(replyTo);
        }

        /**
         * Returns the numeric error code.
         *
         * <p>One of {@code 400} (do not retry) or {@code 500} (retry-eligible); callers use it to
         * decide whether to resurface the report dialog with the original payload pre-filled.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional error text.
         *
         * <p>Empty only when the relay omitted the text; the disjunction validation in
         * {@link #of(Stanza, Stanza)} normally guarantees it is present and paired with
         * {@link #errorCode}.
         *
         * @return an {@link Optional} carrying the text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse an error reply from the given inbound stanza.
         *
         * <p>The client-error envelope (codes below {@code 500}) is tried first, then the
         * server-error envelope, both through {@link SmaxBaseServerErrorMixin}. The resulting
         * code/text pair must be one of the documented {@code (400, "bad-request")} or
         * {@code (500, "internal-server-error")} disjuncts; any other pair, or a missing envelope,
         * yields {@link Optional#empty()}. On success the {@code to} attribute, when present,
         * populates {@link #replyTo}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant
         */
        @WhatsAppWebExport(moduleName = "WASmaxInBugReportingReportBugResponseError",
                exports = "parseReportBugResponseError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        @WhatsAppWebExport(moduleName = "WASmaxInBugReportingReportBugErrors",
                exports = "parseReportBugErrors",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Error> of(Stanza stanza, Stanza request) {
            var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            var serverEnvelope = clientEnvelope == null
                    ? SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null)
                    : null;
            var envelope = clientEnvelope != null ? clientEnvelope : serverEnvelope;
            if (envelope == null) {
                return Optional.empty();
            }
            var code = envelope.code();
            var text = envelope.text();
            if (code == 400 && "bad-request".equals(text)) {
            } else if (code == 500 && "internal-server-error".equals(text)) {
            } else {
                return Optional.empty();
            }
            var replyTo = stanza.getAttributeAsJid("to").orElse(null);
            return Optional.of(new Error(replyTo, code, text));
        }

        /**
         * Indicates whether the given object is an error reply equal to this one.
         *
         * <p>Two replies are equal when their error code, reply-to JID, and error text all match.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal error reply; {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Error) obj;
            return this.errorCode == that.errorCode
                    && Objects.equals(this.replyTo, that.replyTo)
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(replyTo, errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the reply-to JID, error code, and error text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "SmaxBugReportingReportBugResponse.Error[replyTo=" + replyTo
                    + ", errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
