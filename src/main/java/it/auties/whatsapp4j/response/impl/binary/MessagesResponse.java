package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.model.WhatsappProtobuf;
import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import it.auties.whatsapp4j.model.WhatsappMessage;
import it.auties.whatsapp4j.model.WhatsappMessages;
import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@NoArgsConstructor
@ToString
public class MessagesResponse implements BinaryResponseModel {
    private @Nullable WhatsappMessages messages;

    @Override
    public void populateWithData(@NotNull WhatsappNode node) {
        var content = node.content();
        if(!(content instanceof List<?> listContent)) {
            throw new IllegalArgumentException("WhatsappAPI: Cannot parse %s: content should be a list".formatted(node));
        }

        this.messages = WhatsappNode.fromGenericList(listContent)
                .stream()
                .filter(childNode -> childNode.description().equals("message"))
                .map(WhatsappNode::content)
                .filter(childContent -> childContent instanceof WhatsappProtobuf.WebMessageInfo)
                .map(WhatsappProtobuf.WebMessageInfo.class::cast)
                .map(WhatsappMessage::new)
                .collect(Collectors.toCollection(WhatsappMessages::new));
    }

    public @NotNull WhatsappMessages messages() {
        return Objects.requireNonNull(messages, "WhatsappAPI: Cannot access null content of MessagesResponse");
    }
}
