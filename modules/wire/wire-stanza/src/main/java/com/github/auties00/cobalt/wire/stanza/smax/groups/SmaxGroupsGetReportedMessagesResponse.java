package com.github.auties00.cobalt.wire.stanza.smax.groups;

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
 * The sealed reply family for a {@link SmaxGroupsGetReportedMessagesRequest}.
 * <p>
 * Exactly one of three variants matches a given inbound stanza: {@link Success} carries the per-message {@link Report}
 * rows, {@link ClientError} carries a caller-side rejection code, and {@link ServerError} carries a transient
 * relay-side failure code.
 */
public sealed interface SmaxGroupsGetReportedMessagesResponse extends SmaxStanza.Response
        permits SmaxGroupsGetReportedMessagesResponse.Success, SmaxGroupsGetReportedMessagesResponse.ClientError, SmaxGroupsGetReportedMessagesResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsGetReportedMessagesResponse} variant and returns the first
     * that parses cleanly.
     * <p>
     * Variants are tried in priority order: {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     * The result is empty when the stanza matches none of the three variants.
     *
     * @implNote This implementation defers the no-match decision to the caller by returning an empty {@link Optional}
     * rather than throwing, so the caller can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound request
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetReportedMessagesRPC",
            exports = "sendGetReportedMessagesRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetReportedMessagesResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant emitted when the relay returned the pending moderation queue.
     * <p>
     * {@link #reports()} holds one {@link Report} per flagged message, each keyed by the reported message id and
     * carrying its reporter list.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetReportedMessagesResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsGetReportedMessagesResponse {
        /**
         * The per-message report rows.
         */
        private final List<Report> reports;

        /**
         * Constructs a {@link Success} reply.
         *
         * @param reports the per-message report rows; never {@code null}
         * @throws NullPointerException if {@code reports} is {@code null}
         */
        public Success(List<Report> reports) {
            Objects.requireNonNull(reports, "reports cannot be null");
            this.reports = List.copyOf(reports);
        }

        /**
         * Returns the per-message report rows.
         *
         * @return an unmodifiable list of {@link Report}; never {@code null}
         */
        public List<Report> reports() {
            return reports;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         * <p>
         * The envelope is validated through {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and the
         * {@code <reports>} wrapper must be present; each {@code <report/>} child is parsed via {@link Report#of(Stanza)}
         * and a single failed child fails the whole parse. An empty wrapper yields an empty report list.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetReportedMessagesResponseSuccess",
                exports = "parseGetReportedMessagesResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var wrapper = stanza.getChild("reports").orElse(null);
            if (wrapper == null) {
                return Optional.empty();
            }
            var reportNodes = wrapper.getChildren("report");
            var reports = new ArrayList<Report>(reportNodes.size());
            for (var reportNode : reportNodes) {
                var report = Report.of(reportNode).orElse(null);
                if (report == null) {
                    return Optional.empty();
                }
                reports.add(report);
            }
            return Optional.of(new Success(reports));
        }

        /**
         * Compares this success to {@code obj} for value equality across every field.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with identical fields
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
            return Objects.equals(this.reports, that.reports);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(reports);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetReportedMessagesResponse.Success[reports=" + reports + ']';
        }
    }

    /**
     * Per-report projection carrying the reported message id and the non-empty reporter list.
     * <p>
     * The {@link #messageId()} is the stanza id of the original group message; the {@link #reporters()} list is
     * guaranteed non-empty because the relay omits the row entirely when no reporters remain.
     */
    final class Report {
        /**
         * The stanza id of the reported group message.
         */
        private final String messageId;

        /**
         * The reporters who flagged the message; never empty.
         */
        private final List<Reporter> reporters;

        /**
         * Constructs a {@link Report} entry.
         *
         * @param messageId the reported message stanza id; never {@code null}
         * @param reporters the reporter list; never {@code null} and never empty
         * @throws NullPointerException     if {@code messageId} or {@code reporters} is {@code null}
         * @throws IllegalArgumentException if {@code reporters} is empty
         */
        public Report(String messageId, List<Reporter> reporters) {
            this.messageId = Objects.requireNonNull(messageId, "messageId cannot be null");
            Objects.requireNonNull(reporters, "reporters cannot be null");
            if (reporters.isEmpty()) {
                throw new IllegalArgumentException("reporters cannot be empty");
            }
            this.reporters = List.copyOf(reporters);
        }

        /**
         * Returns the reported message stanza id.
         *
         * @return the message id; never {@code null}
         */
        public String messageId() {
            return messageId;
        }

        /**
         * Returns the reporter list.
         *
         * @return an unmodifiable list of {@link Reporter}; never empty
         */
        public List<Reporter> reporters() {
            return reporters;
        }

        /**
         * Tries to parse a {@link Report} from the given {@code <report/>} child.
         * <p>
         * Parsing succeeds when the child carries the {@code message_id} attribute and at least one
         * {@code <reporter/>} grandchild; the relay never emits an empty report row.
         *
         * @param stanza the {@code <report/>} child stanza
         * @return an {@link Optional} carrying the parsed report, or empty when the child does not match
         */
        public static Optional<Report> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("report")) {
                return Optional.empty();
            }
            var messageId = stanza.getAttributeAsString("message_id").orElse(null);
            if (messageId == null) {
                return Optional.empty();
            }
            var reporterNodes = stanza.getChildren("reporter");
            if (reporterNodes.isEmpty()) {
                return Optional.empty();
            }
            var reporters = new ArrayList<Reporter>(reporterNodes.size());
            for (var reporterNode : reporterNodes) {
                var reporter = Reporter.of(reporterNode).orElse(null);
                if (reporter == null) {
                    return Optional.empty();
                }
                reporters.add(reporter);
            }
            return Optional.of(new Report(messageId, reporters));
        }

        /**
         * Compares this report to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Report} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Report) obj;
            return Objects.equals(this.messageId, that.messageId)
                    && Objects.equals(this.reporters, that.reporters);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(messageId, reporters);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetReportedMessagesResponse.Report[messageId=" + messageId
                    + ", reporters=" + reporters + ']';
        }
    }

    /**
     * Per-reporter projection carrying the reporting user's JID and the unix-seconds timestamp at which the report was
     * filed.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsIdentityMixin")
    final class Reporter {
        /**
         * The reporter's user {@link Jid}.
         */
        private final Jid jid;

        /**
         * The unix-seconds timestamp at which the report was filed.
         */
        private final long timestamp;

        /**
         * Constructs a {@link Reporter} entry.
         *
         * @param jid       the reporter's {@link Jid}; never {@code null}
         * @param timestamp the unix-seconds timestamp
         * @throws NullPointerException     if {@code jid} is {@code null}
         * @throws IllegalArgumentException if {@code timestamp} is negative
         */
        public Reporter(Jid jid, long timestamp) {
            this.jid = Objects.requireNonNull(jid, "jid cannot be null");
            if (timestamp < 0) {
                throw new IllegalArgumentException("timestamp must be non-negative");
            }
            this.timestamp = timestamp;
        }

        /**
         * Returns the reporter's {@link Jid}.
         *
         * @return the reporter JID; never {@code null}
         */
        public Jid jid() {
            return jid;
        }

        /**
         * Returns the report timestamp.
         *
         * @return the unix-seconds timestamp
         */
        public long timestamp() {
            return timestamp;
        }

        /**
         * Tries to parse a {@link Reporter} from the given {@code <reporter/>} child.
         * <p>
         * Parsing succeeds when the child carries the {@code jid} and {@code timestamp} attributes.
         *
         * @param stanza the {@code <reporter/>} child stanza
         * @return an {@link Optional} carrying the parsed reporter, or empty when the child does not match
         */
        public static Optional<Reporter> of(Stanza stanza) {
            Objects.requireNonNull(stanza, "stanza cannot be null");
            if (!stanza.hasDescription("reporter")) {
                return Optional.empty();
            }
            var jid = stanza.getAttributeAsJid("jid").orElse(null);
            if (jid == null) {
                return Optional.empty();
            }
            var timestampOptional = stanza.getAttributeAsLong("timestamp");
            if (timestampOptional.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Reporter(jid, timestampOptional.getAsLong()));
        }

        /**
         * Compares this reporter to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Reporter} with identical fields
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Reporter) obj;
            return this.timestamp == that.timestamp
                    && Objects.equals(this.jid, that.jid);
        }

        /**
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(jid, timestamp);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetReportedMessagesResponse.Reporter[jid=" + jid
                    + ", timestamp=" + timestamp + ']';
        }
    }

    /**
     * The reply variant emitted when the relay rejected the moderation-queue query as malformed or unauthorised.
     * <p>
     * The {@link #errorCode()} carries the HTTP-style status assigned by the relay and {@link #errorText()} carries
     * the optional human-readable reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetReportedMessagesResponseClientError")
    final class ClientError implements SmaxGroupsGetReportedMessagesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a {@link ClientError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from {@code stanza}.
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which matches only client-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetReportedMessagesResponseClientError",
                exports = "parseGetReportedMessagesResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ClientError} with identical fields
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
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetReportedMessagesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The reply variant emitted on transient relay-side failure.
     * <p>
     * Unlike {@link ClientError} this code typically signals a retry-eligible relay outage rather than a malformed or
     * unauthorised request; {@link #errorCode()} carries the server-range status and {@link #errorText()} the optional
     * reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetReportedMessagesResponseServerError")
    final class ServerError implements SmaxGroupsGetReportedMessagesResponse {
        /**
         * The numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed by the relay; {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a {@link ServerError} from raw error attributes.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code echoed by the relay.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from {@code stanza}.
         * <p>
         * The shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope is validated through
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which matches only server-range codes.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetReportedMessagesResponseServerError",
                exports = "parseGetReportedMessagesResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this error to {@code obj} for value equality across both fields.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link ServerError} with identical fields
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
         * Returns a hash composed of both fields.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying both fields.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetReportedMessagesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
