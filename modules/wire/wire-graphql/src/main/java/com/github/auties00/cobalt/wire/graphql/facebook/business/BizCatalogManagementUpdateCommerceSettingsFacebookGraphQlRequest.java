package com.github.auties00.cobalt.wire.graphql.facebook.business;

import com.alibaba.fastjson2.JSONWriter;
import com.github.auties00.cobalt.wire.graphql.facebook.FacebookGraphQlOperation;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;
import com.github.auties00.cobalt.wire.core.jid.Jid;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;

/**
 * Builds the Facebook GraphQL mutation that updates a WhatsApp Business commerce setting.
 *
 * <p>The single {@code input} GraphQL variable carries the owning {@code biz_jid} and the
 * {@code cart_enabled} toggle. WhatsApp Web's {@code WAWebBusinessProfileJob.updateCartEnabledGraphQL}
 * fills it with {@code {biz_jid, cart_enabled}} to switch the cart on or off for the business. The
 * Meta graph endpoint echoes the resulting setting under {@code xfb_whatsapp_smb_commerce_settings}; the reply is
 * consumed through {@link BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse}.
 *
 * @see BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlResponse
 */
@WhatsAppWebModule(moduleName = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation")
public final class BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlRequest implements FacebookGraphQlOperation.Request {
    /**
     * The persisted document identifier the Meta graph endpoint maps to the server-side compiled
     * GraphQL document for this operation.
     *
     * <p>Emitted as the {@code doc_id} field of the JSON request body.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation.graphql", exports = "params.id",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String DOC_ID = "9797519763673469";

    /**
     * The GraphQL operation name carried by this request.
     *
     * <p>Used as the persisted-query lookup key and as the perf-telemetry tag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation.graphql", exports = "params.name",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static final String OPERATION_NAME = "WAWebBizCatalogManagementUpdateCommerceSettingsMutation";

    /**
     * The {@code input.biz_jid} field naming the business account whose commerce settings are updated,
     * or {@code null} to omit it.
     */
    private final Jid bizJid;

    /**
     * The {@code input.cart_enabled} flag switching the business cart on or off.
     */
    private final boolean cartEnabled;

    /**
     * Constructs an update-commerce-settings mutation request carrying the owning business account and
     * the new cart toggle.
     *
     * <p>The {@code bizJid} populates the {@code biz_jid} field, omitted when {@code null}; the
     * {@code cartEnabled} flag is always written as the {@code cart_enabled} field.
     *
     * @param bizJid      the business account {@link Jid} whose commerce settings are updated, or
     *                    {@code null} to omit the field
     * @param cartEnabled whether the business cart is to be enabled
     */
    public BizCatalogManagementUpdateCommerceSettingsFacebookGraphQlRequest(Jid bizJid, boolean cartEnabled) {
        this.bizJid = bizJid;
        this.cartEnabled = cartEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String docId() {
        return DOC_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return OPERATION_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @implNote This implementation emits {@code {"input": {"biz_jid": <bizJid>, "cart_enabled":
     * <cartEnabled>}}}, writing the {@code biz_jid} field only when it is non-null and always writing
     * the {@code cart_enabled} flag.
     */
    @WhatsAppWebExport(moduleName = "WAWebBusinessProfileJob", exports = "updateCartEnabledGraphQL",
            adaptation = WhatsAppAdaptation.ADAPTED)
    @Override
    public String variables() {
        try (var writer = JSONWriter.ofUTF8()) {
            writer.startObject();
            writer.writeName("input");
            writer.writeColon();
            writer.startObject();
            if (bizJid != null) {
                writer.writeName("biz_jid");
                writer.writeColon();
                writer.writeString(bizJid.toString());
            }

            writer.writeName("cart_enabled");
            writer.writeColon();
            writer.writeBool(cartEnabled);
            writer.endObject();
            writer.endObject();
            try (var output = new StringWriter()) {
                writer.flushTo(output);
                return output.toString();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
