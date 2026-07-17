package com.github.auties00.cobalt.wire.stanza.usync.protocol;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocol;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncProtocolResult;
import com.github.auties00.cobalt.wire.stanza.usync.UsyncUser;
import com.github.auties00.cobalt.wire.stanza.usync.result.FeatureResult;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the USync {@code feature} protocol.
 *
 * This descriptor asks the relay which of the named features each peer
 * supports. The {@code <feature>} query element carries one empty child per
 * requested feature key; the response carries a {@code value} attribute per
 * supported key. The descriptor carries no per-user request payload; the
 * requested feature set is fixed at construction time.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncFeature")
public final class UsyncFeatureProtocol implements UsyncProtocol {
    /**
     * Holds the wire literal for the protocol tag name.
     */
    public static final String NAME = "feature";

    /**
     * Holds the features the relay is asked to report on; never empty by
     * construction.
     */
    private final List<FeatureQuery> queries;

    /**
     * Creates a feature-protocol descriptor for the given queries.
     *
     * At least one feature key must be supplied; an empty list is rejected
     * with {@link IllegalArgumentException}.
     *
     * @param queries the features to request
     * @throws NullPointerException     if {@code queries} is {@code null}
     * @throws IllegalArgumentException if {@code queries} is empty
     */
    @WhatsAppWebExport(moduleName = "WAWebUsyncFeature",
            exports = "USyncFeaturesProtocol", adaptation = WhatsAppAdaptation.DIRECT)
    public UsyncFeatureProtocol(List<FeatureQuery> queries) {
        Objects.requireNonNull(queries, "queries cannot be null");
        if (queries.isEmpty()) {
            throw new IllegalArgumentException("must specify at least one query");
        }
        this.queries = List.copyOf(queries);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncFeature",
            exports = "USyncFeaturesProtocol.getName", adaptation = WhatsAppAdaptation.DIRECT)
    public String name() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation emits one empty child per requested feature key,
     * keyed by {@link FeatureQuery#wireValue()}, building the child set from
     * the request data rather than from a pre-built dictionary.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncFeature",
            exports = "USyncFeaturesProtocol.getQueryElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza buildQueryElement() {
        var children = queries.stream()
                .map(q -> new StanzaBuilder().description(q.wireValue()).build())
                .toList();
        return new StanzaBuilder().description(NAME).content(children).build();
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation always returns {@link Optional#empty()} because the
     * feature protocol has no per-user payload on the request side.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncFeature",
            exports = "USyncFeaturesProtocol.getUserElement", adaptation = WhatsAppAdaptation.DIRECT)
    public Optional<Stanza> buildUserElement(UsyncUser user) {
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * This override preserves every {@link FeatureQuery} key the relay
     * returns with a {@code value} attribute, even keys that were not
     * requested.
     *
     * @implNote
     * This implementation walks every {@link FeatureQuery} constant rather
     * than only the requested ones, so a relay that reports more keys than
     * were asked for has all of its values captured.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebUsyncFeature",
            exports = "featureParser", adaptation = WhatsAppAdaptation.ADAPTED)
    public UsyncProtocolResult parseUserResult(Stanza child) {
        if (!child.hasDescription(NAME)) {
            throw new IllegalStateException("expected <" + NAME + ">, got <" + child.description() + ">");
        }
        var error = UsyncContactProtocol.parseError(child);
        if (error.isPresent()) {
            return error.get();
        }
        var values = new HashMap<FeatureQuery, String>();
        for (var key : FeatureQuery.values()) {
            child.getChild(key.wireValue()).ifPresent(c ->
                    c.getAttributeAsString("value").ifPresent(v -> values.put(key, v)));
        }
        return new FeatureResult(values);
    }

    /**
     * Enumerates the feature keys the {@code <feature>} query understands.
     *
     * Each constant binds a Java name to the wire literal the relay matches
     * against; the relay ignores keys it does not recognise.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncFeature")
    public enum FeatureQuery {
        /**
         * Requests document-message support.
         */
        DOCUMENT("document"),
        /**
         * Requests generic encryption support.
         */
        ENCRYPT("encrypt"),
        /**
         * Requests encrypted block-list support.
         */
        ENCRYPT_BLIST("encrypt_blist"),
        /**
         * Requests encrypted contact-card support.
         */
        ENCRYPT_CONTACT("encrypt_contact"),
        /**
         * Requests encrypted group v2 support.
         */
        ENCRYPT_GROUP_GEN2("encrypt_group_gen2"),
        /**
         * Requests encrypted image support.
         */
        ENCRYPT_IMAGE("encrypt_image"),
        /**
         * Requests encrypted location-share support.
         */
        ENCRYPT_LOCATION("encrypt_location"),
        /**
         * Requests encrypted URL support.
         */
        ENCRYPT_URL("encrypt_url"),
        /**
         * Requests encryption v2 support.
         */
        ENCRYPT_V2("encrypt_v2"),
        /**
         * Requests VoIP signalling support.
         */
        VOIP("voip"),
        /**
         * Requests multi-agent (CRM) support.
         */
        MULTI_AGENT("multi_agent");

        /**
         * Holds the literal tag name on the wire.
         */
        private final String wireValue;

        /**
         * Binds a constant to its wire literal.
         *
         * @param wireValue the literal tag name on the wire
         */
        FeatureQuery(String wireValue) {
            this.wireValue = wireValue;
        }

        /**
         * Returns the literal tag name on the wire.
         *
         * Used by {@link UsyncFeatureProtocol#buildQueryElement()} to name the
         * per-feature empty child and by
         * {@link UsyncFeatureProtocol#parseUserResult(Stanza)} to look up the
         * matching response element.
         *
         * @return the wire literal
         */
        public String wireValue() {
            return wireValue;
        }
    }
}
