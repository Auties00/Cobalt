import type { ParsedModule } from "../types/module.js";

const MODULE_PATTERN =
  /__d\("([^"]+)",\s*(\[[^\]]*]),\s*\(?function\(([^)]*)\)/g;

const FACTORY_SIGNATURE =
  /^\s*(?:async\s+)?function\b\s*\*?\s*[A-Za-z0-9_$]*\s*\(([^)]*)\)/;

/**
 * Reports whether a Haste module name belongs to the WhatsApp application surface.
 *
 * Keeps every module whose name references WhatsApp ({@code WA}) or the Comet
 * click-to-WhatsApp ads layer ({@code LWI}) - including the {@code use*} React-compiler hooks,
 * {@code convert*} helpers, and {@code *.react} components - while excluding pure third-party
 * vendor modules (react, relay-runtime, scheduler, and the like). The older
 * {@code name.startsWith("WA") && !name.endsWith(".react")} rule silently dropped the entire
 * business/ads surface (the {@code use*}/{@code LWI*} modules and every component that builds a
 * GraphQL input), which never made it into the catalog.
 */
export function isWhatsAppModule(name: string): boolean {
  return /WA|LWI/.test(name);
}

export function extractFunctionBody(
  content: string,
  startIndex: number
): string | null {
  let depth = 0;
  let i = startIndex;

  while (i < content.length && content[i] !== "{") i += 1;
  if (i >= content.length) return null;

  const bodyStart = i;

  const enum State {
    Code,
    SingleQuote,
    DoubleQuote,
    TemplateLiteral,
    TemplateExpr,
    LineComment,
    BlockComment,
    Regex,
  }

  let state: State = State.Code;
  let templateDepth = 0;
  let templateBraceStack: number[] = [];

  for (; i < content.length; i += 1) {
    const ch = content[i];
    const prev = i > 0 ? content[i - 1] : "\0";

    switch (state) {
      case State.SingleQuote:
        if (ch === "'" && prev !== "\\") state = State.Code;
        continue;

      case State.DoubleQuote:
        if (ch === '"' && prev !== "\\") state = State.Code;
        continue;

      case State.TemplateLiteral:
        if (ch === "\\" ) {
          i += 1;
          continue;
        }
        if (ch === "`") {
          state = templateDepth > 0 ? State.TemplateExpr : State.Code;
          if (templateDepth > 0) templateDepth--;
          continue;
        }
        if (ch === "$" && i + 1 < content.length && content[i + 1] === "{") {
          i += 1;
          templateDepth++;
          templateBraceStack.push(depth);
          depth++;
          state = State.Code;
        }
        continue;

      case State.TemplateExpr:
        break;

      case State.LineComment:
        if (ch === "\n") state = State.Code;
        continue;

      case State.BlockComment:
        if (ch === "*" && i + 1 < content.length && content[i + 1] === "/") {
          i += 1;
          state = State.Code;
        }
        continue;

      case State.Regex:
        if (ch === "\\" ) {
          i += 1;
          continue;
        }
        if (ch === "/") state = State.Code;
        continue;

      case State.Code:
        break;
    }

    if (ch === "/" && i + 1 < content.length) {
      const next = content[i + 1];
      if (next === "/") {
        state = State.LineComment;
        i += 1;
        continue;
      }
      if (next === "*") {
        state = State.BlockComment;
        i += 1;
        continue;
      }
      if (isRegexStart(content, i)) {
        state = State.Regex;
        continue;
      }
    }

    if (ch === "'") {
      state = State.SingleQuote;
      continue;
    }
    if (ch === '"') {
      state = State.DoubleQuote;
      continue;
    }
    if (ch === "`") {
      state = State.TemplateLiteral;
      continue;
    }

    if (ch === "{") {
      depth += 1;
    } else if (ch === "}") {
      depth -= 1;
      if (templateBraceStack.length > 0 && depth === templateBraceStack[templateBraceStack.length - 1]) {
        templateBraceStack.pop();
        state = State.TemplateLiteral;
        continue;
      }
      if (depth === 0) return content.slice(bodyStart, i + 1);
    }
  }

  return null;
}

