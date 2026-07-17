package com.github.auties00.cobalt.sync.key;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKey;
import com.github.auties00.cobalt.wire.linked.message.system.appstate.AppStateSyncKeyId;
import com.github.auties00.cobalt.wire.linked.props.ABProp;
import com.github.auties00.cobalt.props.ABPropsService;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collection;

/**
 * Provides the byte-level conversions and gating-{@link ABProp} accessors shared across the
 * syncd subsystem.
 *
 * <p>The class holds no state: every method is a pure function of its arguments. Two
 * families of helpers live here. Buffer-level helpers (concatenate, split, hex round-trip,
 * 64-bit network-order encoding, key-id structure parsing) and constant-time comparison
 * cover the cryptographic plumbing the syncd MAC and key-id flows need. AB-prop accessors
 * read the syncd gating values ({@link ABProp#SYNCD_KEY_MAX_USE_DAYS},
 * {@link ABProp#SYNCD_INLINE_MUTATIONS_MAX_COUNT}, and the rest) consumed by the rotation,
 * request-builder, and timeout flows.
 *
 * <p>A sync key id is a 6-byte big-endian buffer:
 * <ul>
 * <li>Bytes 0-1 ({@code uint16}): device id of the key creator
 * <li>Bytes 2-5 ({@code uint32}): key epoch (monotonically increasing)
 * </ul>
 *
 * <p>The class is {@code final} with a private constructor so the stateless-helper contract
 * is enforced by construction.
 */
@WhatsAppWebModule(moduleName = "WAWebSyncdCryptoUtils")
@WhatsAppWebModule(moduleName = "WASyncdKeyManagementUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdGatingUtils")
@WhatsAppWebModule(moduleName = "WAWebSyncdRotateKey")
@WhatsAppWebModule(moduleName = "WAWebSyncdKeyManagement")
public final class SyncKeyUtils {
    /**
     * Holds the byte length of a sync key id, the sum of the {@code uint16} device id and the
     * {@code uint32} epoch.
     */
    private static final int KEY_ID_LENGTH = 6;

    /**
     * Prevents instantiation of this stateless helper.
     */
    private SyncKeyUtils() {
    }

