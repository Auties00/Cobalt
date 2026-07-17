package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufEnum;
import it.auties.protobuf.annotation.ProtobufEnumIndex;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * App-state sync action that records the federated link state between a
 * WhatsApp account and a Meta Accounts Center identity (internally
 * referred to as the "Waffle" link).
 *
 * <p>Linking a WhatsApp account to a Meta Accounts Center identity is
 * what unlocks cross-product features such as a unified login, shared
 * notification settings, and cross-app posting. The relay broadcasts the
 * current link state to every companion device through this singleton
 * action so each surface renders a consistent view of the federation
 * status. The action is singleton: the sync index is composed solely of
 * {@link #ACTION_NAME} with no trailing arguments, so each new mutation
 * overwrites the previous link state.
 */
@ProtobufMessage(name = "SyncActionValue.WaffleAccountLinkStateAction")
public final class WaffleAccountLinkStateAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton
     * mutation index for this action type.
     */
    public static final String ACTION_NAME = "waffle_account_link_state";

    /**
     * Schema version advertised by this action, used by sync handlers to
     * gate deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Collection this action belongs to, used by the sync protocol to
     * route the mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Current {@link AccountLinkState} between the WhatsApp account and
     * the Meta Accounts Center identity. May be {@code null} when the
     * relay omits the field (treated as unknown).
     */
    @ProtobufProperty(index = 2, type = ProtobufType.ENUM)
    AccountLinkState linkState;

    /**
     * Constructs a new {@code WaffleAccountLinkStateAction} from raw
     * protobuf field values.
     *
     * @param linkState the current link state, possibly {@code null}
     */
    WaffleAccountLinkStateAction(AccountLinkState linkState) {
        this.linkState = linkState;
    }

    /**
     * Returns the canonical action name for every
     * {@code WaffleAccountLinkStateAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every
     * {@code WaffleAccountLinkStateAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns the current link state between the WhatsApp account and
     * the Meta Accounts Center identity.
     *
     * @return an {@link Optional} containing the
     *         {@link AccountLinkState}, or {@link Optional#empty()} if
     *         absent
     */
    public Optional<AccountLinkState> linkState() {
        return Optional.ofNullable(linkState);
    }

    /**
     * Sets the current link state.
     *
     * @param linkState the new {@link AccountLinkState}, or {@code null}
     *                  to clear
     */
    public void setLinkState(AccountLinkState linkState) {
        this.linkState = linkState;
    }

    /**
     * Enumerates the four states the link between a WhatsApp account
     * and a Meta Accounts Center identity may be in.
     */
    @ProtobufEnum(name = "SyncActionValue.WaffleAccountLinkStateAction.AccountLinkState")
    public enum AccountLinkState {
        /**
         * The link is established and functional, so cross-product
         * features are available.
         */
        ACTIVE(0),

        /**
         * The link exists but has been temporarily suspended, typically
         * by the user from the Meta Accounts Center UI.
         */
        PAUSED(1),

        /**
         * The link has been dissolved and the WhatsApp account is no
         * longer associated with any Meta Accounts Center identity.
         */
        UNLINKED(2),

        /**
         * The link state is unreported or unrecognised by this client
         * version.
         */
        UNKNOWN(3);

        /**
         * Protobuf wire index assigned to this enum constant.
         */
        final int index;

        /**
         * Constructs a new {@code AccountLinkState} with the given
         * protobuf wire index.
         *
         * @param index the protobuf wire index
         */
        AccountLinkState(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * Returns the protobuf wire index for this enum constant.
         *
         * @return the wire index
         */
        public int index() {
            return this.index;
        }
    }
}
