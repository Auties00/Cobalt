package com.github.auties00.cobalt.wire.linked.message.list;

import com.github.auties00.cobalt.wire.core.jid.Jid;
import com.github.auties00.cobalt.wire.linked.message.context.ContextInfo;
import com.github.auties00.cobalt.wire.linked.message.context.ContextualMessage;

import java.util.Collections;
import java.util.List;
import it.auties.protobuf.annotation.*;
import it.auties.protobuf.model.*;
import java.util.Optional;

/**
 * A message that presents the recipient with a tappable list of selectable
 * options organised into named sections.
 *
 * <p>List messages are commonly sent by WhatsApp Business accounts to offer
 * the user a structured menu of choices. The recipient taps a button to open
 * a modal sheet that displays the sections and rows; tapping a row sends back
 * a {@link ListResponseMessage} carrying the identifier of the selected row.
 *
 * <p>A list message supports two flavours determined by {@link ListType}:
 * a single-select menu populated via {@link #sections()} where each
 * {@link Section} contains a list of {@link Row} entries, or a product list
 * populated via {@link #productListInfo()} where sections group business
 * products displayed with a header image.
 */
@ProtobufMessage(name = "Message.ListMessage")
public final class ListMessage implements ContextualMessage {
    /**
     * The title shown at the top of the list sheet.
     */
    @ProtobufProperty(index = 1, type = ProtobufType.STRING)
    String title;

    /**
     * The descriptive body text displayed above the button that opens the
     * list sheet.
     */
    @ProtobufProperty(index = 2, type = ProtobufType.STRING)
    String description;

    /**
     * The label shown on the button that, when tapped, opens the list sheet.
     */
    @ProtobufProperty(index = 3, type = ProtobufType.STRING)
    String buttonText;

    /**
     * Declares which list variant this message carries, i.e. whether it is a
     * single-select menu or a product list.
     */
    @ProtobufProperty(index = 4, type = ProtobufType.ENUM)
    ListType listType;

    /**
     * The ordered list of sections rendered in the list sheet when the list
     * type is {@link ListType#SINGLE_SELECT}. Each section groups a set of
     * selectable rows under a common header.
     */
    @ProtobufProperty(index = 5, type = ProtobufType.MESSAGE)
    List<Section> sections;

    /**
     * The product catalogue payload rendered in the list sheet when the list
     * type is {@link ListType#PRODUCT_LIST}.
     */
    @ProtobufProperty(index = 6, type = ProtobufType.MESSAGE)
    ProductListInfo productListInfo;

    /**
     * The footer text shown underneath the body and above the list button.
     */
    @ProtobufProperty(index = 7, type = ProtobufType.STRING)
    String footerText;

    /**
     * Contextual metadata attached to this message such as quoted message
     * information, mentioned users, and forwarding details.
     */
    @ProtobufProperty(index = 8, type = ProtobufType.MESSAGE)
    ContextInfo contextInfo;


    /**
     * Constructs a new {@code ListMessage} with the given properties. This
     * constructor is package-private; instances are normally built via the
     * generated {@code ListMessageBuilder}.
     *
     * @param title           the title shown at the top of the list sheet
     * @param description     the descriptive body text shown above the
     *                        opening button
     * @param buttonText      the label displayed on the opening button
     * @param listType        the list variant carried by this message
     * @param sections        the sections rendered for single-select lists
     * @param productListInfo the catalogue payload rendered for product lists
     * @param footerText      the footer text shown at the bottom of the
     *                        message bubble
     * @param contextInfo     contextual metadata attached to this message
     */
    ListMessage(String title, String description, String buttonText, ListType listType, List<Section> sections, ProductListInfo productListInfo, String footerText, ContextInfo contextInfo) {
        this.title = title;
        this.description = description;
        this.buttonText = buttonText;
        this.listType = listType;
        this.sections = sections;
        this.productListInfo = productListInfo;
        this.footerText = footerText;
        this.contextInfo = contextInfo;
    }

