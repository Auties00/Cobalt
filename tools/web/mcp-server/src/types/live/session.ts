export type WebAuthState =
  | "logged_in"
  | "qr_required"
  | "phone_link_required"
  | "pairing_code_ready"
  | "rate_limited"
  | "unknown";

export type SessionMode = "web" | "desktop" | "desktop_macos";

export type AccountType = "personal" | "business" | "unknown";

export interface SessionIdentity {
  jid: string | null;
  phoneNumber: string | null;
  deviceJid: string | null;
  pushName: string | null;
  accountType: AccountType;
  platform: SessionMode;
  linkedAt: string | null;
}

export interface LiveSessionInfo {
  sessionId: string;
  running: boolean;
  mode: SessionMode;
  currentUrl: string | null;
  revision: string | null;
  snapshotRevision: string | null;
  snapshotMatches: boolean | null;
  authState: WebAuthState;
  pairingCode: string | null;
  startedAt: string | null;
  updatedAt: string;
  identity: SessionIdentity | null;
  ephemeral: boolean;
  userDataDir: string | null;
}

export interface StartSessionOptions {
  sessionId?: string;
  mode?: SessionMode;
  slowMoMs?: number;
  navigationTimeoutMs?: number;
  snapshotRevision?: string | null;
  locale?: string;
  desktopCdpPort?: number;
  ephemeral?: boolean;
}

export interface PhoneLoginOptions {
  countryCode?: string;
  waitForPairingCodeTimeoutMs?: number;
}

export interface PhoneLoginResult {
  status: LiveSessionInfo;
  pairingCode: string | null;
  blockedReason?: "too_many_attempts" | null;
  details: string[];
}

export interface WaitForLoginResult {
  success: boolean;
  status: LiveSessionInfo;
}

export interface BrowserStanzaLogger {
  getEvents?: (query?: Record<string, unknown>) => unknown;
  query?: (query?: Record<string, unknown>) => unknown;
  clearEvents?: () => unknown;
  clearHistory?: () => unknown;
  sendNode?: (node: Record<string, unknown>) => unknown;
}

export interface BrowserWamLogger {
  getEvents?: (query?: Record<string, unknown>) => unknown;
  query?: (query?: Record<string, unknown>) => unknown;
  clearEvents?: () => unknown;
  clearHistory?: () => unknown;
  sendCustomEvent?: (payload: Record<string, unknown>) => unknown;
  getEventDefinitions?: (query?: Record<string, unknown>) => unknown;
  queryDefinitions?: (query?: Record<string, unknown>) => unknown;
}

export interface BrowserAbProps {
  query?: (query?: Record<string, unknown>) => unknown;
  list?: (filter?: string) => unknown;
  definitions?: (filter?: string) => unknown;
  diff?: (filter?: string) => unknown;
  get?: (name: string) => unknown;
  set?: (name: string, value: unknown) => unknown;
  reset?: (name: string) => unknown;
  resetAll?: () => unknown;
}
