package it.auties.whatsapp.routine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

public class GenerateAndroidServerApk {
    public static void main(String[] args) throws IOException, InterruptedException {
        var scanner = new Scanner(System.in);
        var javaHome = Objects.requireNonNullElseGet(System.getenv("JAVA_HOME"), () -> {
            System.out.println("Enter your JAVA_HOME: ");
            return scanner.nextLine().trim();
        });
        var androidHome = Objects.requireNonNullElseGet(System.getenv("ANDROID_HOME"), () -> {
            System.out.println("Enter your ANDROID_HOME: ");
            return scanner.nextLine().trim();
        });
        System.out.println("Select if the server apk is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        var appPath = Path.of("support/android/%s".formatted(business ? "business" : "personal")).toAbsolutePath();
        if(Files.notExists(appPath)) {
            System.err.println("Invalid android support app path: " + appPath);
            return;
        }

        // Define an output path
        var outputApk = Path.of(".bin/whatsapp_%s_server.apk".formatted(business ? "business" : "personal")).toAbsolutePath();
        Files.createDirectories(outputApk.getParent());

        // Build the android app
        System.out.println("Building server apk...");
        var buildProcess = new ProcessBuilder()
                .command("./gradlew", "assembleRelease")
                .directory(appPath.toFile())
                .inheritIO();
        buildProcess.environment().put("JAVA_HOME", javaHome);
        buildProcess.environment().put("ANDROID_HOME", androidHome);
        var buildExitCode = buildProcess.start().waitFor();
        if(buildExitCode != 0) {
            System.err.println("Invalid build exit code: " + buildExitCode);
            return;
        }
        var inputApk =  appPath.resolve("./app/build/outputs/apk/release/app-release-unsigned.apk");
        Files.move(inputApk, outputApk);
        System.out.println("Built server apk at " + outputApk);
    }
}
