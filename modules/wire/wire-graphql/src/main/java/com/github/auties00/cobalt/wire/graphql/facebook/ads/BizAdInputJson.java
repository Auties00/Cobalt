package com.github.auties00.cobalt.wire.graphql.facebook.ads;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.linked.business.ads.AdGroupCreative;
import com.github.auties00.cobalt.wire.linked.business.ads.AdGroupSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdCreationRootInput;
import com.github.auties00.cobalt.wire.linked.business.ads.BusinessAdProduct;
import com.github.auties00.cobalt.wire.linked.business.ads.AdsLwiAudience;
import com.github.auties00.cobalt.wire.linked.business.ads.CallToAction;
import com.github.auties00.cobalt.wire.linked.business.ads.CallToActionValue;
import com.github.auties00.cobalt.wire.linked.business.ads.ChildAttachment;
import com.github.auties00.cobalt.wire.linked.business.ads.CreativeDegreesOfFreedomSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.CreativeFeatureActionMetadata;
import com.github.auties00.cobalt.wire.linked.business.ads.CreativeFeatureAttachment;
import com.github.auties00.cobalt.wire.linked.business.ads.CreativeFeaturesSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.DetailedTargetingItem;
import com.github.auties00.cobalt.wire.linked.business.ads.LinkData;
import com.github.auties00.cobalt.wire.linked.business.ads.LwiBoostedComponentInput;
import com.github.auties00.cobalt.wire.linked.business.ads.MessengerWelcomeMessage;
import com.github.auties00.cobalt.wire.linked.business.ads.ObjectStorySpec;
import com.github.auties00.cobalt.wire.linked.business.ads.OptimizationGoalInput;
import com.github.auties00.cobalt.wire.linked.business.ads.PlacementSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.SensitiveString;
import com.github.auties00.cobalt.wire.linked.business.ads.SpecialAdCategory;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingAutomation;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingFlexibleSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingGeoLocationEntry;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingGeoLocations;
import com.github.auties00.cobalt.wire.linked.business.ads.TargetingSpec;
import com.github.auties00.cobalt.wire.linked.business.ads.TuningOptions;
import com.github.auties00.cobalt.wire.linked.business.ads.VideoData;

import java.util.List;

/**
 * Serializes the WhatsApp Business ad-creation input models to their Facebook GraphQL JSON shape.
 *
 * <p>The model types are pure, transport-agnostic domain holders carrying camelCase fields; the
 * snake_case GraphQL keys the Meta graph endpoint expects are a transport concern that lives here. Each
 * helper writes one input model as a JSON value into a caller-provided {@link JSONWriter}, omitting any
 * field whose value is absent (and, for lists, empty). The class is package-visible across the
 * ad-creation operation packages (it carries the shapes shared by both the {@code graphql.facebook.ads}
 * and {@code graphql.facebook.misc} request operations) but is not part of the module's exported
 * surface.
 */
public final class BizAdInputJson {
    /**
     * Prevents instantiation of this static-helper holder.
     */
    private BizAdInputJson() {
        throw new AssertionError();
    }

    /**
     * Writes a sensitive string as the {@code {sensitive_string_value}} object.
     *
     * @param writer the writer to append the object to
     * @param value  the sensitive string to serialize
     */
    public static void writeSensitiveString(JSONWriter writer, SensitiveString value) {
        writer.startObject();
        value.value().ifPresent(inner -> {
            writer.writeName("sensitive_string_value");
            writer.writeColon();
            writer.writeString(inner);
        });
        writer.endObject();
    }

