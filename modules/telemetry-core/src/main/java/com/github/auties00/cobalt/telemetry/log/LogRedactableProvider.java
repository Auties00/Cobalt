package com.github.auties00.cobalt.telemetry.log;

/**
 * A type that supplies a {@link LogRedactable} view of itself to the privacy-aware logging pipeline.
 *
 * <p>{@link Log} masks any value that implements this interface by rendering the {@link LogRedactable} it
 * returns, so a type whose natural string form would leak sensitive content never reaches the output in the
 * clear. This is the contract for a type that carries a sensitive value but cannot mask it on its own: the
 * fingerprint engine is package-private, so a type declared outside this package states what it holds by
 * returning the record that classifies it rather than hashing anything itself. A JID, for example, returns a
 * {@link LogRedactable.User} over its string form and lets that record decide how much of the address
 * survives.
 *
 * <p>{@link LogRedactable} extends this interface and returns itself, so a type that already renders its own
 * redacted form satisfies both contracts at once and the pipeline needs only a single check to mask either
 * kind of value.
 */
public interface LogRedactableProvider {
    /**
     * Returns the redactable view of this value, whose {@link LogRedactable#toRedactedLog()} the logging
     * pipeline renders in place of this value's {@code toString()}.
     *
     * @return the redactable view; never {@code null}
     */
    LogRedactable toLogRedactable();
}
