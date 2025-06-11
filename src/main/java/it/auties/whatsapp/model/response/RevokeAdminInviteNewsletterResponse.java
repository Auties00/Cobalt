package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public final class RevokeAdminInviteNewsletterResponse {
    private final Jid jid;

    private RevokeAdminInviteNewsletterResponse(Jid jid) {
        this.jid = jid;
    }

    public static Optional<RevokeAdminInviteNewsletterResponse> ofJson(byte[] json) {
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

        var response = data.getJSONObject("xwa2_newsletter_admin_invite_revoke");
        if(response == null) {
            return Optional.empty();
        }

        var id = response.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var jid = Jid.of(id);
        var result = new RevokeAdminInviteNewsletterResponse(jid);
        return Optional.of(result);
    }

    public Jid jid() {
        return jid;
    }
}