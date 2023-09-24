package it.auties.whatsapp.model.button.interactive;

import it.auties.protobuf.annotation.ProtobufMessageName;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufMessage;
import it.auties.protobuf.model.ProtobufType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * A model class that represents the body of a product
 */
@ProtobufMessageName("Message.InteractiveResponseMessage.Body")
public record InteractiveResponseBody(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        @NonNull
        String content
) implements ProtobufMessage {
        public static Optional<InteractiveResponseBody> ofNullable(@Nullable String content) {
                return Optional.ofNullable(content)
                        .map(InteractiveResponseBody::new);
        }
}