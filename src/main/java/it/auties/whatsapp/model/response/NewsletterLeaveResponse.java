package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public final class NewsletterLeaveResponse {
    private final Jid jid;

    private NewsletterLeaveResponse(Jid jid) {
        this.jid = jid;
    }

    public static Optional<NewsletterLeaveResponse> ofJson(String json) {
        if (json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if (jsonObject == null) {
            return Optional.empty();
        }

        var data = jsonObject.getJSONObject("data");
        if (data == null) {
            return Optional.empty();
        }

        var response = data.getJSONObject("xwa2_notify_newsletter_on_leave");
        if (response == null) {
            return Optional.empty();
        }

        var id = response.getString("id");
        if (id == null) {
            return Optional.empty();
        }

        var jid = Jid.of(id);
        var result = new NewsletterLeaveResponse(jid);
        return Optional.of(result);
    }

    public Jid jid() {
        return jid;
    }
}