package com.github.auties00.cobalt.wire.linked.chat.group;

import com.github.auties00.cobalt.wire.core.jid.Jid;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a mention of a WhatsApp group within a message.
 *
 * <p>Group mentions allow users to reference a group chat inline within a
 * message body, similar to how {@code @mentions} work for individual contacts.
 * When a group is mentioned, the message contains this structure identifying
 * which group was referenced and its display name (subject) at the time of
 * mention.
 *
 * <p>This is distinct from mentioning individual participants within a group
 * chat. A group mention creates a tappable link to the referenced group's
 * info page in the WhatsApp client.
 *
 * @see Jid
 */
@ProtobufMessage(name = "GroupMention")
public final class GroupMention {
    /**
     * The JID of the mentioned group, or {@code null} if not available.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid groupJid;

    /**
     * The subject (display name) of the mentioned group at the time the
     * mention was created, or {@code null} if not available.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String groupSubject;


    /**
     * Constructs a new {@code GroupMention} with the specified group JID and
     * subject.
     *
     * @param groupJid     the JID of the mentioned group, or {@code null}
     * @param groupSubject the subject of the mentioned group, or {@code null}
     */
    GroupMention(Jid groupJid, String groupSubject) {
        this.groupJid = groupJid;
        this.groupSubject = groupSubject;
    }

    /**
     * Returns the JID of the mentioned group, if available.
     *
     * @return an {@code Optional} containing the group JID, or empty if not
     *         available
     */
    public Optional<Jid> groupJid() {
        return Optional.ofNullable(groupJid);
    }

    /**
     * Returns the subject (display name) of the mentioned group at the time
     * the mention was created, if available.
     *
     * @return an {@code Optional} containing the group subject, or empty if
     *         not available
     */
    public Optional<String> groupSubject() {
        return Optional.ofNullable(groupSubject);
    }

    /**
     * Sets the JID of the mentioned group.
     *
     * @param groupJid the group JID to set, or {@code null} to clear
     */
    public void setGroupJid(Jid groupJid) {
        this.groupJid = groupJid;
    }

    /**
     * Sets the subject (display name) of the mentioned group.
     *
     * @param groupSubject the group subject to set, or {@code null} to clear
     */
    public void setGroupSubject(String groupSubject) {
        this.groupSubject = groupSubject;
    }
}