    /**
     * Concatenates the supplied byte arrays into a single fresh array.
     *
     * <p>The inputs are copied in argument order into one newly allocated array. An empty
     * argument list is rejected with an {@link IllegalArgumentException}. When exactly one
     * array is supplied that array is returned verbatim without allocating a copy.
     *
     * @implNote This implementation returns the sole input verbatim when {@code buffers.length}
     * is {@code 1}; for multi-input calls one pass computes the total length and a second
     * copies the segments in.
     *
     * @param buffers the byte arrays to concatenate
     * @return a single byte array containing the inputs in order
     * @throws IllegalArgumentException when {@code buffers} is empty
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "combine", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] combine(byte[]... buffers) {
        if (buffers.length == 0) {
            throw new IllegalArgumentException("buffers length is zero");
        }

        if (buffers.length == 1) {
            return buffers[0];
        }

        var totalLength = 0;
        for (var buffer : buffers) {
            totalLength += buffer.length;
        }

        var result = new byte[totalLength];
        var offset = 0;
        for (var buffer : buffers) {
            System.arraycopy(buffer, 0, result, offset, buffer.length);
            offset += buffer.length;
        }
        return result;
    }

    /**
     * Slices a byte array into three independent segments at the given offsets.
     *
     * <p>The result is always a three-element array in {@code [prefix, middle, suffix]} order:
     * the prefix spans {@code [0, offset)}, the middle spans {@code [offset, offset + length)},
     * and the suffix spans {@code [offset + length, buffer.length)}. A negative {@code offset}
     * or {@code length} is rejected. Each segment is a fresh copy detached from {@code buffer}.
     *
     * @implNote This implementation uses {@link Arrays#copyOfRange(byte[], int, int)} so each
     * slice is an independent copy rather than a view aliasing {@code buffer}.
     *
     * @param buffer the byte array to split
     * @param offset the start offset of the middle segment
     * @param length the length of the middle segment
     * @return a three-element array in {@code [prefix, middle, suffix]} order
     * @throws IllegalArgumentException when {@code offset} or {@code length} is negative
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "split", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[][] split(byte[] buffer, int offset, int length) {
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException("buffers length is zero");
        }

        var result = new byte[3][];
        result[0] = Arrays.copyOfRange(buffer, 0, offset);
        result[1] = Arrays.copyOfRange(buffer, offset, offset + length);
        result[2] = Arrays.copyOfRange(buffer, offset + length, buffer.length);
        return result;
    }

    /**
     * Parses a space-separated unpadded hex string back into a byte array.
     *
     * <p>Each whitespace-delimited token is parsed as a radix-16 value and stored in the
     * corresponding output byte. This is the exact inverse of {@link #syncKeyIdToHex(byte[])},
     * so {@code hexToUint8Array(syncKeyIdToHex(x))} reproduces the original bytes.
     *
     * @implNote This implementation parses each token with {@link Integer#parseInt(String, int)}
     * at radix 16.
     *
     * @param hex the space-separated hex string (for example {@code "a 1f 0"})
     * @return the decoded byte array
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "hexToUint8Array", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] hexToUint8Array(String hex) {
        var parts = hex.split(" ");
        var result = new byte[parts.length];
        for (var i = 0; i < parts.length; i++) {
            result[i] = (byte) Integer.parseInt(parts[i], 16);
        }
        return result;
    }

    /**
     * Renders a sync key id as a space-separated unpadded hex string for diagnostics.
     *
     * <p>Each byte is rendered as its unpadded lowercase hex value with single-space
     * separators between bytes. A {@code null} input yields the sentinel {@code "unknown"}
     * so diagnostic call sites can render a key id without a null guard; an empty array
     * yields the empty string.
     *
     * @implNote This implementation does not zero-pad each byte: the unpadded form is the
     * canonical syncd log shape (for example {@code "a 1f 0"} rather than {@code "0a 1f 00"}).
     * For the padded, separator-free form use {@link #arrayBufferToHexPadded(byte[])}.
     *
     * @param keyId the raw key id bytes
     * @return the space-separated unpadded hex string, or {@code "unknown"} for {@code null}
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "syncKeyIdToHex", adaptation = WhatsAppAdaptation.DIRECT)
    public static String syncKeyIdToHex(byte[] keyId) {
        if (keyId == null) {
            return "unknown";
        }

        var sb = new StringBuilder();
        for (var i = 0; i < keyId.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(Integer.toHexString(keyId[i] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Renders the key id of an {@link AppStateSyncKey} as a space-separated unpadded hex
     * string.
     *
     * <p>Unwraps the optional key-id chain of {@code key} and delegates to
     * {@link #syncKeyIdToHex(byte[])}; an absent key id yields {@code "unknown"}.
     *
     * @param key the sync key
     * @return the hex string, or {@code "unknown"} when the key id is absent
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "syncKeyIdToHex", adaptation = WhatsAppAdaptation.ADAPTED)
    public static String syncKeyIdToHex(AppStateSyncKey key) {
        var keyIdBytes = key.keyId()
                .flatMap(AppStateSyncKeyId::keyId)
                .orElse(null);
        return syncKeyIdToHex(keyIdBytes);
    }

    /**
     * Renders a byte array as a zero-padded two-character-per-byte hex string with no
     * separator.
     *
     * <p>Each byte is rendered as exactly two lowercase hex characters with no delimiter, so
     * adjacent bytes stay unambiguous in HMAC diagnostic dumps. A {@code null} or empty input
     * yields the empty string. For the space-separated diagnostic form use
     * {@link #syncKeyIdToHex(byte[])}.
     *
     * @implNote This implementation pre-sizes the {@link StringBuilder} to
     * {@code data.length * 2} to avoid a re-allocation.
     *
     * @param data the byte array to render
     * @return the zero-padded hex string (for example {@code "0a1f00"})
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "arrayBufferToHexPadded", adaptation = WhatsAppAdaptation.DIRECT)
    public static String arrayBufferToHexPadded(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        var sb = new StringBuilder(data.length * 2);
        for (var b : data) {
            var hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Encodes a 32-bit unsigned value as an 8-byte big-endian buffer with the upper four bytes
     * left zero.
     *
     * <p>The value occupies the lower four bytes of the returned buffer in big-endian order;
     * the upper four bytes remain zero. This is the layout the syncd {@code valueMac} HMAC
     * input requires for the version field.
     *
     * @implNote This implementation writes only the low four bytes and leaves the high four at
     * their zero-initialised default rather than re-zeroing them.
     *
     * @param value the value to encode, treated as an unsigned 32-bit quantity
     * @return the 8-byte big-endian buffer
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "to64BitNetworkOrder", adaptation = WhatsAppAdaptation.DIRECT)
    public static byte[] to64BitNetworkOrder(long value) {
        var buffer = new byte[8];
        buffer[4] = (byte) (value >> 24);
        buffer[5] = (byte) (value >> 16);
        buffer[6] = (byte) (value >> 8);
        buffer[7] = (byte) value;
        return buffer;
    }

    /**
     * Compares two sync key ids in constant time.
     *
     * <p>Two {@code null} inputs compare equal; exactly one {@code null} input compares
     * unequal. Two non-null arrays compare equal when they are byte-for-byte identical, with
     * the comparison performed in time independent of the position of the first differing
     * byte.
     *
     * @implNote This implementation delegates to
     * {@link MessageDigest#isEqual(byte[], byte[])} for the constant-time guarantee.
     *
     * @param keyId1 the first key id bytes
     * @param keyId2 the second key id bytes
     * @return {@code true} when the byte arrays are byte-equal
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdCryptoUtils", exports = "syncKeyIdsEqual", adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean syncKeyIdsEqual(byte[] keyId1, byte[] keyId2) {
        if (keyId1 == null && keyId2 == null) {
            return true;
        }

        if (keyId1 == null || keyId2 == null) {
            return false;
        }

        return MessageDigest.isEqual(keyId1, keyId2);
    }

    /**
     * Returns the device id encoded in bytes 0-1 of a sync key id.
     *
     * <p>Reads the leading {@code uint16} that identifies which device minted the key. A
     * {@code null} key id or one shorter than {@link #KEY_ID_LENGTH} bytes yields {@code -1}.
     *
     * @param keyId the raw key id bytes, expected to be at least {@link #KEY_ID_LENGTH} bytes
     * @return the device id, or {@code -1} when {@code keyId} is {@code null} or too short
     */
    @WhatsAppWebExport(moduleName = "WASyncdKeyManagementUtils", exports = "getKeyDeviceId", adaptation = WhatsAppAdaptation.ADAPTED)
    public static int getKeyDeviceId(byte[] keyId) {
        if (keyId == null || keyId.length < KEY_ID_LENGTH) {
            return -1;
        }
        return ByteBuffer.wrap(keyId).getShort(0) & 0xFFFF;
    }

