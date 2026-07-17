package com.github.auties00.cobalt.calls.signaling.relay;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import com.github.auties00.cobalt.calls.signaling.session.TransportStanza;

/**
 * Represents one {@code <te>} or {@code <te2>} relay endpoint parsed from a {@code <relay>} element.
 *
 * <p>The relay info reader walks the endpoint children of a {@code <relay>} block to discover the
 * relay servers a call may use. Each endpoint pairs a set of reference attributes with a packed
 * socket address: {@code relay_id} (the per relay identifier shared across address families), the
 * token reference ids ({@code token_id}, {@code xtoken_id}, {@code auth_token_id}), the round trip
 * time estimates ({@code xrtt_ms}, {@code c2r_rtt}, {@code max_peer_c2r_rtt}), the wire
 * {@code protocol} selector, and the human readable {@code relay_name} and {@code domain_name}. The
 * element content is the packed socket address the relay listens on, decoded into an
 * {@link #address() address} and {@link #port() port}.
 *
 * <p>The two element tags are mutually exclusive within one {@code <relay>}: a relay block carries
 * either a list of {@code <te>} endpoints or a list of {@code <te2>} endpoints, never both, and the
 * {@link #element() element} component records which tag this endpoint was read from so encoding it
 * back preserves it. {@code <te>} names the primary protocol family and {@code <te2>} the alternate.
 *
 * <p>The packed address content is decoded by length: a six byte blob is an IPv4 address (four
 * address bytes followed by a two byte big endian port) and an eighteen or twenty four byte blob is
 * an IPv6 address (sixteen address bytes followed by a two byte big endian port, with any trailing
 * bytes of the twenty four byte sockaddr form ignored). The decoded address and port are exposed
 * through {@link #address()} and {@link #port()}; the raw bytes are retained through
 * {@link #addressBytes()} so an unrecognized length is preserved unchanged. The same packed sockaddr
 * layout backs the transport message type 3 {@code <te>} endpoint advertise carried directly under a
 * {@code <transport>} element (a {@code <te priority="0">} with the packed address as content); it is
 * the identical codec, so no separate endpoint format exists.
 *
 * @param element        the wire element tag this endpoint was read from, {@link #ELEMENT_TE} or
 *                       {@link #ELEMENT_TE2}; never {@code null}
 * @param relayId        the {@code relay_id} attribute, the per relay identifier; never {@code null}
 * @param tokenId        the {@code token_id} attribute, or {@code -1} when absent
 * @param xtokenId       the {@code xtoken_id} attribute, or {@code -1} when absent
 * @param authTokenId    the {@code auth_token_id} attribute, or {@code -1} when absent
 * @param xrttMs         the {@code xrtt_ms} attribute, defaulting to {@code 100} when absent
 * @param protocol       the {@code protocol} attribute, defaulting to {@code 0} when absent
 * @param c2rRtt         the {@code c2r_rtt} attribute, or {@code -1} when absent
 * @param maxPeerC2rRtt  the {@code max_peer_c2r_rtt} attribute, or {@code -1} when absent
 * @param relayName      the {@code relay_name} attribute, or {@code null} when absent
 * @param domainName     the {@code domain_name} attribute, or {@code null} when absent
 * @param addressBytes   the packed socket address content bytes, or {@code null} when the endpoint
 *                       carried no content
 * @see RelayCandidate
 * @see RelayCandidateList
 * @see TransportStanza
 */
