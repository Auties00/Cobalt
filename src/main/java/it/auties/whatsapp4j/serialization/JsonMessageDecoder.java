package it.auties.whatsapp4j.serialization;

import it.auties.whatsapp4j.response.model.common.Response;
import lombok.NonNull;
import jakarta.websocket.Decoder.Text;

public class JsonMessageDecoder implements Text<Response<?>> {
    @Override
    public Response<?> decode(@NonNull String msg) {
        return Response.fromTaggedResponse(msg);
    }

    @Override
    public boolean willDecode(@NonNull String str) {
        return true;
    }
}
