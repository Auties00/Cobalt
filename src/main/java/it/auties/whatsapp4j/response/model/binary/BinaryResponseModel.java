package it.auties.whatsapp4j.response.model.binary;

import it.auties.whatsapp4j.response.model.shared.ResponseModel;
import it.auties.whatsapp4j.model.WhatsappNode;
import org.jetbrains.annotations.NotNull;

public interface BinaryResponseModel extends ResponseModel {
    void populateWithData(@NotNull WhatsappNode node);
}
