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
 * Models the reply family for a {@link SmaxGroupsUnlinkGroupsRequest}.
 *
 * <p>Exactly one of {@link Success}, {@link ClientError}, or {@link ServerError} parses cleanly from a given
 * inbound IQ stanza. {@link Success} wraps the per-sub-group result rows returned by the relay; individual rows
 * may carry an {@link Success.UnlinkedGroup#errorTag()} signalling a per-sub-group failure even when the IQ
 * envelope itself succeeded, so consumers must walk {@link Success#unlinkedGroups()} to detect partial failures
 * rather than treating a {@link Success} verdict as a guarantee that every sub-group was unlinked.
 */
@WhatsAppWebModule(moduleName = "WASmaxGroupsUnlinkGroupsRPC")
public sealed interface SmaxGroupsUnlinkGroupsResponse extends SmaxStanza.Response
        permits SmaxGroupsUnlinkGroupsResponse.Success, SmaxGroupsUnlinkGroupsResponse.ClientError, SmaxGroupsUnlinkGroupsResponse.ServerError {

    /**
     * Dispatches the inbound IQ across each variant in priority order and returns the first that parses cleanly.
     *
     * <p>The variants are tried in the order {@link Success}, {@link ClientError}, {@link ServerError}, matching
     * the priority of the WA Web RPC dispatcher. An empty result means the stanza matched none of the documented
     * shapes; the original {@code request} is forwarded to each variant parser so echoed identifiers can be
     * validated against the outbound stanza.
     *
     * @implNote This implementation returns an empty {@link Optional} when no variant matches and leaves the
     * error-handling decision to the caller, whereas WA Web throws a parsing failure on the same path.
     *
     * @param stanza    the inbound IQ stanza
     * @param request the original outbound {@link SmaxGroupsUnlinkGroupsRequest} stanza, used to validate echoed
     *                identifiers
     * @return an {@link Optional} carrying the parsed variant, or empty when no variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxGroupsUnlinkGroupsRPC",
            exports = "sendUnlinkGroupsRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxGroupsUnlinkGroupsResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the per-sub-group result rows returned when the relay accepted the request envelope.
     *
     * <p>The IQ envelope succeeds even when individual rows carry an {@link UnlinkedGroup#errorTag()} signalling a
     * per-sub-group failure (one of the six tags listed on {@link UnlinkedGroup}), so consumers must walk
     * {@link #unlinkedGroups()} to detect partial failures.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsUnlinkGroupsResponseSuccess")
    final class Success implements SmaxGroupsUnlinkGroupsResponse {
        /**
         * Holds the per-sub-group result rows projected from the {@code <unlink>} child.
         */
        private final List<UnlinkedGroup> unlinkedGroups;

        /**
         * Constructs a success reply from its per-sub-group result rows.
         *
         * <p>The supplied list is defensively copied so the constructed instance is immutable.
         *
         * @param unlinkedGroups the per-sub-group result rows
         * @throws NullPointerException if {@code unlinkedGroups} is {@code null}
         */
        public Success(List<UnlinkedGroup> unlinkedGroups) {
            Objects.requireNonNull(unlinkedGroups, "unlinkedGroups cannot be null");
            this.unlinkedGroups = List.copyOf(unlinkedGroups);
        }

        /**
         * Returns the per-sub-group result rows.
         *
         * @return an unmodifiable list of {@link UnlinkedGroup} entries; never {@code null}
         */
        public List<UnlinkedGroup> unlinkedGroups() {
            return unlinkedGroups;
        }

        /**
         * Tries to parse a success reply from {@code stanza}.
         *
         * <p>The IQ must validate as a {@code type="result"} echo of {@code request} per
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}, must carry an
         * {@code <unlink unlink_type="sub_group">} child, and that child must hold at least one {@code <group>}
         * grand-child carrying a {@code jid} attribute. Each {@code <group>} contributes one {@link UnlinkedGroup}
         * row; its first child whose description is one of the six discriminators recognised by
         * {@link UnlinkedGroup#isErrorTag(String)} becomes the row's {@link UnlinkedGroup#errorTag()}. The result is
         * empty when validation fails, the {@code <unlink>} child is absent or carries a different
         * {@code unlink_type}, any {@code <group>} lacks a {@code jid}, or no rows are produced.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsUnlinkGroupsResponseSuccess",
                exports = "parseUnlinkGroupsResponseSuccess",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var unlink = stanza.getChild("unlink").orElse(null);
            if (unlink == null) {
                return Optional.empty();
            }
            if (!unlink.hasAttribute("unlink_type", "sub_group")) {
                return Optional.empty();
            }
            var unlinkedGroups = new ArrayList<UnlinkedGroup>();
            for (var groupNode : unlink.getChildren("group")) {
                var jid = groupNode.getAttributeAsJid("jid").orElse(null);
                if (jid == null) {
                    return Optional.empty();
                }
                var removeOrphaned = groupNode.hasAttribute("remove_orphaned_members", "true");
                String errorTag = null;
                for (var child : groupNode.children()) {
                    var description = child.description();
                    if (description == null) {
                        continue;
                    }
                    if (UnlinkedGroup.isErrorTag(description)) {
                        errorTag = description;
                        break;
                    }
                }
                unlinkedGroups.add(new UnlinkedGroup(jid, removeOrphaned, errorTag));
            }
            if (unlinkedGroups.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Success(unlinkedGroups));
        }

        /**
         * Compares this success to {@code obj} for value equality on {@link #unlinkedGroups()}.
         *
         * @param obj the other object
         * @return {@code true} when {@code obj} is a {@link Success} with the same result rows
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
            return Objects.equals(this.unlinkedGroups, that.unlinkedGroups);
        }

        /**
         * Returns a hash derived from {@link #unlinkedGroups()}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(unlinkedGroups);
        }

        /**
         * Returns a debug string carrying {@link #unlinkedGroups()}.
         *
         * @return the debug representation
         */
        @Override
        public String toString() {
            return "SmaxGroupsUnlinkGroupsResponse.Success[unlinkedGroups=" + unlinkedGroups + ']';
        }

        /**
         * Holds one per-sub-group result row inside a {@link Success}.
         *
         * <p>{@link #errorTag()} captures the per-sub-group discriminator emitted by the relay when an individual
         * unlink fails; it is empty when the unlink succeeded for that sub-group. The recognised tags are
         * {@code "bad_request"}, {@code "not_authorized"}, {@code "not_exist"}, {@code "not_acceptable"},
         * {@code "partial_server_error"}, and {@code "server_error"}.
         */
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsUnlinkGroupsResponseSuccess")
        @WhatsAppWebModule(moduleName = "WASmaxInGroupsSubGroupBadRequestOrNotAuthorizedOrNotExistOrNotAcceptableOrPartialServerErrorOrServerErrorMixinGroup")
        public static final class UnlinkedGroup {
            /**
             * Returns whether {@code description} is one of the six recognised sub-group error discriminator tags.
             *
             * @param description the child tag to test
             * @return {@code true} when the tag is a sub-group error discriminator
             */
            private static boolean isErrorTag(String description) {
                return "bad_request".equals(description)
                        || "not_authorized".equals(description)
                        || "not_exist".equals(description)
                        || "not_acceptable".equals(description)
                        || "partial_server_error".equals(description)
                        || "server_error".equals(description);
            }

            /**
             * Holds the sub-group {@link Jid} echoed by the relay.
             */
            private final Jid jid;

            /**
             * Holds whether the relay echoed the {@code remove_orphaned_members="true"} flag.
             */
            private final boolean removeOrphanedMembers;

            /**
             * Holds the per-sub-group error-discriminator tag, or {@code null} when the unlink succeeded.
             */
            private final String errorTag;

            /**
             * Constructs a result row from its sub-group identifier, eviction flag, and optional error tag.
             *
             * @param jid                   the sub-group {@link Jid}
             * @param removeOrphanedMembers whether the relay echoed the eviction flag
             * @param errorTag              the optional error-discriminator tag; may be {@code null}
             * @throws NullPointerException if {@code jid} is {@code null}
             */
            public UnlinkedGroup(Jid jid, boolean removeOrphanedMembers, String errorTag) {
                this.jid = Objects.requireNonNull(jid, "jid cannot be null");
                this.removeOrphanedMembers = removeOrphanedMembers;
                this.errorTag = errorTag;
            }

            /**
             * Returns the sub-group {@link Jid}.
             *
             * @return the sub-group {@link Jid}; never {@code null}
             */
            public Jid jid() {
                return jid;
            }

            /**
             * Returns whether the relay echoed the eviction flag.
             *
             * @return {@code true} when the {@code remove_orphaned_members="true"} attribute is present
             */
            public boolean removeOrphanedMembers() {
                return removeOrphanedMembers;
            }

            /**
             * Returns the per-sub-group error-discriminator tag.
             *
             * @return an {@link Optional} carrying the tag, or empty when the unlink succeeded for this sub-group
             */
            public Optional<String> errorTag() {
                return Optional.ofNullable(errorTag);
            }

            /**
             * Compares this row to {@code obj} for value equality across every field.
             *
             * @param obj the other object
             * @return {@code true} when {@code obj} is an {@link UnlinkedGroup} with identical fields
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (UnlinkedGroup) obj;
                return this.removeOrphanedMembers == that.removeOrphanedMembers
                        && Objects.equals(this.jid, that.jid)
                        && Objects.equals(this.errorTag, that.errorTag);
            }

            /**
             * Returns a hash composed of every field.
             *
             * @return the hash code
             */
            @Override
            public int hashCode() {
                return Objects.hash(jid, removeOrphanedMembers, errorTag);
            }

            /**
             * Returns a debug string carrying every field.
             *
             * @return the debug representation
             */
            @Override
            public String toString() {
                return "SmaxGroupsUnlinkGroupsResponse.Success.UnlinkedGroup[jid=" + jid
                        + ", removeOrphanedMembers=" + removeOrphanedMembers
                        + ", errorTag=" + errorTag + ']';
            }
        }
    }

    /**
     * Models the reply emitted when the relay rejected the request envelope.
     *
     * <p>This variant covers rejections such as a malformed envelope, an unauthorised caller, or a reference to a
     * non-existent parent or sub-group pairing.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsUnlinkGroupsResponseClientError")
    final class ClientError implements SmaxGroupsUnlinkGroupsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay, or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from its error code and optional error text.
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
         * Returns the human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a client-error reply from {@code stanza}.
         *
         * <p>Validation is delegated to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}, which
         * matches the shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope for the
         * client-error code range; its {@link com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin.Envelope#code()}
         * and {@link com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin.Envelope#text()} populate
         * this reply. The result is empty when the envelope does not match.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsUnlinkGroupsResponseClientError",
                exports = "parseUnlinkGroupsResponseClientError",
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
            return "SmaxGroupsUnlinkGroupsResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the reply emitted on a transient relay-side failure.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInGroupsUnlinkGroupsResponseServerError")
    final class ServerError implements SmaxGroupsUnlinkGroupsResponse {
        /**
         * Holds the numeric error code echoed by the relay.
         */
        private final int errorCode;

        /**
         * Holds the human-readable error text echoed by the relay, or {@code null} when omitted.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from its error code and optional error text.
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
         * Returns the human-readable error text echoed by the relay.
         *
         * @return an {@link Optional} carrying the error text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a server-error reply from {@code stanza}.
         *
         * <p>Validation is delegated to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}, which
         * matches the shared {@code <iq type="error"><error code="..." text="..."/></iq>} envelope for the
         * server-error code range; its {@link com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin.Envelope#code()}
         * and {@link com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqErrorResponseMixin.Envelope#text()} populate
         * this reply. The result is empty when the envelope does not match.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match
         */
        @WhatsAppWebExport(moduleName = "WASmaxInGroupsUnlinkGroupsResponseServerError",
                exports = "parseUnlinkGroupsResponseServerError",
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
            return "SmaxGroupsUnlinkGroupsResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
