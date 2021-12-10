package it.auties.whatsapp.exchange;

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
