package it.auties.whatsapp.controller;

import it.auties.whatsapp.util.Preferences;
import lombok.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This interface represents is implemented by all WhatsappWeb4J's controllers.
 * It provides an easy way to store IDs and serialize said class.
 */
public sealed interface Controller permits Store, Keys {
    /**
     * Returns all the known IDs
     *
     * @return a non-null list
     */
    static LinkedList<Integer> knownIds() {
        try (var walker = Files.walk(Preferences.home(), 1)) {
            return walker.map(Controller::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
        }
    }

    private static Optional<Integer> parsePathAsId(Path file) {
        try {
            return Optional.of(Integer.parseInt(file.getFileName()
                    .toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Saves this object as a JSON
     *
     * @param preferences the non-null preferences
     * @param async       whether to perform the write operation asynchronously or not
     */
    void save(@NonNull Preferences preferences, boolean async);

    /**
     * Saves this object as a JSON
     *
     * @param path  the non-null path
     * @param async whether to perform the write operation asynchronously or not
     */
    void save(@NonNull Path path, boolean async);

    /**
     * Saves this object as a JSON
     *
     * @param async whether to perform the write operation asynchronously or not
     */
    void save(boolean async);
}
