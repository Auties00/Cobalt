package it.auties.whatsapp.model.poll;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.protobuf.base.ProtobufType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
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
public class PollVoteMessage
        implements ProtobufMessage {
    @ProtobufProperty(implementation = byte[].class, index = 1, name = "selectedOptions", repeated = true, type = ProtobufType.BYTES)
    @Default
    private List<byte[]> selectedOptions = new ArrayList<>();

    public static class PollVoteMessageBuilder {
        public PollVoteMessageBuilder selectedOptions(List<byte[]> selectedOptions) {
            if (!selectedOptions$set) {
                this.selectedOptions$value = selectedOptions;
                this.selectedOptions$set = true;
                return this;
            }

            this.selectedOptions$value.addAll(selectedOptions);
            return this;
        }
    }
}
