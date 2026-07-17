/** The type of an AB prop value as declared in WhatsApp Web. */
export type ABPropType = "bool" | "int" | "float" | "string";

/** A single AB prop definition extracted from WhatsApp Web. */
export interface ABPropDef {
    /** The snake_case property name as it appears in the JS source. */
    readonly name: string;
    /** The unique numeric configuration code. */
    readonly code: number;
    /** The value type: {@code "bool"}, {@code "int"}, {@code "float"}, or {@code "string"}. */
    readonly type: ABPropType;
    /** The production default value as a string. */
    readonly defaultValue: string;
    /** The debug/beta default value as a string, used when Web Beta is joined. */
    readonly debugDefaultValue: string;
    /** The definition as it appears in the JS source */
    readonly sourceDefinition: string;
}
