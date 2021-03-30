package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.api.WhatsappAPI;
import it.auties.whatsapp4j.manager.WhatsappDataManager;
import it.auties.whatsapp4j.model.WhatsappProtobuf.WebMessageInfo;
import it.auties.whatsapp4j.utils.Validate;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents a WhatsappMessage.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 */
@Data
@Accessors(fluent = true)
@ToString
public abstract sealed class WhatsappMessage permits WhatsappUserMessage, WhatsappServerMessage {
    /**
     * A singleton instance of {@link WhatsappDataManager}
     */
    protected static final WhatsappDataManager MANAGER = WhatsappDataManager.singletonInstance();

    /**
     * The raw Protobuf object associated with this message
     */
    protected @NotNull WebMessageInfo info;

    /**
     * Constructs a WhatsappUserMessage from a raw protobuf object if the condition is met
     *
     * @param info the raw protobuf to wrap
     * @param condition the condition to meet
     */
    public WhatsappMessage(@NotNull WhatsappProtobuf.WebMessageInfo info, boolean condition) {
        Validate.isTrue(condition, "WhatsappAPI: Cannot construct WhatsappMessage as the condition to build this object wasn't met");
        this.info = info;
    }

    /**
     * Returns a non null unique identifier for this object
     *
     * @return a non null String
     */
    public @NotNull String id() {
        return info.getKey().getId();
    }

    /**
     * Returns a non null {@link ZonedDateTime} representing the time that this message was sent
     *
     * @return a non null {@link ZonedDateTime}
     */
    public @NotNull ZonedDateTime timestamp() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(info.getMessageTimestamp()), ZoneId.systemDefault());
    }

    /**
     * Returns whether this message was sent by a user or by whatsapp
     *
     * @return true if the message was sent by a user
     */
    public boolean isUserMessage(){
        return this instanceof WhatsappUserMessage;
    }

    /**
     * Returns the ContextInfo of this message if available
     *
     * @return a non empty optional if this message has a context
     */
    public abstract @NotNull Optional<WhatsappProtobuf.ContextInfo> contextInfo();

    /**
     * Checks if this object and {@code other} are equal
     *
     * @return true if {@code other} is an instance of {@link WhatsappMessage} and if their unique ids({@link WhatsappMessage#id()}) are equal
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof WhatsappMessage that && Objects.equals(id(), that.id());
    }
}
