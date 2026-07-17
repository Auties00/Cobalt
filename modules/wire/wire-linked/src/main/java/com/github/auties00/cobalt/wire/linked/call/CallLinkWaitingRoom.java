package com.github.auties00.cobalt.wire.linked.call;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * Describes the waiting-room state attached to a {@link CallLink}.
 *
 * <p>A waiting room is an admin-controlled gate that holds joining
 * participants in a virtual lobby until the link's owner approves them.
 * The state has two independent dimensions: whether the inspecting user
 * is the lobby's {@linkplain #admin() admin}, and whether the lobby gate
 * is currently {@linkplain #enabled() enabled}.
 *
 * <p>The descriptor is immutable: toggling the gate via the relay yields a
 * new {@code CallLink} with a refreshed waiting-room state. The original
 * value is unaffected.
 */
@ProtobufMessage
public final class CallLinkWaitingRoom {
    /**
     * Whether the inspecting user is the waiting-room admin. The admin
     * is the only party authorised to admit pending participants and to
     * toggle the {@linkplain #enabled enabled} flag.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean admin;

    /**
     * Whether the waiting-room gate is currently enabled. When
     * {@code true} new joins are held in the lobby until the
     * {@linkplain #admin admin} admits them; when {@code false} joins
     * fall through directly into the call.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean enabled;

    /**
     * Constructs a new descriptor with the supplied flags.
     *
     * @param admin   whether the inspecting user is the admin
     * @param enabled whether the gate is enabled
     */
    CallLinkWaitingRoom(boolean admin, boolean enabled) {
        this.admin = admin;
        this.enabled = enabled;
    }

    /**
     * Returns whether the inspecting user is the waiting-room admin.
     *
     * @return {@code true} when the inspecting user is an admin;
     *         {@code false} otherwise
     */
    public boolean admin() {
        return admin;
    }

    /**
     * Returns whether the waiting-room gate is enabled.
     *
     * @return {@code true} when the gate is currently enabled;
     *         {@code false} when joins fall through directly into the
     *         call
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Compares this descriptor with the given object for equality.
     *
     * <p>Two descriptors are equal when both flags match.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} when {@code obj} is a
     *         {@code CallLinkWaitingRoom} with the same flags;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CallLinkWaitingRoom that)) {
            return false;
        }
        return admin == that.admin && enabled == that.enabled;
    }

    /**
     * Returns a hash code consistent with {@link #equals(Object)}.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(admin, enabled);
    }

    /**
     * Returns a debug-friendly representation of this descriptor.
     *
     * @return a string with both flag values
     */
    @Override
    public String toString() {
        return "CallLinkWaitingRoom[admin=" + admin + ", enabled=" + enabled + ']';
    }
}
