package it.auties.whatsapp4j.response.model.shared;

import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import it.auties.whatsapp4j.response.model.json.JsonResponseModel;

/**
 * An interface that can be implemented to signal that a class may represent a piece of data sent by WhatsappWeb's WebSocket
 * This class only allows two types of models:<br/>
 * {@link BinaryResponseModel} - a model built from a WhatsappNode <br/>
 * {@link JsonResponseModel} - a model built from a JSON String<br/>
 */
public sealed interface ResponseModel permits BinaryResponseModel, JsonResponseModel {

}
