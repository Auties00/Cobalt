package it.auties.github;

import lombok.SneakyThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Remove duplicates for new protobuf
public class RemoveDuplicates {
    @SneakyThrows
    public static void main(String[] args) {
        var input = Path.of(args[0]);
        var output = Path.of(args[1]);

        var inputs = Files.walk(input)
                .filter(file -> !file.toString().contains(output.toString()) && file.toString().endsWith(".java"))
                .map(Path::getFileName)
                .toList();

        Files.walk(output)
                .filter(file -> inputs.contains(file.getFileName()))
                .forEach(RemoveDuplicates::deleteAndPrint);
    }

    private static void deleteAndPrint(Path file) {
        try {
            Files.delete(file);
        }catch (IOException exception){
            throw new UncheckedIOException(exception);
        }finally {
            System.out.println(file);
        }
    }
}