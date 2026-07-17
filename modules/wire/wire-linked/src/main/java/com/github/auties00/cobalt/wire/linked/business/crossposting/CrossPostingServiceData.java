package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Aggregated state of the caller's Meta cross-posting and account-linking
 * services.
 *
 * <p>WhatsApp lets the user cross-post statuses to linked Facebook and Instagram
 * surfaces (Stories or Reels) and to link a Facebook or Instagram account to the
 * WhatsApp account. Before the cross-posting and linking surfaces are rendered
 * the client fetches the current state of these services: the list of linked
 * destination accounts ({@linkplain CrossPostingDestinationAccount accounts}),
 * the additional-feature-set eligibility marker
 * ({@linkplain CrossPostingAdditionalFeatureSet feature set}), and the
 * per-destination linking-eligibility flags
 * ({@linkplain CrossPostingLinkEligibility link eligibility}).
 *
 * <p>This model is that aggregated state.
 */
@ProtobufMessage(name = "CrossPostingServiceData")
public final class CrossPostingServiceData {
    /**
     * Linked cross-posting destination accounts, in the order the server
     * returned them. Never {@code null}, possibly empty when the user has
     * linked none.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final List<CrossPostingDestinationAccount> destinationAccounts;

    /**
     * Server-reported additional-feature-set eligibility, or {@code null} when
     * the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
    final CrossPostingAdditionalFeatureSet additionalFeatureSet;

    /**
     * Server-reported per-destination linking-eligibility flags, or
     * {@code null} when the server omitted them.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final CrossPostingLinkEligibility linkEligibility;

    /**
     * Constructs a new {@code CrossPostingServiceData}. A {@code null}
     * {@code destinationAccounts} is coerced to an empty list; the other
     * reference arguments may be {@code null} when the server omitted them.
     *
     * @param destinationAccounts  the linked destination accounts; {@code null}
     *                             treated as empty
     * @param additionalFeatureSet the additional-feature-set eligibility, or
     *                             {@code null}
     * @param linkEligibility      the per-destination link-eligibility flags,
     *                             or {@code null}
     */
    CrossPostingServiceData(List<CrossPostingDestinationAccount> destinationAccounts,
                            CrossPostingAdditionalFeatureSet additionalFeatureSet,
                            CrossPostingLinkEligibility linkEligibility) {
        this.destinationAccounts = destinationAccounts == null ? List.of() : destinationAccounts;
        this.additionalFeatureSet = additionalFeatureSet;
        this.linkEligibility = linkEligibility;
    }

    /**
     * Returns the linked cross-posting destination accounts.
     *
     * @return an unmodifiable view of the linked accounts; never {@code null},
     *         possibly empty
     */
    public List<CrossPostingDestinationAccount> destinationAccounts() {
        return Collections.unmodifiableList(destinationAccounts);
    }

    /**
     * Returns the additional-feature-set eligibility.
     *
     * @return the additional-feature-set eligibility, or empty when the server
     *         omitted it
     */
    public Optional<CrossPostingAdditionalFeatureSet> additionalFeatureSet() {
        return Optional.ofNullable(additionalFeatureSet);
    }

    /**
     * Returns the per-destination linking-eligibility flags.
     *
     * @return the per-destination link-eligibility flags, or empty when the
     *         server omitted them
     */
    public Optional<CrossPostingLinkEligibility> linkEligibility() {
        return Optional.ofNullable(linkEligibility);
    }
}
