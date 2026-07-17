package com.github.auties00.cobalt.wire.linked.sync.action.contact;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A sync action that records an outgoing contact card sent to a peer.
 *
 * <p>Outgoing contact actions capture the name under which a contact was
 * shared with someone else. They keep linked devices in agreement about what
 * display name was used when a contact card was dispatched, independently of
 * later edits to the user's address book.
 *
 * <p>Entries are indexed by the user JID string through
 * {@link OutContactActionArgs} and replicated via the
 * {@link SyncPatchType#REGULAR_LOW} collection.
 */
@ProtobufMessage(name = "SyncActionValue.OutContactAction")
public final class OutContactAction implements SyncAction<OutContactActionArgs> {
    /**
     * The canonical action name {@code "out_contact"} used to identify this
     * action inside a sync patch.
     */
    public static final String ACTION_NAME = "out_contact";

    /**
     * The canonical action version for this action type.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * The sync collection this action is carried in.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * The full display name under which the contact was shared.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String fullName;

    /**
     * The first name under which the contact was shared.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String firstName;

    /**
     * Constructs a new {@code OutContactAction}. Intended to be invoked by the
     * generated builder and by the protobuf deserializer.
     *
     * @param fullName  the full display name, or {@code null}
     * @param firstName the first name, or {@code null}
     */
    OutContactAction(String fullName, String firstName) {
        this.fullName = fullName;
        this.firstName = firstName;
    }

    /**
     * Returns the canonical action name {@code "out_contact"}.
     *
     * @return the string {@code "out_contact"}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version declared by this action type.
     *
     * @return the action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the full display name under which the contact was shared.
     *
     * @return the full name, or an empty {@link Optional} if none was provided
     */
    public Optional<String> fullName() {
        return Optional.ofNullable(fullName);
    }

    /**
     * Returns the first name under which the contact was shared.
     *
     * @return the first name, or an empty {@link Optional} if none was
     *         provided
     */
    public Optional<String> firstName() {
        return Optional.ofNullable(firstName);
    }
}
