package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedMetadata;

import java.util.List;
import java.util.Optional;


/**
 * A model class that represents data about a button
 */
@ProtobufMessage(name = "MsgOpaqueData")
public record ButtonOpaqueData(
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        Optional<String> body,
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Optional<String> caption,
        @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
        double longitude,
        @ProtobufProperty(index = 7, type = ProtobufType.DOUBLE)
        double latitude,
        @ProtobufProperty(index = 8, type = ProtobufType.INT32)
        int paymentAmount1000,
        @ProtobufProperty(index = 9, type = ProtobufType.STRING)
        Optional<String> paymentNote,
        @ProtobufProperty(index = 10, type = ProtobufType.STRING)
        Optional<String> canonicalUrl,
        @ProtobufProperty(index = 11, type = ProtobufType.STRING)
        Optional<String> matchedText,
        @ProtobufProperty(index = 12, type = ProtobufType.STRING)
        Optional<String> title,
        @ProtobufProperty(index = 13, type = ProtobufType.STRING)
        Optional<String> description,
        @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
        boolean isLive,
        @ProtobufProperty(index = 14, type = ProtobufType.BYTES)
        Optional<byte[]> futureProofBuffer,
        @ProtobufProperty(index = 15, type = ProtobufType.STRING)
        Optional<String> clientUrl,
        @ProtobufProperty(index = 16, type = ProtobufType.STRING)
        Optional<String> loc,
        @ProtobufProperty(index = 17, type = ProtobufType.STRING)
        Optional<String> pollName,
        @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
        List<PollOption> pollOptions,
        @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
        int pollSelectableOptionsCount,
        @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
        Optional<byte[]> messageSecret,
        @ProtobufProperty(index = 51, type = ProtobufType.STRING)
        Optional<String> originalSelfAuthor,
        @ProtobufProperty(index = 22, type = ProtobufType.INT64)
        long senderTimestampMs,
        @ProtobufProperty(index = 23, type = ProtobufType.STRING)
        Optional<String> pollUpdateParentKey,
        @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
        Optional<PollUpdateEncryptedMetadata> encPollVote,
        @ProtobufProperty(index = 25, type = ProtobufType.STRING)
        Optional<String> encReactionTargetMessageKey,
        @ProtobufProperty(index = 26, type = ProtobufType.BYTES)
        Optional<byte[]> encReactionEncPayload,
        @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
        Optional<byte[]> encReactionEncIv
) {

}