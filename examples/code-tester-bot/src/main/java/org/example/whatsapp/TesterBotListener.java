package org.example.whatsapp;

import it.auties.whatsapp4j.whatsapp.WhatsappAPI;
import it.auties.whatsapp4j.listener.RegisterListener;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.chat.Chat;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.message.standard.TextMessage;
import lombok.NonNull;

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
    public void onNewMessage(@NonNull Chat chat, @NonNull MessageInfo info) {
        var textMessage = info.container().textMessage();
        if(textMessage == null || !textMessage.text().contains("/java")){
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
                api.sendMessage(chat, new TextMessage("The provided code contains errors: %s".formatted(errorStream.toString())), info);
                return;
            }

            var process = Runtime.getRuntime().exec(new String[]{"java", "-cp", directory.toString(), "Test"});
            var result = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.joining("\n"));
            api.sendMessage(chat, new TextMessage("Code compiled successfully: %n%s".formatted(result)), info);
        }catch (IOException ex){
            api.sendMessage(chat, new TextMessage("An IOException occurred while running the provided code: %n%s".formatted(ex.getMessage())), info);
        }
    }
}
