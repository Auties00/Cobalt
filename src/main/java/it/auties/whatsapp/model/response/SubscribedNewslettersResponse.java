package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Json
public final class SubscribedNewslettersResponse {
    private static final SubscribedNewslettersResponse EMPTY = new SubscribedNewslettersResponse(List.of());

    private final List<Newsletter> newsletters;

    private SubscribedNewslettersResponse(List<Newsletter> newsletters) {
        this.newsletters = newsletters;
    }

    @Json.Creator
    static SubscribedNewslettersResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        var newsletterJsonType = Jsonb.builder()
                .build()
                .type(Newsletter.class);
        var newsletters = new ArrayList<Newsletter>(data.size());
        for(var entry : data.entrySet()) {
            if(entry instanceof Map<?,?> newsletterSource) {
                var newsletter = newsletterJsonType.fromObject(newsletterSource);
                newsletters.add(newsletter);
            }
        }
        return new SubscribedNewslettersResponse(newsletters);
    }

    public List<Newsletter> newsletters() {
        return Collections.unmodifiableList(newsletters);
    }
}