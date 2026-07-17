package com.github.auties00.cobalt.calls.config;

import com.github.auties00.cobalt.calls.config.param.VoipParamJsonDeserializer;
import com.github.auties00.cobalt.calls.config.param.VoipParams;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.lang.System.Logger.Level;
import java.util.Optional;

/**
 * Holds one parsed {@code <voip_settings>} element: its format flag and the parameter set
 * carried in its body.
 *
 * <p>The server injects a {@code <voip_settings>} element into the offer it delivers to the
 * callee and into each per device block of the offer acknowledgement. The element carries a
 * single {@code uncompressed} format flag and, in its text body, the configuration document.
 * When the flag is set, the body is plaintext JSON, which this record parses into a
 * {@link VoipParams} set; this is the only format the captured traffic uses. This record is
 * the parsed in memory form of that element, pairing the recovered
 * {@linkplain #uncompressed() format flag} with the resulting
 * {@linkplain #params() parameter set}.
 *
 * <p>An element whose {@code uncompressed} flag is unset carries a body that is not JSON this record
 * does not decode; for such an element the parameter set is left empty and the flag records
 * that the body was not parsed, so a caller can detect the unsupported case without the
 * parse throwing. An element with no body at all also yields an empty parameter set.
 *
 * @param uncompressed whether the element declared its body as uncompressed plaintext JSON
 * @param params       the parameter set parsed from the body, empty when the body was absent
 *                     or not uncompressed JSON
 */
public record VoipSettings(boolean uncompressed, VoipParams params) {
    /**
     * The logger for {@link VoipSettings}.
     */
    private static final System.Logger LOGGER = Log.get(VoipSettings.class);

    /**
     * The element description (tag name) that identifies a {@code <voip_settings>} {@link Stanza}.
     *
     * <p>{@link #of(Stanza, VoipParamJsonDeserializer)} rejects any stanza whose
     * {@link Stanza#description()} is not equal to this value.
     */
    private static final String ELEMENT_DESCRIPTION = "voip_settings";

    /**
     * The attribute name carrying the uncompressed format flag on a {@code <voip_settings>}
     * {@link Stanza}.
     *
     * <p>The attribute holds the wire boolean {@code "1"} when the body is plaintext JSON and is
     * absent otherwise; {@link #of(Stanza, VoipParamJsonDeserializer)} reads it defaulting to
     * {@code false}.
     */
    private static final String UNCOMPRESSED_ATTRIBUTE = "uncompressed";

    /**
     * Parses a {@code <voip_settings>} {@link Stanza} into a settings record.
     *
     * <p>Reads the {@link #UNCOMPRESSED_ATTRIBUTE} flag and, when it is set and the stanza has a
     * text body, parses that body as JSON into a {@link VoipParams} set with the supplied
     * {@link VoipParamJsonDeserializer}. A stanza that is not uncompressed, or that carries no
     * body, yields an empty parameter set while still recording the observed flag, so an
     * unsupported body never causes this method to throw.
     *
     * @param stanza       the {@code <voip_settings>} stanza to parse
     * @param deserializer the deserializer used to parse an uncompressed JSON body
     * @return the parsed settings record pairing the observed flag with the resulting parameter set
     * @throws NullPointerException     if {@code stanza} or {@code deserializer} is {@code null}
     * @throws IllegalArgumentException if {@code stanza} is not a {@code <voip_settings>} stanza,
     *                                  or its uncompressed JSON body is not valid JSON
     */
    public static VoipSettings of(Stanza stanza, VoipParamJsonDeserializer deserializer) {
        if (stanza == null) {
            throw new NullPointerException("stanza must not be null");
        }
        if (deserializer == null) {
            throw new NullPointerException("deserializer must not be null");
        }
        if (!ELEMENT_DESCRIPTION.equals(stanza.description())) {
            throw new IllegalArgumentException("not a <voip_settings> stanza: " + stanza.description());
        }
        var uncompressed = stanza.getAttributeAsBool(UNCOMPRESSED_ATTRIBUTE, false);
        if (!uncompressed) {
            // TODO: decode the compressed (uncompressed flag unset) body into a VoipParams set
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "voip_settings body is compressed, decoding not supported, params discarded");
            }
            return new VoipSettings(false, new VoipParams());
        }
        var params = stanza.toContentString()
                .map(deserializer::parse)
                .orElseGet(VoipParams::new);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "parsed voip_settings, paramCount={0}", params.size());
        }
        return new VoipSettings(true, params);
    }

    /**
     * Returns whether this settings element carried any parameters.
     *
     * @return {@code true} if the {@link #params()} set is non empty, {@code false} otherwise
     */
    public boolean hasParams() {
        return !params.isEmpty();
    }

    /**
     * Returns the parsed {@link VoipParams} set if it is non empty.
     *
     * @return the parameter set, or {@link Optional#empty()} if it carried no parameters
     */
    public Optional<VoipParams> nonEmptyParams() {
        return params.isEmpty() ? Optional.empty() : Optional.of(params);
    }
}
