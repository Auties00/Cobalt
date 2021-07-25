package it.auties.whatsapp4j.serialization;

import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.binary.model.BinaryMessage;
import it.auties.whatsapp4j.request.model.BinaryRequest;
import it.auties.whatsapp4j.response.model.binary.BinaryResponse;
import lombok.NonNull;

import java.nio.ByteBuffer;

public interface WhatsappSerializer {
    @NonNull ByteBuffer serialize(@NonNull BinaryRequest<?> input);
    @NonNull BinaryResponse deserialize(@NonNull BinaryArray input);
}
