package it.auties.whatsapp4j.socket;

import it.auties.whatsapp4j.response.model.common.Response;
import jakarta.validation.constraints.NotNull;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder.Text;

public class WhatsappJsonMessageDecoder implements Text<Response<?>> {
    @Override
    public Response<?> decode(@NotNull String msg) throws DecodeException {
        return Response.fromTaggedResponse(msg);
    }

    @Override
    public boolean willDecode(@NotNull String str) {
        return true;
    }
}
