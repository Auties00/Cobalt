package org.example.whatsapp;

import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.standard.TextMessage;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// This is the main class of our bot
public class TesterBot {
    private static final String JAVA_CODE_TEMPLATE = """
                    import java.util.*;
                    import java.util.stream.*;
                    import java.util.function.*;
                    import java.lang.*;
                    import java.nio.*;
                    import java.io.*;
                    import java.net.http.*;
                    
                    public class Test {
                        public static void main(String[] args){
                            %s
                        }
                    }
            """;
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();

    public static void main(String... args) throws ExecutionException, InterruptedException {
        // Create a new instance of WhatsappAPI
        Whatsapp.lastConnection()
                .addLoggedInListener(() -> System.out.println("Connected!"))
                .addNewMessageListener(TesterBot::onNewMessage)
                .connect()
                .get();
    }

    private static void onNewMessage(Whatsapp api, MessageInfo info) {
        if (!(info.message().content() instanceof TextMessage textMessage)) {
            return;
        }

        try {
            var text = textMessage.text()
                    .replace("/java", "");
            var javaCode = JAVA_CODE_TEMPLATE.formatted(text);

            var directory = Files.createTempDirectory(UUID.randomUUID()
                    .toString());
            var file = Files.createTempFile(directory, "Test", ".java");
            Files.write(file, javaCode.getBytes());

            var errorStream = new ByteArrayOutputStream(4096);
            var compilationResult = COMPILER.run(null, null, errorStream, "-d", directory.toString(), file.toString());
            if (compilationResult != 0) {
                api.sendMessage(info.chatJid(),
                        "The provided code contains errors: %s".formatted(errorStream.toString()), info);
                return;
            }

            var process = Runtime.getRuntime()
                    .exec(new String[]{"java", "-cp", directory.toString(), "Test"});
            var result = new BufferedReader(new InputStreamReader(process.getInputStream())).lines()
                    .collect(Collectors.joining("\n"));
            api.sendMessage(info.chatJid(), "Code compiled successfully: %n%s".formatted(result), info);
        } catch (IOException ex) {
            api.sendMessage(info.chatJid(),
                    "An IOException occurred while running the provided code: %n%s".formatted(ex.getMessage()), info);
        }
    }
}