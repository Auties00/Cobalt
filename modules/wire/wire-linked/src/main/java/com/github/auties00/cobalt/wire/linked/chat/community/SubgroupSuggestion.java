package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Input model shared by {@code approveSubgroupSuggestion} and
 * {@code rejectSubgroupSuggestion}. Carries the per-candidate verdict subject
 * for both the approve and the reject operations: the parent community, the
 * suggested sub-group, and the user who created the suggestion.
 *
 * <p>All three JIDs are required. The same model identifies the candidate
 * whether the verdict is to approve or to reject the suggestion.
 */
@ProtobufMessage
public final class SubgroupSuggestion {
    /**
     * JID of the parent community the suggestion belongs to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final Jid community;

    /**
     * JID of the sub-group that was suggested.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final Jid suggestedSubgroup;

    /**
     * JID of the user who created the suggestion.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final Jid suggestionCreator;

    /**
     * Constructs a new {@code SubgroupSuggestion}.
     *
     * @param community         the parent community JID; required
     * @param suggestedSubgroup the suggested sub-group JID; required
     * @param suggestionCreator the JID of the user who created the suggestion; required
     * @throws NullPointerException if any argument is {@code null}
     */
    SubgroupSuggestion(Jid community, Jid suggestedSubgroup, Jid suggestionCreator) {
        this.community = Objects.requireNonNull(community, "community cannot be null");
        this.suggestedSubgroup = Objects.requireNonNull(suggestedSubgroup, "suggestedSubgroup cannot be null");
        this.suggestionCreator = Objects.requireNonNull(suggestionCreator, "suggestionCreator cannot be null");
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
     * Returns the suggested sub-group JID.
     *
     * @return the suggested sub-group JID, never {@code null}
     */
    public Jid suggestedSubgroup() {
        return suggestedSubgroup;
    }

    /**
     * Returns the JID of the user who created the suggestion.
     *
     * @return the suggestion creator JID, never {@code null}
     */
    public Jid suggestionCreator() {
        return suggestionCreator;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SubgroupSuggestion) obj;
        return Objects.equals(community, that.community) &&
                Objects.equals(suggestedSubgroup, that.suggestedSubgroup) &&
                Objects.equals(suggestionCreator, that.suggestionCreator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(community, suggestedSubgroup, suggestionCreator);
    }

    @Override
    public String toString() {
        return "SubgroupSuggestion[" +
                "community=" + community + ", " +
                "suggestedSubgroup=" + suggestedSubgroup + ", " +
                "suggestionCreator=" + suggestionCreator + ']';
    }
}
