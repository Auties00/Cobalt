package it.auties.whatsapp4j.utils.internal;

import lombok.NonNull;

/**
 * Specifies a generic pair
 *
 * @param <K> Key
 * @param <V> Value
 */
public record Pair<K, V>(@NonNull K key, @NonNull V value) {
}
