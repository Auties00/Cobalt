package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AppVersionResponse(@JsonProperty("isBroken") boolean broken,
                                 @JsonProperty("isBelowSoft") boolean outdatedSoft,
                                 @JsonProperty("isBelowHard") boolean outdatedHard,
                                 @JsonProperty("hardUpdateTime") long outdatedUpdateTime,
                                 @JsonProperty("beta") String beta,
                                 @JsonProperty("currentVersion") String currentVersion)
    implements ResponseWrapper {

}
