package it.auties.whatsapp.util;

import it.auties.whatsapp.controller.Controller;
import it.auties.whatsapp.controller.ControllerProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultControllerProvider implements ControllerProvider {
    @Override
    public LinkedList<Integer> ids() {
        try (var walker = Files.walk(Preferences.home(), 1)
                .sorted(Comparator.comparing(this::getLastModifiedTime))) {
            return walker.map(this::parsePathAsId)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(LinkedList::new));
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot list known ids", exception);
        }
    }

    private FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot get last modification date", exception);
        }
    }

    private Optional<Integer> parsePathAsId(Path file) {
        try {
            return Optional.of(Integer.parseInt(file.getFileName()
                    .toString()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }


    @Override
    public void serialize(Controller<?> controller) {
        controller.preferences()
                .writeJson(controller, true);
    }
}
