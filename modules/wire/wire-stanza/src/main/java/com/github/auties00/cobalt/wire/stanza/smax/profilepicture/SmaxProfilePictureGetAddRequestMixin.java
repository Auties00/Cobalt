package com.github.auties00.cobalt.wire.stanza.smax.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.Objects;
import java.util.Optional;

/**
 * Carries the join-code, optional admin JID, and expiration timestamp that turn
 * a {@link SmaxProfilePictureGetRequest} into a group-join-link picture fetch.
 *
 * <p>An instance is passed to {@link SmaxProfilePictureGetRequest} when fetching
 * a picture as part of a group-add-request flow; the relay correlates the join
 * intent with the picture fetch so the UI can show the inviter's avatar
 * alongside the join prompt. The values populate a single
 * {@code <add_request code expiration admin?/>} child stamped by
 * {@link #toNode()}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureAddRequestMixin")
public final class SmaxProfilePictureGetAddRequestMixin {
    /**
     * The mandatory {@code code} attribute carrying the group-join-link token.
     */
    private final String addRequestCode;

    /**
     * The optional {@code admin} attribute carrying the admin JID that issued
     * the join link.
     */
    private final Jid addRequestAdmin;

    /**
     * The mandatory {@code expiration} attribute carrying the join link's
     * expiry timestamp.
     */
    private final long addRequestExpiration;

    /**
     * Constructs an add-request payload from the given join-link fields.
     *
     * @param addRequestCode       the join-link code; never {@code null}
     * @param addRequestAdmin      the optional admin JID; may be {@code null}
     * @param addRequestExpiration the expiry timestamp
     * @throws NullPointerException if {@code addRequestCode} is {@code null}
     */
    public SmaxProfilePictureGetAddRequestMixin(String addRequestCode, Jid addRequestAdmin, long addRequestExpiration) {
        this.addRequestCode = Objects.requireNonNull(addRequestCode, "addRequestCode cannot be null");
        this.addRequestAdmin = addRequestAdmin;
        this.addRequestExpiration = addRequestExpiration;
    }

    /**
     * Returns the join-link code.
     *
     * @return the code; never {@code null}
     */
    public String addRequestCode() {
        return addRequestCode;
    }

    /**
     * Returns the optional admin JID.
     *
     * <p>{@link #toNode()} reads this value to decide whether to stamp the
     * {@code admin} attribute on the {@code <add_request>} child.
     *
     * @return an {@link Optional} carrying the JID, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<Jid> addRequestAdmin() {
        return Optional.ofNullable(addRequestAdmin);
    }

    /**
     * Returns the expiry timestamp.
     *
     * @return the timestamp
     */
    public long addRequestExpiration() {
        return addRequestExpiration;
    }

    /**
     * Builds the {@code <add_request>} child stanza.
     *
     * <p>The stanza has shape
     * {@snippet lang=xml :
     * <add_request code="..." expiration="N" admin="..."?/>
     * }
     * where the {@code admin} attribute is present only when an admin JID was
     * supplied.
     *
     * @return the {@link Stanza}
     */
    @WhatsAppWebExport(moduleName = "WASmaxOutProfilePictureAddRequestMixin",
            exports = "mergeAddRequestMixin",
            adaptation = WhatsAppAdaptation.DIRECT)
    public Stanza toStanza() {
        var builder = new StanzaBuilder()
                .description("add_request")
                .attribute("code", addRequestCode)
                .attribute("expiration", addRequestExpiration);
        if (addRequestAdmin != null) {
            builder.attribute("admin", addRequestAdmin);
        }
        return builder.build();
    }

    /**
     * Compares this payload to another for value equality.
     *
     * <p>Two payloads are equal when they carry the same code, admin JID, and
     * expiration timestamp.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxProfilePictureGetAddRequestMixin} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxProfilePictureGetAddRequestMixin) obj;
        return this.addRequestExpiration == that.addRequestExpiration
                && Objects.equals(this.addRequestCode, that.addRequestCode)
                && Objects.equals(this.addRequestAdmin, that.addRequestAdmin);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(addRequestCode, addRequestAdmin, addRequestExpiration);
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
        return "SmaxProfilePictureGetAddRequestMixin[addRequestCode=" + addRequestCode
                + ", addRequestAdmin=" + addRequestAdmin
                + ", addRequestExpiration=" + addRequestExpiration + ']';
    }
}
