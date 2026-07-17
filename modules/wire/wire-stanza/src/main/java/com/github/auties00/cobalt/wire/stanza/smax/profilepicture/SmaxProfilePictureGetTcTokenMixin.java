package com.github.auties00.cobalt.wire.stanza.smax.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a privacy token inside an {@code <smax$any><tctoken>...</tctoken></smax$any>}
 * child so a {@link SmaxProfilePictureGetRequest} can be authenticated against
 * that token.
 *
 * <p>An instance is passed to {@link SmaxProfilePictureGetRequest} when the
 * caller holds a fresh privacy token and wants to authenticate the picture-get
 * against it; the relay rejects the fetch when the token is invalid or expired.
 * The contents bytes are opaque to Cobalt and decoded only by the relay's
 * privacy-token validator.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureTCTokenMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePicturePrivacyTokenContentsMixin")
public final class SmaxProfilePictureGetTcTokenMixin {
    /**
     * The optional {@code t} timestamp on the {@code <tctoken/>} element.
     */
    private final Long tctokenT;

    /**
     * The opaque privacy-token contents bytes.
     */
    private final byte[] anyElementValue;

    /**
     * Constructs a privacy-token payload from the given timestamp and contents.
     *
     * @param tctokenT        the optional issuance timestamp; may be
     *                        {@code null}
     * @param anyElementValue the privacy-token bytes; never {@code null}
     * @throws NullPointerException if {@code anyElementValue} is {@code null}
     */
    public SmaxProfilePictureGetTcTokenMixin(Long tctokenT, byte[] anyElementValue) {
        this.tctokenT = tctokenT;
        this.anyElementValue = Objects.requireNonNull(anyElementValue, "anyElementValue cannot be null");
    }

    /**
     * Returns the optional issuance timestamp.
     *
     * <p>{@link #toNode()} reads this value to decide whether to stamp the
     * {@code t} attribute on the {@code <tctoken>} element.
     *
     * @return an {@link Optional} carrying the timestamp, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<Long> tctokenT() {
        return Optional.ofNullable(tctokenT);
    }

    /**
     * Returns the privacy-token contents bytes.
     *
     * <p>The bytes are opaque to Cobalt; the relay's privacy-token validator
     * decodes the contents.
     *
     * @return the bytes; never {@code null}
     */
    public byte[] anyElementValue() {
        return anyElementValue;
    }

    /**
     * Builds the {@code <smax$any>} wrapper stanza carrying the {@code <tctoken/>}
     * child.
     *
     * <p>The stanza has shape
     * {@snippet lang=xml :
     * <smax$any><tctoken t="N"?>...bytes...</tctoken></smax$any>
     * }
     * where the {@code t} attribute is present only when a timestamp was
     * supplied.
     *
     * @return the {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutProfilePictureTCTokenMixin",
            exports = "mergeTCTokenMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza toStanza() {
        var tctokenBuilder = new StanzaBuilder()
                .description("tctoken")
                .content(anyElementValue);
        if (tctokenT != null) {
            tctokenBuilder.attribute("t", tctokenT);
        }
        return new StanzaBuilder()
                .description("smax$any")
                .content(tctokenBuilder.build())
                .build();
    }

    /**
     * Compares this payload to another for value equality on the timestamp and
     * contents bytes.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxProfilePictureGetTcTokenMixin} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxProfilePictureGetTcTokenMixin) obj;
        return Objects.equals(this.tctokenT, that.tctokenT)
                && Arrays.equals(this.anyElementValue, that.anyElementValue);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @implNote
     * This implementation mixes {@link Arrays#hashCode(byte[])} of the contents
     * into the hash so byte-array contents drive the result.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        var result = Objects.hash(tctokenT);
        result = 31 * result + Arrays.hashCode(anyElementValue);
        return result;
    }

    /**
     * Returns a debug-friendly representation of this payload.
     *
     * <p>The format is intended for logging and is not part of the contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxProfilePictureGetTcTokenMixin[tctokenT=" + tctokenT
                + ", anyElementValue=" + Arrays.toString(anyElementValue) + ']';
    }
}
