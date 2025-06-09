package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.Map;
import java.util.Optional;

@Json
public final class NewsletterResponse {
    private static final NewsletterResponse EMPTY = new NewsletterResponse(null);

    private final Newsletter newsletter;

    private NewsletterResponse(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    @Json.Creator
    static NewsletterResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("newsletter") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        var newsletter = Jsonb.builder()
                .build()
                .type(Newsletter.class)
                .fromObject(response);
        return new NewsletterResponse(newsletter);
    }

    public Optional<Newsletter> newsletter() {
        return Optional.ofNullable(newsletter);
    }
}