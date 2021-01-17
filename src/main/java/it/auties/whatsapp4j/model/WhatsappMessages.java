package it.auties.whatsapp4j.model;

import it.auties.whatsapp4j.constant.ProtoBuf;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WhatsappMessages {
    private final List<ProtoBuf.WebMessageInfo> befores;
    private final List<ProtoBuf.WebMessageInfo> lasts;
    public WhatsappMessages(){
        this(new ArrayList<>(), new ArrayList<>());
    }

    public void addBefore(@NotNull ProtoBuf.WebMessageInfo before){
        befores.add(before);
    }

    public void addLast(@NotNull ProtoBuf.WebMessageInfo last){
        lasts.add(last);
    }

    public int size(){
        return befores.size() + lasts.size();
    }

    public List<ProtoBuf.WebMessageInfo> toList(){
        return List.of(befores, lasts).stream().flatMap(List::stream).collect(Collectors.toUnmodifiableList());
    }

    public Stream<ProtoBuf.WebMessageInfo> stream(){
        return toList().stream();
    }
}
