package com.github.auties00.cobalt.wire.linked.sync.action.privacy;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Records the user's preference for the "private processing" feature, which
 * controls whether sensitive on-device computation may run for the linked
 * account.
 *
 * <p>Private processing refers to a set of WhatsApp features that perform
 * computations locally on the device (for example, message classification or
 * on-device AI assistance) rather than delegating them to WhatsApp servers.
 * Because these computations may involve sensitive data, the user can
 * explicitly opt in or out of the feature, and that choice must be mirrored
 * on every linked device.
 *
 * <p>The action carries a single enum value, {@link PrivateProcessingStatus},
 * capturing whether private processing is undefined, enabled, or disabled.
 */
@ProtobufMessage(name = "SyncActionValue.PrivateProcessingSettingAction")
public final class PrivateProcessingSettingAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The canonical action name used to identify this sync action on the
     * wire.
     */
    public static final String ACTION_NAME = "private_processing_setting";

    /**
     * The canonical protocol version of this sync action.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Sync collection this action belongs to, used by the sync protocol to
     * route the mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_HIGH;

    /**
     * Returns the canonical action name used to identify this sync action on
     * the wire.
     *
     * @return the action name {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the canonical protocol version of this sync action.
     *
     * @return the action version {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The current private processing status persisted by this action.
     *
     * <p>A {@code null} value means the field was absent on the wire and the
     * user has not yet expressed a preference on this device.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.ENUM)
    PrivateProcessingStatus privateProcessingStatus;


    /**
     * Constructs a new action carrying the supplied private processing
     * status.
     *
     * @param privateProcessingStatus the private processing status to
     *                                persist, or {@code null} if the field
     *                                is absent
     */
    PrivateProcessingSettingAction(PrivateProcessingStatus privateProcessingStatus) {
        this.privateProcessingStatus = privateProcessingStatus;
    }

    /**
     * Returns the current private processing status carried by this action.
     *
     * @return an {@link Optional} containing the status, or
     *         {@link Optional#empty()} if the field is unset
     */
    public Optional<PrivateProcessingStatus> privateProcessingStatus() {
        return Optional.ofNullable(privateProcessingStatus);
    }

    /**
     * Sets the private processing status carried by this action.
     *
     * @param privateProcessingStatus the new private processing status, or
     *                                {@code null} to clear the field
     */
    public void setPrivateProcessingStatus(PrivateProcessingStatus privateProcessingStatus) {
        this.privateProcessingStatus = privateProcessingStatus;
    }

    /**
     * Enumerates the possible states of the user's private processing
     * preference.
     *
     * <p>The enum distinguishes the case where the user has not yet made a
     * choice ({@link #UNDEFINED}) from an explicit opt-in
     * ({@link #ENABLED}) or opt-out ({@link #DISABLED}).
     */
    @ProtobufEnum(name = "SyncActionValue.PrivateProcessingSettingAction.PrivateProcessingStatus")
    public enum PrivateProcessingStatus {
        /**
         * The user has not yet expressed a preference for the private
         * processing feature.
         */
        UNDEFINED(0),
        /**
         * The user has enabled the private processing feature.
         */
        ENABLED(1),
        /**
         * The user has disabled the private processing feature.
         */
        DISABLED(2);

        /**
         * Constructs a new enum constant with the given protobuf wire index.
         *
         * @param index the protobuf wire index for this enum constant
         */
        PrivateProcessingStatus(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf wire index for this enum constant.
         */
        final int index;

        /**
         * Returns the protobuf wire index for this enum constant.
         *
         * @return the protobuf wire index
         */
        public int index() {
            return this.index;
        }
    }
}
