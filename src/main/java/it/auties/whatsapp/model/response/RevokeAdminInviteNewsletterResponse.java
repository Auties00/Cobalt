package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Map;
import java.util.Optional;

@Json
public final class RevokeAdminInviteNewsletterResponse {
    private static final RevokeAdminInviteNewsletterResponse EMPTY = new RevokeAdminInviteNewsletterResponse(null);

    private final Jid jid;

    private RevokeAdminInviteNewsletterResponse(Jid jid) {
        this.jid = jid;
    }

    @Json.Creator
    static RevokeAdminInviteNewsletterResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        if(!(data.get("xwa2_newsletter_admin_invite_revoke") instanceof Map<?,?> response)) {
            return EMPTY;
        }

        if(!(response.get("id") instanceof String value)) {
            return EMPTY;
        }

        var jid = Jid.of(value);
        return new RevokeAdminInviteNewsletterResponse(jid);
    }

    public Optional<Jid> jid() {
        return Optional.ofNullable(jid);
    }
}