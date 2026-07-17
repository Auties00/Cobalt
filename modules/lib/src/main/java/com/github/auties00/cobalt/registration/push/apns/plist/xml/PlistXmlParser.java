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

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;

/**
 * Parses Apple's XML property-list format into a {@link PlistValue} tree by recursive descent.
 *
 * <p>The parser accepts the small subset of the plist grammar that Cobalt's APNS code exchanges with
 * Apple's activation and bag endpoints. The recognised element set is exactly {@code true}, {@code false},
 * {@code string}, {@code integer}, {@code real}, {@code data}, {@code date}, {@code dict}, and {@code array};
 * any other element name raises {@link IOException}. The {@code <?xml?>} prolog, a single {@code <!DOCTYPE>}
 * declaration, and {@code <!-- ... -->} comments are tolerated and skipped wherever whitespace is allowed.
 * Self-closing forms such as {@code <true/>}, {@code <dict/>}, and {@code <array/>} yield the empty
 * equivalent. Instances are single-use and bound to one source buffer; callers obtain a result through
 * {@link #parse(byte[])} rather than constructing the parser directly.
 *
 * @implNote This implementation runs a single pass over the source {@code byte[]} with no
 * {@link java.io.Reader} wrapper, advancing an integer cursor through the raw bytes. XML namespaces and DTD
 * validation are deliberately not honoured: Apple's plist DTD has been stable since 2002 and no production
 * plist Cobalt parses relies on either feature, so the {@code <!DOCTYPE>} declaration is skipped wholesale
 * rather than interpreted. Dictionary entries are collected into a {@link LinkedHashMap} so source order
 * survives the round-trip.
 */
public final class PlistXmlParser {
    /**
     * The logger for {@link PlistXmlParser}.
     */
    private static final System.Logger LOGGER = Log.get(PlistXmlParser.class);

    /**
     * Holds the source bytes being parsed.
     */
    private final byte[] src;

    /**
     * Tracks the current read offset within {@link #src}.
     */
    private int pos;

    /**
     * Binds a parser instance to a source buffer.
     *
     * <p>The constructor is private so callers route through {@link #parse(byte[])}, which constructs the
     * instance and drives the parse.
     *
     * @param src the source bytes
     */
    private PlistXmlParser(byte[] src) {
        this.src = src;
    }

    /**
     * Parses XML plist bytes into a {@link PlistValue} tree.
     *
     * <p>Constructs an internal parser bound to {@code data} and drives the parse from the start of the
     * buffer through the single root value.
     *
     * @param data the XML plist bytes
     * @return the parsed root value
     * @throws IOException if the source is malformed or contains an unrecognised element
     */
    public static PlistValue parse(byte[] data) throws IOException {
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsing xml plist ({0} bytes)", data.length);
        return new PlistXmlParser(data).parseRoot();
    }

    /**
     * Drives the parse from the start of the source through the {@code <plist>} root.
     *
     * <p>Skips the {@code <?xml?>} prolog and {@code <!DOCTYPE>} preamble, optionally opens the
     * {@code <plist>} wrapper when present, then delegates to {@link #parseValue()} for the single child
     * value the wrapper contains.
     *
     * @return the root value
     * @throws IOException if the source is malformed
     */
    private PlistValue parseRoot() throws IOException {
        skipMisc();
        if (matchAt("<plist")) {
            pos = indexOf('>') + 1;
            skipMisc();
        }
        return parseValue();
    }

