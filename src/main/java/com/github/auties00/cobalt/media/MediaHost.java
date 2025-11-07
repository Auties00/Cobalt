package com.github.auties00.cobalt.media;

import com.github.auties00.cobalt.model.media.MediaPath;
import com.github.auties00.cobalt.model.media.MediaProvider;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public sealed interface MediaHost {
    String hostname();
    Optional<String> fallbackHostname();
    boolean canDownload(MediaProvider provider);
    boolean canUpload(MediaProvider provider);

    record Primary(
            String hostname,
            Optional<String> fallbackHostname,
            String ip4,
            String fallbackIp4,
            String ip6,
            String fallbackIp6,
            Set<MediaPath> download,
            Set<MediaPath> upload
    ) implements MediaHost {
        @Override
        public boolean canUpload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return download.contains(provider.mediaPath());
        }

        @Override
        public boolean canDownload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return upload.contains(provider.mediaPath());
        }
    }

    record Fallback(String hostname) implements MediaHost {
        @Override
        public boolean canDownload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return true;
        }

        @Override
        public boolean canUpload(MediaProvider provider) {
            Objects.requireNonNull(provider, "provider cannot be null");
            return true;
        }

        @Override
        public Optional<String> fallbackHostname() {
            return Optional.empty();
        }
    }
}
