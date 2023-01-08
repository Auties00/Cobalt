package it.auties.whatsapp.model.response;

public sealed interface ResponseWrapper
    permits AppVersionResponse, ContactStatusResponse, HasWhatsappResponse {

}
