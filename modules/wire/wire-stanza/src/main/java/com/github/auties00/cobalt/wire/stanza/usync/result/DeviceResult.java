package com.github.auties00.cobalt.wire.stanza.usync.result;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the success result of the device USync parser.
 *
 * Surfaced by USync queries that request the device protocol, such as the ADV
 * device-list sync that keeps companion-device fan-out in sync, the background
 * contact sync, and developer tooling. Carries the peer's full device list
 * and, when the relay also returns a {@code <key-index-list>} child, the
 * signed key-index metadata used by the ADV verifier.
 */
@WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
public final class DeviceResult implements UsyncProtocolResponse {
    /**
     * Holds the list of devices linked to the peer.
     *
     * Defaults to the empty list when the relay did not return a
     * {@code <device-list>} child.
     */
    private final List<Device> devices;

    /**
     * Holds the peer's signed key-index metadata.
     *
     * Is {@code null} when the {@code <key-index-list>} child is absent.
     */
    private final KeyIndex keyIndex;

    /**
     * Creates a new device result.
     *
     * @param devices  the {@link Device} list, or {@code null} for an empty
     *                 list
     * @param keyIndex the {@link KeyIndex} metadata, or {@code null}
     */
    public DeviceResult(List<Device> devices, KeyIndex keyIndex) {
        this.devices = devices == null ? List.of() : List.copyOf(devices);
        this.keyIndex = keyIndex;
    }

    /**
     * Returns the device list.
     *
     * Each entry is the wire-level
     * {@code <device id="..." key-index="..." is_hosted="..."/>} child,
     * consumed by the ADV device-list sync to refresh the per-peer Signal
     * session fan-out.
     *
     * @return the list, never {@code null}
     */
    public List<Device> devices() {
        return devices;
    }

    /**
     * Returns the signed key-index metadata, when present.
     *
     * Used by the ADV verifier to confirm that the device list was signed by
     * the peer's primary device. Absent for peers that have not enrolled in
     * the signed-key-index scheme yet.
     *
     * @return the {@link KeyIndex}, or empty when absent
     */
    public Optional<KeyIndex> keyIndex() {
        return Optional.ofNullable(keyIndex);
    }

    /**
     * Holds one device in the peer's {@code <device-list>}.
     *
     * Each device entry is the wire shape
     * {@snippet lang = xml:
     * <device id="2" key-index="1" is_hosted="true"/>
     * }
     * with {@code key-index} and {@code is_hosted} both optional.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
    public static final class Device {
        /**
         * Holds the device id, a small non-negative integer where {@code 0} is
         * the primary device.
         */
        private final int id;

        /**
         * Holds the device's signed-key-index attribute value.
         *
         * Is {@code null} when the {@code key-index} attribute is absent.
         */
        private final Integer keyIndex;

        /**
         * Indicates whether the device is marked as hosted.
         *
         * Is {@code false} when the {@code is_hosted} attribute is absent.
         */
        private final boolean hosted;

        /**
         * Creates a new device entry.
         *
         * @param id       the device id
         * @param keyIndex the signed-key-index attribute, or {@code null}
         * @param hosted   the {@code is_hosted} flag
         */
        public Device(int id, Integer keyIndex, boolean hosted) {
            this.id = id;
            this.keyIndex = keyIndex;
            this.hosted = hosted;
        }

        /**
         * Returns the device id.
         *
         * Combined with the peer's JID, identifies a unique device in the
         * Signal session map.
         *
         * @return the id
         */
        public int id() {
            return id;
        }

        /**
         * Returns the signed-key-index, when present.
         *
         * Cross-referenced with the {@link KeyIndex#signedBytes() signed
         * key-index list} during ADV verification.
         *
         * @return the key index, or empty when the attribute is absent
         */
        public Optional<Integer> keyIndex() {
            return Optional.ofNullable(keyIndex);
        }

        /**
         * Returns whether the device is marked as hosted.
         *
         * Hosted devices represent business agent seats.
         *
         * @implNote
         * This implementation always exposes the wire value and leaves the
         * gating decision to the caller, whereas WA Web only honours the flag
         * when its business-coexistence gate is on.
         *
         * @return {@code true} when {@code is_hosted="true"}
         */
        public boolean hosted() {
            return hosted;
        }
    }

    /**
     * Holds the peer's signed key-index-list metadata used by the ADV
     * verifier.
     *
     * Mirrors the wire shape
     * {@snippet lang = xml:
     * <key-index-list ts="1700000000" expected_ts="1700003600">{signedBytes}</key-index-list>
     * }
     * where {@code expected_ts} and the inline content may be absent.
     */
    @WhatsAppWebModule(moduleName = "WAWebUsyncDevice")
    public static final class KeyIndex {
        /**
         * Holds the timestamp the index was signed at, decoded from the
         * {@code ts} attribute.
         */
        private final Instant timestamp;

        /**
         * Holds the raw signed protobuf bytes carried inline in the
         * {@code <key-index-list>} element.
         *
         * Is {@code null} when the element had no inline content.
         */
        private final byte[] signedBytes;

        /**
         * Holds the {@code expected_ts} attribute value.
         *
         * Is {@code null} when the attribute is absent.
         */
        private final Instant expectedTimestamp;

        /**
         * Creates a new key-index metadata block.
         *
         * @param timestamp         the signed timestamp; must not be
         *                          {@code null}
         * @param signedBytes       the inline signed bytes, or {@code null}
         * @param expectedTimestamp the {@code expected_ts} attribute, or
         *                          {@code null}
         */
        public KeyIndex(Instant timestamp, byte[] signedBytes, Instant expectedTimestamp) {
            this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
            this.signedBytes = signedBytes;
            this.expectedTimestamp = expectedTimestamp;
        }

        /**
         * Returns the signed timestamp.
         *
         * Wall-clock time the peer's primary device signed the device list at;
         * the ADV verifier checks it against the local clock to reject stale
         * lists.
         *
         * @return the timestamp, never {@code null}
         */
        public Instant timestamp() {
            return timestamp;
        }

        /**
         * Returns the inline signed bytes, when present.
         *
         * Protobuf-encoded {@code SignedKeyIndexList} payload; decoded by the
         * ADV verifier to cross-check {@link Device#keyIndex()} entries.
         *
         * @return the signed bytes, or empty when the element had no content
         */
        public Optional<byte[]> signedBytes() {
            return Optional.ofNullable(signedBytes);
        }

        /**
         * Returns the expected timestamp, when present.
         *
         * Carries the relay's view of when the next refresh should happen;
         * absent on responses that do not include the attribute.
         *
         * @return the expected timestamp, or empty when absent
         */
        public Optional<Instant> expectedTimestamp() {
            return Optional.ofNullable(expectedTimestamp);
        }
    }
}
