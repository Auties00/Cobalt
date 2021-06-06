package org.example.whatsapp;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappTextMessage;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.UUID;
import java.util.stream.Collectors;

@RegisterListener
public record TesterBotListener(WhatsappAPI api) implements WhatsappListener {
    private static final String JAVA_CODE_TEMPLATE =
            """
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

    @Override
    public void onNewMessageReceived(Chat chat, WhatsappMessage message) {
        if(!(message instanceof WhatsappTextMessage textMessage)){
            return;
        }

        if(!textMessage.text().contains("/java")){
            return;
        }

        try {
            var text = textMessage.text().replace("/java", "");
            var javaCode = JAVA_CODE_TEMPLATE.formatted(text);

            var directory = Files.createTempDirectory(UUID.randomUUID().toString());
            var file = Files.createTempFile(directory, "Test", ".java");
            Files.write(file, javaCode.getBytes());

            var errorStream = new ByteArrayOutputStream(4096);
            var compilationResult = COMPILER.run(null, null, errorStream, "-d", directory.toString(), file.toString());
            if (compilationResult != 0) {
                api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "The code provided contains errors: %s".formatted(errorStream.toString()), textMessage));
                return;
            }

            var process = Runtime.getRuntime().exec(new String[]{"java", "-cp", directory.toString(), "Test"});
            var result = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.joining("\n"));
            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "Code compiled successfully: %n%s".formatted(result), textMessage));
        }catch (IOException ex){
            api.sendMessage(WhatsappTextMessage.newTextMessage(chat, "An IOException occurred while running the provided code: %n%s".formatted(ex.getMessage()), textMessage));
        }
    }
}
