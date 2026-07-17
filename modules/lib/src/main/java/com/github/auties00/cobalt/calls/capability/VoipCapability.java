package com.github.auties00.cobalt.calls.capability;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Enumerates the individual capability bits a device advertises in its versioned voip
 * capability bitset.
 *
 * <p>Each constant names one bit position inside the per version capability mask carried by
 * the {@code voip_capability} child of the {@code offer}, {@code preaccept}, {@code accept},
 * and {@code link_join} call stanzas. A capability is identified by the pair of a bit
 * {@link #index() index} within the version mask and the {@link #sinceVersion() version} the
 * bit was introduced in. That pair packs into a single value as {@code index | (version << 16)}
 * when a self capability is added, and a peer's advertised bit is tested as
 * {@code (mask[index >> 3] >> (index & 7)) & 1}. The index range is {@code 0} through
 * {@link #MAX_INDEX} inclusive for every version.
 *
 * <p>These constants are the bits that make up the {@linkplain VoipCapabilities#standard()
 * standard self-advertisement}: the twenty two version {@code 1} bits at indices
 * {@code 0, 1, 2, 4, 5, 6, 7, 8, 11, 18, 21, 22, 23, 24, 25, 27, 28, 29, 31, 32, 33, 36}.
 * The semantic meaning of each individual bit (which feature it gates) is not encoded in the
 * capability bitset itself, so each constant is named positionally ({@code BIT_<index>}).
 *
 * <p>The standard advertisement serializes to the byte stream
 * {@snippet lang="text" :
 * 01 05 F7 09 E4 BB 13
 * }
 * where the leading {@code 0x01} is the top version, followed for version {@code 1} by a
 * mask length byte {@code 0x05} and five least significant bit first mask bytes
 * {@code F7 09 E4 BB 13}, which expand to the twenty two set bit indices listed above.
 */
public enum VoipCapability {
    /**
     * Capability bit {@code 0} of version {@code 1} (mask byte {@code 0}, bit {@code 0}).
     */
    BIT_0(0, 1),

    /**
     * Capability bit {@code 1} of version {@code 1} (mask byte {@code 0}, bit {@code 1}).
     */
    BIT_1(1, 1),

    /**
     * Capability bit {@code 2} of version {@code 1} (mask byte {@code 0}, bit {@code 2}).
     */
    BIT_2(2, 1),

    /**
     * Capability bit {@code 4} of version {@code 1} (mask byte {@code 0}, bit {@code 4}).
     */
    BIT_4(4, 1),

    /**
     * Capability bit {@code 5} of version {@code 1} (mask byte {@code 0}, bit {@code 5}).
     */
    BIT_5(5, 1),

    /**
     * Capability bit {@code 6} of version {@code 1} (mask byte {@code 0}, bit {@code 6}).
     */
    BIT_6(6, 1),

    /**
     * Capability bit {@code 7} of version {@code 1} (mask byte {@code 0}, bit {@code 7}).
     */
    BIT_7(7, 1),

    /**
     * Capability bit {@code 8} of version {@code 1} (mask byte {@code 1}, bit {@code 0}).
     */
    BIT_8(8, 1),

    /**
     * Capability bit {@code 11} of version {@code 1} (mask byte {@code 1}, bit {@code 3}).
     */
    BIT_11(11, 1),

    /**
     * Capability bit {@code 18} of version {@code 1} (mask byte {@code 2}, bit {@code 2}).
     */
    BIT_18(18, 1),

    /**
     * Capability bit {@code 21} of version {@code 1} (mask byte {@code 2}, bit {@code 5}).
     */
    BIT_21(21, 1),

    /**
     * Capability bit {@code 22} of version {@code 1} (mask byte {@code 2}, bit {@code 6}).
     */
    BIT_22(22, 1),

    /**
     * Capability bit {@code 23} of version {@code 1} (mask byte {@code 2}, bit {@code 7}).
     */
    BIT_23(23, 1),

    /**
     * Capability bit {@code 24} of version {@code 1} (mask byte {@code 3}, bit {@code 0}).
     */
    BIT_24(24, 1),

    /**
     * Capability bit {@code 25} of version {@code 1} (mask byte {@code 3}, bit {@code 1}).
     */
    BIT_25(25, 1),

    /**
     * Capability bit {@code 27} of version {@code 1} (mask byte {@code 3}, bit {@code 3}).
     */
    BIT_27(27, 1),

    /**
     * Capability bit {@code 28} of version {@code 1} (mask byte {@code 3}, bit {@code 4}).
     */
    BIT_28(28, 1),

    /**
     * Capability bit {@code 29} of version {@code 1} (mask byte {@code 3}, bit {@code 5}).
     */
    BIT_29(29, 1),

    /**
     * Capability bit {@code 31} of version {@code 1} (mask byte {@code 3}, bit {@code 7}).
     */
    BIT_31(31, 1),

    /**
     * Capability bit {@code 32} of version {@code 1} (mask byte {@code 4}, bit {@code 0}).
     */
    BIT_32(32, 1),

    /**
     * Capability bit {@code 33} of version {@code 1} (mask byte {@code 4}, bit {@code 1}).
     */
    BIT_33(33, 1),

    /**
     * Capability bit {@code 36} of version {@code 1} (mask byte {@code 4}, bit {@code 4}).
     */
    BIT_36(36, 1);

    /**
     * Resolves a bit index and version pair to its capability, backing {@link #of(int, int)}.
     *
     * <p>Built once at class initialization from each constant's {@link #index} and {@link #sinceVersion},
     * so a pair resolves to its capability in constant time rather than by scanning {@link #values()}. The
     * two ints are packed into a single {@code long} key as {@code ((long) index << 32) | (version &
     * 0xFFFFFFFFL)}, a bijection over all {@code int} pairs, so distinct pairs never collide.
     */
    private static final Map<Long, VoipCapability> BY_INDEX_AND_VERSION;

    static {
        var byIndexAndVersion = new HashMap<Long, VoipCapability>();
        for (var capability : values()) {
            var key = ((long) capability.index << 32) | (capability.sinceVersion & 0xFFFFFFFFL);
            if (byIndexAndVersion.put(key, capability) != null) {
                throw new AssertionError("Conflict");
            }
        }
        BY_INDEX_AND_VERSION = Map.copyOf(byIndexAndVersion);
    }

    /**
     * The largest capability bit index any version mask can carry.
     *
     * <p>The engine allocates a {@code 0x40}-byte ({@code 512}-bit) mask per version and
     * rejects any index outside {@code 0..0x1ff}, so the highest valid index is {@code 511}.
     */
    public static final int MAX_INDEX = 0x1ff;

    /**
     * The bit index of this capability within its version mask.
     */
    private final int index;

    /**
     * The capability version this bit was introduced in.
     */
    private final int sinceVersion;

    /**
     * Constructs a capability constant bound to its bit index and the version it appears in.
     *
     * @param index        the bit index within the version mask
     * @param sinceVersion the capability version this bit was introduced in
     */
    VoipCapability(int index, int sinceVersion) {
        this.index = index;
        this.sinceVersion = sinceVersion;
    }

    /**
     * Returns the bit index of this capability within its version mask.
     *
     * <p>The index selects mask byte {@code index >> 3} and bit {@code index & 7} (least
     * significant first) when the capability is tested or set; it lies in the inclusive
     * range {@code 0} through {@link #MAX_INDEX}.
     *
     * @return the bit index, {@code 0} through {@link #MAX_INDEX}
     */
    public int index() {
        return index;
    }

    /**
     * Returns the capability version this bit was introduced in.
     *
     * <p>A device only honours a capability bit when it also advertises a top version at
     * least this high; the engine packs the pair as {@code index | (sinceVersion << 16)}.
     *
     * @return the capability version this bit belongs to
     */
    public int sinceVersion() {
        return sinceVersion;
    }

    /**
     * Returns the capability whose {@link #index() index} and {@link #sinceVersion() version}
     * match the given pair.
     *
     * @implNote This implementation resolves through the prebuilt {@link #BY_INDEX_AND_VERSION} map, keyed
     * on the {@code index} and {@code version} pair packed into a single {@code long}, rather than scanning
     * {@link #values()}.
     * @param index   the bit index to resolve
     * @param version the capability version to resolve
     * @return the matching capability, or {@link Optional#empty()} if no constant matches
     */
    public static Optional<VoipCapability> of(int index, int version) {
        var key = ((long) index << 32) | (version & 0xFFFFFFFFL);
        return Optional.ofNullable(BY_INDEX_AND_VERSION.get(key));
    }
}
