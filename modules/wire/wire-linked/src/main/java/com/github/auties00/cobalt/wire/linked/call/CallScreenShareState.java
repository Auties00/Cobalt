package com.github.auties00.cobalt.wire.linked.call;

/**
 * Enumerates the states a participant's screen share transitions through during a call.
 *
 * <p>A screen share reports one of these states each time a participant starts sharing, stops
 * sharing, or fails to share. The receive side surfaces the state to the application as a typed
 * value rather than a wire code, so a listener observes the transition without decoding the engine's
 * numeric screen share state.
 */
public enum CallScreenShareState {
    /**
     * Indicates that a participant started sharing their screen.
     */
    STARTED,

    /**
     * Indicates that a participant stopped sharing their screen.
     */
    STOPPED,

    /**
     * Indicates that a participant's screen share failed to start or was interrupted by an error.
     */
    FAILED
}
