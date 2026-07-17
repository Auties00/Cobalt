package com.github.auties00.cobalt.wire.stanza.iq.syncd;

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
import java.util.SequencedCollection;

/**
 * Sealed family of inbound reply variants produced by the relay in response to an
 * {@link IqSyncdServerSyncRequest}.
 *
 * <p>The returned variant discriminates the relay outcome: a {@link Success}
 * carries the typed per-collection projections (state, version, patches and
 * snapshot), a {@link ClientError} carries the global error codes ({@code 400},
 * {@code 404}, {@code 405}, {@code 406}) WA Web treats as non-retryable, and a
 * {@link ServerError} carries the transient codes WA Web retries with exponential
 * backoff plus the relay's optional backoff hint.
 *
 * @implNote
 * This implementation surfaces the per-collection {@code <patch>} and
 * {@code <snapshot>} payloads as raw byte arrays; Cobalt decodes the
 * {@code SyncdPatch} and {@code ExternalBlobReference} protobufs at a higher layer
 * rather than inline in the parser.
 *
 * @deprecated superseded by the untyped sync package server_sync path ({@code LiveWebAppStateService}).
 */
@Deprecated
public sealed interface IqSyncdServerSyncResponse extends IqStanza.Response
        permits IqSyncdServerSyncResponse.Success, IqSyncdServerSyncResponse.ClientError, IqSyncdServerSyncResponse.ServerError {

    /**
     * Parses the inbound stanza into the first matching
     * {@link IqSyncdServerSyncResponse} variant.
     *
     * <p>The priority ordering (success, then client-error, then server-error)
     * matches the wire shape and never returns ambiguous matches; the method is
     * called once per inbound reply.
     *
     * @implNote
     * This implementation calls each variant's {@code of(stanza, request)} in turn
     * and returns the first present result.
     *
     * @param stanza    the inbound IQ stanza received from the relay; never
     *                {@code null}
     * @param request the original outbound stanza; never {@code null}
     * @return an {@link Optional} carrying the parsed variant, or empty when no
     *         documented variant matched
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdServerSync",
            exports = "serverSync", adaptation = WhatsAppAdaptation.ADAPTED)
    static Optional<? extends IqSyncdServerSyncResponse> of(Stanza stanza, Stanza request) {
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
     * Success variant carrying the per-collection projections returned by the relay
     * inside the {@code <sync>} envelope.
     *
     * <p>The {@link #collections()} list drives the per-collection apply pipeline;
     * each entry's {@link IqSyncdServerSyncResponseCollection#state() state} drives
     * the next-iteration decision (apply, reconcile, retry or stop) per
     * {@link IqSyncdServerSyncCollectionState}.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
    @WhatsAppWebModule(moduleName = "WAWebSyncdResponseParser")
    final class Success implements IqSyncdServerSyncResponse {
        /**
         * Holds the typed per-collection projections, one per {@code <collection/>}
         * child of the inbound {@code <sync/>} envelope.
         */
        private final List<IqSyncdServerSyncResponseCollection> collections;

        /**
         * Constructs a successful reply bound to the given per-collection
         * projections.
         *
         * @param collections the projections; never {@code null}, possibly empty
         * @throws NullPointerException if {@code collections} is {@code null}
         */
        public Success(List<IqSyncdServerSyncResponseCollection> collections) {
            Objects.requireNonNull(collections, "collections cannot be null");
            this.collections = collections;
        }

        /**
         * Returns the typed list of per-collection projections.
         *
         * @return an unmodifiable view of the collections; never {@code null},
         *         possibly empty
         */
        public SequencedCollection<IqSyncdServerSyncResponseCollection> collections() {
            return Collections.unmodifiableSequencedCollection(collections);
        }

        /**
         * Parses the inbound stanza into a {@link Success} variant when it matches
         * the success schema.
         *
         * <p>Returns empty when the result-envelope check in
         * {@link SmaxIqResultResponseMixin#validate(Stanza, Stanza)} fails or when the
         * {@code <sync>} child is absent. {@code <collection>} children missing the
         * {@code name} attribute are silently skipped.
         *
         * @implNote
         * This implementation silently drops nameless {@code <collection>} children
         * instead of throwing, deferring fatal-collection-name handling to the
         * caller's apply pipeline rather than the parser.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the
         *         stanza does not match the success schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSyncdResponseParser",
                exports = "syncResponseParser", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<Success> of(Stanza stanza, Stanza request) {
            if (!SmaxIqResultResponseMixin.validate(stanza, request)) {
                return Optional.empty();
            }
            var syncChild = stanza.getChild("sync").orElse(null);
            if (syncChild == null) {
                return Optional.empty();
            }
            var collections = new ArrayList<IqSyncdServerSyncResponseCollection>();
            for (var collectionChild : syncChild.getChildren("collection")) {
                parseCollection(collectionChild).ifPresent(collections::add);
            }
            return Optional.of(new Success(collections));
        }

        /**
         * Projects a single {@code <collection/>} child into a typed
         * {@link IqSyncdServerSyncResponseCollection}.
         *
         * <p>The {@code <collection/>} child lacking a {@code name} attribute yields
         * an empty result and is skipped by {@link #of(Stanza, Stanza)}.
         *
         * @implNote
         * This implementation derives the state from the error branch: code
         * {@code 409} maps to {@link IqSyncdServerSyncCollectionState#CONFLICT} or
         * {@link IqSyncdServerSyncCollectionState#CONFLICT_HAS_MORE} depending on the
         * {@code has_more_patches} attribute, codes {@code 400}, {@code 404} and
         * {@code 405} map to {@link IqSyncdServerSyncCollectionState#ERROR_FATAL},
         * any other code maps to {@link IqSyncdServerSyncCollectionState#ERROR_RETRY},
         * and the absence of an error maps to
         * {@link IqSyncdServerSyncCollectionState#SUCCESS} or
         * {@link IqSyncdServerSyncCollectionState#SUCCESS_HAS_MORE}.
         *
         * @param child the {@code <collection/>} stanza; never {@code null}
         * @return an {@link Optional} carrying the projection, or empty when the
         *         child lacks a {@code name} attribute
         */
        private static Optional<IqSyncdServerSyncResponseCollection> parseCollection(Stanza child) {
            var name = child.getAttributeAsString("name").orElse(null);
            if (name == null) {
                return Optional.empty();
            }
            var hasMorePatches = child.hasAttribute("has_more_patches");
            IqSyncdServerSyncCollectionState state;
            if (child.hasAttribute("type", "error")) {
                var errorChild = child.getChild("error").orElse(null);
                var errorCode = errorChild == null
                        ? null
                        : errorChild.getAttributeAsString("code").orElse(null);
                if ("409".equals(errorCode)) {
                    state = hasMorePatches
                            ? IqSyncdServerSyncCollectionState.CONFLICT_HAS_MORE
                            : IqSyncdServerSyncCollectionState.CONFLICT;
                } else if ("400".equals(errorCode)
                        || "404".equals(errorCode)
                        || "405".equals(errorCode)) {
                    state = IqSyncdServerSyncCollectionState.ERROR_FATAL;
                } else {
                    state = IqSyncdServerSyncCollectionState.ERROR_RETRY;
                }
            } else {
                state = hasMorePatches
                        ? IqSyncdServerSyncCollectionState.SUCCESS_HAS_MORE
                        : IqSyncdServerSyncCollectionState.SUCCESS;
            }
            var version = child.getAttributeAsLong("version").stream().boxed().findFirst().orElse(null);
            var patches = new ArrayList<byte[]>();
            child.getChild("patches").ifPresent(patchesNode ->
                    patchesNode.streamChildren("patch").forEach(patchNode ->
                            patchNode.toContentBytes().ifPresent(patches::add)));
            var snapshot = child.getChild("snapshot")
                    .flatMap(Stanza::toContentBytes)
                    .orElse(null);
            return Optional.of(new IqSyncdServerSyncResponseCollection(
                    name, state, version, patches, snapshot));
        }

        /**
         * Compares this success reply to another for equality across the
         * per-collection projections.
         *
         * @param obj the object to compare against, or {@code null}
         * @return {@code true} when {@code obj} is a {@link Success} with an equal
         *         collection list
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
            return Objects.equals(this.collections, that.collections);
        }

        /**
         * Returns a hash code consistent with {@link #equals(Object)} over the
         * per-collection projections.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(collections);
        }

        /**
         * Returns a debugging representation listing the per-collection projections.
         *
         * @return a string of the form
         *         {@code IqSyncdServerSyncResponse.Success[collections=...]}
         */
        @Override
        public String toString() {
            return "IqSyncdServerSyncResponse.Success[collections=" + collections + ']';
        }
    }

    /**
     * Client-error variant produced when the relay rejects the whole sync with a
     * code WA Web treats as fatal ({@code 400}, {@code 404}, {@code 405},
     * {@code 406}).
     *
     * <p>The sync iteration is not retryable; the caller surfaces the error rather
     * than rescheduling.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
    final class ClientError implements IqSyncdServerSyncResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text, or {@code null} when the
         * relay omitted it.
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
         * @return an {@link Optional} carrying the error text, or empty when the
         *         relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ClientError} variant when it
         * matches the standard SMAX client-error envelope.
         *
         * <p>Returns empty when the envelope check fails, delegating the envelope
         * and code-range validation entirely to
         * {@link SmaxBaseServerErrorMixin#parseClientError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the
         *         stanza does not match the client-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSyncdServerSync",
                exports = "serverSync", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ClientError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseClientError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ClientError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this client-error reply to another for equality across the error
         * code and text.
         *
         * @param obj the object to compare against, or {@code null}
         * @return {@code true} when {@code obj} is a {@link ClientError} with an
         *         equal code and text
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
         * Returns a hash code consistent with {@link #equals(Object)} over the error
         * code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debugging representation of the error code and text.
         *
         * @return a string of the form
         *         {@code IqSyncdServerSyncResponse.ClientError[errorCode=..., errorText=...]}
         */
        @Override
        public String toString() {
            return "IqSyncdServerSyncResponse.ClientError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }

    /**
     * Server-error variant produced when the relay encounters a transient internal
     * failure (typically a code outside the fatal set) processing the sync.
     *
     * <p>The sync iteration is rescheduled through the standard exponential-backoff
     * loop using the relay's optional backoff hint.
     */
    @WhatsAppWebModule(moduleName = "WAWebSyncdServerSync")
    final class ServerError implements IqSyncdServerSyncResponse {
        /**
         * Holds the numeric server-side error code.
         */
        private final int errorCode;

        /**
         * Holds the optional human-readable error text, or {@code null} when the
         * relay omitted it.
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
         * @return an {@link Optional} carrying the error text, or empty when the
         *         relay omitted it
         */
        public Optional<String> errorText() {
            return Optional.ofNullable(errorText);
        }

        /**
         * Parses the inbound stanza into a {@link ServerError} variant when it
         * matches the standard SMAX server-error envelope.
         *
         * <p>Returns empty when the envelope check fails, delegating the envelope
         * and code-range validation entirely to
         * {@link SmaxBaseServerErrorMixin#parseServerError(Stanza, Stanza)}.
         *
         * @param stanza    the inbound IQ stanza
         * @param request the original outbound request
         * @return an {@link Optional} carrying the parsed variant, or empty when the
         *         stanza does not match the server-error schema
         */
        @WhatsAppWebExport(moduleName = "WAWebSyncdServerSync",
                exports = "serverSync", adaptation = WhatsAppAdaptation.ADAPTED)
        public static Optional<ServerError> of(Stanza stanza, Stanza request) {
            var envelope = SmaxBaseServerErrorMixin.parseServerError(stanza, request).orElse(null);
            if (envelope == null) {
                return Optional.empty();
            }
            return Optional.of(new ServerError(envelope.code(), envelope.text()));
        }

        /**
         * Compares this server-error reply to another for equality across the error
         * code and text.
         *
         * @param obj the object to compare against, or {@code null}
         * @return {@code true} when {@code obj} is a {@link ServerError} with an
         *         equal code and text
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
         * Returns a hash code consistent with {@link #equals(Object)} over the error
         * code and text.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(errorCode, errorText);
        }

        /**
         * Returns a debugging representation of the error code and text.
         *
         * @return a string of the form
         *         {@code IqSyncdServerSyncResponse.ServerError[errorCode=..., errorText=...]}
         */
        @Override
        public String toString() {
            return "IqSyncdServerSyncResponse.ServerError[errorCode=" + errorCode
                    + ", errorText=" + errorText + ']';
        }
    }
}
