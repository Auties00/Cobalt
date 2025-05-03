package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Map;
import java.util.Optional;

@Json
public final class CreateAdminInviteNewsletterResponse {
    private static final CreateAdminInviteNewsletterResponse EMPTY = new CreateAdminInviteNewsletterResponse(null, 0);

    private final Jid jid;
    private final long expirationTime;

    private CreateAdminInviteNewsletterResponse(Jid jid, long expirationTime) {
        this.jid = jid;
        this.expirationTime = expirationTime;
    }

    @Json.Creator
    static CreateAdminInviteNewsletterResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_newsletter_admin_invite_create") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("id") instanceof String value)) {
            return EMPTY;
        }

        var jid = Jid.of(value);
        var mute = response.get("invite_expiration_time") instanceof Number number ? number.longValue() : 0;
        return new CreateAdminInviteNewsletterResponse(jid, mute);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }

    public long expirationTime() {
        return expirationTime;
    }
}