package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import it.auties.whatsapp4j.model.WhatsappChat;
import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@NoArgsConstructor
@ToString
public class ChatResponse implements BinaryResponseModel {
    private @Nullable WhatsappChat chat;

    @Override
    public void populateWithData(@NotNull WhatsappNode node) {
        var content = node.content();
        if(!(content instanceof List<?> listContent)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s: content should be a list".formatted(node));
        }

        this.chat = WhatsappNode.fromGenericList(listContent)
                .stream()
                .findFirst()
                .filter(childNode -> childNode.description().equals("chat"))
                .map(WhatsappNode::attrs)
                .map(WhatsappChat::fromAttributes)
                .orElse(null);
    }

    public @NotNull Optional<WhatsappChat> chat() {
        return Optional.ofNullable(chat);
    }
}
