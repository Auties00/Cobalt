package it.auties.whatsapp.model.button.base;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.model.poll.PollOption;
import it.auties.whatsapp.model.poll.PollUpdateEncryptedMetadata;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A model class that represents data about a button
 */
@ProtobufMessage(name = "MsgOpaqueData")
public final class ButtonOpaqueData {
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    final String body;

    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    final String caption;

    @ProtobufProperty(index = 5, type = ProtobufType.DOUBLE)
    final double longitude;

    @ProtobufProperty(index = 7, type = ProtobufType.DOUBLE)
    final double latitude;

    @ProtobufProperty(index = 8, type = ProtobufType.INT32)
    final int paymentAmount1000;

    @ProtobufProperty(index = 9, type = ProtobufType.STRING)
    final String paymentNote;

    @ProtobufProperty(index = 10, type = ProtobufType.STRING)
    final String canonicalUrl;

    @ProtobufProperty(index = 11, type = ProtobufType.STRING)
    final String matchedText;

    @ProtobufProperty(index = 12, type = ProtobufType.STRING)
    final String title;

    @ProtobufProperty(index = 13, type = ProtobufType.STRING)
    final String description;

    @ProtobufProperty(index = 6, type = ProtobufType.BOOL)
    final boolean live;

    @ProtobufProperty(index = 14, type = ProtobufType.BYTES)
    final byte[] futureProofBuffer;

    @ProtobufProperty(index = 15, type = ProtobufType.STRING)
    final String clientUrl;

    @ProtobufProperty(index = 16, type = ProtobufType.STRING)
    final String loc;

    @ProtobufProperty(index = 17, type = ProtobufType.STRING)
    final String pollName;

    @ProtobufProperty(index = 18, type = ProtobufType.MESSAGE)
    final List<PollOption> pollOptions;

    @ProtobufProperty(index = 20, type = ProtobufType.UINT32)
    final int pollSelectableOptionsCount;

    @ProtobufProperty(index = 21, type = ProtobufType.BYTES)
    final byte[] messageSecret;

    @ProtobufProperty(index = 51, type = ProtobufType.STRING)
    final String originalSelfAuthor;

    @ProtobufProperty(index = 22, type = ProtobufType.INT64)
    final long senderTimestampMilliseconds;

    @ProtobufProperty(index = 23, type = ProtobufType.STRING)
    final String pollUpdateParentKey;

    @ProtobufProperty(index = 24, type = ProtobufType.MESSAGE)
    final PollUpdateEncryptedMetadata encPollVote;

    @ProtobufProperty(index = 25, type = ProtobufType.STRING)
    final String encReactionTargetMessageKey;

    @ProtobufProperty(index = 26, type = ProtobufType.BYTES)
    final byte[] encReactionEncPayload;

    @ProtobufProperty(index = 27, type = ProtobufType.BYTES)
    final byte[] encReactionEncIv;

    ButtonOpaqueData(String body, String caption, double longitude, double latitude, int paymentAmount1000, String paymentNote, String canonicalUrl, String matchedText, String title, String description, boolean live, byte[] futureProofBuffer, String clientUrl, String loc, String pollName, List<PollOption> pollOptions, int pollSelectableOptionsCount, byte[] messageSecret, String originalSelfAuthor, long senderTimestampMilliseconds, String pollUpdateParentKey, PollUpdateEncryptedMetadata encPollVote, String encReactionTargetMessageKey, byte[] encReactionEncPayload, byte[] encReactionEncIv) {
        this.body = body;
        this.caption = caption;
        this.longitude = longitude;
        this.latitude = latitude;
        this.paymentAmount1000 = paymentAmount1000;
        this.paymentNote = paymentNote;
        this.canonicalUrl = canonicalUrl;
        this.matchedText = matchedText;
        this.title = title;
        this.description = description;
        this.live = live;
        this.futureProofBuffer = futureProofBuffer;
        this.clientUrl = clientUrl;
        this.loc = loc;
        this.pollName = pollName;
        this.pollOptions = Objects.requireNonNullElse(pollOptions, List.of());
        this.pollSelectableOptionsCount = pollSelectableOptionsCount;
        this.messageSecret = messageSecret;
        this.originalSelfAuthor = originalSelfAuthor;
        this.senderTimestampMilliseconds = senderTimestampMilliseconds;
        this.pollUpdateParentKey = pollUpdateParentKey;
        this.encPollVote = encPollVote;
        this.encReactionTargetMessageKey = encReactionTargetMessageKey;
        this.encReactionEncPayload = encReactionEncPayload;
        this.encReactionEncIv = encReactionEncIv;
    }

    public Optional<String> body() {
        return Optional.ofNullable(body);
    }

    public Optional<String> caption() {
        return Optional.ofNullable(caption);
    }

    public double longitude() {
        return longitude;
    }

    public double latitude() {
        return latitude;
    }

    public int paymentAmount1000() {
        return paymentAmount1000;
    }

    public Optional<String> paymentNote() {
        return Optional.ofNullable(paymentNote);
    }

    public Optional<String> canonicalUrl() {
        return Optional.ofNullable(canonicalUrl);
    }

