package com.github.auties00.cobalt.calls.signaling.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.stanza.model.Stanza;
import com.github.auties00.cobalt.stanza.model.StanzaBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import com.github.auties00.cobalt.calls.signaling.CallMessage;
import com.github.auties00.cobalt.calls.signaling.CallMessages;
import com.github.auties00.cobalt.calls.signaling.SignalingType;

/**
 * Represents a {@code <group_update>} action: a mid call membership and configuration update for an
 * in progress group call.
 *
 * <p>A group update is the engine's omnibus mid call message: it refreshes the participant roster,
 * re applies media and transport configuration, and signals audio to video upgrade intent in one
 * element. It carries the universal call header plus a bundle of optional children, each owned by a
 * different subsystem: the {@code <voip_settings>} tuning bundle, the {@code <relay>} candidate set,
 * the {@code <group_info>} roster, and the {@code <bot_info>}, {@code <extension_info>}, and
 * {@code <link_info>} blocks. Only the {@code <group_info>} roster is typed here, via
 * {@link GroupInfoStanza}; the other blocks are carried as opaque {@link Stanza} trees and forwarded
 * uninterpreted because their typed parse is owned by the configuration, transport, bot, extension,
 * and call link subsystems respectively.
 *
 * <p>The audio to video upgrade is signaled by the {@code av-upgrader}, {@code av-upgradable}, and
 * {@code av_upgrade} attributes on the element itself, modeled here as {@link #avUpgrader()},
 * {@link #avUpgradable()}, and {@link #avUpgrade()}.
 *
 * <p>On the wire the element is
 * {@code <group_update call-id="..." call-creator="..."> <voip_settings/>? <relay/>? <group_info/>?
 * <bot_info/>? <extension_info/>? <link_info/>? </group_update>} with the AV upgrade attributes stamped
 * on the {@code <group_update>} element. Every child is optional; an update may carry any subset.
 *
 * @see GroupInfoStanza
 * @see DestinationStanza
 * @see SignalingType#GROUP_UPDATE
 */
public final class GroupUpdateStanza implements CallMessage {
    /**
     * The wire element tag for a group update action.
     */
    public static final String ELEMENT = "group_update";

    /**
     * The wire attribute naming the audio to video upgrade initiator.
     */
    private static final String AV_UPGRADER_ATTRIBUTE = "av-upgrader";

    /**
     * The wire attribute marking the call as eligible for an audio to video upgrade.
     */
    private static final String AV_UPGRADABLE_ATTRIBUTE = "av-upgradable";

    /**
     * The wire attribute marking an in progress audio to video upgrade.
     */
    private static final String AV_UPGRADE_ATTRIBUTE = "av_upgrade";

    /**
     * The wire literal a set voip boolean attribute carries.
     */
    private static final String FLAG_TRUE = "1";

    /**
     * The wire literal a clear voip boolean attribute carries.
     */
    private static final String FLAG_FALSE = "0";

    /**
     * The call identifier this group update's {@code call-id} header carries.
     */
    private final String callId;

    /**
     * The call creator device JID this group update's {@code call-creator} header carries.
     */
    private final Jid callCreator;

    /**
     * The {@code av-upgrader} attribute value, or {@code null} when absent.
     */
    private final String avUpgrader;

    /**
     * Whether the {@code av-upgradable} attribute is set.
     */
    private final boolean avUpgradable;

    /**
     * Whether the {@code av_upgrade} attribute is set.
     */
    private final boolean avUpgrade;

    /**
     * The typed membership roster, or {@code null} when no {@code <group_info>} child is present.
     */
    private final GroupInfoStanza groupInfo;

    /**
     * Every bundle child other than the typed {@code <group_info>} roster as opaque nodes (the
     * {@code voip_settings}, {@code relay}, {@code bot_info}, {@code extension_info}, and
     * {@code link_info} blocks plus any further child the engine adds to the update); never
     * {@code null}, possibly empty.
     */
    private final List<Stanza> extraChildren;

