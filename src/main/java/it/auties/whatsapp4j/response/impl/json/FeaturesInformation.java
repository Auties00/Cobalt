package it.auties.whatsapp4j.response.impl.json;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record FeaturesInformation(boolean url, String flags) {
}
