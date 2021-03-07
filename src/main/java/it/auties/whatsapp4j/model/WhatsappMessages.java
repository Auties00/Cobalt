package it.auties.whatsapp4j.model;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * This class is an implementation of ArrayList used to store the {@link WhatsappMessage} in a {@link WhatsappChat}
 * The messages stored in this collection are guaranteed to be stored chronologically
 * This contract can be technically broken using reflection to access the array associated with this collection, though, obviously, it's not advisable
 */
@NoArgsConstructor
public class WhatsappMessages extends ArrayList<WhatsappMessage> {
    /**
     * The default comparator used to sort the entries in this collection
     */
    private static final Comparator<WhatsappMessage> ENTRY_COMPARATOR = Comparator.comparingLong(message -> message.info().getMessageTimestamp());


    /**
     * Constructs a new instance of WhatsappMessages from a WhatsappMessage
     * @param message the first non null entry to add to this collection
     */
    public WhatsappMessages(@NotNull WhatsappMessage message) {
        add(message);
    }

    /**
     * Adds {@param message} to this collection in the right position in order to respect the contract explained previously
     * @param message the non null message to add to this collection
     * @return true if {@param message} was added successfully
     */
    @Override
    public boolean add(@NotNull WhatsappMessage message) {
        var initialSize = size();
        var insertionPoint = Collections.binarySearch(this, message, ENTRY_COMPARATOR);
        super.add(insertionPoint > -1 ? insertionPoint : -insertionPoint - 1, message);
        return size() != initialSize;
    }

    /**
     * Adds {@param message} if no other entry in this collection has an id that matches the one of {@param message}
     * Otherwise, it removes said entry and adds {@param message}
     * @param message the non null message to add to this collection
     * @return true if {@param message} was replaced
     */
    public boolean addOrReplace(@NotNull WhatsappMessage message){
        var result = contains(message);
        remove(message);
        add(message);
        return result;
    }

    /**
     * Adds each entry of {@param collection} if no other entry in this collection has an id that matches said entry's
     * Otherwise, it removes said entry and adds said entry
     * @param collection the collection to add to this collection
     */
    @Override
    public boolean addAll(Collection<? extends WhatsappMessage> collection) {
        return collection.stream().map(this::addOrReplace).reduce(true, (a, b) -> a && b);
    }

    /**
     * This method is not supported for this collection because of the contract previously explained
     *
     * @throws UnsupportedOperationException this exception will always be thrown
     */
    @Override
    public void add(int index, WhatsappMessage element) {
        throw new UnsupportedOperationException();
    }

    /**
     * This method is not supported for this collection because of the contract previously explained
     *
     * @throws UnsupportedOperationException this exception will always be thrown
     */
    @Override
    public boolean addAll(int index, Collection<? extends WhatsappMessage> c) {
        throw new UnsupportedOperationException();
    }
}