/** Wire types for WAM event fields, mirroring WamType.java. */
export type WamFieldType = "INTEGER" | "BOOLEAN" | "STRING" | "FLOAT" | "TIMER" | "ENUM";

/** Transport channels for WAM events, mirroring WamChannel.java. */
export type WamChannel = "regular" | "realtime" | "private";

/** A single constant inside a WAM enum. */
export interface WamEnumConstant {
    readonly name: string;
    readonly value: number;
}

/** A fully parsed WAM enum definition. */
export interface WamEnumDef {
    readonly moduleName: string;
    readonly javaName: string;
    readonly constants: readonly WamEnumConstant[];
}

/** A single field inside a WAM event. */
export interface WamFieldDef {
    readonly name: string;
    readonly id: number;
    readonly wamType: WamFieldType;
    /** Present only when wamType === "ENUM". */
    readonly enumJavaName: string | null;
}

/** Per-build sampling weights. */
export interface WamWeights {
    readonly alpha: number;
    readonly beta: number;
    readonly release: number;
}

/** A fully parsed WAM event definition. */
export interface WamEventDef {
    readonly moduleName: string;
    readonly eventName: string;
    readonly javaClassName: string;
    readonly eventId: number;
    readonly fields: readonly WamFieldDef[];
    readonly weights: WamWeights;
    readonly channel: WamChannel;
    readonly privateStatsId: number;
}

/**
 * Bidirectional lookup from JS export names to Java enum class names.
 *
 * Keys can be either a bare export name (e.g. "DOCUMENT_TYPE") or a
 * qualified name (e.g. "WAWebWamEnumDocumentType.DOCUMENT_TYPE").
 */
export type EnumExportMap = ReadonlyMap<string, string>;
