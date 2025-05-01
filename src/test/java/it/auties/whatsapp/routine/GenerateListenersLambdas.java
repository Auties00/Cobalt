package it.auties.whatsapp.routine;

import it.auties.whatsapp.api.Listener;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GenerateListenersLambdas {
    public static void main(String[] args) {
        for(var method : Listener.class.getMethods()) {
            var params = method.getParameters();
            var methodName = "add%sListener".formatted(method.getName().substring(2));
            var signature = "ListenerConsumer." + switch (params.length) {
                case 0 -> "Empty consumer";
                case 1 -> "Unary<%s> consumer".formatted(params[0].getType().getSimpleName());
                case 2 -> "Binary<%s, %s> consumer".formatted(params[0].getType().getSimpleName(), params[1].getType().getSimpleName());
                case 3 -> "Ternary<%s, %s, %s> consumer".formatted(params[0].getType().getSimpleName(), params[1].getType().getSimpleName(), params[2].getType().getSimpleName());
                default -> throw new IllegalStateException("Unexpected value: " + params.length);
            };
            var body = """
                    addListener(new Listener() {
                        @Override
                        public void %s(%s) {
                            %s
                        }
                      });""".formatted(
                            method.getName(),
                    Arrays.stream(method.getParameters())
                            .map(entry -> entry.getType().getSimpleName() + " " + entry.getName())
                            .collect(Collectors.joining(", ")),
                    "consumer.accept(%s);".formatted(
                            Arrays.stream(method.getParameters())
                                    .map(Parameter::getName)
                                    .collect(Collectors.joining(", "))
                    )
            );
            System.out.printf("""
                    public Whatsapp %s(%s) {
                     %s
                     return this;
                    }%n""", methodName, signature, body);
            System.out.println();
        }
    }
}
