package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * The Facebook actor whose advertising account funds the merchant's
 * WhatsApp Business "Click-to-WhatsApp" advertisements.
 *
 * <p>When a merchant manages a paid promotion that opens a chat with the
 * business when tapped, the spend is billed to a linked Facebook actor.
 * The ad-management header surfaces this actor so the merchant always sees
 * whose account is being charged: it shows the actor's profile picture and
 * the kind of actor (for example a personal profile or a Facebook Page).
 *
 * <p>This model is that linked actor: the kind of actor, the URL of the
 * profile picture rendered in the header, and the actor's Facebook
 * identifier (a numeric advertising identifier, not a WhatsApp address).
 */
@ProtobufMessage(name = "BusinessAdBillingActor")
public final class BusinessAdBillingActor {
    /**
     * Kind of actor the spend is billed to, as a server-defined marker
     * (for example a personal profile or a Facebook Page). The full marker
     * set is not recoverable from the WhatsApp client, so the raw marker
     * is exposed as a string. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String kind;

    /**
     * URL of the actor's profile picture, rendered in the ad-management
     * header. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String profilePictureUrl;

    /**
     * Server-issued identifier of the actor. A numeric Facebook
     * advertising identifier, not a WhatsApp address. {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String id;

    /**
     * Constructs a new {@code BusinessAdBillingActor}. Any reference
     * argument may be {@code null} when the server omitted the
     * corresponding field.
     *
     * @param kind              the actor kind marker, or {@code null}
     * @param profilePictureUrl the actor's profile-picture URL, or
     *                          {@code null}
     * @param id                the actor's Facebook identifier, or
     *                          {@code null}
     */
    BusinessAdBillingActor(String kind, String profilePictureUrl, String id) {
        this.kind = kind;
        this.profilePictureUrl = profilePictureUrl;
        this.id = id;
    }

    /**
     * Returns the kind of actor the spend is billed to.
     *
     * @return an {@code Optional} carrying the actor kind marker, or empty
     *         when the server omitted it
     */
    public Optional<String> kind() {
        return Optional.ofNullable(kind);
    }

    /**
     * Returns the URL of the actor's profile picture.
     *
     * @return an {@code Optional} carrying the profile-picture URL, or
     *         empty when the server omitted it
     */
    public Optional<String> profilePictureUrl() {
        return Optional.ofNullable(profilePictureUrl);
    }

    /**
     * Returns the actor's Facebook advertising identifier.
     *
     * @return an {@code Optional} carrying the actor identifier, or empty
     *         when the server omitted it
     */
    public Optional<String> id() {
        return Optional.ofNullable(id);
    }
}
