package com.github.auties00.cobalt.wire.stanza.mex.json.user;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.mex.MexStanza;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

/**
 * Builds the MEX IQ stanza that fetches the authenticated user's privacy preferences.
 *
 * <p>The reply feeds the Settings privacy screen and the gates that outgoing-send paths consult
 * before emitting presence and read receipts. The request materialises the {@code input} GraphQL
 * variable from the requesting device's LID and the privacy features to query; the dispatched
 * stanza is paired with {@link GetPrivacySettingsMexResponse} to consume the reply.
 *
 * <p>The serialised input mirrors the shape WhatsApp Web sends:
 * {@snippet lang = json:
 * {
 *   "query_input": [
 *     {
 *       "jid": "123456789:5@lid",
 *       "privacy_features": ["LAST", "ONLINE", "PROFILE", "ABOUT", "READRECEIPTS",
 *                            "GROUPADD", "CALLADD", "STICKERS", "MESSAGES", "DEFENSE"]
 *     }
 *   ]
 * }
 *}
 * The {@code jid} is the caller's device-addressed LID (WhatsApp Web reads it from
 * {@code getMeDeviceLidOrThrow}), and {@link #DEFAULT_PRIVACY_FEATURES} is the feature set the Web
 * client requests.
 *
 * @see GetPrivacySettingsMexResponse
 */
@WhatsAppWebModule(moduleName = "WAWebMexGetPrivacySetting")
public final class GetPrivacySettingsMexRequest implements MexStanza.Request.Json {
    /**
     * Holds the compiled-document identifier the relay maps to the persisted query.
     *
     * <p>Emitted as the {@code query_id} attribute of the outbound {@code <query>} stanza.
     *
     * @implNote The value matches the compiled query for the WhatsApp Web snapshot this file was
     * generated against, and must be rotated together with that bundle.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacySettingsQuery.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String QUERY_ID = "25637004609323493";

    /**
     * Holds the GraphQL operation name reported alongside this request.
     *
     * <p>WhatsApp Web tags this name onto its per-operation latency metrics.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacySettingsQuery.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "fetchPrivacySettings";

    /**
     * Holds the canonical privacy-feature set WhatsApp Web enumerates when fetching privacy settings.
     *
     * <p>The tokens are the server-side feature enum names; {@code STICKERS} is requested but has no
     * Cobalt privacy-setting counterpart, so it is decoded and dropped by the consumer.
     */
    public static final List<String> DEFAULT_PRIVACY_FEATURES = List.of(
            "LAST", "ONLINE", "PROFILE", "ABOUT", "READRECEIPTS",
            "GROUPADD", "CALLADD", "STICKERS", "MESSAGES", "DEFENSE");

    /**
     * Holds the device-addressed LID that identifies the account whose settings are fetched.
     */
    private final Jid jid;

    /**
     * Holds the privacy feature tokens requested in the {@code privacy_features} array.
     */
    private final List<String> privacyFeatures;

    /**
     * Constructs a privacy-settings fetch request.
     *
     * <p>The {@code jid} is written verbatim into the {@code query_input[0].jid} field; WhatsApp Web
     * populates it from the device-addressed LID of the caller. The {@code privacyFeatures} tokens are
     * the server-side feature enum names; {@link #DEFAULT_PRIVACY_FEATURES} is the set the Web client
     * sends.
     *
     * @param jid the device-addressed LID of the requesting account
     * @param privacyFeatures the privacy feature tokens to request
     * @throws NullPointerException if {@code jid} or {@code privacyFeatures} is {@code null}
     */
    public GetPrivacySettingsMexRequest(Jid jid, List<String> privacyFeatures) {
        this.jid = Objects.requireNonNull(jid, "jid cannot be null");
        this.privacyFeatures = List.copyOf(Objects.requireNonNull(privacyFeatures, "privacyFeatures cannot be null"));
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
     * @implNote This implementation serialises
     * {@code {"variables": {"input": {"query_input": [{"jid": ..., "privacy_features": [...]}]}}}} and
     * defers envelope construction to {@link MexStanza.Request.Json#createMexNode(String, String)}.
     * The {@code query_input} array is always a single element because the Cobalt API fetches one
     * account per request.
     */
    @WhatsAppWebExport(moduleName = "WAWebMexGetPrivacySetting", exports = "fetchPrivacySettings",
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

            writer.writeName("query_input");
            writer.writeColon();
            writer.startArray();
            writer.startObject();

            writer.writeName("jid");
            writer.writeColon();
            writer.writeString(jid.toString());

            writer.writeName("privacy_features");
            writer.writeColon();
            writer.startArray();
            for (var i = 0; i < privacyFeatures.size(); i++) {
                if (i > 0) {
                    writer.writeComma();
                }
                writer.writeString(privacyFeatures.get(i));
            }
            writer.endArray();

            writer.endObject();
            writer.endArray();

            writer.endObject();
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
