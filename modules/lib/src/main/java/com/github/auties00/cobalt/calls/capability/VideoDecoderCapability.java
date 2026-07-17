package com.github.auties00.cobalt.calls.capability;

import java.util.*;

/**
 * Enumerates the video decoder formats a device advertises in its video decode
 * capability descriptor.
 *
 * <p>Each device advertises which video codecs it can decode through a comma separated
 * token string (the {@code vid_dec} attribute) carried alongside its capability bitset
 * and embedded in offer, accept, and group info messages. The engine parses that token
 * string into a codec bitmask, and during negotiation it intersects the local mask with
 * the peer's advertised mask and selects the surviving codec of highest priority. The
 * recognized formats are {@link #H264}, {@link #H265}, {@link #VP8}, {@link #VP9}, and
 * {@link #AV1}.
 *
 * <p>Each constant carries the {@link #token() wire token} used in the descriptor string
 * and a {@link #priority() priority} that orders codecs during negotiation; a higher
 * priority is preferred when more than one codec is common to both sides. When a
 * descriptor string fails to parse, the engine falls back to {@link #H264} only; this
 * enum exposes that behaviour through {@link #parse(String)}, which returns a set
 * containing only {@link #H264} for any unrecognized or malformed input.
 *
 * <p>The bitmask bit assignments are fixed by the wire protocol: each comma separated
 * token contributes exactly one single bit value, {@code H264=0x01}, {@code VP8=0x02},
 * {@code VP9=0x04}, {@code H265=0x08}, {@code AV1=0x10}. Token matching is
 * case insensitive and accepts, alongside each canonical token, the alternate names
 * {@code H.264}/{@code AVC}/{@code H.265}/{@code HEVC} and the numeric ids
 * {@code 1}/{@code 2}/{@code 4}/{@code 8}/{@code 16}, all resolving to the same codec.
 *
 * @implNote The constants are declared in bit order ({@code H264}, {@code VP8},
 * {@code VP9}, {@code H265}, {@code AV1}), which places the middle codecs as
 * {@code VP8,VP9,H265}; this follows the capability bitmask assignment and deliberately
 * does not follow the distinct pixel format descriptor ordering ({@code H264,H265,VP8,VP9}),
 * which does not govern the bit layout.
 */
public enum VideoDecoderCapability {
    /**
     * The H.264 (AVC) decoder, bit {@code 0} (mask {@code 0x01}), the negotiation fallback
     * codec.
     *
     * <p>This is the codec the engine assumes when a capability descriptor fails to
     * parse, so it is treated as universally available.
     */
    H264("H264", 0, 4, "H.264", "AVC", "1"),

    /**
     * The VP8 decoder, bit {@code 1} (mask {@code 0x02}).
     */
    VP8("VP8", 1, 2, "2"),

    /**
     * The VP9 decoder, bit {@code 2} (mask {@code 0x04}).
     */
    VP9("VP9", 2, 1, "4"),

    /**
     * The H.265 (HEVC) decoder, bit {@code 3} (mask {@code 0x08}).
     */
    H265("H265", 3, 3, "H.265", "HEVC", "8"),

    /**
     * The AV1 decoder, bit {@code 4} (mask {@code 0x10}).
     */
    AV1("AV1", 4, 0, "16");

    /**
     * The fallback codec set the engine assumes when a capability descriptor fails to parse, kept as a
     * raw {@link EnumSet} for internal use.
     *
     * <p>Holds only {@link #H264}, the codec treated as universally available. As a raw {@link EnumSet}
     * it is a {@code RegularEnumSet} backed by a single {@code long} bitmask, so feeding it into a bulk
     * set operation ({@link EnumSet#addAll}, {@link EnumSet#retainAll}) stays on the bitwise fast path
     * instead of degrading to element iteration, which an unmodifiable wrapper would force. This
     * instance is never handed to callers; {@link #FALLBACK_UNMODIFIABLE} is exposed in its place.
     */
    private static final Set<VideoDecoderCapability> FALLBACK = EnumSet.of(H264);

    /**
     * The immutable view of {@link #FALLBACK} returned to callers by {@link #parse(String)} on the
     * parse failure path.
     *
     * <p>Wraps {@link #FALLBACK} with {@link Collections#unmodifiableSet(Set)} so the shared fallback
     * instance the parser returns cannot be mutated through the reference callers receive.
     */
    private static final Set<VideoDecoderCapability> FALLBACK_UNMODIFIABLE = Collections.unmodifiableSet(FALLBACK);

