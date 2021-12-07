package it.auties.whatsapp.exchange;

import it.auties.whatsapp.protobuf.model.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
public abstract class Response {
    protected final Node source;

    public Response(){
        this(null);
    }
}
