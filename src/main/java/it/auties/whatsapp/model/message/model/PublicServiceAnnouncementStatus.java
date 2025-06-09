package it.auties.whatsapp.model.message.model;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;
import it.auties.whatsapp.util.Clock;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@ProtobufMessage(name = "StatusPSA")
public final class PublicServiceAnnouncementStatus {
    @ProtobufProperty(index = 44, type = ProtobufType.STRING)
    final String campaignId;

    @ProtobufProperty(index = 45, type = ProtobufType.UINT64)
    final long campaignExpirationTimestampSeconds;

    PublicServiceAnnouncementStatus(String campaignId, long campaignExpirationTimestampSeconds) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId cannot be null");
        this.campaignExpirationTimestampSeconds = campaignExpirationTimestampSeconds;
    }

    public String campaignId() {
        return campaignId;
    }

    public long campaignExpirationTimestampSeconds() {
        return campaignExpirationTimestampSeconds;
    }

    public Optional<ZonedDateTime> campaignExpirationTimestamp() {
        return Clock.parseSeconds(campaignExpirationTimestampSeconds);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PublicServiceAnnouncementStatus that
                && Objects.equals(campaignId, that.campaignId)
                && campaignExpirationTimestampSeconds == that.campaignExpirationTimestampSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(campaignId, campaignExpirationTimestampSeconds);
    }

    @Override
    public String toString() {
        return "PublicServiceAnnouncementStatus[" +
                "campaignId=" + campaignId + ", " +
                "campaignExpirationTimestampSeconds=" + campaignExpirationTimestampSeconds +
                ']';
    }
}