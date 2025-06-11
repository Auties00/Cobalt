package it.auties.whatsapp.model.media;


import com.alibaba.fastjson2.JSON;

import java.util.Optional;

public record MediaUpload(
        String directPath,
        String url,
        String handle
) {
    public static Optional<MediaUpload> ofJson(byte[] json) {
        if(json == null) {
            return Optional.empty();
        }

        var jsonObject = JSON.parseObject(json);
        if(jsonObject == null) {
            return Optional.empty();
        }

        var directPath = jsonObject.getString("direct_path");
        var url = jsonObject.getString("url");
        var handle = jsonObject.getString("handle");
        var result = new MediaUpload(directPath, url, handle);
        return Optional.of(result);
    }
}
