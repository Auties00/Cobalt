package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;

/**
 * Represents a sync action that replicates the set of feature flags advertised
 * by the primary device to its linked companions.
 *
 * <p>The primary device is authoritative for feature capabilities: as new
 * features are rolled out or gated, it publishes the current flag set through
 * this singleton action so every companion exposes the same set of features in
 * its UI. The mutation is singleton, so the sync index is composed solely of
 * {@link #ACTION_NAME} with no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.PrimaryFeature")
public final class PrimaryFeatureAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "primary_feature";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Returns the canonical action name for every {@code PrimaryFeatureAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code PrimaryFeatureAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * List of feature flag names currently advertised by the primary device.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    List<String> flags;


    /**
     * Constructs a new {@code PrimaryFeatureAction} from raw protobuf field
     * values.
     *
     * @param flags the list of advertised feature flag names, possibly
     *              {@code null}
     */
    PrimaryFeatureAction(List<String> flags) {
        this.flags = flags;
    }

    /**
     * Returns the unmodifiable list of feature flag names currently advertised
     * by the primary device.
     *
     * <p>An empty list is returned when no flags were present on the wire; the
     * result is never {@code null}.
     *
     * @return the unmodifiable feature flag list
     */
    public List<String> flags() {
        return flags == null ? List.of() : Collections.unmodifiableList(flags);
    }

    /**
     * Sets the list of feature flag names advertised by the primary device.
     *
     * @param flags the new feature flag list, or {@code null} to clear
     */
    public void setFlags(List<String> flags) {
        this.flags = flags;
    }
}
