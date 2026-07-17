package com.github.auties00.cobalt.wire.stanza.iq.biz;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxBaseServerErrorMixin;
import com.github.auties00.cobalt.wire.stanza.smax.util.SmaxIqResultResponseMixin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the inbound reply the relay produces in response to an {@link IqSetMerchantComplianceRequest}.
 *
 * <p>The sealed family is pattern-matched to drive the merchant-compliance edit surface:
 * {@link Success} echoes the post-mutation compliance entries (same shape as
 * {@link IqGetMerchantComplianceResponse.Success}), {@link ClientError} surfaces a rejected mutation
 * and {@link ServerError} surfaces a transient internal failure.
 */
@WhatsAppWebModule(moduleName = "WAWebMerchantComplianceJob")
public sealed interface IqSetMerchantComplianceResponse extends IqStanza.Response
        permits IqSetMerchantComplianceResponse.Success, IqSetMerchantComplianceResponse.ClientError, IqSetMerchantComplianceResponse.ServerError {

    /**
     * Tries each variant in priority order until one matches.
     *
     * <p>The order is {@link Success}, then {@link ClientError}, then {@link ServerError}; call this
     * on every IQ stanza acking a compliance mutation.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqSetMerchantComplianceResponse> of(Stanza stanza, Stanza request) {
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
     * Models the {@code Success} variant, which carries the post-mutation compliance entries.
     *
     * <p>The relay echoes the same shape as {@link IqGetMerchantComplianceResponse.Success}, so the
     * caller can swap the cached merchant-compliance projection in place from {@link #entries()}
     * without re-fetching.
     */
    final class Success implements IqSetMerchantComplianceResponse {
        /**
         * Holds the post-mutation entries echoed by the relay.
         */
        private final List<IqGetMerchantComplianceResponse.Success.MerchantInfo> entries;

        /**
         * Constructs a success reply from the echoed entries; called from {@link #of(Stanza, Stanza)}.
         *
         * <p>The supplied list is defensively copied.
         *
         * @param entries the entries; never {@code null}
         * @throws NullPointerException if {@code entries} is {@code null}
         */
        public Success(List<IqGetMerchantComplianceResponse.Success.MerchantInfo> entries) {
            Objects.requireNonNull(entries, "entries cannot be null");
            this.entries = List.copyOf(entries);
        }

        /**
         * Returns the post-mutation entries.
         *
         * <p>An empty list is legal when the relay echoes a bare success.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<IqGetMerchantComplianceResponse.Success.MerchantInfo> entries() {
            return entries;
        }

        /**
         * Tries to parse a {@link Success} variant from the stanza.
         *
         * <p>The method validates the {@code <iq type="result">} envelope and decodes every echoed
         * {@code <merchant_info/>} child; called from {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation shares the projection shape with
         * {@link IqGetMerchantComplianceResponse.Success}; both routines decode
         * {@code <customer_care_details/>} and {@code <grievance_officer_details/>} grandchildren
         * into empty-string defaults when a sub-element is absent.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not
         *         match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebMerchantComplianceJob",
                exports = "merchantComplianceResponse", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var entries = new ArrayList<IqGetMerchantComplianceResponse.Success.MerchantInfo>();
            for (var miNode : stanza.getChildren("merchant_info")) {
                var entityName = miNode.getChild("entity_name")
                        .flatMap(Stanza::toContentString).orElse("");
                var entityType = miNode.getChild("entity_type")
                        .flatMap(Stanza::toContentString).orElse("");
                var entityTypeCustom = miNode.getChild("entity_type_custom")
                        .flatMap(Stanza::toContentString).orElse(null);
                var registered = miNode.getAttributeAsString("is_registered")
                        .map("true"::equals).orElse(false);
                var ccdNode = miNode.getChild("customer_care_details").orElse(null);
                var ccdEmail = ccdNode == null
                        ? ""
                        : ccdNode.getChild("email").flatMap(Stanza::toContentString).orElse("");
                var ccdLandline = ccdNode == null
                        ? ""
                        : ccdNode.getChild("landline_number").flatMap(Stanza::toContentString).orElse("");
                var ccdMobile = ccdNode == null
                        ? ""
                        : ccdNode.getChild("mobile_number").flatMap(Stanza::toContentString).orElse("");
                var ccd = new IqGetMerchantComplianceResponse.Success.BusinessContactDetails(
                        ccdEmail, ccdLandline, ccdMobile);
                var godNode = miNode.getChild("grievance_officer_details").orElse(null);
                var godName = godNode == null
                        ? ""
                        : godNode.getChild("name").flatMap(Stanza::toContentString).orElse("");
                var godEmail = godNode == null
                        ? ""
                        : godNode.getChild("email").flatMap(Stanza::toContentString).orElse("");
                var godLandline = godNode == null
                        ? ""
                        : godNode.getChild("landline_number").flatMap(Stanza::toContentString).orElse("");
                var godMobile = godNode == null
                        ? ""
                        : godNode.getChild("mobile_number").flatMap(Stanza::toContentString).orElse("");
                var god = new IqGetMerchantComplianceResponse.Success.BusinessGrievanceOfficerDetails(
                        godName, godEmail, godLandline, godMobile);
                entries.add(new IqGetMerchantComplianceResponse.Success.MerchantInfo(
                        entityName, entityType, registered, entityTypeCustom, ccd, god));
            }
            if (entries.isEmpty()) {
                return Optional.of(new Success(Collections.emptyList()));
            }
            return Optional.of(new Success(entries));
        }

        /**
         * Compares this variant with another for value equality on the entries.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal success
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
            return Objects.equals(this.entries, that.entries);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(entries);
        }

        /**
         * Returns a diagnostic string naming the entries.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSetMerchantComplianceResponse.Success[entries=" + entries + ']';
        }
    }

    /**
     * Models the {@code ClientError} variant, emitted when the relay rejects the mutation as
     * malformed or carrying disallowed fields.
     *
     * <p>The relay returns this shape when the entity type is unrecognised or a required field is
     * missing for the merchant's market; surface it as a user-facing 4xx-class error on the
     * merchant-compliance edit surface.
     */
    final class ClientError implements IqSetMerchantComplianceResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code, used to dispatch on the relay-side rejection reason.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
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
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal client error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSetMerchantComplianceResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Models the {@code ServerError} variant, emitted when the relay returns a transient
     * internal-failure status while processing the mutation.
     *
     * <p>The relay returns this shape when the compliance backend is temporarily unavailable; use it
     * to drive a backoff-and-retry path on the merchant-compliance edit surface.
     */
    final class ServerError implements IqSetMerchantComplianceResponse {
        /**
         * Holds the numeric error code echoed by the {@code <error/>} child.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text echoed by the {@code <error/>} child.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply from the relay's {@code <error/>} envelope; called from
         * {@link #of(Stanza, Stanza)}.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the numeric error code; a 5xx-class value is the canonical retry trigger.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the human-readable error text, when supplied.
         *
         * <p>The text is server-localised and not stable across snapshots, so it is suitable for
         * logging only.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the stanza.
         *
         * <p>The method delegates to {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)} to
         * extract the (code, text) envelope; called from {@link #of(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
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
         * Compares this variant with another for value equality on the code and text.
         *
         * @param obj the object to compare against; may be {@code null}
         * @return {@code true} when {@code obj} is an equal server error
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
         * Returns a hash code consistent with {@link #equals(Object)}.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a diagnostic string naming the error code and text.
         *
         * @return the string form
         */
        @Override
        public String toString() {
            return "IqSetMerchantComplianceResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
