package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.util.Preferences;

import java.util.LinkedList;
import java.util.Objects;

public sealed interface WhatsappController permits WhatsappStore, WhatsappKeys {
    /**
     * The stored preferences for this class
     */
    Preferences SESSIONS = Preferences.of("sessions.json");

    static int saveId(int id){
        var knownIds = knownIds();
        if(knownIds.contains(id)){
            return id;
        }

        knownIds.add(id);
        SESSIONS.writeJsonAsync(knownIds);
        return id;
    }

    static LinkedList<Integer> knownIds() {
        return Objects.requireNonNullElseGet(SESSIONS.readJson(new TypeReference<>() {}),
                LinkedList::new);
    }

    void save(boolean async);
}
