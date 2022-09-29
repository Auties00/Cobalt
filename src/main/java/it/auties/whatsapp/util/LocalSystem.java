package it.auties.whatsapp.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class LocalSystem {
    private final Path DEFAULT_DIRECTORY = Path.of(System.getProperty("user.home") + "/.whatsappweb4j/");
    static {
        try {
            Files.createDirectories(DEFAULT_DIRECTORY);
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot create home directory", exception);
        }
    }

    public Path home() {
        return DEFAULT_DIRECTORY;
    }

    public Path of(String file){
        return DEFAULT_DIRECTORY.resolve(file);
    }
    
    public void delete(String folder){
        try {
            Files.deleteIfExists(of(folder));
        }catch (IOException exception){
            throw new UncheckedIOException("Cannot delete folder", exception);
        }
    }
}
