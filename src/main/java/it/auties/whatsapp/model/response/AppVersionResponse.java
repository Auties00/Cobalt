package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.signal.auth.Version;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

@Jacksonized
@Builder
public record AppVersionResponse(WebVersion web, WebVersion windows, WebVersion macos, MobileVersion android,
                                 MobileVersion ios,
                                 @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "CET") @JsonProperty("lastverification") ZonedDateTime timestamp) implements ResponseWrapper{
    public record MobileVersion(Version messenger, Version business) {
    }

    public record WebVersion(Version latest, Version beta) {
    }
}