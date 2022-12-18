package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Data
@Accessors(fluent = true)
@Jacksonized
@Builder
@ProtobufName("PollVoteMessage")
public class PollVoteMessage implements ProtobufMessage {
    @ProtobufProperty(implementation = byte[].class, index = 1, name = "selectedOptions", repeated = true, type = ProtobufType.BYTES)
    private List<byte[]> selectedOptions;

    public static class PollVoteMessageBuilder {
        public PollVoteMessageBuilder selectedOptions(List<byte[]> selectedOptions) {
            if (this.selectedOptions == null)
                this.selectedOptions = new ArrayList<>();
            this.selectedOptions.addAll(selectedOptions);
            return this;
        }
    }
}