    /**
     * Returns the key epoch encoded in bytes 2-5 of a sync key id.
     *
     * <p>The epoch is the monotonically increasing rotation counter; the newest key is the one
     * with the highest epoch. A {@code null} key id or one shorter than {@link #KEY_ID_LENGTH}
     * bytes yields {@code -1}.
     *
     * @param keyId the raw key id bytes, expected to be at least {@link #KEY_ID_LENGTH} bytes
     * @return the key epoch, or {@code -1} when {@code keyId} is {@code null} or too short
     */
    public static int getKeyEpoch(byte[] keyId) {
        if (keyId == null || keyId.length < KEY_ID_LENGTH) {
            return -1;
        }
        return ByteBuffer.wrap(keyId).getInt(2);
    }

    /**
     * Returns the epoch of an {@link AppStateSyncKey}.
     *
     * <p>Unwraps the optional key-id chain of {@code key} and delegates to
     * {@link #getKeyEpoch(byte[])}; an absent or too-short key id yields {@code -1}.
     *
     * @param key the sync key
     * @return the epoch, or {@code -1} when the key id is absent or too short
     */
    public static int getKeyEpoch(AppStateSyncKey key) {
        return key.keyId()
                .flatMap(AppStateSyncKeyId::keyId)
                .map(SyncKeyUtils::getKeyEpoch)
                .orElse(-1);
    }

