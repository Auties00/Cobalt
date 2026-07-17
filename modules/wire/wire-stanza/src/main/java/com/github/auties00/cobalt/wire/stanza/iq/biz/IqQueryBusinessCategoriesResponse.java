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
 * Models the sealed family of inbound reply variants produced by the relay in response to an {@link IqQueryBusinessCategoriesRequest}.
 *
 * <p>The three documented outcomes of a business-category lookup are {@link Success}, which carries
 * the typed category list and the {@code not_a_biz} sentinel id, {@link ClientError}, which
 * surfaces a relay validation rejection, and {@link ServerError}, which reports a transport or
 * backend failure. The dispatcher invokes {@link #of(Stanza, Stanza)} to project the raw {@link Stanza}
 * into the right variant.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryBusinessCategoriesJob")
public sealed interface IqQueryBusinessCategoriesResponse extends IqStanza.Response
        permits IqQueryBusinessCategoriesResponse.Success, IqQueryBusinessCategoriesResponse.ClientError, IqQueryBusinessCategoriesResponse.ServerError {

    /**
     * Tries each {@link IqQueryBusinessCategoriesResponse} variant in priority order.
     *
     * <p>The success path is tried first, then the client-error envelope, then the server-error
     * envelope. The result is empty only when none of the three documented shapes apply.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    static Optional<? extends IqQueryBusinessCategoriesResponse> of(Stanza stanza, Stanza request) {
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
     * Carries the typed category list and the {@code not_a_biz} sentinel id for a successful lookup.
     *
     * <p>The typed {@link Category} entries drive the rows of the category picker, and the
     * {@code not_a_biz} sentinel id marks the synthetic opt-out row that lets the merchant signal
     * "not a business".
     */
    final class Success implements IqQueryBusinessCategoriesResponse {
        /**
         * Models one category entry decoded from a {@code <category/>} child of the relay reply.
         *
         * <p>The {@link #notABiz()} flag marks the synthetic opt-out row.
         */
        public static final class Category {
            /**
             * Holds the opaque category identifier carried by the {@code id} attribute of the {@code <category/>} child.
             */
            private final String id;

            /**
             * Holds the localised display name shown in the picker UI, lifted from the {@code <category/>} child content.
             */
            private final String localizedDisplayName;

            /**
             * Holds the {@code not_a_biz} sentinel flag.
             *
             * <p>{@code true} only when the entry's id matches the id carried by the
             * {@code <not_a_biz/>} sentinel block.
             */
            private final boolean notABiz;

            /**
             * Constructs a typed category entry from a decoded {@code <category/>} child.
             *
             * <p>The {@code notABiz} flag is {@code true} only when the entry's id matches the
             * {@code <not_a_biz/>} sentinel.
             *
             * @param id                   the category identifier; never {@code null}
             * @param localizedDisplayName the display name; never {@code null}
             * @param notABiz              the sentinel flag
             * @throws NullPointerException if either string is {@code null}
             */
            public Category(String id, String localizedDisplayName, boolean notABiz) {
                this.id = Objects.requireNonNull(id, "id cannot be null");
                this.localizedDisplayName = Objects.requireNonNull(
                        localizedDisplayName, "localizedDisplayName cannot be null");
                this.notABiz = notABiz;
            }

            /**
             * Returns the opaque category identifier.
             *
             * <p>This is the value the SMB profile editor sends back through
             * {@link IqEditBusinessProfileRequest} to mark the merchant's category.
             *
             * @return the identifier; never {@code null}
             */
            public String id() {
                return id;
            }

            /**
             * Returns the localised display name that drives the per-row label in the category picker.
             *
             * @return the display name; never {@code null}
             */
            public String localizedDisplayName() {
                return localizedDisplayName;
            }

            /**
             * Returns the {@code not_a_biz} sentinel flag.
             *
             * <p>Consumers render the synthetic opt-out row differently from the catalogue rows
             * because it does not name a real business category.
             *
             * @return {@code true} when this entry is the synthetic opt-out
             */
            public boolean notABiz() {
                return notABiz;
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                var that = (Category) obj;
                return this.notABiz == that.notABiz
                        && Objects.equals(this.id, that.id)
                        && Objects.equals(this.localizedDisplayName, that.localizedDisplayName);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id, localizedDisplayName, notABiz);
            }

            @Override
            public String toString() {
                return "IqQueryBusinessCategoriesResponse.Success.Category[id=" + id
                        + ", localizedDisplayName=" + localizedDisplayName
                        + ", notABiz=" + notABiz + ']';
            }
        }

        /**
         * Holds the typed category entries in wire order.
         */
        private final List<Category> categories;

        /**
         * Holds the id of the synthetic {@code not_a_biz} sentinel, or the empty string when the relay returned no sentinel block.
         */
        private final String notABizId;

        /**
         * Constructs a typed success reply from the decoded category list and sentinel id.
         *
         * <p>The empty string marks an absent {@code not_a_biz} sentinel block.
         *
         * @param categories the category list; never {@code null}
         * @param notABizId  the sentinel id; never {@code null}, empty string when absent
         * @throws NullPointerException if either argument is {@code null}
         */
        public Success(List<Category> categories, String notABizId) {
            Objects.requireNonNull(categories, "categories cannot be null");
            this.categories = List.copyOf(categories);
            this.notABizId = Objects.requireNonNull(notABizId, "notABizId cannot be null");
        }

        /**
         * Returns the typed category entries.
         *
         * <p>The list preserves the wire order, which mirrors the merchant-facing display order.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<Category> categories() {
            return categories;
        }

        /**
         * Returns the {@code not_a_biz} sentinel id used when filtering the category list.
         *
         * <p>The value is the empty string when the relay did not stamp a sentinel block.
         *
         * @return the sentinel id; empty string when absent
         */
        public String notABizId() {
            return notABizId;
        }

        /**
         * Tries to parse a {@link Success} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a {@code result} envelope matching
         * the original request.
         *
         * @implNote
         * This implementation mirrors the deprecated WAP parser inside {@code WAWebQueryBusinessCategoriesJob.businessCategoriesResponse}:
         * the {@code <response/>} envelope carries an optional {@code <not_a_biz/>} sentinel and a
         * mandatory {@code <categories/>} block. Each {@code <category id/>} child contributes a
         * {@link Category} whose {@code notABiz} flag is set when the entry id matches the sentinel
         * id, replicating the WA Web parser's per-row classification. An empty {@code <response/>}
         * produces an empty list and an empty sentinel id.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebQueryBusinessCategoriesJob",
                exports = "queryBusinessCategories", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var responseNode = stanza.getChild("response").orElse(null);
            if (responseNode == null) {
                return Optional.of(new Success(Collections.emptyList(), ""));
            }
            var notABizId = responseNode.getChild("not_a_biz")
                    .stream()
                    .flatMap(child -> child.streamChildren("category"))
                    .map(category -> category.getAttributeAsString("id").orElse(""))
                    .reduce("", (acc, id) -> id);
            var categoriesNode = responseNode.getChild("categories").orElse(null);
            var categories = new ArrayList<Category>();
            if (categoriesNode != null) {
                for (var categoryNode : categoriesNode.getChildren("category")) {
                    var id = categoryNode.getAttributeAsString("id").orElse(null);
                    if (id == null) {
                        continue;
                    }
                    var displayName = categoryNode.toContentString().orElse("");
                    var isNotABiz = !notABizId.isEmpty() && id.equals(notABizId);
                    categories.add(new Category(id, displayName, isNotABiz));
                }
            }
            return Optional.of(new Success(categories, notABizId));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (Success) obj;
            return Objects.equals(this.categories, that.categories)
                    && Objects.equals(this.notABizId, that.notABizId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(categories, notABizId);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessCategoriesResponse.Success[categories=" + categories
                    + ", notABizId=" + notABizId + ']';
        }
    }

    /**
     * Surfaces a client-side rejection of a category lookup.
     *
     * <p>Typical examples include a relay validation rejection surfaced as a SMAX error envelope.
     */
    final class ClientError implements IqQueryBusinessCategoriesResponse {
        /**
         * Holds the numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed client-error reply from a decoded client-error envelope.
         *
         * <p>A {@code null} {@code errorText} marks a wire shape that omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ClientError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation, when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a client-error envelope matching
         * the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the client-error schema
         */
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessCategoriesResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Surfaces a server-side failure that did not produce a typed category list.
     *
     * <p>WA Web's {@code WAWebQueryBusinessCategoriesJob.queryBusinessCategories} surfaces this as a
     * {@code ServerStatusCodeError} carrying the relay-supplied status.
     */
    final class ServerError implements IqQueryBusinessCategoriesResponse {
        /**
         * Holds the numeric error code lifted from the SMAX error envelope.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text lifted from the SMAX error envelope.
         */
        private final String errorText;

        /**
         * Constructs a typed server-error reply from a decoded server-error envelope.
         *
         * <p>A {@code null} {@code errorText} marks a wire shape that omitted the text field.
         *
         * @param errorCode the numeric error code
         * @param errorText the human-readable error text; may be {@code null}
         */
        public ServerError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the SMAX error code the relay used to classify the failure.
         *
         * @return the error code
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the relay-supplied error explanation, when present.
         *
         * @return an {@link Optional} carrying the error text
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the inbound stanza.
         *
         * <p>The result is empty when the stanza does not carry a server-error envelope matching
         * the original request.
         *
         * @param stanza    the inbound IQ stanza; never {@code null}
         * @param request the original outbound request; never {@code null}
         * @return an {@link Optional} carrying the parsed variant, or empty when the stanza does not match the server-error schema
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        @Override
        public String toString() {
            return "IqQueryBusinessCategoriesResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
