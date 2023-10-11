package it.auties.whatsapp.model.call;

import it.auties.whatsapp.model.jid.Jid;

import java.time.ZonedDateTime;

public record Call(Jid chat, Jid caller, String id, ZonedDateTime time,
                   boolean video, CallStatus status, boolean offline) {
}