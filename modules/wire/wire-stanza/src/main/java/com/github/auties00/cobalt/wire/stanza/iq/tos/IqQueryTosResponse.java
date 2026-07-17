package com.github.auties00.cobalt.wire.stanza.iq.tos;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqQueryTosRequest}.
 *
 * <p>Callers discriminate the relay outcome by switching on the returned variant. A {@link Success}
 * carries the clamped refresh interval plus the per-notice {@link Success.NoticeState accepted-state}
 * entries, which drive the cadence of the local state-pull loop and the acceptance prompts. A
 * {@link ClientError} surfaces a relay rejection, and a {@link ServerError} surfaces a transient
 * relay failure that WhatsApp Web retries via exponential backoff.
 */
public sealed interface IqQueryTosResponse extends IqStanza.Response
        permits IqQueryTosResponse.Success, IqQueryTosResponse.ClientError, IqQueryTosResponse.ServerError {

    /**
     * Holds the minimum server-recommended refresh interval in seconds.
     *
     * <p>Replies below this floor are clamped to {@link #DEFAULT_TOS_REFRESH_INTERVAL_SECONDS}.
     *
     * @implNote The value {@code 7200} (two hours) matches WhatsApp Web's implicit local floor.
     */
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "queryTosState", adaptation = WhatsAppAdaptation.DIRECT)
    int MIN_TOS_REFRESH_INTERVAL_SECONDS = 7200;

    /**
     * Holds the maximum server-recommended refresh interval in seconds.
     *
     * <p>Replies above this ceiling are clamped to {@link #DEFAULT_TOS_REFRESH_INTERVAL_SECONDS}.
     *
     * @implNote The value {@code 259200} (three days) matches WhatsApp Web's implicit local ceiling.
     */
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "queryTosState", adaptation = WhatsAppAdaptation.DIRECT)
    int MAX_TOS_REFRESH_INTERVAL_SECONDS = 259200;

    /**
     * Holds the default server-recommended refresh interval in seconds (24 hours).
     *
     * <p>Used as the fallback when the reply's {@code refresh} value falls outside the
     * {@code [MIN_TOS_REFRESH_INTERVAL_SECONDS, MAX_TOS_REFRESH_INTERVAL_SECONDS]} range.
     *
     * @implNote The value {@code 86400} matches WhatsApp Web's
     *           {@code WAWebTosJob.DEFAULT_TOS_REFRESH_INTERVAL}.
     */
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "DEFAULT_TOS_REFRESH_INTERVAL", adaptation = WhatsAppAdaptation.DIRECT)
    int DEFAULT_TOS_REFRESH_INTERVAL_SECONDS = 86400;

    /**
     * Parses the inbound stanza into the first matching {@link IqQueryTosResponse} variant.
     *
     * <p>The priority ordering (success, then client-error, then server-error) matches the wire
     * shape so the variants are mutually exclusive and the match is never ambiguous.
     *
     * @implNote This implementation calls each variant's {@code of(stanza, request)} in turn and
     *           returns the first present result.
     * @param stanza    the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebTosJob",
            exports = "queryTosState", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqQueryTosResponse> of(Stanza stanza, Stanza request) {
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
     * Models the success variant in which the relay returned the per-notice accepted state plus the
     * clamped refresh interval.
     *
     * <p>The caller reads {@link #refreshIntervalSeconds()} to drive the cadence of the
     * {@link IqQueryTosRequest} loop and {@link #notices()} for the per-notice accepted state. The
     * caller filters the entries against its locally-known notice ids before applying them.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class Success implements IqQueryTosResponse {
        /**
         * Holds the server-recommended refresh interval in seconds, clamped to
         * {@code [MIN_TOS_REFRESH_INTERVAL_SECONDS, MAX_TOS_REFRESH_INTERVAL_SECONDS]} and falling
         * back to {@link #DEFAULT_TOS_REFRESH_INTERVAL_SECONDS} when out of range.
         */
        private final int refreshIntervalSeconds;

        /**
         * Holds the per-notice accepted-state entries returned by the relay.
         */
        private final List<NoticeState> notices;

        /**
         * Constructs a successful reply bound to the clamped refresh interval and the per-notice
         * entries.
         *
         * @param refreshIntervalSeconds the clamped refresh interval
         * @param notices                the per-notice entries; never {@code null}
         * @throws NullPointerException if {@code notices} is {@code null}
         */
        public Success(int refreshIntervalSeconds, List<NoticeState> notices) {
            this.refreshIntervalSeconds = refreshIntervalSeconds;
            Objects.requireNonNull(notices, "notices cannot be null");
            this.notices = List.copyOf(notices);
        }

        /**
         * Returns the clamped server-recommended refresh interval in seconds.
         *
         * @return the refresh interval
         */
        public int refreshIntervalSeconds() {
            return refreshIntervalSeconds;
        }

        /**
         * Returns the unmodifiable list of per-notice accepted-state entries.
         *
         * @return the entries; never {@code null}
         */
        public List<NoticeState> notices() {
            return notices;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the SMAX result-envelope check fails, when the {@code <tos>} child
         * is absent, when its {@code refresh} attribute is missing, or when any {@code <notice>}
         * grandchild is missing the {@code id} attribute. The parsed {@code refresh} value is
         * clamped against the documented bounds, falling back to
         * {@link #DEFAULT_TOS_REFRESH_INTERVAL_SECONDS} when out of range.
         *
         * @implNote This implementation treats an absent {@code state} attribute as accepted and
         *           only {@code state="false"} as not-accepted, matching WhatsApp Web's branch
         *           {@code maybeAttrString("state") !== "false"}.
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "queryTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var tosChild = stanza.getChild("tos").orElse(null);
            if (tosChild == null) {
                return Optional.empty();
            }
            var rawRefresh = tosChild.getAttributeAsInt("refresh").orElse(-1);
            if (rawRefresh < 0) {
                return Optional.empty();
            }
            var clampedRefresh = rawRefresh;
            if (clampedRefresh > MAX_TOS_REFRESH_INTERVAL_SECONDS
                    || clampedRefresh < MIN_TOS_REFRESH_INTERVAL_SECONDS) {
                clampedRefresh = DEFAULT_TOS_REFRESH_INTERVAL_SECONDS;
            }
            var noticeChildren = tosChild.getChildren("notice");
            var notices = new ArrayList<NoticeState>(noticeChildren.size());
            for (var noticeNode : noticeChildren) {
                var id = noticeNode.getAttributeAsString("id").orElse(null);
                if (id == null) {
                    return Optional.empty();
                }
                var stateAttr = noticeNode.getAttributeAsString("state").orElse(null);
                var accepted = stateAttr == null || !stateAttr.equals("false");
                notices.add(new NoticeState(id, accepted));
            }
            return Optional.of(new Success(clampedRefresh, notices));
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>Two successes are equal when they carry the same refresh interval and the same
         * per-notice entries.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with an equal refresh interval
         *         and entries, {@code false} otherwise
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
            return this.refreshIntervalSeconds == that.refreshIntervalSeconds
                    && Objects.equals(this.notices, that.notices);
        }

        /**
         * Returns a hash code derived from the refresh interval and the per-notice entries.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(refreshIntervalSeconds, notices);
        }

        /**
         * Returns a debug string carrying the refresh interval and the per-notice entries.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqQueryTosResponse.Success[refreshIntervalSeconds="
                    + refreshIntervalSeconds + ", notices=" + notices + ']';
        }

        /**
         * Models a per-notice accepted-state entry projected from one {@code <notice/>} child of
         * the {@code <tos/>} reply envelope.
         *
         * <p>An {@link #accepted() true} entry is recorded by the caller as accepted; a false entry
         * is recorded as not-accepted, which then drives the corresponding acceptance prompt on the
         * next surface that gates on the notice.
         */
        @WhatsAppWebModule(moduleName = "WAWebTosJob")
        public static final class NoticeState {
            /**
             * Holds the notice id, echoed from the corresponding request entry.
             */
            private final String id;

            /**
             * Holds the accepted-state flag.
             */
            private final boolean accepted;

            /**
             * Constructs a per-notice entry bound to the given id and flag.
             *
             * <p>The flag is {@code true} when the user has accepted the notice or when the relay
             * omits the {@code state} attribute, and {@code false} only when the relay explicitly
             * returns {@code state="false"}.
             *
             * @param id       the notice id; never {@code null}
             * @param accepted the accepted-state flag
             * @throws NullPointerException if {@code id} is {@code null}
             */
            public NoticeState(String id, boolean accepted) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.accepted = accepted;
            }

            /**
             * Returns the notice id.
             *
             * @return the id; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the accepted-state flag.
             *
             * @return {@code true} when the notice has been accepted, {@code false} otherwise
             */
            public boolean accepted() {
                return accepted;
            }

            /**
             * Compares this entry to the given object for equality.
             *
             * <p>Two entries are equal when they carry the same id and accepted flag.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link NoticeState} with an equal id and
             *         flag, {@code false} otherwise
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (NoticeState) obj;
                return this.accepted == that.accepted
                        && Objects.equals(this.id, that.id);
            }

            /**
             * Returns a hash code derived from the id and accepted flag.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(id, accepted);
            }

            /**
             * Returns a debug string carrying the id and accepted flag.
             *
             * @return the string representation
             */
            @Override
            public String toString() {
                return "IqQueryTosResponse.Success.NoticeState[id="
                        + id + ", accepted=" + accepted + ']';
            }
        }
    }

    /**
     * Models the client-error variant in which the relay rejected the query with a {@code 4xx}
     * code.
     *
     * <p>WhatsApp Web's state-pull loop treats any non-500 code as a fatal failure and stops
     * retrying. A caller should treat this variant the same way unless it has a richer policy.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class ClientError implements IqQueryTosResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the standard
         * SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails. Envelope parsing is delegated to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "queryTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>Two client errors are equal when they carry the same code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with an equal code and
         *         text, {@code false} otherwise
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqQueryTosResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the server-error variant in which the relay encountered a transient internal failure
     * processing the query.
     *
     * <p>WhatsApp Web retries the {@code 500} arm via exponential backoff (up to five retries, with
     * a base growing from 1 second to 16 seconds) and treats any other 5xx code as fatal. A caller
     * that needs a tight retry policy can replicate this.
     */
    @WhatsAppWebModule(moduleName = "WAWebTosJob")
    final class ServerError implements IqQueryTosResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text; {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply carrying the relay-echoed envelope.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric server-side error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional human-readable error text.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the standard
         * SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails. Envelope parsing is delegated to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does
         *         not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebTosJob",
                exports = "queryTosState", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to the given object for equality.
         *
         * <p>Two server errors are equal when they carry the same code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with an equal code and
         *         text, {@code false} otherwise
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
            return this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * Returns a hash code derived from the error code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and text.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "IqQueryTosResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
