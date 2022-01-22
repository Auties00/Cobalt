package it.auties.whatsapp.protobuf.sync;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import it.auties.protobuf.decoder.ProtobufTypeDescriptor;
import it.auties.whatsapp.protobuf.action.*;
import it.auties.whatsapp.protobuf.setting.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public final class ActionValueSync implements GenericSync, ProtobufTypeDescriptor {
    @JsonProperty("1")
    @JsonPropertyDescription("int64")
    private long timestamp;

    @JsonProperty("24")
    @JsonPropertyDescription("PrimaryFeature")
    private PrimaryFeature feature;

    @JsonPropertyDescription("action")
    @JsonAlias({"2", "3", "4", "5", "8", "9", "10", "11", "14", "15", "17", "18", "19", "20", "21", "22", "25", "26"})
    private Action action;

    @JsonPropertyDescription("setting")
    @JsonAlias({"6", "7", "16", "23"})
    private Setting setting;

    public boolean hasAction(){
        return action != null;
    }

    public boolean hasSetting(){
        return setting != null;
    }

    public boolean hasFeature(){
        return feature != null
                && feature.flags() != null
                && !feature.flags().isEmpty();
    }

    @Override
    public Map<Integer, Class<?>> descriptor() {
        return ofEntries(
                entry(1, float.class),
                entry(2, StarAction.class),
                entry(3, ContactAction.class),
                entry(4, MuteAction.class),
                entry(5, PinAction.class),
                entry(6, SecurityNotificationSetting.class),
                entry(7, PushNameSetting.class),
                entry(8, QuickReplyAction.class),
                entry(9, RecentStickerWeightsAction.class),
                entry(10, RecentStickerMetadata.class),
                entry(11, RecentEmojiWeightsAction.class),
                entry(14, LabelEditAction.class),
                entry(15, LabelAssociationAction.class),
                entry(16, LocaleSetting.class),
                entry(17, ArchiveChatAction.class),
                entry(18,DeleteMessageForMeAction.class),
                entry(19, KeyExpiration.class),
                entry(20, MarkChatAsReadAction.class),
                entry(21, ClearChatAction.class),
                entry(22, DeleteChatAction.class),
                entry(23, UnarchiveChatsSetting.class),
                entry( 24, PrimaryFeature.class),
                entry(25, FavoriteStickerAction.class),
                entry( 26, AndroidUnsupportedActions.class)
        );
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class RecentEmojiWeightsAction {
        @JsonProperty("1")
        @JsonPropertyDescription("RecentEmojiWeight")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<RecentEmojiWeight> weights;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class RecentStickerWeightsAction {
        @JsonProperty("1")
        @JsonPropertyDescription("RecentStickerWeight")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<RecentStickerWeight> weights;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class PrimaryFeature {
        @JsonProperty("1")
        @JsonPropertyDescription("string")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        private List<String> flags;
    }
}
