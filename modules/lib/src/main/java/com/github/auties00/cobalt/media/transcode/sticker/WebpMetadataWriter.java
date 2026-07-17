package com.github.auties00.cobalt.media.transcode.sticker;

import com.github.auties00.cobalt.exception.linked.WhatsAppMediaException;
import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.util.DataUtils;
import com.alibaba.fastjson2.JSON;

import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * Appends a sticker-metadata EXIF chunk to an extended WebP file.
 *
 * <p>This type is invoked by {@link StickerPipeline} after libwebp has produced a 512x512 extended-WebP encode of the
 * source image. The metadata payload is the sticker descriptor dictionary WhatsApp ships on the wire (publisher, pack
 * id, emoji list, accessibility text, sticker-maker source type and so on); {@link StickerPipeline#run(
 * com.github.auties00.cobalt.wire.linked.media.MediaProvider, java.nio.channels.SeekableByteChannel, Map)} documents the
 * canonical field set. The descriptor is serialised to JSON and embedded as a new EXIF RIFF chunk appended to the end
 * of the input stream.
 *
 * @implNote This implementation embeds the metadata as an EXIF RIFF chunk holding a minimal little-endian TIFF header
 * with a single private tag (id {@value #WA_PRIVATE_TIFF_TAG}, type {@value #TIFF_TYPE_UNDEFINED}) whose value is a
 * UTF-8 JSON serialisation of the descriptor dictionary. The input must already be an extended WebP whose body begins
 * with {@code RIFF<size>WEBPVP8X...}; the same precondition holds on the WhatsApp Web path. WhatsApp Web does not set
 * the EXIF flag bit in the VP8X chunk header and neither does this implementation, because the WhatsApp decoder reads
 * the EXIF chunk by FourCC regardless of the flag.
 */
@WhatsAppWebModule(moduleName = "WAWebAddWebpMetadata")
final class WebpMetadataWriter {
    /** The logger for {@link WebpMetadataWriter}. */
    private static final System.Logger LOGGER = Log.get(WebpMetadataWriter.class);

    /**
     * Length, in bytes, of the RIFF outer header: {@code "RIFF" + 4-byte size + "WEBP"}.
     *
     * @implNote This implementation uses 12, the fixed size of the RIFF outer header.
     */
    private static final int RIFF_HEADER_LENGTH = 12;

    /**
     * Length, in bytes, of a RIFF chunk header: {@code 4-byte FourCC + 4-byte size}.
     *
     * @implNote This implementation uses 8, the fixed size of a RIFF chunk header.
     */
    private static final int CHUNK_HEADER_LENGTH = 8;

    /**
     * Offset, within the RIFF outer header, of the overall file-size field.
     *
     * <p>The little-endian value written at this offset is {@code totalLength - 8}, per the RIFF specification (the
     * size field excludes the {@code "RIFF"} FourCC and the size field itself).
     *
     * @implNote This implementation uses 4, immediately after the {@code "RIFF"} FourCC.
     */
    private static final int RIFF_SIZE_OFFSET = 4;

    /**
     * FourCC of the chunk this writer emits, namely {@code "EXIF"}.
     */
    private static final byte[] EXIF_CHUNK_NAME = {'E', 'X', 'I', 'F'};

    /**
     * FourCC of the chunk that marks an extended WebP, namely {@code "VP8X"}.
     *
     * <p>The presence of this chunk immediately after the RIFF outer header is the precondition for embedding
     * metadata.
     */
    private static final byte[] VP8X_CHUNK_NAME = {'V', 'P', '8', 'X'};

    /**
     * Length, in bytes, of the TIFF wrapper preceding the JSON payload.
     *
     * @implNote This implementation uses 22: 8 bytes of TIFF header ({@code "II" + magic + first IFD offset}), 2 bytes
     * of tag count, and a single 12-byte tag entry.
     */
    private static final int TIFF_WRAPPER_LENGTH = 22;

    /**
     * Custom TIFF tag id WhatsApp reserves for the sticker descriptor payload.
     *
     * @implNote This implementation uses {@code 0x5741}, which serialises little-endian as the bytes {@code 'A' 'W'}.
     */
    private static final int WA_PRIVATE_TIFF_TAG = 0x5741;

    /**
     * TIFF tag data type {@code UNDEFINED}, which treats the tag value as opaque bytes.
     *
     * @implNote This implementation uses 7, the TIFF code for the {@code UNDEFINED} data type.
     */
    private static final int TIFF_TYPE_UNDEFINED = 7;

    /**
     * Prevents instantiation of this static-only type.
     *
     * @throws AssertionError always
     */
    private WebpMetadataWriter() {
        throw new AssertionError();
    }

    /**
     * Returns a copy of {@code source} with an EXIF chunk holding the given metadata appended to the RIFF stream.
     *
     * <p>The metadata map is keyed by the canonical WhatsApp sticker field names (for example
     * {@code "sticker-pack-publisher"}, {@code "emojis"}, {@code "is-first-party-sticker"}). The values are serialised
     * to JSON via FastJSON2 and follow the standard JSON type mapping (strings, numbers, booleans, arrays, maps). The
     * resulting chunk is appended after the existing RIFF body, an odd-length payload is zero-padded to keep the chunk
     * even-aligned, and the RIFF outer size field is rewritten to cover the enlarged file.
     *
     * @param source   an extended WebP file; its body must begin with {@code RIFF<size>WEBPVP8X...}
     * @param metadata the sticker descriptor dictionary keyed by the canonical WhatsApp field names
     * @return a fresh byte array holding the updated WebP file
     * @throws WhatsAppMediaException.Processing if {@code source} is not a valid extended WebP (missing the
     *         {@code RIFF}, {@code WEBP}, or {@code VP8X} markers)
     * @throws NullPointerException if either argument is {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebAddWebpMetadata", exports = "addWebpMetadata",
                       adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] write(byte[] source, Map<String, Object> metadata)
            throws WhatsAppMediaException.Processing {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(metadata, "metadata");
        if (!isExtendedWebp(source)) {
            if (Log.WARNING) {
                LOGGER.log(Level.WARNING, "webp metadata write rejected, source is not an extended webp, bytes={0}",
                        source.length);
            }
            throw new WhatsAppMediaException.Processing(
                    "WebP source is not an extended file (missing VP8X chunk)");
        }
        var jsonBytes = JSON.toJSONString(metadata).getBytes(StandardCharsets.UTF_8);
        var rawMetadata = buildTiffWrapper(jsonBytes);
        var needsPadding = (jsonBytes.length & 1) != 0;
        var chunkSize = CHUNK_HEADER_LENGTH + rawMetadata.length + (needsPadding ? 1 : 0);
        var totalLength = source.length + chunkSize;
        var result = new byte[totalLength];
        System.arraycopy(source, 0, result, 0, source.length);
        var cursor = source.length;
        System.arraycopy(EXIF_CHUNK_NAME, 0, result, cursor, EXIF_CHUNK_NAME.length);
        cursor += EXIF_CHUNK_NAME.length;
        DataUtils.putInt(result, cursor, rawMetadata.length, ByteOrder.LITTLE_ENDIAN);
        cursor += 4;
        System.arraycopy(rawMetadata, 0, result, cursor, rawMetadata.length);
        cursor += rawMetadata.length;
        if (needsPadding) {
            result[cursor] = 0;
        }
        DataUtils.putInt(result, RIFF_SIZE_OFFSET, totalLength - 8, ByteOrder.LITTLE_ENDIAN);
        if (Log.DEBUG) {
            LOGGER.log(Level.DEBUG, "webp metadata chunk appended, jsonBytes={0}, totalBytes={1}",
                    jsonBytes.length, totalLength);
        }
        return result;
    }

    /**
     * Builds the TIFF wrapper followed by the JSON payload that the WhatsApp private tag points at.
     *
     * <p>The returned buffer is the {@value #TIFF_WRAPPER_LENGTH}-byte wrapper concatenated with the UTF-8 JSON bytes;
     * it becomes the raw value of the appended EXIF chunk. The wrapper is a minimal little-endian TIFF stream with a
     * single IFD entry whose value points just past the wrapper, at the JSON payload.
     *
     * @implNote This implementation lays the wrapper out as follows:
     * {@snippet :
     *  // bytes 0..1   "II" (little-endian TIFF byte-order marker)
     *  // bytes 2..3   42, 0 (TIFF magic 0x002A)
     *  // bytes 4..7   first IFD offset = 8
     *  // bytes 8..9   number of tag entries = 1
     *  // bytes 10..11 tag id = 0x5741 (little-endian bytes 'A' 'W')
     *  // bytes 12..13 data type = 7 (UNDEFINED)
     *  // bytes 14..17 value count = jsonBytes.length
     *  // bytes 18..21 value offset = 22
     *  // bytes 22..   JSON UTF-8 payload
     * }
     *
     * @param jsonBytes the UTF-8 JSON serialisation of the metadata map
     * @return the concatenated TIFF wrapper and JSON payload
     */
    private static byte[] buildTiffWrapper(byte[] jsonBytes) {
        var result = new byte[TIFF_WRAPPER_LENGTH + jsonBytes.length];
        result[0] = 'I';
        result[1] = 'I';
        result[2] = 42;
        result[3] = 0;
        DataUtils.putInt(result, 4, 8, ByteOrder.LITTLE_ENDIAN);
        result[8] = 1;
        result[9] = 0;
        result[10] = (byte) (WA_PRIVATE_TIFF_TAG & 0xFF);
        result[11] = (byte) ((WA_PRIVATE_TIFF_TAG >>> 8) & 0xFF);
        result[12] = (byte) TIFF_TYPE_UNDEFINED;
        result[13] = 0;
        DataUtils.putInt(result, 14, jsonBytes.length, ByteOrder.LITTLE_ENDIAN);
        DataUtils.putInt(result, 18, 22, ByteOrder.LITTLE_ENDIAN);
        System.arraycopy(jsonBytes, 0, result, TIFF_WRAPPER_LENGTH, jsonBytes.length);
        return result;
    }

    /**
     * Reports whether the given byte array is an extended WebP carrying a VP8X chunk right after the RIFF outer header.
     *
     * <p>The first {@value #RIFF_HEADER_LENGTH} bytes must be the RIFF outer header ({@code "RIFF"}, a 4-byte size, and
     * the {@code "WEBP"} form type), and the next four bytes (the first chunk's FourCC) must be {@code "VP8X"}. The
     * sticker pipeline produces extended WebPs before invoking this writer, so the check is expected to pass.
     *
     * @param source the candidate WebP bytes
     * @return {@code true} when {@code source} is a valid extended WebP, {@code false} otherwise
     */
    private static boolean isExtendedWebp(byte[] source) {
        if (source.length < RIFF_HEADER_LENGTH + VP8X_CHUNK_NAME.length) {
            return false;
        }
        if (source[0] != 'R' || source[1] != 'I' || source[2] != 'F' || source[3] != 'F') {
            return false;
        }
        if (source[8] != 'W' || source[9] != 'E' || source[10] != 'B' || source[11] != 'P') {
            return false;
        }
        for (var i = 0; i < VP8X_CHUNK_NAME.length; i++) {
            if (source[RIFF_HEADER_LENGTH + i] != VP8X_CHUNK_NAME[i]) {
                return false;
            }
        }
        return true;
    }

}
