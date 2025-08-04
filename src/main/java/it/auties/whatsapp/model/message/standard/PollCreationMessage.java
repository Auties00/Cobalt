package it.auties.whatsapp.model.message.standard;

import it.auties.protobuf.annotation.ProtobufBuilder;
import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.api.Whatsapp;
import it.auties.whatsapp.model.info.ChatMessageInfo;
import it.auties.whatsapp.model.info.ContextInfo;
import it.auties.whatsapp.model.jid.JidProvider;
import it.auties.whatsapp.model.message.model.ContextualMessage;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.SelectedPollOption;
import it.auties.whatsapp.model.poll.SelectedPollOptionBuilder;
import it.auties.whatsapp.util.Bytes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * A model class that represents a message holding a poll inside
 */
@ProtobufMessage(name = "Message.PollCreationMessage")
public final class PollCreationMessage implements ContextualMessage {
    @ProtobufProperty(index = 1, type = ProtobufType.BYTES)
    byte[] encryptionKey;

    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 3, type = ProtobufType.MESSAGE)
    final List<PollOption> selectableOptions;

    @ProtobufProperty(index = 4, type = ProtobufType.UINT32)
    final int selectableOptionsCount;

    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;

    @ProtobufProperty(index = 999, type = ProtobufType.MAP, mapKeyType = ProtobufType.STRING, mapValueType = ProtobufType.MESSAGE)
    final Map<String, PollOption> selectableOptionsMap;

    @ProtobufProperty(index = 1000, type = ProtobufType.MESSAGE)
    final List<SelectedPollOption> selectedOptions;

    PollCreationMessage(byte[] encryptionKey, String title, List<PollOption> selectableOptions, int selectableOptionsCount, ContextInfo contextInfo, Map<String, PollOption> selectableOptionsMap, List<SelectedPollOption> selectedOptions) {
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
            try {
                var digest = MessageDigest.getInstance("SHA-256");
                var data = entry.name().getBytes();
                var hash = HexFormat.of().formatHex(digest.digest(data));
                result.addSelectableOption(hash, entry);
            } catch (NoSuchAlgorithmException exception) {
                throw new UnsupportedOperationException("Missing sha256 implementation");
            }
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
            var selectedPollOption = new SelectedPollOptionBuilder()
                    .jid(voter.toJid())
                    .name(entry.name())
                    .build();
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
    public Type type() {
        return Type.POLL_CREATION;
    }

    @Override
    public Category category() {
        return Category.STANDARD;
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
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }
}