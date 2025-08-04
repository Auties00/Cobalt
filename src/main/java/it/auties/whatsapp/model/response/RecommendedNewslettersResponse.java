package it.auties.whatsapp.model.response;

import com.alibaba.fastjson2.JSON;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class RecommendedNewslettersResponse {
    private final List<Newsletter> newsletters;

    private RecommendedNewslettersResponse(List<Newsletter> newsletters) {
        this.newsletters = newsletters;
    }

    public static Optional<RecommendedNewslettersResponse> of(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var newsletters = new ArrayList<Newsletter>();
        for(var key : jsonObject.sequencedKeySet()) {
            if(!key.startsWith("xwa2_newsletter_")) {
                continue;
            }

            if(jsonObject.isArray(key)) {
                var newsletterJsonArray = jsonObject.getJSONArray(key);
                for(var i = 0; i < newsletterJsonArray.size(); i++) {
                    var newsletterJsonObject = newsletterJsonArray.getJSONObject(i);
                    Newsletter.ofJson(newsletterJsonObject)
                            .ifPresent(newsletters::add);
                }
            } else {
                var newsletterJsonObject = jsonObject.getJSONObject(key);
                Newsletter.ofJson(newsletterJsonObject)
                        .ifPresent(newsletters::add);
            }
        }
        var result = new RecommendedNewslettersResponse(newsletters);
        return Optional.of(result);
    }

    public List<Newsletter> newsletters() {
        return Collections.unmodifiableList(newsletters);
    }
}