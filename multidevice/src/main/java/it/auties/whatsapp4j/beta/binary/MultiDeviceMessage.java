package it.auties.whatsapp4j.beta.binary;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.binary.BinaryMessage;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Value
@Accessors(fluent = true)
public class MultiDeviceMessage extends BinaryMessage {
    @NonNull BinaryArray decoded;
    @NonNull int length;
    public MultiDeviceMessage(@NonNull BinaryArray array) {
        super(array);
        this.length = array.cut(3).toInt();
        this.decoded = array.slice(3, 3 + length);
    }
}
