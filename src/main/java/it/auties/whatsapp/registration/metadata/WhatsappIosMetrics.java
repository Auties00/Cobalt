package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WhatsappIosMetrics(List<String> exposure, Metrics metrics) {
    public record Metrics(
            @JsonProperty("expid_c")
            Boolean expId,
            @JsonProperty("fdid_c")
            Boolean fdid,
            @JsonProperty("rc_c")
            Boolean rc,
            @JsonProperty("expid_md")
            Long expIdMd,
            @JsonProperty("expid_cd")
            Long expIdCd
    ) {

    }
}
