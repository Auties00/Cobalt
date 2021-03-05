package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

@Jacksonized
public record ModifiedParticipantStatus(String jid, int status) {
    @JsonCreator
    public static ModifiedParticipantStatus forJson(@NotNull Map<String, Map<String, Integer>> json) {
        var jid = new ArrayList<>(json.keySet()).get(0);
        return new ModifiedParticipantStatus(jid, json.get(jid).get("code"));
    }
}
