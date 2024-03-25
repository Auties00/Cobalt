package it.auties.whatsapp.routine;

import it.auties.whatsapp.util.Medias;
import it.auties.whatsapp.util.Specification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Scanner;

public class GenerateAndroidServerApk {
    public static void main(String[] args) throws IOException, InterruptedException {
        var scanner = new Scanner(System.in);
        var javaHome = Objects.requireNonNullElseGet(System.getenv("JAVA_HOME"), () -> {
            System.out.println("Enter your JAVA_HOME: ");
            return scanner.nextLine().trim();
        });
        System.out.println("Select if the server apk is business or personal:\n(1) Business (2) Personal");
        var business = switch (scanner.nextInt()) {
            case 1 -> true;
            case 2 -> false;
            default -> throw new IllegalStateException("Unexpected value: " + scanner.nextInt());
        };
        var localPath = Path.of("support/android/%s".formatted(business ? "business" : "personal")).toAbsolutePath();
        if(Files.notExists(localPath)) {
            System.err.println("Invalid android support app path: " + localPath);
            return;
        }

        // Define an output path
        var outputPath = Path.of(".test/").toAbsolutePath();

        // Build the android app
        System.out.println("Building server apk...");
        var buildProcess = new ProcessBuilder()
                .command("./gradlew", "assembleRelease")
                .directory(localPath.toFile())
                .inheritIO();
        buildProcess.environment().put("JAVA_HOME", javaHome);
        var buildExitCode = buildProcess.start().waitFor();
        if(buildExitCode != 0) {
            System.err.println("Invalid build exit code: " + buildExitCode);
            return;
        }
        System.out.println("Built server apk!");

        // Download the official business apk
        System.out.println("Downloading whatsapp business apk...");
        var tempBusinessApk = outputPath.resolve("input/whatsapp_%s.apk".formatted(business ? "business" : "personal"));
        if(Files.notExists(tempBusinessApk)) {
            var businessApkData = Medias.downloadAsync(business ? Specification.Whatsapp.MOBILE_BUSINESS_ANDROID_URL : Specification.Whatsapp.MOBILE_ANDROID_URL, (String) null).join();
            Files.createDirectories(tempBusinessApk.getParent());
            Files.write(tempBusinessApk, businessApkData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        System.out.println("Downloaded whatsapp business apk!");

        // Sign our server apk
        try {
            System.out.println("Signing server apk...");
            var outputApk = outputPath.resolve("output/whatsapp_%s_server.apk".formatted(business ? "business" : "personal"));
            Files.createDirectories(outputApk.getParent());
            var signProcess = new ProcessBuilder()
                    .command("apksigcopier", "copy", tempBusinessApk.toString(), "./app/build/outputs/apk/release/app-release-unsigned.apk", outputApk.toString())
                    .directory(localPath.toFile())
                    .inheritIO()
                    .start();
            var signExitCode = signProcess.waitFor();
            if(signExitCode != 0) {
                System.err.println("Sign build exit code: " + signExitCode);
                return;
            }

            System.out.println("Signed server apk at " + outputApk);
        }catch (IOException exception) {
            System.err.println("Cannot sign server apk: " + exception.getMessage());
            System.err.println("You might need to download apksigcopier: https://github.com/obfusk/apksigcopier");
        }
    }
}
