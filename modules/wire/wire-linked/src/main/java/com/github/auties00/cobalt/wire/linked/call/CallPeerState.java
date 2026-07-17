package com.github.auties00.cobalt.wire.linked.call;

/**
 * Enumerates the transient mid-call states a peer can advertise through a call peer-state payload.
 *
 * <p>Each constant other than {@link #UNKNOWN} maps to one literal carried by the {@code state}
 * attribute of an inbound {@code peer_state} payload. The receive side parses that literal into one of
 * these constants via {@link #fromWireValue(String)} and surfaces it to the application as a typed
 * value rather than a wire string. Any literal that does not match a known constant resolves to
 * {@link #UNKNOWN}, whose {@link #wireValue()} is {@code null}.
 */
public enum CallPeerState {
    /**
     * Indicates that the peer's transport has come up and media is flowing.
     *
     * <p>This is the peer-side counterpart of the local {@link CallState#ACTIVE} state.
     */
    CONNECTED("connected"),

    /**
     * Indicates that the peer's transport has dropped and the peer is attempting to re-establish it.
     *
     * <p>This typically reflects a network blip on the peer side rather than a deliberate action.
     */
    RECONNECTING("reconnecting"),

    /**
     * Indicates that the peer is busy in another call.
     *
     * <p>This is received during the {@link CallState#RINGING} window, before the peer accepts.
     */
    BUSY("busy"),

    /**
     * Indicates that the peer has placed the call on hold.
     */
    HOLD("hold"),

    /**
     * Indicates that the peer has resumed the call from hold.
     */
    RESUMED("resumed"),

    /**
     * Represents any wire value not covered by another constant.
     *
     * <p>The receive side maps unrecognized literals here; the original literal is not retained, so
     * {@link #wireValue()} returns {@code null} for this constant.
     */
    UNKNOWN(null);

    /**
     * Holds the literal carried by the wire {@code state} attribute, or {@code null} for
     * {@link #UNKNOWN}.
     */
    private final String wireValue;

    /**
     * Constructs a constant bound to its wire-level {@code state} literal.
     *
     * @param wireValue the literal carried by the {@code state} attribute, or {@code null} for
     *                  {@link #UNKNOWN}
     */
    CallPeerState(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-level {@code state} literal that maps to this constant.
     *
     * @return the wire literal, or {@code null} for {@link #UNKNOWN}
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Maps a wire-level {@code state} attribute to its constant.
     *
     * <p>A {@code null} argument and any literal that does not match a known constant both resolve to
     * {@link #UNKNOWN}, so the result is never {@code null}.
     *
     * @param wire the wire string, or {@code null}
     * @return the matching constant, or {@link #UNKNOWN} when unrecognized; never {@code null}
     */
    public static CallPeerState fromWireValue(String wire) {
        if (wire == null) {
            return UNKNOWN;
        }
        for (var v : values()) {
            if (wire.equals(v.wireValue)) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
