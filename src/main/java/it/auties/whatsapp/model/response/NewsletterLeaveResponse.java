package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Map;
import java.util.Optional;

@Json
public final class NewsletterLeaveResponse {
    private static final NewsletterLeaveResponse EMPTY = new NewsletterLeaveResponse(null);

    private final Jid jid;

    private NewsletterLeaveResponse(Jid jid) {
        this.jid = jid;
    }

    @Json.Creator
    static NewsletterLeaveResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_notify_newsletter_on_leave") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("id") instanceof String value)) {
            return EMPTY;
        }

        var jid = Jid.of(value);
        return new NewsletterLeaveResponse(jid);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }
}