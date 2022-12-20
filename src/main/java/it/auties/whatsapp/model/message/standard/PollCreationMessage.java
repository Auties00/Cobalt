package it.auties.whatsapp.model.message.standard;

import it.auties.bytes.Bytes;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.model.contact.ContactJidProvider;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.poll.PollOptionName;
import it.auties.whatsapp.util.KeyHelper;
import lombok.*;
import lombok.Builder.Default;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.*;

@AllArgsConstructor
@Data
@Jacksonized
@SuperBuilder
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ProtobufName("PollCreationMessage")
public final class PollCreationMessage
        extends ContextualMessage {
    @ProtobufProperty(index = 2, name = "name", type = ProtobufType.STRING)
    private String title;

    @ProtobufProperty(implementation = PollOptionName.class, index = 3, name = "options", repeated = true, type = ProtobufType.MESSAGE)
    private List<PollOptionName> selectableOptions;

    @ProtobufProperty(index = 4, name = "selectableOptionsCount", type = ProtobufType.UINT32)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private int selectableOptionsCount;

    @Default
    private Map<String, PollOptionName> selectableOptionsHashesMap = new HashMap<>();

    @Default
    private Map<ContactJid, List<PollOptionName>> selectedOptionsMap = new HashMap<>();

    @ProtobufProperty(index = 1, name = "encKey", type = ProtobufType.BYTES)
    @Default
    private byte[] encryptionKey = KeyHelper.senderKey();

    @ProtobufProperty(index = 5, name = "contextInfo", type = ProtobufType.MESSAGE)
    @Default
    private ContextInfo contextInfo = new ContextInfo();

    @Override
    public MessageType type() {
        return MessageType.POLL_CREATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public List<PollOptionName> getSelectedOptions(@NonNull ContactJidProvider provider) {
        return Objects.requireNonNullElseGet(selectedOptionsMap.get(provider.toJid()), List::of);
    }

    public static abstract class PollCreationMessageBuilder<C extends PollCreationMessage, B extends PollCreationMessageBuilder<C, B>>
            extends ContextualMessageBuilder<C, B> {
        public PollCreationMessageBuilder<C, B> selectableOptions(List<PollOptionName> selectableOptions) {
            if (this.selectableOptions == null) {
                this.selectableOptions = new ArrayList<>();
            }

            selectableOptionsHashesMap$set = true;
            if (selectableOptionsHashesMap$value == null) {
                selectableOptionsHashesMap$value = new HashMap<>();
            }

            selectableOptions.forEach(entry -> {
                var sha256 = Bytes.of(Sha256.calculate(entry.name()))
                        .toHex();
                selectableOptionsHashesMap$value.put(sha256, entry);
            });
            this.selectableOptions.addAll(selectableOptions);
            this.selectableOptionsCount = selectableOptions.size();
            return this;
        }
    }
}