    /**
     * Writes a boosted-component input as its full {@code {adgroup_specs, ads_lwi_goal, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param input  the boosted-component input to serialize
     */
    public static void writeLwiBoostedComponentInput(JSONWriter writer, LwiBoostedComponentInput input) {
        writer.startObject();
        var adgroupSpecs = input.adgroupSpecs();
        if (!adgroupSpecs.isEmpty()) {
            writer.writeName("adgroup_specs");
            writer.writeColon();
            writeArray(writer, adgroupSpecs, BizAdInputJson::writeAdGroupSpec);
        }
        writeStringField(writer, "ads_lwi_goal", input.adsLwiGoal().orElse(null));
        writeStringField(writer, "audience_option", input.audienceOption().orElse(null));
        input.budget().ifPresent(value -> {
            writer.writeName("budget");
            writer.writeColon();
            writer.writeInt64(value);
        });
        writeStringField(writer, "budget_type", input.budgetType().orElse(null));
        writeStringField(writer, "currency", input.currency().orElse(null));
        writeStringField(writer, "dsa_beneficiary", input.dsaBeneficiary().orElse(null));
        writeStringField(writer, "dsa_payor", input.dsaPayor().orElse(null));
        input.durationInDays().ifPresent(value -> {
            writer.writeName("duration_in_days");
            writer.writeColon();
            writer.writeInt32(value);
        });
        writeStringField(writer, "legacy_ad_account_id", input.legacyAdAccountId().orElse(null));
        input.messengerWelcomeMessage().ifPresent(message -> {
            writer.writeName("messenger_welcome_message");
            writer.writeColon();
            writeMessengerWelcomeMessage(writer, message);
        });
        writeStringField(writer, "objective", input.objective().orElse(null));
        input.placementSpec().ifPresent(spec -> {
            writer.writeName("placement_spec");
            writer.writeColon();
            writePlacementSpec(writer, spec);
        });
        writeStringField(writer, "saved_audience_id", input.savedAudienceId().orElse(null));
        writeStringField(writer, "targeting_spec_string", input.targetingSpecString().orElse(null));
        writer.endObject();
    }

    /**
     * Writes an ad-creation-root input as the {@code {ad_account_id, boost_id, flow_id, page_id,
     * product}} object.
     *
     * @param writer the writer to append the object to
     * @param input  the ad-creation-root input to serialize
     */
    public static void writeCreationRootInput(JSONWriter writer, BusinessAdCreationRootInput input) {
        writer.startObject();
        writeStringField(writer, "ad_account_id", input.adAccountId().orElse(null));
        writeStringField(writer, "boost_id", input.boostId().orElse(null));
        writeStringField(writer, "flow_id", input.flowId().orElse(null));
        writeStringField(writer, "page_id", input.pageId().orElse(null));
        writeStringField(writer, "product", input.product().map(BusinessAdProduct::wireValue).orElse(null));
        writer.endObject();
    }

    /**
     * Writes an ad-group spec as the {@code {creative, id}} object.
     *
     * @param writer the writer to append the object to
     * @param spec   the ad-group spec to serialize
     */
    public static void writeAdGroupSpec(JSONWriter writer, AdGroupSpec spec) {
        writer.startObject();
        spec.creative().ifPresent(creative -> {
            writer.writeName("creative");
            writer.writeColon();
            writeAdGroupCreative(writer, creative);
        });
        writeStringField(writer, "id", spec.id().orElse(null));
        writer.endObject();
    }

    /**
     * Writes an ad-group creative as the {@code {object_story_spec, degrees_of_freedom_spec}} object.
     *
     * @param writer   the writer to append the object to
     * @param creative the ad-group creative to serialize
     */
    public static void writeAdGroupCreative(JSONWriter writer, AdGroupCreative creative) {
        writer.startObject();
        creative.objectStorySpec().ifPresent(story -> {
            writer.writeName("object_story_spec");
            writer.writeColon();
            writeObjectStorySpec(writer, story);
        });
        creative.degreesOfFreedomSpec().ifPresent(dof -> {
            writer.writeName("degrees_of_freedom_spec");
            writer.writeColon();
            writeCreativeDegreesOfFreedomSpec(writer, dof);
        });
        writer.endObject();
    }

    /**
     * Writes a creative degrees-of-freedom spec as the {@code {degrees_of_freedom_type,
     * image_transformation_types, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param dof    the degrees-of-freedom spec to serialize
     */
    public static void writeCreativeDegreesOfFreedomSpec(JSONWriter writer, CreativeDegreesOfFreedomSpec dof) {
        writer.startObject();
        writeStringField(writer, "degrees_of_freedom_type", dof.degreesOfFreedomType().orElse(null));
        writeStringArrayField(writer, "image_transformation_types", dof.imageTransformationTypes());
        writeStringArrayField(writer, "stories_transformation_types", dof.storiesTransformationTypes());
        writeStringArrayField(writer, "text_transformation_types", dof.textTransformationTypes());
        dof.creativeFeaturesSpec().ifPresent(spec -> {
            writer.writeName("creative_features_spec");
            writer.writeColon();
            writeCreativeFeaturesSpec(writer, spec);
        });
        writer.endObject();
    }

