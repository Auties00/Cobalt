package com.github.auties00.cobalt.wire.linked.chat.community;

import com.github.auties00.cobalt.wire.linked.chat.ChatEphemeralTimer;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;
import java.util.Optional;

/**
 * Input model for {@code createCommunity}. Carries the display name of the
 * community being created together with an optional description and an
 * optional default disappearing-messages timer applied to its chats.
 *
 * <p>The display name is required; the description and the ephemeral timer
 * are optional. An unset timer is treated as {@link ChatEphemeralTimer#OFF},
 * meaning new community chats do not auto-delete their messages.
 */
@ProtobufMessage
public final class CommunityCreate {
    /**
     * Display name shown for the community.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String name;

    /**
     * Optional description shown on the community's information page, or
     * {@code null} when no description is provided.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String description;

    /**
     * Optional default disappearing-messages timer applied to the
     * community's chats, or {@code null} when no timer is configured.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.ENUM)
    final ChatEphemeralTimer ephemeralTimer;

    /**
     * Constructs a new {@code CommunityCreate}.
     *
     * @param name           the community display name; required
     * @param description    the community description, or {@code null}
     * @param ephemeralTimer the default disappearing-messages timer, or {@code null}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    CommunityCreate(String name, String description, ChatEphemeralTimer ephemeralTimer) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description;
        this.ephemeralTimer = ephemeralTimer;
    }

    /**
     * Returns the community display name.
     *
     * @return the name, never {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns the community description, if one was provided.
     *
     * @return an {@link Optional} containing the description, or an empty
     *         {@link Optional} when no description is set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the default disappearing-messages timer applied to the
     * community's chats.
     *
     * <p>When no timer is configured, this returns {@link ChatEphemeralTimer#OFF},
     * so an unset timer is reported as disabled rather than as {@code null}.
     *
     * @return the ephemeral timer, or {@link ChatEphemeralTimer#OFF} when none
     *         was configured, never {@code null}
     */
    public ChatEphemeralTimer ephemeralTimer() {
        return ephemeralTimer == null ? ChatEphemeralTimer.OFF : ephemeralTimer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CommunityCreate) obj;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(ephemeralTimer, that.ephemeralTimer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, ephemeralTimer);
    }

    @Override
    public String toString() {
        return "CommunityCreate[" +
                "name=" + name + ", " +
                "description=" + description + ", " +
                "ephemeralTimer=" + ephemeralTimer + ']';
    }
}
