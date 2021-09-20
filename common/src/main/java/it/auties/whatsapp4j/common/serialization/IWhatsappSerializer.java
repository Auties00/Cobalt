package it.auties.whatsapp4j.common.serialization;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.request.AbstractBinaryRequest;
import it.auties.whatsapp4j.common.response.BinaryResponse;
import lombok.NonNull;

import java.nio.ByteBuffer;

public interface IWhatsappSerializer {
    @NonNull ByteBuffer serialize(@NonNull AbstractBinaryRequest<?> input);
    @NonNull BinaryResponse deserialize(@NonNull BinaryArray input);
}
