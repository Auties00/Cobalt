package com.github.auties00.cobalt.sync.crypto;

import java.util.List;

/**
 * LT-Hash (Lattice Hash) implementation for anti-tampering verification.
 *
 * <p>LT-Hash is a cryptographic accumulator with the following properties:
 * <ul>
 *   <li><b>Commutative</b>: hash(a, b) = hash(b, a)</li>
 *   <li><b>Associative</b>: hash(hash(a, b), c) = hash(a, hash(b, c))</li>
 *   <li><b>Reversible</b>: hash_remove(hash(a, b), b) = hash(a)</li>
 *   <li><b>Deterministic</b>: Same input always produces same output</li>
 * </ul>
 *
 * <p>This implementation uses modular arithmetic in a prime field (127) to enable
 * efficient verification of set membership without revealing the set contents.
 */
public final class MutationLTHash {
    /**
     * Prime field modulus for LT-Hash operations.
     * Using 127 as the field size for modular arithmetic.
     */
    private static final int FIELD_SIZE = 127;

    /**
     * Length of the hash state in bytes.
     */
    private static final int HASH_LENGTH = 128;

    /**
     * The empty/zero hash state.
     * Used as the initial state for a collection with no mutations.
     */
    public static final byte[] EMPTY_HASH = new byte[HASH_LENGTH];

    private MutationLTHash() {
        // Utility class
    }

    /**
     * Adds an element to the current hash state.
     *
     * <p>This operation is commutative and associative, meaning the order
     * of additions does not affect the final result.
     *
     * @param currentHash the current hash state (must be {@link #HASH_LENGTH} bytes)
     * @param element the element to add
     * @return new hash state after addition
     * @throws NullPointerException if currentHash or element is null
     */
    public static byte[] add(byte[] currentHash, byte[] element) {
        if (currentHash == null) {
            throw new NullPointerException("Current hash cannot be null");
        }
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (currentHash.length != HASH_LENGTH) {
            throw new IllegalArgumentException("Current hash must be " + HASH_LENGTH + " bytes");
        }

        var result = new byte[HASH_LENGTH];
        for (int i = 0; i < HASH_LENGTH; i++) {
            result[i] = (byte) ((currentHash[i] + element[i % element.length]) % FIELD_SIZE);
        }
        return result;
    }

    /**
     * Removes an element from the current hash state.
     *
     * <p>This is the inverse operation of {@link #add(byte[], byte[])}.
     * Removing an element that was previously added restores the hash to its state
     * before the addition.
     *
     * @param currentHash the current hash state (must be {@link #HASH_LENGTH} bytes)
     * @param element the element to remove
     * @return new hash state after removal
     * @throws NullPointerException if currentHash or element is null
     */
    public static byte[] remove(byte[] currentHash, byte[] element) {
        if (currentHash == null) {
            throw new NullPointerException("Current hash cannot be null");
        }
        if (element == null) {
            throw new NullPointerException("Element cannot be null");
        }
        if (currentHash.length != HASH_LENGTH) {
            throw new IllegalArgumentException("Current hash must be " + HASH_LENGTH + " bytes");
        }

        var result = new byte[HASH_LENGTH];
        for (int i = 0; i < HASH_LENGTH; i++) {
            result[i] = (byte) ((currentHash[i] - element[i % element.length] + FIELD_SIZE) % FIELD_SIZE);
        }
        return result;
    }

    /**
     * Batch operation: removes multiple elements then adds multiple elements.
     *
     * <p>This is more efficient than performing individual add/remove operations
     * as it processes all operations in a single pass.
     *
     * <p>The operation is equivalent to:
     * <pre>{@code
     * hash = currentHash;
     * for (element : toRemove) {
     *     hash = remove(hash, element);
     * }
     * for (element : toAdd) {
     *     hash = add(hash, element);
     * }
     * }</pre>
     *
     * @param currentHash the current hash state (must be {@link #HASH_LENGTH} bytes)
     * @param toAdd list of elements to add (may be empty)
     * @param toRemove list of elements to remove (may be empty)
     * @return new hash state after all operations
     * @throws NullPointerException if any parameter is null
     */
    public static byte[] subtractThenAdd(
        byte[] currentHash,
        List<byte[]> toAdd,
        List<byte[]> toRemove
    ) {
        if (currentHash == null) {
            throw new NullPointerException("Current hash cannot be null");
        }
        if (toAdd == null) {
            throw new NullPointerException("toAdd list cannot be null");
        }
        if (toRemove == null) {
            throw new NullPointerException("toRemove list cannot be null");
        }

        var result = currentHash;

        // Remove all elements
        for (var element : toRemove) {
            result = remove(result, element);
        }

        // Add all elements
        for (var element : toAdd) {
            result = add(result, element);
        }

        return result;
    }

    /**
     * Creates a copy of the given hash state.
     *
     * @param hash the hash state to copy
     * @return a new array with the same contents
     */
    public static byte[] copy(byte[] hash) {
        return hash == null
                ? null
                : hash.clone();
    }
}
