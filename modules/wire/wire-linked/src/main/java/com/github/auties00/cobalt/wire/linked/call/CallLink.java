package com.github.auties00.cobalt.wire.linked.call;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a sharable WhatsApp call link.
 *
 * <p>A call link is a short, server-minted URL of the form
 * {@code https://call.whatsapp.com/{voice|video}/{token}}. Anyone who follows
 * the link is dropped directly into a multi-party call session whose media
 * kind is fixed at creation time. The {@linkplain #token() token} component
 * is a 22-character opaque identifier issued by the call relay, while the
 * {@linkplain #media() media} discriminator selects the {@code voice/} or
 * {@code video/} URL prefix.
 *
 * <p>Instances of this class describe a call link as a piece of data, not a
 * live session. Creating a link via the relay yields a value object carrying
 * just the {@linkplain #token() token}, the {@linkplain #media() media kind},
 * and (optionally) a {@linkplain #creator() creator device JID} when the
 * underlying RPC echoed it back. Querying an existing link additionally
 * surfaces the {@linkplain #creatorPn() creator phone-number JID}, the
 * {@linkplain #creatorUsername() creator-supplied display username}, and the
 * link's {@linkplain #waitingRoom() waiting-room state}.
 *
 * <p>The link's owning chat session is unaffected by the link object's
 * lifetime: a {@code CallLink} value can be persisted, shared, and queried
 * again without ever touching the relay.
 */
@ProtobufMessage
public final class CallLink {
    /**
     * The opaque 22-character token component of the link URL. The token is
     * minted by the relay at creation time and remains stable for the
     * lifetime of the link.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String token;

    /**
     * The media kind associated with the link. Encoded on the wire as
     * {@code "audio"} or {@code "video"} and surfaced in the URL prefix as
     * {@code voice/} or {@code video/} respectively.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    final CallLinkMedia media;

    /**
     * The device {@link Jid} of the user who created the link, when echoed
     * by the relay. Always present for query replies, optionally present
     * for create replies.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid creator;

    /**
     * The phone-number {@link Jid} of the link creator, when the relay
     * surfaced both LID and PN identifiers for the same creator. Only
     * supplied on query replies.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final Jid creatorPn;

    /**
     * The display username supplied by the link creator. Surfaces in the
     * join-prompt UI and may differ from any contact-book name.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String creatorUsername;

    /**
     * The pre-allocated call identifier echoed by {@code link_create} replies
     * when the caller already had an in-flight call when the link was
     * created. Absent for fresh links and for query replies.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String callId;

    /**
     * Whether the link is associated with a scheduled call event. Set on
     * query replies that carry an {@code <event/>} child; always
     * {@code false} on create replies.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.BOOL)
    final boolean scheduled;

    /**
     * The waiting-room state attached to the link, when reported by the
     * relay. Only populated on query replies whose {@code <link_query/>}
     * child carries a nested {@code <waiting_room/>} descriptor.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final CallLinkWaitingRoom waitingRoom;

    /**
     * Constructs a new {@code CallLink} carrying every documented attribute.
     *
     * @param token           the non-{@code null} 22-character link token
     * @param media           the non-{@code null} media kind
     * @param creator         the optional creator device JID
     * @param creatorPn       the optional creator phone-number JID
     * @param creatorUsername the optional creator display username
     * @param callId          the optional in-flight call id
     * @param scheduled       whether the link is for a scheduled event
     * @param waitingRoom     the optional waiting-room state
     */
    CallLink(String token,
             CallLinkMedia media,
             Jid creator,
             Jid creatorPn,
             String creatorUsername,
             String callId,
             boolean scheduled,
             CallLinkWaitingRoom waitingRoom) {
        this.token = Objects.requireNonNull(token, "token cannot be null");
        this.media = Objects.requireNonNull(media, "media cannot be null");
        this.creator = creator;
        this.creatorPn = creatorPn;
        this.creatorUsername = creatorUsername;
        this.callId = callId;
        this.scheduled = scheduled;
        this.waitingRoom = waitingRoom;
    }

    /**
     * Returns the opaque link token component of the URL.
     *
     * @return the token; never {@code null}
     */
    public String token() {
        return token;
    }

    /**
     * Returns the media kind associated with the link.
     *
     * @return the media kind; never {@code null}
     */
    public CallLinkMedia media() {
        return media;
    }

    /**
     * Returns the creator's device {@link Jid}.
     *
     * @return an {@link Optional} carrying the creator JID, or empty when
     *         the relay did not echo it
     */
    public Optional<Jid> creator() {
        return Optional.ofNullable(creator);
    }

    /**
     * Returns the creator's phone-number {@link Jid}.
     *
     * @return an {@link Optional} carrying the creator PN JID, or empty
     *         when the relay did not surface a separate PN identifier
     */
    public Optional<Jid> creatorPn() {
        return Optional.ofNullable(creatorPn);
    }

    /**
     * Returns the creator-supplied display username.
     *
     * @return an {@link Optional} carrying the username, or empty when the
     *         creator did not set one
     */
    public Optional<String> creatorUsername() {
        return Optional.ofNullable(creatorUsername);
    }

    /**
     * Returns the in-flight call identifier echoed by create replies when
     * the link was created against an existing call.
     *
     * @return an {@link Optional} carrying the call id, or empty when the
     *         link was minted for a fresh call
     */
    public Optional<String> callId() {
        return Optional.ofNullable(callId);
    }

    /**
     * Returns whether the link is associated with a scheduled call event.
     *
     * @return {@code true} when the link is bound to a scheduled event;
     *         {@code false} for ordinary call links
     */
    public boolean isScheduled() {
        return scheduled;
    }

    /**
     * Returns the waiting-room state attached to the link.
     *
     * @return an {@link Optional} carrying the waiting-room descriptor,
     *         or empty when the relay reported no waiting-room state
     */
    public Optional<CallLinkWaitingRoom> waitingRoom() {
        return Optional.ofNullable(waitingRoom);
    }

    /**
     * Returns the canonical URL for this call link.
     *
     * <p>The URL is built from the {@linkplain #media() media kind} and the
     * {@linkplain #token() token}. {@link CallLinkMedia#AUDIO AUDIO} maps to
     * the {@code voice/} prefix, {@link CallLinkMedia#VIDEO VIDEO} maps to
     * the {@code video/} prefix, mirroring the URL space used by the
     * official WhatsApp client.
     *
     * @return the canonical URL string; never {@code null}
     */
    public String url() {
        var prefix = media == CallLinkMedia.VIDEO ? "video" : "voice";
        return "https://call.whatsapp.com/" + prefix + "/" + token;
    }

    /**
     * Compares this call link with the given object for equality.
     *
     * <p>Two call links are equal when they share the same token, media
     * kind, and every optional attribute.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} when {@code obj} is a {@code CallLink} with
     *         equal field values; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CallLink that)) {
            return false;
        }
        return scheduled == that.scheduled
                && Objects.equals(token, that.token)
                && media == that.media
                && Objects.equals(creator, that.creator)
                && Objects.equals(creatorPn, that.creatorPn)
                && Objects.equals(creatorUsername, that.creatorUsername)
                && Objects.equals(callId, that.callId)
                && Objects.equals(waitingRoom, that.waitingRoom);
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(token, media, creator, creatorPn, creatorUsername,
                callId, scheduled, waitingRoom);
    }

    /**
     * Returns a debug-friendly representation of this call link.
     *
     * @return a string with every field value
     */
    @Override
    public String toString() {
        return "CallLink[token=" + token
                + ", media=" + media
                + ", creator=" + creator
                + ", creatorPn=" + creatorPn
                + ", creatorUsername=" + creatorUsername
                + ", callId=" + callId
                + ", scheduled=" + scheduled
                + ", waitingRoom=" + waitingRoom + ']';
    }
}
