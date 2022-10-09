package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public class MessageReceipt implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING, implementation = ContactJid.class)
    @NonNull
    private String userJid;

    @ProtobufProperty(index = 2, type = INT64)
    private long receiptTimestamp;

    @ProtobufProperty(index = 3, type = INT64)
    private long readTimestamp;

    @ProtobufProperty(index = 4, type = INT64)
    private long playedTimestamp;

    @ProtobufProperty(index = 5, type = STRING, repeated = true)
    private List<String> pendingDeviceJid;

    @ProtobufProperty(index = 6, type = STRING, repeated = true)
    private List<String> deliveredDeviceJid;

    public static class MessageReceiptBuilder {
        public MessageReceiptBuilder pendingDeviceJid(List<String> pendingDeviceJid) {
            if (this.pendingDeviceJid == null)
                this.pendingDeviceJid = new ArrayList<>();
            this.pendingDeviceJid.addAll(pendingDeviceJid);
            return this;
        }

        public MessageReceiptBuilder deliveredDeviceJid(List<String> deliveredDeviceJid) {
            if (this.deliveredDeviceJid == null)
                this.deliveredDeviceJid = new ArrayList<>();
            this.deliveredDeviceJid.addAll(deliveredDeviceJid);
            return this;
        }
    }
}
