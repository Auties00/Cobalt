package com.github.auties00.cobalt.wire.stanza.smax.groups;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;

import java.util.Objects;
import java.util.Optional;

/**
 * Fetches a single group's metadata projection via an {@code <iq type="get" xmlns="w:g2">} stanza, optionally
 * probing the V4-invite-link add-request flow at the same time.
 *
 * <p>The optional {@link #queryPhash()} lets the relay skip parts of the projection that have not changed since
 * the caller's last fetch; the optional add-request triple ({@link #addRequestExpiration()},
 * {@link #addRequestAdmin()}, {@link #addRequestCode()}) attaches the V4 invite-landing probe. Callers pair
 * this request with {@link SmaxGroupsGetGroupInfoResponse}.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetGroupInfoRequest")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseGetGroupMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsBaseIQGetRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsAddRequestMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsCodeMixin")
@WhatsAppWebModule(moduleName = "WASmaxOutGroupsGetGroupInfoRequestTypeMixin")
public final class SmaxGroupsGetGroupInfoRequest implements SmaxStanza.Request {
    /**
     * Holds the group {@link Jid} routed verbatim into the IQ envelope's {@code to} attribute.
     */
    private final Jid groupJid;

    /**
     * Holds the optional dehydration-hash hint stamped on the inner {@code <query phash="...">} child; when
     * supplied, the relay can return a delta-only projection.
     */
    private final String queryPhash;

    /**
     * Holds the optional query request-type stamped on the inner {@code <query request="...">} child; when
     * supplied, the relay returns the matching projection variant (for example {@code "interactive"}).
     */
    private final String queryRequestType;

    /**
     * Holds the optional V4-invite-link {@code <add_request expiration="...">} attribute; non-null switches the
     * request on the invite-landing probe.
     */
    private final Long addRequestExpiration;

    /**
     * Holds the optional V4-invite-link admin-targeted {@code <add_request admin="...">} recipient; mutually
     * exclusive with {@link #addRequestCode}.
     */
    private final Jid addRequestAdmin;

    /**
     * Holds the optional V4-invite-link code-targeted {@code <add_request code="...">} string; mutually
     * exclusive with {@link #addRequestAdmin}.
     */
    private final String addRequestCode;

    /**
     * Constructs a metadata-only request for the given group.
     *
     * <p>Convenience constructor for callers that do not need the dehydration hint or the V4 invite-landing
     * probe; delegates to the full constructor with all optional parameters set to {@code null}.
     *
     * @param groupJid the group {@link Jid}; never {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetGroupInfoRequest(Jid groupJid) {
        this(groupJid, null, null, null, null, null);
    }

    /**
     * Constructs a request carrying the optional dehydration hint and the V4 invite-landing probe.
     *
     * <p>When {@code addRequestExpiration} is supplied, callers pass either {@code addRequestAdmin} or
     * {@code addRequestCode} (not both); the relay rejects requests carrying both target attributes. Delegates to
     * {@link #SmaxGroupsGetGroupInfoRequest(Jid, String, String, Long, Jid, String)} with no query request-type.
     *
     * @param groupJid             the group {@link Jid}; never {@code null}
     * @param queryPhash           the optional dehydration hash hint; may be {@code null}
     * @param addRequestExpiration the optional add-request expiration timestamp; may be {@code null}
     * @param addRequestAdmin      the optional add-request admin target; may be {@code null}
     * @param addRequestCode       the optional add-request code target; may be {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetGroupInfoRequest(Jid groupJid, String queryPhash, Long addRequestExpiration,
                   Jid addRequestAdmin, String addRequestCode) {
        this(groupJid, queryPhash, null, addRequestExpiration, addRequestAdmin, addRequestCode);
    }

    /**
     * Constructs a fully-parametrised request.
     *
     * <p>When {@code addRequestExpiration} is supplied, callers pass either {@code addRequestAdmin} or
     * {@code addRequestCode} (not both); the relay rejects requests carrying both target attributes. The
     * {@code queryRequestType} selects the projection variant (for example {@code "interactive"}) and is mutually
     * usable with {@code queryPhash}.
     *
     * @param groupJid             the group {@link Jid}; never {@code null}
     * @param queryPhash           the optional dehydration hash hint; may be {@code null}
     * @param queryRequestType     the optional query request-type; may be {@code null}
     * @param addRequestExpiration the optional add-request expiration timestamp; may be {@code null}
     * @param addRequestAdmin      the optional add-request admin target; may be {@code null}
     * @param addRequestCode       the optional add-request code target; may be {@code null}
     * @throws NullPointerException if {@code groupJid} is {@code null}
     */
    public SmaxGroupsGetGroupInfoRequest(Jid groupJid, String queryPhash, String queryRequestType,
                   Long addRequestExpiration, Jid addRequestAdmin, String addRequestCode) {
        this.groupJid = Objects.requireNonNull(groupJid, "groupJid cannot be null");
        this.queryPhash = queryPhash;
        this.queryRequestType = queryRequestType;
        this.addRequestExpiration = addRequestExpiration;
        this.addRequestAdmin = addRequestAdmin;
        this.addRequestCode = addRequestCode;
    }

    /**
     * Returns the group {@link Jid} being queried.
     *
     * @return the group JID; never {@code null}
     */
    public Jid groupJid() {
        return groupJid;
    }

    /**
     * Returns the optional dehydration hash.
     *
     * <p>Empty means the request is for a full projection; a non-empty value lets the relay return a
     * delta-only response.
     *
     * @return an {@link Optional} carrying the hash, or empty when the caller did not supply one
     */
    public Optional<String> queryPhash() {
        return Optional.ofNullable(queryPhash);
    }

    /**
     * Returns the optional query request-type.
     *
     * <p>Empty means the request carries no {@code <query request="...">} attribute; a non-empty value selects the
     * matching projection variant.
     *
     * @return an {@link Optional} carrying the request-type, or empty when the caller did not supply one
     */
    public Optional<String> queryRequestType() {
        return Optional.ofNullable(queryRequestType);
    }

    /**
     * Returns the optional V4-invite-link add-request expiration timestamp.
     *
     * @return an {@link Optional} carrying the expiration, or empty when the caller did not supply one
     */
    public Optional<Long> addRequestExpiration() {
        return Optional.ofNullable(addRequestExpiration);
    }

    /**
     * Returns the optional V4-invite-link add-request admin target.
     *
     * @return an {@link Optional} carrying the admin JID, or empty when the caller did not supply one
     */
    public Optional<Jid> addRequestAdmin() {
        return Optional.ofNullable(addRequestAdmin);
    }

    /**
     * Returns the optional V4-invite-link add-request code target.
     *
     * @return an {@link Optional} carrying the code, or empty when the caller did not supply one
     */
    public Optional<String> addRequestCode() {
        return Optional.ofNullable(addRequestCode);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation stamps {@code phash} on the {@code <query/>} child when {@link #queryPhash()} is
     * non-empty and {@code request} when {@link #queryRequestType()} is non-empty, nests an {@code <add_request/>}
     * child carrying the supplied expiration, admin, and code attributes when {@link #addRequestExpiration()} is
     * non-empty, then wraps the result in the {@code <iq xmlns="w:g2" type="get">} envelope addressed to
     * {@link #groupJid()}.
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutGroupsGetGroupInfoRequest",
            exports = "makeGetGroupInfoRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var queryBuilder = new StanzaBuilder()
                .description("query");
        if (queryPhash != null) {
            queryBuilder.attribute("phash", queryPhash);
        }
        if (queryRequestType != null) {
            queryBuilder.attribute("request", queryRequestType);
        }
        if (addRequestExpiration != null) {
            var addRequestBuilder = new StanzaBuilder()
                    .description("add_request")
                    .attribute("expiration", addRequestExpiration);
            if (addRequestAdmin != null) {
                addRequestBuilder.attribute("admin", addRequestAdmin);
            }
            if (addRequestCode != null) {
                addRequestBuilder.attribute("code", addRequestCode);
            }
            queryBuilder.content(addRequestBuilder.build());
        }
        return new StanzaBuilder()
                .description("iq")
                .attribute("xmlns", "w:g2")
                .attribute("to", groupJid)
                .attribute("type", "get")
                .content(queryBuilder.build());
    }

    /**
     * Compares this request to {@code obj} for value equality across every field.
     *
     * @param obj the other object
     * @return {@code true} when {@code obj} is a {@link SmaxGroupsGetGroupInfoRequest} with identical fields
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxGroupsGetGroupInfoRequest) obj;
        return Objects.equals(this.groupJid, that.groupJid)
                && Objects.equals(this.queryPhash, that.queryPhash)
                && Objects.equals(this.queryRequestType, that.queryRequestType)
                && Objects.equals(this.addRequestExpiration, that.addRequestExpiration)
                && Objects.equals(this.addRequestAdmin, that.addRequestAdmin)
                && Objects.equals(this.addRequestCode, that.addRequestCode);
    }

    /**
     * Returns a hash composed of every field.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(groupJid, queryPhash, queryRequestType, addRequestExpiration, addRequestAdmin, addRequestCode);
    }

    /**
     * Returns a debug string carrying every field.
     *
     * @return the debug representation
     */
    @Override
    public String toString() {
        return "SmaxGroupsGetGroupInfoRequest[groupJid=" + groupJid
                + ", queryPhash=" + queryPhash
                + ", queryRequestType=" + queryRequestType
                + ", addRequestExpiration=" + addRequestExpiration
                + ", addRequestAdmin=" + addRequestAdmin
                + ", addRequestCode=" + addRequestCode + ']';
    }
}