    /**
     * Writes a creative-features spec as the {@code {product_extensions}} object.
     *
     * @param writer the writer to append the object to
     * @param spec   the creative-features spec to serialize
     */
    public static void writeCreativeFeaturesSpec(JSONWriter writer, CreativeFeaturesSpec spec) {
        writer.startObject();
        spec.productExtensions().ifPresent(extension -> {
            writer.writeName("product_extensions");
            writer.writeColon();
            writeCreativeFeatureAttachment(writer, extension);
        });
        writer.endObject();
    }

    /**
     * Writes a creative-feature product extension as the {@code {action_metadata, enroll_status}} object.
     *
     * @param writer     the writer to append the object to
     * @param attachment the product extension to serialize
     */
    public static void writeCreativeFeatureAttachment(JSONWriter writer, CreativeFeatureAttachment attachment) {
        writer.startObject();
        attachment.actionMetadata().ifPresent(metadata -> {
            writer.writeName("action_metadata");
            writer.writeColon();
            writeCreativeFeatureActionMetadata(writer, metadata);
        });
        writeStringField(writer, "enroll_status", attachment.enrollStatus().orElse(null));
        writer.endObject();
    }

    /**
     * Writes a creative-feature action-metadata entry as the {@code {type}} object.
     *
     * @param writer   the writer to append the object to
     * @param metadata the action-metadata entry to serialize
     */
    public static void writeCreativeFeatureActionMetadata(JSONWriter writer, CreativeFeatureActionMetadata metadata) {
        writer.startObject();
        writeStringField(writer, "type", metadata.type().orElse(null));
        writer.endObject();
    }

    /**
     * Writes an object story spec as the {@code {page_id, instagram_actor_id, instagram_user_id,
     * link_data, video_data}} object.
     *
     * @param writer the writer to append the object to
     * @param spec   the object story spec to serialize
     */
    public static void writeObjectStorySpec(JSONWriter writer, ObjectStorySpec spec) {
        writer.startObject();
        writeStringField(writer, "page_id", spec.pageId().orElse(null));
        writeStringField(writer, "instagram_actor_id", spec.instagramActorId().orElse(null));
        writeStringField(writer, "instagram_user_id", spec.instagramUserId().orElse(null));
        spec.linkData().ifPresent(linkData -> {
            writer.writeName("link_data");
            writer.writeColon();
            writeLinkData(writer, linkData);
        });
        spec.videoData().ifPresent(videoData -> {
            writer.writeName("video_data");
            writer.writeColon();
            writeVideoData(writer, videoData);
        });
        writer.endObject();
    }

    /**
     * Writes a link-data creative as the {@code {call_to_action, child_attachments, description, ...}}
     * object.
     *
     * @param writer the writer to append the object to
     * @param data   the link data to serialize
     */
    public static void writeLinkData(JSONWriter writer, LinkData data) {
        writer.startObject();
        data.callToAction().ifPresent(cta -> {
            writer.writeName("call_to_action");
            writer.writeColon();
            writeCallToAction(writer, cta);
        });
        var childAttachments = data.childAttachments();
        if (!childAttachments.isEmpty()) {
            writer.writeName("child_attachments");
            writer.writeColon();
            writeArray(writer, childAttachments, BizAdInputJson::writeChildAttachment);
        }
        writeStringField(writer, "description", data.description().orElse(null));
        writeStringField(writer, "event_id", data.eventId().orElse(null));
        writeStringField(writer, "image_crops", data.imageCrops().orElse(null));
        writeStringField(writer, "image_hash", data.imageHash().orElse(null));
        writeStringField(writer, "link", data.link().orElse(null));
        writeStringField(writer, "message", data.message().orElse(null));
        writeStringField(writer, "name", data.name().orElse(null));
        writeStringField(writer, "picture", data.picture().orElse(null));
        writeStringArrayField(writer, "retailer_item_ids", data.retailerItemIds());
        writeBoolField(writer, "use_flexible_image_aspect_ratio", data.useFlexibleImageAspectRatio());
        writer.endObject();
    }

