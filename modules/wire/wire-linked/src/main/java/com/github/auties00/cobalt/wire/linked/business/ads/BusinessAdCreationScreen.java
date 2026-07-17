package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The opening screen a merchant sees when starting a WhatsApp Business
 * advertisement.
 *
 * <p>When a merchant begins creating a "Click-to-WhatsApp" ad (a paid promotion
 * that opens a chat with the business when tapped), the server returns the data
 * the first screen needs: the budget steps the merchant can pick from, the
 * publisher platforms the ad may run on, the business page being promoted, how
 * many advertising accounts are already linked, and, when the account still
 * needs onboarding, the contact email to confirm. This model gathers the
 * fields an embedder needs to render that screen; the full editor policy is not
 * exposed here.
 *
 * <p>{@link #budgetOptions()} lists the offered spend amounts in the billing
 * currency's minor units; {@link #publisherPlatforms()} lists the labels of the
 * platforms the ad may run on; {@link #pageName()} and
 * {@link #pageVerified()} describe the promoted page; {@link #linkedAdAccountCount()}
 * is the number of linked advertising accounts; and {@link #onboardingEmail()} is
 * the contact email awaiting confirmation, present only when onboarding is
 * pending.
 */
@ProtobufMessage(name = "BusinessAdCreationScreen")
public final class BusinessAdCreationScreen {
    /**
     * Offered spend amounts in the billing currency's minor units, each
     * expressed as a string to preserve the server's exact precision, in the
     * order the server returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final List<String> budgetOptions;

    /**
     * Labels of the publisher platforms the ad may run on, in the order the
     * server returned them. Never {@code null}, possibly empty.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final List<String> publisherPlatforms;

    /**
     * Display name of the business page being promoted. Empty when the server
     * omitted it.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String pageName;

    /**
     * Whether the promoted business page is verified. Reported by the server;
     * {@code false} when the server omitted it.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean pageVerified;

    /**
     * Number of advertising accounts already linked for click-to-WhatsApp ads,
     * or {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.INT64)
    final Long linkedAdAccountCount;

    /**
     * Contact email awaiting confirmation during account onboarding, present
     * only when onboarding is pending. Empty when the server omitted it.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.STRING)
    final String onboardingEmail;

    /**
     * Constructs a new {@code BusinessAdCreationScreen}. A {@code null}
     * {@code budgetOptions} or {@code publisherPlatforms} is coerced to an empty
     * list, and the other reference arguments may be {@code null} when the server
     * omitted them.
     *
     * @param budgetOptions        the offered spend amounts; {@code null} treated as empty
     * @param publisherPlatforms   the publisher-platform labels; {@code null} treated as empty
     * @param pageName             the promoted page name, or {@code null}
     * @param pageVerified         whether the promoted page is verified
     * @param linkedAdAccountCount the linked advertising-account count, or {@code null}
     * @param onboardingEmail      the onboarding email, or {@code null}
     */
    BusinessAdCreationScreen(List<String> budgetOptions,
                             List<String> publisherPlatforms,
                             String pageName,
                             boolean pageVerified,
                             Long linkedAdAccountCount,
                             String onboardingEmail) {
        this.budgetOptions = budgetOptions == null ? List.of() : budgetOptions;
        this.publisherPlatforms = publisherPlatforms == null ? List.of() : publisherPlatforms;
        this.pageName = pageName;
        this.pageVerified = pageVerified;
        this.linkedAdAccountCount = linkedAdAccountCount;
        this.onboardingEmail = onboardingEmail;
    }

    /**
     * Returns the offered spend amounts in the billing currency's minor units.
     *
     * @return an unmodifiable view of the offered spend amounts; never
     *         {@code null}, possibly empty
     */
    public List<String> budgetOptions() {
        return Collections.unmodifiableList(budgetOptions);
    }

    /**
     * Returns the labels of the publisher platforms the ad may run on.
     *
     * @return an unmodifiable view of the publisher-platform labels; never
     *         {@code null}, possibly empty
     */
    public List<String> publisherPlatforms() {
        return Collections.unmodifiableList(publisherPlatforms);
    }

    /**
     * Returns the display name of the business page being promoted.
     *
     * @return the promoted page name, or empty when the server omitted it
     */
    public Optional<String> pageName() {
        return Optional.ofNullable(pageName);
    }

    /**
     * Returns whether the promoted business page is verified.
     *
     * @return {@code true} when the page is verified, {@code false} otherwise
     */
    public boolean pageVerified() {
        return pageVerified;
    }

    /**
     * Returns the number of advertising accounts already linked for
     * click-to-WhatsApp ads.
     *
     * @return the linked advertising-account count, or empty when the server
     *         omitted it
     */
    public OptionalLong linkedAdAccountCount() {
        return linkedAdAccountCount == null ? OptionalLong.empty() : OptionalLong.of(linkedAdAccountCount);
    }

    /**
     * Returns the contact email awaiting confirmation during account onboarding.
     *
     * @return the onboarding email, or empty when the server omitted it or
     *         onboarding is not pending
     */
    public Optional<String> onboardingEmail() {
        return Optional.ofNullable(onboardingEmail);
    }
}
