package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Jacksonized
public record GroupMetadataResponse(@JsonProperty("id") String jid,
                                    @JsonProperty("owner") String founder,
                                    @JsonProperty("creation") int foundationTimestamp, String subject,
                                    @JsonProperty("subjectTime") int lastSubjectUpdateTimestamp,
                                    @JsonProperty("subjectOwner") String lastSubjectUpdateJid,
                                    @JsonProperty("restrict") boolean onlyAdminsCanChangeSettings,
                                    @JsonProperty("announce") boolean onlyAdminsCanWriteMessages,
                                    @JsonProperty("desc") String description,
                                    @JsonProperty("descId") String descriptionMessageId,
                                    @JsonProperty("descTime") int lastDescriptionUpdateTimestamp,
                                    @JsonProperty("descOwner") String lastDescriptionUpdateJid,
                                    List<GroupParticipant> participants) implements JsonResponseModel {

}