package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Jacksonized
public record GroupModificationResponse(@JsonProperty("gid") @Nullable String jid, int status, List<ModifiedParticipantStatus> participants) implements JsonResponseModel {

}
