package com.github.auties00.cobalt.wire.stanza.iq.status;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.privacy.StatusPrivacyMode;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Roots the sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqQueryStatusPrivacyRequest}.
 *
 * <p>The hierarchy permits exactly three variants. {@link Success} carries the parsed audience
 * selector ({@link StatusPrivacyMode}) and the paired JID list; {@link ClientError} surfaces a relay
 * rejection in the sub-{@code 500} code range; {@link ServerError} surfaces a transient relay failure
 * in the {@code 500}-and-above range. Callers switch on the parsed variant to discriminate the relay
 * outcome.
 */
@WhatsAppWebModule(moduleName = "WAWebUserPrefsStatus")
public sealed interface IqQueryStatusPrivacyResponse extends IqStanza.Response
        permits IqQueryStatusPrivacyResponse.Success, IqQueryStatusPrivacyResponse.ClientError, IqQueryStatusPrivacyResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching {@link IqQueryStatusPrivacyResponse} variant.
     *
     * <p>Each variant's {@code of(stanza, request)} factory is tried in priority order, success then
     * client-error then server-error, and the first present result is returned. An empty result means
     * no documented variant matched.
     *
     * @param stanza  the inbound IQ stanza received from the relay; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant
     *         matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqQueryStatusPrivacyResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the success outcome: the parsed status-privacy audience selector and paired JID list.
     *
     * <p>{@link #mode()} is the audience selector and {@link #jids()} is its companion list, whose
     * meaning depends on the mode: the allowlist for {@link StatusPrivacyMode#WHITELIST}, the
     * blocklist for {@link StatusPrivacyMode#CONTACTS_EXCEPT}, and empty for
     * {@link StatusPrivacyMode#CONTACTS}.
     */
    @WhatsAppWebModule(moduleName = "WAWebUserPrefsStatus")
    final class Success implements IqQueryStatusPrivacyResponse {
        /**
         * Holds the parsed audience selector.
         */
        private final StatusPrivacyMode mode;

        /**
         * Holds the JIDs paired with the mode, in wire order.
         */
        private final List<Jid> jids;

        /**
         * Constructs a successful reply bound to the given mode and JID list.
         *
         * @param mode the audience selector; never {@code null}
         * @param jids the paired JID list; never {@code null}
         * @throws NullPointerException if {@code mode} or {@code jids} is {@code null}
         */
        public Success(StatusPrivacyMode mode, List<Jid> jids) {
            this.mode = Objects.requireNonNull(mode, "mode cannot be null");
            Objects.requireNonNull(jids, "jids cannot be null");
            this.jids = List.copyOf(jids);
        }

        /**
         * Returns the parsed audience selector.
         *
         * @return the mode; never {@code null}
         */
        public StatusPrivacyMode mode() {
            return mode;
        }

        /**
         * Returns the JIDs paired with the mode.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Jid> jids() {
            return jids;
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches the success
         * schema.
         *
         * <p>Returns empty when the {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)}
         * result-envelope check fails or when the {@code <privacy>} child is absent. The mode is read
         * from the {@code list} attribute, falling back to {@code type} and then to {@code "contacts"};
         * the paired JIDs are read from the {@code jid} attribute of each {@code <user>} grandchild.
         *
         * @implNote This implementation maps {@code "contacts"} to {@link StatusPrivacyMode#CONTACTS},
         * {@code "contact_whitelist"}/{@code "allowlist"}/{@code "whitelist"} to
         * {@link StatusPrivacyMode#WHITELIST}, and
         * {@code "contact_blacklist"}/{@code "denylist"}/{@code "blacklist"} to
         * {@link StatusPrivacyMode#CONTACTS_EXCEPT}, defaulting unknown values to
         * {@link StatusPrivacyMode#CONTACTS}.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebUserPrefsStatus", exports = "getStatusPrivacySetting",
                adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var privacyNode = stanza.getChild("privacy").orElse(null);
            if (privacyNode == null) {
                return Optional.empty();
            }
            var modeAttr = privacyNode.getAttributeAsString("list")
                    .or(() -> privacyNode.getAttributeAsString("type"))
                    .orElse("contacts");
            var mode = switch (modeAttr) {
                case "contacts" -> StatusPrivacyMode.CONTACTS;
                case "contact_whitelist", "allowlist", "whitelist" -> StatusPrivacyMode.WHITELIST;
                case "contact_blacklist", "denylist", "blacklist" -> StatusPrivacyMode.CONTACTS_EXCEPT;
                default -> StatusPrivacyMode.CONTACTS;
            };
            var jids = privacyNode.streamChildren("user")
                    .flatMap(userNode -> userNode.streamAttributeAsJid("jid"))
                    .toList();
            return Optional.of(new Success(mode, jids));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two success variants are equal when they share the same runtime class, the same
         * {@link #mode()}, and the same {@link #jids()}.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal success variant
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
            return this.mode == that.mode
                    && Objects.equals(this.jids, that.jids);
        }

        /**
         * Returns a hash code derived from the mode and JID list.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(mode, jids);
        }

        /**
         * Returns a debug string carrying the mode and JID list.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryStatusPrivacyResponse.Success[mode=" + mode + ", jids=" + jids + ']';
        }
    }

    /**
     * Carries a client-error outcome: the relay rejected the query with a sub-{@code 500} code.
     *
     * <p>A client error is uncommon here because the request has no payload; it typically signals an
     * authorisation or session-state problem rather than a malformed request.
     */
    @WhatsAppWebModule(moduleName = "WAWebUserPrefsStatus")
    final class ClientError implements IqQueryStatusPrivacyResponse {
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it matches the standard
         * SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two client-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal client-error variant
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryStatusPrivacyResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Carries a server-error outcome: the relay hit a transient {@code 500}-and-above failure
     * processing the query.
     *
     * <p>The failure is typically retryable after a short backoff.
     */
    @WhatsAppWebModule(moduleName = "WAWebUserPrefsStatus")
    final class ServerError implements IqQueryStatusPrivacyResponse {
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
         * @return an {@link Optional} carrying the text, or empty when the relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it matches the standard
         * SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails; the parse is delegated entirely to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza  the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this variant to another object for equality.
         *
         * <p>Two server-error variants are equal when they share the same runtime class, the same
         * {@link #errorCode()}, and the same error text.
         *
         * @param obj the object to compare against
         * @return {@code true} when {@code obj} is an equal server-error variant
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
         * Returns a hash code derived from the error code and error text.
         *
         * @return the field-derived hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debug string carrying the error code and error text.
         *
         * @return a string representation
         */
        @Override
        public String toString() {
            return "IqQueryStatusPrivacyResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
