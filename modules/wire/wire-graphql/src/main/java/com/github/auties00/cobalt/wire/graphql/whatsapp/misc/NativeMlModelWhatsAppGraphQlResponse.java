package com.github.auties00.cobalt.wire.graphql.whatsapp.misc;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.github.auties00.cobalt.wire.graphql.whatsapp.WhatsAppGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModel;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelAsset;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelAssetBuilder;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelBuilder;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelManifest;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelManifestBuilder;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelProperty;
import com.github.auties00.cobalt.wire.linked.business.waa.NativeMachineLearningModelPropertyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses the graph.whatsapp.com GraphQL response of the native-ML-model query built by
 * {@link NativeMlModelWhatsAppGraphQlRequest} into a {@link NativeMachineLearningModelManifest}.
 *
 * <p>Reads the linked root {@code aim_model_batched_manifest} and projects it onto the Cobalt
 * domain model: the resolved on-device ML models, each with its downloadable assets and free-form
 * properties, together with the batch-level entry-point, counts and status markers.
 *
 * @see NativeMlModelWhatsAppGraphQlRequest
 */
@WhatsAppWebModule(moduleName = "WAWebNativeMLModelQuery")
public final class NativeMlModelWhatsAppGraphQlResponse implements WhatsAppGraphQlOperation.Response {
    /**
     * Holds the parsed manifest.
     */
    private final NativeMachineLearningModelManifest manifest;

    /**
     * Constructs a response wrapping the parsed manifest.
     *
     * <p>Reserved for the static parser.
     *
     * @param manifest the parsed manifest, or {@code null} when the graph.whatsapp.com endpoint omitted the field
     */
    private NativeMlModelWhatsAppGraphQlResponse(NativeMachineLearningModelManifest manifest) {
        this.manifest = manifest;
    }

    /**
     * Parses the graph.whatsapp.com GraphQL response from the unwrapped GraphQL {@code data} object.
     *
     * <p>Reads the linked root {@code aim_model_batched_manifest} and projects it onto a
     * {@link NativeMachineLearningModelManifest}; the returned {@link Optional} is empty when
     * {@code data} or the manifest object is missing.
     *
     * @param data the unwrapped GraphQL {@code data} object returned by
     *             {@code WhatsAppGraphQlClient#send(WhatsAppGraphQlOperation.Request)}
     * @return the parsed response, or empty when {@code data} or the manifest object is missing
     */
    public static Optional<NativeMlModelWhatsAppGraphQlResponse> of(JSONObject data) {
        if (data == null) {
            return Optional.empty();
        }

        var node = data.getJSONObject("aim_model_batched_manifest");
        if (node == null) {
            return Optional.empty();
        }

        var manifest = new NativeMachineLearningModelManifestBuilder()
                .models(parseModels(node.getJSONArray("models")))
                .entryPoint(node.getString("entry_point"))
                .assetCount(node.getLong("asset_count"))
                .modelCount(node.getLong("model_count"))
                .status(node.getString("status"))
                .statusDetails(node.getString("status_details"))
                .build();
        return Optional.of(new NativeMlModelWhatsAppGraphQlResponse(manifest));
    }

    /**
     * Projects the {@code models} array onto a list of {@link NativeMachineLearningModel}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<NativeMachineLearningModel> parseModels(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<NativeMachineLearningModel>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new NativeMachineLearningModelBuilder()
                    .name(obj.getString("name"))
                    .version(obj.getString("version"))
                    .assets(parseAssets(obj.getJSONArray("assets")))
                    .properties(parseProperties(obj.getJSONArray("properties")))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code assets} array onto a list of {@link NativeMachineLearningModelAsset}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<NativeMachineLearningModelAsset> parseAssets(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<NativeMachineLearningModelAsset>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new NativeMachineLearningModelAssetBuilder()
                    .name(obj.getString("name"))
                    .id(obj.getString("id"))
                    .cacheKey(obj.getString("cache_key"))
                    .sourceContentHash(obj.getString("source_content_hash"))
                    .md5Hash(obj.getString("md5_hash"))
                    .assetHandle(obj.getString("asset_handle"))
                    .creationTime(obj.getString("creation_time"))
                    .url(obj.getString("url"))
                    .sizeBytes(obj.getLong("filesize_bytes"))
                    .compressionType(obj.getString("compression_type"))
                    .assetType(obj.getString("asset_type"))
                    .build());
        }
        return result;
    }

    /**
     * Projects the {@code properties} array onto a list of {@link NativeMachineLearningModelProperty}.
     *
     * @param arr the JSON array to project
     * @return the projected list, empty when {@code arr} is {@code null}
     */
    private static List<NativeMachineLearningModelProperty> parseProperties(JSONArray arr) {
        if (arr == null) {
            return List.of();
        }

        var result = new ArrayList<NativeMachineLearningModelProperty>(arr.size());
        for (var i = 0; i < arr.size(); i++) {
            var obj = arr.getJSONObject(i);
            if (obj == null) {
                continue;
            }

            result.add(new NativeMachineLearningModelPropertyBuilder()
                    .name(obj.getString("name"))
                    .value(obj.getString("value"))
                    .build());
        }
        return result;
    }

    /**
     * Returns the parsed manifest.
     *
     * @return the parsed {@link NativeMachineLearningModelManifest}, never {@code null}
     */
    public NativeMachineLearningModelManifest manifest() {
        return manifest;
    }
}
