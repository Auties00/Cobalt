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
    @JsonProperty(value = "1")
    @JsonPropertyDescription("uint32")
    private int index;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] cipherKey;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("bytes")
    private byte[] macKey;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("bytes")
    private byte[] iv;
}