    /**
     * Returns the title shown at the top of the list sheet.
     *
     * @return an {@link Optional} containing the title, or empty if none is
     *         set
     */
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the descriptive body text displayed above the opening button.
     *
     * @return an {@link Optional} containing the description, or empty if
     *         none is set
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the label shown on the button that opens the list sheet.
     *
     * @return an {@link Optional} containing the button label, or empty if
     *         none is set
     */
    public Optional<String> buttonText() {
        return Optional.ofNullable(buttonText);
    }

    /**
     * Returns the list variant carried by this message.
     *
     * @return an {@link Optional} containing the list type, or empty if none
     *         is set
     */
    public Optional<ListType> listType() {
        return Optional.ofNullable(listType);
    }

    /**
     * Returns the ordered list of sections rendered for single-select lists.
     *
     * @return an unmodifiable {@link List} of sections, never {@code null}
     */
    public List<Section> sections() {
        return sections == null ? List.of() : Collections.unmodifiableList(sections);
    }

    /**
     * Returns the catalogue payload rendered for product lists.
     *
     * @return an {@link Optional} containing the product list info, or empty
     *         if none is set
     */
    public Optional<ProductListInfo> productListInfo() {
        return Optional.ofNullable(productListInfo);
    }

    /**
     * Returns the footer text shown at the bottom of the message bubble.
     *
     * @return an {@link Optional} containing the footer text, or empty if
     *         none is set
     */
    public Optional<String> footerText() {
        return Optional.ofNullable(footerText);
    }

    /**
     * Returns the contextual metadata attached to this message.
     *
     * @return an {@link Optional} containing the context info, or empty if
     *         none is set
     */
    public Optional<ContextInfo> contextInfo() {
        return Optional.ofNullable(contextInfo);
    }

    /**
     * Sets the title shown at the top of the list sheet.
     *
     * @param title the new title, or {@code null} to clear it
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the descriptive body text displayed above the opening button.
     *
     * @param description the new description, or {@code null} to clear it
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the label shown on the button that opens the list sheet.
     *
     * @param buttonText the new button label, or {@code null} to clear it
     */
    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    /**
     * Sets the list variant carried by this message.
     *
     * @param listType the new list type, or {@code null} to clear it
     */
    public void setListType(ListType listType) {
        this.listType = listType;
    }

    /**
     * Sets the ordered list of sections rendered for single-select lists.
     *
     * @param sections the new sections, or {@code null} to clear them
     */
    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    /**
     * Sets the catalogue payload rendered for product lists.
     *
     * @param productListInfo the new product list info, or {@code null} to
     *                        clear it
     */
    public void setProductListInfo(ProductListInfo productListInfo) {
        this.productListInfo = productListInfo;
    }

    /**
     * Sets the footer text shown at the bottom of the message bubble.
     *
     * @param footerText the new footer text, or {@code null} to clear it
     */
    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    /**
     * Sets the contextual metadata attached to this message.
     *
     * @param contextInfo the new context info, or {@code null} to clear it
     */
    public void setContextInfo(ContextInfo contextInfo) {
        this.contextInfo = contextInfo;
    }

    /**
     * Enumerates the variants a {@link ListMessage} can take.
     *
     * <p>The variant determines which payload is used to render the list
     * sheet: {@link #SINGLE_SELECT} uses the {@link Section} list, while
     * {@link #PRODUCT_LIST} uses a {@link ProductListInfo} catalogue.
     */
    @ProtobufEnum(name = "Message.ListMessage.ListType")
    public static enum ListType {
        /**
         * The list variant is unknown or unspecified.
         */
        UNKNOWN(0),
        /**
         * A menu in which the recipient can select exactly one row from the
         * sections.
         */
        SINGLE_SELECT(1),
        /**
         * A catalogue in which the recipient can browse business products
         * organised into sections.
         */
        PRODUCT_LIST(2);

