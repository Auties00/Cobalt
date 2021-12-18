package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class MessageKey {
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int index;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] cipherKey;

    @JsonProperty("3")
    @JsonPropertyDescription("bytes")
    private byte[] macKey;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] iv;
}
