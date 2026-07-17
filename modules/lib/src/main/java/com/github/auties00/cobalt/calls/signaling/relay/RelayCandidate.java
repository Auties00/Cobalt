package com.github.auties00.cobalt.calls.signaling.relay;

import com.github.auties00.cobalt.wire.core.util.DataUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents one relay candidate held in a call's relay list.
 *
 * <p>A relay candidate is the in memory form of a relay server the call may use, assembled from the
 * relay information parsed out of signaling. It carries the relay identity triple that keys the
 * list, the candidate's reachable addresses, and the per relay tokens. The identity triple is the
 * {@code relay_id} byte, the {@code port_byte}, and the {@code proto_af_flag} (the protocol address
 * family flag); a {@link RelayCandidateList} deduplicates on this triple. A candidate may carry an
 * IPv4 address, an IPv6 address, or both, each with its own port; at least one address must be
 * present. The {@code auth_token} (the relay authentication token, JSON key {@code auth_tokens}) and
 * the {@code enc_relay_token} (the encrypted relay token, JSON key {@code relay_tokens}) are each at
 * most {@value #TOKEN_MAX_LENGTH} bytes.
 *
 * <p>A candidate is {@linkplain #isUnassigned() unassigned} when it holds no reachable address: no
 * IPv4 address, both ports zero, and an all zero IPv6 address. An unassigned candidate is a
 * placeholder the engine treats as carrying no usable endpoint.
 *
 * <p>This is the engine side aggregate keyed by relay identity, distinct from the {@code <relay>}
 * wire endpoint {@link RelayEndpoint}, which is the per {@code te}/{@code te2} parsed shape that
 * feeds this model.
 *
 * @implNote This implementation bounds each token at {@value #TOKEN_MAX_LENGTH} bytes to mirror the
 * fixed token capacity of the relay record on the wire.
 * @param relayId         the relay identifier byte; part of the deduplication key
 * @param portByte        the port related byte; part of the deduplication key
 * @param protoOrPref     the protocol or preference byte
 * @param protoAfFlag     the protocol address family flag; part of the deduplication key
 * @param ipv4Address     the IPv4 address, or {@code null} when the candidate has none
 * @param ipv4Port        the IPv4 port in the range {@code 0..65535}, meaningful only when
 *                        {@code ipv4Address} is present
 * @param ipv6Address     the IPv6 address, or {@code null} when the candidate has none
 * @param ipv6Port        the IPv6 port in the range {@code 0..65535}, meaningful only when
 *                        {@code ipv6Address} is present
 * @param authToken       the relay authentication token, at most {@value #TOKEN_MAX_LENGTH} bytes;
 *                        never {@code null}, possibly empty
 * @param encRelayToken   the encrypted relay token, at most {@value #TOKEN_MAX_LENGTH} bytes; never
 *                        {@code null}, possibly empty
 * @see RelayCandidateList
 * @see RelayEndpoint
 */
public record RelayCandidate(int relayId,
                             int portByte,
                             int protoOrPref,
                             int protoAfFlag,
                             Inet4Address ipv4Address,
                             int ipv4Port,
                             Inet6Address ipv6Address,
                             int ipv6Port,
                             byte[] authToken,
                             byte[] encRelayToken) {
    /**
     * The maximum length of the {@code auth_token} and {@code enc_relay_token} byte arrays.
     */
    public static final int TOKEN_MAX_LENGTH = 0x30;

    /**
     * Canonicalizes the record components, validating the addresses and token lengths and defensively
     * copying the token arrays.
     *
     * <p>At least one of {@code ipv4Address} or {@code ipv6Address} must be present; a candidate with
     * neither address is rejected because it names no reachable endpoint. A {@code null} token is
     * normalized to an empty array. Each token must be at most {@value #TOKEN_MAX_LENGTH} bytes.
     *
     * @throws IllegalArgumentException if both addresses are absent, or if either token exceeds
     *                                  {@value #TOKEN_MAX_LENGTH} bytes
     */
    public RelayCandidate {
        if (ipv4Address == null && ipv6Address == null) {
            throw new IllegalArgumentException("relay candidate must carry an IPv4 or IPv6 address");
        }
        authToken = normalizeToken(authToken, "authToken");
        encRelayToken = normalizeToken(encRelayToken, "encRelayToken");
    }

    /**
     * Returns the IPv4 address of this candidate, if present.
     *
     * @return an {@link Optional} holding the IPv4 address, or empty when the candidate has none
     */
    public Optional<Inet4Address> ipv4AddressValue() {
        return Optional.ofNullable(ipv4Address);
    }

    /**
     * Returns the IPv6 address of this candidate, if present.
     *
     * @return an {@link Optional} holding the IPv6 address, or empty when the candidate has none
     */
    public Optional<Inet6Address> ipv6AddressValue() {
        return Optional.ofNullable(ipv6Address);
    }

    /**
     * Returns the relay authentication token bytes backing this candidate.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the {@code auth_token} bytes; never {@code null}, possibly empty
     */
    @Override
    public byte[] authToken() {
        return authToken.clone();
    }

    /**
     * Returns the encrypted relay token bytes backing this candidate.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the stored
     * array cannot be mutated through the returned reference.
     *
     * @return a copy of the {@code enc_relay_token} bytes; never {@code null}, possibly empty
     */
    @Override
    public byte[] encRelayToken() {
        return encRelayToken.clone();
    }

    /**
     * Returns whether this candidate holds no reachable address.
     *
     * <p>A candidate is unassigned when it carries no IPv4 address, both its IPv4 and IPv6 ports are
     * zero, and its IPv6 address is absent or all zero. The engine treats an unassigned candidate as a
     * placeholder with no usable endpoint.
     *
     * @return {@code true} when the candidate names no reachable endpoint
     */
    public boolean isUnassigned() {
        return ipv4Address == null
                && ipv4Port == 0
                && ipv6Port == 0
                && (ipv6Address == null || isAllZero(ipv6Address.getAddress()));
    }

    /**
     * Normalizes a token argument, defaulting {@code null} to an empty array and bounding the length.
     *
     * @param token the token bytes, or {@code null}
     * @param name  the component name for the diagnostic message
     * @return a defensive copy of the token bytes, or an empty array when {@code token} is {@code null}
     * @throws IllegalArgumentException if {@code token} exceeds {@value #TOKEN_MAX_LENGTH} bytes
     */
    private static byte[] normalizeToken(byte[] token, String name) {
        if (token == null) {
            return DataUtils.EMPTY_BYTE_ARRAY;
        }
        if (token.length > TOKEN_MAX_LENGTH) {
            throw new IllegalArgumentException(name + " exceeds " + TOKEN_MAX_LENGTH + " bytes: " + token.length);
        }
        return token.clone();
    }

    /**
     * Returns whether every byte of the given array is zero.
     *
     * @param bytes the bytes to test; never {@code null}
     * @return {@code true} when all bytes are zero
     */
    private static boolean isAllZero(byte[] bytes) {
        for (var b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof RelayCandidate that
                && this.relayId == that.relayId
                && this.portByte == that.portByte
                && this.protoOrPref == that.protoOrPref
                && this.protoAfFlag == that.protoAfFlag
                && this.ipv4Port == that.ipv4Port
                && this.ipv6Port == that.ipv6Port
                && Objects.equals(this.ipv4Address, that.ipv4Address)
                && Objects.equals(this.ipv6Address, that.ipv6Address)
                && Arrays.equals(this.authToken, that.authToken)
                && Arrays.equals(this.encRelayToken, that.encRelayToken));
    }

    @Override
    public int hashCode() {
        return Objects.hash(relayId, portByte, protoOrPref, protoAfFlag, ipv4Address, ipv4Port,
                ipv6Address, ipv6Port, Arrays.hashCode(authToken), Arrays.hashCode(encRelayToken));
    }

    @Override
    public String toString() {
        return "RelayCandidate[relayId=" + relayId + ", portByte=" + portByte
                + ", protoAfFlag=" + protoAfFlag
                + ", ipv4=" + (ipv4Address == null ? null : ipv4Address.getHostAddress() + ':' + ipv4Port)
                + ", ipv6=" + (ipv6Address == null ? null : '[' + ipv6Address.getHostAddress() + "]:" + ipv6Port)
                + ", authTokenLen=" + authToken.length
                + ", encRelayTokenLen=" + encRelayToken.length + ']';
    }
}
