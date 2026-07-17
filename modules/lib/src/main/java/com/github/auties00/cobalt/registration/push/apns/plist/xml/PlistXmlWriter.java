package com.github.auties00.cobalt.registration.push.apns.plist.xml;

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
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Serialises a {@link PlistValue} tree into Apple's XML property-list format.
 *
 * <p>The writer emits the canonical Apple preamble (the XML prolog, the {@code PropertyList-1.0.dtd}
 * doctype, and the {@code <plist version="1.0">} root) followed by the tab-indented value tree and the
 * closing {@code </plist>} block, producing bytes that Apple's activation endpoint accepts verbatim.
 * Containers are emitted one entry per line; dictionary keys and string content are XML-escaped; binary
 * blobs are Base64-encoded; dates and floating-point reals are rendered through their {@link Instant} and
 * {@link Double} string forms.
 *
 * @implNote This implementation runs two passes to stay allocation-free. The first pass computes the exact
 * UTF-8 byte count through {@link #sizeOf(PlistValue, int)} and its helpers; the second fills a single
 * pre-sized {@code byte[]} in place. There is no {@link StringBuilder} growth, no intermediate
 * {@link String} conversion of the tree, and no {@link java.nio.charset.CharsetEncoder} pass: every element
 * tag and entity replacement is a pre-encoded {@code byte[]} constant, the UTF-8 encoder is inlined, and
 * Base64, decimal-integer, and indentation output is written directly into the destination buffer.
 */
public final class PlistXmlWriter {
    /**
     * The logger for {@link PlistXmlWriter}.
     */
    private static final System.Logger LOGGER = Log.get(PlistXmlWriter.class);

    /**
     * Holds the canonical Apple XML preamble, comprising the XML prolog, the {@code PropertyList-1.0.dtd}
     * doctype declaration, and the opening {@code <plist version="1.0">} root, emitted at the head of every
     * {@link #write(PlistValue)} result.
     */
    private static final byte[] PREAMBLE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            """.getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the closing {@code </plist>} block emitted at the tail of every {@link #write(PlistValue)}
     * result.
     */
    private static final byte[] EPILOGUE = "\n</plist>\n".getBytes(StandardCharsets.US_ASCII);

    /**
     * Holds the opening {@code <dict>} tag with a trailing newline.
     */
    private static final byte[] DICT_OPEN = {'<', 'd', 'i', 'c', 't', '>', '\n'};
    /**
     * Holds the closing {@code </dict>} tag.
     */
    private static final byte[] DICT_CLOSE = {'<', '/', 'd', 'i', 'c', 't', '>'};
    /**
     * Holds the opening {@code <array>} tag with a trailing newline.
     */
    private static final byte[] ARRAY_OPEN = {'<', 'a', 'r', 'r', 'a', 'y', '>', '\n'};
    /**
     * Holds the closing {@code </array>} tag.
     */
    private static final byte[] ARRAY_CLOSE = {'<', '/', 'a', 'r', 'r', 'a', 'y', '>'};
    /**
     * Holds the opening {@code <key>} tag.
     */
    private static final byte[] KEY_OPEN = {'<', 'k', 'e', 'y', '>'};
    /**
     * Holds the closing {@code </key>} tag with a trailing newline.
     */
    private static final byte[] KEY_CLOSE = {'<', '/', 'k', 'e', 'y', '>', '\n'};
    /**
     * Holds the opening {@code <string>} tag.
     */
    private static final byte[] STRING_OPEN = {'<', 's', 't', 'r', 'i', 'n', 'g', '>'};
    /**
     * Holds the closing {@code </string>} tag.
     */
    private static final byte[] STRING_CLOSE = {'<', '/', 's', 't', 'r', 'i', 'n', 'g', '>'};
    /**
     * Holds the opening {@code <data>} tag.
     */
    private static final byte[] DATA_OPEN = {'<', 'd', 'a', 't', 'a', '>'};
    /**
     * Holds the closing {@code </data>} tag.
     */
    private static final byte[] DATA_CLOSE = {'<', '/', 'd', 'a', 't', 'a', '>'};
    /**
     * Holds the opening {@code <integer>} tag.
     */
    private static final byte[] INTEGER_OPEN = {'<', 'i', 'n', 't', 'e', 'g', 'e', 'r', '>'};
    /**
     * Holds the closing {@code </integer>} tag.
     */
    private static final byte[] INTEGER_CLOSE = {'<', '/', 'i', 'n', 't', 'e', 'g', 'e', 'r', '>'};
    /**
     * Holds the opening {@code <real>} tag.
     */
    private static final byte[] REAL_OPEN = {'<', 'r', 'e', 'a', 'l', '>'};
    /**
     * Holds the closing {@code </real>} tag.
     */
    private static final byte[] REAL_CLOSE = {'<', '/', 'r', 'e', 'a', 'l', '>'};
    /**
     * Holds the opening {@code <date>} tag.
     */
    private static final byte[] DATE_OPEN = {'<', 'd', 'a', 't', 'e', '>'};
    /**
     * Holds the closing {@code </date>} tag.
     */
    private static final byte[] DATE_CLOSE = {'<', '/', 'd', 'a', 't', 'e', '>'};
    /**
     * Holds the self-closing {@code <true/>} tag.
     */
    private static final byte[] TRUE_TAG = {'<', 't', 'r', 'u', 'e', '/', '>'};
    /**
     * Holds the self-closing {@code <false/>} tag.
     */
    private static final byte[] FALSE_TAG = {'<', 'f', 'a', 'l', 's', 'e', '/', '>'};
    /**
     * Holds the XML entity replacement for {@code &}.
     */
    private static final byte[] AMP_ENTITY = {'&', 'a', 'm', 'p', ';'};
    /**
     * Holds the XML entity replacement for {@code <}.
     */
    private static final byte[] LT_ENTITY = {'&', 'l', 't', ';'};
    /**
     * Holds the XML entity replacement for {@code >}.
     */
    private static final byte[] GT_ENTITY = {'&', 'g', 't', ';'};
    /**
     * Holds the pre-encoded decimal representation of {@link Long#MIN_VALUE}.
     *
     * @implNote This implementation special-cases {@link Long#MIN_VALUE} because negating it overflows, so
     * the generic {@link #writeLong(byte[], int, long)} path cannot derive its magnitude; the constant lets
     * that path emit the value without computing it.
     */
    private static final byte[] LONG_MIN_VALUE = "-9223372036854775808".getBytes(StandardCharsets.US_ASCII);
    /**
     * Holds the standard Base64 alphabet, indexed by 6-bit value.
     */
    private static final byte[] BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".getBytes(StandardCharsets.US_ASCII);

    /**
     * Prevents instantiation of this stateless namespace class.
     */
    private PlistXmlWriter() {
    }

    /**
     * Serialises a value tree as an XML plist with the canonical Apple preamble.
     *
     * <p>Computes the exact output size, allocates a single buffer of that size, then fills it with the
     * preamble, the rendered value tree, and the epilogue. The returned array is exactly the length of the
     * serialised bytes with no slack.
     *
     * @param root the root value
     * @return the UTF-8 encoded XML plist bytes
     */
    public static byte[] write(PlistValue root) {
        var size = PREAMBLE.length + sizeOf(root, 0) + EPILOGUE.length;
        var out = new byte[size];
        var pos = writeBytes(out, 0, PREAMBLE);
        pos = writeValue(out, pos, root, 0);
        writeBytes(out, pos, EPILOGUE);
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "serialized xml plist ({0} bytes)", size);
        return out;
    }

    /**
     * Returns the exact UTF-8 byte count required to serialise a value at a given indent depth.
     *
     * <p>Pass-one driver. Dispatches on the sealed {@link PlistValue} hierarchy, recursing into containers,
     * and excludes the preamble and epilogue, which {@link #write(PlistValue)} adds once. The {@code indent}
     * tab count is included in the returned width.
     *
     * @param value  the value to size
     * @param indent the indentation depth in tabs
     * @return the byte count for {@code value} including its leading indentation
     */
    private static int sizeOf(PlistValue value, int indent) {
        return switch (value) {
            case PlistDictionaryValue d -> sizeOfDict(d, indent);
            case PlistArrayValue a -> sizeOfArray(a, indent);
            case PlistStringValue s -> indent + STRING_OPEN.length + escapedXmlByteLength(s.value()) + STRING_CLOSE.length;
            case PlistDataValue d -> indent + DATA_OPEN.length + base64Length(d.length()) + DATA_CLOSE.length;
            case PlistIntegerValue i -> indent + INTEGER_OPEN.length + decimalDigitCount(i.value()) + INTEGER_CLOSE.length;
            case PlistFloatingPointValue r -> indent + REAL_OPEN.length + Double.toString(r.value()).length() + REAL_CLOSE.length;
            case PlistBooleanValue b -> indent + (b.value() ? TRUE_TAG.length : FALSE_TAG.length);
            case PlistDateValue d -> indent + DATE_OPEN.length + d.value().toString().length() + DATE_CLOSE.length;
        };
    }

    /**
     * Returns the byte count required to serialise a dictionary stanza.
     *
     * <p>Sums the open tag, each indented {@code <key>} / value pair plus its trailing newline, and the
     * close tag, recursing into {@link #sizeOf(PlistValue, int)} for each value.
     *
     * @param dict   the dictionary
     * @param indent the indentation depth in tabs
     * @return the byte count for the dictionary including its leading indentation
     */
    private static int sizeOfDict(PlistDictionaryValue dict, int indent) {
        var total = indent + DICT_OPEN.length;
        var childIndent = indent + 1;
        for (var entry : dict.entries().entrySet()) {
            total += childIndent + KEY_OPEN.length + escapedXmlByteLength(entry.getKey()) + KEY_CLOSE.length;
            total += sizeOf(entry.getValue(), childIndent) + 1;
        }
        total += indent + DICT_CLOSE.length;
        return total;
    }

    /**
     * Returns the byte count required to serialise an array stanza.
     *
     * <p>Sums the open tag, each indented item plus its trailing newline, and the close tag, recursing into
     * {@link #sizeOf(PlistValue, int)} for each item.
     *
     * @param array  the array
     * @param indent the indentation depth in tabs
     * @return the byte count for the array including its leading indentation
     */
    private static int sizeOfArray(PlistArrayValue array, int indent) {
        var total = indent + ARRAY_OPEN.length;
        var childIndent = indent + 1;
        for (var item : array.items()) {
            total += sizeOf(item, childIndent) + 1;
        }
        total += indent + ARRAY_CLOSE.length;
        return total;
    }

    /**
     * Emits a value into the destination buffer at the given write position.
     *
     * <p>Pass-two driver. Dispatches on the sealed {@link PlistValue} hierarchy and writes the indented,
     * tag-wrapped rendering directly into {@code out}. Each branch writes the leading indentation, the
     * opening tag, the encoded body, and the closing tag, except the boolean branch, which emits a single
     * self-closing tag.
     *
     * @param out    the destination buffer
     * @param pos    the current write position
     * @param value  the value to emit
     * @param indent the current indentation depth in tabs
     * @return the write position after the value
     */
    private static int writeValue(byte[] out, int pos, PlistValue value, int indent) {
        return switch (value) {
            case PlistDictionaryValue d -> writeDict(out, pos, d, indent);
            case PlistArrayValue a -> writeArray(out, pos, a, indent);
            case PlistStringValue s -> {
                var p = writeIndent(out, pos, indent);
                p = writeBytes(out, p, STRING_OPEN);
                p = writeXmlEscaped(out, p, s.value());
                yield writeBytes(out, p, STRING_CLOSE);
            }
            case PlistDataValue d -> {
                var p = writeIndent(out, pos, indent);
                p = writeBytes(out, p, DATA_OPEN);
                p = writeBase64(out, p, d.source(), d.offset(), d.length());
                yield writeBytes(out, p, DATA_CLOSE);
            }
            case PlistIntegerValue i -> {
                var p = writeIndent(out, pos, indent);
                p = writeBytes(out, p, INTEGER_OPEN);
                p = writeLong(out, p, i.value());
                yield writeBytes(out, p, INTEGER_CLOSE);
            }
            case PlistFloatingPointValue r -> {
                var p = writeIndent(out, pos, indent);
                p = writeBytes(out, p, REAL_OPEN);
                p = writeAsciiString(out, p, Double.toString(r.value()));
                yield writeBytes(out, p, REAL_CLOSE);
            }
            case PlistBooleanValue b -> {
                var p = writeIndent(out, pos, indent);
                yield writeBytes(out, p, b.value() ? TRUE_TAG : FALSE_TAG);
            }
            case PlistDateValue d -> {
                var p = writeIndent(out, pos, indent);
                p = writeBytes(out, p, DATE_OPEN);
                p = writeAsciiString(out, p, d.value().toString());
                yield writeBytes(out, p, DATE_CLOSE);
            }
        };
    }

    /**
     * Emits a dictionary with one {@code <key>} / value pair per line.
     *
     * <p>Writes the open tag, then for each entry the indented {@code <key>} element followed by its value
     * on its own line, and finally the indented close tag. Key text is routed through
     * {@link #writeXmlEscaped(byte[], int, String)} so keys containing XML metacharacters survive the
     * round-trip.
     *
     * @param out    the destination buffer
     * @param pos    the current write position
     * @param dict   the dictionary
     * @param indent the current indentation depth in tabs
     * @return the write position after the dictionary
     */
    private static int writeDict(byte[] out, int pos, PlistDictionaryValue dict, int indent) {
        pos = writeIndent(out, pos, indent);
        pos = writeBytes(out, pos, DICT_OPEN);
        var childIndent = indent + 1;
        for (var entry : dict.entries().entrySet()) {
            pos = writeIndent(out, pos, childIndent);
            pos = writeBytes(out, pos, KEY_OPEN);
            pos = writeXmlEscaped(out, pos, entry.getKey());
            pos = writeBytes(out, pos, KEY_CLOSE);
            pos = writeValue(out, pos, entry.getValue(), childIndent);
            out[pos++] = '\n';
        }
        pos = writeIndent(out, pos, indent);
        return writeBytes(out, pos, DICT_CLOSE);
    }

    /**
     * Emits an array with one entry per line.
     *
     * <p>Writes the open tag, then each item on its own line at the child indent depth, and finally the
     * indented close tag.
     *
     * @param out    the destination buffer
     * @param pos    the current write position
     * @param array  the array
     * @param indent the current indentation depth in tabs
     * @return the write position after the array
     */
    private static int writeArray(byte[] out, int pos, PlistArrayValue array, int indent) {
        pos = writeIndent(out, pos, indent);
        pos = writeBytes(out, pos, ARRAY_OPEN);
        var childIndent = indent + 1;
        for (var item : array.items()) {
            pos = writeValue(out, pos, item, childIndent);
            out[pos++] = '\n';
        }
        pos = writeIndent(out, pos, indent);
        return writeBytes(out, pos, ARRAY_CLOSE);
    }

    /**
     * Copies a byte array into the destination buffer.
     *
     * <p>Used to emit every pre-encoded tag and entity constant.
     *
     * @param out the destination buffer
     * @param pos the current write position
     * @param src the bytes to copy
     * @return the write position after the copy
     */
    private static int writeBytes(byte[] out, int pos, byte[] src) {
        System.arraycopy(src, 0, out, pos, src.length);
        return pos + src.length;
    }

    /**
     * Writes the requested number of tab characters at the current position.
     *
     * @param out   the destination buffer
     * @param pos   the current write position
     * @param count the indentation depth in tabs
     * @return the write position after the tabs
     */
    private static int writeIndent(byte[] out, int pos, int count) {
        for (var i = 0; i < count; i++) {
            out[pos++] = '\t';
        }
        return pos;
    }

    /**
     * Writes a string as UTF-8 with XML entity escaping.
     *
     * <p>Used for string element content and dictionary key text. The characters {@code &}, {@code <}, and
     * {@code >} are replaced with their entity forms; all other characters are UTF-8 encoded.
     *
     * @implNote This implementation inlines the UTF-8 encoder so the writer stays allocation-free: ASCII
     * characters emit one byte, two-byte BMP characters emit two, a valid high/low surrogate pair decodes to
     * a single 4-byte sequence, and any other character (including a lone surrogate) emits three bytes.
     *
     * @param out   the destination buffer
     * @param pos   the current write position
     * @param value the source string
     * @return the write position after the encoded text
     */
    private static int writeXmlEscaped(byte[] out, int pos, String value) {
        for (var i = 0; i < value.length(); i++) {
            var c = value.charAt(i);
            switch (c) {
                case '&' -> pos = writeBytes(out, pos, AMP_ENTITY);
                case '<' -> pos = writeBytes(out, pos, LT_ENTITY);
                case '>' -> pos = writeBytes(out, pos, GT_ENTITY);
                default -> {
                    if (c < 0x80) {
                        out[pos++] = (byte) c;
                    } else if (c < 0x800) {
                        out[pos++] = (byte) (0xC0 | (c >> 6));
                        out[pos++] = (byte) (0x80 | (c & 0x3F));
                    } else if (Character.isHighSurrogate(c) && i + 1 < value.length()
                            && Character.isLowSurrogate(value.charAt(i + 1))) {
                        var cp = Character.toCodePoint(c, value.charAt(++i));
                        out[pos++] = (byte) (0xF0 | (cp >> 18));
                        out[pos++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                        out[pos++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                        out[pos++] = (byte) (0x80 | (cp & 0x3F));
                    } else {
                        out[pos++] = (byte) (0xE0 | (c >> 12));
                        out[pos++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                        out[pos++] = (byte) (0x80 | (c & 0x3F));
                    }
                }
            }
        }
        return pos;
    }

    /**
     * Writes an already-ASCII string verbatim, one byte per character.
     *
     * <p>Used for the outputs of {@link Double#toString(double)} and {@link Instant#toString()}, neither of
     * which can produce non-ASCII characters, so no encoding is needed.
     *
     * @param out the destination buffer
     * @param pos the current write position
     * @param s   the ASCII source string
     * @return the write position after the bytes
     */
    private static int writeAsciiString(byte[] out, int pos, String s) {
        for (var i = 0; i < s.length(); i++) {
            out[pos++] = (byte) s.charAt(i);
        }
        return pos;
    }

    /**
     * Writes the decimal representation of an integer.
     *
     * <p>Used for the body of every {@code <integer>} element. A leading minus sign is emitted when
     * {@code value} is negative.
     *
     * @implNote This implementation avoids {@link Long#toString(long)} so no intermediate {@link String} is
     * allocated; it counts the digits, then fills them from least to most significant into the destination.
     * The {@link Long#MIN_VALUE} edge case routes through the pre-encoded {@link #LONG_MIN_VALUE} constant
     * because negating that value overflows.
     *
     * @param out   the destination buffer
     * @param pos   the current write position
     * @param value the integer value
     * @return the write position after the digits
     */
    private static int writeLong(byte[] out, int pos, long value) {
        if (value == Long.MIN_VALUE) {
            return writeBytes(out, pos, LONG_MIN_VALUE);
        }
        if (value == 0) {
            out[pos++] = '0';
            return pos;
        }
        var negative = value < 0;
        if (negative) {
            value = -value;
            out[pos++] = '-';
        }
        var digitCount = 0;
        var probe = value;
        while (probe > 0) {
            digitCount++;
            probe /= 10;
        }
        var end = pos + digitCount;
        var writeAt = end - 1;
        while (value > 0) {
            out[writeAt--] = (byte) ('0' + (value % 10));
            value /= 10;
        }
        return end;
    }

    /**
     * Writes the Base64 encoding of a source slice directly into the destination buffer.
     *
     * <p>Used for the body of every {@code <data>} element. The slice {@code [srcOffset, srcOffset +
     * srcLength)} is encoded; a partial final group is padded with {@code '='} per the Base64 standard.
     *
     * @implNote This implementation streams from {@code src} into {@code out} without an intermediate slice
     * or {@link java.util.Base64.Encoder} call, using the pre-encoded {@link #BASE64_ALPHABET} lookup table.
     *
     * @param out       the destination buffer
     * @param pos       the current write position
     * @param src       the source buffer
     * @param srcOffset the start offset within {@code src}
     * @param srcLength the number of source bytes to encode
     * @return the write position after the encoded bytes
     */
    private static int writeBase64(byte[] out, int pos, byte[] src, int srcOffset, int srcLength) {
        var srcEnd = srcOffset + srcLength;
        var i = srcOffset;
        while (i + 3 <= srcEnd) {
            var b = ((src[i] & 0xFF) << 16) | ((src[i + 1] & 0xFF) << 8) | (src[i + 2] & 0xFF);
            out[pos++] = BASE64_ALPHABET[(b >> 18) & 0x3F];
            out[pos++] = BASE64_ALPHABET[(b >> 12) & 0x3F];
            out[pos++] = BASE64_ALPHABET[(b >> 6) & 0x3F];
            out[pos++] = BASE64_ALPHABET[b & 0x3F];
            i += 3;
        }
        var remaining = srcEnd - i;
        if (remaining == 1) {
            var b = (src[i] & 0xFF) << 16;
            out[pos++] = BASE64_ALPHABET[(b >> 18) & 0x3F];
            out[pos++] = BASE64_ALPHABET[(b >> 12) & 0x3F];
            out[pos++] = '=';
            out[pos++] = '=';
        } else if (remaining == 2) {
            var b = ((src[i] & 0xFF) << 16) | ((src[i + 1] & 0xFF) << 8);
            out[pos++] = BASE64_ALPHABET[(b >> 18) & 0x3F];
            out[pos++] = BASE64_ALPHABET[(b >> 12) & 0x3F];
            out[pos++] = BASE64_ALPHABET[(b >> 6) & 0x3F];
            out[pos++] = '=';
        }
        return pos;
    }

    /**
     * Returns the UTF-8 byte count required to serialise a string with XML entity escaping applied.
     *
     * <p>Pass-one helper paired with {@link #writeXmlEscaped(byte[], int, String)}. Counts {@code &},
     * {@code <}, and {@code >} as their entity replacement widths and every other character as its UTF-8
     * byte width, advancing past the low surrogate of a valid pair so it is counted once.
     *
     * @param s the source string
     * @return the escaped UTF-8 byte count
     */
    private static int escapedXmlByteLength(String s) {
        var len = 0;
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            switch (c) {
                case '&' -> len += AMP_ENTITY.length;
                case '<' -> len += LT_ENTITY.length;
                case '>' -> len += GT_ENTITY.length;
                default -> {
                    if (c < 0x80) {
                        len++;
                    } else if (c < 0x800) {
                        len += 2;
                    } else if (Character.isHighSurrogate(c) && i + 1 < s.length()
                            && Character.isLowSurrogate(s.charAt(i + 1))) {
                        len += 4;
                        i++;
                    } else {
                        len += 3;
                    }
                }
            }
        }
        return len;
    }

    /**
     * Returns the length of the Base64 encoding, with padding, of a source slice.
     *
     * <p>Pass-one helper paired with {@link #writeBase64(byte[], int, byte[], int, int)}.
     *
     * @implNote This implementation uses the canonical formula {@code ((n + 2) / 3) * 4}, which rounds the
     * input length up to a whole number of 3-byte groups and accounts for the trailing {@code '='} padding.
     *
     * @param srcLength the source length
     * @return the encoded length in bytes
     */
    private static int base64Length(int srcLength) {
        return ((srcLength + 2) / 3) * 4;
    }

    /**
     * Returns the number of characters {@link Long#toString(long)} would produce for a value.
     *
     * <p>Pass-one helper paired with {@link #writeLong(byte[], int, long)}; counts the digits plus an
     * optional leading minus sign without allocating a {@link String}.
     *
     * @implNote This implementation routes {@link Long#MIN_VALUE} through the {@link #LONG_MIN_VALUE}
     * constant's length because negating that value to take its magnitude overflows.
     *
     * @param value the integer value
     * @return the character count of the decimal representation
     */
    private static int decimalDigitCount(long value) {
        if (value == Long.MIN_VALUE) {
            return LONG_MIN_VALUE.length;
        }
        var sign = value < 0 ? 1 : 0;
        var abs = value < 0 ? -value : value;
        var count = 1;
        while (abs >= 10) {
            count++;
            abs /= 10;
        }
        return count + sign;
    }
}
