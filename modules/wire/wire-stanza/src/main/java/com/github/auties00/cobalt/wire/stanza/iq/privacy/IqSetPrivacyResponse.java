package com.github.auties00.cobalt.wire.stanza.iq.privacy;

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
 * Sealed family of inbound reply variants produced by the relay for an {@link IqSetPrivacyRequest}.
 * <p>
 * Pattern matching against the three permitted subtypes ({@link Success}, {@link ClientError},
 * {@link ServerError}) surfaces the per-category outcome list or the error envelope. A
 * {@link Success} envelope can still carry per-category rejections via
 * {@link CategoryOutcome#errorCode()}; the envelope being a success only means the relay accepted
 * the {@code <iq>} for processing.
 *
 * @implNote
 * This implementation collapses WA Web's split parse-and-throw flow ({@code setPrivacyParser} plus
 * the {@code ServerStatusCodeError} thrown by {@code WAWebSetPrivacyJob.setPrivacy} on a {@code 4xx}
 * or {@code 5xx} reply) into a single sealed hierarchy; an error reply is a typed value here rather
 * than an exception.
 */
public sealed interface IqSetPrivacyResponse extends IqStanza.Response
        permits IqSetPrivacyResponse.Success, IqSetPrivacyResponse.ClientError, IqSetPrivacyResponse.ServerError {

    /**
     * Tries each {@link IqSetPrivacyResponse} variant in priority order and returns the first that
     * parses cleanly.
     * <p>
     * The dispatcher calls this immediately after receiving an inbound {@code <iq>} stanza whose id
     * matches an outstanding {@link IqSetPrivacyRequest}; the empty {@link Optional} indicates the
     * stanza did not match any documented schema.
     *
     * @implNote
     * This implementation tries {@link Success} first, then {@link ClientError}, then
     * {@link ServerError}.
     *
     * @param stanza    the inbound IQ stanza; never {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return the parsed variant, or {@link Optional#empty()} when no documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSetPrivacyJob",
            exports = "setPrivacy", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSetPrivacyResponse> of(Stanza stanza, Stanza request) {
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
     * One per-category outcome inside a {@link Success} response.
     * <p>
     * Carries the new value, the optional echoed digest (for blacklist mutations), and any
     * per-category error code; the relay returns one outcome per {@code <category>} child that was
     * sent in the request. A non-empty {@link #errorText()} together with a non-{@code -1}
     * {@link #errorCode()} indicates the relay rejected this specific category with the
     * {@code value="error"} sentinel even though the envelope itself succeeded.
     *
     * @implNote
     * This implementation surfaces the per-category rejection as typed fields rather than as a
     * {@code ServerStatusCodeError} exception (WA Web's {@code setPrivacyParser} wraps the same
     * payload in an error instance before returning).
     */
    final class CategoryOutcome {
        /**
         * The category name reported by the relay.
         */
        private final IqQueryPrivacySettingsCategoryName name;

        /**
         * The new value reported by the relay, or {@code null} when the relay rejected the
         * per-category mutation with {@code value="error"}.
         */
        private final IqQueryPrivacySettingsVisibility value;

        /**
         * The optional digest echoed back for categories with user-list mutations.
         */
        private final String dhash;

        /**
         * The numeric error code carried inside the embedded {@code <error>} child when the relay
         * rejected this category, or {@code -1} when the category succeeded.
         */
        private final int errorCode;

        /**
         * The optional error text carried inside the embedded {@code <error>} child.
         */
        private final String errorText;

        /**
         * Constructs a category outcome.
         * <p>
         * Instances are normally obtained via {@link Success#of(Stanza, Stanza)}; this constructor is
         * also reachable for tests and synthetic fixtures.
         *
         * @param name      the category; never {@code null}
         * @param value     the new value, or {@code null} when the relay rejected the mutation
         * @param dhash     the optional digest; may be {@code null}
         * @param errorCode the embedded error code, or {@code -1} on success
         * @param errorText the optional error text; may be {@code null}
         * @throws NullPointerException if {@code name} is {@code null}
         */
        public CategoryOutcome(IqQueryPrivacySettingsCategoryName name,
                               IqQueryPrivacySettingsVisibility value,
                               String dhash,
                               int errorCode,
                               String errorText) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            this.value = value;
            this.dhash = dhash;
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        /**
         * Returns the category.
         *
         * @return the category; never {@code null}
         */
        public IqQueryPrivacySettingsCategoryName name() {
            return name;
        }

        /**
         * Returns the new value, when the per-category mutation succeeded.
         * <p>
         * The {@link Optional#empty()} return signals the relay rejected this specific mutation with
         * the {@code value="error"} sentinel even though the envelope itself succeeded;
         * {@link #errorCode()} and {@link #errorText()} carry the rejection reason.
         *
         * @return the value, or {@link Optional#empty()} when rejected
         */
        public Optional<IqQueryPrivacySettingsVisibility> value() {
            return Optional.ofNullable(value);
        }

        /**
         * Returns the optional list digest echoed by the relay.
         * <p>
         * Present only for categories whose request carried a non-empty
         * {@link IqSetPrivacyRequest#users()} list; consumers persist it as the new baseline for the
         * next mutation on the same category.
         *
         * @return the digest, or {@link Optional#empty()} when omitted
         */
        public Optional<String> dhash() {
            return Optional.ofNullable(dhash);
        }

        /**
         * Returns the embedded error code, when the relay rejected this category.
         *
         * @return the error code, or {@code -1} on success
         */
        public int errorCode() {
            return errorCode;
        }

        /**
         * Returns the optional embedded error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares every field by value.
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            var that = (CategoryOutcome) obj;
            return this.name == that.name
                    && this.value == that.value
                    && Objects.equals(this.dhash, that.dhash)
                    && this.errorCode == that.errorCode
                    && Objects.equals(this.errorText, that.errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes every field consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(name, value, dhash, errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation of every field; the format is not
         * stable and must not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyResponse.CategoryOutcome[name=" + name
                    + ", value=" + value + ", dhash=" + dhash
                    + ", errorCode=" + errorCode + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code Success} reply variant; the relay accepted the envelope and returned a per-category
     * outcome list.
     * <p>
     * Iterating {@link #categories()} applies each outcome locally; even on a successful envelope
     * individual categories may still carry a non-{@code -1} {@link CategoryOutcome#errorCode()}
     * signalling per-category rejection.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
    final class Success implements IqSetPrivacyResponse {
        /**
         * The parsed per-category outcomes in stanza order.
         */
        private final List<CategoryOutcome> categories;

        /**
         * Constructs a {@code Success} reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         *
         * @implNote
         * This implementation defensively copies {@code categories} via
         * {@link List#copyOf(java.util.Collection)}; the resulting instance is immutable.
         *
         * @param categories the outcomes; never {@code null}
         * @throws NullPointerException if {@code categories} is {@code null}
         */
        public Success(List<CategoryOutcome> categories) {
            Objects.requireNonNull(categories, "categories cannot be null");
            this.categories = List.copyOf(categories);
        }

        /**
         * Returns the parsed per-category outcomes.
         *
         * @return an unmodifiable list; never {@code null}
         */
        public List<CategoryOutcome> categories() {
            return categories;
        }

        /**
         * Tries to parse a {@link Success} variant from the given inbound stanza.
         * <p>
         * The {@link Optional#empty()} return signals the stanza is not a success envelope; callers
         * fall through to {@link ClientError#of(Stanza, Stanza)} and {@link ServerError#of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation iterates every {@code <category>} child and folds the
         * {@code value="error"} sentinel back into the {@link CategoryOutcome} typed fields: a
         * literal {@code "error"} value triggers a lookup of the embedded {@code <error>} child's
         * {@code code} and {@code text} attributes (matching WA Web's {@code ServerStatusCodeError}
         * construction in {@code setPrivacyParser}), every other value is projected through
         * {@link IqQueryPrivacySettingsVisibility#fromWire(String)}. Entries whose {@code name}
         * cannot be resolved by {@link IqQueryPrivacySettingsCategoryName#fromWire(String)} are
         * silently dropped, as are entries with a missing {@code value} attribute or an unrecognised
         * non-{@code error} value.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the stanza does not match the
         *         success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSetPrivacyJob",
                exports = "setPrivacyParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var privacy = stanza.getChild("privacy").orElse(null);
            if (privacy == null) {
                return Optional.empty();
            }
            var outcomes = new ArrayList<CategoryOutcome>();
            for (var category : privacy.getChildren("category")) {
                var name = category.getAttributeAsString("name")
                        .flatMap(IqQueryPrivacySettingsCategoryName::fromWire)
                        .orElse(null);
                if (name == null) {
                    continue;
                }
                var rawValue = category.getAttributeAsString("value").orElse(null);
                if (rawValue == null) {
                    continue;
                }
                if ("error".equals(rawValue)) {
                    var errorChild = category.getChild("error").orElse(null);
                    if (errorChild == null) {
                        continue;
                    }
                    var code = errorChild.getAttributeAsInt("code").orElse(-1);
                    var text = errorChild.getAttributeAsString("text").orElse(null);
                    outcomes.add(new CategoryOutcome(name, null, null, code, text));
                    continue;
                }
                var value = IqQueryPrivacySettingsVisibility.fromWire(rawValue).orElse(null);
                if (value == null) {
                    continue;
                }
                var dhash = category.getAttributeAsString("dhash").orElse(null);
                outcomes.add(new CategoryOutcome(name, value, dhash, -1, null));
            }
            return Optional.of(new Success(outcomes));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares the categories list by value.
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
            return Objects.equals(this.categories, that.categories);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes the categories list consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(categories);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation; the format is not stable and must
         * not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyResponse.Success[categories=" + categories + ']';
        }
    }

    /**
     * The {@code ClientError} reply variant; the relay rejected the envelope with a {@code 4xx}
     * error code.
     * <p>
     * The most actionable {@code 4xx} for this surface is {@code 409 Conflict}, returned when the
     * supplied {@link IqSetPrivacyRequest#dhash()} no longer matches the relay's view of the
     * per-category exclusion list; WA Web's {@code WAWebSetPrivacyForOneCategoryAction} reacts by
     * re-syncing that disallowed-list before propagating the error.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
    final class ClientError implements IqSetPrivacyResponse {
        /**
         * The numeric server-side error code (typically {@code 4xx}).
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed back by the relay.
         */
        private final String errorText;

        /**
         * Constructs a client-error reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ClientError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
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
         * @implNote
         * This implementation compares both error code and error text by value.
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
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes both fields consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation; the format is not stable and must
         * not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * The {@code ServerError} reply variant; the relay encountered a transient internal failure (a
     * {@code 5xx} error code).
     * <p>
     * Distinguished from {@link ClientError} so callers can choose a different retry policy;
     * transient failures normally warrant a backoff-and-retry, client errors normally do not.
     */
    @WhatsAppWebModule(moduleName = "WAWebSetPrivacyJob")
    final class ServerError implements IqSetPrivacyResponse {
        /**
         * The numeric server-side error code (typically {@code 5xx}).
         */
        private final int errorCode;

        /**
         * The optional human-readable error text echoed back by the relay.
         */
        private final String errorText;

        /**
         * Constructs a server-error reply.
         * <p>
         * Instances are normally obtained via {@link #of(Stanza, Stanza)}; this constructor is also
         * reachable for tests and synthetic fixtures.
         *
         * @param errorCode the numeric error code
         * @param errorText the optional human-readable text; may be {@code null}
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
         * Returns the optional error text.
         *
         * @return the text, or {@link Optional#empty()} when omitted
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Tries to parse a {@link ServerError} variant from the given inbound stanza.
         *
         * @implNote
         * This implementation delegates the envelope match to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return the parsed variant, or {@link Optional#empty()} when the schema does not match
         */
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation compares both error code and error text by value.
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
         * {@inheritDoc}
         *
         * @implNote
         * This implementation hashes both fields consistently with {@link #equals(Object)}.
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * {@inheritDoc}
         *
         * @implNote
         * This implementation emits a debug-only representation; the format is not stable and must
         * not be parsed.
         */
        @Override
        public String toString() {
            return "IqSetPrivacyResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