    /**
     * Writes a video-data creative as the {@code {call_to_action, image_hash, image_url, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param data   the video data to serialize
     */
    public static void writeVideoData(JSONWriter writer, VideoData data) {
        writer.startObject();
        data.callToAction().ifPresent(cta -> {
            writer.writeName("call_to_action");
            writer.writeColon();
            writeCallToAction(writer, cta);
        });
        writeStringField(writer, "image_hash", data.imageHash().orElse(null));
        writeStringField(writer, "image_url", data.imageUrl().orElse(null));
        writeStringField(writer, "link_description", data.linkDescription().orElse(null));
        writeStringField(writer, "message", data.message().orElse(null));
        writeStringField(writer, "title", data.title().orElse(null));
        writeStringField(writer, "video_id", data.videoId().orElse(null));
        writer.endObject();
    }

    /**
     * Writes one carousel card as the {@code {call_to_action, description, image_hash, ...}} object.
     *
     * @param writer     the writer to append the object to
     * @param attachment the carousel card to serialize
     */
    public static void writeChildAttachment(JSONWriter writer, ChildAttachment attachment) {
        writer.startObject();
        attachment.callToAction().ifPresent(cta -> {
            writer.writeName("call_to_action");
            writer.writeColon();
            writeCallToAction(writer, cta);
        });
        writeStringField(writer, "description", attachment.description().orElse(null));
        writeStringField(writer, "image_hash", attachment.imageHash().orElse(null));
        writeStringField(writer, "link", attachment.link().orElse(null));
        writeStringField(writer, "name", attachment.name().orElse(null));
        writeStringField(writer, "picture", attachment.picture().orElse(null));
        writeStringField(writer, "video_id", attachment.videoId().orElse(null));
        writer.endObject();
    }

    /**
     * Writes a call-to-action as the {@code {type, value}} object.
     *
     * @param writer the writer to append the object to
     * @param cta    the call-to-action to serialize
     */
    public static void writeCallToAction(JSONWriter writer, CallToAction cta) {
        writer.startObject();
        writeStringField(writer, "type", cta.type().orElse(null));
        cta.value().ifPresent(value -> {
            writer.writeName("value");
            writer.writeColon();
            writeCallToActionValue(writer, value);
        });
        writer.endObject();
    }

    /**
     * Writes a call-to-action value as the {@code {link, app_link, app_destination, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param value  the call-to-action value to serialize
     */
    public static void writeCallToActionValue(JSONWriter writer, CallToActionValue value) {
        writer.startObject();
        writeStringField(writer, "link", value.link().orElse(null));
        writeStringField(writer, "app_link", value.appLink().orElse(null));
        writeStringField(writer, "app_destination", value.appDestination().orElse(null));
        writeStringField(writer, "event_id", value.eventId().orElse(null));
        writeStringField(writer, "group_id", value.groupId().orElse(null));
        writeStringField(writer, "lead_gen_form_id", value.leadGenFormId().orElse(null));
        writeStringField(writer, "page", value.page().orElse(null));
        writeStringField(writer, "whatsapp_number", value.whatsappNumber().orElse(null));
        writer.endObject();
    }

    /**
     * Writes a welcome message as the {@code {greeting, icebreakers, icebreakers_enabled, ...}} object.
     *
     * @param writer  the writer to append the object to
     * @param message the welcome message to serialize
     */
    public static void writeMessengerWelcomeMessage(JSONWriter writer, MessengerWelcomeMessage message) {
        writer.startObject();
        writeStringField(writer, "greeting", message.greeting().orElse(null));
        var icebreakers = message.icebreakers();
        if (!icebreakers.isEmpty()) {
            writer.writeName("icebreakers");
            writer.writeColon();
            writeStringArray(writer, icebreakers);
        }
        writeBoolField(writer, "icebreakers_enabled", message.icebreakersEnabled());
        writeStringField(writer, "prefill", message.prefill().orElse(null));
        writeBoolField(writer, "prefill_enabled", message.prefillEnabled());
        writeBoolField(writer, "prefill_message_edited", message.prefillMessageEdited());
        writer.endObject();
    }