        /**
         * Constructs a list type with the given protobuf index.
         *
         * @param index the protobuf enum index
         */
        ListType(@ProtobufEnumIndex int index) {
            this.index = index;
        }

        /**
         * The protobuf enum index associated with this variant.
         */
        final int index;

        /**
         * Returns the protobuf enum index associated with this variant.
         *
         * @return the enum index
         */
        public int index() {
            return this.index;
        }
    }

    /**
     * A single product entry inside a {@link ProductSection} of a product
     * list message.
     *
     * <p>Each product references a catalogue item owned by the business
     * account via its identifier.
     */
    @ProtobufMessage(name = "Message.ListMessage.Product")
    public static final class Product {
        /**
         * The identifier of the business catalogue product referenced by
         * this entry.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String productId;


        /**
         * Constructs a new {@code Product} referencing the given catalogue
         * identifier.
         *
         * @param productId the catalogue product identifier
         */
        Product(String productId) {
            this.productId = productId;
        }

        /**
         * Returns the identifier of the referenced catalogue product.
         *
         * @return an {@link Optional} containing the product identifier, or
         *         empty if none is set
         */
        public Optional<String> productId() {
            return Optional.ofNullable(productId);
        }

        /**
         * Sets the identifier of the referenced catalogue product.
         *
         * @param productId the new product identifier, or {@code null} to
         *                  clear it
         */
        public void setProductId(String productId) {
            this.productId = productId;
    }
    }

    /**
     * A header image shown at the top of a product list sheet.
     *
     * <p>The image is tied to a specific product from the business catalogue
     * and is accompanied by an inline JPEG thumbnail that clients render
     * while the full image is still being fetched.
     */
    @ProtobufMessage(name = "Message.ListMessage.ProductListHeaderImage")
    public static final class ProductListHeaderImage {
        /**
         * The identifier of the catalogue product whose image is used as the
         * header.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String productId;

        /**
         * The inline JPEG thumbnail rendered as a low-resolution preview of
         * the header image.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.BYTES)
        byte[] jpegThumbnail;


        /**
         * Constructs a new {@code ProductListHeaderImage} referencing the
         * given product and carrying the given inline thumbnail.
         *
         * @param productId     the catalogue product identifier
         * @param jpegThumbnail the inline JPEG thumbnail bytes
         */
        ProductListHeaderImage(String productId, byte[] jpegThumbnail) {
            this.productId = productId;
            this.jpegThumbnail = jpegThumbnail;
        }

        /**
         * Returns the identifier of the catalogue product whose image is
         * used as the header.
         *
         * @return an {@link Optional} containing the product identifier, or
         *         empty if none is set
         */
        public Optional<String> productId() {
            return Optional.ofNullable(productId);
        }

        /**
         * Returns the inline JPEG thumbnail rendered as a preview of the
         * header image.
         *
         * @return an {@link Optional} containing the thumbnail bytes, or
         *         empty if none are set
         */
        public Optional<byte[]> jpegThumbnail() {
            return Optional.ofNullable(jpegThumbnail);
        }

        /**
         * Sets the identifier of the catalogue product whose image is used
         * as the header.
         *
         * @param productId the new product identifier, or {@code null} to
         *                  clear it
         */
        public void setProductId(String productId) {
            this.productId = productId;
    }

        /**
         * Sets the inline JPEG thumbnail rendered as a preview of the header
         * image.
         *
         * @param jpegThumbnail the new thumbnail bytes, or {@code null} to
         *                      clear them
         */
        public void setJpegThumbnail(byte[] jpegThumbnail) {
            this.jpegThumbnail = jpegThumbnail;
    }
    }