    /**
     * Resolves a normalized wire token or alias to its codec, backing {@link #ofToken(String)}.
     *
     * <p>Populated once at class initialization from each constant's {@link #token} and
     * {@link #aliases}, so every accepted spelling maps to its codec in constant time. Keys are
     * lowercased with {@link Locale#ROOT} to keep matching locale independent, preserving the
     * {@link String#equalsIgnoreCase(String)} semantics this lookup replaces (a plain
     * {@link String#toLowerCase()} would fold, for example, {@code "I"} differently under a Turkish
     * locale).
     */
    private static final Map<String, VideoDecoderCapability> BY_TOKEN;

    static {
        var byToken = new HashMap<String, VideoDecoderCapability>();
        for (var codec : values()) {
            if(byToken.put(codec.token.toLowerCase(Locale.ROOT), codec) != null) {
                throw new AssertionError("Conflict");
            }
            for (var alias : codec.aliases) {
                if(byToken.put(alias.toLowerCase(Locale.ROOT), codec) != null) {
                    throw new AssertionError("Conflict");
                }
            }
        }
        BY_TOKEN = Map.copyOf(byToken);
    }

    /**
     * The wire token used to name this codec in a video decode capability descriptor.
     */
    private final String token;

    /**
     * The bit index this codec occupies in the codec bitmask.
     */
    private final int bit;

    /**
     * The negotiation priority of this codec; higher is preferred.
     */
    private final int priority;

    /**
     * The additional inbound only tokens that resolve to this codec, alongside {@link #token}.
     *
     * <p>These are the alternate names and numeric ids the engine accepts case insensitively for the
     * same codec; they are recognized by {@link #ofToken(String)} on the inbound path but are never
     * emitted, since {@link #token()} is the single canonical wire token.
     */
    private final String[] aliases;

    /**
     * Constructs a codec constant bound to its wire token, bitmask bit, negotiation priority, and the
     * additional inbound only token aliases the engine accepts for it.
     *
     * @param token    the canonical wire token emitted in the capability descriptor
     * @param bit      the bitmask bit index
     * @param priority the negotiation priority, higher being preferred
     * @param aliases  the additional inbound only tokens that resolve to this codec
     */
    VideoDecoderCapability(String token, int bit, int priority, String... aliases) {
        this.token = token;
        this.bit = bit;
        this.priority = priority;
        this.aliases = aliases;
    }

    /**
     * Returns the wire token used to name this codec in a video decode capability
     * descriptor.
     *
     * @return the wire token, such as {@code "H264"} or {@code "AV1"}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the bit index this codec occupies in the codec bitmask.
     *
     * <p>The bits are {@code H264=0}, {@code VP8=1}, {@code VP9=2}, {@code H265=3}, and
     * {@code AV1=4}.
     *
     * @return the bitmask bit index, {@code 0} through {@code 4}
     */
    public int bit() {
        return bit;
    }

    /**
     * Returns the bitmask value (a single set bit) this codec contributes to a codec
     * bitmask.
     *
     * <p>The values are {@code H264=0x01}, {@code VP8=0x02}, {@code VP9=0x04},
     * {@code H265=0x08}, and {@code AV1=0x10}.
     *
     * @return {@code 1 << bit()}
     */
    public int mask() {
        return 1 << bit;
    }

    /**
     * Returns the negotiation priority of this codec.
     *
     * <p>When more than one codec is common to both sides, the engine selects the codec
     * with the highest priority.
     *
     * @return the negotiation priority, higher being preferred
     */
    public int priority() {
        return priority;
    }

    /**
     * Returns the codec whose {@linkplain #token() wire token} or alias equals the given token,
     * ignoring case.
     *
     * <p>Matching honours the full token vocabulary accepted case insensitively: the canonical token
     * ({@code H264}, {@code VP8}, {@code VP9}, {@code H265}, {@code AV1}), the alternate names
     * ({@code H.264}, {@code AVC}, {@code H.265}, {@code HEVC}), and the numeric ids ({@code 1},
     * {@code 2}, {@code 4}, {@code 8}, {@code 16}) all resolve to the same codec. The input is trimmed
     * before comparison.
     *
     * @param token the wire token or alias to resolve
     * @return the matching codec, or {@link Optional#empty()} if no codec matches
     */
    public static Optional<VideoDecoderCapability> ofToken(String token) {
        if (token == null) {
            return Optional.empty();
        }
        var normalized = token.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(BY_TOKEN.get(normalized));
    }

