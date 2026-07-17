package com.github.auties00.cobalt.calls.signaling.link;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;
import com.github.auties00.cobalt.calls.signaling.group.GroupInfoStanza;
import com.github.auties00.cobalt.calls.signaling.group.GroupUpdateStanza;
import com.github.auties00.cobalt.calls.signaling.relay.RelayInfo;
import com.github.auties00.cobalt.calls.signaling.waitingroom.WaitingRoomUser;

/**
 * Represents the acknowledgement of a {@link LinkJoinStanza}, the relay's reply admitting a join.
 *
 * <p>The acknowledgement is delivered inside the shared {@code <ack>} envelope, whose body echoes a
 * {@code <link_join>} stanza. It names the {@link #callId() call} the joined device entered, the
 * {@link #callCreator() call creator} it must address, and echoes the {@link #token() join token}, which
 * the join handler matches against the request and rejects when it differs. It carries the call's
 * {@link #groupInfo() membership roster} and {@link #voipSettings() tuning block}, an optional
 * {@link #event() event flag} and {@link #linkInfoValue() link info block}, and, for a waiting room link,
 * the lobby {@link #waitingRoomUsers() participant list}.
 *
 * <p>On the wire the acknowledged element is shaped as:
 * {@snippet lang="xml" :
 * <link_join token="..." call-id="..." call-creator="..." event="0|1">
 *   <group_info .../>
 *   <voip_settings .../>
 *   <!-- optional -->
 *   <link_info .../>
 *   <!-- zero or more -->
 *   <user .../>
 * </link_join>
 * }
 * The {@code <group_info>} roster is given a typed projection through {@link GroupInfoStanza}; the
 * {@code <voip_settings>} and {@code <link_info>} blocks are forwarded as opaque {@link Stanza} trees
 * because their typed parse is owned by the configuration and call link subsystems, matching the split
 * {@link GroupUpdateStanza} uses.
 *
 * <p>The ack carries no relay candidates. A joiner's relay endpoints reach the media plane through the
 * call's offer (whose sibling {@code <relay>} block, modeled by {@link RelayInfo}, is consumed by the
 * group call update path) or a later {@code <group_update>} broadcast, never through this reply.
 *
 * @implNote This implementation keeps the {@code <voip_settings>} and {@code <link_info>} blocks as raw
 * {@link Stanza} trees rather than typed models because their parse is owned by the configuration
 * ({@link com.github.auties00.cobalt.calls.config.VoipSettings}) and call link subsystems.
 *
 * @param token            the echoed join token; never {@code null}
 * @param callId           the call identifier the relay admitted the joined device into; never
 *                         {@code null}
 * @param callCreator      the call creator the joined device must address; never {@code null}
 * @param groupInfo        the membership roster snapshot the joined device reconciles against; never
 *                         {@code null}
 * @param voipSettings     the raw {@code <voip_settings>} tuning block, parsed by the configuration
 *                         subsystem; never {@code null}
 * @param event            whether the ack stamped the {@code event} flag
 * @param linkInfo         the raw {@code <link_info>} block carrying the link creator metadata, or
 *                         {@code null} when the ack carried none
 * @param waitingRoomUsers the lobby participant list for a waiting room link; never {@code null}, empty
 *                         when the ack carries no participants
 * @see SignalingType#LINK_JOIN_ACK
 * @see LinkJoinStanza
 * @see GroupInfoStanza
 * @see WaitingRoomUser
 * @see RelayInfo
 */
public record LinkJoinAck(String token, String callId, Jid callCreator, GroupInfoStanza groupInfo,
                          Stanza voipSettings, boolean event, Stanza linkInfo,
                          List<WaitingRoomUser> waitingRoomUsers) {
    /**
     * The wire attribute naming the echoed join token.
     */
    private static final String TOKEN_ATTRIBUTE = "token";

    /**
     * The wire element tag for the {@code <voip_settings>} tuning block.
     */
    private static final String VOIP_SETTINGS_ELEMENT = "voip_settings";

    /**
     * The wire attribute naming the event flag.
     */
    private static final String EVENT_ATTRIBUTE = "event";

    /**
     * The wire element tag for the {@code <link_info>} link creator block.
     */
    private static final String LINK_INFO_ELEMENT = "link_info";

    /**
     * Validates the required record components and defensively copies the participant list.
     *
     * @throws NullPointerException if {@code token}, {@code callId}, {@code callCreator},
     *                              {@code groupInfo}, {@code voipSettings}, or {@code waitingRoomUsers}
     *                              is {@code null}
     */
    public LinkJoinAck {
        Objects.requireNonNull(token, "token cannot be null");
        Objects.requireNonNull(callId, "callId cannot be null");
        Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(groupInfo, "groupInfo cannot be null");
        Objects.requireNonNull(voipSettings, "voipSettings cannot be null");
        Objects.requireNonNull(waitingRoomUsers, "waitingRoomUsers cannot be null");
        waitingRoomUsers = List.copyOf(waitingRoomUsers);
    }

    /**
     * Decodes a {@code <link_join>} ack stanza into a {@link LinkJoinAck}.
     *
     * <p>The required {@code token}, {@code call-id}, and {@code call-creator} attributes and the
     * required {@code <group_info>} and {@code <voip_settings>} children are read first; an absent
     * required attribute or child raises a {@link NoSuchElementException} so an error reply with no
     * echoed body surfaces the missing field rather than yielding a partially filled record. The optional
     * {@code event} flag defaults to {@code false} when absent, the optional {@code <link_info>} block is
     * kept verbatim when present and {@code null} otherwise, and every nested {@code <user>} child is
     * decoded into a {@link WaitingRoomUser} forming the lobby participant list, empty when the ack
     * carries no participants.
     *
     * @param stanza the echoed {@code <link_join>} stanza from the {@code <ack>} body
     * @return the decoded link join ack
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code token}, {@code call-id}, or
     *                                {@code call-creator} attribute, or the required {@code <group_info>}
     *                                or {@code <voip_settings>} child, is absent
     */
    public static LinkJoinAck of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var token = stanza.getRequiredAttributeAsString(TOKEN_ATTRIBUTE);
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var groupInfo = stanza.getChild(GroupInfoStanza.ELEMENT)
                .flatMap(GroupInfoStanza::of)
                .orElseThrow(() -> new NoSuchElementException("link_join ack is missing a required <group_info> roster"));
        var voipSettings = stanza.getChild(VOIP_SETTINGS_ELEMENT)
                .orElseThrow(() -> new NoSuchElementException("link_join ack is missing a required <voip_settings> block"));
        var event = "1".equals(stanza.getAttributeAsString(EVENT_ATTRIBUTE).orElse("0"));
        var linkInfo = stanza.getChild(LINK_INFO_ELEMENT).orElse(null);
        var waitingRoomUsers = stanza.streamChildren(WaitingRoomUser.ELEMENT)
                .map(WaitingRoomUser::of)
                .toList();
        return new LinkJoinAck(token, callId, callCreator, groupInfo, voipSettings, event, linkInfo, waitingRoomUsers);
    }

    /**
     * Returns the raw {@code <link_info>} block, if the ack carried one.
     *
     * @return an {@link Optional} holding the {@code <link_info>} stanza, or empty when absent
     */
    public Optional<Stanza> linkInfoValue() {
        return Optional.ofNullable(linkInfo);
    }
}
