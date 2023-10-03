package it.auties.whatsapp.model.call;

import it.auties.whatsapp.model.jid.Jid;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;

public record Call(@NonNull Jid chat, @NonNull Jid caller, @NonNull String id, @NonNull ZonedDateTime time,
                   boolean video, @NonNull CallStatus status, boolean offline) {
}