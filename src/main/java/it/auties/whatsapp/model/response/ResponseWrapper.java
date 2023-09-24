package it.auties.whatsapp.model.response;

public sealed interface ResponseWrapper permits ContactStatusResponse, HasWhatsappResponse, WebVersionResponse {

}
