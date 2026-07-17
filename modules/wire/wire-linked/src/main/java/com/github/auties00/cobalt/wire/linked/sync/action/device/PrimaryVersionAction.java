package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a sync action that replicates the primary device's client version
 * string to every linked companion.
 *
 * <p>Companion clients use the primary's advertised version to gate
 * compatibility decisions, for example refusing to use new protocol features
 * when the primary is too old to support them. The mutation is singleton, so
 * the sync index is composed solely of {@link #ACTION_NAME} with no trailing
 * arguments.
 */
@ProtobufMessage(name = "SyncActionValue.PrimaryVersionAction")
public final class PrimaryVersionAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "primary_version";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 7;

    /**
     * Collection this action belongs to, used by the sync protocol to route the
     * mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR_LOW;

    /**
     * Returns the canonical action name for every {@code PrimaryVersionAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code PrimaryVersionAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Free form version string published by the primary device.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String version;


    /**
     * Constructs a new {@code PrimaryVersionAction} from raw protobuf field
     * values.
     *
     * @param version the primary device version string, possibly {@code null}
     */
    PrimaryVersionAction(String version) {
        this.version = version;
    }

    /**
     * Returns the version string advertised by the primary device, if one was
     * encoded.
     *
     * @return an {@link Optional} containing the version, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    /**
     * Sets the version string advertised by the primary device.
     *
     * @param version the new version string, or {@code null} to clear
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
