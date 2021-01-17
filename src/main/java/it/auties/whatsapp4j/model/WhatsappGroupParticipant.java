package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

public record WhatsappGroupParticipant(@NotNull WhatsappContact contact, boolean isAdmin, boolean isSuperAdmin) {
}
