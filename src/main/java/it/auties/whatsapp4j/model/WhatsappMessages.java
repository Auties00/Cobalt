package it.auties.whatsapp4j.model;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.TreeSet;

public class WhatsappMessages extends TreeSet<WhatsappMessage> {
    private static final Comparator<? super WhatsappMessage> ENTRY_COMPARATOR = (first, second) -> Long.compareUnsigned(first.info().getMessageTimestamp(), second.info().getMessageTimestamp());
    public WhatsappMessages(){
        super(ENTRY_COMPARATOR);
    }

    public WhatsappMessages(@NotNull WhatsappMessage message){
        super(ENTRY_COMPARATOR);
        add(message);
    }
}
