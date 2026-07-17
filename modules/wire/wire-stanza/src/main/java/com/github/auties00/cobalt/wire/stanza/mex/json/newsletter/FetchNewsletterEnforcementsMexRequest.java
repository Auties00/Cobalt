package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that fetches the moderation enforcement history of a newsletter.
 *
 * <p>Backs the admin moderation surface that lists profile-picture deletions, account suspensions,
 * violating-message takedowns and geographical suspensions applied to the newsletter, plus their
 * appeal state. The {@code locale} variable selects the language for the human-readable policy text
 * the server attaches to each enforcement. The matching response is parsed by
 * {@link FetchNewsletterEnforcementsMexResponse}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterEnforcementsJob")
public final class FetchNewsletterEnforcementsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of
     * {@code WAWebMexFetchNewsletterEnforcementsJobQuery.graphql} on the WhatsApp relay.
     *
     * <p>Sent as the {@code query_id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "36570925279217516";

    /**
     * Holds the GraphQL operation name reported by WhatsApp Web's MEX perf tracker for this query.
     */
    public static final String OPERATION_NAME = "mexFetchNewsletterEnforcements";

    /**
     * Holds the locale tag the server uses when localising the policy text embedded in each
     * enforcement record.
     */
    private final String locale;

    /**
     * Holds the newsletter Jid whose enforcement history is being fetched.
     */
    private final String newsletterId;

    /**
     * Constructs a request for the enforcement history of the given newsletter under the given
     * locale.
     *
     * <p>Both arguments are written unconditionally as the two declared top-level GraphQL variables;
     * the relay expects every declared variable to be present, so callers supply non-null values.
     *
     * @param locale       the locale tag for localised policy text
     * @param newsletterId the newsletter Jid
     */
    public FetchNewsletterEnforcementsMexRequest(String locale, String newsletterId) {
        this.locale = locale;
        this.newsletterId = newsletterId;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #QUERY_ID}.
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the value of {@link #OPERATION_NAME}.
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Produces the {@code {variables: {locale, newsletter_id}}} payload; both are declared
     * top-level variables and are always emitted.
     *
     * @implNote This implementation writes the GraphQL variables directly through a
     * {@link JSONWriter} and wraps any {@link IOException} from the in-memory writer in an
     * {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterEnforcementsJob", exports = "mexFetchNewsletterEnforcements",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("locale");
            writer.writeColon();
            writer.writeString(locale);
            writer.writeName("newsletter_id");
            writer.writeColon();
            writer.writeString(newsletterId);
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
