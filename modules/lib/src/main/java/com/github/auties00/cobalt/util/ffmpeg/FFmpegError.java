package com.github.auties00.cobalt.util.ffmpeg;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/**
 * Translates libav* integer return codes into Java exceptions and human-readable messages.
 *
 * <p>libav* follows the convention that any negative return value is an error code, while
 * non-negative values denote success or, for some calls, the number of bytes or frames
 * produced. An error code is either a negated {@code errno} or one of the {@code AVERROR_*}
 * sentinels, and {@code av_strerror(int, char*, size_t)} resolves it to a descriptive string.
 * This class wraps that resolution and the surrounding null- and code-checking patterns that
 * native calls require, raising {@link IllegalStateException} when a call reports failure.
 */
public final class FFmpegError {
    /**
     * Holds the byte capacity of the buffer passed to {@code av_strerror}.
     *
     * @implNote This implementation uses 256 bytes, matching the buffer size FFmpeg's own
     * example programs allocate for error-string lookups.
     */
    private static final int ERROR_BUFFER_SIZE = 256;

    /**
     * Holds the platform's {@code EAGAIN} value, used to recognise libav*'s
     * {@code AVERROR(EAGAIN)} return.
     *
     * <p>libav* signals "send more input before more output can be produced" from
     * {@code avcodec_receive_frame} and {@code avcodec_receive_packet} by returning the
     * negation of the platform {@code EAGAIN}. Because that constant differs across operating
     * systems, the comparison cannot use a single hard-coded number.
     *
     * @implNote This implementation selects 35 on macOS and FreeBSD and 11 on Linux, Windows,
     * and the other BSDs, matching the {@code errno} value each platform assigns to
     * {@code EAGAIN}.
     */
    private static final int EAGAIN_VALUE =
            System.getProperty("os.name", "").toLowerCase().contains("mac") ? 35 : 11;

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always, since the class exposes only static members
     */
    private FFmpegError() {
        throw new AssertionError("FFmpegError is not instantiable");
    }

    /**
     * Returns the given libav* return code unchanged when it is non-negative, throwing otherwise.
     *
     * <p>A negative {@code result} is treated as a libav* error code: the method resolves it
     * through {@link #describe(int)} and raises an {@link IllegalStateException} whose message
     * combines {@code context} with the resolved error string. A non-negative {@code result} is
     * returned as-is so the call site can chain the check inline.
     *
     * @param context human-readable description of the call that produced the result, embedded
     *                in the exception message
     * @param result  the libav* return code
     * @return {@code result} when it is non-negative
     * @throws IllegalStateException if {@code result} is negative
     */
    public static int check(String context, int result) {
        if (result < 0) {
            throw new IllegalStateException(context + " failed: " + describe(result));
        }
        return result;
    }

    /**
     * Returns the descriptive error string for a libav* error code.
     *
     * <p>The method resolves {@code code} through {@code av_strerror} into a confined native
     * buffer and returns the resolved text followed by the numeric code in parentheses. When
     * {@code av_strerror} itself reports failure, the method returns {@code "errno=" + code}
     * instead.
     *
     * @param code the libav* error code
     * @return the resolved error string with the code appended, or {@code "errno=" + code} when
     *         resolution fails
     */
    public static String describe(int code) {
        try (var arena = Arena.ofConfined()) {
            var buf = arena.allocate(ERROR_BUFFER_SIZE);
            if (Ffmpeg.av_strerror(code, buf, ERROR_BUFFER_SIZE) < 0) {
                return "errno=" + code;
            }
            return buf.getString(0L) + " (" + code + ")";
        }
    }

    /**
     * Returns whether a libav* return code is the end-of-stream sentinel.
     *
     * <p>The comparison is against {@code AVERROR_EOF}. Callers use this to map a codec or
     * demuxer drain to the end-of-stream signal expected by the file-backed capture-stream contracts,
     * which expect {@code null} to mark the end of a frame stream.
     *
     * @param code the libav* return code
     * @return {@code true} if {@code code} equals {@code AVERROR_EOF}
     */
    public static boolean isEof(int code) {
        return code == Ffmpeg.AVERROR_EOF();
    }

    /**
     * Returns whether a libav* return code is the {@code AVERROR(EAGAIN)} flow-control signal.
     *
     * <p>The codec pipeline returns this value, the negation of the platform {@code EAGAIN},
     * to indicate that more input must be supplied before further output can be produced. It is
     * a normal control signal rather than a failure. The platform-specific {@code EAGAIN} value
     * used in the comparison is determined once and held in {@link #EAGAIN_VALUE}.
     *
     * @param code the libav* return code
     * @return {@code true} if {@code code} equals {@code -EAGAIN} for this platform
     */
    public static boolean isAgain(int code) {
        return code == -EAGAIN_VALUE;
    }

    /**
     * Returns the given segment unchanged when it points to a non-null native address, throwing otherwise.
     *
     * <p>libav* allocators report failure by returning a null pointer without setting an
     * {@code errno} or raising any other signal. This method treats both a {@code null}
     * reference and a {@link MemorySegment} whose address is zero as failure, raising an
     * {@link IllegalStateException} whose message embeds {@code context}; a valid pointer is
     * returned as-is so the call site can chain the check inline.
     *
     * @param context human-readable description of the call that produced the pointer, embedded
     *                in the exception message
     * @param ptr     the segment to check
     * @return {@code ptr} when it points to a non-null native address
     * @throws IllegalStateException if {@code ptr} is {@code null} or has a zero address
     */
    public static MemorySegment requireNonNull(String context, MemorySegment ptr) {
        if (ptr == null || ptr.address() == 0L) {
            throw new IllegalStateException(context + " returned null");
        }
        return ptr;
    }
}
