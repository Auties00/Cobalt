package com.github.auties00.cobalt.calls.transport.srtp;

/**
 * Enumerates the two SRTP crypto suites the hop by hop relay context selects between, each pairing
 * AES counter mode 128 bit confidentiality with HMAC SHA1 authentication and differing only in the
 * authentication tag length.
 *
 * <p>The relay leg does not negotiate a suite over a handshake the way the Web P2P DTLS SRTP path
 * does; the suite is chosen locally, taking the 80 bit tag for media RTP and the 32 bit tag where the
 * shorter tag is wanted. Both suites use a 16 byte master key and a 14 byte master salt, so the master
 * that key derivation hands out is {@value #SUITE_MASTER_LENGTH} bytes for either suite; only the
 * trailing authentication tag on the wire differs.
 *
 * @implNote This implementation maps each constant onto the matching
 *           {@link com.github.auties00.srtp.SrtpCryptoSuite} of the pure Java SRTP library when
 *           {@link LiveHbhSrtpRelay} builds the relay leg.
 */
public enum SrtpCryptoSuite {
    /**
     * The AES counter mode 128 bit cipher with an 80 bit HMAC SHA1 authentication tag, the suite the
     * relay context uses for media RTP.
     */
    AES_CM_128_HMAC_SHA1_80,

    /**
     * The AES counter mode 128 bit cipher with a 32 bit HMAC SHA1 authentication tag, the suite the
     * relay context uses where the shorter four byte tag is preferred over the eight byte tag.
     */
    AES_CM_128_HMAC_SHA1_32;

    /**
     * Holds the length, in bytes, of the master both suites consume: a 16 byte AES 128 key immediately
     * followed by a 14 byte master salt.
     *
     * <p>Key derivation hands out a master of exactly this length, which {@link LiveHbhSrtpRelay}
     * splits into the 16 byte key and 14 byte salt it passes to the SRTP library.
     */
    public static final int SUITE_MASTER_LENGTH = 30;
}
