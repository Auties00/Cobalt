package com.github.auties00.cobalt.wire.linked.business.crossposting;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Eligibility tree for cross-posting a set of WhatsApp statuses to linked Meta
 * destinations.
 *
 * <p>Before WhatsApp cross-posts statuses to a linked Facebook or Instagram
 * surface it asks the server to evaluate the per-status, per-destination
 * eligibility for the operation. The server replies with the per-purpose public
 * keys used to encrypt the cross-posted payload
 * ({@linkplain CrossPostingPublicKeys public keys}), the echoed unique-ids
 * collection, the per-status resolved destination identities
 * ({@linkplain CrossPostingDestinationResolution resolutions}), and the
 * per-destination eligibility parameters
 * ({@linkplain CrossPostingDestinationParameters parameters}).
 */
@ProtobufMessage(name = "CrossPostingEligibility")
public final class CrossPostingEligibility {
    /**
     * Per-purpose public keys used to encrypt the cross-posted payload, or
     * {@code null} when the server omitted them.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
    final CrossPostingPublicKeys publicKeys;

    /**
     * Echoed unique-ids collection. {@code null} when the server omitted it.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String uniqueIds;

    /**
     * Per-status resolved destination identities, in the order the server
     * returned them. Never {@code null}, possibly empty when the server
     * returned none.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<CrossPostingDestinationResolution> destinationResolutions;

    /**
     * Per-destination eligibility parameters, in the order the server returned
     * them. Never {@code null}, possibly empty when the server returned none.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.MESSAGE)
    final List<CrossPostingDestinationParameters> destinationParameters;

    /**
     * Constructs a new {@code CrossPostingEligibility}. A {@code null}
     * {@code destinationResolutions} or {@code destinationParameters} is
     * coerced to an empty list; the reference scalar arguments may be
     * {@code null} when the server omitted them.
     *
     * @param publicKeys             the per-purpose public keys, or {@code null}
     * @param uniqueIds              the echoed unique-ids collection, or {@code null}
     * @param destinationResolutions the per-status destination resolutions;
     *                               {@code null} treated as empty
     * @param destinationParameters  the per-destination eligibility parameters;
     *                               {@code null} treated as empty
     */
    CrossPostingEligibility(CrossPostingPublicKeys publicKeys,
                            String uniqueIds,
                            List<CrossPostingDestinationResolution> destinationResolutions,
                            List<CrossPostingDestinationParameters> destinationParameters) {
        this.publicKeys = publicKeys;
        this.uniqueIds = uniqueIds;
        this.destinationResolutions = destinationResolutions == null ? List.of() : destinationResolutions;
        this.destinationParameters = destinationParameters == null ? List.of() : destinationParameters;
    }

    /**
     * Returns the per-purpose public keys.
     *
     * @return the per-purpose public keys, or empty when the server omitted
     *         them
     */
    public Optional<CrossPostingPublicKeys> publicKeys() {
        return Optional.ofNullable(publicKeys);
    }

    /**
     * Returns the echoed unique-ids collection.
     *
     * @return the unique-ids collection, or empty when the server omitted it
     */
    public Optional<String> uniqueIds() {
        return Optional.ofNullable(uniqueIds);
    }

    /**
     * Returns the per-status resolved destination identities.
     *
     * @return an unmodifiable view of the resolutions; never {@code null},
     *         possibly empty
     */
    public List<CrossPostingDestinationResolution> destinationResolutions() {
        return Collections.unmodifiableList(destinationResolutions);
    }

    /**
     * Returns the per-destination eligibility parameters.
     *
     * @return an unmodifiable view of the parameters; never {@code null},
     *         possibly empty
     */
    public List<CrossPostingDestinationParameters> destinationParameters() {
        return Collections.unmodifiableList(destinationParameters);
    }
}
