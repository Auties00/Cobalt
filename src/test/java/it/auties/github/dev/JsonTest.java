package it.auties.github.dev;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import lombok.*;
import lombok.experimental.Accessors;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

public class JsonTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new Jdk8Module());

    public static class Carrot extends Consumable {
        public Carrot() {
            super(100);
        }
    }

    public static class Water extends Consumable {
        public Water() {
            super(50);
        }
    }

    @AllArgsConstructor
    @Data
    @Accessors(fluent = true)
    public abstract static class Consumable {
        public int health;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class Wrapper {
        @JsonProperty("consumable")
        @JsonAlias({"carrot", "water"})
        @JsonTypeInfo(use = NAME,include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name="carrot", value = Carrot.class),
                @JsonSubTypes.Type(name="water", value = Water.class),
        })
        private Consumable consumable;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class AnotherWrapper {
        @JsonProperty("wrapped")
        private Wrapper wrapped;
    }


    @SneakyThrows
    public static void main(String[] args) {
        // {"carrot":{"JsonTest$Carrot":{"health":100}}}
        var json = """
                {"carrot":{"health":100}}
                """;

        System.out.println(OBJECT_MAPPER.readValue(json, Wrapper.class));
    }
}
