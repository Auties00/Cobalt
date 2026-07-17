package com.github.auties00.cobalt.stanza.binary;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

/**
 * Defines the leading tag bytes used by WhatsApp's compact binary stanza
 * protocol.
 *
 * <p>Every value in a serialised stanza is preceded by a single byte that
 * identifies its shape: an absent value, a dictionary token, a sized list, a
 * JID variant, a packed nibble or hex string, or a binary blob with an 8, 20,
 * or 32 bit length prefix. {@link StanzaWriter} and {@link StanzaReader} both
 * consume these constants directly so the two sides of the protocol cannot
 * drift apart.
 */
@WhatsAppWebModule(moduleName = "WAWap")
public final class StanzaTags {
    /**
     * Prevents instantiation of this constants holder.
     *
     * @throws AssertionError always
     */
    private StanzaTags() {
        throw new AssertionError();
    }

    /**
     * Tag byte for an empty list or an absent value.
     *
     * <p>Appears in three positions: as the entire encoding of a {@code null}
     * value, as the length placeholder of an empty binary blob (after a
     * {@link #BINARY_8} tag), and as the user-component placeholder in a
     * {@link #JID_PAIR} body whose user is absent.
     */
    public static final byte LIST_EMPTY = 0;

    /**
     * Tag byte that selects {@link StanzaTokens#DICTIONARY_0_TOKENS} for the
     * next index byte.
     *
     * <p>Encodes a two-byte token for any string in the first 256-entry
     * extension dictionary.
     */
    public static final byte DICTIONARY_0 = (byte) 236;

    /**
     * Tag byte that selects {@link StanzaTokens#DICTIONARY_1_TOKENS} for the
     * next index byte.
     *
     * <p>Encodes a two-byte token for any string in the second 256-entry
     * extension dictionary.
     */
    public static final byte DICTIONARY_1 = (byte) 237;

    /**
     * Tag byte that selects {@link StanzaTokens#DICTIONARY_2_TOKENS} for the
     * next index byte.
     *
     * <p>Encodes a two-byte token for any string in the third 256-entry
     * extension dictionary.
     */
    public static final byte DICTIONARY_2 = (byte) 238;

    /**
     * Tag byte that selects {@link StanzaTokens#DICTIONARY_3_TOKENS} for the
     * next index byte.
     *
     * <p>Encodes a two-byte token for any string in the fourth 256-entry
     * extension dictionary.
     */
    public static final byte DICTIONARY_3 = (byte) 239;

    /**
     * Domain code for the standard WhatsApp user server
     * ({@code s.whatsapp.net}) carried inside an {@link #AD_JID} body.
     *
     * <p>Emitted when the outgoing JID's server resolves to the canonical
     * user domain.
     */
    public static final int DOMAIN_WHATSAPP = 0;

    /**
     * Domain code for the linked identity server ({@code lid}) carried inside
     * an {@link #AD_JID} body.
     *
     * <p>Emitted for linked device JIDs that address the LID identity space
     * rather than the canonical phone-number space.
     */
    public static final int DOMAIN_LID = 1;

    /**
     * Domain code for the business hosted server ({@code hosted}) carried
     * inside an {@link #AD_JID} body.
     *
     * <p>The decoder also accepts any byte whose low bit is clear and high
     * bit is set as a hosted JID, so the on-the-wire value is a mask rather
     * than a strict equality.
     */
    public static final int DOMAIN_HOSTED = 128;

    /**
     * Domain code for the business hosted linked identity server
     * ({@code hosted.lid}) carried inside an {@link #AD_JID} body.
     *
     * <p>The dedicated domain for hosted business JIDs that address the LID
     * identity space.
     */
    public static final int DOMAIN_HOSTED_LID = 129;

    /**
     * Tag byte for a cross-platform interoperability JID.
     *
     * <p>The body is {@code (user, device:u16, integrator:u16, server)} where
     * {@code user} and {@code server} are themselves encoded as strings. Used
     * for JIDs that bridge WhatsApp with another Meta-owned messenger.
     */
    public static final byte JID_INTEROP = (byte) 245;

    /**
     * Tag byte for a Facebook Messenger JID.
     *
     * <p>The body is {@code (user, device:u16, server)}. Used for JIDs whose
     * server resolves to the Messenger user domain.
     */
    public static final byte JID_FB = (byte) 246;

    /**
     * Tag byte for a multi-device JID with explicit domain code.
     *
     * <p>The body is {@code (domain:u8, device:u8, user)} where {@code domain}
     * is one of the {@code DOMAIN_*} constants. This is the canonical shape
     * for any JID that carries a device suffix; the {@link #JID_PAIR} shape is
     * used only for device-less JIDs.
     */
    public static final byte AD_JID = (byte) 247;

    /**
     * Tag byte for a list whose length fits in an unsigned 8-bit value.
     *
     * <p>Appears at the top of every stanza (the size header that counts
     * description plus two slots per attribute plus the content slot) and at
     * the top of any child-list body.
     */
    public static final byte LIST_8 = (byte) 248;

    /**
     * Tag byte for a list whose length fits in an unsigned 16-bit value.
     *
     * <p>Selected by {@link StanzaWriter} when a stanza's size header or a child
     * list exceeds 255 entries.
     */
    public static final byte LIST_16 = (byte) 249;

    /**
     * Tag byte for a JID built from a {@code (user, server)} pair, with no
     * device component.
     *
     * <p>The body is {@code (user, server)} where both components are encoded
     * as strings; an absent user is replaced with the {@link #LIST_EMPTY}
     * sentinel. Used for canonical group, status, and call JIDs.
     */
    public static final byte JID_PAIR = (byte) 250;

    /**
     * Tag byte for a hex packed string with an 8-bit length prefix.
     *
     * <p>The body is {@code (lenAndOddBit:u8, payload)} where each payload
     * byte carries two hex nibbles from the {@code [0-9A-F]} alphabet.
     * Selected by {@link StanzaPackedFormat#getPackedType(String)} when a string
     * fits hex but not nibble.
     */
    public static final byte HEX_8 = (byte) 251;

    /**
     * Tag byte for a binary blob whose length fits in an unsigned 8-bit
     * value.
     *
     * <p>The body is {@code (length:u8, payload)}. Selected by
     * {@link StanzaWriter} for any UTF-8-encoded string or raw byte blob shorter
     * than 256 bytes once dictionary lookups have failed.
     */
    public static final byte BINARY_8 = (byte) 252;

    /**
     * Tag byte for a binary blob whose length fits in a 20-bit value.
     *
     * <p>The body is {@code (length:u20, payload)} stored as three bytes (the
     * top nibble of the first byte holds bits 16-19 of the length). Selected
     * when the payload exceeds 255 bytes but fits within 1 MiB.
     */
    public static final byte BINARY_20 = (byte) 253;

    /**
     * Tag byte for a binary blob whose length fits in an unsigned 32-bit
     * value.
     *
     * <p>The body is {@code (length:u32, payload)}. Selected as the
     * fall-through for payloads at or above 1 MiB.
     */
    public static final byte BINARY_32 = (byte) 254;

    /**
     * Tag byte for a nibble packed string with an 8-bit length prefix.
     *
     * <p>The body is {@code (lenAndOddBit:u8, payload)} where each payload
     * byte carries two nibbles from the {@code [0-9.-]} alphabet plus the two
     * extra glyphs (dash at 10, dot at 11). Selected by
     * {@link StanzaPackedFormat#getPackedType(String)} for numeric and
     * dotted-decimal strings.
     */
    public static final byte NIBBLE_8 = (byte) 255;
}
