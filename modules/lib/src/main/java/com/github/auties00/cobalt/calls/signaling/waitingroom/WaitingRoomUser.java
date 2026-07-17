package com.github.auties00.cobalt.calls.signaling.waitingroom;

import com.github.auties00.cobalt.calls.engine.participant.CallParticipantAccountKind;
import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents one {@code <user>} entry inside a waiting room action element.
 *
 * <p>Every waiting room operation that carries participants ({@code admit}, {@code deny}, {@code update},
 * and the participant list echoed on the {@code leave} and {@code toggle} acks) nests zero or more
 * {@code <user>} children describing the parties the operation targets. Each entry pins a participant by
 * its user {@link #jid() JID} and decorates it with the same optional vocabulary the group call
 * {@code <user>} stanza uses: the participant's {@link #userPn() phone number JID}, the
 * {@link #username() display username}, the {@link #admin() admin flag}, the {@link #guestName() guest
 * display name}, and the {@link #accountKind() account kind}. The JID is the only required component;
 * every decoration is optional and absent unless the relay supplied it.
 *
 * <p>This record is the building block the waiting room request and ack stanzas compose; it owns the
 * {@code <user>} element grammar so each stanza applies it identically rather than repeating the
 * attribute literals.
 *
 * <p>On the wire the element is {@code <user jid="..." user_pn="..." username="..." is_admin="1"
 * guest_name="..." account_kind="guest"/>}.
 *
 * @implNote This implementation serializes the boolean {@code is_admin} flag as the {@code "1"} and
 * {@code "0"} literals that every boolean attribute on a call stanza uses, and resolves the
 * {@code account_kind} token through the mapping owned by {@link CallParticipantAccountKind}.
 *
 * @param jid         the participant's user JID; never {@code null}
 * @param userPn      the participant's phone number JID, if present
 * @param username    the participant's display username, if present
 * @param admin       whether the participant is flagged as a waiting room admin
 * @param guestName   the participant's guest display name, if present
 * @param accountKind the participant's account kind, present only when the relay supplied an
 *                    {@code account_kind} token
 * @see CallParticipantAccountKind
 */
public record WaitingRoomUser(Jid jid,
                              Optional<Jid> userPn,
                              Optional<String> username,
                              boolean admin,
                              Optional<String> guestName,
                              Optional<CallParticipantAccountKind> accountKind) {
    /**
     * The wire element tag for a waiting room user entry.
     */
    public static final String ELEMENT = "user";

    /**
     * The wire attribute naming the participant's user JID.
     */
    private static final String JID_ATTRIBUTE = "jid";

    /**
     * The wire attribute naming the participant's phone number JID.
     */
    private static final String USER_PN_ATTRIBUTE = "user_pn";

    /**
     * The wire attribute naming the participant's display username.
     */
    private static final String USERNAME_ATTRIBUTE = "username";

    /**
     * The wire attribute naming the waiting room admin flag.
     */
    private static final String IS_ADMIN_ATTRIBUTE = "is_admin";

    /**
     * The wire attribute naming the participant's guest display name.
     */
    private static final String GUEST_NAME_ATTRIBUTE = "guest_name";

    /**
     * The wire attribute naming the participant's account kind.
     */
    private static final String ACCOUNT_KIND_ATTRIBUTE = "account_kind";

    /**
     * Validates the record components.
     *
     * @throws NullPointerException if {@code jid}, {@code userPn}, {@code username}, {@code guestName},
     *                              or {@code accountKind} is {@code null}
     */
    public WaitingRoomUser {
        Objects.requireNonNull(jid, "jid cannot be null");
        Objects.requireNonNull(userPn, "userPn cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(guestName, "guestName cannot be null");
        Objects.requireNonNull(accountKind, "accountKind cannot be null");
    }

    /**
     * Returns a waiting room user entry that pins a participant by JID alone.
     *
     * <p>The decoration attributes are left absent; this is the shape an {@code admit} or {@code deny}
     * request uses when it only needs to identify the participant to act on.
     *
     * @param jid the participant's user JID
     * @return the user entry
     * @throws NullPointerException if {@code jid} is {@code null}
     */
    public static WaitingRoomUser of(Jid jid) {
        return new WaitingRoomUser(jid, Optional.empty(), Optional.empty(), false, Optional.empty(),
                Optional.empty());
    }

    /**
     * Builds the {@code <user jid user_pn username is_admin guest_name account_kind/>} stanza.
     *
     * <p>Each optional attribute is omitted when its backing component is absent; the {@code is_admin}
     * flag is written only when {@link #admin()} is {@code true}, and {@code account_kind} is written
     * using the kind's wire token only when an account kind is present.
     *
     * @return the user entry stanza
     */
    public Stanza toNode() {
        return new StanzaBuilder()
                .description(ELEMENT)
                .attribute(JID_ATTRIBUTE, jid)
                .attribute(USER_PN_ATTRIBUTE, userPn.orElse(null), userPn.isPresent())
                .attribute(USERNAME_ATTRIBUTE, username.orElse(null), username.isPresent())
                .attribute(IS_ADMIN_ATTRIBUTE, "1", admin)
                .attribute(GUEST_NAME_ATTRIBUTE, guestName.orElse(null), guestName.isPresent())
                .attribute(ACCOUNT_KIND_ATTRIBUTE, accountKind.map(CallParticipantAccountKind::token).orElse(null),
                        accountKind.isPresent())
                .build();
    }

    /**
     * Decodes a waiting room {@code <user>} stanza into a {@link WaitingRoomUser}.
     *
     * <p>An absent {@code is_admin} attribute classifies to {@code false}; an absent {@code account_kind}
     * attribute yields an empty {@link #accountKind()} rather than defaulting to
     * {@link CallParticipantAccountKind#REGULAR}, so a re emitted entry omits the attribute exactly as
     * it arrived.
     *
     * @param stanza the {@code <user>} stanza
     * @return the decoded user entry
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code jid} attribute is absent
     */
    public static WaitingRoomUser of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var jid = stanza.getRequiredAttributeAsJid(JID_ATTRIBUTE);
        var userPn = stanza.getAttributeAsJid(USER_PN_ATTRIBUTE);
        var username = stanza.getAttributeAsString(USERNAME_ATTRIBUTE);
        var admin = "1".equals(stanza.getAttributeAsString(IS_ADMIN_ATTRIBUTE).orElse("0"));
        var guestName = stanza.getAttributeAsString(GUEST_NAME_ATTRIBUTE);
        var accountKind = stanza.getAttributeAsString(ACCOUNT_KIND_ATTRIBUTE)
                .map(CallParticipantAccountKind::ofToken);
        return new WaitingRoomUser(jid, userPn, username, admin, guestName, accountKind);
    }
}