    /**
     * Constructs a group update, defensively copying the extra children list and validating the
     * required header.
     *
     * @param callId        the call identifier; never {@code null}
     * @param callCreator   the call creator's device JID; never {@code null}
     * @param avUpgrader    the {@code av-upgrader} attribute value, or {@code null} when absent
     * @param avUpgradable  whether the {@code av-upgradable} attribute is set
     * @param avUpgrade     whether the {@code av_upgrade} attribute is set
     * @param groupInfo     the typed membership roster, or {@code null} when no {@code <group_info>}
     *                      child is present
     * @param extraChildren every bundle child other than the typed {@code <group_info>} roster as opaque
     *                      nodes (the {@code voip_settings}, {@code relay}, {@code bot_info},
     *                      {@code extension_info}, and {@code link_info} blocks plus any further child the
     *                      engine adds to the update); never {@code null}, possibly empty
     * @throws NullPointerException     if {@code callId}, {@code callCreator}, or {@code extraChildren}
     *                                  is {@code null}, or any extra child is {@code null}
     * @throws IllegalArgumentException if any extra child is a {@code <group_info>} element, which must
     *                                  be supplied through {@code groupInfo} rather than the opaque
     *                                  list
     */
    public GroupUpdateStanza(String callId, Jid callCreator, String avUpgrader, boolean avUpgradable,
                             boolean avUpgrade, GroupInfoStanza groupInfo,
                             List<Stanza> extraChildren) {
        this.callId = Objects.requireNonNull(callId, "callId cannot be null");
        this.callCreator = Objects.requireNonNull(callCreator, "callCreator cannot be null");
        Objects.requireNonNull(extraChildren, "extraChildren cannot be null");
        extraChildren = List.copyOf(extraChildren);
        for (var child : extraChildren) {
            if (child.hasDescription(GroupInfoStanza.ELEMENT)) {
                throw new IllegalArgumentException("group_info must be supplied through the groupInfo component, not extraChildren");
            }
        }
        this.avUpgrader = avUpgrader;
        this.avUpgradable = avUpgradable;
        this.avUpgrade = avUpgrade;
        this.groupInfo = groupInfo;
        this.extraChildren = extraChildren;
    }

    /**
     * {@inheritDoc}
     *
     * @return the call identifier, always present for a group update
     */
    @Override
    public Optional<String> callId() {
        return Optional.of(callId);
    }

    /**
     * {@inheritDoc}
     *
     * @return the call creator device JID, always present for a group update
     */
    @Override
    public Optional<Jid> callCreator() {
        return Optional.of(callCreator);
    }

    /**
     * Returns the {@code av-upgrader} attribute value, or {@code null} when absent.
     *
     * @return the audio to video upgrade initiator, or {@code null} when absent
     */
    public String avUpgrader() {
        return avUpgrader;
    }

    /**
     * Returns whether the {@code av-upgradable} attribute is set.
     *
     * @return {@code true} when the call is eligible for an audio to video upgrade
     */
    public boolean avUpgradable() {
        return avUpgradable;
    }

    /**
     * Returns whether the {@code av_upgrade} attribute is set.
     *
     * @return {@code true} when an audio to video upgrade is in progress
     */
    public boolean avUpgrade() {
        return avUpgrade;
    }

    /**
     * Returns the typed membership roster, or {@code null} when no {@code <group_info>} child is
     * present.
     *
     * @return the membership roster, or {@code null} when absent
     */
    public GroupInfoStanza groupInfo() {
        return groupInfo;
    }

    /**
     * Returns every bundle child other than the typed {@code <group_info>} roster.
     *
     * @return the immutable list of opaque bundle children; never {@code null}, possibly empty
     */
    public List<Stanza> extraChildren() {
        return extraChildren;
    }

    /**
     * Returns the audio to video upgrade initiator, if present.
     *
     * @return an {@link Optional} holding the {@code av-upgrader} value, or empty when absent
     */
    public Optional<String> avUpgraderValue() {
        return Optional.ofNullable(avUpgrader);
    }

