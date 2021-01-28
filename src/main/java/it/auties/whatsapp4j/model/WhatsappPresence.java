package it.auties.whatsapp4j.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.constant.UserPresence;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhatsappPresence {
    @JsonProperty("id")
    private @NotNull String id;
    @JsonProperty("participant")
    private @Nullable String participant;
    @JsonProperty("type")
    private @NotNull UserPresence presence;
    @JsonProperty("t")
    private Long offsetFromLastSeen;
}
