/**
 * Helpers for inspecting WhatsApp Web's service worker source.
 *
 * @remarks
 * The service worker is a small file at {@code /sw.js} that imports the BDLDR
 * bootstrap loader and embeds the build number. Both pieces are needed before
 * any module-level extraction can proceed.
 */

/**
 * Parses the WhatsApp Web client revision out of the service worker source.
 *
 * @param swSource - contents of {@code /sw.js}.
 * @returns the version string, e.g. {@code "2.3000.1039683107"}.
 * @throws Error if the {@code client_revision} field is missing.
 *
 * @remarks
 * The service worker contains a literal of the form
 * {@code client_revision\":NNNNNNNN,} which is the build number that gets
 * appended to {@code 2.3000.} to form the version string shown in the UI.
 */
export function extractVersion(swSource: string): string {
    const match = swSource.match(/client_revision\\":([\d.]+),/);
    if (!match) {
        throw new Error("Could not locate client_revision in service worker");
    }
    return `2.3000.${match[1]}`;
}

/**
 * Pulls the bootstrap loader URL referenced from the service worker.
 *
 * @param swSource - contents of {@code /sw.js}.
 * @returns the absolute URL of the BDLDR bootstrap script, or {@code null} if
 *          no {@code importScripts(...)} call is present.
 *
 * @remarks
 * Playwright never observes this script directly because the {@code importScripts}
 * call happens inside the service-worker thread. Pulling it explicitly is the
 * only way to get the proto-bearing module declarations it registers.
 */
export function extractBootstrapUrl(swSource: string): string | null {
    const cleaned = swSource.replaceAll("/*BTDS*/", "");
    const match = cleaned.match(/importScripts\(["']([^"']+)["']/);
    if (!match) return null;
    return match[1]!.replaceAll("\\", "");
}
