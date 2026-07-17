package com.github.auties00.cobalt.calls.capability;

import com.github.auties00.cobalt.telemetry.log.Log;

import java.io.ByteArrayOutputStream;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * A versioned capability bitset the voip engine advertises in call signaling and tests
 * against a peer's advertisement.
 *
 * <p>The engine groups capability bits by version: each version owns its own {@code 512} bit
 * ({@code 64} byte) mask, and a bit is identified by a {@link VoipCapability#index() bit index}
 * within that mask together with the {@link VoipCapability#sinceVersion() version} it belongs
 * to. Within a mask, bits are stored least significant first: index {@code n} occupies byte
 * {@code n >> 3}, bit {@code n & 7}. A device advertises this structure as a compact byte
 * stream that travels in the {@code voip_capability} child of {@code offer}, {@code preaccept},
 * {@code accept}, and {@code link_join}, and it negotiates features by testing whether a given
 * capability bit is present in both its own and the peer's masks.
 *
 * <p>Instances are mutable through {@link #set(int, int)} and {@link #clear(int, int)} and are
 * queried through {@link #contains(int, int)}; {@link #set(VoipCapability)},
 * {@link #clear(VoipCapability)}, and {@link #contains(VoipCapability)} are the named bit
 * conveniences. {@link #serialize()} produces the wire byte stream and {@link #deserialize(byte[])}
 * parses one back. The bit index must lie in the inclusive range {@code 0} through
 * {@link VoipCapability#MAX_INDEX}, and the version must be a positive value no larger than
 * {@link #MAX_VERSION}; out of range arguments are rejected with {@link IllegalArgumentException}.
 *
 * <p>The {@linkplain #serialize() wire form} is {@code [top_version_byte]} followed, for each
 * version present from the top version downward, by {@code [mask_length_byte][mask bytes]} where
 * the mask length is the number of bytes remaining after trailing all zero bytes are trimmed
 * (a version whose mask is entirely zero is skipped). {@link #standard()} returns the reference
 * client's self advertisement, whose {@link #serialize()} is the canonical seven byte stream
 * {@code 01 05 F7 09 E4 BB 13}.
 *
 * @implNote This implementation holds one {@link #MASK_BYTES} byte mask per version and caps
 * both the version and every serialized mask length below {@code 0xff} so each fits in a single
 * unsigned wire byte; the bit at index {@code n} is byte {@code n >> 3}, bit {@code n & 7},
 * least significant first. The {@link #standard()} bit set is recovered by decoding the captured
 * advertisement {@code 01 05 F7 09 E4 BB 13} (version {@code 1}, length {@code 5}, mask
 * {@code F7 09 E4 BB 13}) into its twenty two set indices; {@link #serialize()} of that set
 * reproduces the stream byte for byte, which is the load bearing interoperability check.
 */
public final class VoipCapabilities implements Cloneable {
    /**
     * The logger for {@link VoipCapabilities}.
     */
    private static final System.Logger LOGGER = Log.get(VoipCapabilities.class);

    /**
     * The number of bytes in a single version's capability mask.
     *
     * <p>Each version owns a {@code 64} byte ({@code 512} bit) mask; this is the buffer width
     * the engine allocates and the upper bound on a serialized mask length.
     */
    public static final int MASK_BYTES = 0x40;

    /**
     * The largest capability version the wire form can carry.
     *
     * <p>The top version byte and every per version length byte are single unsigned bytes the
     * serializer asserts to be strictly less than {@code 0xff}, so the highest representable
     * version is {@code 0xfe}.
     */
    public static final int MAX_VERSION = 0xfe;

    /**
     * Caches the constant array so {@link #standard()} and {@link #namedCapabilities()} do not pay the
     * defensive clone cost of {@link VoipCapability#values()} on every call.
     */
    private static final VoipCapability[] VALUES = VoipCapability.values();

    /**
     * The reference client's standard self advertisement, built once at class initialization and
     * defensively cloned by {@link #standard()}.
     *
     * <p>Holds exactly the version {@code 1} bits the reference wa-voip client advertises; its
     * {@link #serialize()} is the canonical seven byte stream {@code 01 05 F7 09 E4 BB 13}. It is never
     * handed to callers directly, so this shared instance is never mutated after construction.
     */
    private static final VoipCapabilities STANDARD = createStandard();

    /**
     * The per version masks, keyed by version and ordered ascending.
     *
     * <p>A version is present in this map once any of its bits has been set; a mask is a fresh
     * {@link #MASK_BYTES} byte array. The map is iterated in descending key order during
     * {@link #serialize()} so the top version is written first; a typical advertisement holds a single
     * version, so the map stays small.
     */
    private final TreeMap<Integer, byte[]> masks;

    /**
     * Constructs an empty capability bitset with no versions present.
     */
    public VoipCapabilities() {
        this(new TreeMap<>());
    }

    /**
     * Constructs a capability bitset backed by the given version mask map.
     *
     * <p>The map is adopted by reference, not copied, so the caller must neither retain nor mutate it
     * afterwards. It exists so {@link #clone()} can hand over a map built in a single linear pass from
     * an already sorted source rather than inserting entry by entry into an empty tree.
     *
     * @param masks the version mask map to adopt, keyed by version and ordered ascending
     */
    private VoipCapabilities(TreeMap<Integer, byte[]> masks) {
        this.masks = masks;
    }

    /**
     * Returns the reference client's standard self advertisement.
     *
     * <p>The returned bitset has exactly the version {@code 1} bits the reference wa-voip
     * client advertises; its {@link #serialize()} is the canonical seven byte stream
     * {@code 01 05 F7 09 E4 BB 13}. The instance is a fresh, independently mutable copy, so callers
     * may tailor it before serializing without affecting other callers.
     *
     * @return a new capability bitset holding the standard self advertisement
     * @implNote This implementation clones the {@link #STANDARD} bitset built once at class
     * initialization rather than rebuilding it bit by bit on each call.
     */
    public static VoipCapabilities standard() {
        return STANDARD.clone();
    }

    /**
     * Builds the {@link #STANDARD} advertisement by setting every named capability bit.
     *
     * @return a new bitset holding the reference client's standard self advertisement
     */
    private static VoipCapabilities createStandard() {
        var capabilities = new VoipCapabilities();
        for (var capability : VALUES) {
            capabilities.set(capability);
        }
        return capabilities;
    }

    /**
     * Sets the capability bit at the given index for the given version.
     *
     * <p>If the version has no mask yet, one is allocated. The bit is selected as byte
     * {@code index >> 3}, bit {@code index & 7} (least significant first). Setting a bit that
     * is already set has no effect.
     *
     * @param index   the bit index within the version mask
     * @param version the capability version
     * @return this bitset
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..}{@link VoipCapability#MAX_INDEX}
     *                                  or {@code version} is outside {@code 1..}{@link #MAX_VERSION}
     */
    public VoipCapabilities set(int index, int version) {
        checkIndex(index);
        checkVersion(version);
        var mask = masks.computeIfAbsent(version, _ -> new byte[MASK_BYTES]);
        mask[index >> 3] |= (byte) (1 << (index & 7));
        return this;
    }

    /**
     * Sets the bit named by the given capability constant.
     *
     * @param capability the capability to set
     * @return this bitset
     * @throws NullPointerException if {@code capability} is {@code null}
     */
    public VoipCapabilities set(VoipCapability capability) {
        return set(capability.index(), capability.sinceVersion());
    }

    /**
     * Clears the capability bit at the given index for the given version.
     *
     * <p>Clearing a bit in a version that has no mask, or a bit that is already clear, has no
     * effect; the version's mask is retained even when it becomes entirely zero, and a zero
     * mask is skipped by {@link #serialize()}.
     *
     * @param index   the bit index within the version mask
     * @param version the capability version
     * @return this bitset
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..}{@link VoipCapability#MAX_INDEX}
     *                                  or {@code version} is outside {@code 1..}{@link #MAX_VERSION}
     */
    public VoipCapabilities clear(int index, int version) {
        checkIndex(index);
        checkVersion(version);
        var mask = masks.get(version);
        if (mask != null) {
            mask[index >> 3] &= (byte) ~(1 << (index & 7));
        }
        return this;
    }

    /**
     * Clears the bit named by the given capability constant.
     *
     * @param capability the capability to clear
     * @return this bitset
     * @throws NullPointerException if {@code capability} is {@code null}
     */
    public VoipCapabilities clear(VoipCapability capability) {
        return clear(capability.index(), capability.sinceVersion());
    }

    /**
     * Returns whether the capability bit at the given index is set for the given version.
     *
     * <p>The result is {@code false} when the version is absent. Otherwise it is bit
     * {@code index & 7} of mask byte {@code index >> 3} of that version's mask.
     *
     * @param index   the bit index within the version mask
     * @param version the capability version
     * @return {@code true} if the bit is set, {@code false} otherwise
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..}{@link VoipCapability#MAX_INDEX}
     *                                  or {@code version} is outside {@code 1..}{@link #MAX_VERSION}
     */
    public boolean contains(int index, int version) {
        checkIndex(index);
        checkVersion(version);
        var mask = masks.get(version);
        if (mask == null) {
            return false;
        }
        return ((mask[index >> 3] >> (index & 7)) & 1) != 0;
    }

    /**
     * Returns whether the bit named by the given capability constant is set.
     *
     * @param capability the capability to test
     * @return {@code true} if the bit is set, {@code false} otherwise
     * @throws NullPointerException if {@code capability} is {@code null}
     */
    public boolean contains(VoipCapability capability) {
        return contains(capability.index(), capability.sinceVersion());
    }

    /**
     * Returns the highest version that has any mask present.
     *
     * <p>This is the top version byte the {@linkplain #serialize() wire form} leads with. The
     * result is {@code 0} when no version is present.
     *
     * @return the highest present version, or {@code 0} when the bitset is empty
     */
    public int topVersion() {
        return masks.isEmpty() ? 0 : masks.lastKey();
    }

    /**
     * Serializes this bitset into its wire byte stream.
     *
     * <p>The output is a leading top version byte, then for each version present from the top
     * version downward a mask length byte followed by that many mask bytes. The mask length is
     * the index of the highest non zero byte plus one (trailing all zero bytes are trimmed); a
     * version whose mask is entirely zero contributes nothing. An empty bitset serializes to a
     * single {@code 0x00} byte.
     *
     * @return the serialized wire bytes
     * @throws IllegalStateException if any present version exceeds {@link #MAX_VERSION} or any
     *                               trimmed mask length exceeds {@link #MASK_BYTES}
     */
    public byte[] serialize() {
        var top = topVersion();
        if (top > MAX_VERSION) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "voip capability serialize failed, top version {0} exceeds max {1}", top, MAX_VERSION);
            throw new IllegalStateException("Capability version " + top + " exceeds max " + MAX_VERSION);
        }
        var out = new ByteArrayOutputStream();
        out.write(top);
        for (var entry : masks.descendingMap().entrySet()) {
            var version = entry.getKey();
            var mask = entry.getValue();
            var length = trimmedLength(mask);
            if (length == 0) {
                continue;
            }
            if (length > MASK_BYTES) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "voip capability serialize failed, mask length {0} for version {1} exceeds max {2}",
                            length, version, MASK_BYTES);
                }
                throw new IllegalStateException("Capability mask length " + length + " for version " + version + " exceeds max " + MASK_BYTES);
            }
            out.write(length);
            out.write(mask, 0, length);
        }
        var serialized = out.toByteArray();
        if (Log.TRACE) LOGGER.log(Level.TRACE, "serialized voip capabilities, top_version={0}, bytes={1}", top, serialized.length);
        return serialized;
    }

    /**
     * Parses a wire byte stream into a capability bitset.
     *
     * <p>The first byte is the top version. Each subsequent record is a length byte followed by
     * that many mask bytes, assigned to a version counting down from the top version, mirroring
     * the descending order {@link #serialize()} writes. A single {@code 0x00} byte (top version
     * zero with no records) yields an empty bitset.
     *
     * @param bytes the serialized wire bytes
     * @return the parsed capability bitset
     * @throws NullPointerException     if {@code bytes} is {@code null}
     * @throws IllegalArgumentException if {@code bytes} is empty, declares a length byte without
     *                                  enough mask bytes following, exceeds {@link #MASK_BYTES}, or
     *                                  runs out of versions before the bytes are consumed
     */
    public static VoipCapabilities deserialize(byte[] bytes) {
        if (bytes.length == 0) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "voip capability deserialize failed, empty byte stream");
            throw new IllegalArgumentException("Empty capability byte stream");
        }
        var capabilities = new VoipCapabilities();
        var version = bytes[0] & 0xff;
        var offset = 1;
        while (offset < bytes.length) {
            if (version < 1) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "voip capability deserialize failed, more masks than versions, bytes={0}", bytes.length);
                throw new IllegalArgumentException("Capability byte stream has more masks than versions");
            }
            var length = bytes[offset++] & 0xff;
            if (length > MASK_BYTES) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "voip capability deserialize failed, mask length {0} exceeds max {1}", length, MASK_BYTES);
                throw new IllegalArgumentException("Capability mask length " + length + " exceeds max " + MASK_BYTES);
            }
            if (offset + length > bytes.length) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "voip capability deserialize failed, mask length {0} exceeds remaining bytes", length);
                throw new IllegalArgumentException("Capability mask length " + length + " exceeds remaining bytes");
            }
            if (length > 0) {
                var mask = new byte[MASK_BYTES];
                System.arraycopy(bytes, offset, mask, 0, length);
                capabilities.masks.put(version, mask);
            }
            offset += length;
            version--;
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "deserialized voip capabilities, top_version={0}, bytes={1}", capabilities.topVersion(), bytes.length);
        return capabilities;
    }

    /**
     * Returns the set of named capability constants currently set.
     *
     * <p>Only bits that correspond to a {@link VoipCapability} constant appear; bits set at an
     * index or version with no named constant are omitted.
     *
     * @return the set of named capabilities currently set
     */
    public Set<VoipCapability> namedCapabilities() {
        var result = EnumSet.noneOf(VoipCapability.class);
        for (var capability : VALUES) {
            if (contains(capability)) {
                result.add(capability);
            }
        }
        return result;
    }

    /**
     * Returns the trimmed length of a mask: the index of its highest non zero byte plus one.
     *
     * <p>This is the count of mask bytes the serializer emits for a version, with trailing
     * all zero bytes removed; an all zero mask trims to {@code 0}.
     *
     * @param mask the mask to measure
     * @return the trimmed byte length, {@code 0} through {@link #MASK_BYTES}
     */
    private static int trimmedLength(byte[] mask) {
        for (var i = mask.length - 1; i >= 0; i--) {
            if (mask[i] != 0) {
                return i + 1;
            }
        }
        return 0;
    }

    /**
     * Validates that a bit index is within the representable range.
     *
     * @param index the bit index to validate
     * @throws IllegalArgumentException if {@code index} is outside {@code 0..}{@link VoipCapability#MAX_INDEX}
     */
    private static void checkIndex(int index) {
        if (index < 0 || index > VoipCapability.MAX_INDEX) {
            throw new IllegalArgumentException("Capability index " + index + " out of range 0.." + VoipCapability.MAX_INDEX);
        }
    }

    /**
     * Validates that a version is within the representable range.
     *
     * @param version the version to validate
     * @throws IllegalArgumentException if {@code version} is outside {@code 1..}{@link #MAX_VERSION}
     */
    private static void checkVersion(int version) {
        if (version < 1 || version > MAX_VERSION) {
            throw new IllegalArgumentException("Capability version " + version + " out of range 1.." + MAX_VERSION);
        }
    }

    /**
     * Returns an independent, mutable copy of this capability bitset.
     *
     * <p>Every version mask is copied into a fresh byte array, so mutating the returned bitset through
     * {@link #set(int, int)} or {@link #clear(int, int)} leaves this one unchanged.
     *
     * @return a deep copy of this bitset
     * @implNote This implementation copies the mask map through the {@link TreeMap#TreeMap(java.util.SortedMap)}
     * constructor, which builds a balanced tree in one linear pass from the already sorted entries, then
     * clones each mask array in place and adopts the result through the private constructor. It does not
     * call {@link Object#clone()}, which keeps {@link #masks} {@code final}; a shallow {@link Object#clone()}
     * would also alias the version mask arrays between the two instances.
     */
    @Override
    public VoipCapabilities clone() {
        var copy = new TreeMap<>(masks);
        copy.replaceAll((_, mask) -> mask.clone());
        return new VoipCapabilities(copy);
    }

    /**
     * Returns whether the given object is a capability bitset with identical version masks.
     *
     * <p>Two bitsets are equal when they hold the same versions and each version's mask has the
     * same set bits; a version whose mask is entirely zero is treated as absent for the
     * comparison so it agrees with the {@linkplain #serialize() wire form}.
     *
     * @param other the object to compare against
     * @return {@code true} if {@code other} is an equal capability bitset
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VoipCapabilities that)) {
            return false;
        }
        return Arrays.equals(serialize(), that.serialize());
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code of this bitset's serialized form
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(serialize());
    }

    /**
     * Returns a diagnostic string listing the top version and the set named capabilities.
     *
     * @return a human readable description of this bitset
     */
    @Override
    public String toString() {
        return "VoipCapabilities[topVersion=" + topVersion() + ", capabilities=" + namedCapabilities() + "]";
    }
}
