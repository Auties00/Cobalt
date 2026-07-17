package com.github.auties00.cobalt.stanza.binary;

import java.util.Arrays;

import static com.github.auties00.cobalt.stanza.binary.StanzaTags.HEX_8;
import static com.github.auties00.cobalt.stanza.binary.StanzaTags.NIBBLE_8;

/**
 * Holds the shared lookup tables and classifier for the 4-bit-per-character
 * packed string encodings used by {@link StanzaTags#NIBBLE_8} and
 * {@link StanzaTags#HEX_8}.
 *
 * <p>The classifier in {@link #getPackedType(String)} returns the nibble or
 * hex tag for a string whose alphabet fits one of the two compact encodings,
 * and {@code -1} otherwise. Both {@link StanzaSizer} and {@link StanzaWriter}
 * delegate here so that sizing and writing stay in lockstep:
 * {@link StanzaWriter#writePacked(String, byte)} reuses the same
 * {@link #NIBBLE_ENCODE} and {@link #HEX_ENCODE} arrays to emit the actual
 * bytes without a second classification pass.
 *
 * @implNote
 * This implementation pre-computes two reverse alphabets in static arrays
 * sized to the ASCII range so encoding can index by character code in O(1),
 * rather than running the two regex patterns ({@code [^0-9.-]+?} and
 * {@code [^0-9A-F]+?}) that the source matches against the string.
 */
final class StanzaPackedFormat {

    /**
     * Maps an ASCII character to its 4-bit nibble code for
     * {@link StanzaTags#NIBBLE_8}, or to {@code -1} when the character is
     * outside the {@code [0-9.-]} alphabet.
     *
     * <p>The dash maps to {@code 10} and the dot maps to {@code 11}; every
     * remaining slot is the {@code -1} sentinel.
     */
    static final byte[] NIBBLE_ENCODE = new byte[128];

    /**
     * Maps an ASCII character to its 4-bit hex code for
     * {@link StanzaTags#HEX_8}, or to {@code -1} when the character is outside
     * the {@code [0-9A-F]} alphabet.
     *
     * <p>Uppercase only; lowercase hex digits resolve to {@code -1} and fall
     * through to the length-prefixed UTF-8 shape.
     */
    static final byte[] HEX_ENCODE = new byte[128];

    static {
        Arrays.fill(NIBBLE_ENCODE, (byte) -1);
        Arrays.fill(HEX_ENCODE, (byte) -1);
        for (var i = 0; i <= 9; i++) {
            NIBBLE_ENCODE['0' + i] = (byte) i;
            HEX_ENCODE['0' + i] = (byte) i;
        }
        NIBBLE_ENCODE['-'] = 10;
        NIBBLE_ENCODE['.'] = 11;
        for (var i = 0; i < 6; i++) {
            HEX_ENCODE['A' + i] = (byte) (10 + i);
        }
    }

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private StanzaPackedFormat() {
        throw new AssertionError();
    }

    /**
     * Returns the tag byte for the most compact packed encoding that accepts
     * every character of {@code input}, or {@code -1} when neither packed
     * shape applies.
     *
     * <p>Nibble wins over hex when the string fits both alphabets, so numeric
     * ids and short dotted decimals take {@link StanzaTags#NIBBLE_8} while the
     * remaining {@code [0-9A-F]} strings take {@link StanzaTags#HEX_8}. A
     * {@code -1} return tells the caller to fall back to the length-prefixed
     * UTF-8 shape. Callers reach this method only after a string has been
     * ruled out of the single-byte and dictionary token tables and its UTF-8
     * length is below 128 bytes.
     *
     * @implNote
     * This implementation walks the string once, tracking eligibility for
     * both alphabets in parallel, and short-circuits as soon as both are
     * ruled out. Characters above {@code 0x7F} are rejected because the
     * lookup tables are sized to the ASCII range only.
     *
     * @param input the string to classify
     * @return {@link StanzaTags#NIBBLE_8} when every character fits the nibble
     *         alphabet, {@link StanzaTags#HEX_8} when every character fits the
     *         hex alphabet but not the nibble one, or {@code -1} when neither
     *         shape applies
     */
    static byte getPackedType(String input) {
        var nibble = true;
        var hex = true;
        for (var i = 0; i < input.length(); i++) {
            var ch = input.charAt(i);
            if (ch >= 128 || NIBBLE_ENCODE[ch] < 0) {
                nibble = false;
            }
            if (ch >= 128 || HEX_ENCODE[ch] < 0) {
                hex = false;
            }
            if (!nibble && !hex) {
                return -1;
            }
        }
        if (nibble) {
            return NIBBLE_8;
        } else {
            return HEX_8;
        }
    }
}
