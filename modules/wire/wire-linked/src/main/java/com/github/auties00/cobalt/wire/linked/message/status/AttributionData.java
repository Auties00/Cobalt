package com.github.auties00.cobalt.wire.linked.message.status;

/**
 * Marker type for the payload carried by a {@link StatusAttribution}.
 *
 * <p>A status update on WhatsApp may be attributed to a variety of sources,
 * such as a reshare of another user's status, an external share from another
 * app, a music track, a group status, a Ray-Ban Meta glasses capture, or
 * AI-generated content. Each attribution kind carries a different payload
 * with its own set of fields, and this sealed interface enumerates the
 * permitted payload classes.
 *
 * <p>Consumers typically obtain an {@code AttributionData} through
 * {@link StatusAttribution#attributionData()} and then use pattern matching
 * on the sealed permits list to react to the specific attribution kind.
 *
 * @see StatusAttribution
 * @see StatusAttribution.StatusReshare
 * @see StatusAttribution.ExternalShare
 * @see StatusAttribution.Music
 * @see StatusAttribution.GroupStatus
 * @see StatusAttribution.RLAttribution
 * @see StatusAttribution.AiCreatedAttribution
 */
public sealed interface AttributionData permits StatusAttribution.StatusReshare, StatusAttribution.ExternalShare, StatusAttribution.Music, StatusAttribution.GroupStatus, StatusAttribution.RLAttribution, StatusAttribution.AiCreatedAttribution {
}
