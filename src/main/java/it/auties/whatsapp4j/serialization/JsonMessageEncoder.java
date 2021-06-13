package it.auties.whatsapp4j.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.auties.whatsapp4j.request.model.JsonRequest;
import lombok.NonNull;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder.Text;

public class JsonMessageEncoder implements Text<JsonRequest<?>> {
    private static final ObjectWriter JACKSON = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Override
    public @NonNull String encode(@NonNull JsonRequest<?> request) throws EncodeException {
        try {
            return "%s,%s".formatted(request.tag(), JACKSON.writeValueAsString(request.buildBody()));
        }catch (JsonProcessingException ex){
            throw new EncodeException(request, "WhatsappJsonMessageEncoder: Cannot encode message", ex);
        }
    }
}
