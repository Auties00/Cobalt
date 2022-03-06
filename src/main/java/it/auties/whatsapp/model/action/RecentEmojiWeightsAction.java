package it.auties.whatsapp.model.action;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.whatsapp.model.sync.RecentEmojiWeight;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class RecentEmojiWeightsAction implements Action {
    @JsonProperty("1")
    @JsonPropertyDescription("RecentEmojiWeight")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<RecentEmojiWeight> weights;

    @Override
    public String indexName() {
        return "unknown";
    }
}
