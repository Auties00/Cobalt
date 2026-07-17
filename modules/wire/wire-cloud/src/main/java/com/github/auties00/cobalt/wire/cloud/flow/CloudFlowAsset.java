package com.github.auties00.cobalt.wire.cloud.flow;

import java.util.Objects;

/**
 * An asset attached to a WhatsApp Cloud API Flow.
 *
 * <p>Each Flow carries one or more assets; the {@code FLOW_JSON} asset holds the Flow JSON document
 * that defines the screens and routing. This model carries the management view returned by the assets
 * edge: the asset name, its type, and the short-lived CDN download URL.
 */
public final class CloudFlowAsset {
    /**
     * The asset name, for example {@code flow.json}.
     */
    private final String name;

    /**
     * The asset type.
     */
    private final CloudFlowAssetType assetType;

    /**
     * The short-lived CDN download URL for the asset content.
     */
    private final String downloadUrl;

    /**
     * Constructs a new flow asset.
     *
     * @param name        the asset name
     * @param assetType   the asset type
     * @param downloadUrl the short-lived CDN download URL
     * @throws NullPointerException if {@code name}, {@code assetType}, or {@code downloadUrl} is
     *                              {@code null}
     */
    public CloudFlowAsset(String name, CloudFlowAssetType assetType, String downloadUrl) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.assetType = Objects.requireNonNull(assetType, "assetType must not be null");
        this.downloadUrl = Objects.requireNonNull(downloadUrl, "downloadUrl must not be null");
    }

    /**
     * Returns the asset name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the asset type.
     *
     * @return the {@link CloudFlowAssetType}
     */
    public CloudFlowAssetType assetType() {
        return assetType;
    }

    /**
     * Returns the short-lived CDN download URL.
     *
     * @return the download URL
     */
    public String downloadUrl() {
        return downloadUrl;
    }
}
