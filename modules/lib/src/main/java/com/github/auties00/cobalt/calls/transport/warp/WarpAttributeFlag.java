package com.github.auties00.cobalt.calls.transport.warp;

/**
 * Enumerates the WARP message attribute flag bits that select which attributes a WARP media control
 * message carries.
 *
 * <p>A {@link WarpMessage} is a fixed header of five bytes followed by a sequence of attributes
 * selected by the flags. The flags live in one mandatory byte at offset four (carrying bits
 * {@code 0..7}) and one optional extension byte at offset five (carrying bits {@code 8..15}, present
 * only when {@link #EXT_FLAG} is set); offsets zero through three carry the type byte, the packed
 * length byte, and the big endian timestamp {@code u16}. Each constant names one bit, records its
 * numeric mask, and records whether it lives in the base byte or the extension byte. The serializer
 * walks the bits in ascending order and appends each present attribute's payload in that order, so the
 * ordinal of these constants is also the wire append order.
 *
 * <p>Two bits form a pair: {@link #PARTICIPANT_REPORT} ({@code 0x08}) is always set together with its
 * companion {@link #PARTICIPANT_REPORT_COMPANION} ({@code 0x20}), so the participant report appears on
 * the wire as the combined mask {@code 0x28}. {@link #BANDWIDTH_REPORT} ({@code 0x100}) is the only
 * extension byte attribute and is legal only on a standalone (not piggybacked) WARP message; setting it
 * implies {@link #EXT_FLAG}.
 *
 * @implNote This implementation models only the attribute bits the shipped serializer emits:
 *           {@code 0x01}, {@code 0x02}, {@code 0x04}, {@code 0x08}/{@code 0x20}, {@code 0x40}, the
 *           {@code 0x80} extension marker, and the {@code 0x100} extension attribute. Bits {@code 6}
 *           and {@code 7} of the base byte carry no attribute and are skipped by the serializer, so
 *           they are not modeled here.
 */
public enum WarpAttributeFlag {
    /**
     * The sequence number attribute, a big endian {@code u16} always set on a fresh outbound message.
     *
     * <p>It carries the monotonically increasing WARP sequence number taken under the send lock so the
     * peer can detect reordering and loss of control messages.
     */
    SEQUENCE_NUMBER(0x01, false),

    /**
     * The downlink bandwidth attribute, a big endian {@code u16} in kilobits per second.
     *
     * <p>It reports the device's current downlink bandwidth estimate (the engine's downlink bandwidth
     * value divided by one thousand), attached when a recent nonzero sample exists.
     */
    DOWNLINK_BW(0x02, false),

    /**
     * The video encoding attribute, a single byte of direction and encoding flags.
     *
     * <p>Its bits select the active video directions and modes: bit {@code 0} send, bit {@code 1}
     * receive, bit {@code 3} ({@code 0x08}) screen share; attached on a video subscription change.
     */
    VIDEO_ENCODING(0x04, false),

    /**
     * The participant report attribute, a rate control report block of ten bytes.
     *
     * <p>It is always paired with {@link #PARTICIPANT_REPORT_COMPANION} so the two appear together as
     * the combined mask {@code 0x28}; the block is the {@link WarpParticipantReport} payload.
     */
    PARTICIPANT_REPORT(0x08, false),

    /**
     * The companion bit of {@link #PARTICIPANT_REPORT}, carrying no payload of its own.
     *
     * <p>The serializer sets it together with {@link #PARTICIPANT_REPORT}; it has no independent
     * attribute body and exists only so the combined participant report mask is {@code 0x28}.
     */
    PARTICIPANT_REPORT_COMPANION(0x20, false),

    /**
     * The sender bandwidth allocation and SRTP authenticated feedback attribute, a big endian
     * {@code u32}.
     *
     * <p>It carries the value the transport SRTP layer produces for the current RTP index, used by the
     * SFU to authenticate per stream feedback.
     */
    SENDER_BWA(0x40, false),

    /**
     * The extension flag bit marking the presence of the second flag byte for bits {@code 8..15}.
     *
     * <p>It carries no attribute body; it is set whenever any extension byte attribute (currently only
     * {@link #BANDWIDTH_REPORT}) is present so the decoder knows to read the extension byte at offset
     * five.
     */
    EXT_FLAG(0x80, false),

    /**
     * The bandwidth report attribute carried in the extension byte, legal only on a standalone WARP
     * message.
     *
     * <p>It carries the BWE configuration block ({@code version} byte, {@code index} byte,
     * {@code min_remote_bwe} kilobits per second {@code u16}); the serializer rejects it on a
     * piggybacked message. Setting it implies {@link #EXT_FLAG}.
     */
    BANDWIDTH_REPORT(0x100, true);

    /**
     * Caches the constant array so the per message {@link WarpMessage#decode(byte[]) decode} flag walk
     * does not pay the defensive clone cost of {@link #values()} on every WARP message parsed.
     */
    static final WarpAttributeFlag[] VALUES = values();

    /**
     * Holds the numeric bit mask of this flag within the combined sixteen bit flag space.
     */
    private final int mask;

    /**
     * Holds whether this flag lives in the extension byte (bits {@code 8..15}) rather than the base
     * byte (bits {@code 0..7}).
     */
    private final boolean extension;

    /**
     * Constructs a flag bound to its numeric mask and its base or extension placement.
     *
     * @param mask      the numeric bit mask of the flag
     * @param extension whether the flag lives in the extension byte
     */
    WarpAttributeFlag(int mask, boolean extension) {
        this.mask = mask;
        this.extension = extension;
    }

    /**
     * Returns the numeric bit mask of this flag.
     *
     * @return the bit mask, one of {@code 0x01}, {@code 0x02}, {@code 0x04}, {@code 0x08}, {@code 0x20},
     *         {@code 0x40}, {@code 0x80}, or {@code 0x100}
     */
    public int mask() {
        return mask;
    }

    /**
     * Returns whether this flag lives in the extension byte rather than the base byte.
     *
     * @return {@code true} when the flag occupies a bit in {@code 8..15} and therefore requires
     *         {@link #EXT_FLAG}
     */
    public boolean extension() {
        return extension;
    }

    /**
     * Returns whether this flag's bit is set in the given combined flag value.
     *
     * @param flags the combined sixteen bit flag value
     * @return {@code true} when {@code flags} has this flag's {@link #mask() mask} bit set
     */
    public boolean isSet(int flags) {
        return (flags & mask) != 0;
    }
}
