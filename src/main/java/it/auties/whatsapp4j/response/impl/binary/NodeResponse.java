package it.auties.whatsapp4j.response.impl.binary;

import it.auties.whatsapp4j.response.model.binary.BinaryResponseModel;
import it.auties.whatsapp4j.model.WhatsappNode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@NoArgsConstructor
@ToString
public class NodeResponse implements BinaryResponseModel {
    private @Nullable WhatsappNode node;

    @Override
    public void populateWithData(@NotNull WhatsappNode node) {
        this.node = node;
    }

    public @NotNull WhatsappNode node() {
        return Objects.requireNonNull(node);
    }
}
