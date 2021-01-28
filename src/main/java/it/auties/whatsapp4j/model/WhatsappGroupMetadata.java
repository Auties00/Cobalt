package it.auties.whatsapp4j.model;

import io.soabase.recordbuilder.core.RecordBuilder;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RecordBuilder
@ToString
public record WhatsappGroupMetadata(@NotNull String id, @NotNull String owner, @NotNull String subject, @NotNull WhatsappGroupParticipant participants, long creation, @Nullable String description, @Nullable String descriptionOwner, @Nullable String descriptionId, boolean onlyAdminsCanChangeSettings, boolean onlyAdminsCanMessage) {

}
