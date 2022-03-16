package it.auties.whatsapp.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import it.auties.whatsapp.util.Preferences;

import java.util.LinkedList;
import java.util.Objects;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers.
 * It provides an easy way to store IDs and serialize said class.
 */
public sealed interface WhatsappController permits WhatsappStore, WhatsappKeys {
    /**
     * The stored preferences for this class
     */
    Preferences SESSIONS = Preferences.of("sessions.json");

    /**
     * Saves the ID of the controller in a list of known IDs
     *
     * @param id the id to save
     * @return the id that was saved
     */
    static int saveId(int id){
        var knownIds = knownIds();
        if(knownIds.contains(id)){
            return id;
        }

        knownIds.add(id);
        SESSIONS.writeJsonAsync(knownIds);
        return id;
    }

    /**
     * Returns all the known IDs
     *
     * @return a non-null list
     */
    static LinkedList<Integer> knownIds() {
        return Objects.requireNonNullElseGet(SESSIONS.readJson(new TypeReference<>() {}),
                LinkedList::new);
    }


    /**
     * Saves this object as a JSON
     *
     * @param async whether to perform the write operation asynchronously or not
     */
    void save(boolean async);
}