    /**
     * Returns the next epoch value for a key derived from the supplied predecessor.
     *
     * <p>The new epoch is one greater than the epoch encoded in {@code keyId}. The epoch is
     * the only key-id field that advances monotonically across a rotation, so the server can
     * order rotations by it.
     *
     * @param keyId the current key id bytes
     * @return the next epoch value
     */
    public static int generateNewKeyEpoch(byte[] keyId) {
        return getKeyEpoch(keyId) + 1;
    }

    /**
     * Builds a 6-byte sync key id from a device id and key epoch.
     *
     * <p>Writes the device id as the leading {@code uint16} and the epoch as the trailing
     * {@code uint32}, the inverse of {@link #getKeyDeviceId(byte[])} together with
     * {@link #getKeyEpoch(byte[])}.
     *
     * @implNote This implementation uses a default big-endian {@link ByteBuffer} so the wire
     * layout matches the device-id-then-epoch ordering of the syncd key id.
     *
     * @param deviceId the device id of the creator
     * @param keyEpoch the key epoch
     * @return the 6-byte key id
     */
    public static byte[] buildKeyId(int deviceId, int keyEpoch) {
        var buffer = ByteBuffer.allocate(KEY_ID_LENGTH);
        buffer.putShort((short) deviceId);
        buffer.putInt(keyEpoch);
        return buffer.array();
    }

    /**
     * Returns the newest key from the supplied collection, preferring the highest epoch and
     * breaking ties by the lowest device id.
     *
     * <p>The winner is the key with the greatest {@link #getKeyEpoch(AppStateSyncKey) epoch};
     * when several keys share that epoch the one with the lowest
     * {@link #getKeyDeviceId(byte[]) device id} wins. Entries without a key id are skipped
     * rather than treated as candidates. A {@code null} or empty input yields {@code null}.
     * The lowest-device-id tiebreaker is deterministic, so independently rotating clients
     * converge on the same winner.
     *
     * @implNote This implementation skips entries without a key id rather than throwing.
     *
     * @param keys the available sync keys
     * @return the newest key, or {@code null} when the input is {@code null} or empty
     */
    @WhatsAppWebExport(moduleName = "WAWebSyncdKeyManagement",
            exports = "getNewestKeyPair",
            adaptation = WhatsAppAdaptation.ADAPTED)
    public static AppStateSyncKey findNewestKey(Collection<AppStateSyncKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return null;
        }

        var maxEpoch = Integer.MIN_VALUE;
        for (var key : keys) {
            var epoch = getKeyEpoch(key);
            if (epoch > maxEpoch) {
                maxEpoch = epoch;
            }
        }

        AppStateSyncKey bestKey = null;
        var bestDeviceId = Integer.MAX_VALUE;
        for (var key : keys) {
            if (getKeyEpoch(key) != maxEpoch) {
                continue;
            }

            var keyIdBytes = key.keyId()
                    .flatMap(AppStateSyncKeyId::keyId)
                    .orElse(null);
            if (keyIdBytes == null) {
                continue;
            }

            var deviceId = getKeyDeviceId(keyIdBytes);
            if (deviceId < bestDeviceId) {
                bestDeviceId = deviceId;
                bestKey = key;
            }
        }

