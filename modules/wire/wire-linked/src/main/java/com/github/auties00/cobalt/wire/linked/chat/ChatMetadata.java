package com.github.auties00.cobalt.wire.linked.chat;

import com.github.auties00.cobalt.wire.linked.chat.community.CommunityMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupMetadata;
import com.github.auties00.cobalt.wire.linked.chat.group.GroupParticipant;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the shared metadata of a WhatsApp group or community chat.
 *
 * <p>Every group and community on WhatsApp has associated metadata such as
 * a subject (display name), founder, description, participant list, and
 * ephemeral message settings. This sealed interface captures the properties
 * that are common to both {@link GroupMetadata} and {@link CommunityMetadata},
 * allowing callers to work with either type through a single abstraction.
 *
 * <p>Concrete implementations carry additional type-specific state:
 * {@code GroupMetadata} holds group-level settings and an optional parent
 * community link, while {@code CommunityMetadata} holds community-level
 * settings and the set of linked sub-groups.
 *
 * <p>Metadata is typically fetched from the server when joining or opening a
 * group, and is kept in sync via group notification stanzas and history sync
 * payloads.
 */
public sealed interface ChatMetadata permits GroupMetadata, CommunityMetadata {

    /**
     * Returns the JID that uniquely identifies this group or community.
     *
     * @return the non-{@code null} JID
     */
    Jid jid();

    /**
     * Returns the subject (display name) of this group or community.
     *
     * @return the non-{@code null} subject string
     */
    String subject();

    /**
     * Returns the JID of the participant who last set the subject, if known.
     *
     * @return an {@code Optional} containing the author JID, or empty if
     *         the author is not available
     */
    Optional<Jid> subjectAuthorJid();

    /**
     * Returns the instant at which the subject was last changed, if known.
     *
     * @return an {@code Optional} containing the subject timestamp, or empty
     *         if the timestamp is not available
     */
    Optional<Instant> subjectTimestamp();

    /**
     * Returns the instant at which this group or community was created, if
     * known.
     *
     * @return an {@code Optional} containing the foundation timestamp, or
     *         empty if the timestamp is not available
     */
    Optional<Instant> foundationTimestamp();

    /**
     * Returns the JID of the user who originally created this group or
     * community, if known.
     *
     * @return an {@code Optional} containing the founder JID, or empty if
     *         the founder is not available
     */
    Optional<Jid> founderJid();

    /**
     * Returns the description text of this group or community, if one has
     * been set.
     *
     * @return an {@code Optional} containing the description, or empty if
     *         no description has been set
     */
    Optional<String> description();

    /**
     * Returns the server-assigned identifier for the current description
     * revision, if available.
     *
     * @return an {@code Optional} containing the description identifier, or
     *         empty if no description identifier is available
     */
    Optional<String> descriptionId();

    /**
     * Returns an unmodifiable view of the current participants in this group
     * or community.
     *
     * @return an unmodifiable {@code Set} of participants, never
     *         {@code null}
     */
    Set<GroupParticipant> participants();

    /**
     * Adds a participant to this group or community.
     *
     * @param participant the non-{@code null} participant to add
     * @return {@code true} if the participant was added
     */
    boolean addParticipant(GroupParticipant participant);

    /**
     * Removes the specified participant from this group or community.
     *
     * @param participant the non-{@code null} participant to remove
     * @return {@code true} if the participant was present and removed
     */
    boolean removeParticipant(GroupParticipant participant);

    /**
     * Removes the participant identified by the given JID from this group
     * or community.
     *
     * @param jid the non-{@code null} JID of the participant to remove
     * @return {@code true} if a matching participant was found and removed
     */
    boolean removeParticipant(Jid jid);

    /**
     * Removes all participants from this group or community.
     */
    void clearParticipants();

    /**
     * Adds all participants in the given collection to this group or
     * community.
     *
     * @param participants the non-{@code null} collection of participants
     *        to add
     * @return {@code true} if the participants were added
     */
    boolean addAllParticipants(Collection<GroupParticipant> participants);

    /**
     * Returns the ephemeral message timer for this group or community, if
     * one has been configured.
     *
     * <p>When ephemeral messaging is enabled, messages sent to this group or
     * community are automatically deleted after the duration specified by the
     * returned {@link ChatEphemeralTimer}. An empty result indicates that
     * ephemeral messaging is disabled and messages are retained indefinitely.
     *
     * @return an {@code Optional} containing the ephemeral timer, or empty if
     *         ephemeral messaging is disabled
     */
    Optional<ChatEphemeralTimer> ephemeralExpiration();

    /**
     * Sets the ephemeral message timer for this group or community.
     *
     * <p>Pass {@code null} to disable ephemeral messaging so that messages
     * are retained indefinitely.
     *
     * @param ephemeralExpiration the ephemeral timer to set, or {@code null}
     *        to disable ephemeral messaging
     */
    void setEphemeralExpiration(ChatEphemeralTimer ephemeralExpiration);

    /**
     * Returns whether this group or community uses LID (Linked Identity)
     * addressing mode instead of phone-number addressing.
     *
     * @return {@code true} if LID addressing is active
     */
    boolean isLidAddressingMode();

    /**
     * Sets whether this group or community uses LID addressing mode.
     *
     * @param lidAddressingMode {@code true} to enable LID addressing,
     *        {@code false} to disable it
     */
    void setLidAddressingMode(boolean lidAddressingMode);

    /**
     * Returns whether this group or community operates in incognito mode.
     *
     * @return {@code true} if incognito mode is enabled
     */
    boolean isIncognito();

    /**
     * Returns whether the open Meta AI bot is enabled in this group or
     * community.
     *
     * <p>When enabled, the Meta AI bot can participate in the group
     * conversation and respond to messages from group members.
     *
     * @return {@code true} if the open bot group feature is active
     */
    boolean isOpenBotGroup();

    /**
     * Sets whether the open Meta AI bot is enabled in this group or
     * community.
     *
     * @param openBotGroup {@code true} to enable, {@code false} to disable
     */
    void setOpenBotGroup(boolean openBotGroup);
}
