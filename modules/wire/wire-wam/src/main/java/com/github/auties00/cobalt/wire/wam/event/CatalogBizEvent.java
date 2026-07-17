package com.github.auties00.cobalt.wire.wam.event;

import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;

import com.github.auties00.cobalt.wam.annotation.WamEvent;
import com.github.auties00.cobalt.wam.model.WamEventSpec;
import com.github.auties00.cobalt.wam.annotation.WamProperty;
import com.github.auties00.cobalt.wam.model.WamType;
import com.github.auties00.cobalt.wire.wam.type.CatalogBizAction;
import com.github.auties00.cobalt.wire.wam.type.CatalogEntryPoint;
import com.github.auties00.cobalt.wire.wam.type.DeepLinkOpenFrom;
import com.github.auties00.cobalt.wire.wam.type.LastMessageDirection;

import java.util.Optional;
import java.util.OptionalLong;

@WhatsAppWebModule(moduleName = "WAWebCatalogBizWamEvent")
@WamEvent(id = 1722)
public interface CatalogBizEvent extends WamEventSpec {
    @WamProperty(index = 19, type = WamType.STRING)
    Optional<String> adId();

    @WamProperty(index = 13, type = WamType.BOOLEAN)
    Optional<Boolean> cartToggle();

    @WamProperty(index = 4, type = WamType.STRING)
    Optional<String> catalogAppealReason();

    @WamProperty(index = 1, type = WamType.ENUM)
    Optional<CatalogBizAction> catalogBizAction();

    @WamProperty(index = 7, type = WamType.ENUM)
    Optional<CatalogEntryPoint> catalogEntryPoint();

    @WamProperty(index = 3, type = WamType.STRING)
    Optional<String> catalogSessionId();

    @WamProperty(index = 18, type = WamType.INTEGER)
    OptionalLong collectionCount();

    @WamProperty(index = 14, type = WamType.STRING)
    Optional<String> collectionId();

    @WamProperty(index = 15, type = WamType.STRING)
    Optional<String> collectionIndex();

    @WamProperty(index = 8, type = WamType.ENUM)
    Optional<DeepLinkOpenFrom> deepLinkOpenFrom();

    @WamProperty(index = 20, type = WamType.STRING)
    Optional<String> entryPointConversionSource();

    @WamProperty(index = 5, type = WamType.INTEGER)
    OptionalLong errorCode();

    @WamProperty(index = 24, type = WamType.STRING)
    Optional<String> extraAttributes();

    @WamProperty(index = 10, type = WamType.BOOLEAN)
    Optional<Boolean> isOrderMsgAttached();

    @WamProperty(index = 21, type = WamType.ENUM)
    Optional<LastMessageDirection> lastMessageDirection();

    @WamProperty(index = 22, type = WamType.INTEGER)
    OptionalLong messageDepth();

    @WamProperty(index = 9, type = WamType.STRING)
    Optional<String> orderId();

    @WamProperty(index = 6, type = WamType.INTEGER)
    OptionalLong productCount();

    @WamProperty(index = 2, type = WamType.STRING)
    Optional<String> productId();

    @WamProperty(index = 12, type = WamType.STRING)
    Optional<String> productIds();

    @WamProperty(index = 16, type = WamType.STRING)
    Optional<String> productIndex();

    @WamProperty(index = 11, type = WamType.INTEGER)
    OptionalLong quantity();

    @WamProperty(index = 23, type = WamType.STRING)
    Optional<String> threadIdHmac();
}
