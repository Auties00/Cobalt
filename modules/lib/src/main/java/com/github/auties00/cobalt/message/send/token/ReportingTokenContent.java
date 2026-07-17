package com.github.auties00.cobalt.message.send.token;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.reporting.ReportingConfig;
import com.github.auties00.cobalt.wire.linked.reporting.ReportingConfigSpec;
import com.github.auties00.cobalt.wire.linked.reporting.ReportingField;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the deterministic franking content that backs the reporting-token
 * HMAC.
 *
 * <p>The bytes returned by {@link #compute(byte[], int)} are the second argument
 * to {@link ReportingToken#generate}. The output is a sparse copy of the
 * serialised {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer}
 * protobuf carrying only the field numbers that the {@link ReportingConfig} for
 * the current sender version whitelists. HMACing the full payload instead of
 * this sparse copy makes the server-side check fail with a
 * {@code reporting-token-validation-failure}, even when every other stanza field
 * is correct, because the server walks the same config when it replays the HMAC.
 */
@WhatsAppWebModule(moduleName = "WAWebReportingTokenConfig")
@WhatsAppWebModule(moduleName = "WAWebReportingTokenContent")
public final class ReportingTokenContent {
    /**
     * The logger for {@link ReportingTokenContent}.
     */
    private static final System.Logger LOGGER = Log.get(ReportingTokenContent.class);

    /**
     * The protobuf wire type for VARINT fields ({@code int32}, {@code int64},
     * {@code bool}, {@code enum}).
     */
    private static final int WIRE_VARINT = 0;

    /**
     * The protobuf wire type for fixed 64-bit fields ({@code fixed64},
     * {@code sfixed64}, {@code double}).
     */
    private static final int WIRE_BIT64 = 1;

    /**
     * The protobuf wire type for length-delimited fields ({@code string},
     * {@code bytes}, embedded messages).
     */
    private static final int WIRE_LENGTH_DELIMITED = 2;

    /**
     * The protobuf wire type for fixed 32-bit fields ({@code fixed32},
     * {@code sfixed32}, {@code float}).
     */
    private static final int WIRE_BIT32 = 5;

    /**
     * The bit mask for extracting the wire type from a tag varint.
     */
    private static final int WIRE_TYPE_MASK = 0x07;

    /**
     * The bit shift applied to a tag varint to recover the field number.
     */
    private static final int FIELD_NUMBER_SHIFT = 3;

    /**
     * Sorts kept nodes by ascending field number to match WA Web's canonical
     * emission order.
     */
    private static final Comparator<KeptNode> NODE_BY_FIELD_NUMBER =
            Comparator.comparingInt(KeptNode::fieldNumber);

    /**
     * The Base64-encoded {@code Config} protobuf shipped inline in the WA Web
     * bundle.
     *
     * <p>Decoded against {@link ReportingConfigSpec} the first time
     * {@link #getConfig(int)} is consulted. Bumping this value out of lockstep
     * with the server-side whitelist makes every locally-generated reporting
     * token fail validation, so it should only change when WA Web ships a new
     * bundle.
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenConfig", exports = "REPORTING_TOKEN_CONFIG_BASE64",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String REPORTING_TOKEN_CONFIG_BASE64 =
            "CgQIARIACjQIAxIwKgQIAhIAKgQIAxIAKgQICBIAKgQICxIAKhAIERIMKgQIFRIAKgQIFhIAKgQIGRIA"
                    + "CioIBBImCAIqBggBEgIIAioGCBASAggCKhIIERIOCAIqBAgVEgAqBAgWEgAKOggFEjYIAioGCAMSAggC"
                    + "KgYIBBICCAIqBggFEgIIAioGCBASAggCKhIIERIOCAIqBAgVEgAqBAgWEgAKIggGEh4qBAgBEgAqEAgR"
                    + "EgwqBAgVEgAqBAgWEgAqBAgeEgAKLggHEioqBAgCEgAqBAgHEgAqBAgKEgAqEAgREgwqBAgVEgAqBAgW"
                    + "EgAqBAgUEgAKLggIEioqBAgCEgAqBAgHEgAqBAgJEgAqEAgREgwqBAgVEgAqBAgWEgAqBAgVEgAKNAgJ"
                    + "EjAqBAgCEgAqBAgGEgAqBAgHEgAqBAgNEgAqEAgREgwqBAgVEgAqBAgWEgAqBAgUEgAKKAgMEiQIAioG"
                    + "CAESAggCKgYIAhICCAIqCAgOEgQIAiABKgYIDxICCAIKKggSEiYIAioGCAYSAggCKgYIEBICCAIqEggR"
                    + "Eg4IAioECBUSACoECBYSAAouCBoSKioECAQSACoECAUSACoECAgSACoECA0SACoQCBESDCoECBUSACoE"
                    + "CBYSAApCCBwSPggCKgYIARICCAIqBggCEgIIAioGCAQSAggCKgYIBRICCAIqBggGEgIIAioSCAcSDggC"
                    + "KgQIFRIAKgQIFhIACgwIJRIIKgYIARICIAEKUggxEk4IAioGCAISAggCKhYIAxISCAIqBggBEgIIAioG"
                    + "CAISAggCKhIIBRIOCAIqBAgVEgAqBAgWEgAqFggIEhIIAioGCAESAggCKgYIAhICCAIKDAg1EggqBggB"
                    + "EgIgAQoOCDcSCggCKgYIARICIAEKDgg6EgoIAioGCAESAiABCg4IOxIKCAIqBggBEgIgAQpSCDwSTggC"
                    + "KgYIAhICCAIqFggDEhIIAioGCAESAggCKgYIAhICCAIqEggFEg4IAioECBUSACoECBYSACoWCAgSEggC"
                    + "KgYIARICCAIqBggCEgIIAgpSCEASTggCKgYIAhICCAIqFggDEhIIAioGCAESAggCKgYIAhICCAIqEggF"
                    + "Eg4IAioECBUSACoECBYSACoWCAgSEggCKgYIARICCAIqBggCEgIIAgo2CEISMggCKgQIAhIAKgQIBhIA"
                    + "KgQIBxIAKgQIDRIAKhAIERIMKgQIFRIAKgQIFhIAKgQIFBIACg4IShIKCAIqBggBEgIgAQoOCFcSCggC"
                    + "KgYIARICIAEKMghYEi4IAioGCAESAggCKg4IAhIKCAIqBggBEgIIAioSCAMSDggCKgQIFRIAKgQIFhIA"
                    + "Cg4IXBIKCAIqBggBEgIgAQoOCF0SCggCKgYIARICIAEKDgheEgoIAioGCAESAiAB";

    /**
     * The lazily-decoded singleton {@link ReportingConfig}.
     */
    private static volatile ReportingConfig CONFIG;

    /**
     * The cache keyed by sender version.
     *
     * <p>The decoded {@link ReportingConfig} does not currently vary with the
     * version; the cache is keyed for symmetry with WA Web and to leave room for
     * version-specific pruning if the bundle later ships per-version configs.
     */
    private static final Map<Integer, ReportingConfig> CONFIG_CACHE = new ConcurrentHashMap<>();

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private ReportingTokenContent() {
        throw new AssertionError();
    }

    /**
     * Returns the decoded reporting-token configuration for the given sender
     * version.
     *
     * <p>Looked up once per {@link #compute(byte[], int)} call so the
     * version-pruning step inside the walker can consult the per-rule
     * {@link ReportingField#minVersion()} and {@link ReportingField#maxVersion()}
     * bracket without redecoding the Base64 blob.
     *
     * @implNote
     * This implementation returns the same {@link ReportingConfig} for every
     * version because the per-version filter is fused into the byte walker (see
     * {@link #versionMatches}). The {@code senderVersion} parameter is kept to
     * preserve symmetry with WA Web.
     *
     * @param senderVersion the {@code rt_sender_reporting_token_version}
     *                      currently in effect
     * @return the decoded {@link ReportingConfig}
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenConfig", exports = "getReportingTokenConfig",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static ReportingConfig getConfig(int senderVersion) {
        return CONFIG_CACHE.computeIfAbsent(senderVersion, _ -> getOrDecodeConfig());
    }

    /**
     * Computes the deterministic franking content over which the reporting-token
     * HMAC must be applied.
     *
     * <p>Walks the protobuf wire format of {@code messageBytes} field-by-field,
     * retains only the fields the config whitelists for {@code senderVersion},
     * recurses on sub-messages, sorts the retained fields by ascending field
     * number, and concatenates each retained field's raw bytes. The output is
     * bound to the encoded layout of the source: reordering a kept field or
     * re-encoding it changes the HMAC, which is intentional because the server
     * replays the same algorithm on its own copy of the message.
     *
     * @implNote
     * This implementation runs in two passes: an extraction pass that builds a
     * tree of {@link KeptNode} carriers and sums each subtree's emit size,
     * followed by a write pass that materialises the tree into a single
     * pre-sized {@code byte[]}. Carrying lengths through the tree avoids the
     * intermediate allocations the WA Web implementation performs at every
     * recursion level.
     *
     * @param messageBytes  the serialised
     *                      {@link com.github.auties00.cobalt.wire.linked.message.LinkedMessageContainer}
     *                      protobuf
     * @param senderVersion the {@code rt_sender_reporting_token_version}
     *                      currently in effect
     * @return the franking content; possibly empty when no field of the message
     *         survives the filter
     * @throws NullPointerException     if {@code messageBytes} is {@code null}
     * @throws IllegalArgumentException if {@code messageBytes} is malformed
     */
    @WhatsAppWebExport(moduleName = "WAWebReportingTokenContent", exports = "ReportingTokenContentCalculator",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static byte[] compute(byte[] messageBytes, int senderVersion) {
        if (messageBytes == null) {
            throw new NullPointerException("messageBytes cannot be null");
        }
        if (messageBytes.length == 0) {
            return messageBytes;
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "computing reporting token content, input size {0}, version {1}",
                    messageBytes.length, senderVersion);
        }
        var config = getConfig(senderVersion);
        var topRules = config.field();
        var kept = new ArrayList<KeptNode>();
        var totalSize = extract(messageBytes, 0, messageBytes.length, topRules, topRules, senderVersion, kept);
        if (kept.isEmpty()) {
            if (Log.TRACE) {
                LOGGER.log(Level.TRACE, "reporting token content: no whitelisted fields kept, version {0}",
                        senderVersion);
            }
            return new byte[0];
        }
        kept.sort(NODE_BY_FIELD_NUMBER);
        var out = new byte[totalSize];
        var cursor = 0;
        for (var node : kept) {
            cursor = node.write(messageBytes, out, cursor);
        }
        if (Log.TRACE) {
            LOGGER.log(Level.TRACE, "reporting token content computed, output size {0}", out.length);
        }
        return out;
    }

    /**
     * Recursively walks the protobuf region {@code [start, end)} and appends
     * every whitelisted field into {@code out}.
     *
     * <p>The {@code topRules} parameter is consulted when a kept length-delimited
     * field has {@link ReportingField#isMessage()} returning {@code true},
     * indicating that the sub-message is parsed against the top-level rule set
     * rather than the local {@link ReportingField#subfield()} map.
     *
     * @param src           the raw protobuf bytes containing the sub-region
     * @param start         the inclusive start offset of the sub-region
     * @param end           the exclusive end offset of the sub-region
     * @param rules         the rule map applicable at this level
     * @param topRules      the top-level rule map, consulted when a kept field
     *                      has {@link ReportingField#isMessage()} returning
     *                      {@code true}
     * @param senderVersion the {@code rt_sender_reporting_token_version}
     *                      currently in effect
     * @param out           the accumulator for the kept nodes at this level
     * @return the total emit size, in bytes, of the nodes appended at this level
     * @throws IllegalArgumentException if {@code src} is malformed
     */
    private static int extract(byte[] src, int start, int end,
                               Map<Integer, ReportingField> rules,
                               Map<Integer, ReportingField> topRules,
                               int senderVersion, List<KeptNode> out) {
        var emitSize = 0;
        var cursor = start;
        while (cursor < end) {
            var tagStart = cursor;
            var tagRead = readVarInt(src, cursor, end);
            cursor = tagRead.cursor();
            var fieldNumber = (int) (tagRead.value() >>> FIELD_NUMBER_SHIFT);
            var wireType = (int) (tagRead.value() & WIRE_TYPE_MASK);
            var valueStart = cursor;
            cursor = skipValue(src, cursor, end, wireType);
            var rule = rules == null ? null : rules.get(fieldNumber);
            if (rule == null || !versionMatches(rule, senderVersion)) {
                continue;
            }
            var hasSubfields = !rule.subfield().isEmpty();
            var recurse = wireType == WIRE_LENGTH_DELIMITED && (rule.isMessage() || hasSubfields);
            if (recurse) {
                var lengthRead = readVarInt(src, valueStart, end);
                var innerStart = lengthRead.cursor();
                var innerEnd = innerStart + (int) lengthRead.value();
                if (innerEnd > end) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING,
                                "reporting token content: inner submessage exceeds parent bounds, field {0}",
                                fieldNumber);
                    }
                    throw new IllegalArgumentException("Reporting-token content: inner submessage exceeds parent bounds");
                }
                cursor = innerEnd;
                var innerKept = new ArrayList<KeptNode>();
                var innerRules = rule.isMessage() ? topRules : rule.subfield();
                var innerSize = extract(src, innerStart, innerEnd, innerRules, topRules, senderVersion, innerKept);
                if (innerKept.isEmpty()) {
                    continue;
                }
                innerKept.sort(NODE_BY_FIELD_NUMBER);
                var tagLength = valueStart - tagStart;
                var nodeSize = tagLength + getVarIntSize(innerSize) + innerSize;
                out.add(new KeptNode.Branch(fieldNumber, tagStart, tagLength, innerSize, innerKept));
                emitSize += nodeSize;
            } else {
                var fieldLength = cursor - tagStart;
                out.add(new KeptNode.Leaf(fieldNumber, tagStart, fieldLength));
                emitSize += fieldLength;
            }
        }
        return emitSize;
    }

    /**
     * Returns whether a rule applies at the given sender version.
     *
     * <p>A rule is excluded when {@code version} is below
     * {@link ReportingField#minVersion()}, when {@code version} is above
     * {@link ReportingField#maxVersion()}, or when {@code version} is at or
     * above {@link ReportingField#notReportableMinVersion()} (the inclusive
     * lower bound for deprecation).
     *
     * @param rule    the rule from {@link ReportingField}
     * @param version the {@code rt_sender_reporting_token_version} currently in
     *                effect
     * @return {@code true} when {@code rule} applies for {@code version}
     */
    private static boolean versionMatches(ReportingField rule, int version) {
        if (rule.minVersion().isPresent() && version < rule.minVersion().getAsInt()) {
            return false;
        }
        if (rule.maxVersion().isPresent() && version > rule.maxVersion().getAsInt()) {
            return false;
        }
        return rule.notReportableMinVersion().isEmpty() || version < rule.notReportableMinVersion().getAsInt();
    }

    /**
     * Decodes the bundled {@link #REPORTING_TOKEN_CONFIG_BASE64} on first use and
     * returns the cached {@link ReportingConfig} thereafter.
     *
     * @implNote
     * This implementation uses double-checked locking on {@link #CONFIG} to
     * guarantee a single decode even under concurrent first-touch.
     *
     * @return the singleton {@link ReportingConfig}
     */
    private static ReportingConfig getOrDecodeConfig() {
        var local = CONFIG;
        if (local != null) {
            return local;
        }
        synchronized (ReportingTokenContent.class) {
            if (CONFIG == null) {
                var bytes = Base64.getDecoder().decode(REPORTING_TOKEN_CONFIG_BASE64);
                CONFIG = ReportingConfigSpec.decode(bytes);
                if (Log.DEBUG) {
                    LOGGER.log(Level.DEBUG, "reporting token config decoded, {0} top-level rules",
                            CONFIG.field().size());
                }
            }
            return CONFIG;
        }
    }

    /**
     * Advances {@code cursor} past one wire-format value of the given wire type.
     *
     * <p>Consumes exactly the bytes of one value (varint, fixed-32, fixed-64, or
     * length-delimited blob) and returns the post-value position so the caller
     * can decide whether to keep the surrounding field.
     *
     * @param src      the source bytes
     * @param cursor   the position pointing at the first value byte
     * @param end      the exclusive end of the sub-region
     * @param wireType the protobuf wire type
     * @return the position immediately after the value bytes
     * @throws IllegalArgumentException if the value is malformed or the wire
     *                                  type is unsupported
     */
    private static int skipValue(byte[] src, int cursor, int end, int wireType) {
        switch (wireType) {
            case WIRE_VARINT -> {
                return readVarInt(src, cursor, end).cursor();
            }
            case WIRE_BIT64 -> {
                if (cursor + 8 > end) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "reporting token content: truncated 64-bit value");
                    }
                    throw new IllegalArgumentException("Reporting-token content: truncated 64-bit value");
                }
                return cursor + 8;
            }
            case WIRE_LENGTH_DELIMITED -> {
                var lengthRead = readVarInt(src, cursor, end);
                var afterLen = lengthRead.cursor();
                var length = (int) lengthRead.value();
                if (afterLen + length > end) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "reporting token content: truncated length-delimited value");
                    }
                    throw new IllegalArgumentException("Reporting-token content: truncated length-delimited value");
                }
                return afterLen + length;
            }
            case WIRE_BIT32 -> {
                if (cursor + 4 > end) {
                    if (Log.WARNING) {
                        LOGGER.log(Level.WARNING, "reporting token content: truncated 32-bit value");
                    }
                    throw new IllegalArgumentException("Reporting-token content: truncated 32-bit value");
                }
                return cursor + 4;
            }
            default -> {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "reporting token content: unsupported wire type {0}", wireType);
                }
                throw new IllegalArgumentException("Reporting-token content: unsupported wire type " + wireType);
            }
        }
    }

    /**
     * Reads a single LEB128-encoded varint starting at {@code cursor}.
     *
     * <p>Rejects varints longer than 10 bytes (the maximum that can fit a 64-bit
     * unsigned integer) so a malformed source cannot cause an unbounded loop.
     *
     * @param src    the source bytes
     * @param cursor the position pointing at the first varint byte
     * @param end    the exclusive end of the sub-region
     * @return the decoded value paired with the position immediately after the
     *         varint
     * @throws IllegalArgumentException if the varint is truncated or longer than
     *                                  10 bytes
     */
    private static VarInt readVarInt(byte[] src, int cursor, int end) {
        var value = 0L;
        var shift = 0;
        var pos = cursor;
        while (pos < end) {
            var b = src[pos++];
            value |= ((long) (b & 0x7F)) << shift;
            if ((b & 0x80) == 0) {
                return new VarInt(value, pos);
            }
            shift += 7;
            if (shift > 63) {
                if (Log.WARNING) {
                    LOGGER.log(Level.WARNING, "reporting token content: varint overflow");
                }
                throw new IllegalArgumentException("Reporting-token content: varint overflow");
            }
        }
        if (Log.WARNING) {
            LOGGER.log(Level.WARNING, "reporting token content: truncated varint");
        }
        throw new IllegalArgumentException("Reporting-token content: truncated varint");
    }

    /**
     * Returns the number of bytes a non-negative {@code value} occupies when
     * LEB128-encoded.
     *
     * <p>Used by {@link #extract} to pre-size the output buffer; the chain of
     * power-of-two compares avoids the per-value loop the protobuf reference
     * implementation runs.
     *
     * @implNote
     * This implementation treats negative values as 10-byte sign extensions to
     * match the protobuf wire format, even though the walker never produces
     * negative inner-message lengths in practice.
     *
     * @param value the integer to encode
     * @return the number of bytes the LEB128 encoding occupies
     */
    private static int getVarIntSize(long value) {
        if (value < 0) {
            return 10;
        } else if (value < 1L << 7) {
            return 1;
        } else if (value < 1L << 14) {
            return 2;
        } else if (value < 1L << 21) {
            return 3;
        } else if (value < 1L << 28) {
            return 4;
        } else if (value < 1L << 35) {
            return 5;
        } else if (value < 1L << 42) {
            return 6;
        } else if (value < 1L << 49) {
            return 7;
        } else if (value < 1L << 56) {
            return 8;
        } else {
            return 9;
        }
    }

    /**
     * Writes {@code value} as a LEB128-encoded varint into {@code out} starting
     * at {@code cursor}.
     *
     * <p>The caller must have pre-sized {@code out} using
     * {@link #getVarIntSize}.
     *
     * @param out    the destination buffer (must be pre-sized)
     * @param cursor the offset at which to start writing
     * @param value  the unsigned integer to encode
     * @return the offset immediately after the written bytes
     */
    private static int writeVarInt(byte[] out, int cursor, long value) {
        var pos = cursor;
        var v = value;
        while ((v & ~0x7FL) != 0) {
            out[pos++] = (byte) ((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out[pos++] = (byte) (v & 0x7F);
        return pos;
    }

    /**
     * Pairs the decoded value of a varint with the cursor advanced past it.
     *
     * @param value  the decoded value
     * @param cursor the position immediately after the varint
     */
    private record VarInt(long value, int cursor) {
    }

    /**
     * Holds a protobuf field that survived the whitelist filter, plus the logic
     * to write it into the output buffer.
     *
     * <p>Used by {@link #compute(byte[], int)} to defer writing until the entire
     * tree has been built and totals are known; instances are never exposed
     * outside this class.
     */
    private sealed interface KeptNode {
        /**
         * Returns the protobuf field number used for sort order.
         *
         * @return the field number
         */
        int fieldNumber();

        /**
         * Writes this stanza's bytes into {@code out} starting at {@code cursor}.
         *
         * @implSpec
         * Implementations must write exactly the bytes accounted for in the emit
         * size computed during extraction and return {@code cursor} advanced by
         * that count, so that successive nodes pack contiguously into the
         * pre-sized buffer.
         *
         * @param src    the original source bytes (verbatim slices are copied
         *               straight from here)
         * @param out    the destination buffer (must be pre-sized)
         * @param cursor the offset at which to start writing
         * @return the offset immediately after the written bytes
         */
        int write(byte[] src, byte[] out, int cursor);

        /**
         * Holds a field that is kept verbatim from the source.
         *
         * <p>Used for non-recursing kept fields (varint, fixed-32, fixed-64, or
         * length-delimited where the rule has no {@link ReportingField#subfield()}
         * map and {@link ReportingField#isMessage()} returns {@code false}); the
         * writer copies the tag plus the value bytes straight out of the source.
         *
         * @param fieldNumber the protobuf field number used for sort order
         * @param sourceStart the offset into the source where the tag bytes begin
         * @param length      the number of bytes spanning the tag plus the value
         */
        record Leaf(int fieldNumber, int sourceStart, int length) implements KeptNode {
            /**
             * Copies {@link #length} bytes starting at {@link #sourceStart} into
             * {@code out} at {@code cursor}.
             *
             * @param src    the original source bytes
             * @param out    the destination buffer
             * @param cursor the offset at which to start writing
             * @return the offset immediately after the written bytes
             */
            @Override
            public int write(byte[] src, byte[] out, int cursor) {
                System.arraycopy(src, sourceStart, out, cursor, length);
                return cursor + length;
            }
        }

        /**
         * Holds a length-delimited field whose body was recursively pruned.
         *
         * <p>Used for kept sub-messages: the original tag is reused verbatim, but
         * the inner length prefix is freshly emitted because the pruned body's
         * size differs from the source body's size.
         *
         * @param fieldNumber the protobuf field number used for sort order
         * @param tagStart    the offset into the source where the original tag
         *                    bytes begin
         * @param tagLength   the number of bytes spanning the original tag
         * @param innerSize   the total byte size of {@link #children} when
         *                    written
         * @param children    the kept inner nodes, sorted by field number
         */
        record Branch(int fieldNumber, int tagStart, int tagLength,
                      int innerSize, List<KeptNode> children) implements KeptNode {
            /**
             * Copies the original tag, writes a fresh length prefix for the
             * pruned body, and recursively writes each kept child.
             *
             * @param src    the original source bytes
             * @param out    the destination buffer
             * @param cursor the offset at which to start writing
             * @return the offset immediately after the written bytes
             */
            @Override
            public int write(byte[] src, byte[] out, int cursor) {
                System.arraycopy(src, tagStart, out, cursor, tagLength);
                var pos = cursor + tagLength;
                pos = writeVarInt(out, pos, innerSize);
                for (var child : children) {
                    pos = child.write(src, out, pos);
                }
                return pos;
            }
        }
    }
}
