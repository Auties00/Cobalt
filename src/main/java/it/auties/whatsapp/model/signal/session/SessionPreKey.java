package it.auties.whatsapp.model.signal.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record SessionPreKey(@JsonProperty("id") int preKeyId, @JsonProperty("key") byte @NonNull [] baseKey,
                            @JsonProperty("signed_id") int signedPreKeyId) {

}