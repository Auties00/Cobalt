package it.auties.whatsapp4j.test.utils;

import it.auties.whatsapp4j.utils.internal.Validate;
import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@UtilityClass
public class ConfigUtils {
    private final String CONFIG_PATH = "/.test/config.properties";
    public Properties loadConfiguration() throws IOException {
        var config = loadConfigFile();
        return createProperties(config);
    }

    private Properties createProperties(File config) throws IOException {
        var props = new Properties();
        props.load(new FileReader(config));
        return props;
    }

    private File loadConfigFile() throws IOException {
        var config = new File(Path.of(".").toRealPath().toFile(), CONFIG_PATH);
        Validate.isTrue(config.exists(), "Before running any unit test please create a config file at %s", config.getPath(), FileNotFoundException.class);
        return config;
    }
}
