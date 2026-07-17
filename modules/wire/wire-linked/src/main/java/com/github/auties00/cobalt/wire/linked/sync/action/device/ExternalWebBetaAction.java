package com.github.auties00.cobalt.wire.linked.sync.action.device;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.SyncPatchType;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * App-state sync action that toggles the user's enrolment in the desktop
 * (External Web) beta program.
 *
 * <p>WhatsApp lets users opt into a parallel "beta" track that exposes
 * upcoming desktop features ahead of general release. Toggling the
 * preference from any linked device produces an
 * {@code ExternalWebBetaAction} mutation in the regular sync collection
 * so every companion device converges on the same enrolment flag. The
 * action is singleton: the sync index is composed solely of
 * {@link #ACTION_NAME} with no trailing arguments, meaning each new
 * mutation overwrites the previous opt-in value.
 */
@ProtobufMessage(name = "SyncActionValue.ExternalWebBetaAction")
public final class ExternalWebBetaAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * Canonical action name used as the sole component of the singleton
     * mutation index for this action type.
     */
    public static final String ACTION_NAME = "external_web_beta";

    /**
     * Schema version advertised by this action, used by sync handlers to
     * gate deserialisation and handling of newer payload shapes.
     */
    public static final int ACTION_VERSION = 3;

    /**
     * Collection this action belongs to, used by the sync protocol to
     * route the mutation into the correct replication stream.
     */
    public static final SyncPatchType COLLECTION_NAME = SyncPatchType.REGULAR;

    /**
     * Flag that records whether the user has opted into the desktop beta
     * program. Stored as a boxed {@link Boolean} so that an unset wire
     * value can be distinguished from an explicit {@code false}.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    Boolean isOptIn;

    /**
     * Constructs a new {@code ExternalWebBetaAction} from raw protobuf
     * field values.
     *
     * @param isOptIn whether the user opted into the beta program,
     *                possibly {@code null}
     */
    ExternalWebBetaAction(Boolean isOptIn) {
        this.isOptIn = isOptIn;
    }

    /**
     * Returns the canonical action name for every
     * {@code ExternalWebBetaAction}.
     *
     * @return the constant {@link #ACTION_NAME}
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the schema version for every
     * {@code ExternalWebBetaAction}.
     *
     * @return the constant {@link #ACTION_VERSION}
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }

    /**
     * Returns whether the user has opted into the desktop beta program.
     *
     * @return {@code true} if opted in, {@code false} otherwise (also
     *         when the field was unset on the wire)
     */
    public boolean isOptIn() {
        return isOptIn != null && isOptIn;
    }

    /**
     * Sets the opt-in flag for the desktop beta program.
     *
     * @param isOptIn {@code true} to opt in, {@code false} to opt out,
     *                or {@code null} to clear
     */
    public void setOptIn(Boolean isOptIn) {
        this.isOptIn = isOptIn;
    }
}
