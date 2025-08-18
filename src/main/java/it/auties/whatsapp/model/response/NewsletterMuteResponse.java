package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.jid.Jid;

import java.util.Optional;

public final class NewsletterMuteResponse {
    private final Jid jid;
    private final boolean mute;

    private NewsletterMuteResponse(Jid jid, boolean mute) {
        this.jid = jid;
        this.mute = mute;
    }

    public static Optional<NewsletterMuteResponse> ofJson(String json) {
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

        var response = data.getJSONObject("xwa2_notify_newsletter_on_mute_change");
        if (response == null) {
            return Optional.empty();
        }

        var id = response.getString("id");
        if (id == null) {
            return Optional.empty();
        }

        var jid = Jid.of(id);
        var mute = response.getBooleanValue("mute", false);
        var result = new NewsletterMuteResponse(jid, mute);
        return Optional.of(result);
    }

    public Jid jid() {
        return jid;
    }

    public boolean mute() {
        return mute;
    }
}