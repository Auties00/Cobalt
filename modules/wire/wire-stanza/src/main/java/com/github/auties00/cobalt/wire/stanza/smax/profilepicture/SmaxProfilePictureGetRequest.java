package com.github.auties00.cobalt.wire.stanza.smax.profilepicture;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds the outbound {@code <iq xmlns="w:profile:picture" type="get">} stanza
 * that fetches a contact, group, persona, or avatar picture.
 *
 * <p>Depending on the combination of optional fields the request fetches the
 * full picture URL, the preview URL, a cached blob, an avatar pose set, or a
 * privacy-token-authenticated variant; the relay answers with one of the four
 * {@link SmaxProfilePictureGetResponse} success arms or an error envelope. Most
 * callers supply {@code iqTarget}, {@code pictureType}, and
 * {@code pictureQuery}; the remaining fields gate the optional mixin paths.
 *
 * @implNote
 * This implementation flattens the WA Web smax mixin chain (getRequest, getIQ,
 * baseGetIQ, baseIQGet, serverDomainIQ) into a single {@link #toStanza()} call;
 * the avatar overlay path replaces the {@code <picture type=...>} attribute and
 * inlines the avatar children directly.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureGetRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureGetIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureBaseGetIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureServerDomainIQMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureAvatarMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureTCTokenMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePictureAddRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutProfilePicturePrivacyTokenContentsMixin")
public final class SmaxProfilePictureGetRequest implements SmaxStanza.Request {
    /**
     * The target entity JID whose picture is being fetched; routed verbatim
     * into the IQ's {@code target} attribute.
     */
    private final Jid iqTarget;

    /**
     * The optional {@code <picture type>} attribute; one of {@code "image"}
     * (full picture) or {@code "preview"} (small preview).
     *
     * <p>This value is ignored when {@link #avatarMixinArgs} is set; the avatar
     * overlay path pins {@code type="avatar"} on its own.
     */
    private final String pictureType;

    /**
     * The optional pre-known {@code <picture id>}; lets the relay short-circuit
     * with a 304-style hit when the caller already holds the matching cached
     * picture.
     */
    private final String pictureId;

    /**
     * The optional {@code <picture query>} selector; either {@code "url"} or
     * {@code "data"} depending on whether the caller wants the URL or an inlined
     * blob.
     */
    private final String pictureQuery;

    /**
     * The optional {@code <picture invite>} group-join-link token.
     */
    private final String pictureInvite;

    /**
     * The optional {@code <picture persona_id>} for a community or Meta-AI
     * persona.
     */
    private final String picturePersonaId;

    /**
     * The optional {@code <picture common_gid>} common-group JID that drives the
     * contact-card picture in shared-group flows.
     */
    private final Jid pictureCommonGid;

    /**
     * The optional add-request sub-payload for join-link-triggered fetches.
     */
    private final SmaxProfilePictureGetAddRequestMixin addRequestMixinArgs;

    /**
     * The optional privacy-token sub-payload for token-authenticated fetches.
     */
    private final SmaxProfilePictureGetTcTokenMixin tcTokenMixinArgs;

    /**
     * The optional avatar overlay payload; when set, the {@code <picture>} root
     * is emitted with {@code type="avatar"} carrying {@code 0..4}
     * {@code <avatar pose_id/>} children.
     */
    private final SmaxProfilePictureGetAvatarMixin avatarMixinArgs;

    /**
     * Constructs a profile-picture fetch request from the given target and
     * optional selectors.
     *
     * @param iqTarget             the target entity JID; never {@code null}
     * @param pictureType          the optional picture type; may be
     *                             {@code null}
     * @param pictureId            the optional cached picture id; may be
     *                             {@code null}
     * @param pictureQuery         the optional query selector; may be
     *                             {@code null}
     * @param pictureInvite        the optional invite token; may be
     *                             {@code null}
     * @param picturePersonaId     the optional persona id; may be {@code null}
     * @param pictureCommonGid     the optional common-group JID; may be
     *                             {@code null}
     * @param addRequestMixinArgs  the optional add-request payload; may be
     *                             {@code null}
     * @param tcTokenMixinArgs     the optional privacy-token payload; may be
     *                             {@code null}
     * @param avatarMixinArgs      the optional avatar overlay; may be
     *                             {@code null}
     * @throws NullPointerException if {@code iqTarget} is {@code null}
     */
    public SmaxProfilePictureGetRequest(Jid iqTarget,
                   String pictureType, String pictureId, String pictureQuery,
                   String pictureInvite, String picturePersonaId, Jid pictureCommonGid,
                   SmaxProfilePictureGetAddRequestMixin addRequestMixinArgs,
                   SmaxProfilePictureGetTcTokenMixin tcTokenMixinArgs,
                   SmaxProfilePictureGetAvatarMixin avatarMixinArgs) {
        this.iqTarget = Objects.requireNonNull(iqTarget, "iqTarget cannot be null");
        this.pictureType = pictureType;
        this.pictureId = pictureId;
        this.pictureQuery = pictureQuery;
        this.pictureInvite = pictureInvite;
        this.picturePersonaId = picturePersonaId;
        this.pictureCommonGid = pictureCommonGid;
        this.addRequestMixinArgs = addRequestMixinArgs;
        this.tcTokenMixinArgs = tcTokenMixinArgs;
        this.avatarMixinArgs = avatarMixinArgs;
    }

    /**
     * Returns the target entity JID.
     *
     * @return the JID; never {@code null}
     */
    public Jid iqTarget() {
        return iqTarget;
    }

    /**
     * Returns the optional picture type.
     *
     * <p>{@link #toStanza()} reads this value only when no avatar overlay is set.
     *
     * @return an {@link Optional} carrying the type, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<String> pictureType() {
        return Optional.ofNullable(pictureType);
    }

    /**
     * Returns the optional cached picture id.
     *
     * @return an {@link Optional} carrying the id, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<String> pictureId() {
        return Optional.ofNullable(pictureId);
    }

    /**
     * Returns the optional query selector.
     *
     * @return an {@link Optional} carrying the selector, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<String> pictureQuery() {
        return Optional.ofNullable(pictureQuery);
    }

    /**
     * Returns the optional invite token.
     *
     * @return an {@link Optional} carrying the token, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<String> pictureInvite() {
        return Optional.ofNullable(pictureInvite);
    }

    /**
     * Returns the optional persona id.
     *
     * @return an {@link Optional} carrying the id, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<String> picturePersonaId() {
        return Optional.ofNullable(picturePersonaId);
    }

    /**
     * Returns the optional common-group JID.
     *
     * @return an {@link Optional} carrying the JID, or {@link Optional#empty()}
     *         when omitted
     */
    public Optional<Jid> pictureCommonGid() {
        return Optional.ofNullable(pictureCommonGid);
    }

    /**
     * Returns the optional add-request payload.
     *
     * @return an {@link Optional} carrying the payload, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<SmaxProfilePictureGetAddRequestMixin> addRequestMixinArgs() {
        return Optional.ofNullable(addRequestMixinArgs);
    }

    /**
     * Returns the optional privacy-token payload.
     *
     * @return an {@link Optional} carrying the payload, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<SmaxProfilePictureGetTcTokenMixin> tcTokenMixinArgs() {
        return Optional.ofNullable(tcTokenMixinArgs);
    }

    /**
     * Returns the optional avatar overlay payload.
     *
     * @return an {@link Optional} carrying the payload, or
     *         {@link Optional#empty()} when omitted
     */
    public Optional<SmaxProfilePictureGetAvatarMixin> avatarMixinArgs() {
        return Optional.ofNullable(avatarMixinArgs);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned stanza has shape
     * {@snippet lang=xml :
     * <iq xmlns="w:profile:picture" type="get" to="s.whatsapp.net" target="<iqTarget>">
     *   <picture type="image|preview|avatar"? id? query? invite? persona_id? common_gid?>
     *     <avatar pose_id="..."/>?
     *     ...
     *     <smax$any><tctoken/></smax$any>?
     *     <add_request/>?
     *   </picture>
     * </iq>
     * }
     *
     * @implNote
     * This implementation pins the picture type to {@code "avatar"} and folds in
     * {@code <avatar pose_id/>} children when {@link #avatarMixinArgs} is
     * present; otherwise it copies the caller's {@link #pictureType} through.
     * The privacy-token and add-request sub-payloads are emitted as the last
     * children when present. The {@code to} attribute is pinned to
     * {@link Jid#userServer()}.
     *
     * @return a {@link StanzaBuilder} carrying the partially-built IQ envelope
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutProfilePictureGetRequest",
            exports = "makeGetRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var pictureBuilder = new StanzaBuilder()
                .description("picture");
        var pictureChildren = new ArrayList<Stanza>();
        if (avatarMixinArgs != null) {
            pictureBuilder.attribute("type", "avatar");
            for (var avatarArg : avatarMixinArgs.avatarArgs()) {
                pictureChildren.add(avatarArg.toStanza());
            }
        } else if (pictureType != null) {
            pictureBuilder.attribute("type", pictureType);
        }
        if (pictureId != null) {
            pictureBuilder.attribute("id", pictureId);
        }
        if (pictureQuery != null) {
            pictureBuilder.attribute("query", pictureQuery);
        }
        if (pictureInvite != null) {
            pictureBuilder.attribute("invite", pictureInvite);
        }
        if (picturePersonaId != null) {
            pictureBuilder.attribute("persona_id", picturePersonaId);
        }
        if (pictureCommonGid != null) {
            pictureBuilder.attribute("common_gid", pictureCommonGid);
        }
        if (tcTokenMixinArgs != null) {
            pictureChildren.add(tcTokenMixinArgs.toStanza());
        }
        if (addRequestMixinArgs != null) {
            pictureChildren.add(addRequestMixinArgs.toStanza());
        }
        if (!pictureChildren.isEmpty()) {
            pictureBuilder.content(pictureChildren);
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:profile:picture")
                .attribute("to", Jid.userServer())
                .attribute("target", iqTarget)
                .attribute("type", "get")
                .content(pictureBuilder.build());
    }

    /**
     * Compares this request to another for value equality on every field.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a
     *         {@link SmaxProfilePictureGetRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxProfilePictureGetRequest) obj;
        return Objects.equals(this.iqTarget, that.iqTarget)
                && Objects.equals(this.pictureType, that.pictureType)
                && Objects.equals(this.pictureId, that.pictureId)
                && Objects.equals(this.pictureQuery, that.pictureQuery)
                && Objects.equals(this.pictureInvite, that.pictureInvite)
                && Objects.equals(this.picturePersonaId, that.picturePersonaId)
                && Objects.equals(this.pictureCommonGid, that.pictureCommonGid)
                && Objects.equals(this.addRequestMixinArgs, that.addRequestMixinArgs)
                && Objects.equals(this.tcTokenMixinArgs, that.tcTokenMixinArgs)
                && Objects.equals(this.avatarMixinArgs, that.avatarMixinArgs);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(iqTarget, pictureType, pictureId, pictureQuery, pictureInvite,
                picturePersonaId, pictureCommonGid, addRequestMixinArgs, tcTokenMixinArgs,
                avatarMixinArgs);
    }

    /**
     * Returns a debug-friendly representation of this request.
     *
     * <p>The format is intended for logging and is not part of the contract.
     *
     * @return the string form
     */
    @Override
    public String toString() {
        return "SmaxProfilePictureGetRequest[iqTarget=" + iqTarget
                + ", pictureType=" + pictureType
                + ", pictureId=" + pictureId
                + ", pictureQuery=" + pictureQuery
                + ", pictureInvite=" + pictureInvite
                + ", picturePersonaId=" + picturePersonaId
                + ", pictureCommonGid=" + pictureCommonGid
                + ", addRequestMixinArgs=" + addRequestMixinArgs
                + ", tcTokenMixinArgs=" + tcTokenMixinArgs
                + ", avatarMixinArgs=" + avatarMixinArgs + ']';
    }
}
