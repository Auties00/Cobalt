package com.github.auties00.cobalt.calls.signaling.link;

import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents the relay's acknowledgement of a {@link LinkEditStanza}.
 *
 * <p>A link edit ack is delivered inside the shared {@code <ack>} envelope, whose body echoes a
 * {@code <link_edit>} element confirming the {@link #token() edited token} and, when the edit touched the
 * waiting room gate, the applied {@link #waitingRoomEnabled() gate state}. This is a parse only result model,
 * not a transmittable action, so it implements no {@link CallMessage} contract.
 *
 * <p>On the wire the acknowledged element is:
 * {@snippet lang = "xml":
 * <link_edit token="..." waiting_room_enabled="1"/>
 *}
 *
 * <p>The {@code waiting_room_enabled} attribute is optional: an ack that did not touch the gate yields an
 * empty {@link #waitingRoomEnabled()}. When present it is carried as the wire boolean literal {@code "1"} or
 * {@code "0"}.
 *
 * @param token              the echoed call link token; never {@code null}
 * @param waitingRoomEnabled the applied waiting room gate state, present only when the ack echoed the toggle
 * @see SignalingType#LINK_EDIT_ACK
 * @see LinkEditStanza
 */
public record LinkEditAck(String token, Optional<Boolean> waitingRoomEnabled) {
    /**
     * The wire attribute naming the echoed call link token.
     */
    private static final String TOKEN_ATTRIBUTE = "token";

    /**
     * The wire attribute naming the waiting room gate state.
     */
    private static final String WAITING_ROOM_ENABLED_ATTRIBUTE = "waiting_room_enabled";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code token} or {@code waitingRoomEnabled} is {@code null}
     */
    public LinkEditAck {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(waitingRoomEnabled, "waitingRoomEnabled cannot be null");
    }

    /**
     * Decodes an echoed {@code <link_edit>} ack element into a {@link LinkEditAck}.
     *
     * <p>Reads the required {@code token} attribute and the optional {@code waiting_room_enabled} attribute,
     * mapping the latter to {@code true} when its wire value equals {@code "1"} and to {@code false}
     * otherwise, or to an empty {@link Optional} when the attribute is absent.
     *
     * @param stanza the echoed {@code <link_edit>} element taken from the {@code <ack>} body
     * @return the decoded link edit ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code token} attribute is absent
     */
    public static LinkEditAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var token = stanza.getRequiredAttributeAsString(TOKEN_ATTRIBUTE);
        var waitingRoomEnabled = stanza.getAttributeAsString(WAITING_ROOM_ENABLED_ATTRIBUTE)
                .map("1"::equals);
        return new LinkEditAck(token, waitingRoomEnabled);
    }
}
