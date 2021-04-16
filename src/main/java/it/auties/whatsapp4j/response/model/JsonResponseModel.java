package it.auties.whatsapp4j.response.model;

/**
 * An interface to represent a class that may represent a JSON String sent by WhatsappWeb's WebSocket
 */
public interface JsonResponseModel<J extends JsonResponseModel<J>> extends ResponseModel<J> {
}