    /**
     * The catalogue payload of a product list message, grouping products
     * into sections and attaching a header image plus the business owner
     * identity.
     */
    @ProtobufMessage(name = "Message.ListMessage.ProductListInfo")
    public static final class ProductListInfo {
        /**
         * The ordered list of sections, each grouping a set of related
         * products.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.MESSAGE)
        List<ProductSection> productSections;

        /**
         * The header image rendered at the top of the catalogue sheet.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        ProductListHeaderImage headerImage;

        /**
         * The {@link Jid} of the business account that owns the products in
         * this list.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        Jid businessOwnerJid;


        /**
         * Constructs a new {@code ProductListInfo} with the given sections,
         * header image, and business owner identity.
         *
         * @param productSections  the sections grouping the listed products
         * @param headerImage      the header image rendered at the top of
         *                         the sheet
         * @param businessOwnerJid the {@link Jid} of the business account
         *                         owning the products
         */
        ProductListInfo(List<ProductSection> productSections, ProductListHeaderImage headerImage, Jid businessOwnerJid) {
            this.productSections = productSections;
            this.headerImage = headerImage;
            this.businessOwnerJid = businessOwnerJid;
        }

        /**
         * Returns the ordered list of product sections.
         *
         * @return an unmodifiable {@link List} of sections, never
         *         {@code null}
         */
        public List<ProductSection> productSections() {
            return productSections == null ? List.of() : Collections.unmodifiableList(productSections);
        }

        /**
         * Returns the header image rendered at the top of the catalogue
         * sheet.
         *
         * @return an {@link Optional} containing the header image, or empty
         *         if none is set
         */
        public Optional<ProductListHeaderImage> headerImage() {
            return Optional.ofNullable(headerImage);
        }

        /**
         * Returns the {@link Jid} of the business account that owns the
         * listed products.
         *
         * @return an {@link Optional} containing the business owner JID, or
         *         empty if none is set
         */
        public Optional<Jid> businessOwnerJid() {
            return Optional.ofNullable(businessOwnerJid);
        }

        /**
         * Sets the ordered list of product sections.
         *
         * @param productSections the new sections, or {@code null} to clear
         *                        them
         */
        public void setProductSections(List<ProductSection> productSections) {
            this.productSections = productSections;
    }

        /**
         * Sets the header image rendered at the top of the catalogue sheet.
         *
         * @param headerImage the new header image, or {@code null} to clear
         *                    it
         */
        public void setHeaderImage(ProductListHeaderImage headerImage) {
            this.headerImage = headerImage;
    }

        /**
         * Sets the {@link Jid} of the business account that owns the listed
         * products.
         *
         * @param businessOwnerJid the new business owner JID, or {@code null}
         *                         to clear it
         */
        public void setBusinessOwnerJid(Jid businessOwnerJid) {
            this.businessOwnerJid = businessOwnerJid;
    }
    }

    /**
     * A named group of {@link Product} entries inside a product list
     * message.
     *
     * <p>Each section is rendered as a headed block in the catalogue sheet,
     * allowing businesses to organise their products by category.
     */
    @ProtobufMessage(name = "Message.ListMessage.ProductSection")
    public static final class ProductSection {
        /**
         * The header title displayed above the products in this section.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;

        /**
         * The ordered list of products contained in this section.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        List<Product> products;


        /**
         * Constructs a new {@code ProductSection} with the given title and
         * product entries.
         *
         * @param title    the header title of the section
         * @param products the products contained in the section
         */
        ProductSection(String title, List<Product> products) {
            this.title = title;
            this.products = products;
        }

        /**
         * Returns the header title displayed above the products in this
         * section.
         *
         * @return an {@link Optional} containing the section title, or empty
         *         if none is set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the ordered list of products contained in this section.
         *
         * @return an unmodifiable {@link List} of products, never
         *         {@code null}
         */
        public List<Product> products() {
            return products == null ? List.of() : Collections.unmodifiableList(products);
        }

        /**
         * Sets the header title displayed above the products in this
         * section.
         *
         * @param title the new section title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the ordered list of products contained in this section.
         *
         * @param products the new products, or {@code null} to clear them
         */
        public void setProducts(List<Product> products) {
            this.products = products;
    }
    }

