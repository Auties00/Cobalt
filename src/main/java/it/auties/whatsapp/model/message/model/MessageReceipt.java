package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.auties.protobuf.base.ProtobufType.INT64;
import static it.auties.protobuf.base.ProtobufType.STRING;

/**
 * A model that represents the receipt for a message
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("UserReceipt")
public class MessageReceipt
        implements ProtobufMessage {
    @ProtobufProperty(index = 1, type = STRING, implementation = ContactJid.class)
    private ContactJid userJid;

    @ProtobufProperty(index = 2, type = INT64)
    private Long receiptTimestamp;

    @ProtobufProperty(index = 3, type = INT64)
    private Long readTimestamp;

    @ProtobufProperty(index = 4, type = INT64)
    private Long playedTimestamp;

    @ProtobufProperty(index = 5, type = STRING, repeated = true, implementation = ContactJid.class)
    @Default
    private List<ContactJid> pendingJids = new ArrayList<>();

    @ProtobufProperty(index = 6, type = STRING, repeated = true, implementation = ContactJid.class)
    @Default
    private List<ContactJid> readJids = new ArrayList<>();
   
    public static MessageReceipt of(){
        return MessageReceipt.builder()
                .build();
    }
    
    public Optional<ZonedDateTime> receiptTimestamp(){
        return Clock.parse(readTimestamp);
    }

    public Optional<ZonedDateTime> readTimestamp(){
        return Clock.parse(readTimestamp);
    }

    public Optional<ZonedDateTime> playedTimestamp(){
        return Clock.parse(playedTimestamp);
    }

    public static class MessageReceiptBuilder {
        public MessageReceiptBuilder pendingJids(List<ContactJid> pendingJids) {
            if (!this.pendingJids$set) {
                this.pendingJids$value = new ArrayList<>();
                this.pendingJids$set = true;
            }
            
            this.pendingJids$value.addAll(pendingJids);
            return this;
        }

        public MessageReceiptBuilder readJids(List<ContactJid> readJids) {
            if (!this.readJids$set) {
                this.readJids$value = new ArrayList<>();
                this.readJids$set = true;
            }
            
            this.readJids$value.addAll(readJids);
            return this;
        }
    }
}