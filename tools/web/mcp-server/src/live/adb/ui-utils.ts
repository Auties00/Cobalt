import type { ParsedBounds, UiNode } from "../../types/live/adb.js";

export function decodeXmlAttribute(value: string): string {
  return value
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&");
}

export function parseBounds(bounds: string): ParsedBounds | null {
  const match = bounds.match(/\[(\d+),(\d+)]\[(\d+),(\d+)]/);
  if (!match) return null;
  return {
    left: Number(match[1]),
    top: Number(match[2]),
    right: Number(match[3]),
    bottom: Number(match[4]),
  };
}

export function normalizeAdbTextInput(value: string): string {
  return value
    .replace(/\s+/g, "%s")
    .replace(/["']/g, "")
    .replace(/[(){}[\]<>|&;`\\]/g, "");
}

export function parseBool(value: string | undefined): boolean {
  return value === "true";
}

export function hasAllResourceIds(nodes: UiNode[], resourceIds: string[]): boolean {
  const idSet = new Set(nodes.map((node) => node.resourceId));
  return resourceIds.every((id) => idSet.has(id));
}

export function hasAnyResourceId(nodes: UiNode[], resourceIds: string[]): boolean {
  const idSet = new Set(nodes.map((node) => node.resourceId));
  return resourceIds.some((id) => idSet.has(id));
}

export function isInteractive(node: UiNode): boolean {
  return node.enabled && (node.clickable || node.focusable);
}

export function centerY(bounds: ParsedBounds): number {
  return Math.floor((bounds.top + bounds.bottom) / 2);
}
