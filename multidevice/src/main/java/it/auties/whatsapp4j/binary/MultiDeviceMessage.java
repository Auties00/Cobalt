package it.auties.whatsapp4j.binary;

import it.auties.whatsapp4j.common.binary.BinaryArray;
import it.auties.whatsapp4j.common.binary.BinaryMessage;
import it.auties.whatsapp4j.common.utils.Validate;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

@EqualsAndHashCode(callSuper = true)
@Value
@Accessors(fluent = true)
public class MultiDeviceMessage extends BinaryMessage {
    @NonNull BinaryArray decoded;
    int length;
    public MultiDeviceMessage(@NonNull BinaryArray array) {
        super(Validate.isValid(array, array.size() > 4, "MultiDeviceMessage: Invalid message length(%s)", array.size()));
        this.length = array.cut(3).toInt();
        this.decoded = array.slice(3, 3 + length);
    }

    public MultiDeviceMessage(byte @NonNull [] array) {
        this(BinaryArray.forArray(array));
    }
}