public record RelayEndpoint(String element,
                            String relayId,
                            int tokenId,
                            int xtokenId,
                            int authTokenId,
                            int xrttMs,
                            int protocol,
                            int c2rRtt,
                            int maxPeerC2rRtt,
                            String relayName,
                            String domainName,
                            byte[] addressBytes) {
    /**
     * The wire element tag naming a primary family relay endpoint.
     */
    public static final String ELEMENT_TE = "te";

    /**
     * The wire element tag naming an alternate family relay endpoint.
     */
    public static final String ELEMENT_TE2 = "te2";

    /**
     * The default value the engine assigns to an absent {@code token_id}, {@code xtoken_id},
     * {@code auth_token_id}, {@code c2r_rtt}, or {@code max_peer_c2r_rtt} attribute.
     */
    public static final int UNSET_ID = -1;

    /**
     * The default value the engine assigns to an absent {@code xrtt_ms} attribute.
     */
    public static final int DEFAULT_XRTT_MS = 100;

    /**
     * The packed address length of an IPv4 endpoint: four address bytes plus a two byte port.
     */
    private static final int IPV4_ADDRESS_LENGTH = 6;

    /**
     * The packed address length of the compact IPv6 endpoint: sixteen address bytes plus a two byte
     * port.
     */
    private static final int IPV6_ADDRESS_LENGTH = 0x12;

    /**
     * The packed address length of the extended IPv6 sockaddr endpoint: sixteen address bytes, a two
     * byte port, and trailing sockaddr fields the parser ignores.
     */
    private static final int IPV6_SOCKADDR_LENGTH = 0x18;

    /**
     * The number of address bytes preceding the port in an IPv4 packed address.
     */
    private static final int IPV4_ADDRESS_OCTETS = 4;

    /**
     * The number of address bytes preceding the port in an IPv6 packed address.
     */
    private static final int IPV6_ADDRESS_OCTETS = 16;

    /**
     * The wire attribute naming the per relay identifier.
     */
    private static final String RELAY_ID_ATTRIBUTE = "relay_id";

    /**
     * The wire attribute naming the relay token reference.
     */
    private static final String TOKEN_ID_ATTRIBUTE = "token_id";

    /**
     * The wire attribute naming the alternate relay token reference.
     */
    private static final String XTOKEN_ID_ATTRIBUTE = "xtoken_id";

    /**
     * The wire attribute naming the auth token reference.
     */
    private static final String AUTH_TOKEN_ID_ATTRIBUTE = "auth_token_id";

    /**
     * The wire attribute naming the encrypted round trip time estimate.
     */
    private static final String XRTT_MS_ATTRIBUTE = "xrtt_ms";

    /**
     * The wire attribute naming the transport protocol selector.
     */
    private static final String PROTOCOL_ATTRIBUTE = "protocol";

    /**
     * The wire attribute naming the client to relay round trip time.
     */
    private static final String C2R_RTT_ATTRIBUTE = "c2r_rtt";

    /**
     * The wire attribute naming the maximum peer client to relay round trip time.
     */
    private static final String MAX_PEER_C2R_RTT_ATTRIBUTE = "max_peer_c2r_rtt";

    /**
     * The wire attribute naming the short relay identifier.
     */
    private static final String RELAY_NAME_ATTRIBUTE = "relay_name";

    /**
     * The wire attribute naming the relay DNS name.
     */
    private static final String DOMAIN_NAME_ATTRIBUTE = "domain_name";

    /**
     * Canonicalizes the record components, validating the element tag and defensively copying the
     * address bytes.
     *
     * @throws NullPointerException     if {@code element} or {@code relayId} is {@code null}
     * @throws IllegalArgumentException if {@code element} is neither {@link #ELEMENT_TE} nor
     *                                  {@link #ELEMENT_TE2}
     */
    public RelayEndpoint {
        Objects.requireNonNull(element, "element cannot be null");
        Objects.requireNonNull(relayId, "relayId cannot be null");
        if (!element.equals(ELEMENT_TE) && !element.equals(ELEMENT_TE2)) {
            throw new IllegalArgumentException("relay endpoint element must be te or te2, was " + element);
        }
        addressBytes = addressBytes == null ? null : addressBytes.clone();
    }

    /**
     * Returns the relay token reference, if present.
     *
     * @return an {@link OptionalInt} holding the {@code token_id}, or empty when it is {@link #UNSET_ID}
     */
    public OptionalInt tokenIdValue() {
        return tokenId == UNSET_ID ? OptionalInt.empty() : OptionalInt.of(tokenId);
    }

    /**
     * Returns the alternate relay token reference, if present.
     *
     * @return an {@link OptionalInt} holding the {@code xtoken_id}, or empty when it is
     *         {@link #UNSET_ID}
     */
    public OptionalInt xtokenIdValue() {
        return xtokenId == UNSET_ID ? OptionalInt.empty() : OptionalInt.of(xtokenId);
    }

    /**
     * Returns the auth token reference, if present.
     *
     * @return an {@link OptionalInt} holding the {@code auth_token_id}, or empty when it is
     *         {@link #UNSET_ID}
     */
    public OptionalInt authTokenIdValue() {
        return authTokenId == UNSET_ID ? OptionalInt.empty() : OptionalInt.of(authTokenId);
    }

    /**
     * Returns the client to relay round trip time, if present.
     *
     * @return an {@link OptionalInt} holding the {@code c2r_rtt}, or empty when it is {@link #UNSET_ID}
     */
    public OptionalInt c2rRttValue() {
        return c2rRtt == UNSET_ID ? OptionalInt.empty() : OptionalInt.of(c2rRtt);
    }

    /**
     * Returns the maximum peer client to relay round trip time, if present.
     *
     * @return an {@link OptionalInt} holding the {@code max_peer_c2r_rtt}, or empty when it is
     *         {@link #UNSET_ID}
     */
    public OptionalInt maxPeerC2rRttValue() {
        return maxPeerC2rRtt == UNSET_ID ? OptionalInt.empty() : OptionalInt.of(maxPeerC2rRtt);
    }

    /**
     * Returns the short relay identifier, if present.
     *
     * @return an {@link Optional} holding the {@code relay_name}, or empty when absent
     */
    public Optional<String> relayNameValue() {
        return Optional.ofNullable(relayName);
    }

    /**
     * Returns the relay DNS name, if present.
     *
     * @return an {@link Optional} holding the {@code domain_name}, or empty when absent
     */
    public Optional<String> domainNameValue() {
        return Optional.ofNullable(domainName);
    }

    /**
     * Returns a defensive copy of the packed socket address content bytes.
     *
     * @return an {@link Optional} holding a copy of the address bytes, or empty when the endpoint
     *         carried no content
     */
    public Optional<byte[]> addressBytesValue() {
        return addressBytes == null ? Optional.empty() : Optional.of(addressBytes.clone());
    }

    /**
     * Returns the {@link InetAddress} decoded from the packed socket address content.
     *
     * <p>A six byte content decodes as IPv4 (the first four bytes) and an eighteen or twenty four byte
     * content decodes as IPv6 (the first sixteen bytes). A content of any other length, or an absent
     * content, yields an empty result; the raw bytes remain available through {@link #addressBytes()}.
     *
     * @return an {@link Optional} holding the decoded address, or empty when the content length is not
     *         a recognized packed address length
     */
    public Optional<InetAddress> address() {
        if (addressBytes == null) {
            return Optional.empty();
        }
        return switch (addressBytes.length) {
            case IPV4_ADDRESS_LENGTH -> toInetAddress(addressBytes, IPV4_ADDRESS_OCTETS);
            case IPV6_ADDRESS_LENGTH, IPV6_SOCKADDR_LENGTH -> toInetAddress(addressBytes, IPV6_ADDRESS_OCTETS);
            default -> Optional.empty();
        };
    }

    /**
     * Returns the port decoded from the packed socket address content.
     *
     * <p>The port is the two byte big endian value following the address octets: bytes four and five
     * for an IPv4 content, bytes sixteen and seventeen for an IPv6 content. A content of any other
     * length, or an absent content, yields an empty result.
     *
     * @return an {@link OptionalInt} holding the port in the range {@code 0..65535}, or empty when the
     *         content length is not a recognized packed address length
     */
    public OptionalInt port() {
        if (addressBytes == null) {
            return OptionalInt.empty();
        }
        return switch (addressBytes.length) {
            case IPV4_ADDRESS_LENGTH -> OptionalInt.of(readPort(addressBytes, IPV4_ADDRESS_OCTETS));
            case IPV6_ADDRESS_LENGTH, IPV6_SOCKADDR_LENGTH -> OptionalInt.of(readPort(addressBytes, IPV6_ADDRESS_OCTETS));
            default -> OptionalInt.empty();
        };
    }

    /**
     * Returns the {@link InetSocketAddress} decoded from the packed socket address content.
     *
     * <p>This fuses {@link #address()} and {@link #port()} into a single decode: it is present exactly
     * when both would be present, that is when the content length is a recognized packed address length
     * and the address octets form a valid {@link InetAddress}.
     *
     * @return an {@link Optional} holding the decoded socket address, or empty when the content is absent,
     *         of an unrecognized length, or not a valid address
     * @implNote This implementation allocates the {@link InetAddress} once rather than decoding the
     * address and the port through the separate {@link #address()} and {@link #port()} paths.
     */
    public Optional<InetSocketAddress> toSocketAddress() {
        if (addressBytes == null) {
            return Optional.empty();
        }
        var octets = switch (addressBytes.length) {
            case IPV4_ADDRESS_LENGTH -> IPV4_ADDRESS_OCTETS;
            case IPV6_ADDRESS_LENGTH, IPV6_SOCKADDR_LENGTH -> IPV6_ADDRESS_OCTETS;
            default -> -1;
        };
        if (octets < 0) {
            return Optional.empty();
        }
        return toInetAddress(addressBytes, octets)
                .map(inet -> new InetSocketAddress(inet, readPort(addressBytes, octets)));
    }

    /**
     * Builds the {@code <te>} or {@code <te2>} stanza for this endpoint.
     *
     * <p>Absent reference attributes are omitted rather than written as their sentinel; the
     * {@code xrtt_ms} attribute is always written because the engine carries an explicit default of
     * {@value #DEFAULT_XRTT_MS}. The packed address bytes become the element content when present.
     *
     * @return the relay endpoint stanza
     */
    public Stanza toNode() {
        return new StanzaBuilder()
                .description(element)
                .attribute(RELAY_ID_ATTRIBUTE, relayId)
                .attribute(TOKEN_ID_ATTRIBUTE, tokenId, tokenId != UNSET_ID)
                .attribute(XTOKEN_ID_ATTRIBUTE, xtokenId, xtokenId != UNSET_ID)
                .attribute(AUTH_TOKEN_ID_ATTRIBUTE, authTokenId, authTokenId != UNSET_ID)
                .attribute(XRTT_MS_ATTRIBUTE, xrttMs)
                .attribute(PROTOCOL_ATTRIBUTE, protocol)
                .attribute(C2R_RTT_ATTRIBUTE, c2rRtt, c2rRtt != UNSET_ID)
                .attribute(MAX_PEER_C2R_RTT_ATTRIBUTE, maxPeerC2rRtt, maxPeerC2rRtt != UNSET_ID)
                .attribute(RELAY_NAME_ATTRIBUTE, relayName)
                .attribute(DOMAIN_NAME_ATTRIBUTE, domainName)
                .content(addressBytes)
                .build();
    }

    /**
     * Returns a copy of this endpoint with the client to relay round trip hints cleared.
     *
     * <p>The {@code c2r_rtt} and {@code max_peer_c2r_rtt} attributes are reset to {@link #UNSET_ID} so
     * encoding the copy back omits them, matching the {@code <te>}/{@code <te2>} shape a callee echoes
     * in its accept; every other attribute and the packed address are preserved.
     *
     * @return a copy of this endpoint without the round trip time hints
     */
    public RelayEndpoint withoutRoundTripHints() {
        return new RelayEndpoint(element, relayId, tokenId, xtokenId, authTokenId, xrttMs, protocol,
                UNSET_ID, UNSET_ID, relayName, domainName, addressBytes);
    }

    /**
     * Decodes a {@code <te>} or {@code <te2>} stanza into a {@link RelayEndpoint}.
     *
     * <p>A stanza whose tag is neither {@link #ELEMENT_TE} nor {@link #ELEMENT_TE2} yields an empty
     * result so callers iterating a mixed child list can skip it. Absent attributes take the engine
     * defaults: the token references and round trip times default to {@link #UNSET_ID}, the encrypted
     * round trip time defaults to {@value #DEFAULT_XRTT_MS}, and the protocol defaults to {@code 0}.
     *
     * @param stanza the relay endpoint stanza
     * @return the decoded endpoint, or an empty result when the stanza is not a {@code <te>} or
     *         {@code <te2>} element
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<RelayEndpoint> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT_TE) && !stanza.hasDescription(ELEMENT_TE2)) {
            return Optional.empty();
        }
        var relayId = stanza.getAttributeAsString(RELAY_ID_ATTRIBUTE, "");
        var tokenId = stanza.getAttributeAsInt(TOKEN_ID_ATTRIBUTE, UNSET_ID);
        var xtokenId = stanza.getAttributeAsInt(XTOKEN_ID_ATTRIBUTE, UNSET_ID);
        var authTokenId = stanza.getAttributeAsInt(AUTH_TOKEN_ID_ATTRIBUTE, UNSET_ID);
        var xrttMs = stanza.getAttributeAsInt(XRTT_MS_ATTRIBUTE, DEFAULT_XRTT_MS);
        var protocol = stanza.getAttributeAsInt(PROTOCOL_ATTRIBUTE, 0);
        var c2rRtt = stanza.getAttributeAsInt(C2R_RTT_ATTRIBUTE, UNSET_ID);
        var maxPeerC2rRtt = stanza.getAttributeAsInt(MAX_PEER_C2R_RTT_ATTRIBUTE, UNSET_ID);
        var relayName = stanza.getAttributeAsString(RELAY_NAME_ATTRIBUTE, null);
        var domainName = stanza.getAttributeAsString(DOMAIN_NAME_ATTRIBUTE, null);
        var addressBytes = stanza.toContentBytes().orElse(null);
        return Optional.of(new RelayEndpoint(stanza.description(), relayId, tokenId, xtokenId, authTokenId,
                xrttMs, protocol, c2rRtt, maxPeerC2rRtt, relayName, domainName, addressBytes));
    }

    /**
     * Decodes the leading {@code octets} address bytes of a packed socket address into an
     * {@link InetAddress}.
     *
     * @param bytes  the packed socket address content
     * @param octets the number of leading address bytes, four for IPv4 or sixteen for IPv6
     * @return an {@link Optional} holding the decoded address, or empty when the bytes do not form a
     *         valid address
     */
    private static Optional<InetAddress> toInetAddress(byte[] bytes, int octets) {
        var raw = Arrays.copyOf(bytes, octets);
        try {
            return Optional.of(InetAddress.getByAddress(raw));
        } catch (UnknownHostException _) {
            return Optional.empty();
        }
    }

    /**
     * Reads the two byte big endian port that follows the address octets of a packed socket address.
     *
     * @param bytes  the packed socket address content
     * @param offset the index of the high port byte, which equals the address octet count
     * @return the port in the range {@code 0..65535}
     */
    private static int readPort(byte[] bytes, int offset) {
        return DataUtils.getShort(bytes, offset, ByteOrder.BIG_ENDIAN) & 0xffff;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayEndpoint that
                && this.element.equals(that.element)
                && this.relayId.equals(that.relayId)
                && this.tokenId == that.tokenId
                && this.xtokenId == that.xtokenId
                && this.authTokenId == that.authTokenId
                && this.xrttMs == that.xrttMs
                && this.protocol == that.protocol
                && this.c2rRtt == that.c2rRtt
                && this.maxPeerC2rRtt == that.maxPeerC2rRtt
                && Objects.equals(this.relayName, that.relayName)
                && Objects.equals(this.domainName, that.domainName)
                && Arrays.equals(this.addressBytes, that.addressBytes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, relayId, tokenId, xtokenId, authTokenId, xrttMs, protocol,
                c2rRtt, maxPeerC2rRtt, relayName, domainName, Arrays.hashCode(addressBytes));
    }

    @Override
    public String toString() {
        return "RelayEndpoint[" + element + " relayId=" + relayId
                + ", relayName=" + relayName + ", domainName=" + domainName
                + ", addressLen=" + (addressBytes == null ? 0 : addressBytes.length) + ']';
    }
}
