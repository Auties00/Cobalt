package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A business user assigned to a WhatsApp Business Account.
 *
 * <p>Each entry pairs a business user with the set of tasks the user may perform on the account, as
 * returned by the account's assigned-users edge. Each task is a {@link CloudBusinessAccountUserTask}
 * constant, with unrecognised tokens resolved to {@link CloudBusinessAccountUserTask#UNKNOWN}.
 */
public final class CloudWabaAssignedUser {
    /**
     * The business user id.
     */
    private final String id;

    /**
     * The business user name, or {@code null} when not projected.
     */
    private final String name;

    /**
     * The tasks the user may perform on the account.
     */
    private final List<CloudBusinessAccountUserTask> tasks;

    /**
     * Constructs a new assigned user.
     *
     * @param id    the business user id
     * @param name  the business user name
     * @param tasks the tasks the user may perform, or {@code null} for none
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudWabaAssignedUser(String id, String name, List<CloudBusinessAccountUserTask> tasks) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.tasks = tasks == null ? List.of() : List.copyOf(tasks);
    }

    /**
     * Returns the business user id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the business user name.
     *
     * @return an {@link Optional} carrying the name, or empty when not projected
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the tasks the user may perform on the account.
     *
     * @return an unmodifiable list of tasks, empty when none were projected
     */
    public List<CloudBusinessAccountUserTask> tasks() {
        return tasks;
    }
}
