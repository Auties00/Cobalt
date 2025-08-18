package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.Optional;

public final class NewsletterResponse {
    private final Newsletter newsletter;

    private NewsletterResponse(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public static Optional<NewsletterResponse> ofJson(byte[] json) {
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

        var dataKeys = data.sequencedKeySet();
        if(dataKeys.isEmpty()) {
            return Optional.empty();
        }

        var newsletter = Newsletter.ofJson(data.getJSONObject(dataKeys.getFirst()));
        if(newsletter.isEmpty()) {
            return Optional.empty();
        }

        var result = new NewsletterResponse(newsletter.get());
        return Optional.of(result);
    }

    public Newsletter newsletter() {
        return newsletter;
    }
}