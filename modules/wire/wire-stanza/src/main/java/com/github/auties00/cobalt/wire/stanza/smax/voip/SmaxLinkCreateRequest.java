package com.github.auties00.cobalt.wire.stanza.smax.voip;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.core.jid.JidServer;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;
import com.github.auties00.cobalt.wire.stanza.smax.SmaxStanza;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Mints a fresh shareable call-link token on the VoIP relay.
 *
 * <p>This is the outbound {@code <call><link_create/></call>} request behind the
 * "Create call link" surface and the scheduled-event call-link generator. The
 * token returned in {@link SmaxLinkCreateResponse.Success#linkCreateToken()} is
 * suffixed onto {@code https://call.whatsapp.com/voice/} or
 * {@code https://call.whatsapp.com/video/} to produce the URL the user shares.
 */
@WhatsAppWebModule(moduleName = "WASmaxOutVoipLinkCreateRequest")
public final class SmaxLinkCreateRequest implements SmaxStanza.Request {
    /**
     * The optional media type carried by the {@code media} attribute.
     *
     * <p>Holds {@code "audio"} or {@code "video"} on the wire, kept as a raw
     * {@link String} so relay-side enum extensions do not force a Cobalt
     * rebuild.
     */
    private final String linkCreateMedia;

    /**
     * The optional creator-device JID carried by the {@code call-creator}
     * attribute.
     *
     * <p>Set when the link should be associated with a specific device of the
     * caller rather than the user's primary device.
     */
    private final Jid linkCreateCallCreator;

    /**
     * The optional call identifier carried by the {@code call-id} attribute.
     *
     * <p>Set when the call link is generated from an already in-flight call
     * rather than minted standalone.
     */
    private final String linkCreateCallId;

    /**
     * The optional creator-username carried by the {@code link_creator_username}
     * attribute.
     *
     * <p>Populated when username-based calling is gated on for the local
     * account so the join-prompt surface can display the creator's username.
     */
    private final String linkCreateLinkCreatorUsername;

    /**
     * Whether the {@code waiting_room_enabled="1"} marker is emitted.
     *
     * <p>Maps to the "Require approval to join" toggle on the call-link create
     * sheet. When {@code false} the attribute is omitted, which the relay
     * treats as disabled.
     */
    private final boolean linkCreateWaitingRoomEnabled;

    /**
     * The optional scheduled-event start instant.
     *
     * <p>Supplied by the scheduled-call surface and rendered as an inner
     * {@code <event start_time="..."/>} child whose value is seconds since the
     * epoch.
     */
    private final Instant eventStartTime;

    /**
     * Constructs a request with every wire-level attribute spelled out.
     *
     * <p>Most callers leave the optional fields {@code null} and let the relay
     * supply defaults; the fields a call-link UI typically sets are
     * {@code linkCreateMedia} ({@code "audio"} or {@code "video"}) and, for
     * scheduled calls, {@code eventStartTime}.
     *
     * @param linkCreateMedia               the optional media type; may be {@code null}
     * @param linkCreateCallCreator         the optional creator-device JID; may be {@code null}
     * @param linkCreateCallId              the optional pre-allocated call id; may be {@code null}
     * @param linkCreateLinkCreatorUsername the optional creator username; may be {@code null}
     * @param linkCreateWaitingRoomEnabled  whether the waiting-room gate is enabled at creation time
     * @param eventStartTime                the optional event start instant; may be {@code null}
     */
    public SmaxLinkCreateRequest(String linkCreateMedia,
                   Jid linkCreateCallCreator,
                   String linkCreateCallId,
                   String linkCreateLinkCreatorUsername,
                   boolean linkCreateWaitingRoomEnabled,
                   Instant eventStartTime) {
        this.linkCreateMedia = linkCreateMedia;
        this.linkCreateCallCreator = linkCreateCallCreator;
        this.linkCreateCallId = linkCreateCallId;
        this.linkCreateLinkCreatorUsername = linkCreateLinkCreatorUsername;
        this.linkCreateWaitingRoomEnabled = linkCreateWaitingRoomEnabled;
        this.eventStartTime = eventStartTime;
    }

    /**
     * Returns the optional media type carried by the {@code media} attribute.
     *
     * @return an {@link Optional} carrying the media type, or empty when omitted
     */
    public Optional<String> linkCreateMedia() {
        return Optional.ofNullable(linkCreateMedia);
    }

    /**
     * Returns the optional creator-device JID carried by the
     * {@code call-creator} attribute.
     *
     * @return an {@link Optional} carrying the device JID, or empty when omitted
     */
    public Optional<Jid> linkCreateCallCreator() {
        return Optional.ofNullable(linkCreateCallCreator);
    }

    /**
     * Returns the optional pre-allocated call identifier carried by the
     * {@code call-id} attribute.
     *
     * @return an {@link Optional} carrying the call id, or empty when omitted
     */
    public Optional<String> linkCreateCallId() {
        return Optional.ofNullable(linkCreateCallId);
    }

    /**
     * Returns the optional creator-username carried by the
     * {@code link_creator_username} attribute.
     *
     * @return an {@link Optional} carrying the username, or empty when omitted
     */
    public Optional<String> linkCreateLinkCreatorUsername() {
        return Optional.ofNullable(linkCreateLinkCreatorUsername);
    }

    /**
     * Returns whether the {@code waiting_room_enabled="1"} marker is emitted.
     *
     * @return {@code true} when the gate is requested, {@code false} otherwise
     */
    public boolean linkCreateWaitingRoomEnabled() {
        return linkCreateWaitingRoomEnabled;
    }

    /**
     * Returns the optional scheduled-event start instant.
     *
     * @return an {@link Optional} carrying the event start, or empty when omitted
     */
    public Optional<Instant> eventStartTime() {
        return Optional.ofNullable(eventStartTime);
    }

    /**
     * {@inheritDoc}
     *
     * @implNote
     * This implementation wraps a {@code <link_create/>} child in a
     * {@code <call to="call">} envelope, emitting each optional attribute only
     * when its backing field is set. A present {@link #eventStartTime()} is
     * rendered as an inner {@code <event start_time="..."/>} child whose value
     * is seconds since the epoch.
     *
     * @return a {@link StanzaBuilder} carrying the {@code <call><link_create/></call>}
     *         stanza
     */
    @Override
    @WhatsAppWebExport(moduleName = "WASmaxOutVoipLinkCreateRequest",
            exports = "makeLinkCreateRequest", adaptation = WhatsAppAdaptation.DIRECT)
    public StanzaBuilder toStanza() {
        var linkCreateBuilder = new StanzaBuilder()
                .description("link_create");
        if (linkCreateMedia != null) {
            linkCreateBuilder.attribute("media", linkCreateMedia);
        }
        if (linkCreateCallCreator != null) {
            linkCreateBuilder.attribute("call-creator", linkCreateCallCreator);
        }
        if (linkCreateCallId != null) {
            linkCreateBuilder.attribute("call-id", linkCreateCallId);
        }
        if (linkCreateLinkCreatorUsername != null) {
            linkCreateBuilder.attribute("link_creator_username", linkCreateLinkCreatorUsername);
        }
        if (linkCreateWaitingRoomEnabled) {
            linkCreateBuilder.attribute("waiting_room_enabled", "1");
        }
        if (eventStartTime != null) {
            var eventNode = new StanzaBuilder()
                    .description("event")
                    .attribute("start_time", eventStartTime.getEpochSecond())
                    .build();
            linkCreateBuilder.content(eventNode);
        }
        return new StanzaBuilder()
                .description("call")
                .attribute("to", JidServer.call())
                .content(linkCreateBuilder.build());
    }

    /**
     * Compares this request to another object for value equality.
     *
     * @param obj the object to compare against; may be {@code null}
     * @return {@code true} when {@code obj} is a {@link SmaxLinkCreateRequest}
     *         with equal fields, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (SmaxLinkCreateRequest) obj;
        return this.linkCreateWaitingRoomEnabled == that.linkCreateWaitingRoomEnabled
                && Objects.equals(this.linkCreateMedia, that.linkCreateMedia)
                && Objects.equals(this.linkCreateCallCreator, that.linkCreateCallCreator)
                && Objects.equals(this.linkCreateCallId, that.linkCreateCallId)
                && Objects.equals(this.linkCreateLinkCreatorUsername, that.linkCreateLinkCreatorUsername)
                && Objects.equals(this.eventStartTime, that.eventStartTime);
    }

    /**
     * Returns a hash code derived from every field of this request.
     *
     * @return the hash code consistent with {@link #equals(Object)}
     */
    @Override
    public int hashCode() {
        return Objects.hash(linkCreateMedia, linkCreateCallCreator, linkCreateCallId,
                linkCreateLinkCreatorUsername, linkCreateWaitingRoomEnabled, eventStartTime);
    }

    /**
     * Returns a debug string listing every field of this request.
     *
     * @return the string representation of this request
     */
    @Override
    public String toString() {
        return "SmaxLinkCreateRequest[linkCreateMedia=" + linkCreateMedia
                + ", linkCreateCallCreator=" + linkCreateCallCreator
                + ", linkCreateCallId=" + linkCreateCallId
                + ", linkCreateLinkCreatorUsername=" + linkCreateLinkCreatorUsername
                + ", linkCreateWaitingRoomEnabled=" + linkCreateWaitingRoomEnabled
                + ", eventStartTime=" + eventStartTime + ']';
    }
}
