package com.github.auties00.cobalt.wire.linked.signal;

import java.util.List;
import java.util.Objects;

/**
 * Carries the parsed result of a bulk Signal pre-key bundle fetch.
 *
 * <p>The relay's reply is a per-user {@code <list>} that mixes
 * successful key-bundle entries with per-user error entries — a
 * single addressee can fail (rate-limit, mismatch, no pre-keys
 * uploaded) while the rest of the batch resolves cleanly. This
 * carrier preserves the partition: callers iterate
 * {@link #entries() entries} to seed Signal sessions and
 * {@link #errors() errors} to surface per-addressee failures back to
 * the caller's UI / retry policy.
 *
 * <p>This carrier is a plain immutable value object (not a
 * {@link it.auties.protobuf.annotation.ProtobufMessage}) — it surfaces
 * the parsed reply to caller code and never travels on the wire.
 */
public final class PreKeyBundleResult {
    /**
     * The successful per-user pre-key-bundle entries.
     */
    private final List<PreKeyBundleEntry> entries;

    /**
     * The per-user error entries.
     */
    private final List<PreKeyBundleEntryError> errors;

    /**
     * Constructs a new result.
     *
     * @param entries the successful entries; never {@code null}
     * @param errors  the per-user errors; never {@code null}
     * @throws NullPointerException if either argument is {@code null}
     */
    public PreKeyBundleResult(List<PreKeyBundleEntry> entries, List<PreKeyBundleEntryError> errors) {
        Objects.requireNonNull(entries, "entries cannot be null");
        Objects.requireNonNull(errors, "errors cannot be null");
        this.entries = List.copyOf(entries);
        this.errors = List.copyOf(errors);
    }

    /**
     * Returns the successful per-user pre-key-bundle entries.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<PreKeyBundleEntry> entries() {
        return entries;
    }

    /**
     * Returns the per-user error entries.
     *
     * @return an unmodifiable list; never {@code null}
     */
    public List<PreKeyBundleEntryError> errors() {
        return errors;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (PreKeyBundleResult) obj;
        return Objects.equals(this.entries, that.entries)
                && Objects.equals(this.errors, that.errors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, errors);
    }

    @Override
    public String toString() {
        return "PreKeyBundleResult[entries=" + entries
                + ", errors=" + errors + ']';
    }
}
