package com.github.auties00.cobalt.calls.engine.control;

import com.github.auties00.cobalt.calls.signaling.link.LinkQueryStanza;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the action verb a call link query carries to select what the relay resolves.
 *
 * <p>A call link query asks the relay to expand a link token, and the {@code action} verb tells the relay
 * which view of the link to return: a passive {@link #PREVIEW} for a user who is about to join, or a
 * {@link #LINK_EDIT} lookup for the link owner who is about to change the link's configuration. Each
 * constant binds the {@link #wireValue() wire literal} that the query stamps into its {@code action}
 * attribute, one of {@code preview} or {@code link_edit}.
 *
 * <p>The verb is carried on the wire by {@link LinkQueryStanza#action()} as a raw string; this enum is the
 * typed control layer view a controller selects with, mapped to and from that string by
 * {@link #wireValue()} and {@link #ofWire(String)}.
 *
 * @see LinkQueryStanza
 */
public enum CallLinkQueryAction {
    /**
     * Represents a passive preview lookup a user issues before joining a call link.
     */
    PREVIEW("preview"),

    /**
     * Represents an edit lookup the link owner issues before changing the link configuration.
     */
    LINK_EDIT("link_edit");

    /**
     * Resolves a wire literal to its action, backing {@link #ofWire(String)}.
     *
     * <p>Built once at class initialization from each constant's {@link #wireValue}, so a literal resolves
     * to its action in constant time rather than by scanning {@link #values()}. Keys are the raw literals,
     * so matching is case sensitive, preserving the {@link String#equals(Object)} semantics this lookup
     * replaces.
     */
    private static final Map<String, CallLinkQueryAction> BY_WIRE;

    static {
        var byWire = new HashMap<String, CallLinkQueryAction>();
        for (var action : values()) {
            if (byWire.put(action.wireValue, action) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_WIRE = Map.copyOf(byWire);
    }

    /**
     * Holds the wire literal this action stamps into the {@code action} attribute.
     */
    private final String wireValue;

    /**
     * Constructs a query action constant bound to its wire literal.
     *
     * @param wireValue the literal stamped into the {@code action} attribute
     */
    CallLinkQueryAction(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire literal this action stamps into the {@code action} attribute.
     *
     * @return the {@code action} attribute literal; never {@code null}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Looks up the query action whose {@linkplain #wireValue() wire literal} equals the given value.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_WIRE} map rather than
     * scanning {@link #values()}; a {@code null} literal maps to no entry and yields an empty result.
     * @param wireValue the {@code action} attribute literal to resolve, or {@code null}
     * @return the matching query action, or an empty result when the literal names no action
     */
    public static Optional<CallLinkQueryAction> ofWire(String wireValue) {
        return Optional.ofNullable(BY_WIRE.get(wireValue));
    }
}
