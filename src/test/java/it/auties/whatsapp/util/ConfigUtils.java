package it.auties.whatsapp.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConfigUtils {
    private static final String CONFIG_PATH = "/.test/config.properties";

    public static Properties loadConfiguration() throws IOException {
        var config = loadConfigFile();
        return createProperties(config);
    }

    private static Path loadConfigFile() throws IOException {
        var config = Path.of("./" + CONFIG_PATH).toAbsolutePath();
        if (Files.notExists(config)) {
            throw new FileNotFoundException("Before running any unit test please create a config file at %s".formatted(config));
        }
        return config;
    }

    private static Properties createProperties(Path config) throws IOException {
        var props = new Properties();
        props.load(Files.newBufferedReader(config));
        return props;
    }
}
