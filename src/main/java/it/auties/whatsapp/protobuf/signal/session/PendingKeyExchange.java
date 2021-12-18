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
    @JsonProperty("1")
    @JsonPropertyDescription("uint32")
    private int sequence;

    @JsonProperty("2")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKey;

    @JsonProperty("3")
    @JsonPropertyDescription("bytes")
    private byte[] localBaseKeyPrivate;

    @JsonProperty("4")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKey;

    @JsonProperty("5")
    @JsonPropertyDescription("bytes")
    private byte[] localRatchetKeyPrivate;

    @JsonProperty("7")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKey;

    @JsonProperty("8")
    @JsonPropertyDescription("bytes")
    private byte[] localIdentityKeyPrivate;
}