        return bestKey;
    }

    /**
     * Returns the configured maximum number of days a sync key may be used before rotation.
     *
     * <p>The raw {@link ABProp#SYNCD_KEY_MAX_USE_DAYS} value is returned without clamping; the
     * rotation flow clamps it inline. The default value when the AB prop is unset is
     * {@code 30}.
     *
     * @param abPropsService the AB prop source
     * @return the configured {@code syncd_key_max_use_days} value
     */
    public static int getSyncdKeyMaxUseDays(ABPropsService abPropsService) {
        return abPropsService.getInt(ABProp.SYNCD_KEY_MAX_USE_DAYS);
    }

    /**
     * Returns the configured sentinel-mutation flush timeout in seconds.
     *
     * <p>Bounds how long the syncd logout path waits for the sentinel mutation flush before
     * giving up. The default value when {@link ABProp#SYNCD_SENTINEL_TIMEOUT_SECONDS} is unset
     * is {@code 3}.
     *
     * @param abPropsService the AB prop source
     * @return the configured {@code syncd_sentinel_timeout_seconds} value
     */
    public static int getSyncdSentinelTimeoutSeconds(ABPropsService abPropsService) {
        return abPropsService.getInt(ABProp.SYNCD_SENTINEL_TIMEOUT_SECONDS);
    }

    /**
     * Returns the configured maximum number of mutations that may be sent inline in a single
     * patch before the request builder switches to an MMS upload.
     *
     * <p>Read by {@link com.github.auties00.cobalt.sync.exchange.MutationRequestBuilder}, which
     * clamps the value inline. The default value when
     * {@link ABProp#SYNCD_INLINE_MUTATIONS_MAX_COUNT} is unset is {@code 100}.
     *
     * @param abPropsService the AB prop source
     * @return the configured {@code syncd_inline_mutations_max_count} value
     */
    public static int getSyncdInlineMutationsMaxCount(ABPropsService abPropsService) {
        return abPropsService.getInt(ABProp.SYNCD_INLINE_MUTATIONS_MAX_COUNT);
    }

    /**
     * Returns the configured maximum patch protobuf size in kilobytes before the request
     * builder switches to an MMS upload.
     *
     * <p>Read by {@link com.github.auties00.cobalt.sync.exchange.MutationRequestBuilder}, which
     * clamps the value inline. The default value when
     * {@link ABProp#SYNCD_PATCH_PROTOBUF_MAX_SIZE} is unset is {@code 10}.
     *
     * @param abPropsService the AB prop source
     * @return the configured {@code syncd_patch_protobuf_max_size} value
     */
    public static int getSyncdPatchProtobufMaxSize(ABPropsService abPropsService) {
        return abPropsService.getInt(ABProp.SYNCD_PATCH_PROTOBUF_MAX_SIZE);
    }

    /**
     * Returns the configured number of days the timeout scheduler waits for a missing key
     * before raising a fatal sync exception.
     *
     * <p>Read by {@link MissingSyncKeyTimeoutScheduler} for the wait-for-key timeout. The
     * default value when {@link ABProp#SYNCD_WAIT_FOR_KEY_TIMEOUT_DAYS} is unset is {@code 7}.
     *
     * @param abPropsService the AB prop source
     * @return the configured {@code syncd_wait_for_key_timeout_days} value
     */
    public static int getSyncdWaitForKeyTimeoutDays(ABPropsService abPropsService) {
        return abPropsService.getInt(ABProp.SYNCD_WAIT_FOR_KEY_TIMEOUT_DAYS);
    }

    /**
     * Returns whether a freshly rotated key must wait for a server acknowledgment before being
     * persisted locally.
     *
     * <p>Gates the order of the rotation steps: when {@code true} the rotation broadcast is
     * sent before the local store update, so persistence only happens after the share returns.
     * The default value when
     * {@link ABProp#WA_WEB_ENABLE_SYNCD_KEY_PERSISTENCE_ONLY_AFTER_SERVER_ACK} is unset is
     * {@code false}.
     *
     * @param abPropsService the AB prop source
     * @return {@code true} when key persistence should wait for a server acknowledgment
     */
    public static boolean getEnableSyncdKeyPersistenceOnlyAfterServerAck(ABPropsService abPropsService) {
        return abPropsService.getBool(ABProp.WA_WEB_ENABLE_SYNCD_KEY_PERSISTENCE_ONLY_AFTER_SERVER_ACK);
    }
}
