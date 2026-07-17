package com.github.auties00.cobalt.registration.push.apns.plist.binary;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistArrayValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistBooleanValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDataValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDateValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistDictionaryValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistFloatingPointValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistIntegerValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistStringValue;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistValue;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Parses Apple's {@code bplist00} binary property-list format into a {@link PlistValue} tree.
 *
 * <p>A binary plist closes with a fixed 32-byte trailer carrying the offset-table location, the
 * number of objects, the index of the top-level object, and the byte widths used for offset-table
 * entries and inter-object references. Every object is identified by a single marker byte whose high
 * nibble selects the type and whose low nibble carries either an inline count or the {@code 1 << n}
 * byte width of the encoded scalar. The parser reads the trailer once during construction and then
 * decodes objects on demand, following the inter-object reference chains from the top object
 * downward. Strings are materialised into Java {@link String}s and {@code <data>} payloads are
 * exposed as offset/length slices over the source buffer via {@link PlistDataValue}; both arrays and
 * dictionaries resolve their children through the object table rather than recursing on adjacent
 * bytes. This is the binary half of the format-detecting facade reached through
 * {@link com.github.auties00.cobalt.registration.push.apns.plist.Plist#parse(byte[])}.
 *
 * @implNote This implementation exposes {@code <data>} payloads as zero-copy slices over the source
 *           buffer rather than copying them, because the APNS payloads decoded here are read once and
 *           never outlive the source array; strings, by contrast, must be allocated since the JVM has
 *           no zero-copy view over arbitrary-encoding bytes.
 */
public final class PlistBinaryParser {
    /**
     * The logger for {@link PlistBinaryParser}.
     */
    private static final System.Logger LOGGER = Log.get(PlistBinaryParser.class);

    /**
     * Holds the byte length of the trailer that closes every binary plist.
     *
     * @implNote This implementation hard-codes 32 to match the fixed trailer layout defined by
     *           Apple's {@code CFBinaryPList.c}: 5 unused bytes, a 1-byte sort-version, a 1-byte
     *           offset width, a 1-byte reference width, and three 8-byte fields.
     */
    private static final int TRAILER_SIZE = 32;

    /**
     * Holds the magic header bytes that identify a binary plist.
     *
     * <p>Declared package-private so {@link PlistBinaryWriter} can emit the same prefix and
     * {@link #isBinary(byte[])} can probe an input for it.
     */
    static final byte[] MAGIC = {'b', 'p', 'l', 'i', 's', 't', '0', '0'};

    /**
     * Holds the source bytes the parser decodes.
     */
    private final byte[] src;

    /**
     * Holds the width in bytes of each offset-table entry.
     */
    private final int offsetSize;

    /**
     * Holds the width in bytes of each inter-object reference.
     */
    private final int refSize;

    /**
     * Holds the index of the top-level object within the object table.
     */
    private final int topObject;

    /**
     * Holds the absolute offset of the offset table within {@link #src}.
     */
    private final int offsetTableOffset;

    /**
     * Binds a parser to the source bytes and reads the trailer.
     *
     * <p>Reads the offset width, reference width, object count, top-object index, and offset-table
     * location from the trailer, validates that both widths fall in the inclusive range {@code [1, 8]},
     * and bounds-checks that the offset table lies wholly within the source. Callers route through
     * {@link #parse(byte[])} rather than constructing the parser directly.
     *
     * @implNote This implementation reads the trailer fields at fixed offsets relative to
     *           {@code src.length - TRAILER_SIZE} per Apple's {@code CFBinaryPList.c}: the offset
     *           width at byte 6, the reference width at byte 7, the object count at byte 8, the
     *           top-object index at byte 16, and the offset-table location at byte 24.
     * @param src the source bytes
     * @throws IOException if the source is too short to hold a trailer, the offset or reference widths
     *                     are out of range, or the offset table escapes the source
     */
    private PlistBinaryParser(byte[] src) throws IOException {
        if (src.length < MAGIC.length + TRAILER_SIZE) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "binary plist too short, bytes={0}", src.length);
            throw new IOException("binary plist too short: " + src.length);
        }
        this.src = src;
        var trailer = src.length - TRAILER_SIZE;
        this.offsetSize = src[trailer + 6] & 0xFF;
        this.refSize = src[trailer + 7] & 0xFF;
        if (offsetSize < 1 || offsetSize > 8 || refSize < 1 || refSize > 8) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "invalid plist trailer widths, offset={0}, ref={1}", offsetSize, refSize);
            throw new IOException("invalid trailer widths: offset=" + offsetSize + " ref=" + refSize);
        }
        var numObjects = readUnsignedBigEndian(trailer + 8, 8);
        this.topObject = (int) readUnsignedBigEndian(trailer + 16, 8);
        this.offsetTableOffset = (int) readUnsignedBigEndian(trailer + 24, 8);
        if (offsetTableOffset < 0 || offsetTableOffset > src.length
                || (long) offsetTableOffset + numObjects * offsetSize > src.length) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "plist offset table escapes source, offset={0}, objects={1}", offsetTableOffset, numObjects);
            throw new IOException("offset table escapes source: at " + offsetTableOffset
                    + " for " + numObjects + " objects of " + offsetSize + " bytes");
        }
    }

    /**
     * Reports whether the source bytes start with the {@code bplist00} magic.
     *
     * <p>Returns {@code false} for any input shorter than the magic prefix, allowing the
     * format-detecting facade {@link com.github.auties00.cobalt.registration.push.apns.plist.Plist#parse(byte[])}
     * to dispatch between the binary and XML parsers without throwing.
     *
     * @param data the source bytes
     * @return {@code true} if {@code data} starts with the binary plist magic
     */
    public static boolean isBinary(byte[] data) {
        if (data.length < MAGIC.length) {
            return false;
        }
        for (var i = 0; i < MAGIC.length; i++) {
            if (data[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses the source bytes into a {@link PlistValue} tree.
     *
     * <p>Constructs an internal parser instance, which reads and validates the trailer, then decodes
     * the object at the top-object index and returns it as the root of the tree.
     *
     * @param data the binary plist bytes
     * @return the parsed root value
     * @throws IOException if the source is malformed
     */
    public static PlistValue parse(byte[] data) throws IOException {
        var parser = new PlistBinaryParser(data);
        return parser.readObject(parser.topObject);
    }

    /**
     * Resolves the on-disk offset of an object through the offset table and decodes it.
     *
     * <p>Looks up the offset-table entry for {@code index}, reads the absolute object offset it holds,
     * and decodes the object found there. This is the indirection the array and dictionary decoders use
     * to follow inter-object references.
     *
     * @param index the index into the object table
     * @return the decoded value
     * @throws IOException if the marker is unknown or the encoded payload escapes the source
     */
    private PlistValue readObject(int index) throws IOException {
        var entry = offsetTableOffset + index * offsetSize;
        var offset = (int) readUnsignedBigEndian(entry, offsetSize);
        return readObjectAt(offset);
    }

    /**
     * Decodes the object starting at the given absolute offset.
     *
     * <p>Dispatches on the high nibble of the marker byte: {@code 0x0} for the boolean singletons,
     * {@code 0x1} for integers, {@code 0x2} for reals, {@code 0x3} for dates, {@code 0x4} for data,
     * {@code 0x5}/{@code 0x6}/{@code 0x7} for ASCII/UTF-16BE/UTF-8 strings, {@code 0xA} for arrays, and
     * {@code 0xD} for dictionaries. Within the {@code 0x0} family the low nibble {@code 0x8} decodes
     * {@code false} and {@code 0x9} decodes {@code true}. Any other type or singleton nibble is
     * rejected.
     *
     * @param offset the absolute offset within {@link #src}
     * @return the decoded value
     * @throws IOException if the marker is unknown or the encoded payload escapes the source
     */
    private PlistValue readObjectAt(int offset) throws IOException {
        var marker = src[offset] & 0xFF;
        var type = marker >>> 4;
        var info = marker & 0x0F;
        return switch (type) {
            case 0x0 -> switch (info) {
                case 0x8 -> new PlistBooleanValue(false);
                case 0x9 -> new PlistBooleanValue(true);
                default -> {
                    if (Log.WARNING) LOGGER.log(Level.WARNING, "unsupported plist singleton, info=0x{0}", Integer.toHexString(info));
                    throw new IOException("unsupported singleton 0x0/" + info);
                }
            };
            case 0x1 -> readInteger(offset, info);
            case 0x2 -> readReal(offset, info);
            case 0x3 -> readDate(offset);
            case 0x4 -> readData(offset, info);
            case 0x5 -> readString(offset, info, StandardCharsets.US_ASCII, 1);
            case 0x6 -> readString(offset, info, StandardCharsets.UTF_16BE, 2);
            case 0x7 -> readString(offset, info, StandardCharsets.UTF_8, 1);
            case 0xA -> readArray(offset, info);
            case 0xD -> readDict(offset, info);
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unsupported plist marker, tag=0x{0}", Integer.toHexString(marker));
                throw new IOException("unsupported plist marker 0x" + Integer.toHexString(marker));
            }
        };
    }

    /**
     * Decodes a {@code 0x1n} integer marker into a {@link PlistIntegerValue}.
     *
     * <p>The low nibble {@code info} gives the byte count as {@code 1 << info}. Widths of 1, 2, and 4
     * bytes are read unsigned and an 8-byte width is read as signed two's-complement; the 16-byte
     * (uint128) width is rejected.
     *
     * @implNote This implementation rejects the 16-byte width because Cobalt has no 128-bit integer
     *           type to surface it through {@link PlistIntegerValue}, whose component is a {@code long}.
     * @param offset the marker offset
     * @param info   the low nibble of the marker, where {@code 1 << info} is the byte count
     * @return the integer value
     * @throws IOException if the encoded width exceeds 8 bytes
     */
    private PlistIntegerValue readInteger(int offset, int info) throws IOException {
        var byteCount = 1 << info;
        if (byteCount > 8) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "16-byte plist integer not supported");
            throw new IOException("16-byte plist integer not supported");
        }
        return new PlistIntegerValue(readUnsignedBigEndian(offset + 1, byteCount));
    }

    /**
     * Decodes a {@code 0x2n} floating-point marker into a {@link PlistFloatingPointValue}.
     *
     * <p>The low nibble {@code info} gives the byte count as {@code 1 << info}. A 4-byte width decodes
     * an IEEE-754 {@code float32} and an 8-byte width decodes a {@code float64}; any other width is
     * rejected.
     *
     * @implNote This implementation reinterprets the big-endian bit pattern with
     *           {@link Float#intBitsToFloat(int)} and {@link Double#longBitsToDouble(long)} rather than
     *           computing the value arithmetically, matching how Apple stores reals as raw IEEE-754 bits.
     * @param offset the marker offset
     * @param info   the low nibble of the marker, where {@code 1 << info} is the byte count
     * @return the floating-point value
     * @throws IOException if the encoded width is neither 4 nor 8
     */
    private PlistFloatingPointValue readReal(int offset, int info) throws IOException {
        var byteCount = 1 << info;
        return switch (byteCount) {
            case 4 -> new PlistFloatingPointValue(Float.intBitsToFloat((int) readUnsignedBigEndian(offset + 1, 4)));
            case 8 -> new PlistFloatingPointValue(Double.longBitsToDouble(readUnsignedBigEndian(offset + 1, 8)));
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unsupported plist real width, bytes={0}", byteCount);
                throw new IOException("unsupported real width: " + byteCount);
            }
        };
    }

    /**
     * Decodes a {@code 0x33} date marker into a {@link PlistDateValue}.
     *
     * <p>A date is stored as an 8-byte IEEE-754 double holding seconds (including a fractional part)
     * relative to the Apple reference date 2001-01-01T00:00:00Z. The result is an {@link Instant}
     * rebased onto the Unix epoch.
     *
     * @implNote This implementation adds {@code 978307200} seconds, the difference between the Unix
     *           epoch and the Apple reference date, and carries the fractional seconds through as nanos.
     * @param offset the marker offset
     * @return the date value
     */
    private PlistDateValue readDate(int offset) {
        var seconds = Double.longBitsToDouble(readUnsignedBigEndian(offset + 1, 8));
        var whole = (long) Math.floor(seconds);
        var nanos = (long) ((seconds - whole) * 1_000_000_000L);
        return new PlistDateValue(Instant.ofEpochSecond(whole + 978_307_200L, nanos));
    }

    /**
     * Decodes a {@code 0x4n} data marker into a {@link PlistDataValue}.
     *
     * <p>Resolves the payload length (inline when {@code info} is below {@code 0xF}, otherwise from the
     * extended-length marker that follows) and returns a slice over {@link #src}. The returned value
     * shares the source buffer; callers that need an isolated copy call {@link PlistDataValue#toByteArray()}.
     *
     * @param offset the marker offset
     * @param info   the inline length, or {@code 0xF} when an extended length follows the marker
     * @return the data value
     * @throws IOException if the slice escapes the source
     */
    private PlistDataValue readData(int offset, int info) throws IOException {
        var span = readLength(offset, info);
        return new PlistDataValue(src, span.dataOffset(), span.length());
    }

    /**
     * Decodes a {@code 0x5n}, {@code 0x6n}, or {@code 0x7n} string marker into a {@link PlistStringValue}.
     *
     * <p>The resolved length is a character count; multiplying it by {@code bytesPerUnit} yields the
     * byte length of the payload, which is decoded with {@code charset}. The {@code 0x5} family is
     * ASCII and {@code 0x7} is UTF-8 (one byte per code unit), while {@code 0x6} is UTF-16BE (two bytes
     * per code unit). The decoded text is always returned as a Java {@link String}.
     *
     * @param offset       the marker offset
     * @param info         the inline character count, or {@code 0xF} for an extended count
     * @param charset      the source encoding
     * @param bytesPerUnit the source bytes per code unit
     * @return the string value
     * @throws IOException if the slice escapes the source
     */
    private PlistStringValue readString(int offset, int info, Charset charset, int bytesPerUnit) throws IOException {
        var span = readLength(offset, info);
        var byteLength = span.length() * bytesPerUnit;
        return new PlistStringValue(new String(src, span.dataOffset(), byteLength, charset));
    }

    /**
     * Decodes a {@code 0xAn} array marker into a {@link PlistArrayValue}.
     *
     * <p>The resolved length is the element count, and the payload is that many inter-object references
     * laid back-to-back, each {@link #refSize} bytes wide. Each reference is resolved through
     * {@link #readObject(int)} and the decoded values are collected in order.
     *
     * @param offset the marker offset
     * @param info   the inline element count, or {@code 0xF} for an extended count
     * @return the array value
     * @throws IOException if any element fails to decode
     */
    private PlistArrayValue readArray(int offset, int info) throws IOException {
        var span = readLength(offset, info);
        var count = span.length();
        var items = new ArrayList<PlistValue>(count);
        for (var i = 0; i < count; i++) {
            var ref = (int) readUnsignedBigEndian(span.dataOffset() + i * refSize, refSize);
            items.add(readObject(ref));
        }
        return new PlistArrayValue(items);
    }

    /**
     * Decodes a {@code 0xDn} dictionary marker into a {@link PlistDictionaryValue}.
     *
     * <p>The resolved length is the entry count, and the payload is that many key references followed
     * by the same number of value references, all contiguous. Each key reference must resolve to a
     * {@link PlistStringValue}, whose decoded text becomes the map key; the matching value reference is
     * resolved to the entry value.
     *
     * @implNote This implementation backs the entries with a {@link LinkedHashMap} so the on-disk entry
     *           order is preserved for downstream callers that iterate the dictionary.
     * @param offset the marker offset
     * @param info   the inline entry count, or {@code 0xF} for an extended count
     * @return the dictionary value
     * @throws IOException if any key fails to decode as a string
     */
    private PlistDictionaryValue readDict(int offset, int info) throws IOException {
        var span = readLength(offset, info);
        var count = span.length();
        var keysOffset = span.dataOffset();
        var valuesOffset = keysOffset + count * refSize;
        var entries = new LinkedHashMap<String, PlistValue>(Math.max(count * 2, 4));
        for (var i = 0; i < count; i++) {
            var keyRef = (int) readUnsignedBigEndian(keysOffset + i * refSize, refSize);
            var valueRef = (int) readUnsignedBigEndian(valuesOffset + i * refSize, refSize);
            var key = readObject(keyRef);
            if (!(key instanceof PlistStringValue s)) {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "plist dictionary key is not a string, type={0}", key.getClass().getSimpleName());
                throw new IOException("dictionary key is not a string: " + key);
            }
            entries.put(s.value(), readObject(valueRef));
        }
        return new PlistDictionaryValue(entries);
    }

    /**
     * Resolves the count and the start offset of the payload for a variable-length marker.
     *
     * <p>When the marker's low nibble {@code info} is below {@code 0xF} the count is inline and the
     * payload begins immediately after the marker byte. Otherwise the byte after the marker is itself an
     * integer marker ({@code 0x1n}) whose payload carries the real count, and the payload begins after
     * that integer.
     *
     * @param markerOffset the offset of the original marker
     * @param info         the low nibble of the original marker
     * @return the resolved length and payload start offset
     * @throws IOException if the extended-length marker is not an integer or its width exceeds 8 bytes
     */
    private LengthSpan readLength(int markerOffset, int info) throws IOException {
        if (info < 0xF) {
            return new LengthSpan(info, markerOffset + 1);
        }
        var nextMarker = src[markerOffset + 1] & 0xFF;
        if ((nextMarker >>> 4) != 0x1) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "expected integer marker for extended plist length, marker=0x{0}", Integer.toHexString(nextMarker));
            throw new IOException("expected integer marker for extended length, got 0x"
                    + Integer.toHexString(nextMarker));
        }
        var byteCount = 1 << (nextMarker & 0x0F);
        if (byteCount > 8) {
            if (Log.WARNING) LOGGER.log(Level.WARNING, "extended plist length exceeds 8 bytes, bytes={0}", byteCount);
            throw new IOException("extended length exceeds 8 bytes");
        }
        var length = (int) readUnsignedBigEndian(markerOffset + 2, byteCount);
        return new LengthSpan(length, markerOffset + 2 + byteCount);
    }

    /**
     * Reads {@code byteCount} big-endian bytes starting at {@code offset} as a {@code long}.
     *
     * <p>Serves every multi-byte field the format defines: offset-table entries, inter-object
     * references, extended lengths, and integer payloads.
     *
     * @implNote This implementation lets the left-shift accumulation overflow naturally, which yields
     *           the correct two's-complement value when {@code byteCount} is 8 and the high bit is set,
     *           matching how Apple encodes signed 8-byte integers.
     * @param offset    the start offset
     * @param byteCount the number of bytes from 1 to 8
     * @return the assembled value
     */
    private long readUnsignedBigEndian(int offset, int byteCount) {
        long value = 0;
        for (var i = 0; i < byteCount; i++) {
            value = (value << 8) | (src[offset + i] & 0xFFL);
        }
        return value;
    }

    /**
     * Pairs a resolved count with the offset of the first payload byte.
     *
     * <p>Returned by {@link #readLength(int, int)} so the variable-length decoders know both how many
     * elements or bytes follow and where they begin.
     *
     * @param length     the count or byte length
     * @param dataOffset the offset of the first payload byte
     */
    private record LengthSpan(int length, int dataOffset) {
    }
}
