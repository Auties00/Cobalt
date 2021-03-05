package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.JsonResponseModel;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Jacksonized
public record GroupModificationResponse(@JsonProperty("gid") @Nullable String jid, int status, List<ModifiedParticipantStatus> participants) implements JsonResponseModel {

}
