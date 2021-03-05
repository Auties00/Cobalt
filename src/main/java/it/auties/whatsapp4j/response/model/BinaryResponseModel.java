package it.auties.whatsapp4j.response.model;

import it.auties.whatsapp4j.response.model.ResponseModel;
import it.auties.whatsapp4j.model.WhatsappNode;
import org.jetbrains.annotations.NotNull;

public interface BinaryResponseModel extends ResponseModel {
    void populateWithData(@NotNull WhatsappNode node);
}
