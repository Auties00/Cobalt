package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.contact.ContactJid;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record ChannelResponse(@JsonProperty("id") ContactJid jid, ChannelState state,
                              @JsonProperty("thread_metadata") ChannelMetadata metadata,
                              @JsonProperty("viewer_metadata") ChannelViewerMetadata viewerMetadata

) {
    public static Optional<ChannelResponse> ofJson(@NonNull String json) {
        return Json.readValue(json, JsonResponse.class)
                .data()
                .flatMap(JsonData::response);
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(
            @JsonAlias("xwa2_newsletter_update") @JsonProperty("xwa2_newsletter_create") Optional<ChannelResponse> response) {

    }

    public record ChannelMetadata(ChannelName name, ChannelDescription description,
                                  Optional<RecommendedChannelsResponse.ChannelPicture> picture, Optional<String> handle,
                                  Optional<ChannelSettings> settings,
                                  String invite, @JsonProperty("subscribers_count") int subscribers,
                                  String verification, @JsonProperty("creation_time") long creationTimestampSeconds) {
        public Optional<ZonedDateTime> creationTimestamp() {
            return Clock.parseSeconds(creationTimestampSeconds);
        }
    }

    public record ChannelName(String id, String text, @JsonProperty("update_time") long updateTimeSeconds) {
        public Optional<ZonedDateTime> updateTime() {
            return Clock.parseSeconds(updateTimeSeconds);
        }
    }


    public record ChannelDescription(String id, String text, @JsonProperty("update_time") long updateTimeSeconds) {
        public Optional<ZonedDateTime> updateTime() {
            return Clock.parseSeconds(updateTimeSeconds);
        }
    }

    public record ChannelPicture(String id, String text, @JsonProperty("direct_path") String directPath) {

    }

    public record ChannelPreview(String id, String type, @JsonProperty("direct_path") String directPath) {

    }

    public record ChannelState(String type) {

    }

    public record ChannelViewerMetadata(String mute, String role) {

    }

    public record ReactionCodes(String value, @JsonProperty("blocked_codes") List<String> blockedCodes,
                                @JsonProperty("enabled_ts_sec") long enabledTimestampSeconds) {

    }

    public record ChannelSettings(@JsonProperty("reaction_codes") ReactionCodes reactionCodes) {

    }
}