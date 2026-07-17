package com.github.auties00.cobalt.wire.linked.business.subscription;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Optional;

/**
 * One subscription a merchant may be offered an upsell into.
 *
 * <p>WhatsApp surfaces subscription upsells (such as Meta Verified) at various
 * entry points. For each subscription the server reports its
 * {@linkplain #subscriptionType() type}, whether the web client is
 * {@linkplain #webEntryPointEligible() eligible} to surface the upsell, and
 * the {@linkplain #webEntryPointRedirectionUri() redirection URI} the web
 * client opens to start the subscription flow.
 *
 * <p>This model is one such subscription entry point as the server reports it.
 * The subscription type is exposed as a raw marker (for example a Meta
 * Verified marker) because the value set is server-defined.
 */
@ProtobufMessage(name = "SubscriptionEntryPoint")
public final class SubscriptionEntryPoint {
    /**
     * Subscription type the entry point advertises, as a server-defined marker.
     * {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String subscriptionType;

    /**
     * Whether the web client is eligible to surface this subscription's entry
     * point. Reported by the server as a flag; {@code false} when the server
     * omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean webEntryPointEligible;

    /**
     * URI the web client opens to start the subscription flow, or {@code null}
     * when the server omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String webEntryPointRedirectionUri;

    /**
     * Constructs a new {@code SubscriptionEntryPoint}. The reference arguments
     * may be {@code null} when the server omitted them.
     *
     * @param subscriptionType            the subscription-type marker, or {@code null}
     * @param webEntryPointEligible       whether the web entry point is eligible
     * @param webEntryPointRedirectionUri the web redirection URI, or {@code null}
     */
    SubscriptionEntryPoint(String subscriptionType, boolean webEntryPointEligible, String webEntryPointRedirectionUri) {
        this.subscriptionType = subscriptionType;
        this.webEntryPointEligible = webEntryPointEligible;
        this.webEntryPointRedirectionUri = webEntryPointRedirectionUri;
    }

    /**
     * Returns the subscription type the entry point advertises.
     *
     * @return the subscription-type marker, or empty when the server omitted it
     */
    public Optional<String> subscriptionType() {
        return Optional.ofNullable(subscriptionType);
    }

    /**
     * Returns whether the web client is eligible to surface this subscription's
     * entry point.
     *
     * @return {@code true} when the server reported the web entry point
     *         eligible, {@code false} otherwise
     */
    public boolean webEntryPointEligible() {
        return webEntryPointEligible;
    }

    /**
     * Returns the URI the web client opens to start the subscription flow.
     *
     * @return the web redirection URI, or empty when the server omitted it
     */
    public Optional<String> webEntryPointRedirectionUri() {
        return Optional.ofNullable(webEntryPointRedirectionUri);
    }
}
