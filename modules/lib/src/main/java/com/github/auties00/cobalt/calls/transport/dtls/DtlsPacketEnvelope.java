package com.github.auties00.cobalt.calls.transport.dtls;

import com.github.auties00.cobalt.exception.linked.WhatsAppCallException;

import java.util.Objects;

/**
 * Carries one opaque DTLS or SCTP over DTLS record across the data channel to transport seam, tagging
 * it with the {@code sockaddr_conn} discriminator the native engine validates before it ships the bytes
 * out the elected relay.
 *
 * <p>On the WhatsApp Web build the data channel controller hands the transport an outbound DTLS record
 * wrapped in a synthetic {@code sockaddr_conn}: the address {@code family} is set to {@code 2} (the
 * {@code AF_CONN} marker the voip engine reuses as its DTLS tag) and the address length to {@code 0x10},
 * and the transport refuses any packet whose family or length does not match before forwarding it. This
 * record reproduces that envelope. It is the unit of exchange between the Web P2P DTLS bridge (which
 * produces the DTLS bytes) and the datagram sink (which puts them on the wire). The DTLS records the relay
 * leg ships do not carry this synthetic {@code sockaddr_conn} on the wire; it is the in memory
 * discriminator the native engine validates before a send, modelled here so a packet decoded from a
 * foreign source round trips it, while {@link #relayPacket()} yields just the bare DTLS record bytes.
 *
 * <p>The {@link #payload() payload} is the raw DTLS record bytes and is defensively copied on
 * construction and on read so the envelope is immutable. The {@link #family() family} and
 * {@link #addressLength() addressLength} are validated against the fixed {@link #DTLS_SOCKADDR_FAMILY}
 * and {@link #DTLS_SOCKADDR_LEN} the engine requires; a mismatch is rejected before the envelope is built.
 *
 * @param family        the {@code sockaddr_conn} address family discriminator; must equal
 *                      {@link #DTLS_SOCKADDR_FAMILY}
 * @param addressLength the {@code sockaddr_conn} address length; must equal {@link #DTLS_SOCKADDR_LEN}
 * @param payload       the opaque DTLS record bytes; never {@code null}
 * @implNote This implementation keeps the fixed value discriminator fields as record components rather
 * than implicit constants so an envelope decoded from a foreign source round trips its declared
 * discriminator and the {@link #of(byte[])} factory can validate it; {@link #relayPacket()} returns just
 * the payload because the relay transport ships the DTLS record bytes verbatim with no sockaddr prefix.
 */
public record DtlsPacketEnvelope(int family, int addressLength, byte[] payload) {
    /**
     * Holds the {@code sockaddr_conn} address family value that marks a DTLS packet for the transport.
     *
     * @implNote This implementation uses {@code 2}, the {@code AF_CONN} style family value the voip engine
     * overloads as its tag meaning the wrapped record is a DTLS packet.
     */
    public static final int DTLS_SOCKADDR_FAMILY = 2;

    /**
     * Holds the {@code sockaddr_conn} address length value that marks a DTLS packet for the transport.
     *
     * @implNote This implementation uses {@code 0x10}: the synthetic address occupies sixteen bytes
     * regardless of the underlying family.
     */
    public static final int DTLS_SOCKADDR_LEN = 0x10;

    /**
     * Validates the discriminator fields and defensively copies the payload.
     *
     * @throws NullPointerException              if {@code payload} is {@code null}
     * @throws WhatsAppCallException.DataChannel if {@code family} is not {@link #DTLS_SOCKADDR_FAMILY} or
     *                                           {@code addressLength} is not {@link #DTLS_SOCKADDR_LEN}
     */
    public DtlsPacketEnvelope {
        Objects.requireNonNull(payload, "payload cannot be null");
        if (family != DTLS_SOCKADDR_FAMILY) {
            throw new WhatsAppCallException.DataChannel(
                    "DTLS envelope family must be " + DTLS_SOCKADDR_FAMILY + " but was " + family);
        }
        if (addressLength != DTLS_SOCKADDR_LEN) {
            throw new WhatsAppCallException.DataChannel(
                    "DTLS envelope address length must be 0x10 but was 0x" + Integer.toHexString(addressLength));
        }
        payload = payload.clone();
    }

    /**
     * Wraps the given DTLS record bytes in an envelope carrying the fixed transport discriminator.
     *
     * <p>The {@link #DTLS_SOCKADDR_FAMILY} and {@link #DTLS_SOCKADDR_LEN} values are supplied
     * automatically, so the caller passes only the record; this is the factory the Web P2P DTLS bridge
     * uses to envelope an outbound DTLS record before it reaches the relay datagram sink.
     *
     * @param payload the opaque DTLS record bytes
     * @return a new envelope wrapping a copy of {@code payload}
     * @throws NullPointerException if {@code payload} is {@code null}
     */
    public static DtlsPacketEnvelope of(byte[] payload) {
        return new DtlsPacketEnvelope(DTLS_SOCKADDR_FAMILY, DTLS_SOCKADDR_LEN, payload);
    }

    /**
     * Returns a defensive copy of the opaque DTLS record bytes.
     *
     * @return a fresh copy of the payload the caller owns
     */
    @Override
    public byte[] payload() {
        return payload.clone();
    }

    /**
     * Returns the bytes to put on the relay transport, which is the DTLS record without any sockaddr
     * prefix.
     *
     * <p>The {@link #family()} and {@link #addressLength()} are an in memory discriminator the native
     * code reads from the synthetic {@code sockaddr_conn}; they are never serialised, so the bytes a
     * relay datagram carries are exactly the {@link #payload() payload}.
     *
     * @return a fresh copy of the DTLS record bytes ready for the relay datagram sink
     */
    public byte[] relayPacket() {
        return payload.clone();
    }
}
