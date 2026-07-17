package com.github.auties00.cobalt.wire.linked.sync.action.chat;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Sync action that associates a phone-number JID with a LID-addressed chat.
 *
 * <p>WhatsApp identifiers can be either traditional phone-number JIDs (PN)
 * or privacy-preserving Linked Identity (LID) JIDs. When a user knows the
 * real phone number behind a LID-only contact, this action records the
 * PN-for-LID mapping so that every linked device resolves the same chat
 * consistently.
 */
@ProtobufMessage(name = "SyncActionValue.PnForLidChatAction")
public final class PnForLidChatAction implements SyncAction<PnForLidChatActionArgs> {
    /**
     * Canonical action name written as the first element of the sync index.
     */
    public static final String ACTION_NAME = "pnForLidChat";

    /**
     * Schema version declared by this action.
     */
    public static final int ACTION_VERSION = 8;

    /**
     * Collection this action is stored in when encoded into an app state patch.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for this action type.
     *
     * @return the string {@code "pnForLidChat"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version of this action type.
     *
     * @return the integer value {@code 8}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Phone-number JID linked to the LID chat referenced by the index
     * arguments.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    Jid pnJid;


    /**
     * Constructs a new {@code PnForLidChatAction} with the given phone-number
     * JID.
     *
     * @param pnJid the phone-number JID to associate with a LID chat
     */
    PnForLidChatAction(Jid pnJid) {
        this.pnJid = pnJid;
    }

    /**
     * Returns the phone-number JID associated with the LID chat.
     *
     * @return an {@link Optional} containing the phone-number {@link Jid},
     *         or an empty {@code Optional} when no mapping is defined
     */
    public Optional<Jid> pnJid() {
        return Optional.ofNullable(pnJid);
    }

    /**
     * Sets the phone-number JID associated with the LID chat.
     *
     * @param pnJid the new phone-number JID, or {@code null} to clear it
     */
    public void setPnJid(Jid pnJid) {
        this.pnJid = pnJid;
    }


}