    /**
     * Writes a placement spec as the {@code {publisher_platforms}} object.
     *
     * @param writer the writer to append the object to
     * @param spec   the placement spec to serialize
     */
    public static void writePlacementSpec(JSONWriter writer, PlacementSpec spec) {
        writer.startObject();
        var platforms = spec.publisherPlatforms();
        if (!platforms.isEmpty()) {
            writer.writeName("publisher_platforms");
            writer.writeColon();
            writeStringArray(writer, platforms);
        }
        writer.endObject();
    }

    /**
     * Writes an optimisation goal as the {@code {optimization_goal}} object.
     *
     * @param writer the writer to append the object to
     * @param input  the optimisation goal to serialize
     */
    public static void writeOptimizationGoalInput(JSONWriter writer, OptimizationGoalInput input) {
        writer.startObject();
        writeStringField(writer, "optimization_goal", input.optimizationGoal().orElse(null));
        writer.endObject();
    }

    /**
     * Writes an audience option as the {@code {audience_option, audience_key, name, ...}} object.
     *
     * @param writer   the writer to append the object to
     * @param audience the audience option to serialize
     */
    public static void writeAdsLwiAudience(JSONWriter writer, AdsLwiAudience audience) {
        writer.startObject();
        writeStringField(writer, "audience_option", audience.audienceOption().orElse(null));
        writeStringField(writer, "audience_key", audience.audienceKey().orElse(null));
        writeStringField(writer, "name", audience.name().orElse(null));
        writeBoolField(writer, "client_editable", audience.clientEditable());
        writeBoolField(writer, "subject_to_dsa", audience.subjectToDsa());
        writeStringField(writer, "target_spec_string_without_placements",
                audience.targetSpecStringWithoutPlacements().orElse(null));
        writer.endObject();
    }

    /**
     * Writes a targeting spec as the {@code {age_min, age_max, genders, geo_locations, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param spec   the targeting spec to serialize
     */
    public static void writeTargetingSpec(JSONWriter writer, TargetingSpec spec) {
        writer.startObject();
        spec.ageMin().ifPresent(value -> {
            writer.writeName("age_min");
            writer.writeColon();
            writer.writeInt32(value);
        });
        spec.ageMax().ifPresent(value -> {
            writer.writeName("age_max");
            writer.writeColon();
            writer.writeInt32(value);
        });
        writeIntArrayField(writer, "age_range", spec.ageRange());
        writeIntArrayField(writer, "genders", spec.genders());
        spec.geoLocations().ifPresent(geo -> {
            writer.writeName("geo_locations");
            writer.writeColon();
            writeTargetingGeoLocations(writer, geo);
        });
        var flexibleSpec = spec.flexibleSpec();
        if (!flexibleSpec.isEmpty()) {
            writer.writeName("flexible_spec");
            writer.writeColon();
            writeArray(writer, flexibleSpec, BizAdInputJson::writeTargetingFlexibleSpec);
        }
        spec.targetingAutomation().ifPresent(automation -> {
            writer.writeName("targeting_automation");
            writer.writeColon();
            writeTargetingAutomation(writer, automation);
        });
        writer.endObject();
    }

    /**
     * Writes one flexible detailed-targeting group as the {@code {interests, behaviors, life_events,
     * ...}} object, writing each category array only when non-empty.
     *
     * @param writer the writer to append the object to
     * @param spec   the flexible detailed-targeting group to serialize
     */
    public static void writeTargetingFlexibleSpec(JSONWriter writer, TargetingFlexibleSpec spec) {
        writer.startObject();
        writeDetailedTargetingItems(writer, "interests", spec.interests());
        writeDetailedTargetingItems(writer, "behaviors", spec.behaviors());
        writeDetailedTargetingItems(writer, "life_events", spec.lifeEvents());
        writeDetailedTargetingItems(writer, "education_statuses", spec.educationStatuses());
        writeDetailedTargetingItems(writer, "education_schools", spec.educationSchools());
        writeDetailedTargetingItems(writer, "education_majors", spec.educationMajors());
        writeDetailedTargetingItems(writer, "work_positions", spec.workPositions());
        writeDetailedTargetingItems(writer, "work_employers", spec.workEmployers());
        writeDetailedTargetingItems(writer, "relationship_statuses", spec.relationshipStatuses());
        writeDetailedTargetingItems(writer, "interested_in", spec.interestedIn());
        writer.endObject();
    }