    /**
     * A single selectable row inside a {@link Section} of a single-select
     * list message.
     *
     * <p>Each row displays a title and an optional description, and carries
     * a stable identifier that the recipient's client echoes back in a
     * {@link ListResponseMessage} when the row is tapped.
     */
    @ProtobufMessage(name = "Message.ListMessage.Row")
    public static final class Row {
        /**
         * The primary title text shown on the row.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;

        /**
         * The secondary description text shown under the title.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.STRING)
        String description;

        /**
         * The stable identifier echoed back in the selection reply when this
         * row is tapped.
         */
        @ProtobufProperty(index = 3, type = ProtobufType.STRING)
        String rowId;


        /**
         * Constructs a new {@code Row} with the given title, description,
         * and row identifier.
         *
         * @param title       the primary title shown on the row
         * @param description the secondary description shown under the title
         * @param rowId       the identifier echoed back on selection
         */
        Row(String title, String description, String rowId) {
            this.title = title;
            this.description = description;
            this.rowId = rowId;
        }

        /**
         * Returns the primary title text shown on the row.
         *
         * @return an {@link Optional} containing the title, or empty if none
         *         is set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the secondary description text shown under the title.
         *
         * @return an {@link Optional} containing the description, or empty
         *         if none is set
         */
        public Optional<String> description() {
            return Optional.ofNullable(description);
        }

        /**
         * Returns the stable identifier echoed back when this row is
         * selected.
         *
         * @return an {@link Optional} containing the row identifier, or
         *         empty if none is set
         */
        public Optional<String> rowId() {
            return Optional.ofNullable(rowId);
        }

        /**
         * Sets the primary title text shown on the row.
         *
         * @param title the new title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the secondary description text shown under the title.
         *
         * @param description the new description, or {@code null} to clear
         *                    it
         */
        public void setDescription(String description) {
            this.description = description;
    }

        /**
         * Sets the stable identifier echoed back when this row is selected.
         *
         * @param rowId the new row identifier, or {@code null} to clear it
         */
        public void setRowId(String rowId) {
            this.rowId = rowId;
    }
    }

    /**
     * A named group of selectable {@link Row} entries inside a single-select
     * list message.
     *
     * <p>Each section is rendered as a headed block in the list sheet,
     * allowing senders to organise the available choices by category.
     */
    @ProtobufMessage(name = "Message.ListMessage.Section")
    public static final class Section {
        /**
         * The header title displayed above the rows in this section.
         */
        @ProtobufProperty(index = 1, type = ProtobufType.STRING)
        String title;

        /**
         * The ordered list of selectable rows contained in this section.
         */
        @ProtobufProperty(index = 2, type = ProtobufType.MESSAGE)
        List<Row> rows;


        /**
         * Constructs a new {@code Section} with the given title and rows.
         *
         * @param title the header title of the section
         * @param rows  the selectable rows contained in the section
         */
        Section(String title, List<Row> rows) {
            this.title = title;
            this.rows = rows;
        }

        /**
         * Returns the header title displayed above the rows in this section.
         *
         * @return an {@link Optional} containing the section title, or empty
         *         if none is set
         */
        public Optional<String> title() {
            return Optional.ofNullable(title);
        }

        /**
         * Returns the ordered list of selectable rows contained in this
         * section.
         *
         * @return an unmodifiable {@link List} of rows, never {@code null}
         */
        public List<Row> rows() {
            return rows == null ? List.of() : Collections.unmodifiableList(rows);
        }

        /**
         * Sets the header title displayed above the rows in this section.
         *
         * @param title the new section title, or {@code null} to clear it
         */
        public void setTitle(String title) {
            this.title = title;
    }

        /**
         * Sets the ordered list of selectable rows contained in this
         * section.
         *
         * @param rows the new rows, or {@code null} to clear them
         */
        public void setRows(List<Row> rows) {
            this.rows = rows;
    }
    }
}
