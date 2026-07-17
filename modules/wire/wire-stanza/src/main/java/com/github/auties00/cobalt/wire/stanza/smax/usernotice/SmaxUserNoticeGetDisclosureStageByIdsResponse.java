package com.github.auties00.cobalt.wire.stanza.smax.usernotice;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Closes the inbound reply variants for {@link SmaxUserNoticeGetDisclosureStageByIdsRequest}.
 *
 * <p>The reply is one of three documented variants: {@link ClientSuccess} carries one
 * {@link ClientSuccess.DisclosureStageNotice} per polled disclosure, {@link ClientError} signals
 * that the relay rejected the request as malformed, and {@link ServerError} signals a transient
 * relay-side failure or rate limit. The static {@link #of(Stanza, Stanza)} factory lifts an inbound
 * stanza into the matching variant; the success branch lets a caller check whether a polled
 * disclosure has progressed in its acceptance lifecycle.
 */
public sealed interface SmaxUserNoticeGetDisclosureStageByIdsResponse extends SmaxStanza.Response
        permits SmaxUserNoticeGetDisclosureStageByIdsResponse.ClientSuccess, SmaxUserNoticeGetDisclosureStageByIdsResponse.ClientError, SmaxUserNoticeGetDisclosureStageByIdsResponse.ServerError {

    /**
     * Tries each {@link SmaxUserNoticeGetDisclosureStageByIdsResponse} variant in priority order and
     * returns the first that parses cleanly.
     *
     * <p>This is the dispatcher entry point that lifts an inbound stanza into the sealed
     * disjunction. An empty result indicates a protocol violation: the stanza matched none of the
     * documented variants.
     *
     * @implNote
     * This implementation tries {@link ClientSuccess}, then {@link ClientError}, then
     * {@link ServerError}, in that order.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound stanza, used to validate echoed identifiers
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no
     *         variant matches
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxUserNoticeGetDisclosureStageByIdsRPC",
            exports = "sendGetDisclosureStageByIdsRPC",
            adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxUserNoticeGetDisclosureStageByIdsResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var clientSuccess = ClientSuccess.of(stanza, request);
        if (clientSuccess.isPresent()) {
            return clientSuccess;
        }
        var clientError = ClientError.of(stanza, request);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza, request);
    }

    /**
     * Carries one {@link DisclosureStageNotice} per polled disclosure on a successful reply.
     *
     * <p>This is the success variant of {@link SmaxUserNoticeGetDisclosureStageByIdsResponse}. The
     * entries appear in the order the relay produced them, and a caller matches each id with the
     * corresponding query and dispatches on the stage.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseClientSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeIQResultResponseMixin")
    final class ClientSuccess implements SmaxUserNoticeGetDisclosureStageByIdsResponse {
        /**
         * Holds the stage entries returned by the relay.
         */
        private final List<DisclosureStageNotice> notices;

        /**
         * Constructs a successful reply wrapping the given per-disclosure stage entries.
         *
         * <p>The list is defensively copied; a {@code null} argument yields an empty list.
         *
         * @param notices the per-disclosure stage entries, or {@code null} for none
         */
        public ClientSuccess(List<DisclosureStageNotice> notices) {
            this.notices = List.copyOf(Objects.requireNonNullElse(notices, List.of()));
        }

        /**
         * Returns the list of stage entries.
         *
         * <p>Each entry carries the per-disclosure {@code (id, t, stage)} triple plus optional
         * version and type metadata; a caller matches the id with the corresponding query and
         * dispatches on the stage. The returned {@link List} is unmodifiable.
         *
         * @return an unmodifiable {@link List} of {@link DisclosureStageNotice}
         */
        public List<DisclosureStageNotice> notices() {
            return notices;
        }

        /**
         * Tries to parse a {@link ClientSuccess} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the envelope fails the standard
         * {@code <iq type="result">} validation or when any {@code <notice>} child fails per-entry
         * parsing.
         *
         * @implNote
         * This implementation validates the envelope via
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, then walks every
         * {@code <notice>} child via {@link DisclosureStageNotice#of(Stanza)}; any failed entry
         * aborts the whole parse so the dispatcher can fall through to the error branches.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on
         *         no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseClientSuccess",
                exports = "parseGetDisclosureStageByIdsResponseClientSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientSuccess> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var notices = new ArrayList<DisclosureStageNotice>();
            for (var noticeNode : stanza.getChildren("notice")) {
                var notice = DisclosureStageNotice.of(noticeNode).orElse(null);
                if (notice == null) {
                    return Optional.empty();
                }
                notices.add(notice);
            }
            return Optional.of(new ClientSuccess(notices));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two successful replies are equal when they carry equal notice lists.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (ClientSuccess) obj;
            return Objects.equals(this.notices, that.notices);
        }

        /**
         * {@inheritDoc}
         *
         * <p>The hash is derived from the notice list.
         */
        @Override
        public int hashCode() {
            return Objects.hash(notices);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Renders the type name and the notice list in the record-like form shared across the
         * {@code Smax} response family.
         */
        @Override
        public String toString() {
            return "SmaxUserNoticeGetDisclosureStageByIdsResponse.ClientSuccess[notices=" + notices + ']';
        }

        /**
         * Projects a single {@code <notice>} entry carrying the per-disclosure stage marker.
         *
         * <p>Unlike
         * {@link SmaxUserNoticeGetDisclosuresResponse.ClientSuccess.DisclosureNotice}, the
         * {@code version} and {@code type} attributes are optional here; this RPC only commits to
         * surfacing the {@code (id, t, stage)} triple, exposing version and type through
         * {@link Optional}-returning accessors when the relay includes them.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeStageMixin")
        public static final class DisclosureStageNotice {
            /**
             * Holds the relay-side timestamp, in seconds since the UNIX epoch, taken from the
             * {@code t} attribute.
             */
            private final long timestampSeconds;

            /**
             * Holds the optional notice version from the {@code version} attribute, at least one
             * when present and {@code null} when absent.
             */
            private final Integer version;

            /**
             * Holds the optional notice type from the {@code type} attribute, non-negative when
             * present and {@code null} when absent.
             */
            private final Integer type;

            /**
             * Holds the disclosure id from the {@code id} attribute.
             */
            private final long noticeId;

            /**
             * Holds the current acceptance stage in the {@code [0, 1000]} range.
             */
            private final int stage;

            /**
             * Constructs a disclosure-stage notice from its parsed attributes.
             *
             * @param timestampSeconds the relay-side timestamp in seconds since the UNIX epoch
             * @param version          the optional notice version, or {@code null} when absent
             * @param type             the optional notice type, or {@code null} when absent
             * @param noticeId         the disclosure id
             * @param stage            the current acceptance stage
             */
            public DisclosureStageNotice(long timestampSeconds, Integer version, Integer type,
                                         long noticeId, int stage) {
                this.timestampSeconds = timestampSeconds;
                this.version = version;
                this.type = type;
                this.noticeId = noticeId;
                this.stage = stage;
            }

            /**
             * Returns the relay-side timestamp.
             *
             * @return the timestamp in seconds since the UNIX epoch
             */
            public long timestampSeconds() {
                return timestampSeconds;
            }

            /**
             * Returns the optional notice version.
             *
             * <p>The version distinguishes successive revisions of the same disclosure when
             * surfaced; it is absent on entries that carry only the stage triple.
             *
             * @return an {@link Optional} carrying the version, or {@link Optional#empty()} when
             *         absent
             */
            public Optional<Integer> version() {
                return Optional.ofNullable(version);
            }

            /**
             * Returns the optional notice type.
             *
             * <p>The type identifies the disclosure category when surfaced; it is absent on entries
             * that carry only the stage triple.
             *
             * @return an {@link Optional} carrying the type, or {@link Optional#empty()} when absent
             */
            public Optional<Integer> type() {
                return Optional.ofNullable(type);
            }

            /**
             * Returns the disclosure id.
             *
             * <p>The id matches the {@code disclosureId} of the corresponding outbound
             * {@link SmaxUserNoticeGetDisclosureStageByIdsRequest.DisclosureStageQuery}.
             *
             * @return the disclosure id
             */
            public long noticeId() {
                return noticeId;
            }

            /**
             * Returns the current acceptance stage.
             *
             * <p>The stage reflects where the disclosure sits in the acceptance lifecycle (such as
             * soft-opt-in or accepted) and is used to decide whether the user has dismissed or
             * accepted it.
             *
             * @return the stage in the {@code [0, 1000]} range
             */
            public int stage() {
                return stage;
            }

            /**
             * Tries to parse a stage notice from the given {@code <notice>} child.
             *
             * <p>Returns {@link Optional#empty()} for any sub-tree missing a required attribute or
             * carrying an out-of-range value.
             *
             * @implNote
             * This implementation enforces the range contracts: version at least {@code 1} when
             * present, type non-negative when present, and stage within {@code [0, 1000]}. An
             * attribute that is present but out of range aborts the parse, while a missing optional
             * attribute leaves the corresponding {@link Optional} empty. The {@code 1000} ceiling
             * and the per-attribute floors mirror the values the relay enforces on the wire.
             *
             * @param stanza the {@code <notice>} child
             * @return an {@link Optional} carrying the parsed entry, or {@link Optional#empty()} on
             *         no-match
             * @throws NullPointerException if {@code stanza} is {@code null}
             */
            @WhatsAppWebExport(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseClientSuccess",
                    exports = "parseGetDisclosureStageByIdsResponseClientSuccessNotice",
                    adaptation = WhatsAppAdaptation.ADAPTED)
            public static Optional<DisclosureStageNotice> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("notice")) {
                    return Optional.empty();
                }
                var timestampOpt = stanza.getAttributeAsLong("t");
                if (timestampOpt.isEmpty()) {
                    return Optional.empty();
                }
                var versionAttr = stanza.getAttributeAsInt("version").orElse(-1);
                Integer version = null;
                if (versionAttr >= 1) {
                    version = versionAttr;
                } else if (stanza.hasAttribute("version")) {
                    return Optional.empty();
                }
                var typeAttr = stanza.getAttributeAsInt("type").orElse(-1);
                Integer type = null;
                if (typeAttr >= 0) {
                    type = typeAttr;
                } else if (stanza.hasAttribute("type")) {
                    return Optional.empty();
                }
                var idOpt = stanza.getAttributeAsLong("id");
                if (idOpt.isEmpty()) {
                    return Optional.empty();
                }
                var stageOpt = stanza.getAttributeAsInt("stage");
                if (stageOpt.isEmpty() || stageOpt.getAsInt() < 0 || stageOpt.getAsInt() > 1000) {
                    return Optional.empty();
                }
                return Optional.of(new DisclosureStageNotice(timestampOpt.getAsLong(),
                        version, type, idOpt.getAsLong(), stageOpt.getAsInt()));
            }

            /**
             * {@inheritDoc}
             *
             * <p>Two notices are equal when the timestamp, version, type, id, and stage all match.
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (DisclosureStageNotice) obj;
                return this.timestampSeconds == that.timestampSeconds
                        && this.noticeId == that.noticeId
                        && this.stage == that.stage
                        && Objects.equals(this.version, that.version)
                        && Objects.equals(this.type, that.type);
            }

            /**
             * {@inheritDoc}
             *
             * <p>The hash is derived from the timestamp, version, type, id, and stage.
             */
            @Override
            public int hashCode() {
                return Objects.hash(timestampSeconds, version, type, noticeId, stage);
            }

            /**
             * {@inheritDoc}
             *
             * <p>Renders every carried field in the record-like form shared across the {@code Smax}
             * response family.
             */
            @Override
            public String toString() {
                return "SmaxUserNoticeGetDisclosureStageByIdsResponse.ClientSuccess.DisclosureStageNotice[timestampSeconds="
                        + timestampSeconds
                        + ", version=" + version
                        + ", type=" + type
                        + ", noticeId=" + noticeId
                        + ", stage=" + stage + ']';
            }
        }
    }

    /**
     * Carries a client-side rejection of the stage-by-ids request.
     *
     * <p>This is the error variant raised when the relay rejected the request as malformed (always
     * {@code 400 bad-request}); a caller typically logs the failure and skips the affected poll.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseClientError")
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeIQErrorBadRequestMixin")
    final class ClientError implements SmaxUserNoticeGetDisclosureStageByIdsResponse {
        /**
         * Holds the numeric error code, always {@code 400} in practice.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, typically {@code "bad-request"}.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from a parsed error code and text.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text, or {@code null} when absent
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * <p>The code is below {@code 500}; a caller typically maps it into a client-facing
         * exception.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * <p>The text is useful for logging and as the exception message.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when absent
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed client-error
         * envelope.
         *
         * @implNote
         * This implementation delegates the envelope and code-range checks to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on
         *         no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseClientError",
                exports = "parseGetDisclosureStageByIdsResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two client errors are equal when both the code and the text match.
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
         * {@inheritDoc}
         *
         * <p>The hash is derived from the code and the text.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Renders the code and text in the record-like form shared across the {@code Smax}
         * response family.
         */
        @Override
        public String toString() {
            return "SmaxUserNoticeGetDisclosureStageByIdsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a transient relay-side failure of the stage-by-ids request.
     *
     * <p>This is the error variant raised when the relay encountered an internal failure or
     * rate-limited the caller; a caller should retry with backoff. Both genuine {@code 5xx} codes
     * and the {@code 429 rate-overlimit} throttle land here, which suits callers that poll inside
     * an exponential-backoff loop.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseServerError")
    @WhatsAppWebModule(moduleName = "WASmaxInUserNoticeUserNoticeServerError")
    final class ServerError implements SmaxUserNoticeGetDisclosureStageByIdsResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, when the relay supplied one.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from a parsed error code and text.
         *
         * <p>Both genuine codes at or above {@code 500} and the {@code 429 rate-overlimit} fallback
         * land here.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text, or {@code null} when absent
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * <p>The code is either a genuine {@code 5xx} from the server-error path or the
         * {@code 429} rate-overlimit code surfaced through the client-error path.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * <p>The text is useful for logging and as the exception message.
         *
         * @return an {@link Optional} carrying the text, or {@link Optional#empty()} when the relay
         *         omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * <p>Returns {@link Optional#empty()} when the stanza is not a well-formed server-error
         * envelope and does not carry the {@code 429 rate-overlimit} client-error pair.
         *
         * @implNote
         * This implementation first delegates to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}; the user-notice domain
         * folds {@code 429 rate-overlimit} into the server-error disjunction, so a second pass
         * through {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} picks up that one
         * code when the primary helper declines.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} on
         *         no-match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInUserNoticeGetDisclosureStageByIdsResponseServerError",
                exports = "parseGetDisclosureStageByIdsResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                var clientEnvelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
                if (clientEnvelope == null || clientEnvelope.code() != 429) {
                    return Optional.empty();
                }
                return Optional.of(new ServerError(clientEnvelope.code(), clientEnvelope.text()));
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * <p>Two server errors are equal when both the code and the text match.
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
         * {@inheritDoc}
         *
         * <p>The hash is derived from the code and the text.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * <p>Renders the code and text in the record-like form shared across the {@code Smax}
         * response family.
         */
        @Override
        public String toString() {
            return "SmaxUserNoticeGetDisclosureStageByIdsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
