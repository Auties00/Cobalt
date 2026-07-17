export interface DebugScriptInfo {
  scriptId: string;
  url: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  hash: string | null;
  executionContextId: number | null;
  length: number | null;

  scriptLanguage: string | null;

  codeOffset: number | null;
}

export interface SetBreakpointResult {
  breakpointId: string;
  locations: Array<{
    scriptId: string;
    lineNumber: number;
    columnNumber: number;
  }>;
}

export interface EvaluateResult {
  resultType: string;
  value: unknown;
  description: string | null;
  unserializableValue: string | null;
  exception: string | null;
}

export interface RuntimeRemoteObject {
  type?: string;
  subtype?: string;
  value?: unknown;
  description?: string;
  unserializableValue?: string;
  objectId?: string;
}

export interface RuntimeExceptionDetails {
  text?: string;
  exception?: RuntimeRemoteObject;
}

export interface RuntimeEvaluateResponse {
  result?: RuntimeRemoteObject;
  exceptionDetails?: RuntimeExceptionDetails;
}

export interface DebuggerScriptParsedEvent {
  scriptId: string;
  url: string;
  startLine: number;
  startColumn: number;
  endLine: number;
  endColumn: number;
  hash?: string;
  executionContextId?: number;
  length?: number;
  scriptLanguage?: string;
  codeOffset?: number;
}

export interface WasmMemoryReadResult {
  addr: number;
  len: number;

  base64: string | null;
  error?: string;
}

export interface DebuggerSetBreakpointResponse {
  breakpointId: string;
  actualLocation?: {
    scriptId: string;
    lineNumber: number;
    columnNumber: number;
  };
}

export interface DebuggerSetBreakpointByUrlResponse {
  breakpointId: string;
  locations: Array<{
    scriptId: string;
    lineNumber: number;
    columnNumber: number;
  }>;
}

export type DebugCommand = "pause" | "resume" | "step_over" | "step_into" | "step_out";

export interface PausedCallFrame {

  callFrameId: string;
  functionName: string;
  scriptId: string;
  url: string;
  lineNumber: number;
  columnNumber: number;

  scriptLanguage?: string;
  scopeChain: Array<{
    type: string;
    object?: { objectId?: string };
  }>;
  variables: Array<{
    name: string;
    value: string;
    type: string;
  }>;
}

export interface PausedState {
  reason: string;
  callFrames: PausedCallFrame[];
  ts: string;
}

/** A CDP protocol frame on the multiplexed browser WebSocket (command reply or event). */
export interface CdpMessage {
  id?: number;
  method?: string;
  params?: Record<string, unknown>;
  result?: unknown;
  error?: { code?: number; message?: string };
  sessionId?: string;
}

/** A CDP scope entry in a paused frame's scopeChain. */
export interface CdpScope {
  type: string;
  name?: string;
  object?: { objectId?: string };
}

/** A resolved breakpoint location (CDP Debugger.Location). */
export interface BreakpointLocation {
  scriptId: string;
  lineNumber: number;
  columnNumber: number;
}

/** CDP Target.TargetInfo (subset used). */
export interface TargetInfo {
  targetId?: string;
  type?: string;
  title?: string;
  url?: string;
}

/** A target this mux has an attached session for. */
export interface SessionTarget {
  type: string;
  url: string;
  targetId: string;
}

/** An entry from the browser's /json target list (Chrome reports the target id as `id`). */
export interface BrowserTargetInfo {
  id?: string;
  targetId?: string;
  type?: string;
  title?: string;
  url?: string;
  webSocketDebuggerUrl?: string;
}

/** The browser's /json/version response (subset used). */
export interface BrowserVersionInfo {
  webSocketDebuggerUrl?: string;
  Browser?: string;
  "Protocol-Version"?: string;
}

/** A property descriptor from Runtime.getProperties. */
export interface RuntimePropertyDescriptor {
  name: string;
  value?: RuntimeRemoteObject;
}

/** The Runtime.getProperties response (subset used). */
export interface RuntimeGetPropertiesResponse {
  result?: RuntimePropertyDescriptor[];
}

/** A single buffered logpoint capture (one hit of a non-suspending logpoint). */
export interface LogpointCapture {
  id: string;
  ts: string;
  value: unknown;
}

/** A url-bound wasm breakpoint/logpoint spec. With a logExpression the registry installs a
 *  non-suspending capture (a $stack expression takes the suspending read path; see the registry). */
export interface UrlBreakpointSpec {
  url: string;
  byteOffset: number;
  condition?: string;
  logExpression?: string;
  block?: boolean;
  /** Restrict binding to these CDP target types (e.g. "worker"); null/undefined binds every target. */
  targetTypes?: ReadonlySet<string> | null;
}

/** Per-target outcome of binding a url breakpoint, so callers can see exactly which targets resolved it. */
export interface BindTargetResult {
  sessionId: string;
  type: string;
  url: string;
  bound: boolean;
  locations: BreakpointLocation[];
  error?: string;
}

/** Result of adding a url breakpoint: the registry id, the aggregated resolved locations, the per-target
 *  binding report, and an optional advisory about the breakpoint's pausing semantics. */
export interface UrlBreakpointAddResult {
  id: string;
  locations: BreakpointLocation[];
  perTarget: BindTargetResult[];
  warning?: string;
}

/** Result returned to the set_wasm_breakpoints caller: the breakpoint id (named breakpointId for the tool),
 *  the aggregated resolved locations, the per-target binding report, and an optional pausing-semantics warning. */
export interface SetWasmBreakpointResult {
  breakpointId: string;
  locations: BreakpointLocation[];
  perTarget: BindTargetResult[];
  warning?: string;
}
