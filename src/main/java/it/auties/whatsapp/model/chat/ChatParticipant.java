package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.model.jid.Jid;

public sealed interface ChatParticipant permits GroupParticipant, CommunityParticipant {
    Jid jid();
}
