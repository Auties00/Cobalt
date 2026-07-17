package com.github.auties00.cobalt.wire.graphql.whatsappWeb.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.whatsappWeb.WhatsAppWebGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Builds the relay query that fetches the GenAI persona profiles of one or more bots.
 *
 * <p>The query takes one {@code ids} GraphQL variable, mapped to the server-side {@code persona_ids}
 * argument of {@code xfb_fetch_genai_personas}. WhatsApp Web's
 * {@code WAWebFetchBotProfilesGQL.fetchBotProfilesGQL(ids)} forwards the list straight to the relay as
 * {@code {ids: [...]}}. The caller {@code WAWebRequestBotProfiles} derives each id as a bot's
 * {@code fbid} (the persona id substring before {@code "$"}, or the fbid-bot user part), so the ids
 * are plain Facebook persona identifiers rather than WhatsApp addresses and are modelled as
 * {@code String}. The relay returns the matched personas under {@code xfb_fetch_genai_personas}; the
 * reply is consumed through {@link FetchBotProfilesGqlWhatsAppWebGraphQlResponse}.
 *
 * @see FetchBotProfilesGqlWhatsAppWebGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebFetchBotProfilesGQLQuery")
public final class FetchBotProfilesGqlWhatsAppWebGraphQlRequest implements WhatsAppWebGraphQlOperation.Request {
    /**
     * The persisted document identifier the relay maps to the server-side compiled GraphQL document
     * for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the url-encoded request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchBotProfilesGQLQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "26368585139502858";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchBotProfilesGQLQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebFetchBotProfilesGQLQuery";

    /**
     * The {@code ids} GraphQL variable listing the bot persona ids to fetch, or {@code null} to omit
     * it.
     */
    private final List<String> ids;

    /**
     * Constructs a fetch-bot-profiles query request carrying the persona ids to fetch.
     *
     * <p>A {@code null} value omits the {@code ids} variable from the serialized object; an empty list
     * serializes as an empty array.
     *
     * @param ids the bot persona ids to fetch, or {@code null} to omit the variable
     */
    public FetchBotProfilesGqlWhatsAppWebGraphQlRequest(List<String> ids) {
        this.ids = ids;
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
     * @implNote This implementation emits {@code {"ids": [...]}}, writing the {@code ids} array only
     * when the list is non-null and emitting {@code "{}"} when it is {@code null}.
     */
    @WhatsAppWebExport(moduleName = "WAWebFetchBotProfilesGQL", exports = "fetchBotProfilesGQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            if (ids != null) {
                writer.writeName("ids");
                writer.writeColon();
                writer.startArray();
                for (var i = 0; i < ids.size(); i++) {
                    if (i > 0) {
                        writer.writeComma();
                    }
                    writer.writeString(ids.get(i));
                }
                writer.endArray();
            }
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
