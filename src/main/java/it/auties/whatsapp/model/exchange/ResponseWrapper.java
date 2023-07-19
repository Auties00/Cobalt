package it.auties.whatsapp.model.exchange;

public sealed interface ResponseWrapper permits WebVersionResponse, ContactStatusResponse, HasWhatsappResponse {

}
