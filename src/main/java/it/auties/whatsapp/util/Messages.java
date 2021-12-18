package it.auties.whatsapp.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import it.auties.whatsapp.protobuf.chat.Chat;
import it.auties.whatsapp.protobuf.info.MessageInfo;
import it.auties.whatsapp.protobuf.message.model.Message;
import it.auties.whatsapp.protobuf.sync.HistorySyncMsg;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.*;

/**
 * This class is an implementation of ArrayList used to store the {@link Message} in a {@link Chat}.
 * The messages stored in this collection are guaranteed to be stored chronologically.
 * This contract can be technically broken using reflection to access the array associated with this collection, though, obviously, it's not advisable.
 */
@NoArgsConstructor
public class Messages extends ArrayList<MessageInfo> {
    /**
     * The default comparator used to sort the entries in this collection
     */
    private static final Comparator<MessageInfo> ENTRY_COMPARATOR = Comparator.comparingLong(MessageInfo::timestamp);

    /**
     * Constructs a new instance of Messages from a list of HistorySyncMsg
     *
     * @param data the non-null list of HistorySyncMsg to use as a model
     */
    @JsonCreator
    public Messages(@NonNull List<HistorySyncMsg> data) {
        super();
        data.forEach(sync -> add(sync.message()));
    }

    /**
     * Constructs a new instance of Messages from a WebMessageInfo
     *
     * @param message the first non-null entry to add to this collection
     */
    public Messages(@NonNull MessageInfo message) {
        super();
        add(message);
    }

    /**
     * Adds {@code message} to this collection in the right position in order to respect the contract explained previously
     *
     * @param message the non-null message to add to this collection
     * @return true if {@code message} was added successfully
     */
    @Override
    public boolean add(@NonNull MessageInfo message) {
        var initialSize = size();
        var insertionPoint = Collections.binarySearch(this, message, ENTRY_COMPARATOR);
        super.add(insertionPoint > -1 ? insertionPoint : -insertionPoint - 1, message);
        return size() != initialSize;
    }

    /**
     * Adds {@code message} if no other entry in this collection has an jid that matches the one of {@code message}.
     * Otherwise, it removes said entry and adds {@code message}.
     *
     * @param message the non-null message to add to this collection
     * @return true if {@code message} was replaced
     */
    public boolean addOrReplace(@NonNull MessageInfo message) {
        var result = remove(message);
        add(message);
        return result;
    }

    /**
     * Adds each entry of {@code collection} if no other entry in this collection has an jid that matches said entry's.
     * Otherwise, it removes said entry and adds said entry.
     *
     * @param collection the collection to add to this collection
     */
    @Override
    public boolean addAll(Collection<? extends MessageInfo> collection) {
        return collection.stream()
                .map(this::addOrReplace)
                .reduce(true, (first, second) -> first && second);
    }

    /**
     * This method is not supported for this collection because of the contract previously explained
     *
     * @throws UnsupportedOperationException this exception will always be thrown
     */
    @Override
    public void add(int index, MessageInfo element) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported for this collection because of the contract previously explained
     *
     * @throws UnsupportedOperationException this exception will always be thrown
     */
    @Override
    public boolean addAll(int index, Collection<? extends MessageInfo> c) {
        throw new UnsupportedOperationException();
    }
}