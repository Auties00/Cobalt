package it.auties.github.dev;

import it.auties.whatsapp.util.JacksonProvider;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class JacksonTest implements JacksonProvider {
    @Test
    @SneakyThrows
    public void test(){
        record Example(Map<String, Inner> abc) {
            record Inner(String abc){

            }
        }

        var abc = new Example(Map.of("hello", new Example.Inner("hello")));
        var encoded = JACKSON.writeValueAsString(abc);
        System.out.println(encoded);
        var decoded = JACKSON.readValue(encoded, Example.class);
        System.out.println(decoded);
    }
}
