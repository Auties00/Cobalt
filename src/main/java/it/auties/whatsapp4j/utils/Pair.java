package it.auties.whatsapp4j.utils;

import jakarta.validation.constraints.NotNull;

/**
 * Specifies a generic pair
 *
 * @param <K> Key
 * @param <V> Value
 */
public record Pair<K, V>(@NotNull K key, @NotNull V value) {
}
