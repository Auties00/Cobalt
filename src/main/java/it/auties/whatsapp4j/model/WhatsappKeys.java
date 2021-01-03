package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.utils.BytesArray;

public record WhatsappKeys(BytesArray encKey, BytesArray macKey) {
}
