package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The advertising-platform page provisioned for a WhatsApp Ads identity.
 *
 * <p>Entering the Click-to-WhatsApp advertising flow either provisions a new
 * advertising-platform page tied to the advertiser's phone number or resumes
 * the existing one. The server reports the resulting page identifier the
 * WhatsApp client subsequently passes to downstream advertising calls. This
 * model is that provisioned page; an absent {@link #id() id} signals the
 * provisioning failed.
 */
@ProtobufMessage(name = "WhatsAppAdsIdentityPage")
public final class WhatsAppAdsIdentityPage {
    /**
     * Advertising-platform page identifier, or {@code null} when the server
     * omitted it (which the client treats as a provisioning failure).
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code WhatsAppAdsIdentityPage}.
     *
     * @param id the page identifier, or {@code null}
     */
    WhatsAppAdsIdentityPage(String id) {
        this.id = id;
    }

    /**
     * Returns the advertising-platform page identifier.
     *
     * @return the page identifier, or empty when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
