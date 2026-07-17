package com.github.auties00.cobalt.wire.linked.message;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

import java.util.Objects;

/**
 * A citation block attached to a message, typically produced by the
 * WhatsApp AI bot to reference an external source.
 *
 * <p>When the AI bot answers a question by consulting an external
 * knowledge provider, the response can include one or more citations
 * that attribute the information back to its source. Each citation
 * renders as a small card inside the chat with a title, optional
 * subtitle, and a tappable image.
 *
 * <p>All four fields are required for a citation to render correctly.
 */
@ProtobufMessage(name = "Citation")
public final class MessageCitation {
    /**
     * The headline of the citation, shown as the card's primary text.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    /**
     * The secondary text of the citation, shown beneath the title.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String subtitle;

    /**
     * The identifier of the citation within the content management
     * system that served it, used to resolve the full source when the
     * card is opened.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String cmsId;

    /**
     * The URL of the thumbnail image displayed on the citation card.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.STRING)
    String imageUrl;


    /**
     * Constructs a new {@code MessageCitation} with all required fields.
     *
     * <p>The constructor is package-private; use
     * {@code MessageCitationBuilder} to instantiate new citations.
     * All arguments must be non-{@code null}.
     *
     * @param title    the headline shown on the card
     * @param subtitle the secondary text shown below the title
     * @param cmsId    the CMS identifier of the source
     * @param imageUrl the URL of the thumbnail image
     * @throws NullPointerException if any argument is {@code null}
     */
    MessageCitation(String title, String subtitle, String cmsId, String imageUrl) {
        this.title = Objects.requireNonNull(title);
        this.subtitle = Objects.requireNonNull(subtitle);
        this.cmsId = Objects.requireNonNull(cmsId);
        this.imageUrl = Objects.requireNonNull(imageUrl);
    }

    /**
     * Returns the headline shown as the card's primary text.
     *
     * @return the non-{@code null} title
     */
    public String title() {
        return title;
    }

    /**
     * Returns the secondary text displayed beneath the title.
     *
     * @return the non-{@code null} subtitle
     */
    public String subtitle() {
        return subtitle;
    }

    /**
     * Returns the CMS identifier used to resolve the source when the
     * citation card is opened.
     *
     * @return the non-{@code null} CMS identifier
     */
    public String cmsId() {
        return cmsId;
    }

    /**
     * Returns the URL of the thumbnail image displayed on the card.
     *
     * @return the non-{@code null} image URL
     */
    public String imageUrl() {
        return imageUrl;
    }

    /**
     * Updates the headline shown as the card's primary text.
     *
     * @param title the new title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Updates the secondary text displayed beneath the title.
     *
     * @param subtitle the new subtitle
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Updates the CMS identifier.
     *
     * @param cmsId the new CMS identifier
     */
    public void setCmsId(String cmsId) {
        this.cmsId = cmsId;
    }

    /**
     * Updates the URL of the thumbnail image.
     *
     * @param imageUrl the new image URL
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
