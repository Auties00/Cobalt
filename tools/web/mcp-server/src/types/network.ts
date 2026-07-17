export interface WebSocketFrame {
  index: number;
  ts: string;
  direction: "sent" | "received";
  opcode: number;
  payloadLength: number;
  payloadBase64: string;
  mask: boolean;
  requestId: string;
}

export interface NetworkRequest {
  index: number;
  ts: string;
  method: string;
  url: string;
  status: number | null;
  type: string;
  mimeType: string | null;
  requestSize: number | null;
  responseSize: number | null;
  timing: number | null;
  requestId: string;
}

export interface NetworkCaptureState {
  capturing: boolean;
  wsFrameCount: number;
  httpRequestCount: number;
}

export interface NetworkCaptureQuery {
  type?: "websocket" | "http" | "all";
  direction?: "sent" | "received" | "any";
  urlFilter?: string;
  query?: string;
  limit?: number;
  history?: boolean;
}
