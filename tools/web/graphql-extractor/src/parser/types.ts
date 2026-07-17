/**
 * Normalized stanza in a flattened Relay response selection tree.
 *
 * Mirrors the relevant Relay normalization-AST stanza kinds:
 * - "scalar"         : a ScalarField (leaf value).
 * - "linked"         : a LinkedField (nested object/connection); `type` is the concrete GraphQL
 *                      type when Relay records one, `plural` marks list edges.
 * - "inlineFragment" : an InlineFragment narrowing to `onType`.
 * - "condition"      : a @include/@skip Condition gated on a request variable.
 * - other kinds (ModuleImport, Defer, Stream, ...) are passed through with their raw kind.
 */
export type ResponseNode =
  | { kind: "scalar"; name: string; alias?: string }
  | {
      kind: "linked";
      name: string;
      type: string | null;
      plural: boolean;
      alias?: string;
      selections: ResponseNode[];
    }
  | { kind: "inlineFragment"; onType: string | null; selections: ResponseNode[] }
  | {
      kind: "condition";
      variable: string;
      passingValue: boolean;
      selections: ResponseNode[];
    }
  | { kind: string; name?: string; selections?: ResponseNode[] };

/**
 * A single GraphQL request variable as recorded in the compiled artifact.
 *
 * Compiled Relay persisted queries carry only the variable name and its default value; the GraphQL
 * scalar type (String!, ID!, Boolean, ...) is stripped at build time and is not recoverable here.
 */
export interface MexVariable {
  readonly name: string;
  readonly defaultValue: unknown;
}

/**
 * Transport over which an operation is dispatched, resolved from the module dependency graph.
 *
 * - "stanza_mex" : a consumer imports a MEX client, so the persisted query is sent as a
 *                  {@code w:mex} IQ stanza over the WhatsApp socket.
 * - "http_relay" : a consumer imports the imperative Relay client
 *                  ({@code WAWebRelayClient.fetchQuery}/{@code commitMutation}), so the operation
 *                  runs over HTTPS against a Meta GraphQL endpoint.
 * - "http_comet" : the artifact is a preloadable Comet query that self-registers with
 *                  {@code relay-runtime}'s {@code PreloadableQueryRegistry} and is dispatched by id
 *                  via {@code CometRelay.loadQuery}; also HTTPS against a Meta GraphQL endpoint.
 * - "unknown"    : no consumer imports a recognised client and the artifact carries no Comet
 *                  self-signal (for example an inbound notification parser), or the dependency
 *                  graph is unavailable.
 */
export type Transport = "stanza_mex" | "http_relay" | "http_comet" | "unknown";

export type Environment =
  | "whatsapp_web"
  | "whatsapp_www"
  | "whatsapp_guest"
  | "whatsapp_catalog"
  | "facebook";

/**
 * One extracted GraphQL operation: its persisted-query identity, request and response schemas, and
 * the set of transports its consumers dispatch it over.
 *
 * The set holds the distinct transports observed across all consumers: a single-element array for
 * the common case, {@code ["mex", "http"]} when distinct consumers dispatch the same artifact over
 * both, and {@code ["unknown"]} when no consumer directly imports a client.
 */
export interface MexOperation {
  readonly module: string;
  readonly id: string | null;
  readonly name: string | null;
  readonly operationKind: string | null;
  readonly transports: Transport[];
  readonly variables: ReadonlyArray<MexVariable>;
  readonly response: ReadonlyArray<ResponseNode>;
}

export interface ClassifiedOperation extends MexOperation {
  readonly environments: Environment[];
}

export interface MexPageResult {
  readonly operations: MexOperation[];
  readonly relaySources: Record<string, string>;
}
