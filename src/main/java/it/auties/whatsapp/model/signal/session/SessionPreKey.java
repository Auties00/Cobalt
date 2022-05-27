package it.auties.whatsapp.model.signal.session;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record SessionPreKey(int preKeyId, byte @NonNull [] baseKey, int signedKeyId) {
}