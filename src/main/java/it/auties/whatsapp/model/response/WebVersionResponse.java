package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
public record WebVersionResponse(@JsonProperty("isBroken") boolean broken,
                                 @JsonProperty("isBelowSoft") boolean outdatedSoft,
                                 @JsonProperty("isBelowHard") boolean outdatedHard,
                                 @JsonProperty("hardUpdateTime") long outdatedUpdateTime,
                                 @JsonProperty("beta") String beta,
                                 @JsonProperty("currentVersion") String currentVersion) implements ResponseWrapper {

}
