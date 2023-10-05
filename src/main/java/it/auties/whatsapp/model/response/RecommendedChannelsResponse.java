package it.auties.whatsapp.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp.model.jid.Jid;
import it.auties.whatsapp.util.Clock;
import it.auties.whatsapp.util.Json;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record RecommendedChannelsResponse(List<RecommendedChannel> recommendedChannels) {
    public static Optional<RecommendedChannelsResponse> ofJson(@NonNull String json) {
        System.out.println(json);
        return Json.readValue(json, JsonResponse.class)
                .data()
                .flatMap(JsonData::response);
    }

    private record JsonResponse(Optional<JsonData> data) {

    }

    private record JsonData(@JsonProperty("xwa2_newsletters_directory_list") Optional<RecommendedChannelsResponse> response) {

    }


    public record RecommendedChannel(@JsonProperty("id") Jid jid,
                                     @JsonProperty("thread_metadata") ChannelMetadata metadata) {

    }

    public record ChannelMetadata(ChannelName name, ChannelDescription description, Optional<ChannelPicture> picture,
                                  Optional<String> handle, String invite, @JsonProperty("subscribers_count") int subscribers, String verification,
                                  @JsonProperty("creation_time") long creationTimestampSeconds) {
        public Optional<ZonedDateTime> creationTimestamp() {
            return Clock.parseSeconds(creationTimestampSeconds);
        }
    }

    public record ChannelDescription(String id, String text, @JsonProperty("update_time") long updateTimeSeconds) {
        public Optional<ZonedDateTime> updateTime() {
            return Clock.parseSeconds(updateTimeSeconds);
        }
    }

    public record ChannelName(String id, String text, @JsonProperty("update_time") long updateTimeSeconds) {
        public Optional<ZonedDateTime> updateTime() {
            return Clock.parseSeconds(updateTimeSeconds);
        }
    }

    public record ChannelPicture(String id, String text, @JsonProperty("direct_path") String directPath) {

    }
}