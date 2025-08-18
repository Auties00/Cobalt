package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public final class CreateAdminInviteNewsletterResponse {
    private final Jid jid;

    private final long expirationTime;

    private CreateAdminInviteNewsletterResponse(Jid jid, long expirationTime) {
        this.jid = jid;
        this.expirationTime = expirationTime;
    }

    public static Optional<CreateAdminInviteNewsletterResponse> ofJson(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if(data == null) {
            return Optional.empty();
        }

        var response = data.getJSONObject("xwa2_newsletter_admin_invite_create");
        if(response == null) {
            return Optional.empty();
        }

        var id = response.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var jid = Jid.of(id);
        var inviteExpirationTime = response.getLongValue("invite_expiration_time", 0);
        var result = new CreateAdminInviteNewsletterResponse(jid, inviteExpirationTime);
        return Optional.of(result);
    }

    public Jid jid() {
        return jid;
    }

    public long expirationTime() {
        return expirationTime;
    }
}