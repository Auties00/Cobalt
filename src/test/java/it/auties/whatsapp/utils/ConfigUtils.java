package it.auties.whatsapp.utils;

import lombok.experimental.UtilityClass;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@UtilityClass
public class ConfigUtils {
    private final String CONFIG_PATH = "/.test/config.properties";
    public Properties loadConfiguration() throws IOException {
        var config = loadConfigFile();
        return createProperties(config);
    }

    private Properties createProperties(Path config) throws IOException {
        var props = new Properties();
        props.load(Files.newBufferedReader(config));
        return props;
    }

    private Path loadConfigFile() throws IOException {
        var config = Path.of("./%s".formatted(CONFIG_PATH)).toRealPath();
        if(Files.notExists(config)){
            throw new FileNotFoundException("Before running any unit test please create a config file at %s".formatted(config));
        }

        return config;
    }
}
