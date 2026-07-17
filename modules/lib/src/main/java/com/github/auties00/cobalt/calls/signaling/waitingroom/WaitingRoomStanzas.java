package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.List;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Holds the shared {@code <waiting_room>} element grammar the waiting room action records compose.
 *
 * <p>The five waiting room operations ({@code leave}, {@code toggle}, {@code admit}, {@code deny}, and
 * {@code update}) are distinct signaling message types that share one element shape: the universal
 * {@code call-id}/{@code call-creator} header, an optional {@code enabled} gate flag, an optional
 * {@code link-token}, an optional {@code is_admin} flag, and a list of {@code <user>} children. This holder
 * centralizes the attribute names and the build and parse of that common shape so each operation record
 * applies it identically rather than repeating the literals, while still emitting its own operation element
 * tag and carrying its own {@link SignalingType}. Each nested {@code <user>} entry is handled by
 * {@link WaitingRoomUser}. It holds no state and is not instantiable.
 *
 * @implNote This implementation serializes the {@code enabled} and {@code is_admin} attributes as the
 * {@code '1'}/{@code '0'} literals WhatsApp uses for every voip boolean attribute, not the JSON
 * {@code true}/{@code false} tokens.
 */
final class WaitingRoomStanzas {
    /**
     * The wire attribute naming the waiting room enabled gate flag.
     */
    static final String ENABLED_ATTRIBUTE = "enabled";

    /**
     * The wire attribute naming the call link token a waiting room operation targets.
     */
    static final String LINK_TOKEN_ATTRIBUTE = "link-token";

    /**
     * The wire attribute naming the waiting room admin flag.
     */
    static final String IS_ADMIN_ATTRIBUTE = "is_admin";

    /**
     * The wire literal a set voip boolean attribute carries.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal a cleared voip boolean attribute carries.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * Prevents instantiation of this utility holder.
     *
     * @throws AssertionError always, since this class is not instantiable
     */
    private WaitingRoomStanzas() {
        throw new AssertionError("WaitingRoomStanzas is not instantiable");
    }

    /**
     * Builds a waiting room action stanza carrying the common shape.
     *
     * <p>Stamps the universal call header, then writes the optional {@code enabled}, {@code link-token},
     * and {@code is_admin} attributes only when present, and nests every supplied {@link WaitingRoomUser}
     * as a {@code <user>} child. The {@code enabled} and {@code is_admin} flags are written using the
     * {@code '1'}/{@code '0'} literal only when their value is present.
     *
     * @param element     the operation wire element tag
     * @param callId      the call identifier
     * @param callCreator the call creator's device JID
     * @param enabled     the gate flag, absent to omit
     * @param linkToken   the targeted call link token, absent to omit
     * @param admin       the admin flag, absent to omit
     * @param users       the user entries to nest; never {@code null}
     * @return the waiting room action stanza
     */
    static Stanza build(String element,
                        String callId,
                        Jid callCreator,
                        Optional<Boolean> enabled,
                        Optional<String> linkToken,
                        Optional<Boolean> admin,
                        List<WaitingRoomUser> users) {
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(element), callId, callCreator)
                .attribute(ENABLED_ATTRIBUTE, enabled.map(value -> value ? FLAG_TRUE : FLAG_FALSE).orElse(null), enabled.isPresent())
                .attribute(LINK_TOKEN_ATTRIBUTE, linkToken.orElse(null), linkToken.isPresent())
                .attribute(IS_ADMIN_ATTRIBUTE, admin.map(value -> value ? FLAG_TRUE : FLAG_FALSE).orElse(null), admin.isPresent());
        if (!users.isEmpty()) {
            builder.content(users.stream().map(WaitingRoomUser::toNode).toList());
        }
        return builder.build();
    }

    /**
     * Decodes the {@code enabled} gate flag from a waiting room action stanza.
     *
     * <p>The flag is read from the {@code '1'}/{@code '0'} voip boolean literal: a present {@code "1"}
     * maps to {@code true} and any other present value to {@code false}, while an absent attribute stays
     * empty so the gate stays omitted when the stanza is rebuilt exactly as it arrived.
     *
     * @param stanza the waiting room action stanza
     * @return the gate flag, or empty when the attribute is absent
     */
    static Optional<Boolean> enabled(Stanza stanza) {
        return stanza.getAttributeAsString(ENABLED_ATTRIBUTE)
                .map(FLAG_TRUE::equals);
    }

    /**
     * Decodes the {@code is_admin} flag from a waiting room action stanza.
     *
     * <p>The flag is read from the {@code '1'}/{@code '0'} voip boolean literal: a present {@code "1"}
     * maps to {@code true} and any other present value to {@code false}, while an absent attribute stays
     * empty.
     *
     * @param stanza the waiting room action stanza
     * @return the admin flag, or empty when the attribute is absent
     */
    static Optional<Boolean> admin(Stanza stanza) {
        return stanza.getAttributeAsString(IS_ADMIN_ATTRIBUTE)
                .map(FLAG_TRUE::equals);
    }

    /**
     * Decodes the {@code link-token} from a waiting room action stanza.
     *
     * @param stanza the waiting room action stanza
     * @return the targeted call link token, or empty when the attribute is absent
     */
    static Optional<String> linkToken(Stanza stanza) {
        return stanza.getAttributeAsString(LINK_TOKEN_ATTRIBUTE);
    }

    /**
     * Decodes the nested {@code <user>} list from a waiting room action stanza.
     *
     * @param stanza the waiting room action stanza
     * @return the decoded user entries; never {@code null}, empty when the stanza nests none
     */
    static List<WaitingRoomUser> users(Stanza stanza) {
        return stanza.streamChildren(WaitingRoomUser.ELEMENT)
                .map(WaitingRoomUser::of)
                .toList();
    }
}
