package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.model.newsletter.NewsletterState;

import java.util.Optional;

public final class NewsletterStateResponse {
    private final Jid jid;

    private final boolean requestor;

    private final NewsletterState state;

    private NewsletterStateResponse(Jid jid, boolean requestor, NewsletterState state) {
        this.jid = jid;
        this.requestor = requestor;
        this.state = state;
    }

    public static Optional<NewsletterStateResponse> ofJson(String json) {
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

        var response = data.getJSONObject("xwa2_notify_newsletter_on_state_change");
        if(response == null) {
            return Optional.empty();
        }

        var id = response.getString("id");
        if(id == null) {
            return Optional.empty();
        }

        var jid = Jid.of(id);
        var requestor = response.getBooleanValue("is_requestor", false);
        var stateObject = response.getJSONObject("state");
        var state = stateObject != null ? NewsletterState.ofJson(stateObject).orElse(null) : null;
        var result = new NewsletterStateResponse(jid, requestor, state);
        return Optional.of(result);
    }

    public Jid jid() {
        return jid;
    }

    public boolean requestor() {
        return requestor;
    }

    public Optional<NewsletterState> state() {
        return Optional.ofNullable(state);
    }
}