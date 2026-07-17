package com.github.auties00.cobalt.wire.stanza.iq.group;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.iq.IqStanza;
import java.util.Objects;
import java.util.Optional;

/**
 * Models the outbound IQ request that fetches the profile picture of an invite-only group before the caller has joined it.
 *
 * <p>This request backs the "preview group avatar from an invite" surface, fed by either a
 * {@code chat.whatsapp.com/<code>} link or an in-chat invite-message attachment. The two flows ship
 * different stanza shapes, selected by {@link IqQueryGroupInviteProfilePicMode}: the link mode
 * addresses the group JID under {@code w:g2} and only carries the invite code, while the message
 * mode addresses {@code s.whatsapp.net} under {@code w:profile:picture} and additionally carries
 * the inviting admin JID and the invite-message expiration timestamp. The relay responds with the
 * CDN URL and a stable picture id that the caller can pass back on subsequent fetches to
 * short-circuit when the cached id is still authoritative.
 *
 * @implNote
 * This implementation collapses the link and message picture queries into a single request class
 * keyed by {@link IqQueryGroupInviteProfilePicMode}, mapping the picture options object onto the
 * {@link #pictureId()}, {@link #pictureType()} and {@link #pictureQuery()} accessors.
 */
@WhatsAppWebModule(moduleName = "WAWebQueryGroupInviteProfilePicApi")
public final class IqQueryGroupInviteProfilePicRequest implements IqStanza.Request {
    /**
     * Holds the dispatch mode.
     */
    private final IqQueryGroupInviteProfilePicMode mode;

    /**
     * Holds the target group JID.
     */
    private final Jid groupJid;

    /**
     * Holds the invite code being previewed.
     */
    private final String code;

    /**
     * Holds the previously-cached picture identifier, or {@code null} when no cached id is
     * supplied.
     */
    private final String pictureId;

    /**
     * Holds the picture variant, or {@code null} when the caller defers to the relay default.
     */
    private final String pictureType;

    /**
     * Holds the picture-query mode, or {@code null} when the caller defers to the relay default.
     */
    private final String pictureQuery;

    /**
     * Holds the inviting admin JID, only used in
     * {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE}.
     */
    private final Jid adminJid;

    /**
     * Holds the invite-message expiration timestamp (seconds since the epoch), only used in
     * {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE}.
     */
    private final String expiration;

    /**
     * Constructs a request with the given parameters.
     *
     * <p>Pass {@code pictureId} when the caller already holds a cached picture id from an earlier
     * fetch; the relay then omits the {@code <picture>} child entirely on the success reply,
     * signalling that the cached id is still authoritative. Pass {@code pictureType = "preview"}
     * for the low-resolution thumbnail and {@code pictureType = "image"} for the full-size avatar;
     * the upstream link flow uses {@code {type: "preview", query: "url"}} and the message flow uses
     * {@code {type: "image", query: "url"}}.
     *
     * @param mode         the dispatch mode; never {@code null}
     * @param groupJid     the target group {@link Jid}; never {@code null}
     * @param code         the invite code; never {@code null}
     * @param pictureId    the cached picture id, or {@code null}
     * @param pictureType  the picture variant, or {@code null}
     * @param pictureQuery the picture-query mode, or {@code null}
     * @param adminJid     the inviting admin {@link Jid}; required in {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE}, ignored in {@link IqQueryGroupInviteProfilePicMode#INVITE_LINK}
     * @param expiration   the invite expiration timestamp; required in {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE}, ignored in {@link IqQueryGroupInviteProfilePicMode#INVITE_LINK}
     * @throws NullPointerException if {@code mode}, {@code groupJid} or {@code code} is {@code null}, or if {@code adminJid} or {@code expiration} is {@code null} in {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE}
     */
    public IqQueryGroupInviteProfilePicRequest(IqQueryGroupInviteProfilePicMode mode, Jid groupJid, String code, String pictureId, String pictureType,
                   String pictureQuery, Jid adminJid, String expiration) {
        Objects.requireNonNull(mode, "mode cannot be null");
        Objects.requireNonNull(groupJid, "groupJid cannot be null");
        Objects.requireNonNull(code, "code cannot be null");
        if (mode == IqQueryGroupInviteProfilePicMode.INVITE_MESSAGE) {
            Objects.requireNonNull(adminJid, "adminJid cannot be null in INVITE_MESSAGE mode");
            Objects.requireNonNull(expiration, "expiration cannot be null in INVITE_MESSAGE mode");
        }
        this.mode = mode;
        this.groupJid = groupJid;
        this.code = code;
        this.pictureId = pictureId;
        this.pictureType = pictureType;
        this.pictureQuery = pictureQuery;
        this.adminJid = adminJid;
        this.expiration = expiration;
    }

    /**
     * Returns the dispatch mode.
     *
     * @return the {@link IqQueryGroupInviteProfilePicMode}; never {@code null}
     */
    public IqQueryGroupInviteProfilePicMode mode() {
        return mode;
    }

    /**
     * Returns the target group JID.
     *
     * @return the group {@link Jid}; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the invite code.
     *
     * @return the code; never {@code null}
     */
    public String code() {
        return code;
    }

    /**
     * Returns the optional cached picture identifier.
     *
     * @return an {@link Optional} carrying the cached picture id, or empty when no cached id was supplied
     */
    public Optional<String> pictureId() {
        return Optional.ofNullable(pictureId);
    }

    /**
     * Returns the optional picture variant.
     *
     * @return an {@link Optional} carrying the picture variant, or empty when the caller defers to the relay default
     */
    public Optional<String> pictureType() {
        return Optional.ofNullable(pictureType);
    }

