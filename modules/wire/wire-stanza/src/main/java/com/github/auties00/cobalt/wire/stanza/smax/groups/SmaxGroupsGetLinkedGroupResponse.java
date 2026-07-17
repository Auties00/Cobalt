package com.github.auties00.cobalt.wire.stanza.smax.groups;

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
 * The sealed reply family for a {@link SmaxGroupsGetLinkedGroupRequest}.
 * <p>
 * Exactly one of three variants matches a given inbound stanza: {@link Success} carries the located linked group's
 * preview metadata, {@link ClientError} carries a caller-side rejection code, and {@link ServerError} carries a
 * transient relay-side failure code.
 */
public sealed interface SmaxGroupsGetLinkedGroupResponse extends SmaxStanza.Response
        permits SmaxGroupsGetLinkedGroupResponse.Success, SmaxGroupsGetLinkedGroupResponse.ClientError, SmaxGroupsGetLinkedGroupResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each {@link SmaxGroupsGetLinkedGroupResponse} variant and returns the first that
     * parses cleanly.
     * <p>
     * Variants are tried in priority order: {@link Success} first, then {@link ClientError}, then {@link ServerError}.
     * The result is empty when the stanza matches none of the three variants.
     *
     * @implNote This implementation defers the no-match decision to the caller by returning an empty {@link Optional}
     * rather than throwing, so the caller can apply its own error-handling policy.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsGetLinkedGroupRequest} stanza, used to validate echoed ids
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsGetLinkedGroupRPC",
            exports = "sendGetLinkedGroupRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsGetLinkedGroupResponse> of(Stanza stanza, Stanza request) {
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
     * The reply variant emitted when the relay located the linked group and returned its preview.
     * <p>
     * The linked group's subject, picture, owner, admin list, ephemeral expiration, and addressing mode are carried
     * verbatim in the {@link #group()} subtree; {@link #linkedGroupJid()} echoes the wrapper's {@code jid} and
     * {@link #groupSize()} carries the participant count when the relay supplied it.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupResponseSuccess")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsLinkedGroupInfoMixin")
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGroupAddressingModeMixin")
    final class Success implements SmaxGroupsGetLinkedGroupResponse {
        /**
         * The linked group's {@link Jid} echoed on the {@code <linked_group jid="..."/>} wrapper.
         */
        private final Jid linkedGroupJid;

        /**
         * The optional participant count; {@code null} when the relay omitted the {@code size} attribute.
         */
        private final Integer groupSize;

        /**
         * The raw {@code <group/>} subtree carrying the group-info payload.
         */
        private final Stanza group;

        /**
         * Constructs a {@link Success} reply.
         * <p>
         * Pass {@code null} for {@code groupSize} when the relay's response omits the {@code size} attribute; a
         * non-null negative value is rejected.
         *
         * @param linkedGroupJid the linked group {@link Jid}; never {@code null}
         * @param groupSize      the optional participant count; may be {@code null}
         * @param group          the {@code <group/>} sub-stanza; never {@code null}
         * @throws NullPointerException     if {@code linkedGroupJid} or {@code group} is {@code null}
         * @throws IllegalArgumentException if {@code groupSize} is negative
         */
        public Success(Jid linkedGroupJid, Integer groupSize, Stanza group) {
            this.linkedGroupJid = Objects.requireNonNull(linkedGroupJid, "linkedGroupJid cannot be null");
            if (groupSize != null && groupSize < 0) {
                throw new IllegalArgumentException("groupSize must be non-negative");
            }
            this.groupSize = groupSize;
            this.group = Objects.requireNonNull(group, "group cannot be null");
        }

        /**
         * Returns the linked group's {@link Jid}.
         *
         * @return the linked group {@link Jid}; never {@code null}
         */
        public Jid linkedGroupJid() {
            return linkedGroupJid;
        }

        /**
         * Returns the linked group's participant count.
         *
         * @return an {@link Optional} carrying the participant count, or empty when the relay omitted the {@code size}
         *         attribute
         */
        public Optional<Integer> groupSize() {
            return Optional.ofNullable(groupSize);
        }

        /**
         * Returns the raw {@code <group/>} sub-stanza carrying the group-info payload.
         * <p>
         * The subtree is exposed verbatim so callers project subject, picture, owner, admin list, ephemeral
         * expiration, and addressing mode directly without committing to the full mixin schema.
         *
         * @return the {@code <group/>} stanza; never {@code null}
         */
        public Stanza group() {
            return group;
        }

        /**
         * Tries to parse a {@link Success} variant from {@code stanza}.
         * <p>
         * The envelope is validated through {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} and the
         * {@code <linked_group jid="..."><group .../></linked_group>} payload must be present; the {@code size}
         * attribute is optional.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupResponseSuccess",
                exports = "parseGetLinkedGroupResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var linkedGroup = stanza.getChild("linked_group").orElse(null);
            if (linkedGroup == null) {
                return Optional.empty();
            }
            var linkedGroupJid = linkedGroup.getAttributeAsJid("jid").orElse(null);
            if (linkedGroupJid == null) {
                return Optional.empty();
            }
            var group = linkedGroup.getChild("group").orElse(null);
            if (group == null) {
                return Optional.empty();
            }
            var sizeOpt = group.getAttributeAsInt("size");
            var size = sizeOpt.isPresent() ? sizeOpt.getAsInt() : null;
            return Optional.of(new Success(linkedGroupJid, size, group));
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
            return Objects.equals(this.groupSize, that.groupSize)
                    && Objects.equals(this.linkedGroupJid, that.linkedGroupJid)
                    && Objects.equals(this.group, that.group);
        }

        /**
         * Returns a hash composed of every field.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(linkedGroupJid, groupSize, group);
        }

        /**
         * Returns a debug string carrying every field.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsGetLinkedGroupResponse.Success[linkedGroupJid=" + linkedGroupJid
                    + ", groupSize=" + groupSize + ", group=" + group + ']';
        }
    }

    /**
     * The reply variant emitted when the relay rejected the linked-group lookup as malformed, unauthorised, or
     * referencing a non-existent linkage.
     * <p>
     * The {@link #errorCode()} carries the HTTP-style status assigned by the relay and {@link #errorText()} carries
     * the optional human-readable reason.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupResponseClientError")
    final class ClientError implements SmaxGroupsGetLinkedGroupResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupResponseClientError",
                exports = "parseGetLinkedGroupResponseClientError",
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
            return "SmaxGroupsGetLinkedGroupResponse.ClientError[errorCode=" + errorCode
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
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsGetLinkedGroupResponseServerError")
    final class ServerError implements SmaxGroupsGetLinkedGroupResponse {
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
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsGetLinkedGroupResponseServerError",
                exports = "parseGetLinkedGroupResponseServerError",
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
            return "SmaxGroupsGetLinkedGroupResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
