package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamChannel;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.BizPlatform;
import com.github.auties00.cobalt.wire.wam.type.CatalogEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.CatalogViewAction;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkOpenFrom;
import com.github.auties00.cobalt.wire.wam.type.EntryPointConversationInitiated;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebBizCatalogViewWamEvent")
@WamEvent(id = 3006, channel = WamChannel.PRIVATE, privateStatsId = 0)
public interface BizCatalogViewEvent extends WamEventSpec {
    @WamProperty(index = 26, type = WamType.STRING)
    Optional<String> adId();

    @WamProperty(index = 14, type = WamType.ENUM)
    Optional<BizPlatform> bizPlatform();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> cartToggle();

    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> catalogCategoryId();

    @WamProperty(index = 2, type = WamType.ENUM)
    Optional<CatalogEntryPoint> catalogEntryPoint();

    @WamProperty(index = 11, type = WamType.BOOLEAN)
    Optional<Boolean> catalogEventSampled();

    @WamProperty(index = 10, type = WamType.STRING)
    Optional<String> catalogOwnerJid();

    @WamProperty(index = 8, type = WamType.STRING)
    Optional<String> catalogReportReasonCode();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> catalogSessionId();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CatalogViewAction> catalogViewAction();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> collectionId();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> collectionIndex();

    @WamProperty(index = 12, type = WamType.ENUM)
    Optional<DeepLinkOpenFrom> deepLinkOpenFrom();

    @WamProperty(index = 22, type = WamType.ENUM)
    Optional<EntryPointConversationInitiated> entryPointConversationInitiated();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> entryPointConversionApp();

    @WamProperty(index = 21, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 23, type = WamType.BOOLEAN)
    Optional<Boolean> hasVariants();

    @WamProperty(index = 7, type = WamType.BOOLEAN)
    Optional<Boolean> isNewProductAddedToCart();

    @WamProperty(index = 5, type = WamType.BOOLEAN)
    Optional<Boolean> isOrderMsgAttached();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> orderId();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> productId();

    @WamProperty(index = 17, type = WamType.STRING)
    Optional<String> productIndex();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong quantity();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong sequenceNumber();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> variantTypes();

    @WamProperty(index = 25, type = WamType.STRING)
    Optional<String> variantsExtraAttributes();
}
