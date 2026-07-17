package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * Metadata attached to messages that are part of a premium (paid
 * messaging) business campaign.
 *
 * <p>WhatsApp Business allows verified businesses to reach users
 * through paid campaigns managed on the WhatsApp server. When a
 * message is sent as part of one of these campaigns, it carries the
 * identifier of the campaign in this metadata block so that the
 * server can bill the business, track engagement, and enforce
 * campaign-specific quotas.
 *
 * <p>This information is normally irrelevant to the end user and is
 * handled transparently by the messaging infrastructure.
 */
@ProtobufMessage(name = "PremiumMessageInfo")
public final class PremiumMessageInfo {
    /**
     * The identifier of the server-side premium messaging campaign
     * this message is attributed to.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String serverCampaignId;


    /**
     * Constructs a new {@code PremiumMessageInfo}.
     *
     * <p>The constructor is package-private; use
     * {@code PremiumMessageInfoBuilder} to instantiate new values.
     *
     * @param serverCampaignId the campaign identifier, or {@code null}
     */
    PremiumMessageInfo(String serverCampaignId) {
        this.serverCampaignId = serverCampaignId;
    }

    /**
     * Returns the identifier of the premium messaging campaign the
     * message belongs to, if any.
     *
     * @return an {@link Optional} holding the campaign identifier, or
     *         empty if none was set
     */
    public Optional<String> serverCampaignId() {
        return Optional.ofNullable(serverCampaignId);
    }

    /**
     * Updates the identifier of the premium messaging campaign.
     *
     * @param serverCampaignId the new campaign identifier, or
     *                         {@code null} to clear
     */
    public void setServerCampaignId(String serverCampaignId) {
        this.serverCampaignId = serverCampaignId;
    }
}
