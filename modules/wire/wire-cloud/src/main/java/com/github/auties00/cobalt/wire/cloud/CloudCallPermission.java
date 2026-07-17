package com.github.auties00.cobalt.wire.cloud;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A WhatsApp Cloud API user call-permission state.
 *
 * <p>Before a business may place a call to a consumer, the consumer must grant calling permission. This
 * model projects the permission state the Calling API reports for a given consumer: the permission
 * status, the optional expiration of a temporary grant, and the structured set of actions the business
 * may currently perform.
 *
 * <p>The status and its expiration are read from the response's {@code permission} object; observed
 * statuses are {@code temporary} and {@code no_permission}. Each {@link Action} carries the action name,
 * whether it can currently be performed, and the rate {@link Limit}s that govern it.
 */
public final class CloudCallPermission {
    /**
     * The permission status string, for example {@code "temporary"} or {@code "no_permission"}.
     */
    private final String status;

    /**
     * The expiration of a temporary permission grant, or {@code null} when the grant does not expire or
     * none exists.
     */
    private final Instant expirationTime;

    /**
     * The actions the business may currently perform for the consumer.
     */
    private final List<Action> actions;

    /**
     * Constructs a new call-permission state.
     *
     * @param status         the permission status string
     * @param expirationTime the expiration of a temporary grant, or {@code null} when none
     * @param actions        the structured actions, or {@code null} for none
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public CloudCallPermission(String status, Instant expirationTime, List<Action> actions) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.expirationTime = expirationTime;
        this.actions = actions == null ? List.of() : List.copyOf(actions);
    }

    /**
     * Returns the permission status.
     *
     * @return the permission status string
     */
    public String status() {
        return status;
    }

    /**
     * Returns the expiration of a temporary permission grant.
     *
     * @return an {@link Optional} carrying the expiration, or empty when the grant does not expire or
     *         none exists
     */
    public Optional<Instant> expirationTime() {
        return Optional.ofNullable(expirationTime);
    }

    /**
     * Returns the actions the business may currently perform for the consumer.
     *
     * @return an unmodifiable list of actions, empty when none were reported
     */
    public List<Action> actions() {
        return actions;
    }

    /**
     * A single action the business may perform for the consumer, with its rate limits.
     */
    public static final class Action {
        /**
         * The action name, for example {@code "send_call_permission_request"} or {@code "start_call"}.
         */
        private final String actionName;

        /**
         * Whether the action can currently be performed.
         */
        private final boolean canPerformAction;

        /**
         * The rate limits that govern the action.
         */
        private final List<Limit> limits;

        /**
         * Constructs a new action.
         *
         * @param actionName       the action name
         * @param canPerformAction whether the action can currently be performed
         * @param limits           the rate limits, or {@code null} for none
         * @throws NullPointerException if {@code actionName} is {@code null}
         */
        public Action(String actionName, boolean canPerformAction, List<Limit> limits) {
            this.actionName = Objects.requireNonNull(actionName, "actionName must not be null");
            this.canPerformAction = canPerformAction;
            this.limits = limits == null ? List.of() : List.copyOf(limits);
        }

        /**
         * Returns the action name.
         *
         * @return the action name
         */
        public String actionName() {
            return actionName;
        }

        /**
         * Returns whether the action can currently be performed.
         *
         * @return {@code true} if the action can currently be performed
         */
        public boolean canPerformAction() {
            return canPerformAction;
        }

        /**
         * Returns the rate limits that govern the action.
         *
         * @return an unmodifiable list of limits, empty when none were reported
         */
        public List<Limit> limits() {
            return limits;
        }
    }

    /**
     * A single rate limit governing an {@link Action} over a rolling time period.
     */
    public static final class Limit {
        /**
         * The rolling time period as an ISO-8601 duration, for example {@code "PT24H"} or {@code "P7D"}.
         */
        private final String timePeriod;

        /**
         * The maximum number of times the action may be performed in the period.
         */
        private final int maxAllowed;

        /**
         * The number of times the action has already been performed in the current period.
         */
        private final int currentUsage;

        /**
         * The instant at which the current period's usage resets, or {@code null} when none was returned.
         */
        private final Instant limitExpirationTime;

        /**
         * Constructs a new rate limit.
         *
         * @param timePeriod          the rolling time period as an ISO-8601 duration
         * @param maxAllowed          the maximum number of times the action may be performed
         * @param currentUsage        the number of times the action has already been performed
         * @param limitExpirationTime the instant the current period resets, or {@code null} when none
         * @throws NullPointerException if {@code timePeriod} is {@code null}
         */
        public Limit(String timePeriod, int maxAllowed, int currentUsage, Instant limitExpirationTime) {
            this.timePeriod = Objects.requireNonNull(timePeriod, "timePeriod must not be null");
            this.maxAllowed = maxAllowed;
            this.currentUsage = currentUsage;
            this.limitExpirationTime = limitExpirationTime;
        }

        /**
         * Returns the rolling time period as an ISO-8601 duration.
         *
         * @return the time period
         */
        public String timePeriod() {
            return timePeriod;
        }

        /**
         * Returns the maximum number of times the action may be performed in the period.
         *
         * @return the maximum allowed count
         */
        public int maxAllowed() {
            return maxAllowed;
        }

        /**
         * Returns the number of times the action has already been performed in the current period.
         *
         * @return the current usage count
         */
        public int currentUsage() {
            return currentUsage;
        }

        /**
         * Returns the instant at which the current period's usage resets.
         *
         * @return an {@link Optional} carrying the reset instant, or empty when none was returned
         */
        public Optional<Instant> limitExpirationTime() {
            return Optional.ofNullable(limitExpirationTime);
        }
    }
}
