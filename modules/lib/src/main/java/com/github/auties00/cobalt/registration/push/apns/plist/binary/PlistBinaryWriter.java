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

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Serialises a {@link PlistValue} tree into Apple's {@code bplist00} binary property-list format.
 *
 * <p>The emitted layout is the on-wire shape Apple's parsers accept: an 8-byte magic header, an object
 * table laid out in depth-first order, an offset table whose entry width depends on the location of the
 * offset table itself, and a fixed 32-byte trailer pointing at the top object. Serialisation runs in
 * passes. A collection pass walks the tree once to enumerate every object, synthesising one
 * {@link PlistStringValue} per dictionary key because dictionary keys occupy the object table just like
 * values. A sizing pass measures each object given the now-known reference width and accumulates
 * per-object offsets, which locates the offset table and fixes the offset width. A single
 * {@code byte[]} of the exact total size is then allocated and filled in place: the object payloads,
 * the offset table, and the trailer. No object deduplication is performed, so equal-but-distinct values
 * receive separate slots, which Apple's parsers accept. This is the binary half of the facade reached
 * through {@link com.github.auties00.cobalt.registration.push.apns.plist.Plist#writeBinary(PlistValue)}.
 *
 * @implNote This implementation derives the offset width only after the object payloads are sized,
 *           because the trailer's offset-width field must accommodate the offset-table location, which
 *           is itself the sum of the magic length and every object's size; this ordering matches Apple's
 *           own writer.
 */
public final class PlistBinaryWriter {
    /**
     * The logger for {@link PlistBinaryWriter}.
     */
    private static final System.Logger LOGGER = Log.get(PlistBinaryWriter.class);

    /**
     * Holds the byte length of the trailer that closes every binary plist.
     *
     * @implNote This implementation hard-codes 32 to match the fixed trailer layout defined by
     *           Apple's {@code CFBinaryPList.c}: 5 unused bytes, a 1-byte sort-version, a 1-byte
     *           offset width, a 1-byte reference width, and three 8-byte fields.
     */
    private static final int TRAILER_SIZE = 32;

    /**
     * Prevents instantiation of this stateless namespace.
     */
    private PlistBinaryWriter() {
    }

    /**
     * Serialises a value tree as a {@code bplist00} binary plist.
     *
     * <p>Collects the tree into a depth-first object table, picks the reference width from the largest
     * object index, sizes every object and records its offset, locates the offset table after the last
     * object, picks the offset width from that location, allocates the exact total buffer, then writes
     * the magic header, the encoded objects, the offset table, and the trailer. The trailer records the
     * offset width at byte 6, the reference width at byte 7, the object count at byte 8, the top-object
     * index (always 0, the root) at byte 16, and the offset-table location at byte 24.
     *
     * @param root the root value
     * @return the binary plist bytes
     */
    public static byte[] write(PlistValue root) {
        var ctx = new Context();
        ctx.collect(root);
        var numObjects = ctx.objects.size();
        var refSize = byteCountFor(numObjects - 1);

        var offsets = new int[numObjects];
        var objectTableSize = 0;
        for (var i = 0; i < numObjects; i++) {
            offsets[i] = PlistBinaryParser.MAGIC.length + objectTableSize;
            objectTableSize += sizeOf(ctx.objects.get(i), refSize);
        }

        var offsetTableOffset = PlistBinaryParser.MAGIC.length + objectTableSize;
        var offsetSize = byteCountFor(offsetTableOffset);
        var trailerOffset = offsetTableOffset + numObjects * offsetSize;
        var totalSize = trailerOffset + TRAILER_SIZE;

        var out = new byte[totalSize];
        System.arraycopy(PlistBinaryParser.MAGIC, 0, out, 0, PlistBinaryParser.MAGIC.length);

        var pos = PlistBinaryParser.MAGIC.length;
        for (var i = 0; i < numObjects; i++) {
            pos = encodeObject(out, pos, ctx.objects.get(i), refSize, ctx);
        }

        for (var i = 0; i < numObjects; i++) {
            writeBigEndian(out, offsetTableOffset + i * offsetSize, offsets[i], offsetSize);
        }

        out[trailerOffset + 6] = (byte) offsetSize;
        out[trailerOffset + 7] = (byte) refSize;
        writeBigEndian(out, trailerOffset + 8, numObjects, 8);
        writeBigEndian(out, trailerOffset + 16, 0L, 8);
        writeBigEndian(out, trailerOffset + 24, offsetTableOffset, 8);

        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "encoded binary plist, objects={0}, bytes={1}", numObjects, totalSize);
        return out;
    }

    /**
     * Returns the encoded byte length of a value.
     *
     * <p>Accounts for the marker byte, any extended-length suffix when the count reaches {@code 0xF},
     * and either the scalar payload (for primitives) or {@code refSize} bytes per child reference (for
     * arrays and dictionaries, the latter holding one reference per key plus one per value). Drives the
     * object-table layout in {@link #write(PlistValue)} before any encode call runs.
     *
     * @param v       the value
     * @param refSize the inter-object reference width
     * @return the byte count
     */
    private static int sizeOf(PlistValue v, int refSize) {
        return switch (v) {
            case PlistBooleanValue b -> 1;
            case PlistIntegerValue i -> 1 + integerWidth(i.value());
            case PlistFloatingPointValue r -> 1 + 8;
            case PlistDateValue d -> 1 + 8;
            case PlistDataValue d -> 1 + extendedLengthBytes(d.length()) + d.length();
            case PlistStringValue s -> {
                var charCount = s.value().length();
                var payloadBytes = isAscii(s.value()) ? charCount : charCount * 2;
                yield 1 + extendedLengthBytes(charCount) + payloadBytes;
            }
            case PlistArrayValue a -> 1 + extendedLengthBytes(a.items().size()) + a.items().size() * refSize;
            case PlistDictionaryValue d -> 1 + extendedLengthBytes(d.entries().size()) + 2 * d.entries().size() * refSize;
        };
    }

    /**
     * Encodes one object into the destination buffer at the given position.
     *
     * <p>Dispatches on the sealed {@link PlistValue} hierarchy. Booleans become the {@code 0x08} or
     * {@code 0x09} singleton byte; integers a {@code 0x1n} marker plus a big-endian payload of the
     * minimum width; reals a {@code 0x23} marker plus an 8-byte IEEE-754 double; dates a {@code 0x33}
     * marker plus an 8-byte double rebased onto the Apple reference date; data a {@code 0x4n} marker
     * plus the raw bytes; strings a {@code 0x5n} (ASCII, one byte per character) or {@code 0x6n}
     * (UTF-16BE, two bytes per character) marker plus the encoded text; arrays a {@code 0xAn} marker
     * plus one reference per item; and dictionaries a {@code 0xDn} marker plus all key references
     * followed by all value references. Child references are resolved through {@code ctx}, and
     * dictionary keys are read from the synthesised key list recorded during collection so the
     * keys-then-values layout matches the entry iteration order.
     *
     * @implNote This implementation rebases a date by subtracting {@code 978307200} seconds, the
     *           difference between the Unix epoch and the Apple reference date, and adding the
     *           fractional nanos as {@code nano / 1e9}.
     * @param out     the destination buffer
     * @param pos     the current write position
     * @param v       the object to encode
     * @param refSize the inter-object reference width
     * @param ctx     the collection context resolving child references
     * @return the position after the object
     */
    private static int encodeObject(byte[] out, int pos, PlistValue v, int refSize, Context ctx) {
        return switch (v) {
            case PlistBooleanValue b -> {
                out[pos++] = (byte) (b.value() ? 0x09 : 0x08);
                yield pos;
            }
            case PlistIntegerValue i -> {
                var width = integerWidth(i.value());
                out[pos++] = (byte) (0x10 | Integer.numberOfTrailingZeros(width));
                writeBigEndian(out, pos, i.value(), width);
                yield pos + width;
            }
            case PlistFloatingPointValue r -> {
                out[pos++] = 0x23;
                writeBigEndian(out, pos, Double.doubleToLongBits(r.value()), 8);
                yield pos + 8;
            }
            case PlistDateValue d -> {
                out[pos++] = 0x33;
                var seconds = (d.value().getEpochSecond() - 978_307_200L) + d.value().getNano() / 1e9;
                writeBigEndian(out, pos, Double.doubleToLongBits(seconds), 8);
                yield pos + 8;
            }
            case PlistDataValue d -> {
                pos = writeMarkerWithLength(out, pos, 0x40, d.length());
                System.arraycopy(d.source(), d.offset(), out, pos, d.length());
                yield pos + d.length();
            }
            case PlistStringValue s -> {
                var value = s.value();
                var charCount = value.length();
                if (isAscii(value)) {
                    pos = writeMarkerWithLength(out, pos, 0x50, charCount);
                    for (var i = 0; i < charCount; i++) {
                        out[pos++] = (byte) value.charAt(i);
                    }
                } else {
                    pos = writeMarkerWithLength(out, pos, 0x60, charCount);
                    for (var i = 0; i < charCount; i++) {
                        var c = value.charAt(i);
                        out[pos++] = (byte) (c >> 8);
                        out[pos++] = (byte) c;
                    }
                }
                yield pos;
            }
            case PlistArrayValue a -> {
                pos = writeMarkerWithLength(out, pos, 0xA0, a.items().size());
                for (var item : a.items()) {
                    writeBigEndian(out, pos, ctx.indices.get(item), refSize);
                    pos += refSize;
                }
                yield pos;
            }
            case PlistDictionaryValue d -> {
                pos = writeMarkerWithLength(out, pos, 0xD0, d.entries().size());
                var keys = ctx.dictKeys.get(d);
                for (var key : keys) {
                    writeBigEndian(out, pos, ctx.indices.get(key), refSize);
                    pos += refSize;
                }
                for (var entry : d.entries().entrySet()) {
                    writeBigEndian(out, pos, ctx.indices.get(entry.getValue()), refSize);
                    pos += refSize;
                }
                yield pos;
            }
        };
    }

    /**
     * Writes a marker byte with either an inline or an extended count.
     *
     * <p>Serves every container, data, and string marker. When {@code count} is below 15 it is encoded
     * in the low nibble of the marker byte. Otherwise the marker carries the extension nibble
     * {@code 0xF} and is followed by an integer marker ({@code 0x1n}) whose big-endian payload holds the
     * real count.
     *
     * @param out        the destination buffer
     * @param pos        the current write position
     * @param markerBase the high nibble of the marker, for example {@code 0x40} for data or
     *                   {@code 0xA0} for array
     * @param count      the count to encode
     * @return the position after the marker
     */
    private static int writeMarkerWithLength(byte[] out, int pos, int markerBase, int count) {
        if (count < 0xF) {
            out[pos++] = (byte) (markerBase | count);
            return pos;
        }
        out[pos++] = (byte) (markerBase | 0x0F);
        var width = integerWidth(count);
        out[pos++] = (byte) (0x10 | Integer.numberOfTrailingZeros(width));
        writeBigEndian(out, pos, count, width);
        return pos + width;
    }

    /**
     * Returns the smallest power-of-two byte width that holds an integer value.
     *
     * <p>Returns 8 for any negative value, and 1, 2, 4, or 8 for non-negative values according to the
     * highest set byte.
     *
     * @implNote This implementation returns 8 for negative values because Apple's spec encodes 8-byte
     *           integers as signed two's complement, which is the only width that can represent a
     *           negative integer.
     * @param value the integer value
     * @return the width in bytes (1, 2, 4, or 8)
     */
    private static int integerWidth(long value) {
        if (value < 0) {
            return 8;
        }
        if (value <= 0xFFL) {
            return 1;
        }
        if (value <= 0xFFFFL) {
            return 2;
        }
        if (value <= 0xFFFF_FFFFL) {
            return 4;
        }
        return 8;
    }

    /**
     * Returns the byte length of the extended-length suffix for a count.
     *
     * <p>Returns zero when the count fits in a marker's low nibble (below {@code 0xF}); otherwise
     * returns one byte for the integer marker plus the integer payload's own width. Used by
     * {@link #sizeOf(PlistValue, int)} to size container, data, and string markers.
     *
     * @param count the count
     * @return the suffix size in bytes
     */
    private static int extendedLengthBytes(int count) {
        if (count < 0xF) {
            return 0;
        }
        return 1 + integerWidth(count);
    }

    /**
     * Returns the smallest power-of-two byte width sufficient to encode a value unsigned.
     *
     * <p>Picks the reference width (sized to the largest object index) and the offset width (sized to
     * the offset-table location). Returns 8 for any negative value and 1, 2, 4, or 8 otherwise.
     *
     * @param maxValue the largest value to be stored
     * @return the width in bytes (1, 2, 4, or 8)
     */
    private static int byteCountFor(long maxValue) {
        if (maxValue < 0) {
            return 8;
        }
        if (maxValue <= 0xFFL) {
            return 1;
        }
        if (maxValue <= 0xFFFFL) {
            return 2;
        }
        if (maxValue <= 0xFFFF_FFFFL) {
            return 4;
        }
        return 8;
    }

    /**
     * Reports whether every character of a string lies in the ASCII range.
     *
     * <p>Determines whether a string can be emitted with the {@code 0x5n} ASCII marker (one byte per
     * code unit) or must use the {@code 0x6n} UTF-16BE marker (two bytes per code unit).
     *
     * @param s the string
     * @return {@code true} if every character is below {@code U+0080}
     */
    private static boolean isAscii(String s) {
        for (var i = 0; i < s.length(); i++) {
            if (s.charAt(i) >= 0x80) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a value as a fixed-width big-endian integer.
     *
     * <p>Emits the most-significant byte first across {@code byteCount} bytes, serving every multi-byte
     * field the format produces: offset-table entries, inter-object references, scalar integers, and the
     * trailer fields.
     *
     * @param out       the destination buffer
     * @param pos       the start offset
     * @param value     the value to write
     * @param byteCount the byte width from 1 to 8
     */
    private static void writeBigEndian(byte[] out, int pos, long value, int byteCount) {
        for (var i = byteCount - 1; i >= 0; i--) {
            out[pos + i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
    }

    /**
     * Holds the tree-walk state shared between the sizing and the encoding passes.
     *
     * <p>Carries the depth-first object table, the identity-keyed index map that resolves a value to its
     * object-table slot, and the synthesised key strings for each dictionary, since dictionary keys are
     * themselves objects in the table.
     *
     * @implNote This implementation keys the index and dictionary-key maps by identity rather than value
     *           equality, so two equal-but-distinct values get separate slots; value-equality keying
     *           would force the writer to predict which shared slot each future reference resolves to.
     */
    private static final class Context {
        /**
         * Holds the object table in depth-first order, with the root at index 0.
         */
        private final List<PlistValue> objects = new ArrayList<>();

        /**
         * Maps each collected object, by identity, to its object-table index.
         *
         * <p>Identity-keyed so equal-but-distinct values are not deduplicated into one slot.
         */
        private final IdentityHashMap<PlistValue, Integer> indices = new IdentityHashMap<>();

        /**
         * Maps each dictionary, by identity, to the synthesised key strings for its entries in order.
         *
         * <p>Dictionary keys are themselves objects in the table, so one {@link PlistStringValue} is
         * materialised per key during collection, letting the encode pass emit a uniform layout of N key
         * references followed by N value references.
         */
        private final IdentityHashMap<PlistDictionaryValue, List<PlistStringValue>> dictKeys = new IdentityHashMap<>();

        /**
         * Assigns an object-table index to a value and recursively collects its descendants.
         *
         * <p>Returns an already-assigned index when the same instance has been seen before; otherwise
         * appends the value to the object table, records its index, and walks its children. Arrays are
         * walked by item order; dictionaries materialise one key {@link PlistStringValue} per entry into
         * {@link #dictKeys} and collect those keys before collecting the entry values. Called by
         * {@link #write(PlistValue)} on the root before any sizing or encoding runs.
         *
         * @param v the value
         * @return the assigned index
         */
        int collect(PlistValue v) {
            var existing = indices.get(v);
            if (existing != null) {
                return existing;
            }
            var idx = objects.size();
            objects.add(v);
            indices.put(v, idx);
            switch (v) {
                case PlistArrayValue arr -> {
                    for (var item : arr.items()) {
                        collect(item);
                    }
                }
                case PlistDictionaryValue dict -> {
                    var keys = new ArrayList<PlistStringValue>(dict.entries().size());
                    for (var entry : dict.entries().entrySet()) {
                        var keyObject = new PlistStringValue(entry.getKey());
                        keys.add(keyObject);
                        collect(keyObject);
                    }
                    dictKeys.put(dict, keys);
                    for (var entry : dict.entries().entrySet()) {
                        collect(entry.getValue());
                    }
                }
                default -> {
                }
            }
            return idx;
        }
    }
}
