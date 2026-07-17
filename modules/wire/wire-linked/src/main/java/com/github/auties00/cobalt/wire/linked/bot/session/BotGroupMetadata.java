package com.github.auties00.cobalt.wire.linked.bot.session;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Metadata about bot participants in a group chat context on WhatsApp.
 *
 * <p>When a bot interaction occurs within a group chat, this metadata
 * identifies which bot participants are involved. It is populated when the
 * group bot participant feature is enabled, and contains a list of
 * {@link BotGroupParticipantMetadata} entries, one for each bot participant
 * in the group. The first entry in the list is typically used to resolve
 * the bot's JID for routing purposes.
 *
 * <p>This metadata is carried inside
 * {@link com.github.auties00.cobalt.wire.linked.bot.BotMetadata#botGroupMetadata()}.
 */
@ProtobufMessage(name = "BotGroupMetadata")
public final class BotGroupMetadata {
    /**
     * The list of bot participants in the group, each identified by their
     * Facebook ID.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    List<BotGroupParticipantMetadata> participantsMetadata;


    /**
     * Constructs a new {@code BotGroupMetadata} with the specified
     * participants.
     *
     * @param participantsMetadata the list of bot participant metadata, or
     *        {@code null}
     */
    BotGroupMetadata(List<BotGroupParticipantMetadata> participantsMetadata) {
        this.participantsMetadata = participantsMetadata;
    }

    /**
     * Returns the list of bot participants in the group.
     *
     * @return an unmodifiable list of {@link BotGroupParticipantMetadata}
     *         entries, or an empty list if none were set
     */
    public List<BotGroupParticipantMetadata> participantsMetadata() {
        return participantsMetadata == null ? List.of() : Collections.unmodifiableList(participantsMetadata);
    }

    /**
     * Sets the list of bot participants in the group.
     *
     * @param participantsMetadata the new list of bot participant metadata,
     *        or {@code null}
     */
    public void setParticipantsMetadata(List<BotGroupParticipantMetadata> participantsMetadata) {
        this.participantsMetadata = participantsMetadata;
    }
}
