package it.auties.whatsapp4j.response.impl.json;

import lombok.extern.jackson.Jacksonized;

@Jacksonized
public record GroupParticipant(String jid, boolean isAdmin, boolean isSuperAdmin) {

}