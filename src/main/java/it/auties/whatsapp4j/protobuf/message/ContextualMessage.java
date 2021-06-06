package it.auties.whatsapp4j.protobuf.message;

import it.auties.whatsapp4j.protobuf.info.ContextInfo;
import jakarta.validation.constraints.NotNull;

public interface ContextualMessage extends Message {
    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    @NotNull ContextInfo contextInfo();
}
