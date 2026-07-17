/**
 * AST utilities shared across the JS-proto pipeline.
 *
 * @remarks
 * Acorn nodes are typed loosely as {@link AstNode} = {@code any}. The surface
 * area we touch (CallExpression arguments, MemberExpression chains,
 * ObjectExpression properties, ArrayExpression elements) is not in acorn's
 * base {@code Node} type, and modelling every variant adds noise without
 * catching real bugs at this scale.
 */

/* eslint-disable @typescript-eslint/no-explicit-any */
/** A loosely-typed acorn AST stanza. */
export type AstNode = any;

/** Drops the {@code "Spec"} suffix exported by the WA JS bundle, leaving the canonical proto name. */
export function unspecName(name: string): string {
    return name.endsWith("Spec") ? name.slice(0, -4) : name;
}

/** Returns the final {@code $}-delimited segment, used when emitting a nested type by its short name. */
export function unnestName(name: string): string {
    return name.split("$").slice(-1)[0]!;
}

/** Returns everything up to (but excluding) the final {@code $}-delimited segment. */
export function getNesting(name: string): string {
    return name.split("$").slice(0, -1).join("$");
}

/** Canonical name produced from a raw JS export name. */
export const renameIdentifier = (name: string): string => unspecName(name);

/**
 * Recursively flattens every expression reachable from a top-level statement.
 *
 * @param node - the AST stanza to walk.
 * @returns a flat array containing the stanza and every nested expression.
 *
 * @remarks
 * Used by the module finder to scan a module body for the
 * {@code .internalSpec=} sentinel without committing to a single AST shape.
 */
export function extractAllExpressions(node: AstNode): AstNode[] {
    const expressions: AstNode[] = [node];
    const exp = node?.expression;
    if (exp) expressions.push(exp);

    const args = node?.expression?.arguments;
    if (Array.isArray(args)) {
        for (const arg of args) {
            const body = arg?.body?.body;
            if (Array.isArray(body)) {
                for (const inner of body) {
                    expressions.push(...extractAllExpressions(inner));
                }
            }
        }
    }

    const innerBody = node?.body?.body;
    if (Array.isArray(innerBody)) {
        for (const inner of innerBody) {
            if (inner?.expression) {
                expressions.push(...extractAllExpressions(inner.expression));
            }
        }
    }

    const seqExpressions = node?.expression?.expressions;
    if (Array.isArray(seqExpressions)) {
        for (const seq of seqExpressions) {
            expressions.push(...extractAllExpressions(seq));
        }
    }

    return expressions;
}

/** Flattens a chain of {@code |} binary expressions into the leaves on either side. */
export function unwrapBinaryOr(n: AstNode): AstNode[] {
    if (n?.type === "BinaryExpression" && n.operator === "|") {
        return [...unwrapBinaryOr(n.left), ...unwrapBinaryOr(n.right)];
    }
    return [n];
}
