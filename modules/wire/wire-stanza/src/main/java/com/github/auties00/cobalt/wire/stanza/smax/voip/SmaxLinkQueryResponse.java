package com.github.auties00.cobalt.wire.stanza.smax.voip;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Projects the relay's reply to a {@link SmaxLinkQueryRequest} into a sealed
 * pair of variants.
 *
 * <p>An inbound {@code <ack class="call">} stanza resolves to either
 * {@link Success} (link metadata returned) or {@link ClientError} (token
 * rejected). The call-link join flow uses the result to decide whether to
 * present a join prompt, route the user through the waiting room, or surface an
 * "Invalid link" error.
 */
public sealed interface SmaxLinkQueryResponse extends SmaxStanza.Response
        permits SmaxLinkQueryResponse.Success, SmaxLinkQueryResponse.ClientError {

    /**
     * Parses an inbound stanza into the first matching reply variant.
     *
     * <p>The {@link Success} parser is tried first, then {@link ClientError}.
     *
     * @implNote
     * This implementation returns {@link Optional#empty()} on no match, where
     * the WA Web dispatcher would instead throw an {@code SmaxParsingFailure}.
     *
     * @param stanza    the inbound stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty on no match
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WASmaxVoipLinkQueryRPC",
            exports = "sendLinkQueryRPC", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends SmaxLinkQueryResponse> of(Stanza stanza, Stanza request) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        Objects.requireNonNull(request, "request cannot be null");
        var success = Success.of(stanza, request);
        if (success.isPresent()) {
            return success;
        }
        return ClientError.of(stanza, request);
    }

    /**
     * Validates the {@code <ack class="call">} envelope common to both reply
     * variants.
     *
     * <p>The envelope matches when the stanza has the {@code <ack>} description,
     * the {@code class="call"} marker, an {@code id} echoing the request's
     * {@code id}, and the literal {@code from="call"} server.
     *
     * @param stanza    the inbound stanza
     * @param request the original outbound request
     * @return {@code true} when the envelope matches, {@code false} otherwise
     */
    private static boolean validateAckEnvelope(Stanza stanza, Stanza request) {
        if (!stanza.hasDescription("ack")) {
            return false;
        }
        if (!stanza.hasAttribute("class", "call")) {
            return false;
        }
        var requestId = request.getAttributeAsString("id").orElse(null);
        if (requestId == null) {
            return false;
        }
        if (!stanza.hasAttribute("id", requestId)) {
            return false;
        }
        var from = stanza.getAttributeAsString("from").orElse(null);
        if (from == null || !from.equals(JidServer.call().toString())) {
            return false;
        }
        return true;
    }

    /**
     * The successful reply carrying the call link's full metadata.
     *
     * <p>Surfaces the creator identity (device JID, optional PN-form mapping,
     * optional display username), the echoed token and media, the
     * scheduled-event presence flag, and the optional waiting-room descriptor
     * consumed by the join UI.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInVoipLinkQueryResponseLinkQueryAck")
    final class Success implements SmaxLinkQueryResponse {
        /**
         * The creator-device JID carried by the {@code link_creator} attribute.
         */
        private final Jid linkQueryLinkCreator;

        /**
         * The optional creator phone-number JID carried by the
         * {@code link_creator_pn} attribute.
         *
         * <p>Supplied when the relay knows both the LID and PN identifiers for
         * the creator, which is typical for migrated 1:1 chat accounts.
         */
        private final Jid linkQueryLinkCreatorPn;

        /**
         * The optional creator-supplied display username carried by the
         * {@code link_creator_username} attribute.
         */
        private final String linkQueryLinkCreatorUsername;

        /**
         * The optional echoed action carried by the {@code action} attribute.
         *
         * <p>Either {@code "preview"} or {@code "edit"} when present.
         */
        private final String linkQueryAction;

        /**
         * The echoed call-link token carried by the {@code token} attribute.
         */
        private final String linkQueryToken;

        /**
         * The echoed media type carried by the {@code media} attribute.
         *
         * <p>Either {@code "audio"} or {@code "video"}.
         */
        private final String linkQueryMedia;

        /**
         * Whether the inner {@code <event/>} child is present.
         *
         * <p>A present child indicates the link is bound to a scheduled call.
         */
        private final boolean hasLinkQueryEvent;

        /**
         * The optional waiting-room descriptor parsed from the inner
         * {@code <waiting_room/>} child.
         */
        private final WaitingRoom linkQueryWaitingRoom;

        /**
         * Constructs a successful reply.
         *
         * @param linkQueryLinkCreator         the creator-device JID; never {@code null}
         * @param linkQueryLinkCreatorPn       the optional creator PN-form JID; may be {@code null}
         * @param linkQueryLinkCreatorUsername the optional creator display username; may be {@code null}
         * @param linkQueryAction              the optional echoed action; may be {@code null}
         * @param linkQueryToken               the echoed token; never {@code null}
         * @param linkQueryMedia               the echoed media type; never {@code null}
         * @param hasLinkQueryEvent            whether the link has an associated scheduled event
         * @param linkQueryWaitingRoom         the optional waiting-room descriptor; may be {@code null}
         * @throws NullPointerException if any non-optional argument is {@code null}
         */
        public Success(Jid linkQueryLinkCreator,
                       Jid linkQueryLinkCreatorPn,
                       String linkQueryLinkCreatorUsername,
                       String linkQueryAction,
                       String linkQueryToken,
                       String linkQueryMedia,
                       boolean hasLinkQueryEvent,
                       WaitingRoom linkQueryWaitingRoom) {
            this.linkQueryLinkCreator = Objects.requireNonNull(linkQueryLinkCreator, "linkQueryLinkCreator cannot be null");
            this.linkQueryLinkCreatorPn = linkQueryLinkCreatorPn;
            this.linkQueryLinkCreatorUsername = linkQueryLinkCreatorUsername;
            this.linkQueryAction = linkQueryAction;
            this.linkQueryToken = Objects.requireNonNull(linkQueryToken, "linkQueryToken cannot be null");
            this.linkQueryMedia = Objects.requireNonNull(linkQueryMedia, "linkQueryMedia cannot be null");
            this.hasLinkQueryEvent = hasLinkQueryEvent;
            this.linkQueryWaitingRoom = linkQueryWaitingRoom;
        }

        /**
         * Returns the creator-device JID.
         *
         * @return the creator JID; never {@code null}
         */
        public Jid linkQueryLinkCreator() {
            return linkQueryLinkCreator;
        }

        /**
         * Returns the optional creator phone-number JID.
         *
         * @return an {@link Optional} carrying the PN-form JID, or empty when omitted
         */
        public Optional<Jid> linkQueryLinkCreatorPn() {
            return Optional.ofNullable(linkQueryLinkCreatorPn);
        }

        /**
         * Returns the optional creator-supplied display username.
         *
         * @return an {@link Optional} carrying the username, or empty when omitted
         */
        public Optional<String> linkQueryLinkCreatorUsername() {
            return Optional.ofNullable(linkQueryLinkCreatorUsername);
        }

        /**
         * Returns the optional echoed action.
         *
         * @return an {@link Optional} carrying the action, or empty when omitted
         */
        public Optional<String> linkQueryAction() {
            return Optional.ofNullable(linkQueryAction);
        }

        /**
         * Returns the echoed call-link token.
         *
         * @return the token; never {@code null}
         */
        public String linkQueryToken() {
            return linkQueryToken;
        }

        /**
         * Returns the echoed media type.
         *
         * @return the media type; never {@code null}
         */
        public String linkQueryMedia() {
            return linkQueryMedia;
        }

        /**
         * Returns whether the link has an associated scheduled event.
         *
         * @return {@code true} when the inner {@code <event/>} child was present,
         *         {@code false} otherwise
         */
        public boolean hasLinkQueryEvent() {
            return hasLinkQueryEvent;
        }

        /**
         * Returns the optional waiting-room descriptor.
         *
         * @return an {@link Optional} carrying the descriptor, or empty when the
         *         {@code <waiting_room/>} child was absent
         */
        public Optional<WaitingRoom> linkQueryWaitingRoom() {
            return Optional.ofNullable(linkQueryWaitingRoom);
        }

        /**
         * Parses an inbound stanza into a {@link Success} variant.
         *
         * @implNote
         * This implementation requires the shared ack envelope, the
         * {@code type="link_query"} marker, and an inner {@code <link_query>}
         * child carrying non-null {@code link_creator}, {@code token}, and
         * {@code media} attributes; it then optionally parses
         * {@code link_creator_pn}, {@code link_creator_username},
         * {@code action}, the {@code <event/>} child presence, and the
         * {@code <waiting_room/>} descriptor.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInVoipLinkQueryResponseLinkQueryAck",
                exports = "parseLinkQueryResponseLinkQueryAck",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "link_query")) {
                return Optional.empty();
            }
            var linkQuery = stanza.getChild("link_query").orElse(null);
            if (linkQuery == null) {
                return Optional.empty();
            }
            var linkCreator = linkQuery.getAttributeAsString("link_creator")
                    .map(Jid::of)
                    .orElse(null);
            if (linkCreator == null) {
                return Optional.empty();
            }
            var token = linkQuery.getAttributeAsString("token").orElse(null);
            if (token == null) {
                return Optional.empty();
            }
            var media = linkQuery.getAttributeAsString("media").orElse(null);
            if (media == null) {
                return Optional.empty();
            }
            var linkCreatorPn = linkQuery.getAttributeAsString("link_creator_pn")
                    .map(Jid::of)
                    .orElse(null);
            var linkCreatorUsername = linkQuery.getAttributeAsString("link_creator_username").orElse(null);
            var action = linkQuery.getAttributeAsString("action").orElse(null);
            var hasEvent = linkQuery.getChild("event").isPresent();
            var waitingRoom = linkQuery.getChild("waiting_room")
                    .flatMap(WaitingRoom::of)
                    .orElse(null);
            return Optional.of(new Success(
                    linkCreator,
                    linkCreatorPn,
                    linkCreatorUsername,
                    action,
                    token,
                    media,
                    hasEvent,
                    waitingRoom));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with equal
         *         fields, {@code false} otherwise
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
            return this.hasLinkQueryEvent == that.hasLinkQueryEvent
                    && Objects.equals(this.linkQueryLinkCreator, that.linkQueryLinkCreator)
                    && Objects.equals(this.linkQueryLinkCreatorPn, that.linkQueryLinkCreatorPn)
                    && Objects.equals(this.linkQueryLinkCreatorUsername, that.linkQueryLinkCreatorUsername)
                    && Objects.equals(this.linkQueryAction, that.linkQueryAction)
                    && Objects.equals(this.linkQueryToken, that.linkQueryToken)
                    && Objects.equals(this.linkQueryMedia, that.linkQueryMedia)
                    && Objects.equals(this.linkQueryWaitingRoom, that.linkQueryWaitingRoom);
        }

        /**
         * Returns a hash code derived from every field of this reply.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(linkQueryLinkCreator, linkQueryLinkCreatorPn,
                    linkQueryLinkCreatorUsername, linkQueryAction, linkQueryToken,
                    linkQueryMedia, hasLinkQueryEvent, linkQueryWaitingRoom);
        }

        /**
         * Returns a debug string listing every field of this reply.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "SmaxLinkQueryResponse.Success[linkQueryLinkCreator=" + linkQueryLinkCreator
                    + ", linkQueryLinkCreatorPn=" + linkQueryLinkCreatorPn
                    + ", linkQueryLinkCreatorUsername=" + linkQueryLinkCreatorUsername
                    + ", linkQueryAction=" + linkQueryAction
                    + ", linkQueryToken=" + linkQueryToken
                    + ", linkQueryMedia=" + linkQueryMedia
                    + ", hasLinkQueryEvent=" + hasLinkQueryEvent
                    + ", linkQueryWaitingRoom=" + linkQueryWaitingRoom + ']';
        }

        /**
         * Projects the inner {@code <waiting_room is_admin enabled/>} child
         * carried by {@link Success} replies when the call link has a configured
         * waiting room.
         */
        public static final class WaitingRoom {
            /**
             * Whether the caller is an admin of this call link's waiting room.
             *
             * <p>Reflects the {@code is_admin="1"} marker; the relay omits the
             * attribute for non-admin callers.
             */
            private final boolean isAdmin;

            /**
             * The raw {@code enabled} attribute value.
             *
             * <p>Either {@code "0"} or {@code "1"} per the WA Web
             * {@code ENUM_0_1} validator.
             */
            private final String enabled;

            /**
             * Constructs a descriptor.
             *
             * @param isAdmin whether the caller is an admin of the waiting room
             * @param enabled the raw enabled string; never {@code null}
             * @throws NullPointerException if {@code enabled} is {@code null}
             */
            public WaitingRoom(boolean isAdmin, String enabled) {
                this.isAdmin = isAdmin;
                this.enabled = Objects.requireNonNull(enabled, "enabled cannot be null");
            }

            /**
             * Returns whether the caller is an admin of this call link's waiting
             * room.
             *
             * @return {@code true} when the caller is an admin, {@code false} otherwise
             */
            public boolean isAdmin() {
                return isAdmin;
            }

            /**
             * Returns the raw {@code enabled} attribute value.
             *
             * @return the enabled string; never {@code null}
             */
            public String enabled() {
                return enabled;
            }

            /**
             * Parses a descriptor from an inner {@code <waiting_room>} stanza.
             *
             * @implNote
             * This implementation asserts the {@code <waiting_room>} tag,
             * requires a non-null {@code enabled} attribute, and treats a
             * missing {@code is_admin} marker as {@code false}.
             *
             * @param stanza the inner stanza; never {@code null}
             * @return an {@link Optional} carrying the parsed descriptor, or empty when malformed
             * @throws NullPointerException if {@code stanza} is {@code null}
             */
            public static Optional<WaitingRoom> of(Stanza stanza) {
                Objects.requireNonNull(stanza, "stanza cannot be null");
                if (!stanza.hasDescription("waiting_room")) {
                    return Optional.empty();
                }
                var enabled = stanza.getAttributeAsString("enabled").orElse(null);
                if (enabled == null) {
                    return Optional.empty();
                }
                var isAdmin = stanza.hasAttribute("is_admin", "1");
                return Optional.of(new WaitingRoom(isAdmin, enabled));
            }

            /**
             * Compares this descriptor to another object for value equality.
             *
             * @param obj the object to compare against; may be {@code null}
             * @return {@code true} when {@code obj} is a {@link WaitingRoom} with
             *         equal fields, {@code false} otherwise
             */
            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (WaitingRoom) obj;
                return this.isAdmin == that.isAdmin && Objects.equals(this.enabled, that.enabled);
            }

            /**
             * Returns a hash code derived from every field of this descriptor.
             *
             * @return the hash code consistent with {@link #equals(Object)}
             */
            @Override
            public int hashCode() {
                return Objects.hash(isAdmin, enabled);
            }

            /**
             * Returns a debug string listing every field of this descriptor.
             *
             * @return the string representation of this descriptor
             */
            @Override
            public String toString() {
                return "SmaxLinkQueryResponse.Success.WaitingRoom[isAdmin=" + isAdmin
                        + ", enabled=" + enabled + ']';
            }
        }
    }

    /**
     * The client-error reply produced when the relay rejects the query.
     *
     * <p>Typical causes are an unknown or revoked token; the per-RPC
     * {@link #errorToken()} echoes the token that triggered the error so the
     * caller can distinguish errors when multiple queries are in flight.
     */
    @WhatsAppWebModule(moduleName = "WASmaxInVoipLinkQueryResponseLinkQueryNack")
    final class ClientError implements SmaxLinkQueryResponse {
        /**
         * The numeric error code parsed from the {@code error} attribute, or
         * {@code -1} when the raw value is non-numeric.
         */
        private final int errorCode;

        /**
         * The raw {@code error} attribute value.
         */
        private final String errorText;

        /**
         * The per-RPC token carried by the inner {@code <error>} child's
         * {@code token} attribute.
         *
         * <p>Identifies which token the error refers to.
         */
        private final String errorToken;

        /**
         * Constructs a client-error reply.
         *
         * @param errorCode  the numeric code; {@code -1} when the raw attribute is non-numeric
         * @param errorText  the raw error string; may be {@code null}
         * @param errorToken the per-RPC token; may be {@code null}
         */
        public ClientError(int errorCode, String errorText, String errorToken) {
            this.errorCode = errorCode;
            this.errorText = errorText;
            this.errorToken = errorToken;
        }

        /**
         * Returns the numeric error code.
         *
         * @return the error code, or {@code -1} when the raw {@code error}
         *         attribute was non-numeric
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional raw error string.
         *
         * @return an {@link Optional} carrying the error string, or empty when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Returns the per-RPC token carried by the inner {@code <error>} child.
         *
         * @return an {@link Optional} carrying the token, or empty when omitted
         */
        public Optional<String> errorToken() {
            return Optional.ofNullable(errorToken);
        }

        /**
         * Parses an inbound stanza into a {@link ClientError} variant.
         *
         * @implNote
         * This implementation requires the shared ack envelope, the
         * {@code type="link_query"} marker, a non-null {@code error} attribute,
         * and a non-null {@code token} attribute on the inner {@code <error>}
         * child; the {@code error} value is parsed as a decimal integer and
         * falls back to {@code -1} when non-numeric.
         *
         * @param stanza    the inbound stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty on schema mismatch
         */
        @WhatsAppWebExport(moduleName = "WASmaxInVoipLinkQueryResponseLinkQueryNack",
                exports = "parseLinkQueryResponseLinkQueryNack",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            if (!validateAckEnvelope(stanza, request)) {
                return Optional.empty();
            }
            if (!stanza.hasAttribute("type", "link_query")) {
                return Optional.empty();
            }
            var errorAttr = stanza.getAttributeAsString("error").orElse(null);
            if (errorAttr == null) {
                return Optional.empty();
            }
            var errorChild = stanza.getChild("error").orElse(null);
            if (errorChild == null) {
                return Optional.empty();
            }
            var token = errorChild.getAttributeAsString("token").orElse(null);
            if (token == null) {
                return Optional.empty();
            }
            int code;
            try {
                code = Integer.parseInt(errorAttr);
            } catch (NumberFormatException _) {
                code = -1;
            }
            return Optional.of(new ClientError(code, errorAttr, token));
        }

        /**
         * Compares this reply to another object for value equality.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with
         *         equal fields, {@code false} otherwise
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
                    && Objects.equals(this.errorText, that.errorText)
                    && Objects.equals(this.errorToken, that.errorToken);
        }

        /**
         * Returns a hash code derived from every field of this reply.
         *
         * @return the hash code consistent with {@link #equals(Object)}
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText, errorToken);
        }

        /**
         * Returns a debug string listing every field of this reply.
         *
         * @return the string representation of this reply
         */
        @Override
        public String toString() {
            return "SmaxLinkQueryResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText
                    + ", errorToken=" + errorToken + ']';
        }
    }
}
