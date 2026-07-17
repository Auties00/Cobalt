package com.github.auties00.cobalt.bot;

/**
 * The forwarding-verification enforcement level, mirroring WA Web's gating tiers.
 */
public enum BotSignatureEnforcementLevel {
    /**
     * Verification is disabled; a no-op that reports the message as passed.
     */
    NONE,
    /**
     * Verification runs and is logged, but a failure is still treated as passed.
     */
    LOG_ONLY,
    /**
     * Verification runs and a failure is propagated to the caller.
     */
    ENFORCE_BLOCKING
}
