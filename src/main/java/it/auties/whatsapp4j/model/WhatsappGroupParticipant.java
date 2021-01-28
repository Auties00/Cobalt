package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@RecordBuilder
@ToString
public record WhatsappGroupParticipant(@NotNull WhatsappContact contact, boolean isAdmin, boolean isSuperAdmin) {
}