    /**
     * Reads exactly one value element starting at the current position.
     *
     * <p>Consumes the opening tag, then dispatches on the element name to the matching value constructor.
     * Self-closing variants are tolerated and yield the empty equivalent: {@code <true/>} and
     * {@code <false/>} yield the corresponding boolean, {@code <string/>} yields the empty string,
     * {@code <data/>} yields a zero-length blob, and {@code <dict/>} / {@code <array/>} yield empty
     * containers.
     *
     * @return the parsed value
     * @throws IOException if the leading {@code <} is missing, the tag is unterminated, or the element name
     *                     is not one of the recognised plist elements
     */
    private PlistValue parseValue() throws IOException {
        skipMisc();
        if (pos >= src.length || src[pos] != '<') {
            throw new IOException("expected '<' at " + pos);
        }
        pos++;
        var tagStart = pos;
        while (pos < src.length) {
            var c = src[pos];
            if (c == '>' || c == '/' || c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                break;
            }
            pos++;
        }
        var tagName = new String(src, tagStart, pos - tagStart, StandardCharsets.US_ASCII);
        var selfClose = false;
        while (pos < src.length && src[pos] != '>') {
            if (src[pos] == '/') {
                selfClose = true;
            }
            pos++;
        }
        if (pos >= src.length) {
            throw new IOException("unterminated tag <" + tagName);
        }
        pos++;
        return switch (tagName) {
            case "true" -> {
                if (!selfClose) {
                    consumeClose("true");
                }
                yield new PlistBooleanValue(true);
            }
            case "false" -> {
                if (!selfClose) {
                    consumeClose("false");
                }
                yield new PlistBooleanValue(false);
            }
            case "string" -> selfClose
                    ? new PlistStringValue("")
                    : new PlistStringValue(readTextAndClose("string"));
            case "integer" -> new PlistIntegerValue(Long.parseLong(readTextAndClose("integer").trim()));
            case "real" -> new PlistFloatingPointValue(Double.parseDouble(readTextAndClose("real").trim()));
            case "data" -> {
                if (selfClose) {
                    yield new PlistDataValue(new byte[0], 0, 0);
                }
                var raw = readTextAndClose("data");
                var decoded = Base64.getMimeDecoder().decode(raw);
                yield new PlistDataValue(decoded, 0, decoded.length);
            }
            case "date" -> new PlistDateValue(Instant.parse(readTextAndClose("date").trim()));
            case "dict" -> selfClose ? new PlistDictionaryValue(new LinkedHashMap<>()) : parseDict();
            case "array" -> selfClose ? new PlistArrayValue(new ArrayList<>()) : parseArray();
            default -> {
                if (Log.WARNING) LOGGER.log(Level.WARNING, "unknown plist element <{0}>", tagName);
                throw new IOException("unknown plist element <" + tagName + ">");
            }
        };
    }

    /**
     * Reads alternating {@code <key>} and value pairs until the closing {@code </dict>}.
     *
     * <p>Invoked after {@link #parseValue()} has consumed the opening {@code <dict>}. Each iteration reads
     * one {@code <key>} element, decodes its entity-escaped text, then recurses into {@link #parseValue()}
     * for the associated value. Iteration stops at the closing tag.
     *
     * @return the parsed dictionary, preserving source order
     * @throws IOException if a {@code <key>} is expected but absent, a {@code </key>} close is missing, or a
     *                     value is malformed
     */
    private PlistDictionaryValue parseDict() throws IOException {
        var entries = new LinkedHashMap<String, PlistValue>();
        while (true) {
            skipMisc();
            if (matchAt("</dict>")) {
                pos += "</dict>".length();
                return new PlistDictionaryValue(entries);
            }
            if (!matchAt("<key>")) {
                throw new IOException("expected <key> at " + pos);
            }
            pos += "<key>".length();
            var keyStart = pos;
            while (pos < src.length && src[pos] != '<') {
                pos++;
            }
            var key = decodeEntities(new String(src, keyStart, pos - keyStart, StandardCharsets.UTF_8));
            if (!matchAt("</key>")) {
                throw new IOException("expected </key> at " + pos);
            }
            pos += "</key>".length();
            entries.put(key, parseValue());
        }
    }

    /**
     * Reads successive values until the closing {@code </array>}.
     *
     * <p>Invoked after {@link #parseValue()} has consumed the opening {@code <array>}. Each iteration
     * recurses into {@link #parseValue()} for one element; iteration stops at the closing tag.
     *
     * @return the parsed array
     * @throws IOException if an element is malformed before the close tag is reached
     */
    private PlistArrayValue parseArray() throws IOException {
        var items = new ArrayList<PlistValue>();
        while (true) {
            skipMisc();
            if (matchAt("</array>")) {
                pos += "</array>".length();
                return new PlistArrayValue(items);
            }
            items.add(parseValue());
        }
    }

    /**
     * Reads the text content of an element up to the next {@code <}, then consumes the matching close tag.
     *
     * <p>Serves the {@code string}, {@code integer}, {@code real}, {@code data}, and {@code date} branches
     * of {@link #parseValue()}. The captured text is entity-decoded through {@link #decodeEntities(String)}
     * before being returned.
     *
     * @param tag the element name, used to validate the close tag
     * @return the entity-decoded text content
     * @throws IOException if the expected close tag is not present
     */
    private String readTextAndClose(String tag) throws IOException {
        var start = pos;
        while (pos < src.length && src[pos] != '<') {
            pos++;
        }
        var raw = new String(src, start, pos - start, StandardCharsets.UTF_8);
        consumeClose(tag);
        return decodeEntities(raw);
    }

    /**
     * Consumes a closing tag at the current position.
     *
     * <p>Verifies that the literal {@code </tag>} appears at the cursor and advances past it. A mismatch
     * is rejected rather than tolerated.
     *
     * @param tag the element name whose close tag is expected
     * @throws IOException if the expected close tag is not present at the current position
     */
    private void consumeClose(String tag) throws IOException {
        var close = "</" + tag + ">";
        if (!matchAt(close)) {
            throw new IOException("expected " + close + " at " + pos);
        }
        pos += close.length();
    }

