package com.github.auties00.cobalt.registration.push.apns.plist;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.registration.push.apns.plist.binary.PlistBinaryParser;
import com.github.auties00.cobalt.registration.push.apns.plist.binary.PlistBinaryWriter;
import com.github.auties00.cobalt.registration.push.apns.plist.value.PlistValue;
import com.github.auties00.cobalt.registration.push.apns.plist.xml.PlistXmlParser;
import com.github.auties00.cobalt.registration.push.apns.plist.xml.PlistXmlWriter;

import java.io.IOException;
import java.lang.System.Logger.Level;

/**
 * Routes property-list parsing and serialisation to the format-specific Apple
 * {@code Foundation/PropertyList} implementations used by the APNS code.
 *
 * <p>This class is the single entry point callers depend on: {@link #parse(byte[])}
 * auto-detects the input format from its magic bytes and dispatches to
 * {@link PlistBinaryParser} or {@link PlistXmlParser}, while {@link #writeXml(PlistValue)}
 * and {@link #writeBinary(PlistValue)} emit each concrete format through the matching
 * writer. Depending on this class rather than on the implementation classes keeps the
 * format-detection logic in one place, so adding a future format (for example
 * {@code bplist15} or OpenStep) is a single edit here. The package exposes only this
 * type and the
 * {@link PlistValue}
 * value hierarchy.
 */
public final class Plist {
    /**
     * The logger for {@link Plist}.
     */
    private static final System.Logger LOGGER = Log.get(Plist.class);

    /**
     * Prevents instantiation of this stateless namespace.
     */
    private Plist() {
    }

    /**
     * Parses a plist, auto-detecting between the binary and XML formats.
     *
     * <p>The format is inferred from the leading magic bytes of {@code data}: input
     * beginning with the {@code bplist00} signature is treated as a binary plist and
     * parsed by {@link PlistBinaryParser#parse(byte[])}, and anything else is treated
     * as XML and parsed by {@link PlistXmlParser#parse(byte[])}. APNS callers can feed
     * the activation response or the bag bytes here without tracking which encoding
     * Apple returned.
     *
     * @implNote This implementation delegates the magic-byte check to
     *           {@link PlistBinaryParser#isBinary(byte[])}; only input starting with
     *           {@code bplist00} takes the binary branch.
     * @param data the source bytes
     * @return the root value
     * @throws IOException if the source is malformed for the detected format
     */
    public static PlistValue parse(byte[] data) throws IOException {
        if (PlistBinaryParser.isBinary(data)) {
            if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsing binary plist, bytes={0}", data.length);
            return PlistBinaryParser.parse(data);
        }
        if (Log.DEBUG) LOGGER.log(Level.DEBUG, "parsing xml plist, bytes={0}", data.length);
        return PlistXmlParser.parse(data);
    }

    /**
     * Serialises a value tree as an XML plist with the canonical Apple preamble.
     *
     * <p>This is the form the activation flow emits for the inner and outer activation
     * plists, which Apple's activation endpoint requires.
     *
     * @param root the root value
     * @return the UTF-8 encoded XML plist bytes
     */
    public static byte[] writeXml(PlistValue root) {
        return PlistXmlWriter.write(root);
    }

    /**
     * Serialises a value tree as a {@code bplist00} binary plist.
     *
     * <p>This counterpart to {@link #writeXml(PlistValue)} is not exercised by the
     * current APNS code path but remains available for callers that need the binary
     * encoding.
     *
     * @param root the root value
     * @return the binary plist bytes
     */
    public static byte[] writeBinary(PlistValue root) {
        return PlistBinaryWriter.write(root);
    }
}
