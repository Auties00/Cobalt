package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlEnvironment;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Builds the graph.whatsapp.com GraphQL mutation that runs the Waffle cross-posting eligibility (XE) check for a set of
 * status unique ids against the linked Facebook and Instagram destinations.
 *
 * <p>The single {@code input} GraphQL variable is the {@code WaffleXEInput} object, mapped onto the
 * server-side {@code input_params} argument. WhatsApp Web's
 * {@code WAWebCrosspostingAPI.makeEligibilityRequest} fills it with the request expiration instants
 * ({@code exp_time}), the caller's ephemeral cross-posting public key ({@code purpose_client_pub_key}),
 * the unique-id count and the unique-id list themselves ({@code waffle_unique_id_count},
 * {@code waffle_unique_ids}), the targeted destinations ({@code waffle_xas}), and the cross-posting
 * session id ({@code session_id}). The graph.whatsapp.com endpoint returns the per-purpose public keys, the echoed unique
 * ids, and the per-destination eligibility data under {@code waffle_xe_root}; the reply is consumed
 * through {@link WaffleXeWhatsAppGraphQlResponse}.
 *
 * @see WaffleXeWhatsAppGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebWaffleXEQuery")
public final class WaffleXeWhatsAppGraphQlRequest implements WhatsAppGraphQlOperation.Request {
    /**
     * The persisted document identifier the graph.whatsapp.com endpoint maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleXEQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "32172601809054525";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebWaffleXEQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebWaffleXEQuery";

    /**
     * The {@code exp_time} field of the {@code input} object listing the request expiration instants,
     * each serialized as an epoch-second integer.
     */
    private final List<Instant> expirationTimes;

    /**
     * The {@code purpose_client_pub_key} field of the {@code input} object carrying the caller's
     * base64-encoded ephemeral cross-posting public key, or {@code null} to omit it.
     */
    private final String purposeClientPublicKey;

    /**
     * The {@code waffle_unique_ids} field of the {@code input} object listing the status unique ids to
     * check.
     *
     * <p>The companion {@code waffle_unique_id_count} field is serialized as the size of this list.
     */
    private final List<String> uniqueIds;

    /**
     * The {@code waffle_xas} field of the {@code input} object listing the targeted cross-posting
     * destinations.
     */
    private final List<WaffleXas> destinations;

    /**
     * The {@code session_id} field of the {@code input} object naming the cross-posting session, or
     * {@code null} to omit it.
     */
    private final String sessionId;

