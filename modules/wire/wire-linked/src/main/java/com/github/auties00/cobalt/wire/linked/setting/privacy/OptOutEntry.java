package com.github.auties00.cobalt.wire.linked.setting.privacy;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a single membership row in the marketing-message
 * opt-out list returned by the relay.
 *
 * <p>Each row binds an {@link OptOutTarget target} (either an
 * individual business user or a brand) to the action the user took
 * against it ({@code in} / {@code out} / {@code signup}, etc.), the
 * privacy category the action is scoped to, and the optional expiry
 * after which the action is automatically reverted.
 *
 * <p>The {@code action} and {@code category} attributes come straight
 * from the wire and are exposed as opaque strings so that callers can
 * forward them back to the relay verbatim. A future revision can
 * tighten these into typed enums once the WhatsApp catalog of
 * supported values stabilises.
 */
public final class OptOutEntry {
    /**
     * The optional action recorded against the target, for example
     * {@code "in"}, {@code "out"} or {@code "signup"}. Forwarded
     * verbatim from the relay attribute of the same name.
     */
    private final String action;

    /**
     * The optional privacy category the action is scoped to. Forwarded
     * verbatim from the relay attribute of the same name.
     */
    private final String category;

    /**
     * The optional expiry after which the action is automatically
     * reverted. Decoded from the {@code expiry_at} second-precision
     * timestamp.
     */
    private final Instant expiry;

    /**
     * The opt-out target. Never {@code null}.
     */
    private final OptOutTarget target;

    /**
     * Constructs a new opt-out entry.
     *
     * @param action   the optional action; may be {@code null}
     * @param category the optional category; may be {@code null}
     * @param expiry   the optional expiry; may be {@code null}
     * @param target   the opt-out target; never {@code null}
     * @throws NullPointerException if {@code target} is {@code null}
     */
    public OptOutEntry(String action, String category, Instant expiry, OptOutTarget target) {
        this.action = action;
        this.category = category;
        this.expiry = expiry;
        this.target = Objects.requireNonNull(target, "target cannot be null");
    }

    /**
     * Returns the opt-out target.
     *
     * @return the target; never {@code null}
     */
    public OptOutTarget target() {
        return target;
    }

    /**
     * Returns the optional action.
     *
     * @return an {@link Optional} carrying the action, or empty when
     *         the relay omitted it
     */
    public Optional<String> action() {
        return Optional.ofNullable(action);
    }

    /**
     * Returns the optional category.
     *
     * @return an {@link Optional} carrying the category, or empty when
     *         the relay omitted it
     */
    public Optional<String> category() {
        return Optional.ofNullable(category);
    }

    /**
     * Returns the optional expiry.
     *
     * @return an {@link Optional} carrying the expiry instant, or
     *         empty when the relay omitted it
     */
    public Optional<Instant> expiry() {
        return Optional.ofNullable(expiry);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OptOutEntry that
                && Objects.equals(action, that.action)
                && Objects.equals(category, that.category)
                && Objects.equals(expiry, that.expiry)
                && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, category, expiry, target);
    }

    @Override
    public String toString() {
        return "OptOutEntry[action=" + action
                + ", category=" + category
                + ", expiry=" + expiry
                + ", target=" + target + ']';
    }
}
