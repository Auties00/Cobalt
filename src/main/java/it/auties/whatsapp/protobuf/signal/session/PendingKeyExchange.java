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
public class PendingKeyExchange {
    @JsonProperty(value = "1")
    @JsonPropertyDescription("uint32")
    private int sequence;

    @JsonProperty(value = "2")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKey;

    @JsonProperty(value = "3")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKeyPrivate;

    @JsonProperty(value = "4")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKey;

    @JsonProperty(value = "5")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKeyPrivate;

    @JsonProperty(value = "7")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKey;

    @JsonProperty(value = "8")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKeyPrivate;
}
