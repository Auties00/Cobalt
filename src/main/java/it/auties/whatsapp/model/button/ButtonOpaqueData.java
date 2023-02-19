package it.auties.whatsapp.model.button;

import it.auties.protobuf.base.ProtobufMessage;
import it.auties.protobuf.base.ProtobufName;
import it.auties.protobuf.base.ProtobufProperty;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedMetadata;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

import static it.auties.protobuf.base.ProtobufType.*;

/**
 * A model class that represents data about a button
 */
@AllArgsConstructor
@Data
@Builder
@Jacksonized
@Accessors(fluent = true)
@ProtobufName("MsgOpaqueData")
public class ButtonOpaqueData implements ProtobufMessage {
    /**
     * The body of the button
     */
    @ProtobufProperty(index = 1, type = STRING)
    private String body;

    /**
     * The caption of the button
     */
    @ProtobufProperty(index = 3, type = STRING)
    private String caption;

    /**
     * The longitude of the button
     */
    @ProtobufProperty(index = 5, type = DOUBLE)
    private double longitude;

    /**
     * The latitude of the button
     */
    @ProtobufProperty(index = 7, type = DOUBLE)
    private double latitude;

    /**
     * The payment amount of the button
     */
    @ProtobufProperty(index = 8, type = INT32)
    private int paymentAmount1000;

    /**
     * The note attached to the payment of the button
     */
    @ProtobufProperty(index = 9, type = STRING)
    private String paymentNote;

    /**
     * The canonical url of the button
     */
    @ProtobufProperty(index = 10, type = STRING)
    private String canonicalUrl;

    /**
     * The matched text of the button
     */
    @ProtobufProperty(index = 11, type = STRING)
    private String matchedText;

    /**
     * The title of the button
     */
    @ProtobufProperty(index = 12, type = STRING)
    private String title;

    /**
     * The description of the button
     */
    @ProtobufProperty(index = 13, type = STRING)
    private String description;

    @ProtobufProperty(index = 6, name = "isLive", type = BOOL)
    private Boolean isLive;

    @ProtobufProperty(index = 14, name = "futureproofBuffer", type = BYTES)
    private byte[] futureproofBuffer;

    @ProtobufProperty(index = 15, name = "clientUrl", type = STRING)
    private String clientUrl;

    @ProtobufProperty(index = 16, name = "loc", type = STRING)
    private String loc;

    @ProtobufProperty(index = 17, name = "pollName", type = STRING)
    private String pollName;

    @ProtobufProperty(implementation = PollOption.class, index = 18, name = "pollOptions", repeated = true, type = MESSAGE)
    private List<PollOption> pollOptions;

    @ProtobufProperty(index = 20, name = "pollSelectableOptionsCount", type = UINT32)
    private Integer pollSelectableOptionsCount;

    @ProtobufProperty(index = 21, name = "messageSecret", type = BYTES)
    private byte[] messageSecret;

    @ProtobufProperty(index = 51, name = "originalSelfAuthor", type = STRING)
    private String originalSelfAuthor;

    @ProtobufProperty(index = 22, name = "senderTimestampMs", type = INT64)
    private Long senderTimestampMs;

    @ProtobufProperty(index = 23, name = "pollUpdateParentKey", type = STRING)
    private String pollUpdateParentKey;

    @ProtobufProperty(index = 24, name = "encPollVote", type = MESSAGE)
    private PollUpdateEncryptedMetadata encPollVote;

    @ProtobufProperty(index = 25, name = "encReactionTargetMessageKey", type = STRING)
    private String encReactionTargetMessageKey;

    @ProtobufProperty(index = 26, name = "encReactionEncPayload", type = BYTES)
    private byte[] encReactionEncPayload;

    @ProtobufProperty(index = 27, name = "encReactionEncIv", type = BYTES)
    private byte[] encReactionEncIv;

    public static class ButtonOpaqueDataBuilder {
        public ButtonOpaqueData.ButtonOpaqueDataBuilder pollOptions(List<PollOption> pollOptions) {
            if (this.pollOptions == null) {
                this.pollOptions = new ArrayList<>();
            }
            this.pollOptions.addAll(pollOptions);
            return this;
        }
    }
}