    /**
     * Skips whitespace, XML processing instructions, {@code <!DOCTYPE>} declarations, and comments.
     *
     * <p>Called before every value read so the parser tolerates arbitrary whitespace and comment
     * interleaving between elements. The cursor stops at the first byte that begins a value element.
     *
     * @implNote This implementation loops until a non-skip token is observed. Processing instructions and
     * comments must be terminated by their canonical {@code ?>} and {@code -->} trailers; a {@code <!DOCTYPE>}
     * declaration is skipped wholesale up to its closing {@code >} without interpreting its contents, since
     * Apple's plist DTD is fixed and never validated here.
     *
     * @throws IOException if a processing instruction or comment is unterminated
     */
    private void skipMisc() throws IOException {
        while (pos < src.length) {
            var c = src[pos];
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                pos++;
                continue;
            }
            if (matchAt("<?")) {
                var end = indexOfPair("?>");
                if (end < 0) {
                    throw new IOException("unterminated <?...?>");
                }
                pos = end + 2;
                continue;
            }
            if (matchAt("<!--")) {
                var end = indexOfPair("-->");
                if (end < 0) {
                    throw new IOException("unterminated <!-- -->");
                }
                pos = end + 3;
                continue;
            }
            if (matchAt("<!DOCTYPE") || matchAt("<!doctype")) {
                pos = indexOf('>') + 1;
                continue;
            }
            break;
        }
    }

    /**
     * Reports whether a literal appears at the current read position.
     *
     * <p>Compares {@code expected} byte-for-byte against the source starting at {@link #pos} without
     * advancing the cursor.
     *
     * @param expected the literal to test for
     * @return {@code true} if {@code expected} starts at {@link #pos}, {@code false} otherwise
     */
    private boolean matchAt(String expected) {
        if (pos + expected.length() > src.length) {
            return false;
        }
        for (var i = 0; i < expected.length(); i++) {
            if (src[pos + i] != (byte) expected.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the next byte position of a single character at or after the current read position.
     *
     * <p>Used to skip past {@code <plist ...>} attributes and {@code <!DOCTYPE ...>} declarations to their
     * closing {@code >}.
     *
     * @param target the byte to find
     * @return the index of the first occurrence, or {@code -1} if the source ends first
     */
    private int indexOf(char target) {
        for (var i = pos; i < src.length; i++) {
            if (src[i] == target) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the next position at which a multi-character sequence starts.
     *
     * <p>Used to locate the {@code ?>} and {@code -->} terminators during {@link #skipMisc()}.
     *
     * @param pair the two-or-more character sequence to find
     * @return the start index of the first occurrence, or {@code -1} if the source ends first
     */
    private int indexOfPair(String pair) {
        outer:
        for (var i = pos; i + pair.length() <= src.length; i++) {
            for (var j = 0; j < pair.length(); j++) {
                if (src[i + j] != (byte) pair.charAt(j)) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Decodes the XML entities that may appear in plist text payloads.
     *
     * <p>Recognises the named entities {@code &amp;}, {@code &lt;}, {@code &gt;}, {@code &quot;}, and
     * {@code &apos;}, as well as numeric character references in both decimal ({@code &#NN;}) and hexadecimal
     * ({@code &#xNN;}) forms. An unrecognised entity is passed through verbatim, including its surrounding
     * {@code &} and {@code ;}, so non-standard payloads still survive the round-trip.
     *
     * @implNote This implementation fast-paths the common case of text containing no {@code &} by returning
     * the input unchanged; otherwise it walks the string character by character into a
     * {@link StringBuilder}.
     *
     * @param raw the raw text
     * @return the decoded text
     */
    private static String decodeEntities(String raw) {
        if (raw.indexOf('&') < 0) {
            return raw;
        }
        var sb = new StringBuilder(raw.length());
        var i = 0;
        while (i < raw.length()) {
            var c = raw.charAt(i);
            if (c != '&') {
                sb.append(c);
                i++;
                continue;
            }
            var semi = raw.indexOf(';', i);
            if (semi < 0) {
                sb.append(raw, i, raw.length());
                break;
            }
            var name = raw.substring(i + 1, semi);
            switch (name) {
                case "amp" -> sb.append('&');
                case "lt" -> sb.append('<');
                case "gt" -> sb.append('>');
                case "quot" -> sb.append('"');
                case "apos" -> sb.append('\'');
                default -> {
                    if (name.startsWith("#x") || name.startsWith("#X")) {
                        sb.appendCodePoint(Integer.parseInt(name, 2, name.length(), 16));
                    } else if (name.startsWith("#")) {
                        sb.appendCodePoint(Integer.parseInt(name, 1, name.length(), 10));
                    } else {
                        sb.append('&').append(name).append(';');
                    }
                }
            }
            i = semi + 1;
        }
        return sb.toString();
    }
}
