package it.auties.whatsapp.registration.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

record WebVersionResponse(@JsonProperty("isBroken") boolean broken,
                                 @JsonProperty("isBelowSoft") boolean outdatedSoft,
                                 @JsonProperty("isBelowHard") boolean outdatedHard,
                                 @JsonProperty("hardUpdateTime") long outdatedUpdateTime,
                                 @JsonProperty("beta") String beta,
                                 @JsonProperty("currentVersion") String currentVersion) {

}
