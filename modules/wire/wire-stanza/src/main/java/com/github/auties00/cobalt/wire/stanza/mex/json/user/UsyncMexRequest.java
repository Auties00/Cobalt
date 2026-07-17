package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX IQ stanza that batches user-directory lookups through the GraphQL usync variant.
 *
 * <p>This query backs contact sync, device-list refresh, and the per-feature about-status,
 * country-code, and username lookups. Each {@code include_*} flag toggles whether the corresponding
 * sub-object is projected on every result row; the flags are independent, so a single request can
 * combine projections that the per-feature callers would otherwise issue separately. The reply is
 * consumed through {@link UsyncMexResponse}.
 *
 * @implNote This implementation forwards {@code input} as a pre-serialised scalar. WA Web's mirror
 * constructs it from {@code {users: [{jid, privacy_token?}], telemetry: {context}}} after filtering
 * each entry against {@code WAWebWidFactory.createWid(jid).isEligibleForUSync()}; Cobalt callers
 * must perform that filtering and serialisation at a higher layer.
 *
 * @see UsyncMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexUsync")
public final class UsyncMexRequest implements MexStanza.Request.Json {
    /**
     * The compiled-document id the relay maps to the persisted query.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsyncQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "29829202653362039";

    /**
     * The GraphQL operation name reported alongside this request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsyncQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "mexUsyncQuery";

    /**
     * The {@code include_about_status} flag; a {@code null} value is emitted as {@code false}.
     */
    private final Boolean includeAboutStatus;

    /**
     * The {@code include_country_code} flag; a {@code null} value is emitted as {@code false}.
     */
    private final Boolean includeCountryCode;

    /**
     * The {@code include_username} flag; a {@code null} value is emitted as {@code false}.
     */
    private final Boolean includeUsername;

    /**
     * The {@code input} GraphQL variable carrying the pre-serialised batch.
     */
    private final String input;

    /**
     * Constructs a usync query request.
     *
     * <p>Each {@code include_*} toggle controls whether the corresponding sub-object is projected on
     * each result row: {@code includeAboutStatus} drives {@link UsyncMexResponse.Item#aboutStatusInfo()},
     * {@code includeCountryCode} drives {@link UsyncMexResponse.Item#countryCode()}, and
     * {@code includeUsername} drives {@link UsyncMexResponse.Item#usernameInfo()}. The toggles are
     * independent and may be combined in a single request to amortise the round-trip cost.
     *
     * @param includeAboutStatus whether to include the about-status sub-object; {@code null} is
     *                           emitted as {@code false}
     * @param includeCountryCode whether to include the country-code field; {@code null} is emitted
     *                           as {@code false}
     * @param includeUsername whether to include the username sub-object; {@code null} is emitted as
     *                        {@code false}
     * @param input the serialised batch input
     */
    public UsyncMexRequest(Boolean includeAboutStatus, Boolean includeCountryCode, Boolean includeUsername, String input) {
        this.includeAboutStatus = includeAboutStatus;
        this.includeCountryCode = includeCountryCode;
        this.includeUsername = includeUsername;
        this.input = input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String id() {
        return QUERY_ID;
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
     * @implNote This implementation always materialises every declared top-level variable, mirroring
     * the relay's compiled query: the three {@code include_*} flags default to {@code false} when
     * {@code null} and {@code input} carries the serialised batch, then defers envelope construction
     * to {@link MexStanza.Request.Json#createMexNode(String, String)}.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexUsync", exports = "mexUsyncQuery",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("include_about_status");
            writer.writeColon();
            writer.writeBool(includeAboutStatus != null && includeAboutStatus);
            writer.writeName("include_country_code");
            writer.writeColon();
            writer.writeBool(includeCountryCode != null && includeCountryCode);
            writer.writeName("include_username");
            writer.writeColon();
            writer.writeBool(includeUsername != null && includeUsername);
            writer.writeName("input");
            writer.writeColon();
            writer.writeString(input);
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return Json.createMexNode(QUERY_ID, output.toString());
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
