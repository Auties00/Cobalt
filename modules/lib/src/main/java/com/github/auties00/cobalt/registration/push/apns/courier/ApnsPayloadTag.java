package com.github.auties00.cobalt.registration.push.apns.courier;

/**
 * Enumerates the one-byte type codes that prefix every APNS courier frame on the
 * wire.
 *
 * <p>The constant names match the labels Apple's push daemon ({@code apsd}) uses
 * internally and the values are the bytes observed on the {@code apns-security-v3}
 * protocol. {@link ApnsPacket#tag()} carries the resolved tag of a decoded packet,
 * and the courier connection's send path uses {@link #value()} to label outbound
 * frames.
 *
 * @implNote This implementation backs {@link #of(int)} with a wire-byte indexed
 * array sized to the largest declared tag value, so lookup is branchless after the
 * bounds check. Unknown wire bytes resolve to {@code null} rather than throwing,
 * letting the read pump log and skip them without tearing down the courier
 * connection.
 */
public enum ApnsPayloadTag {
    /**
     * Identifies an outbound client login, sent immediately after the TLS handshake
     * completes.
     *
     * <p>The packet carries the device certificate, the connect-time nonce and the
     * nonce signature. The courier replies with {@link #READY} on success.
     */
    CONNECT(0x07),
    /**
     * Identifies the inbound response to {@link #CONNECT}.
     *
     * <p>The packet carries a one-byte status (zero on success) and the auth token
     * the courier expects to see echoed back in every subsequent authenticated
     * request.
     */
    READY(0x08),
    /**
     * Identifies an outbound topic-subscription packet.
     *
     * <p>The packet carries the SHA-1 hashes of the bundle ids the client wants
     * pushes for; the courier delivers only notifications whose topic hash appears
     * in this set.
     */
    FILTER(0x09),
    /**
     * Identifies an inbound delivered push.
     *
     * <p>The packet carries the notification id and the application payload (often a
     * JSON object). The client must reply with {@link #ACK}.
     */
    NOTIFICATION(0x0A),
    /**
     * Identifies an outbound ack for a delivered {@link #NOTIFICATION}.
     *
     * <p>The packet echoes the notification id and a status byte; the courier
     * resends the notification if the ack is missing.
     */
    ACK(0x0B),
    /**
     * Identifies an outbound keep-alive ping sent on a fixed cadence.
     *
     * <p>Cobalt emits one every five seconds to match the value the native
     * {@code apsd} uses for keeping NAT entries alive on cellular networks.
     */
    KEEP_ALIVE_SEND(0x0C),
    /**
     * Identifies the inbound ack to a {@link #KEEP_ALIVE_SEND}.
     */
    KEEP_ALIVE_ACK(0x0D),
    /**
     * Identifies an inbound notice that the courier has no spare storage.
     *
     * <p>Cobalt does not currently react to this; the read pump logs and drops it.
     */
    NO_STORAGE(0x0E),
    /**
     * Identifies an outbound request for the push token of a bundle id.
     *
     * <p>The packet carries the auth token from {@link #READY} and the SHA-1 hash of
     * the bundle id; the courier replies with {@link #TOKEN_RESPONSE}.
     */
    GET_TOKEN(0x11),
    /**
     * Identifies the inbound response to {@link #GET_TOKEN}.
     *
     * <p>The packet carries the 32-byte push token and the topic hash so callers can
     * correlate the response against their original request.
     */
    TOKEN_RESPONSE(0x12),
    /**
     * Identifies an outbound presence and idle-state announcement.
     *
     * <p>The packet is sent once after {@link #READY} so the courier knows the
     * client is active and how much storage it can buffer.
     */
    STATE(0x14),
    /**
     * Identifies an outbound pub/sub control.
     *
     * <p>The WhatsApp registration flow Cobalt impersonates does not use this; it is
     * declared so unknown inbound bytes still resolve to a meaningful tag if Apple's
     * wire format expands.
     */
    PUB_SUB(0x1D),
    /**
     * Identifies the inbound pub/sub response paired with {@link #PUB_SUB}.
     */
    PUB_SUB_RESPONSE(0x20);

    /**
     * Holds the wire-byte indexed lookup table populated once on class load.
     *
     * <p>The table is indexed directly by the byte read off the wire; slots without
     * a declared tag hold {@code null}.
     *
     * @implNote This implementation sizes the table to {@code max + 1} where
     * {@code max} is the largest declared value, trading a few unused slots for
     * branchless lookup in {@link #of(int)}.
     */
    private static final ApnsPayloadTag[] BY_VALUE = buildLookup();

    /**
     * Holds the wire byte that identifies this tag.
     */
    private final int value;

    /**
     * Constructs a tag bound to a wire byte.
     *
     * @param value the wire byte
     */
    ApnsPayloadTag(int value) {
        this.value = value;
    }

    /**
     * Returns the wire byte this tag is encoded as.
     *
     * @return the wire byte
     */
    public int value() {
        return value;
    }

    /**
     * Resolves a wire byte to the matching tag.
     *
     * <p>The courier read pump uses this to classify every decoded frame. It returns
     * {@code null} when the byte is not one of the declared tags so callers can log
     * and skip an unknown packet rather than tear down the connection.
     *
     * @param value the wire tag byte
     * @return the matching tag, or {@code null} if {@code value} is out of range or
     *         has no declared tag
     * @implNote This implementation routes through the cached {@link #BY_VALUE} table
     * for {@code O(1)} lookup.
     */
    public static ApnsPayloadTag of(int value) {
        if (value < 0 || value >= BY_VALUE.length) {
            return null;
        }
        return BY_VALUE[value];
    }

    /**
     * Builds the wire-byte indexed lookup table.
     *
     * <p>This is called once during class initialization to populate
     * {@link #BY_VALUE}.
     *
     * @return the populated lookup array
     * @implNote This implementation sizes the array to {@code max(value) + 1} across
     * all declared tags and leaves intermediate slots {@code null}.
     */
    private static ApnsPayloadTag[] buildLookup() {
        var max = 0;
        for (var tag : values()) {
            if (tag.value > max) {
                max = tag.value;
            }
        }
        var table = new ApnsPayloadTag[max + 1];
        for (var tag : values()) {
            table[tag.value] = tag;
        }
        return table;
    }
}
