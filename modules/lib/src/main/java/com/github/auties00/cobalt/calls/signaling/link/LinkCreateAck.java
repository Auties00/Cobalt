package com.github.auties00.cobalt.calls.signaling.link;

import com.github.auties00.cobalt.wire.linked.call.CallLink;
import com.github.auties00.cobalt.wire.linked.call.CallLinkBuilder;
import com.github.auties00.cobalt.wire.linked.call.CallLinkMedia;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the acknowledgement of a {@link LinkCreateStanza}, the relay's reply minting a call link.
 *
 * <p>The acknowledgement is delivered inside the shared {@code <ack>} envelope, whose body echoes a
 * {@code <link_create>} stanza carrying the minted {@link #token() token} and the {@link #media() media kind}
 * the link was created with, plus the preallocated {@link #callId() call id} when the link was minted against
 * an in flight call. This is a parse only result model, not a transmittable action, so it implements no
 * {@link CallMessage} contract; the decoded result is exposed both as its components and as a composed
 * {@link #toCallLink() CallLink} value.
 *
 * <p>On the wire the acknowledged element is
 * {@snippet lang="xml" :
 * <link_create token="..." media="<type>" call-id="..."/>
 * }
 *
 * @param token  the minted call link token; never {@code null}
 * @param media  the media kind the link was created with; never {@code null}
 * @param callId the preallocated call id echoed for an in flight call link, if present
 * @see SignalingType#LINK_CREATE_ACK
 * @see LinkCreateStanza
 * @see CallLink
 */
public record LinkCreateAck(String token, CallLinkMedia media, Optional<String> callId) {
    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code token}, {@code media}, or {@code callId} is {@code null}
     */
    public LinkCreateAck {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(media, "media cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
    }

    /**
     * Returns this ack as a composed {@link CallLink} value.
     *
     * <p>The resulting link carries the minted token, the media kind, and the optional call id; the
     * creator, creator phone number, creator username, scheduled flag, and waiting room state are left
     * unset because a create acknowledgement does not surface them.
     *
     * @return the composed call link value; never {@code null}
     */
    public CallLink toCallLink() {
        return new CallLinkBuilder()
                .token(token)
                .media(media)
                .callId(callId.orElse(null))
                .build();
    }

    /**
     * Decodes a {@code <link_create>} ack stanza into a {@link LinkCreateAck}.
     *
     * @param stanza the echoed {@code <link_create>} stanza from the {@code <ack>} body
     * @return the decoded link create ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code token} attribute is absent or the
     *                                {@code media} attribute is absent or unrecognized
     */
    public static LinkCreateAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var token = stanza.getRequiredAttributeAsString("token");
        var media = CallLinkMedia.ofWire(stanza.getAttributeAsString("media").orElse(null))
                .orElseThrow(() -> new NoSuchElementException("link_create ack is missing a recognized media attribute"));
        var callId = stanza.getAttributeAsString("call-id");
        return new LinkCreateAck(token, media, callId);
    }
}
