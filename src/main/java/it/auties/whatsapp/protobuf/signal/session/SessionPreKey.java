package it.auties.whatsapp.protobuf.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
public class SessionPreKey {
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int preKeyId;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] baseKey;

    @JsonProperty("3")
    @JsonPropertyDescription("int32")
    private int signedPreKeyId;
}