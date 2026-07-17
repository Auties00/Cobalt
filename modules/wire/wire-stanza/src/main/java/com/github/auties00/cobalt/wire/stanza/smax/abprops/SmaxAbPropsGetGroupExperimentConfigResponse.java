package com.github.auties00.cobalt.wire.stanza.smax.abprops;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the sealed family of inbound replies to a
 * {@link SmaxAbPropsGetGroupExperimentConfigRequest}.
 *
 * <p>A reply is exactly one of three variants: {@link Success} carrying the materialised
 * group-scoped props bundle, {@link ClientError} for a do-not-retry rejection, or {@link ServerError}
 * for a transient failure that is retry-eligible. Callers pattern-match on the variant to either
 * flush the parsed bundle into the per-group store or signal a sync failure, and
 * {@link #of(Stanza, Stanza)} disambiguates the three by trying each variant in turn.
 */
public sealed interface SmaxAbPropsGetGroupExperimentConfigResponse extends SmaxStanza.Response
        permits SmaxAbPropsGetGroupExperimentConfigResponse.Success, SmaxAbPropsGetGroupExperimentConfigResponse.ClientError, SmaxAbPropsGetGroupExperimentConfigResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching response variant.
     *
     * <p>Variants are tried in priority order: {@link Success} first, then {@link ClientError},
     * then {@link ServerError}. The original {@code request} is supplied so each variant can
     * cross-check the {@code id} correlation against the reply. The result is empty when none of the
     * three variants parse cleanly.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} when no variant parses, leaving recovery
     * to the configurable error handler; WhatsApp Web instead throws a parsing failure that rejects
     * the upstream promise.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when none matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxAbPropsGetGroupExperimentConfigRPC",
            exports = "sendGetGroupExperimentConfigRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxAbPropsGetGroupExperimentConfigResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the materialised group-scoped props bundle returned on a successful fetch.
     *
     * <p>Surfaced when the relay returns the {@code <props/>} subtree scoped to the requested group.
     * The parsed hash, refresh cooldown, refresh id, and A/B framework key let the caller flush the
     * bundle into the per-group cache and schedule the next sync; the raw {@code <props/>} stanza is
     * retained for the caller to re-parse the per-experiment overrides.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInAbPropsIQResultResponseMixin")
    final class Success implements SmaxAbPropsGetGroupExperimentConfigResponse {
        /**
         * Holds the relay-returned content hash, or {@code null} when the relay omitted it.
         *
         * <p>Echoed back on the next conditional fetch for the same group.
         */
        private final String propsHash;

        /**
         * Holds the relay-returned refresh-cooldown hint in seconds, or {@code null} when omitted.
         *
         * <p>Drives the next-sync timer for the per-group sync loop.
         */
        private final Integer propsRefresh;

        /**
         * Holds the relay-returned refresh id, or {@code null} when the relay omitted it.
         */
        private final Integer propsRefreshId;

        /**
         * Holds the relay-returned A/B framework key, or {@code null} when the relay omitted it.
         *
         * <p>Identifies the framework variant the relay assigned to this group and is surfaced in
         * downstream WAM tagging.
         */
        private final String propsAbKey;

        /**
         * Holds the raw {@code <props/>} subtree carrying the per-experiment overrides for this group.
         *
         * <p>Retained verbatim so the caller can re-parse it into the per-experiment overrides;
         * never {@code null}.
         */
        private final Stanza propsStanza;

        /**
         * Constructs a success projection from the parsed {@code <props/>} attributes and subtree.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the IQ-result envelope validates and the
         * {@code <props/>} child is found. All scalar arguments are nullable; only {@code propsStanza}
         * is required.
         *
         * @param propsHash      the content hash, or {@code null} when omitted
         * @param propsRefresh   the refresh cooldown in seconds, or {@code null} when omitted
         * @param propsRefreshId the refresh id, or {@code null} when omitted
         * @param propsAbKey     the A/B framework key, or {@code null} when omitted
         * @param propsStanza      the {@code <props/>} subtree; never {@code null}
         * @throws NullPointerException if {@code propsStanza} is {@code null}
         */
        public Success(String propsHash, Integer propsRefresh, Integer propsRefreshId,
                       String propsAbKey, Stanza propsStanza) {
            this.propsHash = propsHash;
            this.propsRefresh = propsRefresh;
            this.propsRefreshId = propsRefreshId;
            this.propsAbKey = propsAbKey;
            this.propsStanza = Objects.requireNonNull(propsStanza, "propsStanza cannot be null");
        }

        /**
         * Returns the content hash, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the hash, or empty when omitted
         */
        public Optional<String> propsHash() {
            return Optional.ofNullable(propsHash);
        }

        /**
         * Returns the refresh-cooldown hint in seconds, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the cooldown, or empty when omitted
         */
        public Optional<Integer> propsRefresh() {
            return Optional.ofNullable(propsRefresh);
        }

        /**
         * Returns the refresh id, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the refresh id, or empty when omitted
         */
        public Optional<Integer> propsRefreshId() {
            return Optional.ofNullable(propsRefreshId);
        }

        /**
         * Returns the A/B framework key, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the key, or empty when omitted
         */
        public Optional<String> propsAbKey() {
            return Optional.ofNullable(propsAbKey);
        }

        /**
         * Returns the raw {@code <props/>} subtree.
         *
         * <p>The caller re-parses this subtree to extract the per-experiment overrides for this group.
         *
         * @return the {@code <props/>} stanza; never {@code null}
         */
        public Stanza propsNode() {
            return propsStanza;
        }

        /**
         * Tries to parse the inbound stanza as a {@link Success} variant.
         *
         * <p>Returns empty when the IQ-result envelope fails to validate or when the {@code <props/>}
         * child is missing. Otherwise it lifts the {@code hash}, {@code refresh}, {@code refresh_id},
         * and {@code ab_key} attributes and retains the subtree.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when it does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseSuccess",
                exports = "parseGetGroupExperimentConfigResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var props = stanza.getChild("props").orElse(null);
            if (props == null) {
                return Optional.empty();
            }
            var hash = props.getAttributeAsString("hash").orElse(null);
            var refresh = props.getAttributeAsInt("refresh").isPresent()
                    ? props.getAttributeAsInt("refresh").getAsInt()
                    : null;
            var refreshId = props.getAttributeAsInt("refresh_id").isPresent()
                    ? props.getAttributeAsInt("refresh_id").getAsInt()
                    : null;
            var abKey = props.getAttributeAsString("ab_key").orElse(null);
            return Optional.of(new Success(hash, refresh, refreshId, abKey, props));
        }

        /**
         * Compares this projection with another for value equality over all parsed fields.
         *
         * @param obj the object to compare against, may be {@code null}
         * @return {@code true} when {@code obj} is a {@code Success} with equal hash, refresh,
         *         refresh id, A/B key, and {@code <props/>} subtree; {@code false} otherwise
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
            return Objects.equals(this.propsHash, that.propsHash)
                    && Objects.equals(this.propsRefresh, that.propsRefresh)
                    && Objects.equals(this.propsRefreshId, that.propsRefreshId)
                    && Objects.equals(this.propsAbKey, that.propsAbKey)
                    && Objects.equals(this.propsStanza, that.propsStanza);
        }

        /**
         * Returns a hash code derived from all parsed fields.
         *
         * @return the combined hash of hash, refresh, refresh id, A/B key, and {@code <props/>} subtree
         */
        @Override
        public int hashCode() {
            return Objects.hash(propsHash, propsRefresh, propsRefreshId, propsAbKey, propsStanza);
        }

        /**
         * Returns a debug representation listing the scalar fields.
         *
         * <p>The {@code <props/>} subtree is omitted to keep the representation compact.
         *
         * @return a string of the form {@code SmaxAbPropsGetGroupExperimentConfigResponse.Success[propsHash=..., ...]}
         */
        @Override
        public String toString() {
            return "SmaxAbPropsGetGroupExperimentConfigResponse.Success[propsHash=" + propsHash
                    + ", propsRefresh=" + propsRefresh
                    + ", propsRefreshId=" + propsRefreshId
                    + ", propsAbKey=" + propsAbKey + ']';
        }
    }

    /**
     * Carries a do-not-retry rejection returned when the relay deems the request malformed or
     * unauthorised.
     *
     * <p>Surfaced for error codes below {@code 500}. The caller treats this as a permanent per-group
     * failure and does not re-issue the request.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseErrorNoRetry")
    final class ClientError implements SmaxAbPropsGetGroupExperimentConfigResponse {
        /**
         * Holds the numeric server-side error code, always below {@code 500} for this variant.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a client-error projection from the lifted error envelope.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the client-error envelope has been parsed from
         * the inbound stanza.
         *
         * @param errorCode the error code
         * @param errorText the error text, or {@code null} when omitted
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the error text, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse the inbound stanza as a {@link ClientError} variant.
         *
         * <p>Delegates envelope validation and the {@code code < 500} gate to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} and returns empty when the
         * stanza is not a client-error envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when it does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseErrorNoRetry",
                exports = "parseGetGroupExperimentConfigResponseErrorNoRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this projection with another for value equality over the error code and text.
         *
         * @param obj the object to compare against, may be {@code null}
         * @return {@code true} when {@code obj} is a {@code ClientError} with equal code and text;
         *         {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the combined hash of {@code errorCode} and {@code errorText}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the error code and text.
         *
         * @return a string of the form {@code SmaxAbPropsGetGroupExperimentConfigResponse.ClientError[errorCode=..., errorText=...]}
         */
        @Override
        public String toString() {
            return "SmaxAbPropsGetGroupExperimentConfigResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a retry-eligible failure returned when the relay hit a transient internal error.
     *
     * <p>Surfaced for error codes at or above {@code 500}. The caller re-issues the request after a
     * backoff.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseErrorRetry")
    final class ServerError implements SmaxAbPropsGetGroupExperimentConfigResponse {
        /**
         * Holds the numeric server-side error code, always at or above {@code 500} for this variant.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text, or {@code null} when the relay omitted it.
         */
        private final String errorText;

        /**
         * Constructs a server-error projection from the lifted error envelope.
         *
         * <p>Invoked by {@link #of(Stanza, Stanza)} once the server-error envelope has been parsed from
         * the inbound stanza.
         *
         * @param errorCode the error code
         * @param errorText the error text, or {@code null} when omitted
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the error text, when the relay supplied one.
         *
         * @return an {@link Optional} carrying the text, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse the inbound stanza as a {@link ServerError} variant.
         *
         * <p>Delegates envelope validation and the {@code code >= 500} gate to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} and returns empty when the
         * stanza is not a server-error envelope.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when it does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInAbPropsGetGroupExperimentConfigResponseErrorRetry",
                exports = "parseGetGroupExperimentConfigResponseErrorRetry",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this projection with another for value equality over the error code and text.
         *
         * @param obj the object to compare against, may be {@code null}
         * @return {@code true} when {@code obj} is a {@code ServerError} with equal code and text;
         *         {@code false} otherwise
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
         * Returns a hash code derived from the error code and text.
         *
         * @return the combined hash of {@code errorCode} and {@code errorText}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug representation listing the error code and text.
         *
         * @return a string of the form {@code SmaxAbPropsGetGroupExperimentConfigResponse.ServerError[errorCode=..., errorText=...]}
         */
        @Override
        public String toString() {
            return "SmaxAbPropsGetGroupExperimentConfigResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