function isRegexStart(content: string, slashIndex: number): boolean {
  let j = slashIndex - 1;
  while (j >= 0 && (content[j] === " " || content[j] === "\t")) j--;
  if (j < 0) return true;
  const ch = content[j];
  return (
    ch === "=" ||
    ch === "(" ||
    ch === "[" ||
    ch === "!" ||
    ch === "&" ||
    ch === "|" ||
    ch === "?" ||
    ch === ":" ||
    ch === "," ||
    ch === ";" ||
    ch === "{" ||
    ch === "}" ||
    ch === "\n" ||
    ch === "~" ||
    ch === "^" ||
    ch === "+"  ||
    ch === "-"
  );
}

export function extractExportNames(
  body: string,
  exportsParam: string
): string[] {
  const exports = new Set<string>();

  const dotPattern = new RegExp(
    `${exportsParam}\\.([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*=`,
    "g"
  );
  const bracketPattern = new RegExp(
    `${exportsParam}\\[["']([^"']+)["']\\]\\s*=`,
    "g"
  );

  let match: RegExpExecArray | null;
  while ((match = dotPattern.exec(body))) exports.add(match[1]);
  while ((match = bracketPattern.exec(body))) exports.add(match[1]);

  const definePropertyPattern = new RegExp(
    `Object\\.defineProperty\\(\\s*${exportsParam}\\s*,\\s*["']([^"']+)["']`,
    "g"
  );
  while ((match = definePropertyPattern.exec(body))) exports.add(match[1]);

  const assignPattern = new RegExp(
    `Object\\.assign\\(\\s*${exportsParam}\\s*,\\s*\\{([^}]*)\\}`,
    "g"
  );
  while ((match = assignPattern.exec(body))) {
    const block = match[1];
    const keyPattern = /([a-zA-Z_$][a-zA-Z0-9_$]*)\s*[,:]/g;
    let keyMatch: RegExpExecArray | null;
    while ((keyMatch = keyPattern.exec(block))) {
      exports.add(keyMatch[1]);
    }
  }

  const spreadPattern = new RegExp(
    `Object\\.keys\\(([a-zA-Z_$][a-zA-Z0-9_$]*)\\)\\.forEach[\\s\\S]*?${exportsParam}\\[`,
    "g"
  );
  while ((match = spreadPattern.exec(body))) {

  }

  return [...exports];
}

export function parseModules(content: string): ParsedModule[] {
  const modules: ParsedModule[] = [];
  MODULE_PATTERN.lastIndex = 0;

  let match: RegExpExecArray | null;
  while ((match = MODULE_PATTERN.exec(content))) {
    const [fullMatch, name, depsStr, paramsStr] = match;
    if (!isWhatsAppModule(name)) continue;

    const funcStart = match.index + fullMatch.length;

    let dependencies: string[] = [];
    try {
      dependencies = JSON.parse(depsStr);
    } catch {
      dependencies = (depsStr.match(/"([^"]+)"/g) ?? []).map((d) =>
        d.slice(1, -1)
      );
    }

    const body = extractFunctionBody(content, funcStart);
    if (!body) continue;

    const params = paramsStr.split(",").map((p) => p.trim());
    const exportsParam = params[params.length - 1];
    const exports = exportsParam ? extractExportNames(body, exportsParam) : [];

    modules.push({
      name,
      dependencies,
      exports,
      body,
    });
  }

  return modules;
}

/**
 * Builds a {@link ParsedModule} from a module definition captured live in the browser.
 *
 * Takes the module name and dependency array as recorded by the {@code __d} define-hook, plus the
 * factory function's {@code Function.prototype.toString()} source, and extracts the same body and
 * export set that {@link parseModules} derives from re-fetched bundle text. Reading straight from
 * the runtime {@code __d} registry avoids the file-based pipeline's failure modes: it is immune to
 * factory-shape variations (named factories, wrapped {@code (function(){})} forms), to lazily
 * force-loaded chunks whose network response was never captured, and to Node-side re-fetch failures.
 * Returns {@code null} when {@code factorySource} carries no braced function body to extract.
 */
export function parseRuntimeModule(
  name: string,
  dependencies: string[],
  factorySource: string
): ParsedModule | null {
  const signature = factorySource.match(FACTORY_SIGNATURE);
  const params = signature
    ? signature[1].split(",").map((p) => p.trim()).filter(Boolean)
    : [];

  const body = extractFunctionBody(factorySource, 0);
  if (!body) return null;

  const exportsParam = params[params.length - 1];
  const exports = exportsParam ? extractExportNames(body, exportsParam) : [];

  return { name, dependencies, exports, body };
}
