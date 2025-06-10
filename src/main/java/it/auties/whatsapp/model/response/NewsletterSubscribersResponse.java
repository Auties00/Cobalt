package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;

import java.util.Optional;
import java.util.OptionalLong;

public final class NewsletterSubscribersResponse {
    private final Long subscribersCount;

    private NewsletterSubscribersResponse(Long subscribersCount) {
        this.subscribersCount = subscribersCount;
    }

    public static Optional<NewsletterSubscribersResponse> ofJson(String json) {
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

        var newsletter = data.getJSONObject("xwa2_newsletter");
        if(newsletter == null) {
            return Optional.empty();
        }

        var metadata = newsletter.getJSONObject("thread_metadata");
        if(metadata == null) {
            return Optional.empty();
        }

        var subscribersCount = metadata.getLong("subscribers_count");
        var result = new NewsletterSubscribersResponse(subscribersCount);
        return Optional.of(result);
    }

    public OptionalLong subscribersCount() {
        return subscribersCount == null ? OptionalLong.empty() : OptionalLong.of(subscribersCount);
    }
}