    /**
     * Constructs a Waffle cross-posting eligibility-check request.
     *
     * <p>All values populate the {@code input} GraphQL object. A {@code null} list is treated as the
     * empty list and a {@code null} {@code purposeClientPublicKey} or {@code sessionId} omits its
     * field. The {@code waffle_unique_id_count} field is derived as {@code uniqueIds.size()}.
     *
     * @param expirationTimes        the request expiration instants for {@code exp_time}, or
     *                               {@code null} for none
     * @param purposeClientPublicKey the base64-encoded ephemeral public key, or {@code null} to omit
     *                               the field
     * @param uniqueIds              the status unique ids for {@code waffle_unique_ids}, or
     *                               {@code null} for none
     * @param destinations           the targeted destinations for {@code waffle_xas}, or {@code null}
     *                               for none
     * @param sessionId              the cross-posting session id, or {@code null} to omit the field
     */
    public WaffleXeWhatsAppGraphQlRequest(List<Instant> expirationTimes, String purposeClientPublicKey, List<String> uniqueIds, List<WaffleXas> destinations, String sessionId) {
        this.expirationTimes = expirationTimes == null ? List.of() : List.copyOf(expirationTimes);
        this.purposeClientPublicKey = purposeClientPublicKey;
        this.uniqueIds = uniqueIds == null ? List.of() : List.copyOf(uniqueIds);
        this.destinations = destinations == null ? List.of() : List.copyOf(destinations);
        this.sessionId = sessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {"exp_time": [...], "purpose_client_pub_key":
     * <key>, "waffle_unique_id_count": <n>, "waffle_unique_ids": [...], "waffle_xas": [...],
     * "session_id": <id>}}}, rendering each {@code exp_time} entry as its epoch-second integer, writing
     * {@code waffle_unique_id_count} as the unique-id list size, and omitting the scalar key and
     * session fields when {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebCrosspostingAPI", exports = "makeEligibilityRequest",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();

            writer.writeName("exp_time");
            writer.writeColon();
            writer.startArray();
            for (var i = 0; i < expirationTimes.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                writer.writeInt64(expirationTimes.get(i).getEpochSecond());
            }
            writer.endArray();

            if (purposeClientPublicKey != null) {
                writer.writeName("purpose_client_pub_key");
                writer.writeColon();
                writer.writeString(purposeClientPublicKey);
            }

            writer.writeName("waffle_unique_id_count");
            writer.writeColon();
            writer.writeInt32(uniqueIds.size());

            writer.writeName("waffle_unique_ids");
            writer.writeColon();
            writer.startArray();
            for (var i = 0; i < uniqueIds.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                writer.writeString(uniqueIds.get(i));
            }
            writer.endArray();

            writer.writeName("waffle_xas");
            writer.writeColon();
            writer.startArray();
            for (var i = 0; i < destinations.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                destinations.get(i).writeTo(writer);
            }
            writer.endArray();

            if (sessionId != null) {
                writer.writeName("session_id");
                writer.writeColon();
                writer.writeString(sessionId);
            }

            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /**
     * Models one {@code waffle_xas} entry of the {@code input} object: a single targeted cross-posting
     * destination.
     *
     * <p>Carries the destination application name ({@code waffle_xan}) and the cross-posting surface
     * marker ({@code waffle_xs}). WhatsApp Web builds each entry as
     * {@code {waffle_xan: <"F"|"I">, waffle_xs: "S"}} from the targeted destination list.
     */
    public static final class WaffleXas {
        /**
         * Holds the destination application name.
         */
        private final WaffleXan applicationName;

        /**
         * Holds the cross-posting surface marker emitted under {@code waffle_xs}.
         *
         * <p>Kept as a raw string: WhatsApp Web emits the single literal {@code "S"} here, but the
         * field's full server value set is not declared in the snapshot {@code 1040120866} bundle.
         */
        private final String surface;

        /**
         * Constructs a destination entry.
         *
         * @param applicationName the destination application name, or {@code null} to omit the
         *                        {@code waffle_xan} field
         * @param surface         the cross-posting surface marker, or {@code null} to omit the
         *                        {@code waffle_xs} field
         */
        public WaffleXas(WaffleXan applicationName, String surface) {
            this.applicationName = applicationName;
            this.surface = surface;
        }

        /**
         * Returns the destination application name.
         *
         * @return the application name, or empty when none was set
         */
        public Optional<WaffleXan> applicationName() {
            return Optional.ofNullable(applicationName);
        }

        /**
         * Returns the cross-posting surface marker.
         *
         * @return the surface marker, or empty when none was set
         */
        public Optional<String> surface() {
            return Optional.ofNullable(surface);
        }

        /**
         * Serializes this destination entry as a JSON object onto the given writer.
         *
         * <p>Emits {@code {"waffle_xan": <name>, "waffle_xs": <surface>}}, writing each field only when
         * its value is non-null.
         *
         * @param writer the JSON writer to emit onto
         */
        void writeTo(JSONWriter writer) {
            writer.startObject();
            if (applicationName != null) {
                writer.writeName("waffle_xan");
                writer.writeColon();
                writer.writeString(applicationName.value());
            }

            if (surface != null) {
                writer.writeName("waffle_xs");
                writer.writeColon();
                writer.writeString(surface);
            }
            writer.endObject();
        }
    }

    /**
     * Enumerates the cross-posting destination application names accepted by {@code waffle_xan}.
     *
     * <p>The value set mirrors WhatsApp Web's {@code WAWebCrossposting.flow.CrosspostingDestinationGQLValue}
     * enum, which maps {@code FACEBOOK} to the wire value {@code "F"} and {@code INSTAGRAM} to
     * {@code "I"}.
     */
    public enum WaffleXan {
        /**
         * The Facebook cross-posting destination, wire value {@code "F"}.
         */
        FACEBOOK("F"),

        /**
         * The Instagram cross-posting destination, wire value {@code "I"}.
         */
        INSTAGRAM("I");

        /**
         * Holds the wire value emitted for this destination.
         */
        private final String value;

        /**
         * Constructs a destination constant bound to its wire value.
         *
         * @param value the wire value emitted under {@code waffle_xan}
         */
        WaffleXan(String value) {
            this.value = value;
        }

        /**
         * Returns the wire value emitted for this destination.
         *
         * @return the wire value, never {@code null}
         */
        public String value() {
            return value;
        }

        /**
         * Resolves a {@link WaffleXan} from its wire value.
         *
         * @param value the wire value to resolve, may be {@code null}
         * @return the matching constant, or empty when {@code value} is {@code null} or unrecognized
         */
        public static Optional<WaffleXan> of(String value) {
            if (value == null) {
                return Optional.empty();
            }
            for (var candidate : values()) {
                if (candidate.value.equals(value)) {
                    return Optional.of(candidate);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WhatsAppGraphQlEnvironment environment() {
        return WhatsAppGraphQlEnvironment.CATALOG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> accessToken() {
        return Optional.empty();
    }
}
