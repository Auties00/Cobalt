package com.github.auties00.cobalt.wire.linked.sync.action.contact;


import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionArgs;

/**
 * Index arguments that locate a specific {@link LabelAssociationAction} inside
 * a sync patch.
 *
 * <p>A label association is uniquely addressed by the pair formed by the
 * label identifier and the JID of the chat or contact it is applied to. When
 * building or reading a patch the sync engine translates these arguments into
 * the index tuple {@code ["label_jid", labelId, chatJid]}.
 *
 * @param labelId the identifier of the label
 * @param chatJid the JID string of the chat or contact that is being
 *                associated with the label
 */
public record LabelAssociationActionArgs(String labelId, String chatJid) implements SyncActionArgs {
    /**
     * Returns the index components used by the sync engine to address this
     * label association entry.
     *
     * @return a two-element array containing the label identifier and the
     *         chat JID string
     */
    @Override
    public String[] toIndexArgs() {
        return new String[]{labelId, chatJid};
    }
}
