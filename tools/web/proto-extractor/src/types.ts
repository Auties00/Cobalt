/**
 * Shared types representing the proto schema extracted from WhatsApp Web.
 *
 * @remarks
 * These types are produced by both the JavaScript-source extractor
 * ({@link import("./parse-js")}) and the wasm extractor
 * ({@link import("./parse-wasm")}), and consumed by the emitter
 * ({@link import("./emit")}).
 */

/** A {@code key = id} pair inside an enum body. */
export interface EnumValue {
    readonly name: string;
    readonly id: number;
}

/**
 * A single proto field, or -- when {@link type} is {@code "__oneof__"} -- a
 * oneof group whose member fields are stored in {@link members}.
 */
export interface MessageMember {
    name: string;
    id?: number;
    type: string;
    flags: string[];
    /** Set to {@code " [packed=true]"} once {@code packed} has been folded into the trailing options string. */
    packed?: string;
    /** Populated iff this member is a synthetic {@code __oneof__} group. */
    members?: MessageMember[];
}

/** A reference to a message or enum identifier imported from another module. */
export interface CrossRef {
    readonly alias: string;
    readonly module: string;
}

/** An aggregate proto identifier (a message or an enum) extracted from one source module. */
export interface Identifier {
    /** The canonical name, with {@code $} as the nested-type separator. */
    readonly name: string;
    /** The one-letter local alias the source assigns to this identifier inside its IIFE. */
    alias?: string;
    /** Populated for enums; the list of {@code (name, id)} pairs. */
    enumValues?: EnumValue[];
    /** Populated for messages; the field list (possibly including {@code __oneof__} groups). */
    members?: MessageMember[];
    /** Set when emitting a nested type, so the inner emit uses just the trailing segment. */
    displayName?: string;
}

/** Everything extracted from a single proto-bearing source module. */
export interface ModuleInfo {
    /** Aliases of identifiers imported from sibling proto modules. */
    crossRefs: CrossRef[];
    /** Identifiers declared in this module, keyed by their canonical {@code $}-nested name. */
    identifiers: Record<string, Identifier>;
}

/** Tracks parent/child relationships between nested identifiers via their {@code Parent$Child$Grandchild} names. */
export interface IndentationEntry {
    /** The {@code $}-joined parent path; empty string for top-level identifiers. */
    indentation?: string;
    /** All immediate or transitive children that name this identifier as an ancestor. */
    members?: Set<string>;
}

/** The aggregate result produced by both extractors. */
export interface ParsedProtos {
    /** Per-module identifier/crossRef state, keyed by the source module name. */
    readonly modulesInfo: Record<string, ModuleInfo>;
    /** Indentation/nesting map keyed by canonical identifier name. */
    readonly indentation: Record<string, IndentationEntry>;
    /** The original order in which top-level modules were discovered; used for stable iteration. */
    readonly moduleOrder: string[];
}
