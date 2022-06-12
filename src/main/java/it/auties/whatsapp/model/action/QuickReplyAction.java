package it.auties.whatsapp.model.action;

import it.auties.protobuf.api.model.ProtobufProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.api.model.ProtobufProperty.Type.*;

@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
public final class QuickReplyAction implements Action {
    @ProtobufProperty(index = 1, type = STRING)
    private String shortcut;

    @ProtobufProperty(index = 2, type = STRING)
    private String message;

    @ProtobufProperty(index = 3, type = STRING, repeated = true)
    private List<String> keywords;

    @ProtobufProperty(index = 4, type = INT32)
    private int count;

    @ProtobufProperty(index = 5, type = BOOLEAN)
    private boolean deleted;

    @Override
    public String indexName() {
        return "quick_reply";
    }

    public static class QuickReplyActionBuilder {
        public QuickReplyActionBuilder keywords(List<String> keywords) {
            if (this.keywords == null)
                this.keywords = new ArrayList<>();
            this.keywords.addAll(keywords);
            return this;
        }
    }
}
