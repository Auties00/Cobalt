package it.auties.whatsapp4j.response.model;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Specifies a generic pair
 *
 * @param <K> Key
 * @param <V> Value
 */
public class Pair<K, V>
		implements Comparable<Pair<K, V>> {
	private static final Pair<?, ?> emptyPair = new Pair<>(null, null);

	/**
	 * The specified key
	 */
	private K key;
	/**
	 * The specified value
	 */
	private V value;

	/**
	 * Constructs a new blank pair
	 */
	public Pair() {
		//Nothing Needed
	}

	/**
	 * Constructs a new key value pair
	 *
	 * @param key   The key for the pair
	 * @param value The value for the pair
	 */
	public Pair(@NotNull K key, V value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public String toString() {
		return "Key[" + getKey() + "]-[" + getValue() + "}";
	}

	/**
	 * Gets the key for the given pair
	 *
	 * @return The key given
	 */
	public K getKey() {
		return key;
	}

	/**
	 * Returns the value for the given pair
	 *
	 * @return Sets this Pairs value
	 */
	public V getValue() {
		return value;
	}

	/**
	 * Sets the value for the given pair
	 *
	 * @param value Sets this pairs values
	 * @return this Pair
	 */
	public Pair<K, V> setValue(V value) {
		this.value = value;
		return this;
	}

	/**
	 * Sets the key for the given pair
	 *
	 * @param key Sets this pairs key
	 * @return The pair
	 */
	public Pair<K, V> setKey(@NotNull K key) {
		this.key = key;
		return this;
	}

	@Override
	public int compareTo(@NotNull Pair<K, V> o) {
		return getKey().toString()
					   .compareTo(o.getKey()
								   .toString());
	}

	/**
	 * Returns an empty pair
	 *
	 * @return An empty pair
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Pair<K, V> empty() {
		return (Pair<K, V>) emptyPair;
	}

	/**
	 * If the pair is empty
	 *
	 * @return if the key is null
	 */
	public boolean isEmpty() {
		return key == null;
	}

	/**
	 * Returns a new instance of a pair
	 *
	 * @param key   The key
	 * @param value The value
	 * @param <K>   The key type
	 * @param <V>   The value type
	 * @return The new instance of Pair
	 */
	public static <K, V> Pair<K, V> of(K key, V value) {
		return new Pair<>(key, value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Pair<?, ?> pair = (Pair<?, ?>) o;
		return Objects.equals(getKey(), pair.getKey());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getKey());
	}


}