    /**
     * Writes a geographic constraint as the {@code {countries, cities, regions, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param geo    the geographic constraint to serialize
     */
    public static void writeTargetingGeoLocations(JSONWriter writer, TargetingGeoLocations geo) {
        writer.startObject();
        writeStringArrayField(writer, "countries", geo.countries());
        writeGeoEntries(writer, "cities", geo.cities());
        writeGeoEntries(writer, "regions", geo.regions());
        writeGeoEntries(writer, "country_groups", geo.countryGroups());
        writeGeoEntries(writer, "custom_locations", geo.customLocations());
        writeGeoEntries(writer, "zips", geo.zips());
        writeGeoEntries(writer, "neighborhoods", geo.neighborhoods());
        writeGeoEntries(writer, "geo_markets", geo.geoMarkets());
        writeGeoEntries(writer, "places", geo.places());
        writer.endObject();
    }

    /**
     * Writes one geographic entry as the {@code {key, name, radius, distance_unit, ...}} object.
     *
     * @param writer the writer to append the object to
     * @param entry  the geographic entry to serialize
     */
    public static void writeTargetingGeoLocationEntry(JSONWriter writer, TargetingGeoLocationEntry entry) {
        writer.startObject();
        writeStringField(writer, "key", entry.key().orElse(null));
        writeStringField(writer, "name", entry.name().orElse(null));
        entry.radius().ifPresent(value -> {
            writer.writeName("radius");
            writer.writeColon();
            writer.writeDouble(value);
        });
        writeStringField(writer, "distance_unit", entry.distanceUnit().orElse(null));
        entry.latitude().ifPresent(value -> {
            writer.writeName("latitude");
            writer.writeColon();
            writer.writeDouble(value);
        });
        entry.longitude().ifPresent(value -> {
            writer.writeName("longitude");
            writer.writeColon();
            writer.writeDouble(value);
        });
        writeStringField(writer, "country", entry.country().orElse(null));
        writeStringField(writer, "country_code", entry.countryCode().orElse(null));
        writeStringField(writer, "country_name", entry.countryName().orElse(null));
        writeStringField(writer, "region", entry.region().orElse(null));
        writeStringField(writer, "primary_city", entry.primaryCity().orElse(null));
        writeStringField(writer, "address_string", entry.addressString().orElse(null));
        writer.endObject();
    }

    /**
     * Writes targeting automation as the {@code {advantage_audience}} object.
     *
     * @param writer     the writer to append the object to
     * @param automation the targeting automation to serialize
     */
    public static void writeTargetingAutomation(JSONWriter writer, TargetingAutomation automation) {
        writer.startObject();
        automation.advantageAudience().ifPresent(value -> {
            writer.writeName("advantage_audience");
            writer.writeColon();
            writer.writeInt32(value);
        });
        writer.endObject();
    }

    /**
     * Writes one detailed-targeting item as the {@code {id, name, type}} object.
     *
     * @param writer the writer to append the object to
     * @param item   the detailed-targeting item to serialize
     */
    public static void writeDetailedTargetingItem(JSONWriter writer, DetailedTargetingItem item) {
        writer.startObject();
        writeStringField(writer, "id", item.id().orElse(null));
        writeStringField(writer, "name", item.name().orElse(null));
        writeStringField(writer, "type", item.type().orElse(null));
        writer.endObject();
    }

    /**
     * Writes tuning options as the {@code {clear_custom_audiences}} object.
     *
     * @param writer  the writer to append the object to
     * @param options the tuning options to serialize
     */
    public static void writeTuningOptions(JSONWriter writer, TuningOptions options) {
        writer.startObject();
        writeBoolField(writer, "clear_custom_audiences", options.clearCustomAudiences());
        writer.endObject();
    }

