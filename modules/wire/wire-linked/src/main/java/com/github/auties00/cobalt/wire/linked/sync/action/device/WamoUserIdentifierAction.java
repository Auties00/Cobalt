package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Represents a sync action that replicates the generated WhatsApp Marketing
 * Outreach (WAMO) user identifier across every linked device.
 *
 * <p>The WAMO identifier is a stable opaque string used to attribute marketing
 * and outreach events to the same logical user without exposing the phone
 * number based JID. Replicating it through app state sync ensures every
 * companion device reports the same identifier to the server. The mutation is
 * singleton, so the sync index is composed solely of {@link #ACTION_NAME} with
 * no trailing arguments.
 */
@ProtobufMessage(name = "SyncActionValue.WamoUserIdentifierAction")
public final class WamoUserIdentifierAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton mutation
     * index for this action type.
     */
    public static final String ACTION_NAME = "generated_wui";

    /**
     * Schema version advertised by this action, used by sync handlers to gate
     * deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the canonical action name for every {@code WamoUserIdentifierAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every {@code WamoUserIdentifierAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * Opaque WAMO user identifier generated for this account.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String identifier;


    /**
     * Constructs a new {@code WamoUserIdentifierAction} from raw protobuf field
     * values.
     *
     * @param identifier the generated WAMO user identifier, possibly
     *                   {@code null}
     */
    WamoUserIdentifierAction(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Returns the generated WAMO user identifier, if one was encoded.
     *
     * @return an {@link Optional} containing the identifier, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<String> identifier() {
        return Optional.ofNullable(identifier);
    }

    /**
     * Sets the generated WAMO user identifier.
     *
     * @param identifier the new identifier, or {@code null} to clear
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
