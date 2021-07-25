package it.auties.whatsapp4j.binary.message;

import it.auties.whatsapp4j.binary.model.BinaryArray;
import it.auties.whatsapp4j.binary.model.BinaryMessage;
import lombok.NonNull;

public record MultiDeviceBinaryMessage(@NonNull BinaryArray message, int messageLength) implements BinaryMessage {
    public MultiDeviceBinaryMessage(@NonNull BinaryArray message){
        this(message.slice(3, 3 + message.cut(3).toInt()), message.cut(3).toInt());
        System.out.println("HEx: " + message.cut(3).toHex());
    }
}
