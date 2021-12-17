package it.auties.github;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class RemoveDuplicates {
    @SneakyThrows
    public static void main(String[] args) {
        var input = Path.of(args[0]);
        var output = Path.of(args[1]);

        var inputs = Files.walk(input)
                .filter(s -> s.toString().endsWith(".java"))
                .map(Path::getFileName)
                .toList();

        Files.walk(output)
                .filter(s -> s.toString().endsWith(".java"))
                .filter()
    }
}