    /**
     * Parses a comma separated video decode capability descriptor into a set of codecs.
     *
     * <p>The descriptor is scanned for comma separated tokens, each resolved through
     * {@link #ofToken(String)}, so every token matches case insensitively against a codec's canonical
     * wire token or any of its aliases; unrecognized tokens are ignored. If the input is {@code null},
     * blank, or yields no recognized codec, the result is a set containing only {@link #H264}, matching
     * the engine's parse failure fallback.
     *
     * @param descriptor the comma separated capability descriptor, may be {@code null}
     * @return the parsed set of codecs, unmodifiable and never empty
     * @implNote This implementation walks the descriptor with {@link String#indexOf(int, int)} rather
     * than {@link String#split(String)} to avoid allocating the intermediate token array; only the
     * per-token substrings the map lookup requires are allocated.
     */
    public static Set<VideoDecoderCapability> parse(String descriptor) {
        if (descriptor == null || descriptor.isBlank()) {
            return FALLBACK_UNMODIFIABLE;
        }
        var result = EnumSet.noneOf(VideoDecoderCapability.class);
        var start = 0;
        int comma;
        while ((comma = descriptor.indexOf(',', start)) >= 0) {
            ofToken(descriptor.substring(start, comma)).ifPresent(result::add);
            start = comma + 1;
        }
        ofToken(descriptor.substring(start)).ifPresent(result::add);
        return result.isEmpty() ? FALLBACK_UNMODIFIABLE : Collections.unmodifiableSet(result);
    }

    /**
     * Serializes a set of codecs into a comma separated capability descriptor.
     *
     * <p>Codecs are emitted in descending {@link #priority()} order so the
     * highest priority codec appears first. An empty set serializes to the
     * {@link #H264} token, matching the fallback the engine assumes when no codec is
     * advertised.
     *
     * @param codecs the codecs to serialize
     * @return the comma separated capability descriptor, never empty
     */
    public static String toDescriptor(Set<VideoDecoderCapability> codecs) {
        var ordered = new ArrayList<>(codecs.isEmpty() ? FALLBACK : codecs);
        ordered.sort(Comparator.comparingInt(VideoDecoderCapability::priority).reversed());
        var builder = new StringBuilder();
        for (var codec : ordered) {
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(codec.token);
        }
        return builder.toString();
    }

    /**
     * Returns the codecs common to two advertised capability sets.
     *
     * <p>This is the intersection step of codec negotiation: only codecs both sides can
     * decode survive.
     *
     * @param self the local codec set
     * @param peer the peer's advertised codec set
     * @return the intersection of the two sets, possibly empty
     * @implNote This implementation collects the result into a fresh {@link EnumSet} and narrows it with
     * {@link EnumSet#retainAll(Collection)}; for this small enum both operands are backed by a
     * single {@code long} bitmask, so the intersection resolves to a bitwise AND when the inputs are raw
     * {@link EnumSet}s. The returned set is itself a raw {@link EnumSet}, so downstream set operations
     * (such as {@link #negotiate(Set, Set)}) stay on that fast path.
     */
    public static Set<VideoDecoderCapability> intersect(Set<VideoDecoderCapability> self, Set<VideoDecoderCapability> peer) {
        var result = EnumSet.noneOf(VideoDecoderCapability.class);
        result.addAll(self);
        result.retainAll(peer);
        return result;
    }

    /**
     * Returns the negotiated codec for two advertised capability sets.
     *
     * <p>This combines {@link #intersect(Set, Set)} with the max priority selection: it
     * returns the highest {@link #priority() priority} codec common to both sides, or
     * {@link Optional#empty()} when the two sets share no codec.
     *
     * @param self the local codec set
     * @param peer the peer's advertised codec set
     * @return the negotiated codec, or {@link Optional#empty()} if there is no common
     *         codec
     */
    public static Optional<VideoDecoderCapability> negotiate(Set<VideoDecoderCapability> self, Set<VideoDecoderCapability> peer) {
        return intersect(self, peer)
                .stream()
                .max(Comparator.comparingInt(VideoDecoderCapability::priority));
    }
}
