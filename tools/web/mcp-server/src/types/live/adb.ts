export interface AdbDeviceInfo {
  serial: string;
  state: string;
  model: string | null;
}

export interface AdbLinkOptions {
  serial?: string;
  packageName?: string;
  stepTimeoutMs?: number;
}

export interface AdbLinkResult {
  success: boolean;
  device: AdbDeviceInfo | null;
  code: string;
  maxDevicesReached?: boolean;
  blockedReason?: string | null;
  details: string[];
}

export interface UiNode {
  index: number;
  text: string;
  contentDesc: string;
  resourceId: string;
  className: string;
  packageName: string;
  bounds: string;
  clickable: boolean;
  enabled: boolean;
  focusable: boolean;
  password: boolean;
}

export interface ParsedBounds {
  left: number;
  top: number;
  right: number;
  bottom: number;
}

export interface ExecResult {
  stdout: string;
  stderr: string;
}

export interface LinkedDeviceEntry {
  name: string;
  status: string;
  anchorNode: UiNode;
  yCenter: number;
}
