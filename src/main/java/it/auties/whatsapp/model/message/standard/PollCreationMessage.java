package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.crypto.Sha256;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.message.model.MessageCategory;
import it.auties.whatsapp.model.message.model.MessageType;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.SelectedPollOption;
import it.auties.whatsapp.util.Bytes;

import java.util.*;


/**
 * A model class that represents a message holding a poll inside
 */
@ProtobufMessage(name = "Message.PollCreationMessage")
public final class PollCreationMessage implements ContextualMessage<PollCreationMessage> {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    private byte[] encryptionKey;
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    private final String title;
    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    private final List<PollOption> selectableOptions;
    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    private final int selectableOptionsCount;
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    private ContextInfo contextInfo;
    @ProtobufProperty(index = 999, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final Map<String, PollOption> selectableOptionsMap;
    @ProtobufProperty(index = 1000, type = ProtobufType.MESSAGE)
    final List<SelectedPollOption> selectedOptions;

    public PollCreationMessage(byte[] encryptionKey, String title, List<PollOption> selectableOptions, int selectableOptionsCount, ContextInfo contextInfo, Map<String, PollOption> selectableOptionsMap, List<SelectedPollOption> selectedOptions) {
        this.encryptionKey = encryptionKey;
        this.title = title;
        this.selectableOptions = selectableOptions;
        this.selectableOptionsCount = selectableOptionsCount;
        this.contextInfo = contextInfo;
        this.selectableOptionsMap = selectableOptionsMap;
        this.selectedOptions = selectedOptions;
    }

    /**
     * Constructs a new builder to create a PollCreationMessage The newsletters can be later sent using
     * {@link Whatsapp#sendMessage(ChatMessageInfo)}
     *
     * @param title             the non-null title of the poll
     * @param selectableOptions the null-null non-empty options of the poll
     * @return a non-null new message
     */
    @ProtobufBuilder(className = "PollCreationMessageSimpleBuilder")
    static PollCreationMessage simpleBuilder(String title, List<PollOption> selectableOptions) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }
        if (selectableOptions.size() <= 1) {
            throw new IllegalArgumentException("Options must have at least two entries");
        }
        var result = new PollCreationMessageBuilder()
                .encryptionKey(Bytes.random(32))
                .title(title)
                .selectableOptions(selectableOptions)
                .selectableOptionsCount(selectableOptions.size())
                .build();
        for (var entry : result.selectableOptions()) {
            var sha256 = HexFormat.of().formatHex(Sha256.calculate(entry.name()));
            result.addSelectableOption(sha256, entry);
        }
        return result;
    }

    /**
     * Returns an unmodifiable list of the options that a contact voted in this poll
     *
     * @param voter the non-null contact that voted in this poll
     * @return a non-null unmodifiable map
     */
    public Collection<SelectedPollOption> getSelectedOptions(JidProvider voter) {
        return selectedOptions.stream()
                .filter(entry -> Objects.equals(entry.jid(), voter.toJid()))
                .toList();
    }

    public void addSelectedOptions(JidProvider voter, Collection<PollOption> voted) {
        for (var entry : voted) {
            var selectedPollOption = new SelectedPollOption(voter.toJid(), entry.name());
            selectedOptions.add(selectedPollOption);
        }
    }

    public void addSelectableOption(String hash, PollOption option) {
        selectableOptionsMap.put(hash, option);
    }

    public Optional<PollOption> getSelectableOption(String hash) {
        return Optional.ofNullable(selectableOptionsMap.get(hash));
    }

    @Override
    public MessageType type() {
        return MessageType.POLL_CREATION;
    }

    @Override
    public MessageCategory category() {
        return MessageCategory.STANDARD;
    }

    public String title() {
        return title;
    }

    public List<PollOption> selectableOptions() {
        return selectableOptions;
    }

    public int selectableOptionsCount() {
        return selectableOptionsCount;
    }

    public Optional<byte[]> encryptionKey() {
        return Optional.ofNullable(encryptionKey);
    }

    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    @Override
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    @Override
    public PollCreationMessage setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
        return this;
    }
}