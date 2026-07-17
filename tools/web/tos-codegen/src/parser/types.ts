/** A single Terms-of-Service notice definition extracted from WhatsApp Web. */
export interface TosNoticeDef {
    /** The UPPER_SNAKE_CASE constant name to emit, e.g. {@code "TOS_3"}. */
    readonly name: string;
    /**
     * The default notice id baked into the JS source (e.g. {@code "20210210"}), or
     * {@code null} when the id is supplied entirely by an AB-prop.
     */
    readonly defaultId: string | null;
    /**
     * The snake_case AB-prop that supplies or overrides the notice id for the web
     * (non-SMB) client, or {@code null} when the id is a fixed constant.
     */
    readonly webProp: string | null;
    /**
     * The snake_case AB-prop that supplies the notice id for the SMB (WhatsApp
     * Business) client, used in place of {@link webProp} when the linked device is
     * a business app, or {@code null} when the notice has no SMB variant.
     */
    readonly smbProp: string | null;
    /**
     * Whether the resolved AB-prop value is a comma-separated list of ids (a notice
     * group) rather than a single id.
     */
    readonly multiValued: boolean;
    /** The source location the definition was extracted from, recorded in the javadoc. */
    readonly source: string;
}
