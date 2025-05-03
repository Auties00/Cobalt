package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Map;
import java.util.Optional;

@Json
public final class NewsletterMuteResponse {
    private static final NewsletterMuteResponse EMPTY = new NewsletterMuteResponse(null, false);

    private final Jid jid;
    private final boolean mute;

    private NewsletterMuteResponse(Jid jid, boolean mute) {
        this.jid = jid;
        this.mute = mute;
    }

    @Json.Creator
    static NewsletterMuteResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_notify_newsletter_on_mute_change") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("id") instanceof String value)) {
            return EMPTY;
        }

        var jid = Jid.of(value);
        var mute = response.get("mute") instanceof Boolean muteValue ? muteValue : false;
        return new NewsletterMuteResponse(jid, mute);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }

    public boolean mute() {
        return mute;
    }
}