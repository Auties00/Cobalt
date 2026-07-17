package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code LinkedWhatsAppClient.suggestNewSubgroup} — recommend
 * a brand-new sub-group be linked under a community parent.
 *
 * <p>{@link #community} and {@link #subject} are required.
 * {@link #description} is optional. The three boolean toggles seed the
 * sub-group's settings; they default to {@code false} when unset on
 * the builder.
 */
@ProtobufMessage
public final class SubgroupSuggestionNew {
    /**
     * JID of the parent community.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid community;

    /**
     * Display subject for the suggested sub-group.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String subject;

    /**
     * Optional description for the suggested sub-group.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String description;

    /**
     * Whether to seed the sub-group with the locked toggle.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean locked;

    /**
     * Whether to seed the sub-group with the announcement-mode toggle.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.BOOL)
    final boolean announcement;

    /**
     * Whether the sub-group should be hidden from the parent community's
     * directory.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean hiddenGroup;

    /**
     * Constructs a new {@code SubgroupSuggestionNew}.
     *
     * @param community    the parent community JID; required
     * @param subject      the sub-group subject; required
     * @param description  the sub-group description, or {@code null}
     * @param locked       whether to seed with the locked toggle
     * @param announcement whether to seed with announcement mode
     * @param hiddenGroup  whether the sub-group should be hidden
     * @throws NullPointerException if {@code community} or {@code subject}
     *                              is {@code null}
     */
    SubgroupSuggestionNew(Jid community, String subject, String description, boolean locked,
                          boolean announcement, boolean hiddenGroup) {
        this.community = Objects.requireNonNull(community, "community cannot be null");
        this.subject = Objects.requireNonNull(subject, "subject cannot be null");
        this.description = description;
        this.locked = locked;
        this.announcement = announcement;
        this.hiddenGroup = hiddenGroup;
    }

    /**
     * Returns the parent community JID.
     *
     * @return the community JID, never {@code null}
     */
    public Jid community() {
        return community;
    }

    /**
     * Returns the sub-group subject.
     *
     * @return the subject, never {@code null}
     */
    public String subject() {
        return subject;
    }

    /**
     * Returns the optional sub-group description.
     *
     * @return an {@link Optional} carrying the description, or empty
     *         when unset
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns whether the sub-group is seeded with the locked toggle.
     *
     * @return {@code true} when the sub-group is locked
     */
    public boolean locked() {
        return locked;
    }

    /**
     * Returns whether the sub-group is seeded with announcement mode.
     *
     * @return {@code true} when announcement mode is enabled
     */
    public boolean announcement() {
        return announcement;
    }

    /**
     * Returns whether the sub-group is hidden from the community
     * directory.
     *
     * @return {@code true} when the sub-group is hidden
     */
    public boolean hiddenGroup() {
        return hiddenGroup;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SubgroupSuggestionNew) obj;
        return Objects.equals(community, that.community) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(description, that.description) &&
                locked == that.locked &&
                announcement == that.announcement &&
                hiddenGroup == that.hiddenGroup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(community, subject, description, locked, announcement, hiddenGroup);
    }

    @Override
    public String toString() {
        return "SubgroupSuggestionNew[" +
                "community=" + community + ", " +
                "subject=" + subject + ", " +
                "description=" + description + ", " +
                "locked=" + locked + ", " +
                "announcement=" + announcement + ", " +
                "hiddenGroup=" + hiddenGroup + ']';
    }
}
