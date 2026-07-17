package com.github.auties00.cobalt.calls.signaling.session;

import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a single {@code <capability>} advertisement carried inside an offer, accept, or
 * preaccept.
 *
 * <p>A capability child advertises one version of a device's feature bitmask: a {@code ver}
 * attribute selecting the bitmask version and a binary blob holding the mask itself, with the least
 * significant bit first within each byte. A device may advertise several such children, one per
 * supported bitmask version; this record models exactly one version tagged mask as it appears on
 * the wire, and leaves the typed interpretation of the individual bit indices and the assembly of
 * the multiple versions to the wider capability subsystem.
 *
 * <p>On the wire the element is {@code <capability ver="N">MASK_BYTES</capability>} where {@code N}
 * ranges over {@code 1..0x40} and the mask holds between one and sixty four bytes. A captured one
 * to one offer advertises the following seven byte mask under version {@code 1}.
 *
 * {@snippet lang = "xml":
 * <capability ver="1">01 05 F7 09 E4 BB 13</capability>
 *}
 *
 * @param version the bitmask version this advertisement carries, in the range {@code 1..0x40}
 * @param mask    the capability bitmask bytes, with the least significant bit first within each
 *                byte; never {@code null} and never empty
 * @see SignalingType
 */
public record CallCapability(int version, byte[] mask) {
    /**
     * The wire attribute naming the bitmask version on a {@code <capability>} element.
     */
    private static final String VERSION_ATTRIBUTE = "ver";

    /**
     * The wire element tag for a capability advertisement.
     */
    public static final String ELEMENT = "capability";

    /**
     * Canonicalizes the record components, defensively copying the mask and rejecting an empty mask.
     *
     * @throws NullPointerException     if {@code mask} is {@code null}
     * @throws IllegalArgumentException if {@code mask} is empty
     */
    public CallCapability {
        Objects.requireNonNull(mask, "mask cannot be null");
        if (mask.length == 0) {
            throw new IllegalArgumentException("capability mask cannot be empty");
        }
        mask = mask.clone();
    }

    /**
     * Returns the capability bitmask bytes backing this advertisement.
     *
     * <p>This accessor overrides the implicit record accessor to return a defensive copy so the
     * stored array cannot be mutated through the returned reference.
     *
     * @return a copy of the mask bytes
     */
    @Override
    public byte[] mask() {
        return mask.clone();
    }

    /**
     * Builds the {@code <capability ver="N">MASK</capability>} stanza for this advertisement.
     *
     * @return the capability stanza
     */
    public Stanza toStanza() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(VERSION_ATTRIBUTE, version)
                .content(mask.clone())
                .build();
    }

    /**
     * Decodes a {@code <capability>} stanza into a {@link CallCapability}.
     *
     * <p>The {@code ver} attribute supplies the version and the element content supplies the mask. A
     * stanza with no content, or a stanza that is not a {@code <capability>} element, yields an empty
     * result rather than throwing so callers iterating a mixed child list can skip it.
     *
     * @param stanza the {@code <capability>} stanza
     * @return the decoded capability, or an empty result when the stanza is not a usable capability
     *         element
     * @throws NullPointerException if {@code stanza} is {@code null}
     */
    public static Optional<CallCapability> of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        if (!stanza.hasDescription(ELEMENT)) {
            return Optional.empty();
        }
        var mask = stanza.toContentBytes();
        if (mask.isEmpty()) {
            return Optional.empty();
        }
        var version = stanza.getAttributeAsInt(VERSION_ATTRIBUTE, 1);
        return Optional.of(new CallCapability(version, mask.get()));
    }

    /**
     * Compares this advertisement with another object for value equality.
     *
     * <p>Two capabilities are equal when they carry the same {@code version} and the same mask
     * bytes. The mask is compared by content through {@link Arrays#equals(byte[], byte[])} rather
     * than by array identity, so that the default reference comparison the record would otherwise
     * synthesize does not treat two equal masks as distinct.
     *
     * @param obj the object to compare against
     * @return {@code true} if {@code obj} is a {@link CallCapability} with the same version and mask
     *         bytes
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof CallCapability that
                && this.version == that.version
                && Arrays.equals(this.mask, that.mask));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * <p>The hash combines the {@code version} with the content hash of the mask bytes from
     * {@link Arrays#hashCode(byte[])}, so that two capabilities equal by content share a hash code.
     *
     * @return the hash code for this advertisement
     */
    @Override
    public int hashCode() {
        return Objects.hash(version, Arrays.hashCode(mask));
    }

    /**
     * Returns a textual representation of this advertisement.
     *
     * <p>The representation reports the {@code version} and the mask length rather than the raw
     * mask bytes, keeping the output compact.
     *
     * @return a string describing this advertisement
     */
    @Override
    public String toString() {
        return "CallCapability[version=" + version + ", maskLen=" + mask.length + ']';
    }
}
