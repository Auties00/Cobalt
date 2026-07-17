package com.github.auties00.cobalt.wire.linked.setting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * Single Terms-of-Service notice the user is being asked to acknowledge.
 *
 * <p>WhatsApp periodically pushes legal updates (regional addenda, policy
 * changes, age-gating disclosures) that the user must accept before they can
 * keep using parts of the app. The server tracks each pending update by an
 * opaque identifier and a per-user accepted/not-accepted flag; one
 * {@code TosNotice} carries that pair for one update.
 */
@ProtobufMessage
public final class TosNotice {
    /**
     * The server-assigned identifier for this notice. Opaque to the
     * client; used to correlate the user's acknowledgement back to the
     * server-side notice record.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String id;

    /**
     * Whether the user has acknowledged this notice. {@code true} when the
     * user has accepted (or otherwise marked "true") the notice;
     * {@code false} when the {@code state} attribute on the wire is
     * {@code "false"}, signalling the notice is still pending acceptance.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    boolean accepted;

    /**
     * Constructs a new {@code TosNotice} with the supplied identifier and
     * acceptance flag.
     *
     * @param id       the server-assigned notice identifier, or {@code null}
     *                 if not set
     * @param accepted the acceptance flag
     */
    TosNotice(String id, boolean accepted) {
        this.id = id;
        this.accepted = accepted;
    }

    /**
     * Returns the server-assigned notice identifier.
     *
     * @return an {@code Optional} containing the identifier, or empty if
     *         not set
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns whether this notice has been acknowledged by the user.
     *
     * @return {@code true} when the user has accepted (or otherwise marked
     *         "true") the notice, {@code false} otherwise
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Sets the server-assigned notice identifier.
     *
     * @param id the identifier to set, or {@code null} to clear
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Sets whether the user has acknowledged this notice.
     *
     * @param accepted {@code true} when the notice has been accepted,
     *                 {@code false} otherwise
     */
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
