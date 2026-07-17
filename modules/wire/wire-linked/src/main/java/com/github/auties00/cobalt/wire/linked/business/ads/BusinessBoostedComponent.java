package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A live WhatsApp Business advertisement, as published from a draft.
 *
 * <p>When a merchant finishes assembling a "Click-to-WhatsApp" ad draft (a paid
 * promotion that opens a chat with the business when tapped) and confirms it,
 * the server turns the draft into a running ad called a boosted component. This
 * model is that running ad as the server reports it right after publishing: the
 * surface it promotes, the audience it targets, the money and time it is
 * scheduled to spend, and the creative text shown to people who see it.
 *
 * <p>{@link #id()} is the handle used to later pause, resume, or delete the ad.
 * {@link #placement()} carries what the ad promotes and where; {@link #budget()},
 * {@link #budgetKind()}, and {@link #durationInDays()} carry the spend plan;
 * {@link #objective()} is the human-readable goal; {@link #audience()} is the
 * targeted audience; {@link #creativeBodies()} lists the ad's body text; and
 * {@link #targetingEditable()} reports whether the targeting may still be
 * changed.
 */
@ProtobufMessage(name = "BusinessBoostedComponent")
public final class BusinessBoostedComponent {
    /**
     * Server-issued identifier of the live ad. This is the handle used to
     * pause, resume, or delete the ad; it is a numeric advertising identifier,
     * not a WhatsApp address. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Server-defined delivery status of the ad (for example active or paused).
     * The full marker set is not recoverable from the WhatsApp client, so the
     * raw marker is exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String status;

    /**
     * Placement of the ad: what it promotes and where, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final Placement placement;

    /**
     * Budget amount in the billing currency's minor units, expressed as a
     * string to preserve the server's exact precision. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    final String budget;

    /**
     * Server-defined kind of budget (for example a daily or a lifetime budget).
     * The full marker set is not recoverable from the WhatsApp client, so the
     * raw marker is exposed as a string. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.STRING)
    final String budgetKind;

    /**
     * Number of days the ad is scheduled to run, or {@code null} when the
     * server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.INT64)
    final Long durationInDays;

    /**
     * Human-readable goal of the ad (for example getting more messages). Empty
     * when the server omitted it.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    final String objective;

    /**
     * Audience the ad targets, or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    final Audience audience;

    /**
     * Body text shown in each of the ad's creatives, in the order the server
     * returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final List<String> creativeBodies;

    /**
     * Whether the ad's targeting may still be changed by the merchant. Reported
     * by the server; {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 10, type = ProtobufType.BOOL)
    final boolean targetingEditable;

    /**
     * Constructs a new {@code BusinessBoostedComponent}. A {@code null}
     * {@code creativeBodies} is coerced to an empty list, and the other
     * reference arguments may be {@code null} when the server omitted them.
     *
     * @param id                the ad identifier, or {@code null}
     * @param status            the delivery status marker, or {@code null}
     * @param placement         the placement, or {@code null}
     * @param budget            the budget amount, or {@code null}
     * @param budgetKind        the budget kind marker, or {@code null}
     * @param durationInDays    the run duration in days, or {@code null}
     * @param objective         the human-readable goal, or {@code null}
     * @param audience          the targeted audience, or {@code null}
     * @param creativeBodies    the creative body texts; {@code null} treated as empty
     * @param targetingEditable whether the targeting may still be changed
     */
    BusinessBoostedComponent(String id,
                             String status,
                             Placement placement,
                             String budget,
                             String budgetKind,
                             Long durationInDays,
                             String objective,
                             Audience audience,
                             List<String> creativeBodies,
                             boolean targetingEditable) {
        this.id = id;
        this.status = status;
        this.placement = placement;
        this.budget = budget;
        this.budgetKind = budgetKind;
        this.durationInDays = durationInDays;
        this.objective = objective;
        this.audience = audience;
        this.creativeBodies = creativeBodies == null ? List.of() : creativeBodies;
        this.targetingEditable = targetingEditable;
    }

    /**
     * Returns the server-issued identifier of the live ad.
     *
     * @return the ad id, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Returns the server-defined delivery status of the ad.
     *
     * @return the status marker, or empty when the server omitted it
     */
    public Optional<String> status() {
        return Optional.ofNullable(status);
    }

    /**
     * Returns the placement of the ad.
     *
     * @return the placement, or empty when the server omitted it
     */
    public Optional<Placement> placement() {
        return Optional.ofNullable(placement);
    }

    /**
     * Returns the budget amount in the billing currency's minor units.
     *
     * @return the budget amount, or empty when the server omitted it
     */
    public Optional<String> budget() {
        return Optional.ofNullable(budget);
    }

    /**
     * Returns the server-defined kind of budget.
     *
     * @return the budget kind marker, or empty when the server omitted it
     */
    public Optional<String> budgetKind() {
        return Optional.ofNullable(budgetKind);
    }

    /**
     * Returns the number of days the ad is scheduled to run.
     *
     * @return the run duration in days, or empty when the server omitted it
     */
    public OptionalLong durationInDays() {
        return durationInDays == null ? OptionalLong.empty() : OptionalLong.of(durationInDays);
    }

    /**
     * Returns the human-readable goal of the ad.
     *
     * @return the goal, or empty when the server omitted it
     */
    public Optional<String> objective() {
        return Optional.ofNullable(objective);
    }

    /**
     * Returns the audience the ad targets.
     *
     * @return the audience, or empty when the server omitted it
     */
    public Optional<Audience> audience() {
        return Optional.ofNullable(audience);
    }

    /**
     * Returns the body text shown in each of the ad's creatives.
     *
     * @return an unmodifiable view of the creative body texts; never
     *         {@code null}, possibly empty
     */
    public List<String> creativeBodies() {
        return Collections.unmodifiableList(creativeBodies);
    }

    /**
     * Returns whether the ad's targeting may still be changed by the merchant.
     *
     * @return {@code true} when the targeting is editable, {@code false}
     *         otherwise
     */
    public boolean targetingEditable() {
        return targetingEditable;
    }

    /**
     * What a live advertisement promotes and where it is hosted.
     *
     * <p>A boosted ad is anchored to a promoted surface (for example a message
     * or a status), to a target object on the advertising platform, and to the
     * page the boost runs under. These bindings tell the merchant which content
     * the ad amplifies.
     */
    @ProtobufMessage(name = "BusinessBoostedComponent.Placement")
    public static final class Placement {
        /**
         * Server-defined surface being promoted (for example a message or a
         * status). The full marker set is not recoverable from the WhatsApp
         * client, so the raw marker is exposed as a string. Empty when the
         * server omitted it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String promotedSurface;

        /**
         * Identifier of the promoted target object on the advertising platform.
         * Empty when the server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String targetId;

        /**
         * Identifier of the page the boost runs under. Empty when the server
         * omitted it.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        final String pageId;

        /**
         * Constructs a new {@code Placement}. The reference arguments may be
         * {@code null} when the server omitted them.
         *
         * @param promotedSurface the promoted-surface marker, or {@code null}
         * @param targetId        the target object identifier, or {@code null}
         * @param pageId          the page identifier, or {@code null}
         */
        Placement(String promotedSurface, String targetId, String pageId) {
            this.promotedSurface = promotedSurface;
            this.targetId = targetId;
            this.pageId = pageId;
        }

        /**
         * Returns the server-defined surface being promoted.
         *
         * @return the promoted-surface marker, or empty when the server omitted
         *         it
         */
        public Optional<String> promotedSurface() {
            return Optional.ofNullable(promotedSurface);
        }

        /**
         * Returns the identifier of the promoted target object.
         *
         * @return the target object id, or empty when the server omitted it
         */
        public Optional<String> targetId() {
            return Optional.ofNullable(targetId);
        }

        /**
         * Returns the identifier of the page the boost runs under.
         *
         * @return the page id, or empty when the server omitted it
         */
        public Optional<String> pageId() {
            return Optional.ofNullable(pageId);
        }
    }

    /**
     * The audience a live advertisement targets.
     *
     * <p>An ad is shown to a configured group of people; the server identifies
     * that group by a display name and an identifier the merchant can reuse for
     * later ads.
     */
    @ProtobufMessage(name = "BusinessBoostedComponent.Audience")
    public static final class Audience {
        /**
         * Display name of the targeted audience. Empty when the server omitted
         * it.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        final String name;

        /**
         * Server-issued identifier of the targeted audience. Empty when the
         * server omitted it.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        final String id;

        /**
         * Constructs a new {@code Audience}. The reference arguments may be
         * {@code null} when the server omitted them.
         *
         * @param name the audience display name, or {@code null}
         * @param id   the audience identifier, or {@code null}
         */
        Audience(String name, String id) {
            this.name = name;
            this.id = id;
        }

        /**
         * Returns the display name of the targeted audience.
         *
         * @return the audience name, or empty when the server omitted it
         */
        public Optional<String> name() {
            return Optional.ofNullable(name);
        }

        /**
         * Returns the server-issued identifier of the targeted audience.
         *
         * @return the audience id, or empty when the server omitted it
         */
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }
    }
}
