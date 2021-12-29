package it.auties.whatsapp.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import it.auties.whatsapp.crypto.SignalHelper;

import java.io.IOException;

public class SignalKeyDeserializer extends JsonDeserializer<byte[]> {
    @Override
    public byte[] deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return SignalHelper.removeKeyHeader(jsonParser.getBinaryValue());
    }
}
