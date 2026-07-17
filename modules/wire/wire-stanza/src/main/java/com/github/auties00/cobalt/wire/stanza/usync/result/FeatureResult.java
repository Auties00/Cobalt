package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.stanza.usync.protocol.UsyncFeatureProtocol;

import java.util.Map;

/**
 * Holds the success result of the feature USync parser.
 *
 * Surfaced by USync queries that request the features protocol, such as the
 * VoIP-feature probe and the background contact sync, both of which ask the
 * relay which per-peer feature flags are supported. Keys that the relay omits
 * from the response do not appear in the map; keys that are present carry the
 * {@code value} attribute verbatim (the wire value, typically {@code "1"} for
 * supported).
 *
 * @implNote
 * This implementation accepts only the feature keys enumerated in
 * {@link UsyncFeatureProtocol.FeatureQuery} because the JS parser silently
 * ignores any child element whose tag is not in its dictionary.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncFeature")
public final class FeatureResult implements UsyncProtocolResponse {
    /**
     * Holds the per-feature support map keyed by
     * {@link UsyncFeatureProtocol.FeatureQuery} and valued by the raw
     * {@code value} attribute string returned by the relay.
     */
    private final Map<UsyncFeatureProtocol.FeatureQuery, String> features;

    /**
     * Creates a new feature result.
     *
     * @param features the per-feature map, or {@code null} for an empty map
     */
    public FeatureResult(Map<UsyncFeatureProtocol.FeatureQuery, String> features) {
        this.features = features == null ? Map.of() : Map.copyOf(features);
    }

    /**
     * Returns the per-feature support map.
     *
     * Each entry's value is the raw {@code value} attribute as a string
     * (typically {@code "1"} for supported). A missing key means the relay did
     * not return that feature for this peer, not that the feature is
     * unsupported.
     *
     * @return the feature map, never {@code null}
     */
    public Map<UsyncFeatureProtocol.FeatureQuery, String> features() {
        return features;
    }
}
