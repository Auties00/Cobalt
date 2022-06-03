package it.auties.whatsapp.test;

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

        try(var inputWalker = Files.walk(input)){
            var inputs = inputWalker.filter(file -> !file.toString().contains(output.toString()) && file.toString().endsWith(".java"))
                    .map(Path::getFileName)
                    .toList();
            try(var outputWalker = Files.walk(output)){
                outputWalker.filter(file -> inputs.contains(file.getFileName()))
                        .forEach(RemoveDuplicates::deleteAndPrint);
            }
        }
    }

    private static void deleteAndPrint(Path file) {
        try {
            Files.delete(file);
        }catch (IOException exception){
            throw new UncheckedIOException(exception);
        }finally {
            System.out.printf("Removed: %s%n", file);
        }
    }
}