package com.github.auties00.cobalt.wire.linked.newsletter;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * A single anonymous attribution event reported to the relay so it can
 * rank channels in the explore tab and admin dashboards.
 *
 * <p>When the client renders a channel surface that exposes a particular
 * capability (for example, the Insights dashboard, the Photo Polls
 * authoring sheet, or the Music attachment picker), it batches an
 * exposure entry pairing the channel JID with the capability whose UI
 * was actually surfaced. The batched list is later flushed to the relay
 * via the dedicated MEX log job; the relay treats the events as
 * anonymous signal for ranking and feature-rollout decisions and never
 * attributes them back to the user.
 *
 * <p>An exposure carries no timestamp because the relay records the
 * server-side ingest time.
 */
@ProtobufMessage
public final class NewsletterExposure {
    /**
     * The channel JID serialised as a string, exactly as it appears on
     * the wire under the {@code newsletter_id} key.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String newsletterId;

    /**
     * The resolved server-side capability token (for example
     * {@code "INSIGHTS"} or {@code "PHOTO_POLLS"}), exactly as it appears
     * on the wire under the {@code capability} key.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String capability;

    /**
     * Constructs a new exposure event with the supplied channel JID and
     * capability token.
     *
     * @param newsletterId the channel JID
     * @param capability   the capability token
     */
    NewsletterExposure(String newsletterId, String capability) {
        this.newsletterId = newsletterId;
        this.capability = capability;
    }

    /**
     * Returns the channel JID this exposure event refers to.
     *
     * @return an {@code Optional} containing the channel JID, or empty
     *         if not set
     */
    public Optional<String> newsletterId() {
        return Optional.ofNullable(newsletterId);
    }

    /**
     * Returns the capability token whose UI was surfaced.
     *
     * @return an {@code Optional} containing the capability token, or
     *         empty if not set
     */
    public Optional<String> capability() {
        return Optional.ofNullable(capability);
    }

    /**
     * Sets the channel JID this exposure event refers to.
     *
     * @param newsletterId the new channel JID, or {@code null} to
     *                     clear it
     */
    public void setNewsletterId(String newsletterId) {
        this.newsletterId = newsletterId;
    }

    /**
     * Sets the capability token whose UI was surfaced.
     *
     * @param capability the new capability token, or {@code null} to
     *                   clear it
     */
    public void setCapability(String capability) {
        this.capability = capability;
    }
}
