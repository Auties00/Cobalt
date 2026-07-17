package com.github.auties00.cobalt.wire.stanza.mex.json.newsletter;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the MEX request that fetches the full metadata of a single newsletter, keyed either by
 * newsletter Jid or by invite token.
 *
 * <p>This request drives the primary newsletter-hydration path. The {@link Input#type()}
 * discriminator selects whether {@link Input#key()} is interpreted as a newsletter Jid
 * ({@code "JID"}) or an invite token ({@code "INVITE"}), and the optional {@code fetch_*} booleans
 * gate the inclusion of heavier fragments (creation time, full image, status metadata, viewer
 * metadata, paid-subscription details). Submit it through the MEX IQ dispatcher and pair the result
 * with {@link FetchNewsletterMexResponse#of(Stanza)}.
 */
@WhatsAppWebModule(moduleName = "WAWebMexFetchNewsletterJob")
public final class FetchNewsletterMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled persisted-query identifier of {@code WAWebMexFetchNewsletterJobQuery.graphql}
     * on the WhatsApp relay.
     *
     * <p>Sent as the {@code id} attribute of the outgoing {@code <query>} child.
     */
    public static final String QUERY_ID = "35452404184358876";

    /**
     * Holds the GraphQL operation name reported by WA Web's {@code MexPerfTracker} for this query.
     */
    public static final String OPERATION_NAME = "mexGetNewsletter";

    /**
     * Holds the {@code fetch_creation_time} GraphQL variable; {@code null} is serialised as
     * {@code false}.
     */
    private final Boolean fetchCreationTime;

    /**
     * Holds the {@code fetch_full_image} GraphQL variable; {@code null} is serialised as
     * {@code false}.
     */
    private final Boolean fetchFullImage;

    /**
     * Holds the {@code fetch_status_metadata} GraphQL variable; {@code null} is serialised as
     * {@code false}.
     */
    private final Boolean fetchStatusMetadata;

    /**
     * Holds the {@code fetch_viewer_metadata} GraphQL variable; {@code null} is serialised as
     * {@code false}.
     */
    private final Boolean fetchViewerMetadata;

    /**
     * Holds the {@code fetch_wamo_sub} GraphQL variable; {@code null} is serialised as
     * {@code false}.
     */
    private final Boolean fetchWamoSub;

    /**
     * Holds the structured {@code input} GraphQL variable.
     */
    private final Input input;

    /**
     * Constructs a request without the {@code fetch_status_metadata} flag, for callers on the legacy
     * non-status code path.
     *
     * <p>Equivalent to invoking the full constructor with {@code fetchStatusMetadata} set to
     * {@code null}, which serialises as {@code false}.
     *
     * @param fetchCreationTime   the {@code fetch_creation_time} flag; {@code null} is sent as {@code false}
     * @param fetchFullImage      the {@code fetch_full_image} flag; {@code null} is sent as {@code false}
     * @param fetchViewerMetadata the {@code fetch_viewer_metadata} flag; {@code null} is sent as {@code false}
     * @param fetchWamoSub        the {@code fetch_wamo_sub} flag; {@code null} is sent as {@code false}
     * @param input               the structured {@code input} GraphQL variable
     */
    public FetchNewsletterMexRequest(Boolean fetchCreationTime, Boolean fetchFullImage, Boolean fetchViewerMetadata, Boolean fetchWamoSub, Input input) {
        this(fetchCreationTime, fetchFullImage, null, fetchViewerMetadata, fetchWamoSub, input);
    }

    /**
     * Constructs a request with the full set of fragment-gating flags and the structured input.
     *
     * <p>Every {@code fetch_*} flag is a declared top-level GraphQL variable and is always emitted;
     * passing {@code null} serialises as {@code false} rather than omitting the variable.
     *
     * @param fetchCreationTime    the {@code fetch_creation_time} flag; {@code null} is sent as {@code false}
     * @param fetchFullImage       the {@code fetch_full_image} flag; {@code null} is sent as {@code false}
     * @param fetchStatusMetadata  the {@code fetch_status_metadata} flag; {@code null} is sent as {@code false}
     * @param fetchViewerMetadata  the {@code fetch_viewer_metadata} flag; {@code null} is sent as {@code false}
     * @param fetchWamoSub         the {@code fetch_wamo_sub} flag; {@code null} is sent as {@code false}
     * @param input                the structured {@code input} GraphQL variable
     */
    public FetchNewsletterMexRequest(Boolean fetchCreationTime, Boolean fetchFullImage, Boolean fetchStatusMetadata, Boolean fetchViewerMetadata, Boolean fetchWamoSub, Input input) {
        this.fetchCreationTime = fetchCreationTime;
        this.fetchFullImage = fetchFullImage;
        this.fetchStatusMetadata = fetchStatusMetadata;
        this.fetchViewerMetadata = fetchViewerMetadata;
        this.fetchWamoSub = fetchWamoSub;
        this.input = input;
    }

    /**
     * Returns {@link #QUERY_ID}.
     *
     * @return the persisted-query identifier of this query
     */
    @Override
    public String id() {
        return QUERY_ID;
    }

    /**
     * Returns {@link #OPERATION_NAME}.
     *
     * @return the GraphQL operation name of this query
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * Serialises this request into a MEX IQ {@link StanzaBuilder}.
     *
     * <p>Produces the
     * {@code {variables: {input: {key, type, view_role}, fetch_viewer_metadata, fetch_full_image, fetch_creation_time, fetch_wamo_sub, fetch_status_metadata}}}
     * payload. The {@code input} object and all five {@code fetch_*} booleans are declared top-level
     * variables and are always emitted; a {@code null} boolean is written as {@code false}. Only the
     * {@code input} sub-fields ({@code key}, {@code type}, {@code view_role}) are omitted when unset.
     *
     * @implNote This implementation writes the GraphQL variables directly through {@link JSONWriter}
     * and wraps any {@link IOException} from the in-memory writer in an {@link UncheckedIOException}.
     *
     * @return the {@link StanzaBuilder} carrying the IQ envelope and serialised GraphQL variables
     * @throws UncheckedIOException if the underlying writer fails
     */
    @WhatsAppWebExport(moduleName = "WAWebMexFetchNewsletterJob", exports = "mexGetNewsletter",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public StanzaBuilder toStanza() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("variables");
            writer.writeColon();
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (input.key() != null) {
                writer.writeName("key");
                writer.writeColon();
                writer.writeString(input.key());
            }
            if (input.type() != null) {
                writer.writeName("type");
                writer.writeColon();
                writer.writeString(input.type());
            }
            if (input.viewRole() != null) {
                writer.writeName("view_role");
                writer.writeColon();
                writer.writeString(input.viewRole());
            }
            writer.endObject();
            writer.writeName("fetch_viewer_metadata");
            writer.writeColon();
            writer.writeBool(fetchViewerMetadata != null && fetchViewerMetadata);
            writer.writeName("fetch_full_image");
            writer.writeColon();
            writer.writeBool(fetchFullImage != null && fetchFullImage);
            writer.writeName("fetch_creation_time");
            writer.writeColon();
            writer.writeBool(fetchCreationTime != null && fetchCreationTime);
            writer.writeName("fetch_wamo_sub");
            writer.writeColon();
            writer.writeBool(fetchWamoSub != null && fetchWamoSub);
            writer.writeName("fetch_status_metadata");
            writer.writeColon();
            writer.writeBool(fetchStatusMetadata != null && fetchStatusMetadata);
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

    /**
     * Wraps the structured {@code input} GraphQL variable consumed by the {@code mexGetNewsletter}
     * query.
     *
     * <p>The {@link #type()} discriminator decides how the relay interprets {@link #key()}:
     * {@code "JID"} for a newsletter Jid lookup and {@code "INVITE"} for an invite-token lookup.
     * {@link #viewRole()} selects which viewer-role fragment the relay populates in the response.
     */
    public static final class Input {
        /**
         * Holds the newsletter Jid string or the invite token.
         */
        private final String key;

        /**
         * Holds the lookup discriminator: {@code "JID"} or {@code "INVITE"}.
         */
        private final String type;

        /**
         * Holds the {@code view_role} GraphQL variable, or {@code null} to omit it.
         */
        private final String viewRole;

        /**
         * Constructs an input wrapper from the parsed sub-fields.
         *
         * <p>Higher-level builders typically derive {@code type} from whether {@code key} is a
         * newsletter Jid, as WA Web does.
         *
         * @param key      the newsletter Jid string or invite token
         * @param type     the lookup discriminator, either {@code "JID"} or {@code "INVITE"}
         * @param viewRole the viewer role enum name, may be {@code null}
         */
        public Input(String key, String type, String viewRole) {
            this.key = key;
            this.type = type;
            this.viewRole = viewRole;
        }

        /**
         * Returns the newsletter Jid string or invite token.
         *
         * @return the {@code key} variable, or {@code null} when unset
         */
        public String key() {
            return key;
        }

        /**
         * Returns the lookup discriminator.
         *
         * @return the {@code type} variable, or {@code null} when unset
         */
        public String type() {
            return type;
        }

        /**
         * Returns the {@code view_role} GraphQL variable.
         *
         * @return the viewer-role enum name, or {@code null} when unset
         */
        public String viewRole() {
            return viewRole;
        }
    }
}
