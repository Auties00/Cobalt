package com.github.auties00.cobalt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Properties;

/**
 * Test-harness configuration source that exposes typed test settings, reading them from a local
 * {@code .test/config.properties} file when run on a developer machine and from environment
 * variables when run under GitHub Actions.
 */
public sealed abstract class Config permits Config.Local, Config.GithubActions {
    private static final String GITHUB_ACTIONS = "GITHUB_ACTIONS";

    protected final Properties properties;

    protected Config(Properties properties) {
        this.properties = properties;
    }

    public static Config loadConfig() throws IOException {
        return Boolean.parseBoolean(System.getenv(GITHUB_ACTIONS))
                ? new GithubActions()
                : new Local();
    }

    public Optional<String> getString(String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }

    public OptionalLong getLong(String key) {
        var value = properties.getProperty(key);
        return value != null ? OptionalLong.of(Long.parseLong(value)) : OptionalLong.empty();
    }

    public OptionalDouble getDouble(String key) {
        var value = properties.getProperty(key);
        return value != null ? OptionalDouble.of(Double.parseDouble(value)) : OptionalDouble.empty();
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    private static final class Local extends Config {
        private static final String CONFIG_PATH = "/.test/config.properties";

        private Local() throws IOException {
            var config = Path.of("./" + CONFIG_PATH).toAbsolutePath();
            if (Files.notExists(config)) {
                throw new FileNotFoundException("Before running any unit test please create a config file at %s".formatted(config));
            }
            var props = new Properties();
            props.load(Files.newBufferedReader(config));
            super(props);
        }
    }

    private static final class GithubActions extends Config {
        private GithubActions() {
            var props = new Properties();
            System.getenv().forEach((key, value) -> props.setProperty(key.toLowerCase(), value));
            super(props);
        }
    }
}