    public Optional<String> matchedText() {
        return Optional.ofNullable(matchedText);
    }

    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    public boolean live() {
        return live;
    }

    public Optional<byte[]> futureProofBuffer() {
        return Optional.ofNullable(futureProofBuffer);
    }

    public Optional<String> clientUrl() {
        return Optional.ofNullable(clientUrl);
    }

    public Optional<String> loc() {
        return Optional.ofNullable(loc);
    }

    public Optional<String> pollName() {
        return Optional.ofNullable(pollName);
    }

    public List<PollOption> pollOptions() {
        return pollOptions;
    }

    public int pollSelectableOptionsCount() {
        return pollSelectableOptionsCount;
    }

    public Optional<byte[]> messageSecret() {
        return Optional.ofNullable(messageSecret);
    }

    public Optional<String> originalSelfAuthor() {
        return Optional.ofNullable(originalSelfAuthor);
    }

    public long senderTimestampMilliseconds() {
        return senderTimestampMilliseconds;
    }

    public Optional<ZonedDateTime> senderTimestamp() {
        return Clock.parseMilliseconds(senderTimestampMilliseconds);
    }

    public Optional<String> pollUpdateParentKey() {
        return Optional.ofNullable(pollUpdateParentKey);
    }

    public Optional<PollUpdateEncryptedMetadata> encPollVote() {
        return Optional.ofNullable(encPollVote);
    }

    public Optional<String> encReactionTargetMessageKey() {
        return Optional.ofNullable(encReactionTargetMessageKey);
    }

    public Optional<byte[]> encReactionEncPayload() {
        return Optional.ofNullable(encReactionEncPayload);
    }

    public Optional<byte[]> encReactionEncIv() {
        return Optional.ofNullable(encReactionEncIv);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ButtonOpaqueData that
                && longitude == that.longitude
                && latitude == that.latitude
                && paymentAmount1000 == that.paymentAmount1000
                && live == that.live
                && pollSelectableOptionsCount == that.pollSelectableOptionsCount
                && senderTimestampMilliseconds == that.senderTimestampMilliseconds
                && Objects.equals(body, that.body)
                && Objects.equals(caption, that.caption)
                && Objects.equals(paymentNote, that.paymentNote)
                && Objects.equals(canonicalUrl, that.canonicalUrl)
                && Objects.equals(matchedText, that.matchedText)
                && Objects.equals(title, that.title)
                && Objects.equals(description, that.description)
                && Arrays.equals(futureProofBuffer, that.futureProofBuffer)
                && Objects.equals(clientUrl, that.clientUrl)
                && Objects.equals(loc, that.loc)
                && Objects.equals(pollName, that.pollName)
                && Objects.equals(pollOptions, that.pollOptions)
                && Arrays.equals(messageSecret, that.messageSecret)
                && Objects.equals(originalSelfAuthor, that.originalSelfAuthor)
                && Objects.equals(pollUpdateParentKey, that.pollUpdateParentKey)
                && Objects.equals(encPollVote, that.encPollVote)
                && Objects.equals(encReactionTargetMessageKey, that.encReactionTargetMessageKey)
                && Arrays.equals(encReactionEncPayload, that.encReactionEncPayload)
                && Arrays.equals(encReactionEncIv, that.encReactionEncIv);
    }

    @Override
    public int hashCode() {
        return Objects.hash(body, caption, longitude, latitude, paymentAmount1000, paymentNote,
                canonicalUrl, matchedText, title, description, live, Arrays.hashCode(futureProofBuffer),
                clientUrl, loc, pollName, pollOptions, pollSelectableOptionsCount,
                Arrays.hashCode(messageSecret), originalSelfAuthor, senderTimestampMilliseconds,
                pollUpdateParentKey, encPollVote, encReactionTargetMessageKey,
                Arrays.hashCode(encReactionEncPayload), Arrays.hashCode(encReactionEncIv));
    }

    @Override
    public String toString() {
        return "ButtonOpaqueData[" +
                "body=" + body +
                ", caption=" + caption +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", paymentAmount1000=" + paymentAmount1000 +
                ", paymentNote=" + paymentNote +
                ", canonicalUrl=" + canonicalUrl +
                ", matchedText=" + matchedText +
                ", title=" + title +
                ", description=" + description +
                ", isLive=" + live +
                ", futureProofBuffer=" + Arrays.toString(futureProofBuffer) +
                ", clientUrl=" + clientUrl +
                ", loc=" + loc +
                ", pollName=" + pollName +
                ", pollOptions=" + pollOptions +
                ", pollSelectableOptionsCount=" + pollSelectableOptionsCount +
                ", messageSecret=" + Arrays.toString(messageSecret) +
                ", originalSelfAuthor=" + originalSelfAuthor +
                ", senderTimestampMs=" + senderTimestampMilliseconds +
                ", pollUpdateParentKey=" + pollUpdateParentKey +
                ", encPollVote=" + encPollVote +
                ", encReactionTargetMessageKey=" + encReactionTargetMessageKey +
                ", encReactionEncPayload=" + Arrays.toString(encReactionEncPayload) +
                ", encReactionEncIv=" + Arrays.toString(encReactionEncIv) +
                ']';
    }
}