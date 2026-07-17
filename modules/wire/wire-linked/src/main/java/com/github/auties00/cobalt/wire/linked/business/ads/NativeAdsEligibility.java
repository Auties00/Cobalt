package com.github.auties00.cobalt.wire.linked.business.ads;

import it.auties.protobuf.annotation.ProtobufMessage;
import it.auties.protobuf.annotation.ProtobufProperty;
import it.auties.protobuf.model.ProtobufType;

/**
 * Eligibility signals that gate the WhatsApp Business native-ads creation
 * entry point.
 *
 * <p>Before showing the "create native ad" entry point on the advertising
 * surface, WhatsApp asks the server for four boolean signals describing the
 * advertiser's past advertising activity and whether an advertising-platform
 * page asset is currently linked. This model is that eligibility view; each
 * flag defaults to {@code false} when the server omitted it.
 */
@ProtobufMessage(name = "NativeAdsEligibility")
public final class NativeAdsEligibility {
    /**
     * Whether the advertiser has ever published a Click-to-WhatsApp native
     * ad. Defaults to {@code false} when the server omitted the flag.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.BOOL)
    final boolean lifetimeAdvertiser;

    /**
     * Whether the advertiser created a Click-to-WhatsApp ad on the WhatsApp
     * web client within the last ninety days. Defaults to {@code false} when
     * the server omitted the flag.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.BOOL)
    final boolean recentWebAdvertiser;

    /**
     * Whether an advertising-platform page asset is currently linked.
     * Defaults to {@code false} when the server omitted the flag.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.BOOL)
    final boolean pageAssetLinked;

    /**
     * Whether a page-less advertising asset is currently linked. Defaults to
     * {@code false} when the server omitted the flag.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.BOOL)
    final boolean pagelessAssetLinked;

    /**
     * Constructs a new {@code NativeAdsEligibility}.
     *
     * @param lifetimeAdvertiser  whether the advertiser has ever published a
     *                            Click-to-WhatsApp native ad
     * @param recentWebAdvertiser whether the advertiser created an ad on the
     *                            web client in the last ninety days
     * @param pageAssetLinked     whether a page asset is linked
     * @param pagelessAssetLinked whether a page-less asset is linked
     */
    NativeAdsEligibility(boolean lifetimeAdvertiser, boolean recentWebAdvertiser,
                         boolean pageAssetLinked, boolean pagelessAssetLinked) {
        this.lifetimeAdvertiser = lifetimeAdvertiser;
        this.recentWebAdvertiser = recentWebAdvertiser;
        this.pageAssetLinked = pageAssetLinked;
        this.pagelessAssetLinked = pagelessAssetLinked;
    }

    /**
     * Returns whether the advertiser has ever published a Click-to-WhatsApp
     * native ad.
     *
     * @return {@code true} when the advertiser is a lifetime native
     *         advertiser, {@code false} when not or when the server omitted
     *         the flag
     */
    public boolean lifetimeAdvertiser() {
        return lifetimeAdvertiser;
    }

    /**
     * Returns whether the advertiser created a Click-to-WhatsApp ad on the
     * WhatsApp web client within the last ninety days.
     *
     * @return {@code true} when the advertiser created such an ad in the last
     *         ninety days, {@code false} when not or when the server omitted
     *         the flag
     */
    public boolean recentWebAdvertiser() {
        return recentWebAdvertiser;
    }

    /**
     * Returns whether an advertising-platform page asset is currently linked.
     *
     * @return {@code true} when a page asset is linked, {@code false} when
     *         not or when the server omitted the flag
     */
    public boolean pageAssetLinked() {
        return pageAssetLinked;
    }

    /**
     * Returns whether a page-less advertising asset is currently linked.
     *
     * @return {@code true} when a page-less asset is linked, {@code false}
     *         when not or when the server omitted the flag
     */
    public boolean pagelessAssetLinked() {
        return pagelessAssetLinked;
    }
}
