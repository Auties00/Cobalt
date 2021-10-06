package it.auties.whatsapp4j.binary;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.binary.BinaryMessage;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Value
@Accessors(fluent = true)
public class StandardMessage extends BinaryMessage {
    @NonNull String tag;
    @NonNull BinaryArray message;
    public StandardMessage(@NonNull BinaryArray array) {
        super(array);
        var splitter = array.indexOf(',')
                .orElseThrow(() -> new IllegalArgumentException("Cannot parse %s as a standard message".formatted(array.toHex())));
        this.tag = array.cut(splitter).toString();
        this.message = array.slice(splitter);
    }
}
