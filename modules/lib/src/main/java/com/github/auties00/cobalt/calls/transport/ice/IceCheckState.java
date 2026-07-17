package com.github.auties00.cobalt.calls.transport.ice;

/**
 * Enumerates the connectivity check states of one ICE candidate pair on the Web to peer interop path.
 *
 * <p>Each pair in the ICE checklist progresses through these states, modelled on the RFC 8445
 * candidate pair check states, as connectivity checks run: it starts {@link #FROZEN}, becomes
 * {@link #WAITING} when it is eligible to be checked, moves to {@link #IN_PROGRESS} while its binding
 * request is outstanding, and ends {@link #SUCCEEDED} when a valid binding response returns or
 * {@link #FAILED} when the check gives up. A succeeded pair may be nominated to carry media. Only the
 * Web to peer interop path runs this checklist; the relay path does not.
 */
public enum IceCheckState {
    /**
     * The pair is not yet eligible for a connectivity check.
     *
     * <p>A frozen pair waits until another pair in its foundation unfreezes it before it can be
     * checked.
     */
    FROZEN,

    /**
     * The pair is eligible to be checked and is queued for a binding request.
     */
    WAITING,

    /**
     * The pair's binding request is outstanding and its response has not yet returned.
     */
    IN_PROGRESS,

    /**
     * The pair's connectivity check succeeded; a valid binding response returned.
     *
     * <p>A succeeded pair is a candidate for nomination to carry media.
     */
    SUCCEEDED,

    /**
     * The pair's connectivity check failed; no valid response returned before it gave up.
     */
    FAILED
}
