package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SubscribedNewslettersResponse {
    private final List<Newsletter> newsletters;

    private SubscribedNewslettersResponse(List<Newsletter> newsletters) {
        this.newsletters = newsletters;
    }

    public static Optional<SubscribedNewslettersResponse> ofJson(String json) {
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

        var newsletters = new ArrayList<Newsletter>(data.size());
        for(var key : data.keySet()) {
            var object = data.getJSONObject(key);
            if(object != null) {
                Newsletter.ofJson(object)
                        .ifPresent(newsletters::add);
            }
        }
        var result = new SubscribedNewslettersResponse(newsletters);
        return Optional.of(result);
    }

    public List<Newsletter> newsletters() {
        return Collections.unmodifiableList(newsletters);
    }
}