    /**
     * Writes a list of special ad categories as a JSON array of their wire literals.
     *
     * @param writer     the writer to append the array to
     * @param categories the categories to serialize, in order
     */
    public static void writeSpecialAdCategories(JSONWriter writer, List<SpecialAdCategory> categories) {
        writer.startArray();
        for (var i = 0; i < categories.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            writer.writeString(categories.get(i).wireValue());
        }
        writer.endArray();
    }

    /**
     * Writes a string field only when {@code value} is non-null.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param value  the value, or {@code null} to omit the field
     */
    private static void writeStringField(JSONWriter writer, String name, String value) {
        if (value != null) {
            writer.writeName(name);
            writer.writeColon();
            writer.writeString(value);
        }
    }

    /**
     * Writes a boolean field.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param value  the boolean value
     */
    private static void writeBoolField(JSONWriter writer, String name, boolean value) {
        writer.writeName(name);
        writer.writeColon();
        writer.writeBool(value);
    }

    /**
     * Writes a list of strings as a JSON array.
     *
     * @param writer the writer to append the array to
     * @param values the strings to serialize, in order
     */
    private static void writeStringArray(JSONWriter writer, List<String> values) {
        writer.startArray();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            writer.writeString(values.get(i));
        }
        writer.endArray();
    }

    /**
     * Writes a named string-array field only when {@code values} is non-empty.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param values the strings to serialize, in order
     */
    private static void writeStringArrayField(JSONWriter writer, String name, List<String> values) {
        if (!values.isEmpty()) {
            writer.writeName(name);
            writer.writeColon();
            writeStringArray(writer, values);
        }
    }

    /**
     * Writes a named integer-array field only when {@code values} is non-empty.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param values the integers to serialize, in order
     */
    private static void writeIntArrayField(JSONWriter writer, String name, List<Integer> values) {
        if (values.isEmpty()) {
            return;
        }
        writer.writeName(name);
        writer.writeColon();
        writer.startArray();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            writer.writeInt32(values.get(i));
        }
        writer.endArray();
    }

    /**
     * Writes a named detailed-targeting-item array field only when {@code items} is non-empty.
     *
     * @param writer the writer to append to
     * @param name   the JSON key
     * @param items  the detailed-targeting items to serialize, in order
     */
    private static void writeDetailedTargetingItems(JSONWriter writer, String name, List<DetailedTargetingItem> items) {
        if (!items.isEmpty()) {
            writer.writeName(name);
            writer.writeColon();
            writeArray(writer, items, BizAdInputJson::writeDetailedTargetingItem);
        }
    }

    /**
     * Writes a named geographic-entry array field only when {@code entries} is non-empty.
     *
     * @param writer  the writer to append to
     * @param name    the JSON key
     * @param entries the geographic entries to serialize, in order
     */
    private static void writeGeoEntries(JSONWriter writer, String name, List<TargetingGeoLocationEntry> entries) {
        if (!entries.isEmpty()) {
            writer.writeName(name);
            writer.writeColon();
            writeArray(writer, entries, BizAdInputJson::writeTargetingGeoLocationEntry);
        }
    }

    /**
     * Writes each element of {@code values} as a JSON array using {@code elementWriter}.
     *
     * @param writer        the writer to append the array to
     * @param values        the elements to serialize, in order
     * @param elementWriter the per-element serializer
     * @param <T>           the element type
     */
    private static <T> void writeArray(JSONWriter writer, List<T> values, ElementWriter<T> elementWriter) {
        writer.startArray();
        for (var i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.writeComma();
            }
            elementWriter.write(writer, values.get(i));
        }
        writer.endArray();
    }

    /**
     * Serializes a single element of a JSON array into a {@link JSONWriter}.
     *
     * @param <T> the element type this writer serializes
     */
    @FunctionalInterface
    private interface ElementWriter<T> {
        /**
         * Writes {@code value} into {@code writer}.
         *
         * @param writer the writer to append to
         * @param value  the element to serialize
         */
        void write(JSONWriter writer, T value);
    }
}
