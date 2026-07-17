package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply to a {@link SmaxGroupsGetInviteGroupInfoRequest} as a sealed variant family.
 *
 * <p>{@link Success} carries the inviting group's preview metadata (size plus the raw {@code <group/>}
 * subtree); {@link ClientError} and {@link ServerError} surface the relay's reason codes. Callers pattern-match
 * the variant returned by {@link #of(Stanza)}.
 */
public sealed interface SmaxGroupsGetInviteGroupInfoResponse extends SmaxStanza.Response
        permits SmaxGroupsGetInviteGroupInfoResponse.Success, SmaxGroupsGetInviteGroupInfoResponse.ClientError, SmaxGroupsGetInviteGroupInfoResponse.ServerError {

    /**
     * Parses the inbound IQ stanza into the first matching variant.
     *
     * <p>The probes run in priority order: {@link Success}, then {@link ClientError} (HTTP-style 4xx codes),
     * then {@link ServerError} (HTTP-style 5xx codes). An empty {@link Optional} signals a stanza shape outside
     * the documented union.
     *
     * @implNote
     * This implementation defers the recovery decision to the caller rather than throwing a parsing-failure
     * exception, so the caller can apply its own error-handling policy.
     *
     * @param stanza the inbound IQ stanza received from the relay; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when no documented
     *         variant matched
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetInviteGroupInfoRPC",
            exports = "sendGetInviteGroupInfoRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetInviteGroupInfoResponse> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var success = Success.of(stanza);
        if (success.isPresent()) {
            return success;
        }
        var clientError = ClientError.of(stanza);
        if (clientError.isPresent()) {
            return clientError;
        }
        return ServerError.of(stanza);
    }

    /**
     * Reports that the relay accepted the invite code and returned the inviting group's preview.
     *
     * <p>The caller binds subject, picture, owner, and admin list from {@link #group()} and the participant
     * count from {@link #groupSize()}.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsInviteLinkGroupInfoMixin")
    final class Success implements SmaxGroupsGetInviteGroupInfoResponse {
        /**
         * Holds the participant count reported by the relay; clamped server-side to {@code [0, 19999]}.
         */
        private final int groupSize;

        /**
         * Holds the raw {@code <group/>} subtree carrying the invite-link group-info payload.
         */
        private final Stanza group;

        /**
         * Constructs a {@link Success} reply.
         *
         * <p>{@code groupSize} mirrors the relay's clamp; {@code 0} is valid for an empty preview.
         *
         * @param groupSize the participant count
         * @param group     the {@code <group/>} sub-stanza; never {@code null}
         * @throws NullPointerException     if {@code group} is {@code null}
         * @throws IllegalArgumentException if {@code groupSize} is negative
         */
        public Success(int groupSize, Stanza group) {
            if (groupSize < 0) {
                throw new IllegalArgumentException("groupSize must be non-negative");
            }
            this.groupSize = groupSize;
            this.group = Objects.requireNonNull(group, "group cannot be null");
        }

        /**
         * Returns the participant count reported by the relay.
         *
         * @return the group size
         */
        public int groupSize() {
            return groupSize;
        }

        /**
         * Returns the raw {@code <group/>} sub-stanza carrying the invite-link group-info payload.
         *
         * <p>The subtree is exposed verbatim so callers project subject, picture, owner, and admin list
         * directly via {@link Stanza#getAttributeAsString(String)} without committing to the full schema.
         *
         * @return the {@code <group/>} stanza; never {@code null}
         */
        public Stanza group() {
            return group;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant.
         *
         * <p>Matches when the IQ is a {@code type="result"} envelope carrying a {@code <group>} child; the
         * {@code size} attribute defaults to {@code 0} when the relay omits it.
         *
         * @param stanza the inbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the stanza
         *         does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseSuccess",
                exports = "parseGetInviteGroupInfoResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "result")) {
                return Optional.empty();
            }
            var group = stanza.getChild("group").orElse(null);
            if (group == null) {
                return Optional.empty();
            }
            var size = group.getAttributeAsInt("size").orElse(0);
            var success = new Success(size, group);
            return Optional.of(success);
        }

        /**
         * Compares this variant to {@code obj} for value equality across every field.
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
            return this.groupSize == that.groupSize && Objects.equals(this.group, that.group);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(groupSize, group);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetInviteGroupInfoResponse.Success[groupSize=" + groupSize
                    + ", group=" + group + ']';
        }
    }

    /**
     * Reports that the relay rejected the request as malformed, unauthorised, or referencing a revoked or
     * non-existent invite code.
     *
     * <p>The caller's UI typically maps {@link #errorCode()} into a "this invite link is no longer valid"
     * surface.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseClientError")
    final class ClientError implements SmaxGroupsGetInviteGroupInfoResponse {
        /**
         * Holds the numeric error code echoed by the relay (HTTP-style 4xx).
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
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
         * Parses the inbound stanza into a {@link ClientError} variant.
         *
         * <p>Matches when the IQ is a {@code type="error"} envelope carrying an {@code <error code="4xx"/>}
         * child.
         *
         * @param stanza the inbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the stanza
         *         does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseClientError",
                exports = "parseGetInviteGroupInfoResponseClientError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "error")) {
                return Optional.empty();
            }
            var error = stanza.getChild("error").orElse(null);
            if (error == null) {
                return Optional.empty();
            }
            var code = error.getAttributeAsInt("code").orElse(-1);
            if (code < 400 || code >= 500) {
                return Optional.empty();
            }
            var text = error.getAttributeAsString("text").orElse(null);
            var clientError = new ClientError(code, text);
            return Optional.of(clientError);
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsGetInviteGroupInfoResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Reports a transient relay-side failure.
     *
     * <p>Callers typically log it and surface a retry-eligible relay outage rather than a caller error.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseServerError")
    final class ServerError implements SmaxGroupsGetInviteGroupInfoResponse {
        /**
         * Holds the numeric error code echoed by the relay (HTTP-style 5xx).
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the relay.
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
         * Parses the inbound stanza into a {@link ServerError} variant.
         *
         * <p>Matches when the IQ is a {@code type="error"} envelope carrying an {@code <error code="5xx"/>}
         * child.
         *
         * @param stanza the inbound IQ stanza
         * @return an {@link Optional} carrying the parsed variant, or {@link Optional#empty()} when the stanza
         *         does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetInviteGroupInfoResponseServerError",
                exports = "parseGetInviteGroupInfoResponseServerError",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza) {
            if (!stanza.hasDescription("iq")) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "error")) {
                return Optional.empty();
            }
            var error = stanza.getChild("error").orElse(null);
            if (error == null) {
                return Optional.empty();
            }
            var code = error.getAttributeAsInt("code").orElse(-1);
            if (code < 500) {
                return Optional.empty();
            }
            var text = error.getAttributeAsString("text").orElse(null);
            var serverError = new ServerError(code, text);
            return Optional.of(serverError);
        }

        /**
         * Compares this variant to {@code obj} for value equality across both fields.
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
            return "SmaxGroupsGetInviteGroupInfoResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
