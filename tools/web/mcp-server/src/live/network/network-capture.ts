import type { BrowserContext, CDPSession, Page } from "playwright";
import type {
  NetworkCaptureQuery,
  NetworkCaptureState,
  NetworkRequest,
  WebSocketFrame,
} from "../../types/network.js";
import { createLogger } from "../../utils/logger.js";

const log = createLogger("live:network");

export class NetworkCapture {
  private cdp: CDPSession | null = null;
  private capturing = false;
  private readonly wsFrames: WebSocketFrame[] = [];
  private readonly wsHistory: WebSocketFrame[] = [];
  private readonly httpRequests: NetworkRequest[] = [];
  private readonly httpHistory: NetworkRequest[] = [];
  private readonly pendingRequests = new Map<string, Partial<NetworkRequest>>();
  private readonly requireContext: () => BrowserContext;
  private readonly requirePage: () => Page;

  constructor(requireContext: () => BrowserContext, requirePage: () => Page) {
    this.requireContext = requireContext;
    this.requirePage = requirePage;
  }

  async start(): Promise<NetworkCaptureState> {
    if (this.capturing) {
      log.debug("network capture already active");
      return this.getState();
    }
    log.info("starting network capture via CDP");
    const context = this.requireContext();
    const page = this.requirePage();
    const cdp = await context.newCDPSession(page);
    this.cdp = cdp;

    cdp.on("Network.webSocketFrameSent", (event: Record<string, unknown>) => {
      this.recordWsFrame("sent", event);
    });
    cdp.on("Network.webSocketFrameReceived", (event: Record<string, unknown>) => {
      this.recordWsFrame("received", event);
    });
    cdp.on("Network.requestWillBeSent", (event: Record<string, unknown>) => {
      this.recordRequestStart(event);
    });
    cdp.on("Network.responseReceived", (event: Record<string, unknown>) => {
      this.recordResponseReceived(event);
    });
    cdp.on("Network.loadingFinished", (event: Record<string, unknown>) => {
      this.finalizeRequest(event);
    });
    cdp.on("Network.loadingFailed", (event: Record<string, unknown>) => {
      this.finalizeRequest(event);
    });

    await cdp.send("Network.enable");
    this.capturing = true;
    return this.getState();
  }

  async stop(): Promise<void> {
    if (!this.cdp) return;
    try {
      await this.cdp.send("Network.disable");
    } catch {  }
    try {
      await this.cdp.detach();
    } catch {  }
    this.cdp = null;
    this.capturing = false;
  }

  getState(): NetworkCaptureState {
    return {
      capturing: this.capturing,
      wsFrameCount: this.wsHistory.length,
      httpRequestCount: this.httpHistory.length,
    };
  }

  queryWsFrames(query: NetworkCaptureQuery = {}): WebSocketFrame[] {
    const limit = Math.max(1, query.limit ?? 100);
    const source = query.history !== false ? this.wsHistory : this.wsFrames;
    const filtered = source.filter((frame) => {
      if (query.direction && query.direction !== "any" && frame.direction !== query.direction) return false;
      if (query.query) {
        const haystack = `${frame.requestId} ${frame.payloadBase64}`.toLowerCase();
        if (!haystack.includes(query.query.toLowerCase())) return false;
      }
      return true;
    });
    return filtered.length <= limit ? filtered : filtered.slice(filtered.length - limit);
  }

  queryHttpRequests(query: NetworkCaptureQuery = {}): NetworkRequest[] {
    const limit = Math.max(1, query.limit ?? 100);
    const source = query.history !== false ? this.httpHistory : this.httpRequests;
    const filtered = source.filter((req) => {
      if (query.urlFilter && !req.url.toLowerCase().includes(query.urlFilter.toLowerCase())) return false;
      if (query.query) {
        const haystack = `${req.method} ${req.url} ${req.type} ${req.mimeType ?? ""}`.toLowerCase();
        if (!haystack.includes(query.query.toLowerCase())) return false;
      }
      return true;
    });
    return filtered.length <= limit ? filtered : filtered.slice(filtered.length - limit);
  }

  query(query: NetworkCaptureQuery = {}): { wsFrames: WebSocketFrame[]; httpRequests: NetworkRequest[] } {
    const type = query.type ?? "all";
    return {
      wsFrames: type === "http" ? [] : this.queryWsFrames(query),
      httpRequests: type === "websocket" ? [] : this.queryHttpRequests(query),
    };
  }

  clearBuffers(): { wsCleared: number; httpCleared: number } {
    const ws = this.wsFrames.length;
    const http = this.httpRequests.length;
    this.wsFrames.length = 0;
    this.httpRequests.length = 0;
    this.pendingRequests.clear();
    return { wsCleared: ws, httpCleared: http };
  }

  clearHistory(): { wsCleared: number; httpCleared: number } {
    const ws = this.wsHistory.length;
    const http = this.httpHistory.length;
    this.wsHistory.length = 0;
    this.httpHistory.length = 0;
    this.wsFrames.length = 0;
    this.httpRequests.length = 0;
    this.pendingRequests.clear();
    return { wsCleared: ws, httpCleared: http };
  }

  private recordWsFrame(direction: "sent" | "received", event: Record<string, unknown>): void {
    const response = event.response as Record<string, unknown> | undefined;
    const payloadData = (response?.payloadData as string) ?? "";
    const opcode = (response?.opcode as number) ?? 0;
    const mask = (response?.mask as boolean) ?? false;
    const requestId = (event.requestId as string) ?? "";

    const frame: WebSocketFrame = {
      index: this.wsHistory.length,
      ts: new Date().toISOString(),
      direction,
      opcode,
      payloadLength: payloadData.length,
      payloadBase64: Buffer.from(payloadData, "utf8").toString("base64").slice(0, 4096),
      mask,
      requestId,
    };
    this.wsFrames.push(frame);
    this.wsHistory.push(frame);
  }

  private recordRequestStart(event: Record<string, unknown>): void {
    const requestId = event.requestId as string;
    const request = event.request as Record<string, unknown> | undefined;
    if (!request) return;

    this.pendingRequests.set(requestId, {
      index: this.httpHistory.length,
      ts: new Date().toISOString(),
      method: (request.method as string) ?? "GET",
      url: (request.url as string) ?? "",
      status: null,
      type: (event.type as string) ?? "",
      mimeType: null,
      requestSize: null,
      responseSize: null,
      timing: null,
      requestId,
    });
  }

  private recordResponseReceived(event: Record<string, unknown>): void {
    const requestId = event.requestId as string;
    const pending = this.pendingRequests.get(requestId);
    if (!pending) return;

    const response = event.response as Record<string, unknown> | undefined;
    if (response) {
      pending.status = (response.status as number) ?? null;
      pending.mimeType = (response.mimeType as string) ?? null;
      const timing = response.timing as Record<string, number> | undefined;
      if (timing && typeof timing.receiveHeadersEnd === "number") {
        pending.timing = Math.round(timing.receiveHeadersEnd);
      }
    }
    pending.type = (event.type as string) ?? pending.type;
  }

  private finalizeRequest(event: Record<string, unknown>): void {
    const requestId = event.requestId as string;
    const pending = this.pendingRequests.get(requestId);
    if (!pending) return;

    pending.responseSize = (event.encodedDataLength as number) ?? null;
    const req = pending as NetworkRequest;
    this.httpRequests.push(req);
    this.httpHistory.push(req);
    this.pendingRequests.delete(requestId);
  }
}
