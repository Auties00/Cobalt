package it.auties.whatsapp.model.call;

import it.auties.whatsapp.model.contact.ContactJid;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;

public record Call(@NonNull ContactJid chat, @NonNull ContactJid caller, @NonNull String id, @NonNull ZonedDateTime time,
                   boolean video, @NonNull CallStatus status, boolean offline) {
}