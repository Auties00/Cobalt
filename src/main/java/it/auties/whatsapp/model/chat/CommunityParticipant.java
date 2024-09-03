package it.auties.whatsapp.model.chat;

import it.auties.whatsapp.model.jid.Jid;

/**
 * A model class that represents the participant of a community
 *
 * @param jid the jid of the user
 */
public record CommunityParticipant(Jid jid) implements ChatParticipant {

}
