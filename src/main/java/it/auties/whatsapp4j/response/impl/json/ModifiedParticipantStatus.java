package it.auties.whatsapp4j.response.impl.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Map;

public record ModifiedParticipantStatus(String jid, int status) {
    @JsonCreator
    public ModifiedParticipantStatus(@NotNull Map<String, Map<String, Integer>> json) {
        this(new ArrayList<>(json.keySet()).get(0), json.get(new ArrayList<>(json.keySet()).get(0)).get("code"));
    }
}
