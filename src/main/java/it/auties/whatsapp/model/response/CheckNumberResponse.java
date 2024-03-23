package it.auties.whatsapp.model.response;

public record CheckNumberResponse(boolean hasWhatsapp, boolean hasBan, RegistrationResponse metadata) {

}