    /**
     * Returns the typed membership roster, if present.
     *
     * @return an {@link Optional} holding the {@code <group_info>} roster, or empty when absent
     */
    public Optional<GroupInfoStanza> groupInfoValue() {
        return Optional.ofNullable(groupInfo);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link SignalingType#GROUP_UPDATE}, the message type of a group update
     */
    @Override
    public SignalingType type() {
        return SignalingType.GROUP_UPDATE;
    }

    /**
     * Builds the {@code <group_update>} action stanza.
     *
     * <p>The common header is stamped first, then the AV upgrade attributes are written when set, then
     * the {@code <group_info>} roster (when present) and the forwarded bundle children are emitted as
     * content. An absent AV upgrade attribute, an absent roster, and an empty extra children list are
     * each omitted rather than written as a sentinel.
     *
     * @return the group update action stanza
     */
    @Override
    public Stanza toStanza() {
        var children = new ArrayList<Stanza>();
        if (groupInfo != null) {
            children.add(groupInfo.toStanza());
        }
        children.addAll(extraChildren);
        var builder = CallMessages.stampHeader(new StanzaBuilder().description(ELEMENT), callId, callCreator)
                .attribute(AV_UPGRADER_ATTRIBUTE, avUpgrader)
                .attribute(AV_UPGRADABLE_ATTRIBUTE, FLAG_TRUE, avUpgradable)
                .attribute(AV_UPGRADE_ATTRIBUTE, FLAG_TRUE, avUpgrade);
        if (!children.isEmpty()) {
            builder.content(children);
        }
        return builder.build();
    }

    /**
     * Decodes a {@code <group_update>} action stanza into a {@link GroupUpdateStanza}.
     *
     * <p>The {@code <group_info>} child is decoded through {@link GroupInfoStanza#of(Stanza)}; every other
     * child is carried through uninterpreted into {@link #extraChildren()}. The known bundle blocks
     * ({@code voip_settings}, {@code relay}, {@code bot_info}, {@code extension_info}, {@code link_info})
     * and any further child the engine adds to the omnibus update are all forwarded verbatim rather than
     * confined to a fixed vocabulary, so an unrecognized bundle child round trips instead of being
     * dropped.
     *
     * @param stanza the {@code <group_update>} stanza
     * @return the decoded group update
     * @throws NullPointerException   if {@code stanza} is {@code null}
     * @throws NoSuchElementException if the required {@code call-id} or {@code call-creator} attribute
     *                                is absent
     */
    public static GroupUpdateStanza of(Stanza stanza) {
        Objects.requireNonNull(stanza, "stanza cannot be null");
        var callId = stanza.getRequiredAttributeAsString(CallMessages.CALL_ID_ATTRIBUTE);
        var callCreator = stanza.getRequiredAttributeAsJid(CallMessages.CALL_CREATOR_ATTRIBUTE);
        var avUpgrader = stanza.getAttributeAsString(AV_UPGRADER_ATTRIBUTE, null);
        var avUpgradable = FLAG_TRUE.equals(stanza.getAttributeAsString(AV_UPGRADABLE_ATTRIBUTE, FLAG_FALSE));
        var avUpgrade = FLAG_TRUE.equals(stanza.getAttributeAsString(AV_UPGRADE_ATTRIBUTE, FLAG_FALSE));
        var groupInfo = stanza.getChild(GroupInfoStanza.ELEMENT)
                .flatMap(GroupInfoStanza::of)
                .orElse(null);
        var extraChildren = stanza.streamChildren()
                .filter(child -> !child.hasDescription(GroupInfoStanza.ELEMENT))
                .toList();
        return new GroupUpdateStanza(callId, callCreator, avUpgrader, avUpgradable, avUpgrade, groupInfo, extraChildren);
    }

    /**
     * Returns whether {@code obj} is a {@link GroupUpdateStanza} with equal components.
     *
     * @param obj the object to compare against
     * @return {@code true} when {@code obj} is a group update equal by value
     */
    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof GroupUpdateStanza that
                && callId.equals(that.callId)
                && callCreator.equals(that.callCreator)
                && Objects.equals(avUpgrader, that.avUpgrader)
                && avUpgradable == that.avUpgradable
                && avUpgrade == that.avUpgrade
                && Objects.equals(groupInfo, that.groupInfo)
                && extraChildren.equals(that.extraChildren));
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the value hash of this group update
     */
    @Override
    public int hashCode() {
        return Objects.hash(callId, callCreator, avUpgrader, avUpgradable, avUpgrade, groupInfo, extraChildren);
    }

    /**
     * Returns a debug string for this group update.
     *
     * @return the debug string
     */
    @Override
    public String toString() {
        return "GroupUpdateStanza[callId=" + callId
                + ", callCreator=" + callCreator
                + ", avUpgrader=" + avUpgrader
                + ", avUpgradable=" + avUpgradable
                + ", avUpgrade=" + avUpgrade
                + ", groupInfo=" + groupInfo
                + ", extraChildren=" + extraChildren + ']';
    }
}
