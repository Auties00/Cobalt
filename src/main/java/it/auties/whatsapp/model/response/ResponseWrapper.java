package it.auties.whatsapp.model.response;

import it.auties.whatsapp.model.request.Node;

public sealed interface ResponseWrapper permits HasWhatsappResponse, ContactStatusResponse {
    Node response();
}