    /**
     * Returns the optional picture-query mode.
     *
     * @return an {@link Optional} carrying the picture-query mode, or empty when the caller defers to the relay default
     */
    public Optional<String> pictureQuery() {
        return Optional.ofNullable(pictureQuery);
    }

    /**
     * Returns the optional inviting admin JID.
     *
     * @return an {@link Optional} carrying the admin {@link Jid}, or empty in {@link IqQueryGroupInviteProfilePicMode#INVITE_LINK}
     */
    public Optional<Jid> adminJid() {
        return Optional.ofNullable(adminJid);
    }

    /**
     * Returns the optional invite-message expiration timestamp.
     *
     * @return an {@link Optional} carrying the expiration, or empty in {@link IqQueryGroupInviteProfilePicMode#INVITE_LINK}
     */
    public Optional<String> expiration() {
        return Optional.ofNullable(expiration);
    }

    /**
     * Builds the outbound IQ stanza as a {@link StanzaBuilder} ready for dispatch.
     *
     * <p>Switches on {@link #mode()} and delegates to either {@link #buildInviteLinkStanza()} or
     * {@link #buildInviteMessageStanza()}.
     *
     * @implNote
     * This implementation drops omitted picture-option attributes by skipping the corresponding
     * {@code attribute(...)} calls when the field is {@code null}.
     *
     * @return the IQ envelope builder
     */
    @Override
    @WhatsAppWebExport(moduleName = "WAWebQueryGroupInviteProfilePicApi",
            exports = "queryGroupInviteLinkProfilePic",
            adaptation = WhatsAppAdaptation.DIRECT)
    @WhatsAppWebExport(moduleName = "WAWebQueryGroupInviteProfilePicApi",
            exports = "queryGroupInviteMessageProfilePic",
            adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        if (mode == IqQueryGroupInviteProfilePicMode.INVITE_LINK) {
            return buildInviteLinkStanza();
        }
        return buildInviteMessageStanza();
    }

    /**
     * Builds the {@link IqQueryGroupInviteProfilePicMode#INVITE_LINK} stanza variant.
     *
     * <p>The IQ envelope is addressed to the group JID under {@code w:g2}, and the
     * {@code <picture>} child carries the invite code under the {@code invite} attribute plus the
     * optional {@code id}, {@code type} and {@code query} picture options.
     *
     * @return the IQ envelope builder
     */
    private StanzaBuilder buildInviteLinkStanza() {
        var pictureBuilder = new StanzaBuilder()
                .description("picture")
                .attribute("invite", code);
        if (pictureId != null) {
            pictureBuilder.attribute("id", pictureId);
        }
        if (pictureType != null) {
            pictureBuilder.attribute("type", pictureType);
        }
        if (pictureQuery != null) {
            pictureBuilder.attribute("query", pictureQuery);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "get")
                .content(pictureBuilder.build());
    }

    /**
     * Builds the {@link IqQueryGroupInviteProfilePicMode#INVITE_MESSAGE} stanza variant.
     *
     * <p>The IQ envelope is addressed to {@code s.whatsapp.net} under {@code w:profile:picture}
     * with the group JID in the {@code target} attribute, and the {@code <picture>} child wraps an
     * {@code <add_request code expiration admin/>} grandchild that proves the caller holds a valid
     * in-chat invite-message context for the target group.
     *
     * @return the IQ envelope builder
     */
    private StanzaBuilder buildInviteMessageStanza() {
        var addRequestBuilder = new StanzaBuilder()
                .description("add_request")
                .attribute("code", code)
                .attribute("expiration", expiration)
                .attribute("admin", adminJid);
        var pictureBuilder = new StanzaBuilder()
                .description("picture");
        if (pictureId != null) {
            pictureBuilder.attribute("id", pictureId);
        }
        if (pictureType != null) {
            pictureBuilder.attribute("type", pictureType);
        }
        if (pictureQuery != null) {
            pictureBuilder.attribute("query", pictureQuery);
        }
        pictureBuilder.content(addRequestBuilder.build());
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:profile:picture")
                .attribute("to", JidServer.user())
                .attribute("target", groupJid)
                .attribute("type", "get")
                .content(pictureBuilder.build());
    }

    /**
     * Compares this request with another object for equality.
     *
     * <p>Two requests are equal when they carry the same mode, group JID, code, picture options,
     * admin JID and expiration.
     *
     * @param obj the object to compare with; may be {@code null}
     * @return {@code true} when {@code obj} is an equal request, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (IqQueryGroupInviteProfilePicRequest) obj;
        return this.mode == that.mode
                && Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.code, that.code)
                && Objects.equals(this.pictureId, that.pictureId)
                && Objects.equals(this.pictureType, that.pictureType)
                && Objects.equals(this.pictureQuery, that.pictureQuery)
                && Objects.equals(this.adminJid, that.adminJid)
                && Objects.equals(this.expiration, that.expiration);
    }

    /**
     * Returns a hash code derived from all request fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(mode, groupJid, code, pictureId, pictureType, pictureQuery,
                adminJid, expiration);
    }

    /**
     * Returns a debug string describing all request fields.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "IqQueryGroupInviteProfilePicRequest[mode=" + mode
                + ", groupJid=" + groupJid
                + ", code=" + code
                + ", pictureId=" + pictureId
                + ", pictureType=" + pictureType
                + ", pictureQuery=" + pictureQuery
                + ", adminJid=" + adminJid
                + ", expiration=" + expiration + ']';
    }
}
