package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterState;

import java.util.Map;
import java.util.Optional;

@Json
public final class NewsletterStateResponse {
    private static final NewsletterState NO_STATE = new NewsletterState(null);
    private static final NewsletterStateResponse EMPTY = new NewsletterStateResponse(null, false, NO_STATE);

    private final Jid jid;
    private final boolean requestor;
    private final NewsletterState state;

    private NewsletterStateResponse(Jid jid, boolean requestor, NewsletterState state) {
        this.jid = jid;
        this.requestor = requestor;
        this.state = state;
    }

    @Json.Creator
    static NewsletterStateResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_notify_newsletter_on_state_change") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("id") instanceof String value)) {
            return EMPTY;
        }

        var jid = Jid.of(value);
        var requestor = response.get("is_requestor") instanceof Boolean booleanValue ? booleanValue : false;
        var state = response.get("state") instanceof String stringValue ? new NewsletterState(stringValue) : NO_STATE;
        return new NewsletterStateResponse(jid, requestor, state);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }

    public boolean requestor() {
        return requestor;
    }

    public NewsletterState state() {
        return state;
    }
}