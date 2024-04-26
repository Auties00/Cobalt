package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WhatsappIosMetrics(List<String> exposure, Metrics metrics) {
    public record Metrics(
            @JsonProperty("expid_c")
            boolean expId,
            @JsonProperty("fdid_c")
            boolean fdid,
            @JsonProperty("rc_c")
            boolean rcC,
            @JsonProperty("expid_md")
            long expIdMd,
            @JsonProperty("expid_cd")
            long expIdCd
    ) {

    }
}
