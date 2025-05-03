package it.auties.whatsapp.model.response;

import io.avaje.jsonb.Json;
import io.avaje.jsonb.Jsonb;
import it.auties.whatsapp.model.newsletter.Newsletter;

import java.util.*;

@Json
public final class RecommendedNewslettersResponse {
    private static final RecommendedNewslettersResponse EMPTY = new RecommendedNewslettersResponse(List.of());

    private final List<Newsletter> newsletters;

    private RecommendedNewslettersResponse(List<Newsletter> newsletters) {
        this.newsletters = newsletters;
    }

    @Json.Creator
    static RecommendedNewslettersResponse of(@Json.Unmapped Map<String, Object> json) {
        if(!(json.get("data") instanceof Map<?,?> data)) {
            return EMPTY;
        }

        for(var entry : data.entrySet()) {
            if (!(entry.getKey() instanceof String key) || !key.startsWith("xwa2_newsletter_")) {
                continue;
            }

            var newsletterJsonType = Jsonb.builder()
                    .build()
                    .type(Newsletter.class);
            var newsletters = switch (entry.getValue()) {
                case List<?> values -> {
                    var results = new ArrayList<Newsletter>(values.size());
                    for(var value : values) {
                        if(value instanceof Map<?,?> newsletterSource) {
                            var newsletter = newsletterJsonType.fromObject(newsletterSource);
                            results.add(newsletter);
                        }
                    }
                    yield results;
                }
                case Map<?,?> value -> {
                    var newsletter = newsletterJsonType.fromObject(value);
                    yield List.of(newsletter);
                }
                default -> List.<Newsletter>of();
            };
            return new RecommendedNewslettersResponse(newsletters);
        }

        return EMPTY;
    }

    public List<Newsletter> newsletters() {
        return Collections.unmodifiableList(newsletters);
    }
}