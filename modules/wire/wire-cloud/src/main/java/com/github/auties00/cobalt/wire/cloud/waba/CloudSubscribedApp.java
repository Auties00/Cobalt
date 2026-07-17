package com.github.auties00.cobalt.wire.cloud.waba;

import java.util.Objects;
import java.util.Optional;

/**
 * An application subscribed to a WhatsApp Business Account's webhooks.
 *
 * <p>A WhatsApp Business Account fans its webhook notifications out to the Meta applications subscribed to
 * it. This model projects one such subscription entry: the subscribed application's {@link #id() id}, its
 * {@link #name() display name} when the listing carried one, and the per-subscription
 * {@link #overrideCallbackUrl() callback URL override} when the application points this account's
 * notifications at a URL other than its default. The id is always present; the name and override are
 * populated only when the listing returned them.
 */
public final class CloudSubscribedApp {
    /**
     * The subscribed application's id.
     */
    private final String id;

    /**
     * The subscribed application's display name, or {@code null} when not projected.
     */
    private final String name;

    /**
     * The per-subscription callback URL override, or {@code null} when the application uses its default.
     */
    private final String overrideCallbackUrl;

    /**
     * Constructs a new subscribed-app entry.
     *
     * @param id                  the subscribed application's id
     * @param name                the display name, or {@code null} when not projected
     * @param overrideCallbackUrl the callback URL override, or {@code null} when the default is used
     * @throws NullPointerException if {@code id} is {@code null}
     */
    public CloudSubscribedApp(String id, String name, String overrideCallbackUrl) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.overrideCallbackUrl = overrideCallbackUrl;
    }

    /**
     * Returns the subscribed application's id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the subscribed application's display name.
     *
     * @return an {@link Optional} carrying the name, or empty when not projected
     */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    /**
     * Returns the per-subscription callback URL override.
     *
     * @return an {@link Optional} carrying the override URL, or empty when the application uses its default
     */
    public Optional<String> overrideCallbackUrl() {
        return Optional.ofNullable(overrideCallbackUrl);
    }
}
