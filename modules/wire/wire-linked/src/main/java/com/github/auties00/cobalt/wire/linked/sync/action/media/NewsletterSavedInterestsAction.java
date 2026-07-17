package com.github.auties00.cobalt.wire.linked.sync.action.media;

import com.github.auties00.cobalt.wire.linked.sync.action.SyncActionEmptyArgs;
import com.github.auties00.cobalt.wire.linked.sync.action.SyncAction;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A sync action that propagates the set of newsletter interests the user has
 * saved across linked devices.
 *
 * <p>The action carries a serialized representation of the user's selected
 * newsletter topics so that every linked device shows the same saved interests
 * when the user browses newsletter recommendations.
 */
@ProtobufMessage(name = "SyncActionValue.NewsletterSavedInterestsAction")
public final class NewsletterSavedInterestsAction implements SyncAction<SyncActionEmptyArgs> {
    /**
     * The app-state action name that identifies this action type on the wire.
     */
    public static final String ACTION_NAME = "newsletter_saved_interests";

    /**
     * The app-state action version that identifies this action revision on the
     * wire.
     */
    public static final int ACTION_VERSION = 1;

    /**
     * Returns the action name used to route this action through the app-state
     * sync pipeline.
     *
     * @return the canonical action name
     */
    @Override
    public String actionName() {
        return ACTION_NAME;
    }

    /**
     * Returns the action version used to route this action through the
     * app-state sync pipeline.
     *
     * @return the canonical action version
     */
    @Override
    public int actionVersion() {
        return ACTION_VERSION;
    }


    /**
     * The opaque serialized payload describing the user's saved newsletter
     * interests.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String newsletterSavedInterests;


    /**
     * Constructs a new {@code NewsletterSavedInterestsAction} carrying the
     * supplied serialized interests payload.
     *
     * @param newsletterSavedInterests the serialized interests payload, or
     *                                 {@code null} if unset
     */
    NewsletterSavedInterestsAction(String newsletterSavedInterests) {
        this.newsletterSavedInterests = newsletterSavedInterests;
    }

    /**
     * Returns the serialized payload describing the user's saved newsletter
     * interests.
     *
     * @return the interests payload, or {@link Optional#empty()} if unset
     */
    public Optional<String> newsletterSavedInterests() {
        return Optional.ofNullable(newsletterSavedInterests);
    }

    /**
     * Sets the serialized payload describing the user's saved newsletter
     * interests.
     *
     * @param newsletterSavedInterests the new interests payload, or
     *                                 {@code null} to clear it
     */
    public void setNewsletterSavedInterests(String newsletterSavedInterests) {
        this.newsletterSavedInterests = newsletterSavedInterests;
    }
}
