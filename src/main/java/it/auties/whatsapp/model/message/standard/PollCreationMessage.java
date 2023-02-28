package it.auties.whatsapp.model.message.standard;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.info.MessageInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.util.KeyHelper;
import it.auties.whatsapp.util.Validate;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents a message holding a poll inside
 */
@AllArgsConstructor
@Data
@Jacksonized
@SuperBuilder
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ProtobufName("PollCreationMessage")
public final class PollCreationMessage extends ContextualMessage {
    /**
     * The title of the poll
     */
    @ProtobufProperty(index = 2, name = "name", type = STRING)
    private String title;

    /**
     * A list of options that can be selected in the poll
     */
    @ProtobufProperty(implementation = PollOption.class, index = 3, name = "options", repeated = true, type = MESSAGE)
    private List<PollOption> selectableOptions;

    /**
     * Internal field required by the protobuf to count the number of selectable options
     */
    @ProtobufProperty(index = 4, name = "selectableOptionsCount", type = UINT32)
    private int selectableOptionsCount;

    /**
     * The SHA256 hashes of {@link PollCreationMessage#selectableOptions}
     */
    @Default
    private Map<String, PollOption> selectableOptionsHashesMap = new ConcurrentHashMap<>();

    /**
     * The toMap of the options selected by each person that can vote in this poll
     */
    @Default
    private Map<ContactJid, List<PollOption>> selectedOptionsMap = new ConcurrentHashMap<>();

    /**
     * The encryption key of this poll
     */
    @ProtobufProperty(index = 1, name = "encKey", type = BYTES)
    @Default
    private byte[] encryptionKey = KeyHelper.senderKey();

    /**
     * The context of this message
     */
    @ProtobufProperty(index = 5, name = "contextInfo", type = MESSAGE)
    @Default
    private ContextInfo contextInfo = new ContextInfo();

    /**
     * Constructs a new builder to create a PollCreationMessage The result can be later sent using
     * {@link Whatsapp#sendMessage(MessageInfo)}
     *
     * @param title             the non-null title of the poll
     * @param selectableOptions the null-null non-empty options of the poll
     * @return a non-null new message
     */
    @Builder(builderClassName = "SimplePollCreationMessageBuilder", builderMethodName = "simpleBuilder")
    private static PollCreationMessage customBuilder(@NonNull String title, @NonNull List<PollOption> selectableOptions) {
        Validate.isTrue(!selectableOptions.isEmpty(), "Options cannot be empty");
        return PollCreationMessage.builder().title(title).selectableOptions(selectableOptions).build();
    }

    /**
     * Returns an unmodifiable list of the options that a contact voted in this poll
     *
     * @param contact the non-null contact that voted in this poll
     * @return a non-null unmodifiable toMap
     */
    public List<PollOption> getSelectedOptions(@NonNull ContactJidProvider contact) {
        return Optional.of(selectedOptionsMap.get(contact.toJid()))
                .map(Collections::unmodifiableList)
                .orElseGet(List::of);
    }

    @Override
    public MessageType type() {
        return MessageType.POLL_CREATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public abstract static class PollCreationMessageBuilder<C extends PollCreationMessage, B extends PollCreationMessageBuilder<C, B>> extends ContextualMessageBuilder<C, B> {
        public PollCreationMessageBuilder<C, B> selectableOptions(List<PollOption> selectableOptions) {
            if (this.selectableOptions == null) {
                this.selectableOptions = new ArrayList<>();
            }
            selectableOptionsHashesMap$set = true;
            if (selectableOptionsHashesMap$value == null) {
                selectableOptionsHashesMap$value = new HashMap<>();
            }
            selectableOptions.forEach(entry -> {
                var sha256 = Bytes.of(Sha256.calculate(entry.name())).toHex();
                selectableOptionsHashesMap$value.put(sha256, entry);
            });
            this.selectableOptions.addAll(selectableOptions);
            this.selectableOptionsCount = selectableOptions.size();
            return this;
        }
    }
}