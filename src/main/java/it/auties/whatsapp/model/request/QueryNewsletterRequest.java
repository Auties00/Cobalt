package it.auties.whatsapp.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterViewerRole;

public record QueryNewsletterRequest(Variable variables) {
    public record Variable(Input input, @JsonProperty("fetch_viewer_metadata") boolean fetchViewerMetadata,
                           @JsonProperty("fetch_full_image") boolean fetchFullImage,
                           @JsonProperty("fetch_creation_time") boolean fetchCreationTime) {

    }

    public record Input(Jid key, String type, @JsonProperty("view_role") NewsletterViewerRole viewRole) {

    }
}
