package com.github.auties00.cobalt.registration.push.apns.plist.value;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedMap;

/**
 * Holds a plist {@code <dict>} stanza of ordered string-keyed entries, the binary plist
 * {@code 0xD0..0xDF} marker family.
 *
 * <p>An instance represents a keyed structure carried by an APNS plist payload, such as the connect
 * handshake, the FairPlay descriptor, or a notification envelope. Iteration order matches the
 * source order so that re-encoding the decoded tree reproduces the captured bytes and any signature
 * recomputed over it stays bit-identical.
 *
 * @implNote This implementation backs the entries with a {@link SequencedMap} (typically a
 *           {@link LinkedHashMap} produced by the parser) and exposes an unmodifiable view via
 *           {@link #entries()}, avoiding the per-construction defensive copy that would otherwise
 *           inflate parsing of large dictionaries.
 * @param entries the ordered entries
 */
public record PlistDictionaryValue(SequencedMap<String, PlistValue> entries) implements PlistValue {
    /**
     * Constructs a dictionary stanza, rejecting a {@code null} backing map.
     *
     * @param entries the ordered entries
     * @throws NullPointerException if {@code entries} is {@code null}
     */
    public PlistDictionaryValue {
        Objects.requireNonNull(entries, "entries");
    }

    /**
     * Returns an unmodifiable view of the backing map.
     *
     * <p>The returned map is a read-only window onto the stored entries; iteration order matches
     * the order the parser observed in the source bytes. Mutating the underlying map is neither
     * supported nor exposed through this view.
     *
     * @return an unmodifiable sequenced map of the entries
     */
    @Override
    public SequencedMap<String, PlistValue> entries() {
        return Collections.unmodifiableSequencedMap(entries);
    }

    /**
     * Returns the value stored under {@code key}, if any.
     *
     * <p>The {@code null}-versus-absent distinction of {@link SequencedMap#get(Object)} is hidden
     * behind {@link Optional}: a present mapping yields a populated optional, an absent key yields
     * an empty one.
     *
     * @param key the lookup key
     * @return an {@link Optional} containing the value, or empty when the key is absent
     */
    public Optional<PlistValue> get(String key) {
        return Optional.ofNullable(entries.get(key));
    }

    /**
     * Returns a new {@link Builder} for assembling a dictionary incrementally.
     *
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Assembles an immutable {@link PlistDictionaryValue} one entry at a time.
     *
     * <p>Call sites use this builder to synthesise an APNS request payload field by field. Each
     * {@code put} overload returns {@code this} so calls chain, and {@link #build()} produces the
     * resulting dictionary.
     *
     * @implNote This implementation backs the in-progress entries with a {@link LinkedHashMap} so
     *           insertion order survives into the resulting dictionary; {@link #build()} snapshots
     *           the map so later mutations of the builder do not bleed into a dictionary already
     *           returned.
     */
    public static final class Builder {
        /**
         * Holds the insertion-ordered entries accumulated by the {@code put} overloads until
         * {@link #build()} snapshots them.
         */
        private final LinkedHashMap<String, PlistValue> map = new LinkedHashMap<>();

        /**
         * Constructs an empty builder.
         *
         * <p>Private so the only entry point is {@link PlistDictionaryValue#builder()}.
         */
        private Builder() {
        }

        /**
         * Stores {@code value} under {@code key}.
         *
         * @param key   the entry key
         * @param value the entry value
         * @return this builder
         * @throws NullPointerException if either argument is {@code null}
         */
        public Builder put(String key, PlistValue value) {
            map.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /**
         * Wraps {@code value} in a {@link PlistStringValue} and stores it under {@code key}.
         *
         * @param key   the entry key
         * @param value the string value
         * @return this builder
         * @throws NullPointerException if either argument is {@code null}
         */
        public Builder put(String key, String value) {
            return put(key, new PlistStringValue(value));
        }

        /**
         * Wraps {@code value} in a {@link PlistDataValue} and stores it under {@code key}.
         *
         * <p>The bytes are retained by reference for zero-copy serialization, so the caller must
         * not mutate {@code value} after this call returns.
         *
         * @param key   the entry key
         * @param value the byte payload
         * @return this builder
         * @throws NullPointerException if either argument is {@code null}
         */
        public Builder put(String key, byte[] value) {
            return put(key, new PlistDataValue(value));
        }

        /**
         * Wraps {@code value} in a {@link PlistBooleanValue} and stores it under {@code key}.
         *
         * @param key   the entry key
         * @param value the boolean value
         * @return this builder
         * @throws NullPointerException if {@code key} is {@code null}
         */
        public Builder put(String key, boolean value) {
            return put(key, new PlistBooleanValue(value));
        }

        /**
         * Wraps {@code value} in a {@link PlistIntegerValue} and stores it under {@code key}.
         *
         * @param key   the entry key
         * @param value the integer value
         * @return this builder
         * @throws NullPointerException if {@code key} is {@code null}
         */
        public Builder put(String key, long value) {
            return put(key, new PlistIntegerValue(value));
        }

        /**
         * Snapshots the accumulated entries into an immutable {@link PlistDictionaryValue}.
         *
         * <p>Safe to call more than once; each call returns a new dictionary built from the current
         * builder state.
         *
         * @implNote This implementation copies the in-progress {@link LinkedHashMap} so subsequent
         *           {@code put} calls on the builder do not affect a dictionary already returned.
         * @return the resulting dictionary
         */
        public PlistDictionaryValue build() {
            return new PlistDictionaryValue(new LinkedHashMap<>(map));
        }
    }
}
