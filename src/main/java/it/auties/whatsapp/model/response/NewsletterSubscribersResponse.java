package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;

import java.util.Map;
import java.util.OptionalLong;

@Json
public final class NewsletterSubscribersResponse {
    private static final NewsletterSubscribersResponse EMPTY = new NewsletterSubscribersResponse(null);

    private final Long subscribersCount;

    private NewsletterSubscribersResponse(Long subscribersCount) {
        this.subscribersCount = subscribersCount;
    }

    @Json.Creator
    static NewsletterSubscribersResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_newsletter") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("thread_metadata") instanceof Map<?,?> metadata)) {
            return EMPTY;
        }

        if(!(metadata.get("subscribers_count") instanceof Number value)) {
            return EMPTY;
        }

        return new NewsletterSubscribersResponse(value.longValue());
    }

    public OptionalLong subscribersCount() {
        return subscribersCount == null ? OptionalLong.empty() : OptionalLong.of(subscribersCount);
    }
}